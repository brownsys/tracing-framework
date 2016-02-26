#include <jni.h>
#include "ThreadCPUTimer.h"
#include <Windows.h>

JNIEXPORT jlong JNICALL Java_edu_brown_cs_systems_clockcycles_CPUCycles_getNative(JNIEnv *env, jclass thisObj) {
	LONG64 cycles;
	QueryThreadCycleTime(GetCurrentThread(), &cycles); 
	return cycles;
}
