/* Moss.cpp
 * Calculates moss metrics for file pairs
 */

// Combocheck libraries
#include "com_combocheck_algo_JNIFunctions.h"
#include "jnialgo.h"

// C libraries
#include <cstdlib>
#include <cstring>
#include <cstdio>

// Variables specific to this algorithm
static int K, W;
static vector<int> *fingerprints;
static int *pair_diffs;

// Moss preprocessing function (fingerprint generation)
static void *do_moss_preprocessing(void *data) {

	// Starting index in the striped fingerprint array computation
	void **thread_data = (void**) data;
	int idx = *(int*) thread_data[0];
	JNIEnv *env = (JNIEnv*) thread_data[1];
	jclass LanguageUtils = *(jclass*) thread_data[2];
	jmethodID GetNormalizedFile = *(jmethodID*) thread_data[3];
	while(idx < file_count) {

		// Get the normalized file contents from the LanguageUtils class
		char *fname = file_names[idx];
		jstring ParamString = env->NewStringUTF(fname);
		printf("%d 0\n", idx);
		env->CallStaticObjectMethod(LanguageUtils, GetNormalizedFile, ParamString);
		printf("%d 1\n", idx);
		jstring ReturnString = (jstring) env->CallStaticObjectMethod(LanguageUtils,
				GetNormalizedFile, ParamString);
		printf("%d 2\n", idx);
		const char *nstr = env->GetStringUTFChars(ReturnString, 0);
		int len = env->GetStringUTFLength(ReturnString);
		char *str = (char*) malloc((len + 1) * sizeof(char));
		strcpy(str, nstr);
		env->ReleaseStringUTFChars(ReturnString, nstr);
		printf("%d 3\n", idx);

		// Compute the fingerprint for the file
		fingerprints[idx] = get_moss_fingerprint(str, len, K, W);

		idx += thread_count;
	}

	// Clean up
	free(thread_data[0]);
	free(data);
	return NULL;
}

// Moss difference function
static void *do_moss_difference(void *data) {

	// Starting index in the striped fingerprint array computation
	int idx = *(int*) data;
	while(idx < pair_count) {

		printf("\t%d\n", idx);

		// Get the file pair arrays
		int idx1 = file_pairs[idx << 1];
		int idx2 = file_pairs[(idx << 1) + 1];
		int *fp1 = fingerprints[idx1].data();
		int s1 = fingerprints[idx1].size();
		int *fp2 = fingerprints[idx2].data();
		int s2 = fingerprints[idx2].size();

		// Compute the difference for the fingerprints
		pair_diffs[idx] = edit_distance_int(fp1, s1, fp2, s2);

		idx += thread_count;
	}

	// Clean up
	free(data);
	return NULL;
}

// Moss function
JNIEXPORT jintArray JNICALL Java_com_combocheck_algo_JNIFunctions_JNIMoss(
	JNIEnv *env, jclass cls, jint jK, jint jW) {

	// Set the K and W moss parameters
	K = (int) jK;
	W = (int) jW;

	// Initialize the arrays for calculating differences
	fingerprints = (vector<int>*) malloc(sizeof(vector<int>) * file_count);
	pair_diffs = (int*) malloc(sizeof(int) * pair_count);

	// Initialize thread pool
	int tc = thread_count;
	pthread_t *threads = (pthread_t*) malloc(tc * sizeof(pthread_t));

	// Initialize moss preprocessing threads
	jclass LanguageUtils =
			env->FindClass("com/combocheck/algo/LanguageUtils");
	jmethodID GetNormalizedFile = env->GetMethodID(LanguageUtils,
			"GetNormalizedFile", "(Ljava/lang/String;)Ljava/lang/String;");
	if(env->ExceptionOccurred()) {
		printf("Method thrown!\n");
		env->ExceptionDescribe();
		jintArray ret = env->NewIntArray(pair_count);
		env->SetIntArrayRegion(ret, 0, pair_count, pair_diffs);
		free(fingerprints);
		free(pair_diffs);
		return ret;
	}
	for(int i = 0; i < thread_count; ++i) {
		int *thread_idx = (int*) malloc(sizeof(int));
		*thread_idx = i;
		void **thread_data = (void**) malloc(sizeof(void*) << 2);
		thread_data[0] = thread_idx;
		thread_data[1] = env;
		thread_data[2] = &LanguageUtils;
		thread_data[3] = &GetNormalizedFile;
		pthread_create(threads + i, NULL, do_moss_preprocessing, thread_data);
	}

	// Join moss preprocessing threads
	for(int i = 0; i < thread_count; ++i) {
		pthread_join(threads[i], NULL);
	}

	// Initialize moss difference threads
	completed = 0;
	for(int i = 0; i < thread_count; ++i) {
		int *thread_idx = (int*) malloc(sizeof(int));
		*thread_idx = i;
		pthread_create(threads + i, NULL, do_moss_difference, thread_idx);
	}

	// Join moss difference threads
	for(int i = 0; i < thread_count; ++i) {
		pthread_join(threads[i], NULL);
	}

	// Construct the jintArray to return the data
	jintArray ret = env->NewIntArray(pair_count);
	env->SetIntArrayRegion(ret, 0, pair_count, pair_diffs);

	// Clean up
	free(threads);
	for(int i = 0; i < file_count; ++i) {
		fingerprints[i].~vector<int>();
	}
	free(fingerprints);
	free(pair_diffs);

	return ret;
}
