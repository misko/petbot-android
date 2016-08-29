
#ifdef A20
#endif

#ifdef OSX
#endif

#ifdef ANDROID
	#include <android/log.h>
	#define PBPRINTF(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, "PetBot", fmt, ##args)
#else
	#define PBPRINTF(fmt, args...) fprintf(stderr, "DEBUG: %s:%d:%s(): " fmt, \
    	  __FILE__, __LINE__, __func__, ##args)
#endif

void pbdelay(int d);


