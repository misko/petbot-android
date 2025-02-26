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
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

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



void Java_com_atos_petbot_PBConnector_makeIceRequest(JNIEnv * env, jobject thiz) {

        jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
        jfieldID ice_to_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_to_child", "J");
        jfieldID ice_from_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_from_child", "J");


       int * ice_thread_pipes_to_child = (int*)( (*env)->GetLongField(env,thiz, ice_to_child_field ));
       int * ice_thread_pipes_from_child = (int*)( (*env)->GetLongField(env,thiz, ice_from_child_field ));


        pbsock * pbs =  get_pbsock(env,thiz);


        pbmsg * ice_request_m = make_ice_request(ice_thread_pipes_from_child,ice_thread_pipes_to_child);
        fprintf(stderr,"make the ice request!\n");
        LOGD("OUR ICE %s\n",ice_request_m->pbmsg);
        send_pbmsg(pbs, ice_request_m);
}


void Java_com_atos_petbot_PBConnector_setStun(JNIEnv* env,jobject thiz, jstring server, jstring port, jstring username, jstring password ) {

    const char* jni_stun_server = (*env)->GetStringUTFChars(env,server,0);
    const char* jni_stun_port = (*env)->GetStringUTFChars(env,port,0);
    const char* jni_stun_username = (*env)->GetStringUTFChars(env,username,0);
    const char* jni_stun_password = (*env)->GetStringUTFChars(env,password,0);

    set_stun(jni_stun_server,jni_stun_port,jni_stun_username,jni_stun_password);
    return;

}

jstring Java_com_atos_petbot_PBConnector_getIcePair(JNIEnv * env, jobject thiz) {

    jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
    jfieldID pbnio_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_pbnio", "J");
    pb_nice_io * pbnio = (pb_nice_io*)( (*env)->GetLongField(env,thiz, pbnio_field ));

    if (pbnio->ice_pair!=NULL) {
        return (*env)->NewStringUTF(env, pbnio->ice_pair);
    }
    return (*env)->NewStringUTF(env, "");
}

jstring Java_com_atos_petbot_PBConnector_getError(JNIEnv * env, jobject thiz) {

    jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
    jfieldID pbnio_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_pbnio", "J");
    pb_nice_io * pbnio = (pb_nice_io*)( (*env)->GetLongField(env,thiz, pbnio_field ));
    if (pbnio!=NULL && pbnio->error!=NULL) {
        return (*env)->NewStringUTF(env, pbnio->error);
    }
    return (*env)->NewStringUTF(env, "");
}

void Java_com_atos_petbot_PBConnector_iceNegotiate(JNIEnv * env, jobject thiz, jobject jpbmsg) {

        jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
        jfieldID ice_to_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_to_child", "J");
        jfieldID ice_from_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_from_child", "J");
        jfieldID streamer_id_field = (*env)->GetFieldID(env,PBConnectorClass, "streamer_id", "I");
        jfieldID ptr_agent_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_agent", "J");
        jfieldID stream_id_field = (*env)->GetFieldID(env,PBConnectorClass, "stream_id", "I");
        jfieldID pbnio_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_pbnio", "J");

       int * ice_thread_pipes_to_child = (int*)( (*env)->GetLongField(env,thiz, ice_to_child_field ));
       int * ice_thread_pipes_from_child = (int*)( (*env)->GetLongField(env,thiz, ice_from_child_field ));
       pb_nice_io * pbnio = (pb_nice_io*)( (*env)->GetLongField(env,thiz, pbnio_field ));




       pbmsg * m = jpbmsg_to_pbmsg(env,jpbmsg);
        (*env)->SetIntField(env,thiz,streamer_id_field,m->pbmsg_from);
        recvd_ice_response(m,pbnio);

        //set the java variables right for calling after

       (*env)->SetLongField(env,thiz, ptr_agent_field , pbnio->agent);
       (*env)->SetIntField(env,thiz, stream_id_field , pbnio->stream_id);

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

void Java_com_atos_petbot_PBConnector_init(JNIEnv * env, jobject thiz) {
        GMainLoop * main_loop = g_main_loop_new (NULL, FALSE);

        jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
        jfieldID mainloop_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_mainloop", "J");
        (*env)->SetLongField(env,thiz, mainloop_field, main_loop);

        pthread_t gst_app_thread; // TODO KEEP TRACK OF THIS?
	    pthread_create (&gst_app_thread, NULL, &start_GMain, main_loop);
        pthread_detach(gst_app_thread);

    clear_stun_servers();
    add_stun_server("sfturn.petbot.com", "3478", "misko", "misko");
    add_stun_server("bangturn.petbot.com", "3478", "misko", "misko");
    add_stun_server("torturn.petbot.com", "3478", "misko", "misko");
    add_stun_server("frankturn.petbot.com", "3478", "misko", "misko");

    jfieldID ice_to_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_to_child", "J");
    jfieldID ice_from_child_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ice_thread_pipes_from_child", "J");

    jfieldID nice_mode_field = (*env)->GetFieldID(env,PBConnectorClass, "nice_mode", "I");
    int nice_mode = (*env)->GetIntField(env,thiz,nice_mode_field);

    int * ice_thread_pipes_to_child = (int*)malloc(sizeof(int)*2);
    assert(ice_thread_pipes_to_child!=NULL);
    int * ice_thread_pipes_from_child = (int*)malloc(sizeof(int)*2);
    assert(ice_thread_pipes_from_child!=NULL);

    (*env)->SetLongField(env,thiz, ice_to_child_field, ice_thread_pipes_to_child);
    (*env)->SetLongField(env,thiz, ice_from_child_field, ice_thread_pipes_from_child);

    //setup basic ICE
    int ret = pipe(ice_thread_pipes_to_child);
    LOGD("PIPES RET IS %d\n",ret);
    ret= pipe(ice_thread_pipes_from_child);
    LOGD("PIPES RET IS %d\n",ret);

    pb_nice_io * pbnio =  new_pbnio();
    pbnio->mode=nice_mode;
    pbnio->pipe_to_child=ice_thread_pipes_to_child[1];
    pbnio->pipe_to_parent=ice_thread_pipes_from_child[1];
    pbnio->pipe_from_parent=ice_thread_pipes_to_child[0];
    pbnio->pipe_from_child=ice_thread_pipes_from_child[0];
    pbnio->controlling=0;
    init_ice(pbnio);

    jfieldID pbnio_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_pbnio", "J");
    (*env)->SetLongField(env,thiz, pbnio_field, pbnio);

}


void Java_com_atos_petbot_PBConnector_nativeClose(JNIEnv* env,jobject thiz) {

        pbsock * pbs = get_pbsock(env,thiz);
    LOGW("CLOSING THE SOCKET %p %d\n",pbs,pbs->client_sock);
        free_pbsock(pbs);

        LOGW("CLOSING THE SOCKET %p %d\n",pbs,pbs->client_sock);

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
       jclass pbmsg_cls = (*env)->FindClass(env, "com/atos/petbot/PBMsg");
        //get the fields for the jpbmsg
       jfieldID pbmsg_len_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_len", "I");
       jfieldID pbmsg_type_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_type", "I");
       jfieldID pbmsg_from_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_from", "I");
       jfieldID pbmsg_msg_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg", "[B");
       int len = (*env)->GetIntField(env,jpbmsg,pbmsg_len_fid);
       int type = (*env)->GetIntField(env,jpbmsg,pbmsg_type_fid);
       int from = (*env)->GetIntField(env,jpbmsg,pbmsg_from_fid);

       //todo check for null termination?

       jbyteArray jary = (jbyteArray)((*env)->GetObjectField(env,jpbmsg, pbmsg_msg_fid));
        jbyte *b = (jbyte *)((*env)->GetByteArrayElements(env,jary, NULL));

       //TODO assert lengths are correct...
       int alen = (*env)->GetArrayLength( env, jary );
       assert(alen==len);
       int null_terminate = ((type & PBMSG_STRING) !=0) && b[len-1]!='\0';

       char * ray = (char*)malloc(sizeof(char)*(len+(null_terminate ? 1 : 0)));
       for (int i=0; i<len; i++) {
         ray[i]=b[i];
       }
       if (null_terminate) {
           ray[len]='\0';
       }

       pbmsg * m = new_pbmsg();
       m->pbmsg_len = len+(null_terminate ? 1 : 0);
       m->pbmsg_type = type;
       m->pbmsg_from = from;
       m->pbmsg = ray;
        return m;
}

void Java_com_atos_petbot_PBConnector_sendPBMsg(JNIEnv* env,jobject thiz, jobject jpbmsg) {
        pbsock * pbs = get_pbsock(env,thiz);
        //lets get the fields and such...
        pbmsg * m = jpbmsg_to_pbmsg(env,jpbmsg);
       send_pbmsg(pbs,m);
       free_pbmsg(m);

}

jobject Java_com_atos_petbot_PBConnector_readPBMsg(JNIEnv* env,jobject thiz) {
    jclass pbmsg_cls = (*env)->FindClass(env, "com/atos/petbot/PBMsg");
    jmethodID constructor = (*env)->GetMethodID(env, pbmsg_cls,"<init>","()V"); //call the basic constructor
    jobject jpbmsg = (*env)->NewObject(env, pbmsg_cls, constructor);
    //ok so now allocated a Java object... lets actually get a pbmsg..
        pbsock * pbs = get_pbsock(env,thiz); //recover the socket

        //get the fields for the jpbmsg
       jfieldID pbmsg_len_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_len", "I");
       jfieldID pbmsg_type_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_type", "I");
       jfieldID pbmsg_from_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg_from", "I");
       jfieldID pbmsg_msg_fid = (*env)->GetFieldID(env,pbmsg_cls, "pbmsg", "[B");


    //get a msg
        LOGW("LISTENING FOR MESSAGE ON PBSOCKET %p %d",pbs,pbs!=NULL ? pbs->client_sock : -1);
       pbmsg * m = recv_pbmsg(pbs);
        if (m==NULL) {
            LOGW("LISTENING FOR MESSAGE ON PBSOCKET %p NULL",pbs);
            return NULL;
        }
    LOGW("LISTENING FOR MESSAGE ON PBSOCKET %p - NOT NULL",pbs);

        //make sure we convert strings correctly
        if ((m->pbmsg_type & PBMSG_STRING ) !=0 ) {
            m->pbmsg_len=strlen(m->pbmsg);
        }
       //ok now copy it into the structure...
       (*env)->SetIntField(env,jpbmsg, pbmsg_len_fid, m->pbmsg_len);
       (*env)->SetIntField(env,jpbmsg, pbmsg_type_fid, m->pbmsg_type);
       (*env)->SetIntField(env,jpbmsg, pbmsg_from_fid, m->pbmsg_from);

       jbyteArray d = (*env)->NewByteArray(env, m->pbmsg_len);
       char * ray = (char*)malloc(sizeof(char)*m->pbmsg_len);
       for (int i=0; i<m->pbmsg_len; i++) {
         ray[i]=m->pbmsg[i];
       }
       //PBPRINTF("GOT MESSAGE %d %d %d %d\n",m->pbmsg_len,m->pbmsg_type,m->pbmsg_from,m->pbmsg_type&PBMSG_STRING);
       (*env)->SetByteArrayRegion(env, d, 0, m->pbmsg_len, ray);
       (*env)->SetObjectField(env,jpbmsg, pbmsg_msg_fid, d);
        //TODO MEMORY LEAK? should release malloced?
       free_pbmsg(m);
    return jpbmsg;
}

  void Java_com_atos_petbot_PBConnector_connectToServerWithKey(JNIEnv* env,jobject thiz, jstring hostname, int portno, jstring key ) {

        const char* hostname_str = (*env)->GetStringUTFChars(env,hostname,0);
      LOGD("HOSTNAMExx2 %s with %d \n",hostname_str,portno);
        const char* key_str = (*env)->GetStringUTFChars(env,key,0);


      LOGD("HOSTNAME %s with %d key %s\n",hostname_str,portno,key_str);
       jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
       jfieldID pbs_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_pbs", "J");
       jfieldID ctx_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_ctx", "J");
      LOGD("HOSTNAME %s with %d key %s\n",hostname_str,portno,key_str);

	   SSL_CTX* ctx;
	   OpenSSL_add_ssl_algorithms();
	   SSL_load_error_strings();
	   ctx = SSL_CTX_new (SSLv23_client_method());
	   CHK_NULL(ctx);
      LOGD("HOSTNAME %s with %d key %s\n",hostname_str,portno,key_str);

       pbsock * pbs = connect_to_server_with_key(hostname_str, portno, ctx, key_str);
      LOGD("HOSTNAME x4 %s with %d key %s\n",hostname_str,portno,key_str);
       //jlong ptr_pbs = env->GetLongField(obj, pbs_field);

       jlong l_pbs = (jlong) pbs;
       (*env)->SetLongField(env,thiz, pbs_field, l_pbs);

      LOGD("HOSTNAME x3 %s with %d key %s\n",hostname_str,portno,key_str);
       jlong l_ctx = (jlong) ctx;
       (*env)->SetLongField(env,thiz, ctx_field, l_ctx);

      LOGD("HOSTNAME x %s with %d key %s\n",hostname_str,portno,key_str);
        (*env)->ReleaseStringUTFChars(env,hostname,hostname_str);
        (*env)->ReleaseStringUTFChars(env,key,key_str);
      LOGD("HOSTNAME x2 %s with %d key %s\n",hostname_str,portno,key_str);
  }

  jbyteArray Java_com_atos_petbot_PBConnector_newByteArray(JNIEnv* env,jobject thiz ) {
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
Java_com_atos_petbot_PBConnector_stringFromJNI( JNIEnv* env,
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

    return (*env)->NewStringUTF(env, "Hello from JNI !  Compiled with ABI F " ABI ".");
}
