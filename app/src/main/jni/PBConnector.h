//
// Created by Misko Dzamba on 16-08-28.
//

#ifndef PETBOT_ANDROID_PBCONNECTOR_H
#define PETBOT_ANDROID_PBCONNECTOR_H
JNIEXPORT jstring JNICALL
Java_com_petbot_PBConnector_stringFromJNI( JNIEnv* env,
                                                  jobject thiz );

JNIEXPORT jbyteArray JNICALL Java_com_petbot_PBConnector_newByteArray(JNIEnv* env,jobject thiz )
#endif //PETBOT_ANDROID_PBCONNECTOR_H
