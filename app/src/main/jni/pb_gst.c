#include <string.h>
#include <stdint.h>
#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <gst/gst.h>
#include <gst/video/video.h>
#include <pthread.h>

#include <nice/agent.h>

//#include "nice_utils.h"
#include "tcp_utils.h"
#include "pb.h"


NiceAgent * gst_agent;
guint gst_stream_id;

GST_DEBUG_CATEGORY_STATIC (debug_category);
#define GST_CAT_DEFAULT debug_category

/*
 * These macros provide a way to store the native pointer to CustomData, which might be 32 or 64 bits, into
 * a jlong, which is always 64 bits, without warnings.
 */
#if GLIB_SIZEOF_VOID_P == 8
# define GET_CUSTOM_DATA(env, thiz, fieldID) (CustomData *)(*env)->GetLongField (env, thiz, fieldID)
# define SET_CUSTOM_DATA(env, thiz, fieldID, data) (*env)->SetLongField (env, thiz, fieldID, (jlong)data)
#else
# define GET_CUSTOM_DATA(env, thiz, fieldID) (CustomData *)(jint)(*env)->GetLongField (env, thiz, fieldID)
# define SET_CUSTOM_DATA(env, thiz, fieldID, data) (*env)->SetLongField (env, thiz, fieldID, (jlong)(jint)data)
#endif

static int pfd[2];
static pthread_t thr;
static const char *tag = "myapp";

/* Structure to contain all our information, so we can pass it to callbacks */
typedef struct _CustomData {
	jobject app;            /* Application instance, used to call its methods. A global reference is kept. */
	GstElement *pipeline;   /* The running pipeline */
	//GMainContext *context;  /* GLib context used to run the main loop */
	GMainLoop *main_loop;   /* GLib main loop */
	gboolean initialized;   /* To avoid informing the UI multiple times about the initialization */
	GstElement *video_sink; /* The video sink element which receives XOverlay commands */
	ANativeWindow *native_window; /* The Android native window where video will be rendered */

	GstElement *nicesrc;
	GstElement *rtph264depay;
	GstElement *avdec_h264;
	GstElement *videoconvert;
	GstElement *queue;
	GstElement *autovideosink;
	GstElement *rtpjitterbuffer;
	gint64  * jitter_stats;
} CustomData;

/* These global variables cache values which are not changing during execution */
static pthread_t gst_app_thread;
static pthread_key_t current_jni_env;
static JavaVM *java_vm;
static jfieldID custom_data_field_id;
static jmethodID set_message_method_id;
static jmethodID on_gstreamer_initialized_method_id;

/*
 * Private methods
 */

/* Register this thread with the VM */
static JNIEnv *attach_current_thread (void) {
	JNIEnv *env;
	JavaVMAttachArgs args;

	GST_DEBUG ("Attaching thread %p", g_thread_self ());
	args.version = JNI_VERSION_1_4;
	args.name = NULL;
	args.group = NULL;

	if ((*java_vm)->AttachCurrentThread (java_vm, &env, &args) < 0) {
	GST_ERROR ("Failed to attach current thread");
	return NULL;
	}

	return env;
}

/* Unregister this thread from the VM */
static void detach_current_thread (void *env) {
	GST_DEBUG ("Detaching thread %p", g_thread_self ());
	(*java_vm)->DetachCurrentThread (java_vm);
}

/* Retrieve the JNI environment for this thread */
static JNIEnv *get_jni_env (void) {
	JNIEnv *env;

	if ((env = pthread_getspecific (current_jni_env)) == NULL) {
	env = attach_current_thread ();
	pthread_setspecific (current_jni_env, env);
	}

	return env;
}

/* Change the content of the UI's TextView */
static void set_ui_message (const gchar *message, CustomData *data) {

	JNIEnv *env = get_jni_env ();
	GST_DEBUG ("Setting message to: %s", message);
	jstring jmessage = (*env)->NewStringUTF(env, message);
	(*env)->CallVoidMethod (env, data->app, set_message_method_id, jmessage);

	if ((*env)->ExceptionCheck (env)) {
		GST_ERROR ("Failed to call Java method");
		(*env)->ExceptionClear (env);
	}
	(*env)->DeleteLocalRef (env, jmessage);
}

/* Retrieve errors from the bus and show them on the UI */
static void error_cb (GstBus *bus, GstMessage *msg, CustomData *data) {
	GError *err;
	gchar *debug_info;
	gchar *message_string;

	gst_message_parse_error (msg, &err, &debug_info);
	message_string = g_strdup_printf ("Error received from element %s: %s", GST_OBJECT_NAME (msg->src), err->message);
	g_clear_error (&err);
	g_free (debug_info);
	set_ui_message (message_string, data);
	g_free (message_string);
	gst_element_set_state (data->pipeline, GST_STATE_NULL);
}

/* Notify UI about pipeline state changes */
static void state_changed_cb (GstBus *bus, GstMessage *msg, CustomData *data) {

	GstState old_state, new_state, pending_state;
	gst_message_parse_state_changed (msg, &old_state, &new_state, &pending_state);

	/* Only pay attention to messages coming from the pipeline, not its children */
	if (GST_MESSAGE_SRC (msg) == GST_OBJECT (data->pipeline)) {
		gchar *message = g_strdup_printf("State changed to %s", gst_element_state_get_name(new_state));
		set_ui_message(message, data);
		g_free (message);
	}
}

/* Check if all conditions are met to report GStreamer as initialized.
 * These conditions will change depending on the application */
static void check_initialization_complete (CustomData *data) {
	JNIEnv *env = get_jni_env ();
	if (!data->initialized && data->native_window && data->main_loop) {
	GST_DEBUG ("Initialization complete, notifying application. native_window:%p main_loop:%p", data->native_window, data->main_loop);

	/* The main loop is running and we received a native window, inform the sink about it */
	gst_video_overlay_set_window_handle (GST_VIDEO_OVERLAY (data->video_sink), (guintptr)data->native_window);

	(*env)->CallVoidMethod (env, data->app, on_gstreamer_initialized_method_id);
	if ((*env)->ExceptionCheck (env)) {
		GST_ERROR ("Failed to call Java method");
		(*env)->ExceptionClear (env);
	}
	data->initialized = TRUE;
	}
}

/* Main method for the native code. This is executed on its own thread. */
static void *app_function (void *userdata) {

	JavaVMAttachArgs args;
	GstBus *bus;
    CustomData *data = (CustomData *)userdata;
	GSource *bus_source;
	GError *error = NULL;

	//GST_DEBUG ("Creating pipeline in CustomData at %p", data);


	/* Build pipeline */
	// data->pipeline = gst_parse_launch("videotestsrc ! warptv ! videoconvert ! autovideosink", &error);

	// construct pipeline elements
	data->nicesrc = gst_element_factory_make ("nicesrc", "nicesrc");
	data->rtph264depay = gst_element_factory_make ("rtph264depay", "rtph264depay");
	data->avdec_h264 = gst_element_factory_make ("avdec_h264", "avdec_h264");
	data->videoconvert = gst_element_factory_make ("videoconvert", "videoconvert");
	data->queue = gst_element_factory_make ("queue", "queue");
	data->autovideosink = gst_element_factory_make ("fpsdisplaysink", "autovideosink");
	data->rtpjitterbuffer = gst_element_factory_make ("rtpjitterbuffer", "rtpjitterbuffer");
	//GstElement *videotestsrc = gst_element_factory_make ("videotestsrc", "videotestsrc");

    PBPRINTF("nicesrc %p\n",nicesrc);
    PBPRINTF("rtph264depay %p\n",rtph264depay);
    PBPRINTF("avdec_h264 %p\n",avdec_h264);
    PBPRINTF("videoconvert %p\n",videoconvert);
	PBPRINTF("autovideosink %p\n",autovideosink);
	PBPRINTF("queue %p\n",queue);
    //PBPRINTF("videotestsrc %p\n",videotestsrc);


	g_object_set (data->autovideosink, "text-overlay", FALSE, NULL);
	g_object_set (data->nicesrc, "agent", gst_agent, NULL);
	g_object_set (data->nicesrc, "stream", gst_stream_id, NULL);
	g_object_set (data->nicesrc, "component", 1, NULL);
	g_object_set (data->queue, "leaky", 2, "max-size-buffers",20, NULL);
	GstCaps *nicesrc_caps = gst_caps_from_string("application/x-rtp, media=(string)video, clock-rate=(int)90000, encoding-name=(string)H264, payload=96");

	data->pipeline = gst_pipeline_new ("receive-pipeline");

	gst_bin_add_many (GST_BIN (data->pipeline), data->nicesrc, data->rtph264depay, data->avdec_h264, data->videoconvert, data->queue, data->rtpjitterbuffer, data->autovideosink,  NULL);
	//gst_bin_add_many (GST_BIN (data->pipeline), videotestsrc, videoconvert, autovideosink,  NULL);
	if (!gst_element_link_filtered( data->nicesrc, data->rtpjitterbuffer, nicesrc_caps)) {
		GST_ERROR ("Failed to link 1");
		return NULL;
	}
	if (!gst_element_link_many(data->rtpjitterbuffer, data->rtph264depay,data->avdec_h264,data->videoconvert,data->queue,data->autovideosink,NULL)) {
		GST_ERROR ("Failed to link 2");
		return NULL;
	}

	//g_object_set( G_OBJECT(nicesrc), "port", udp_port,NULL);
	g_object_set( G_OBJECT(data->autovideosink), "sync", FALSE, NULL);


  //data->pipeline = gst_parse_launch("videotestsrc ! videoconvert ! autovideosink", &error);

	/* Set the pipeline to READY, so it can already accept a window handle, if we have one */
	if (gst_element_set_state(data->pipeline, GST_STATE_PLAYING)==GST_STATE_CHANGE_FAILURE) {
        PBPRINTF("FAILED TO CHANGE STATE!!!\n");
	}

	data->video_sink = gst_bin_get_by_interface(GST_BIN(data->pipeline), GST_TYPE_VIDEO_OVERLAY);
	if (!data->video_sink) {
		GST_ERROR ("Could not retrieve video sink");
		return NULL;
	}

	/* Instruct the bus to emit signals for each received message, and connect to the interesting signals */
	bus = gst_element_get_bus (data->pipeline);
	bus_source = gst_bus_create_watch (bus);
	g_source_set_callback (bus_source, (GSourceFunc) gst_bus_async_signal_func, NULL, NULL);
	//g_source_attach (bus_source, data->context);
	g_source_attach (bus_source, NULL);
	g_source_unref (bus_source);
	g_signal_connect (G_OBJECT (bus), "message::error", (GCallback)error_cb, data);
	g_signal_connect (G_OBJECT (bus), "message::state-changed", (GCallback)state_changed_cb, data);
	gst_object_unref (bus);


	//return NULL;
	/* Create a GLib Main Loop and set it to run */
	GST_DEBUG ("Entering main loop... (CustomData:%p)", data);
	//data->main_loop = g_main_loop_new (data->context, FALSE);
	data->main_loop = g_main_loop_new (NULL, FALSE);
	check_initialization_complete (data);
	g_main_loop_run (data->main_loop);

	return NULL;
	/*GST_DEBUG ("Exited main loop");
	g_main_loop_unref (data->main_loop);
	data->main_loop = NULL;

	g_main_context_pop_thread_default(data->context);
	g_main_context_unref (data->context);
	gst_element_set_state (data->pipeline, GST_STATE_NULL);
	gst_object_unref (data->video_sink);
	gst_object_unref (data->pipeline);

	return NULL;*/
}

/*
 * Java Bindings
 */

/* Instruct the native code to create its internal data structure, pipeline and thread */

static void gst_play_with_agent(JNIEnv* env, jobject thiz, jlong jagent, jint jstream_id) {
    gst_agent = (NiceAgent*)jagent;
    gst_stream_id = jstream_id;
    PBPRINTF("PLAYWITH AGENT %p %d\n",gst_agent,gst_stream_id);
	pthread_create (&gst_app_thread, NULL, &app_function, GET_CUSTOM_DATA (env, thiz, custom_data_field_id));
}

static void gst_native_init (JNIEnv* env, jobject thiz) {

	//start_logger("petbot");


	CustomData *data = g_new0 (CustomData, 1);
	SET_CUSTOM_DATA (env, thiz, custom_data_field_id, data);
	GST_DEBUG_CATEGORY_INIT (debug_category, "tutorial-3", 0, "Android tutorial 3");
	gst_debug_set_threshold_for_name("tutorial-3", GST_LEVEL_DEBUG);
	GST_DEBUG ("Created CustomData at %p", data);
	data->app = (*env)->NewGlobalRef (env, thiz);
	GST_DEBUG ("Created GlobalRef for app object at %p", data->app);


	data->nicesrc=NULL;
	data->rtph264depay=NULL;
	data->avdec_h264=NULL;
	data->videoconvert=NULL;
	data->queue=NULL;
	data->autovideosink=NULL;
	data->rtpjitterbuffer=NULL;

	data->jitter_stats=(guint64*)malloc(sizeof(guint64)*4);
	if (data->jitter_stats==NULL) {
		PBPRINTF("ERROR MALLOC!");
	}

	//pthread_create (&gst_app_thread, NULL, &app_function, data);
}

/* Quit the main loop, remove the native thread and free resources */
static void gst_native_finalize (JNIEnv* env, jobject thiz) {
	CustomData *data = GET_CUSTOM_DATA (env, thiz, custom_data_field_id);
	if (!data) return;
	GST_DEBUG ("Quitting main loop...");
	g_main_loop_quit (data->main_loop);
	g_main_loop_unref (data->main_loop);
	if (gst_agent!=NULL) {
		gst_element_set_state (data->pipeline, GST_STATE_NULL);
		gst_object_unref (data->pipeline);
		g_object_unref(gst_agent);
		gst_agent=NULL;
	}
	GST_DEBUG ("Waiting for thread to finish...");
	pthread_join (gst_app_thread, NULL);
	GST_DEBUG ("Deleting GlobalRef for app object at %p", data->app);
	(*env)->DeleteGlobalRef (env, data->app);
	GST_DEBUG ("Freeing CustomData at %p", data);
	g_free (data);
	SET_CUSTOM_DATA (env, thiz, custom_data_field_id, NULL);
	GST_DEBUG ("Done finalizing");
}


static jintArray get_jitter_stats (JNIEnv* env, jobject thiz) {
	CustomData *data = GET_CUSTOM_DATA (env, thiz, custom_data_field_id);
	if (data->rtpjitterbuffer==NULL || data->autovideosink==NULL) {
		return NULL;
	}
	GstStructure * stats;
	g_object_get (data->rtpjitterbuffer, "stats", &stats, NULL);
	gst_structure_get_uint64 (stats, "num-pushed", data->jitter_stats);
	gst_structure_get_uint64 (stats, "num-lost", data->jitter_stats+1);
	gst_structure_get_uint64 (stats, "num-late", data->jitter_stats+2);

	g_object_get(data->autovideosink,"frames-rendered",data->jitter_stats+3,NULL);


	int size = 4;
	jintArray result;
	result = (*env)->NewIntArray(env, size);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}
	// fill a temp structure to use to populate the java int array
	jint fill[size];
	int i;
	for (i = 0; i < size; i++) {
		fill[i] = data->jitter_stats[i]; // put whatever logic you want to populate the values here.
	}
	// move from the temp structure to the java structure
	(*env)->SetIntArrayRegion(env, result, 0, size, fill);
	return result;
}

/* Set pipeline to PLAYING state */
static void gst_native_play (JNIEnv* env, jobject thiz) {
	CustomData *data = GET_CUSTOM_DATA (env, thiz, custom_data_field_id);
	if (!data) return;
	GST_DEBUG ("Setting state to PLAYING");
	gst_element_set_state (data->pipeline, GST_STATE_PLAYING);
}

/* Set pipeline to PAUSED state */
static void gst_native_pause (JNIEnv* env, jobject thiz) {
	CustomData *data = GET_CUSTOM_DATA (env, thiz, custom_data_field_id);
	if (!data) return;
	GST_DEBUG ("Setting state to PAUSED");
	gst_element_set_state (data->pipeline, GST_STATE_PAUSED);
}

/* Static class initializer: retrieve method and field IDs */
static jboolean gst_native_class_init (JNIEnv* env, jclass klass) {
	custom_data_field_id = (*env)->GetFieldID (env, klass, "native_custom_data", "J");
	set_message_method_id = (*env)->GetMethodID (env, klass, "setMessage", "(Ljava/lang/String;)V");
	on_gstreamer_initialized_method_id = (*env)->GetMethodID (env, klass, "onGStreamerInitialized", "()V");

	if (!custom_data_field_id || !set_message_method_id || !on_gstreamer_initialized_method_id) {
	/* We emit this message through the Android log instead of the GStreamer log because the later
	 * has not been initialized yet.
	 */
	__android_log_print (ANDROID_LOG_ERROR, "tutorial-3", "The calling class does not implement all necessary interface methods");
	return JNI_FALSE;
	}
	return JNI_TRUE;
}

static void gst_native_surface_init (JNIEnv *env, jobject thiz, jobject surface) {
	CustomData *data = GET_CUSTOM_DATA (env, thiz, custom_data_field_id);
	if (!data) return;
	ANativeWindow *new_native_window = ANativeWindow_fromSurface(env, surface);
	GST_DEBUG ("Received surface %p (native window %p)", surface, new_native_window);

	if (data->native_window) {
	ANativeWindow_release (data->native_window);
	if (data->native_window == new_native_window) {
		GST_DEBUG ("New native window is the same as the previous one %p", data->native_window);
		if (data->video_sink) {
		gst_video_overlay_expose(GST_VIDEO_OVERLAY (data->video_sink));
		gst_video_overlay_expose(GST_VIDEO_OVERLAY (data->video_sink));
		}
		return;
	} else {
		GST_DEBUG ("Released previous native window %p", data->native_window);
		data->initialized = FALSE;
	}
	}
	data->native_window = new_native_window;

	check_initialization_complete (data);
}

static void gst_native_surface_finalize (JNIEnv *env, jobject thiz) {
	CustomData *data = GET_CUSTOM_DATA (env, thiz, custom_data_field_id);
	if (!data) return;
	GST_DEBUG ("Releasing Native Window %p", data->native_window);

	if (data->video_sink) {
		gst_video_overlay_set_window_handle (GST_VIDEO_OVERLAY (data->video_sink), (guintptr)NULL);
		gst_element_set_state (data->pipeline, GST_STATE_PAUSED); //MISKO
	}

	ANativeWindow_release (data->native_window);
	data->native_window = NULL;
	data->initialized = FALSE;
}

/* List of implemented native methods */
static JNINativeMethod native_methods[] = {
		{ "jitterStats", "()[I", (void *) get_jitter_stats},
	{ "nativeInit", "()V", (void *) gst_native_init},
	{ "nativePlayAgent", "(JI)V", (void *) gst_play_with_agent},
	{ "nativeFinalize", "()V", (void *) gst_native_finalize},
	{ "nativePlay", "()V", (void *) gst_native_play},
	{ "nativePause", "()V", (void *) gst_native_pause},
	{ "nativeSurfaceInit", "(Ljava/lang/Object;)V", (void *) gst_native_surface_init},
	{ "nativeSurfaceFinalize", "()V", (void *) gst_native_surface_finalize},
	{ "nativeClassInit", "()Z", (void *) gst_native_class_init}
};

/* Library initializer */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
	JNIEnv *env = NULL;

	java_vm = vm;

	if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
	__android_log_print (ANDROID_LOG_ERROR, "tutorial-3", "Could not retrieve JNIEnv");
	return 0;
	}
	jclass klass = (*env)->FindClass (env, "com/atos/petbot/PetBot");
	(*env)->RegisterNatives (env, klass, native_methods, G_N_ELEMENTS(native_methods));

	pthread_key_create (&current_jni_env, detach_current_thread);

	return JNI_VERSION_1_4;
}
