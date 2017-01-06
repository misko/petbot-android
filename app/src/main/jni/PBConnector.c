/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <android/log.h>
#include <jni.h>
#include "pb.h"
#include "nice_utils.h"
#include "tcp_utils.h"
#include <assert.h>

#define  LOG_TAG    "Petbot"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

//static jclass pbmsg_cls;

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/hellojni/HelloJni.java
 */
/*
 JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    PBPRINTF("WTF WTF WTF LOAD LOAD LOAD LOAD LOAD\n");
    JNIEnv *env;
    //(*jvm)->GetEnv(jvm, (void**)&env, JNI_VERSION_1_4);
    //pbmsg_cls = (*env)->FindClass(env, "com/petbot$PBMsg");

        if ((*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
            return JNI_ERR;
        } else {
            jclass localSimpleCls = (*env)->FindClass(env,"com/petbot/PBMsg");
            if ((*env)->ExceptionCheck(env)) {
               return -1;
            }

            if (localSimpleCls == NULL) {
                return JNI_ERR;
            }
            pbmsg_cls = (jclass) (*env)->NewGlobalRef(env, localSimpleCls);
        }
        return JNI_VERSION_1_6;
 }*/


pbmsg * jpbmsg_to_pbmsg(JNIEnv* env,jobject jpbmsg);



pbsock * get_pbsock(JNIEnv* env,jobject thiz) {
       jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
       jfieldID pbs_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_pbs", "J");
       return (pbsock*)((*env)->GetLongField(env,thiz, pbs_field));
}



void Java_com_petbot_PBConnector_iceRequest(JNIEnv * env, jobject thiz) {

        jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
        jfieldID ice_to_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_to_child", "J");
        jfieldID ice_from_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_from_child", "J");

       int * ice_thread_pipes_to_child = (int*)( (*env)->GetLongField(env,thiz, ice_to_child_field ));
       int * ice_thread_pipes_from_child = (int*)( (*env)->GetLongField(env,thiz, ice_from_child_field ));


    LOGD("POINTERS FOR ICE PIPES %p %p\n",ice_thread_pipes_to_child,ice_thread_pipes_from_child);

       pbsock * pbs =  get_pbsock(env,thiz);


        pbmsg * ice_request_m = make_ice_request(ice_thread_pipes_from_child,ice_thread_pipes_to_child);
        fprintf(stderr,"make the ice request!\n");
        send_pbmsg(pbs, ice_request_m);
}


void Java_com_petbot_PBConnector_iceNegotiate(JNIEnv * env, jobject thiz, jobject jpbmsg) {

        jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
        jfieldID ice_to_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_to_child", "J");
        jfieldID ice_from_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_from_child", "J");
        jfieldID streamer_id_field = (*env)->GetFieldID(env,PBConnectorClass, "streamer_id", "I");
        jfieldID ptr_agent_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_agent", "J");
        jfieldID stream_id_field = (*env)->GetFieldID(env,PBConnectorClass, "stream_id", "I");

       int * ice_thread_pipes_to_child = (int*)( (*env)->GetLongField(env,thiz, ice_to_child_field ));
       int * ice_thread_pipes_from_child = (int*)( (*env)->GetLongField(env,thiz, ice_from_child_field ));




       pbmsg * m = jpbmsg_to_pbmsg(env,jpbmsg);
    LOGD("WTF!!!\n");

        (*env)->SetIntField(env,thiz,streamer_id_field,m->pbmsg_from);
    LOGD("WTF!!!\n");
        //int bb_streamer_id = m->pbmsg_from; //TODO save this in java object?
        //PBPRINTF("BBSTREAMER ID %d\n",bb_streamer_id);
        recvd_ice_response(m,ice_thread_pipes_from_child,ice_thread_pipes_to_child);
    LOGD("WTF!!!\n");

        //set the java variables right for calling after

       (*env)->SetLongField(env,thiz, ptr_agent_field , agent);
       (*env)->SetIntField(env,thiz, stream_id_field , stream_id);
    LOGD("PASSING TO JAVA AGENT %p and STREAM_ID %d\n",agent,stream_id);

       /*    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
               [gst_backend app_function];
           });*/
}


static void * start_GMain(void * x) {
    LOGD("PBCONNECTOR START GMAIN\n");
         GMainLoop * main_loop = ( GMainLoop * )x;
         g_main_loop_run (main_loop);
    LOGD("PBCONNECTOR END GMAIN\n");
        //TODO cleanup? Thread join?
}

void Java_com_petbot_PBConnector_initGlib(JNIEnv * env, jobject thiz) {
        GMainLoop * main_loop = g_main_loop_new (NULL, FALSE);

        jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
        jfieldID mainloop_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_mainloop", "J");
        (*env)->SetLongField(env,thiz, mainloop_field, main_loop);

        pthread_t gst_app_thread; // TODO KEEP TRACK OF THIS?
	    pthread_create (&gst_app_thread, NULL, &start_GMain, main_loop);
        pthread_detach(gst_app_thread);
}


/*void Java_com_petbot_PBConnector_startGThread(JNIEnv * env, jobject thiz) {

        jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
        jfieldID mainloop_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_mainloop", "J");

        GMainLoop * main_loop = (GMainContext*)((*env)->GetLongField(env,thiz, mainloop_field ));

        PBPRINTF("PBCONNECTOR ENTER MAIN LOOP CONTEXT\n",);
        g_main_loop_run (main_loop);
        PBPRINTF("PBCONNECTOR EXIT MAIN LOOP\n");
}*/

void Java_com_petbot_PBConnector_startNiceThread(JNIEnv * env, jobject thiz, jint s) {

        jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
        jfieldID ice_to_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_to_child", "J");
        jfieldID ice_from_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_from_child", "J");

        int * ice_thread_pipes_to_child = (int*)malloc(sizeof(int)*2);
        int * ice_thread_pipes_from_child = (int*)malloc(sizeof(int)*2);
        if (ice_thread_pipes_to_child==NULL || ice_thread_pipes_from_child==NULL) {
            LOGD("Failed to allocate memory for pipes\n");
        }

        int ret = pipe(ice_thread_pipes_to_child);
        if (ret!=0) {
            LOGD("ERROR MAKING PIPE!\n");
        }
        ret = pipe(ice_thread_pipes_from_child);
        if (ret!=0) {
            LOGD("ERROR MAKING PIPE!\n");
        }

        (*env)->SetLongField(env,thiz, ice_to_child_field, ice_thread_pipes_to_child);
        (*env)->SetLongField(env,thiz, ice_from_child_field, ice_thread_pipes_from_child);
    LOGD("POINTERS FOR ICE PIPES %p %p\n",ice_thread_pipes_to_child,ice_thread_pipes_from_child);

        start_nice_thread(0,ice_thread_pipes_from_child,ice_thread_pipes_to_child);
}

void Java_com_petbot_PBConnector_nativeClose(JNIEnv* env,jobject thiz) {

        pbsock * pbs = get_pbsock(env,thiz);
        free_pbsock(pbs);

        jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
        jfieldID pbs_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_pbs", "J");
        jfieldID ctx_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ctx", "J");
        jfieldID mainloop_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_mainloop", "J");

        (*env)->SetLongField(env,thiz, pbs_field, 0);
        (*env)->SetLongField(env,thiz, ctx_field, 0);
        //QUIT MAIN GLOOP?
        GMainLoop * mainloop = (GMainLoop*)((*env)->GetLongField(env,thiz, mainloop_field));
        g_main_loop_quit (mainloop);
}

pbmsg * jpbmsg_to_pbmsg(JNIEnv* env,jobject jpbmsg) {
       jclass pbmsg_cls = (*env)->FindClass(env, "com/petbot/PBMsg");
        //get the fields for the jpbmsg
       jfieldID pbmsg_len_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_len", "I");
       jfieldID pbmsg_type_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_type", "I");
       jfieldID pbmsg_from_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_from", "I");
       jfieldID pbmsg_msg_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg", "[B");
       int len = (*env)->GetIntField(env,jpbmsg,pbmsg_len_fid);
       int type = (*env)->GetIntField(env,jpbmsg,pbmsg_type_fid);
       int from = (*env)->GetIntField(env,jpbmsg,pbmsg_from_fid);

       jbyteArray jary = (jbyteArray)((*env)->GetObjectField(env,jpbmsg, pbmsg_msg_fid));
        jbyte *b = (jbyte *)((*env)->GetByteArrayElements(env,jary, NULL));

       //TODO assert lengths are correct...
       int alen = (*env)->GetArrayLength( env, jary );
       assert(alen==len);
       char * ray = (char*)malloc(sizeof(char)*len);
       for (int i=0; i<len; i++) {
         ray[i]=b[i];
       }

       pbmsg * m = new_pbmsg();
       m->pbmsg_len = len;
       m->pbmsg_type = type;
       m->pbmsg_from = from;
       m->pbmsg = ray;
        return m;
}

void Java_com_petbot_PBConnector_sendPBMsg(JNIEnv* env,jobject thiz, jobject jpbmsg) {
        pbsock * pbs = get_pbsock(env,thiz);
        //lets get the fields and such...
        pbmsg * m = jpbmsg_to_pbmsg(env,jpbmsg);
       send_pbmsg(pbs,m);
       free_pbmsg(m);

}

jobject Java_com_petbot_PBConnector_readPBMsg(JNIEnv* env,jobject thiz) {
    LOGD("IN PBMSG\n");
    jclass pbmsg_cls = (*env)->FindClass(env, "com/petbot/PBMsg");
    jmethodID constructor = (*env)->GetMethodID(env, pbmsg_cls,"<init>","()V"); //call the basic constructor
    jobject jpbmsg = (*env)->NewObject(env, pbmsg_cls, constructor);
    LOGD("IN PBMSG 2\n");

    //ok so now allocated a Java object... lets actually get a pbmsg..
        pbsock * pbs = get_pbsock(env,thiz); //recover the socket

    LOGD("IN PBMSG 3\n");
        //get the fields for the jpbmsg
       jfieldID pbmsg_len_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_len", "I");
       jfieldID pbmsg_type_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_type", "I");
       jfieldID pbmsg_from_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_from", "I");
       jfieldID pbmsg_msg_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg", "[B");

    LOGD("IN PBMSG 3\n");

    LOGD("WAITING ON MSG\n");

    //get a msg
       pbmsg * m = recv_pbmsg(pbs);
        if (m==NULL) {
            LOGD("IN PBMSG 5\n");
            return NULL;
        }
    LOGD("GOT MSG\n");

    LOGD("IN PBMSG 4\n");
       //ok now copy it into the structure...
       (*env)->SetIntField(env,jpbmsg, pbmsg_len_fid, m->pbmsg_len);
       (*env)->SetIntField(env,jpbmsg, pbmsg_type_fid, m->pbmsg_type);
       (*env)->SetIntField(env,jpbmsg, pbmsg_from_fid, m->pbmsg_from);


    LOGD("IN PBMSG 6\n");
       jbyteArray d = (*env)->NewByteArray(env, m->pbmsg_len);
       char * ray = (char*)malloc(sizeof(char)*m->pbmsg_len);
       for (int i=0; i<m->pbmsg_len; i++) {
         ray[i]=m->pbmsg[i];
       }
       PBPRINTF("GOT MESSAGE %d %d %d %d\n",m->pbmsg_len,m->pbmsg_type,m->pbmsg_from,m->pbmsg_type&PBMSG_STRING);
       (*env)->SetByteArrayRegion(env, d, 0, m->pbmsg_len, ray);
       (*env)->SetObjectField(env,jpbmsg, pbmsg_msg_fid, d);
        //TODO MEMORY LEAK? should release malloced?
       free_pbmsg(m);
    return jpbmsg;
}

  void Java_com_petbot_PBConnector_connectToServerWithKey(JNIEnv* env,jobject thiz, jstring hostname, int portno, jstring key ) {

        const char* hostname_str = (*env)->GetStringUTFChars(env,hostname,0);
        const char* key_str = (*env)->GetStringUTFChars(env,key,0);


      LOGD("HOSTNAME %s with %d key %s\n",hostname_str,portno,key_str);
      LOGD("AGENT %p\n",agent);
       jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
       jfieldID pbs_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_pbs", "J");
       jfieldID ctx_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ctx", "J");

	   SSL_CTX* ctx;
	   OpenSSL_add_ssl_algorithms();
	   SSL_load_error_strings();
	   ctx = SSL_CTX_new (SSLv23_client_method());
	   CHK_NULL(ctx);

       pbsock * pbs = connect_to_server_with_key(hostname_str, portno, ctx, key_str);
       //jlong ptr_pbs = env->GetLongField(obj, pbs_field);

       jlong l_pbs = (jlong) pbs;
       (*env)->SetLongField(env,thiz, pbs_field, l_pbs);

       jlong l_ctx = (jlong) ctx;
       (*env)->SetLongField(env,thiz, ctx_field, l_ctx);

        (*env)->ReleaseStringUTFChars(env,hostname,hostname_str);
        (*env)->ReleaseStringUTFChars(env,key,key_str);
  }

  jbyteArray Java_com_petbot_PBConnector_newByteArray(JNIEnv* env,jobject thiz ) {
    int size = 12;
    char * ray = (char*)malloc(sizeof(char)*size);
    jbyteArray jArray = (*env)->NewByteArray(env, size);
    if (jArray != NULL) {
        int i=0;
        for (i = 0; i < size; i++) {
           ray[i] = i;
        }
       (*env)->SetByteArrayRegion(env, jArray, 0, size, ray);
     }

       /* Get a reference to jctf object's class */
       jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);

       //jclass PBConnectorClass = (*env)->FindClass(env,"com/petbot/PBConnector");
       /* Get the Field ID of the instance variables "jni_result" */
       jfieldID pbs_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_pbs", "J");

       /* Change the variable "jni_result" */
       jlong pbs = (jlong) 12345;
       (*env)->SetLongField(env,thiz, pbs_field, pbs);
     return jArray;
 }

JNIEXPORT jstring JNICALL
Java_com_petbot_PBConnector_stringFromJNI( JNIEnv* env,
                                                  jobject thiz )
{
#if defined(__arm__)
  #if defined(__ARM_ARCH_7A__)
    #if defined(__ARM_NEON__)
      #if defined(__ARM_PCS_VFP)
        #define ABI "armeabi-v7a/NEON (hard-float)"
      #else
        #define ABI "armeabi-v7a/NEON"
      #endif
    #else
      #if defined(__ARM_PCS_VFP)
        #define ABI "armeabi-v7a (hard-float)"
      #else
        #define ABI "armeabi-v7a"
      #endif
    #endif
  #else
   #define ABI "armeabi"
  #endif
#elif defined(__i386__)
   #define ABI "x86"
#elif defined(__x86_64__)
   #define ABI "x86_64"
#elif defined(__mips64)  /* mips64el-* toolchain defines __mips__ too */
   #define ABI "mips64"
#elif defined(__mips__)
   #define ABI "mips"
#elif defined(__aarch64__)
   #define ABI "arm64-v8a"
#else
   #define ABI "unknown"
#endif
    LOGD("WTF WTF WTF WTF WTF WTF\n");

    return (*env)->NewStringUTF(env, "Hello from JNI !  Compiled with ABI F " ABI ".");
}
