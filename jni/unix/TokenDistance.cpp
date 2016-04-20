/* TokenDistance.cpp
 * Calculates edit distance between token streams for file pairs
 */

// Combocheck libraries
#include "com_combocheck_algo_JNIFunctions.h"
#include "jnialgo.h"

// C libraries
#include <cstdlib>
#include <cstring>
#include <cstdio>

// Variables specific to this algorithm
static int *pair_diffs;
static int **token_arrs;
static int *token_lens;
static jclass LanguageUtils;
static jmethodID GetTokenIDs;
static JavaVM *jvm;

// Token distance preprocessing function
static void *do_token_preprocessing(void *data) {

	// Get thread data
	int idx = *(int*) data;
	JNIEnv *env;
	jvm->AttachCurrentThread((void**) &env, NULL);

	// Starting index in the striped fingerprint array computation
	while(!halt && idx < file_count) {

		// Get the tokens via the LanguageUtils class
		char *fname = file_names[idx];
		jstring ParamString = env->NewStringUTF(fname);
		jintArray tokens = (jintArray) env->CallStaticObjectMethod(
				LanguageUtils, GetTokenIDs, ParamString);
		env->DeleteLocalRef(ParamString);

		// Set the token data for this file
		token_lens[idx] = env->GetArrayLength(tokens);
		token_arrs[idx] = (int*) malloc(sizeof(int) * token_lens[idx]);
		jint *jtokens = env->GetIntArrayElements(tokens, 0);
		memcpy(token_arrs[idx], jtokens, sizeof(int) * token_lens[idx]);
		env->ReleaseIntArrayElements(tokens, jtokens, 0);

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

// Token distance difference function
static void *do_token_difference(void *data) {

	// Starting index in the striped fingerprint array computation
	int idx = *(int*) data;
	while(!halt && idx < pair_count) {

		// Get the file pair arrays
		int idx1 = file_pairs[idx << 1];
		int idx2 = file_pairs[(idx << 1) + 1];
		int *tokens1 = token_arrs[idx1];
		int *tokens2 = token_arrs[idx2];
		int len1 = token_lens[idx1];
		int len2 = token_lens[idx2];

		// Compute the difference
		pair_diffs[idx] = edit_distance_int(tokens1, len1, tokens2, len2);

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

// Spawn token distance function threads
JNIEXPORT jintArray JNICALL Java_com_combocheck_algo_JNIFunctions_JNITokenDistance(
		JNIEnv *env, jclass cls) {

	progress = completed = 0;
	current_check = "Token distance preprocessing";

	// Initialize the arrays for calculating differences
	pair_diffs = (int*) malloc(sizeof(int) * pair_count);
	token_arrs = (int**) calloc(file_count, sizeof(int*));
	token_lens = (int*) malloc(sizeof(int) * file_count);

	// Create global references for callback parameters
	LanguageUtils = env->FindClass("com/combocheck/algo/LanguageUtils");
	GetTokenIDs = env->GetStaticMethodID(LanguageUtils, "GetTokenIDs",
			"(Ljava/lang/String;)[I");
	env->GetJavaVM(&jvm);

	// Initialize thread pool
	int tc = thread_count;
	pthread_t *threads = (pthread_t*) malloc(tc * sizeof(pthread_t));

	// Initialize token distance preprocessing threads
	for(int i = 0; i < thread_count; ++i) {
		int *thread_idx = (int*) malloc(sizeof(int));
		*thread_idx = i;
		pthread_create(threads + i, NULL, do_token_preprocessing, thread_idx);
	}

	// Join token distance preprocessing threads
	for(int i = 0; i < thread_count; ++i) {
		pthread_join(threads[i], NULL);
	}
	++checks_completed;

	progress = completed = 0;
	current_check = "Token distance comparisons";

	// Initialize token distance difference threads
	completed = 0;
	for(int i = 0; i < thread_count; ++i) {
		int *thread_idx = (int*) malloc(sizeof(int));
		*thread_idx = i;
		pthread_create(threads + i, NULL, do_token_difference, thread_idx);
	}

	// Join token distance difference threads
	for(int i = 0; i < thread_count; ++i) {
		pthread_join(threads[i], NULL);
	}
	++checks_completed;

	// Construct the jintArray to return the data
	jintArray ret = env->NewIntArray(pair_count);
	env->SetIntArrayRegion(ret, 0, pair_count, pair_diffs);

	// Clean up
	for(int i = 0; i < file_count; ++i) {
		if(token_arrs[i]) {
			free(token_arrs[i]);
		}
	}
	free(token_lens);
	free(token_arrs);
	free(threads);
	free(pair_diffs);

	return ret;
}
