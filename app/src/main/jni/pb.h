
#ifdef A20
#endif

#ifdef OSX
#endif

#ifdef ANDROID
	#include <android/log.h>
	//#define PBPRINTF(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, "Petbot", "DEBUG: %s:%d:%s(): "  fmt, __FILE__, __LINE__, __func__, ##args)
	#define  LOG_TAG    "Petbot"
	#define  PBPRINTF(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#else
	#define PBPRINTF(fmt, args...) fprintf(stderr, "DEBUG: %s:%d:%s(): " fmt, \
    	  __FILE__, __LINE__, __func__, ##args)
#endif

void pbdelay(int d);


