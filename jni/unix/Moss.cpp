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
static vector<vector<int>> fingerprints;
static int *pair_diffs;
static jclass LanguageUtils;
static jmethodID GetNormalizedFile;
static JavaVM *jvm;
static jobject normalization;

// Moss preprocessing function (fingerprint generation)
static void *do_moss_preprocessing(void *data) {

	// Get thread data
	int idx = *(int*) data;
	JNIEnv *env;
	jvm->AttachCurrentThread((void**) &env, NULL);

	// Starting index in the striped fingerprint array computation
	while(idx < file_count) {

		// Get the normalized file contents from the LanguageUtils class
		char *fname = file_names[idx];
		jstring ParamString = env->NewStringUTF(fname);
		jstring ReturnString = (jstring) env->CallStaticObjectMethod(
				LanguageUtils, GetNormalizedFile, ParamString, normalization);
		env->DeleteLocalRef(ParamString);
		const char *nstr = env->GetStringUTFChars(ReturnString, 0);
		int len = env->GetStringUTFLength(ReturnString);
		char *str = (char*) malloc((len + 1) * sizeof(char));
		strcpy(str, nstr);
		env->ReleaseStringUTFChars(ReturnString, nstr);

		// Compute the fingerprint for the file
		fingerprints[idx] = get_moss_fingerprint(str, len, K, W);
		free(str);

		// Update progress
		pthread_mutex_lock(&progress_mutex);
		progress = 100 * ++completed / file_count;
		pthread_mutex_unlock(&progress_mutex);

		idx += thread_count;
	}

	// Clean up
	free(data);
	jvm->DetachCurrentThread();
	return NULL;
}

// Moss difference function
static void *do_moss_difference(void *data) {

	// Starting index in the striped fingerprint array computation
	int idx = *(int*) data;
	while(idx < pair_count) {

		// Get the file pair arrays
		int idx1 = file_pairs[idx << 1];
		int idx2 = file_pairs[(idx << 1) + 1];
		int *fp1 = fingerprints[idx1].data();
		int s1 = fingerprints[idx1].size();
		int *fp2 = fingerprints[idx2].data();
		int s2 = fingerprints[idx2].size();

		// Compute the difference for the fingerprints
		pair_diffs[idx] = edit_distance_int(fp1, s1, fp2, s2);

		// Update progress
		pthread_mutex_lock(&progress_mutex);
		progress = 100 * ++completed / pair_count;
		pthread_mutex_unlock(&progress_mutex);

		idx += thread_count;
	}

	// Clean up
	free(data);
	return NULL;
}

// Spawn moss function threads
JNIEXPORT jintArray JNICALL Java_com_combocheck_algo_JNIFunctions_JNIMoss(
	JNIEnv *env, jclass cls) {

	progress = 0;
	current_check = "Moss preprocessing";

	// Set the Moss parameters
	jclass MossAlgorithm = env->FindClass("com/combocheck/algo/MossAlgorithm");
	jfieldID K_ID = env->GetStaticFieldID(MossAlgorithm, "K", "I");
	K = (int) env->GetStaticIntField(MossAlgorithm, K_ID);
	jfieldID W_ID = env->GetStaticFieldID(MossAlgorithm, "W", "I");
	W = (int) env->GetStaticIntField(MossAlgorithm, W_ID);
	jfieldID N_ID = env->GetStaticFieldID(MossAlgorithm, "Normalization",
			"Lcom/combocheck/algo/LanguageUtils$NormalizerType;");
	if(env->ExceptionOccurred()) env->ExceptionDescribe();
	normalization = env->GetStaticObjectField(MossAlgorithm, N_ID);
	if(env->ExceptionOccurred()) env->ExceptionDescribe();

	// Initialize the arrays for calculating differences
	fingerprints = vector<vector<int>>(file_count);
	pair_diffs = (int*) malloc(sizeof(int) * pair_count);

	// Create global references for callback parameters
	LanguageUtils = env->FindClass("com/combocheck/algo/LanguageUtils");
	GetNormalizedFile = env->GetStaticMethodID(LanguageUtils,
			"GetNormalizedFile", "(Ljava/lang/String;Lcom/combocheck/"
			"algo/LanguageUtils$NormalizerType;)Ljava/lang/String;");
	if(env->ExceptionOccurred()) env->ExceptionDescribe();
	env->GetJavaVM(&jvm);

	// Initialize thread pool
	int tc = thread_count;
	pthread_t *threads = (pthread_t*) malloc(tc * sizeof(pthread_t));

	// Initialize moss preprocessing threads
	for(int i = 0; i < thread_count; ++i) {
		int *thread_idx = (int*) malloc(sizeof(int));
		*thread_idx = i;
		pthread_create(threads + i, NULL, do_moss_preprocessing, thread_idx);
	}

	// Join moss preprocessing threads
	for(int i = 0; i < thread_count; ++i) {
		pthread_join(threads[i], NULL);
	}

	progress = 0;
	current_check = "Moss fingerprint comparisons";

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
	free(pair_diffs);

	return ret;
}
