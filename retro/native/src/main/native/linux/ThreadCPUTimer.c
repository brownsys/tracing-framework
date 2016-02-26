#include <jni.h>
#include <stdio.h>
#include <time.h>
#include "../ThreadCPUTimer.h"
 
JNIEXPORT jlong JNICALL Java_edu_brown_cs_systems_clockcycles_CPUCycles_getNative(JNIEnv *, jclass) {
   struct timespec tp;
   clock_gettime(CLOCK_THREAD_CPUTIME_ID, &tp);
   return 1000000000LL * ((long long) tp.tv_sec) + ((long long) tp.tv_nsec);
}
