//
// Created by Misko Dzamba on 16-08-28.
//

#ifndef PETBOT_ANDROID_PBCONNECTOR_H
#define PETBOT_ANDROID_PBCONNECTOR_H

void Java_com_atos_petbot_PBConnector_nativeClose(JNIEnv * env, jobject thiz);
void Java_com_atos_petbot_PBConnector_iceRequest(JNIEnv * env, jobject thiz);
void Java_com_atos_petbot_PBConnector_iceNegotiate(JNIEnv * env, jobject thiz, jobject jpbmsg);
void Java_com_atos_petbot_PBConnector_startNiceThread(JNIEnv * env, jobject thiz, jint s);
void Java_com_atos_petbot_PBConnector_sendPBMsg(JNIEnv* env,jobject thiz);
jobject Java_com_atos_petbot_PBConnector_readPBMsg(JNIEnv* env,jobject thiz);
void Java_com_atos_petbot_PBConnector_sendPBMsg(JNIEnv* env,jobject thiz, jobject jpbmsg);
void Java_com_atos_petbot_PBConnector_setStun(JNIEnv* env,jobject thiz, jstring server, jstring port, jstring username, jstring password );
JNIEXPORT jstring JNICALL
Java_com_atos_petbot_PBConnector_stringFromJNI( JNIEnv* env,
                                                  jobject thiz );
JNIEXPORT jstring JNICALL Java_com_atos_petbot_PBConnector_init(JNIEnv * env, jobject thiz);
JNIEXPORT jbyteArray JNICALL Java_com_atos_petbot_PBConnector_newByteArray(JNIEnv* env,jobject thiz );
#endif //PETBOT_ANDROID_PBCONNECTOR_H
