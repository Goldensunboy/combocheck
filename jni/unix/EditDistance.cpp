/* EditDistance.cpp
 * Calculates edit distance for a list of file pairs
 */

// Combocheck libraries
#include "com_combocheck_algo_JNIFunctions.h"
#include "jnialgo.h"

// C libraries
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <cerrno>

// Variables specific to this algorithm
static int *pair_distances;
static char **normalized_files;
static int *file_lens;
static jclass LanguageUtils;
static jmethodID GetNormalizedFile;
static JavaVM *jvm;
static jobject normalization;

// Edit distance preprocessing function
static void *do_ed_preprocessing(void *data) {

	// Get thread data
	int idx = *(int*) data;
	JNIEnv *env;
	jvm->AttachCurrentThread((void**) &env, NULL);

	// Starting index in the striped fingerprint array computation
	while(!halt && idx < file_count) {

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

		// Set the information for this file
		normalized_files[idx] = str;
		file_lens[idx] = strlen(str);

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

// Function to be run by the threads
static void *do_ed_difference(void *data) {

	// Starting index in the striped distance array computation
	int idx = *(int*) data;
	while(!halt && idx < pair_count) {

		// Get the buffers for the files for the edit distance calculation
		int idx1 = file_pairs[idx << 1];
		int idx2 = file_pairs[(idx << 1) + 1];
		char *buf1 = normalized_files[idx1];
		char *buf2 = normalized_files[idx2];
		int size1 = file_lens[idx1];
		int size2 = file_lens[idx2];

		// Get the edit distance
		pair_distances[idx] = edit_distance_char(buf1, size1, buf2, size2);

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

// Spawn edit distance function threads
JNIEXPORT jintArray JNICALL Java_com_combocheck_algo_JNIFunctions_JNIEditDistance(
		JNIEnv *env, jclass cls) {

	progress = completed = 0;
	current_check = "Edit distance preprocessing";

	// Get callback parameters
	jclass EDAlgorithm = env->FindClass(
			"com/combocheck/algo/EditDistanceAlgorithm");
	jfieldID N_ID = env->GetStaticFieldID(EDAlgorithm, "Normalization",
			"Lcom/combocheck/algo/LanguageUtils$NormalizerType;");
	normalization = env->GetStaticObjectField(EDAlgorithm, N_ID);
	LanguageUtils = env->FindClass("com/combocheck/algo/LanguageUtils");
	GetNormalizedFile = env->GetStaticMethodID(LanguageUtils,
			"GetNormalizedFile", "(Ljava/lang/String;Lcom/combocheck/"
			"algo/LanguageUtils$NormalizerType;)Ljava/lang/String;");
	env->GetJavaVM(&jvm);

	// The array for the file pair metrics
	pair_distances = (int*) malloc(sizeof(int) * pair_count);
	normalized_files = (char**) calloc(file_count, sizeof(char*));
	file_lens = (int*) malloc(sizeof(int) * file_count);

	// Initialize thread pool
	int tc = thread_count;
	pthread_t *threads = (pthread_t*) malloc(tc * sizeof(pthread_t));

	// Initialize edit distance preprocessing threads
	completed = 0;
	for(int i = 0; i < thread_count; ++i) {
		int *thread_idx = (int*) malloc(sizeof(int));
		*thread_idx = i;
		pthread_create(threads + i, NULL, do_ed_preprocessing, thread_idx);
	}

	// Join edit distance preprocessing threads
	for(int i = 0; i < thread_count; ++i) {
		pthread_join(threads[i], NULL);
	}
	++checks_completed;

	progress = completed = 0;
	current_check = "Edit distance comparisons";

	// Initialize edit distance difference threads
	completed = 0;
	for(int i = 0; i < thread_count; ++i) {
		int *thread_idx = (int*) malloc(sizeof(int));
		*thread_idx = i;
		pthread_create(threads + i, NULL, do_ed_difference, thread_idx);
	}

	// Join edit distance difference threads
	for(int i = 0; i < thread_count; ++i) {
		pthread_join(threads[i], NULL);
	}
	++checks_completed;

	// Construct the jintArray to return the data
	jintArray ret = env->NewIntArray(pair_count);
	env->SetIntArrayRegion(ret, 0, pair_count, pair_distances);

	// Clean up
	free(threads);
	free(pair_distances);
	for(int i = 0; i < file_count; ++i) {
		if(normalized_files[i]) {
			free(normalized_files[i]);
		}
	}
	free(normalized_files);
	free(file_lens);

	return ret;
}
