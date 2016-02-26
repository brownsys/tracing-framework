#include <jni.h>
#include <stdio.h>
#include <time.h>
#include "../ThreadCPUTimer.h"

#include <stdint.h>

#include <mach/mach_init.h>
#include <mach/thread_act.h>
#include <mach/mach_port.h>

typedef uint64_t u64;

// http://stackoverflow.com/questions/17372110/clock-thread-cputime-id-on-macosx
// gets the CPU time used by the current thread (both system and user), in
// microseconds, returns 0 on failure
static u64 getthreadtime(thread_port_t thread) {
    mach_msg_type_number_t count = THREAD_BASIC_INFO_COUNT;
    thread_basic_info_data_t info;

    int kr = thread_info(thread, THREAD_BASIC_INFO, (thread_info_t) &info, &count);
    if (kr != KERN_SUCCESS) {
        return 0;
    }

    // add system and user time
    return (u64) info.user_time.seconds * (u64) 1e6 +
        (u64) info.user_time.microseconds +
        (u64) info.system_time.seconds * (u64) 1e6 +
        (u64) info.system_time.microseconds;
}

JNIEXPORT jlong JNICALL Java_edu_brown_cs_systems_clockcycles_CPUCycles_getNative(JNIEnv *, jclass) {
    thread_port_t thread = mach_thread_self();
    u64 us = getthreadtime(thread);
    mach_port_deallocate(mach_task_self(), thread);
    return us * 1000LL;   
}
