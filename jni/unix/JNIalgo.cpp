/* JNIalgo.cpp
 * Contains global functions to interface with the native library
 */

// Combocheck libraries
#include "com_combocheck_algo_JNIFunctions.h"
#include "jnialgo.h"

// C libraries
#include <cstdlib>
#include <cstring>
#include <cstdio>

// Variables for incremental algorithm progress
int thread_count = 8;
volatile int progress = 0;
pthread_mutex_t progress_mutex;
int completed;
const char *current_check;
int checks_completed;

// Variables for file lists accessible to algorithms
int file_count;
int pair_count;
char **file_names;
int *file_pairs;

/**
 * Frees the file metadata for JNIalgo, which is malloc'd
 */
static void free_metadata() {
	// On init, file_count is zero due to being static
	for(int i = 0; i < file_count; ++i) {
		free(file_names[i]);
	}
	if(file_count) {
		free(file_names);
		free(file_pairs);
	}
}

// Set the file pair data for running algorithms
JNIEXPORT void JNICALL Java_com_combocheck_algo_JNIFunctions_SetJNIFilePairData(
		JNIEnv *env, jclass cls, jobjectArray fileNames, jintArray pairData) {

	// Initialize the progress mutex
	static int init = 1;
	if(init) {
		init = 0;
		pthread_mutex_init(&progress_mutex, NULL);
	}

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

// Perform edit distance on two char buffers
int edit_distance_char(char *buf1, int size1, char *buf2, int size2) {

	// Perform edit distance on the buffers
	int rowlen = size1 + 1;
	int *D = (int*) malloc((rowlen * sizeof(int)) << 1);
	for(int i = 0; i <= size1; ++i) {
		D[i] = i;
	}
	D[rowlen] = 1;
	for(int i = 1; i <= size2; ++i) {
		for(int j = 1; j <= size1; ++j) {
			int sub = D[((i + 1) & 1) * rowlen + j - 1];
			if(buf1[j - 1] != buf2[i - 1]) {
				int ins = D[((i + 1) & 1) * rowlen + j];
				int del = D[(i & 1) * rowlen + j - 1];
				sub = sub < ins ? sub : ins;
				sub = sub < del ? sub : del;
				++sub;
			}
			D[(i & 1) * rowlen + j] = sub;
		}
		D[((i + 1) & 1) * rowlen] = D[(i & 1) * rowlen] + 1;
	}

	// Clean up
	int dist = D[(size2 & 1) * rowlen + size1];
	free(D);
	return dist;
}

// Perform edit distance on two int arrays
int edit_distance_int(int *arr1, int size1, int *arr2, int size2) {

	// Perform edit distance on the arrays
	int rowlen = size1 + 1;
	int *D = (int*) malloc((rowlen * sizeof(int)) << 1);
	for(int i = 0; i <= size1; ++i) {
		D[i] = i;
	}
	D[rowlen] = 1;
	for(int i = 1; i <= size2; ++i) {
		for(int j = 1; j <= size1; ++j) {
			int sub = D[((i + 1) & 1) * rowlen + j - 1];
			if(arr1[j - 1] != arr2[i - 1]) {
				int ins = D[((i + 1) & 1) * rowlen + j];
				int del = D[(i & 1) * rowlen + j - 1];
				sub = sub < ins ? sub : ins;
				sub = sub < del ? sub : del;
				++sub;
			}
			D[(i & 1) * rowlen + j] = sub;
		}
		D[((i + 1) & 1) * rowlen] = D[(i & 1) * rowlen] + 1;
	}

	// Clean up
	int dist = D[(size2 & 1) * rowlen + size1];
	free(D);
	return dist;
}

// C implementation of the JRE's String.hashCode() method
static int JRE_String_HashCode(char *str, int len) {
	int pow = 1;
	int hash = 0;
	for(int i = len - 1; i >=0; --i) {
		hash += str[i] * pow;
		pow *= len;
	}
	return hash;
}

// Get the moss fingerprint for a buffer
vector<int> get_moss_fingerprint(char *buf, int size, int K, int W) {
	
	// If the buffer is extremely short, fingerprint is just hash of it
	vector<int> fingerprint;
	if(size <= K) {
		fingerprint.push_back(JRE_String_HashCode(buf, size));
	} else {

		// Compute K-grams
		int ksize = size - K + 1;
		int *kgrams = (int*) malloc(sizeof(int) * ksize);
		for(int i = 0; i < ksize; ++i) {
			kgrams[i] = JRE_String_HashCode(buf + i, K);
		}

		// Create fingerprint
		int smallest = kgrams[0];
		int smallest_idx = 0;
		for(int i = 1; i < W; ++i) {
			if(kgrams[i] < smallest) {
				smallest = kgrams[i];
				smallest_idx = i;
			}
		}
		fingerprint.push_back(smallest);
		int current = smallest;
		int current_idx = smallest_idx;
		for(int i = 1; i < ksize - W + 1; ++i) {
			smallest = kgrams[i];
			smallest_idx = i;
			for(int j = 1; j < W; ++j) {
				if(kgrams[i + j] < smallest) {
					smallest = kgrams[i + j];
					smallest_idx = i + j;
				}
			}
			if(current > smallest || current_idx <= i - W) {
				fingerprint.push_back(smallest);
				current = smallest;
				current_idx = smallest_idx;
			}
		}

		// Clean up
		free(kgrams);
	}
	return fingerprint;
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

// Find the name of the current check
JNIEXPORT jstring JNICALL Java_com_combocheck_algo_JNIFunctions_GetJNICurrentCheck(
		JNIEnv *env, jclass cls) {
	return env->NewStringUTF(current_check);
}

// Reset the number of checks completed
JNIEXPORT void JNICALL Java_com_combocheck_algo_JNIFunctions_JNIClearChecksCompleted(
		JNIEnv *env, jclass cls) {
	checks_completed = 0;
}

// Poll how many checks have completed
JNIEXPORT jint JNICALL Java_com_combocheck_algo_JNIFunctions_JNIPollChecksCompleted(
		JNIEnv *env, jclass cls) {
	return (jint) checks_completed;
}
