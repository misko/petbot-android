//
// Created by Misko Dzamba on 16-08-28.
//

#ifndef PETBOT_ANDROID_PBCONNECTOR_H
#define PETBOT_ANDROID_PBCONNECTOR_H

void Java_com_petbot_PBConnector_iceRequest(JNIEnv * env, jobject thiz);
void Java_com_petbot_PBConnector_iceNegotiate(JNIEnv * env, jobject thiz, jobject jpbmsg);
void Java_com_petbot_PBConnector_startNiceThread(JNIEnv * env, jobject thiz, jint s);
void Java_com_petbot_PBConnector_sendPBMsg(JNIEnv* env,jobject thiz);
jobject Java_com_petbot_PBConnector_readPBMsg(JNIEnv* env,jobject thiz);
void Java_com_petbot_PBConnector_sendPBMsg(JNIEnv* env,jobject thiz, jobject jpbmsg);
JNIEXPORT jstring JNICALL
Java_com_petbot_PBConnector_stringFromJNI( JNIEnv* env,
                                                  jobject thiz );

JNIEXPORT jbyteArray JNICALL Java_com_petbot_PBConnector_newByteArray(JNIEnv* env,jobject thiz )
#endif //PETBOT_ANDROID_PBCONNECTOR_H
