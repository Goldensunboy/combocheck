/* JNIalgo.cpp
 * Contains global functions to interface with the native library
 */

#include "com_combocheck_algo_JNIFunctions.h"
#include "jnialgo.h"

#include <cstdlib>
#include <cstring>
#include <cstdio>

int thread_count = 8;
volatile int progress = 0;

int file_count;
int pair_count;
char **file_names;
int *file_pairs;

/**
 * Frees the file metadata for JNIalgo, which is malloc'd
 */
void free_metadata() {
	for(int i = 0; i < file_count; ++i) {
		free(file_names + i);
	}
	free(file_names);
	free(file_pairs);
}

// Set the file pair data for running algorithms
JNIEXPORT void JNICALL Java_com_combocheck_algo_JNIFunctions_SetJNIFilePairData(
		JNIEnv *env, jclass cls, jobjectArray fileNames, jintArray pairData) {

	// Clean up from previous invocation
	free_metadata();

	// Set file names
	file_count = env->GetArrayLength(fileNames);
	file_names = (char**) malloc(file_count * sizeof(char*));
	for(int i = 0; i < file_count; ++i) {
		jstring jstr = (jstring) env->GetObjectArrayElement(fileNames, i);
		const char *nstr = env->GetStringUTFChars(jstr, 0);
		char *str = (char*) malloc((env->GetStringUTFLength(jstr) + 1) * sizeof(char));
		strcpy(str, nstr);
		file_names[i] = str;
		env->ReleaseStringUTFChars(jstr, nstr);
	}

	// Set pair data
	pair_count = env->GetArrayLength(pairData) >> 1;
	file_pairs = (int*) malloc(pair_count * sizeof(int) << 1);
	for(int i = 0; i < pair_count; ++i) {
		jint *jarr = env->GetIntArrayElements(pairData, 0);
		memcpy(file_pairs, jarr, pair_count * sizeof(int) << 1);
		env->ReleaseIntArrayElements(pairData, jarr, 0);
	}
}	

// Set the thread count
JNIEXPORT void JNICALL Java_com_combocheck_algo_JNIFunctions_SetJNIThreads(
		JNIEnv *env, jclass cls, jint threadCount) {
	thread_count = threadCount;
}

// Polling the algorithm completion
JNIEXPORT jint JNICALL Java_com_combocheck_algo_JNIFunctions_PollJNIProgress(
		JNIEnv *env, jclass cls) {
	return progress;
}

