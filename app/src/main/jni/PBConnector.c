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
/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/hellojni/HelloJni.java
 */

  void Java_com_petbot_PBConnector_connectToServerWithKey(JNIEnv* env,jobject thiz, jstring hostname, int portno, jstring key ) {

        const char* hostname_str = (*env)->GetStringUTFChars(env,hostname,0);
        const char* key_str = (*env)->GetStringUTFChars(env,key,0);


        PBPRINTF("HOSTNAME %s with %d key %s\n",hostname_str,portno,key_str);
        PBPRINTF("AGENT %p\n",agent);
       jclass PBConnectorClass = (*env)->GetObjectClass(env,thiz);
       jfieldID pbs_field = (*env)->GetFieldID(env,PBConnectorClass, "ptr_pbs", "J");

	   SSL_CTX* ctx;
	   OpenSSL_add_ssl_algorithms();
	   SSL_load_error_strings();
	   ctx = SSL_CTX_new (SSLv23_client_method());
	   CHK_NULL(ctx);

       pbsock * pbs = connect_to_server_with_key(hostname_str, portno, ctx, key_str);
       PBPRINTF("WTF");
       //jlong ptr_pbs = env->GetLongField(obj, pbs_field);

       jlong l_pbs = (jlong) 12346;
       (*env)->SetLongField(env,thiz, pbs_field, l_pbs);

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
    PBPRINTF("WTF WTF WTF WTF WTF WTF\n");

    return (*env)->NewStringUTF(env, "Hello from JNI !  Compiled with ABI F " ABI ".");
}
