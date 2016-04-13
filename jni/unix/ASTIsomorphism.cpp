/* ASTIsomorphism.cpp
 * Determines whether or not two ASTs are isomorphic by the AHU algorithm
 */

// Combocheck libraries
#include "com_combocheck_algo_JNIFunctions.h"
#include "jnialgo.h"

// C libraries
#include <cstdlib>
#include <cstring>
#include <cstdio>

// Variables specific to this algorithm
static char **canonical_names;
static int *pair_diffs;
static jclass LanguageUtils, ParseTree;
static jmethodID GetAST, getChildCount, getChild;
static JavaVM *jvm;

// Recursively create a canonical name from an AST
static char *get_canonical_name(JNIEnv *env, jobject node) {

	// Find how many children there are for this node
	int child_count = (int) env->CallIntMethod(node, getChildCount);
	struct name_t {
		char *str;
		int len;
	};
	struct name_t *child_names = (struct name_t*)
			malloc(sizeof(struct name_t) * child_count);
	int total_len = 0;

	// Get the canonical names for the children
	for(int i = 0; i < child_count; ++i) {
		jobject child = env->CallObjectMethod(node, getChild, i);
		child_names[i].str = get_canonical_name(env, child);
		child_names[i].len = strlen(child_names[i].str);
		total_len += child_names[i].len;
	}

	// Sort the canonical names lexicographically
	qsort(child_names, child_count, sizeof(struct name_t),
			(int (*)(const void*, const void*)) strcmp);

	// Construct return string (strcmp sorted them descending)
	char *ret = (char*) malloc(sizeof(char) * (total_len + 3));
	ret[0] = '1';
	char *ptr = ret + 1;
	for(int i = child_count - 1; i >= 0; --i) {
		strcpy(ptr, child_names[i].str);
		ptr += child_names[i].len;
		free(child_names[i].str);
	}
	strcpy(ptr, "0");

	// Clean up
	free(child_names);

	return ret;
}

// AST isomorphism preprocessing function (canonical name generation)
static void *do_iso_preprocessing(void *data) {

	// Get thread data
	int idx = *(int*) data;
	JNIEnv *env;
	jvm->AttachCurrentThread((void**) &env, NULL);

	// Starting index in the striped fingerprint array computation
	while(idx < file_count) {

		// Get the AST via the LanguageUtils class
		char *fname = file_names[idx];
		jstring ParamString = env->NewStringUTF(fname);
		jobject AST = env->CallStaticObjectMethod(LanguageUtils, GetAST,
				ParamString);
		env->DeleteLocalRef(ParamString);

		// Create canonical name if the AST is not NULL
		canonical_names[idx] = AST ? get_canonical_name(env, AST) : NULL;

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

// AST isomorphism difference function
static void *do_iso_difference(void *data) {

	// Starting index in the striped fingerprint array computation
	int idx = *(int*) data;
	while(idx < pair_count) {

		// Get the file pair arrays
		int idx1 = file_pairs[idx << 1];
		int idx2 = file_pairs[(idx << 1) + 1];
		char *cname1 = canonical_names[idx1];
		char *cname2 = canonical_names[idx2];

		// Compute the difference
		if(cname1 && cname2) {
			pair_diffs[idx] = strcmp(cname1, cname2) ? 1 : 0;
		} else {
			pair_diffs[idx] = 0x7FFFFFFF;
		}

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

// Spawn AST isomorphism function threads
JNIEXPORT jintArray JNICALL Java_com_combocheck_algo_JNIFunctions_JNIASTIsomorphism(
	JNIEnv *env, jclass cls) {

	progress = 0;
	current_check = "AST isomorphism preprocessing";

	// Initialize the arrays for calculating differences
	canonical_names = (char**) malloc(sizeof(char*) * file_count);
	pair_diffs = (int*) malloc(sizeof(int) * pair_count);

	// Create global references for callback parameters
	LanguageUtils = env->FindClass("com/combocheck/algo/LanguageUtils");
	ParseTree = env->FindClass("org/antlr/v4/runtime/tree/ParseTree");
	GetAST = env->GetStaticMethodID(LanguageUtils, "GetAST",
			"(Ljava/lang/String;)Lorg/antlr/v4/runtime/tree/ParseTree;");
	getChildCount = env->GetMethodID(ParseTree, "getChildCount", "()I");
	getChild = env->GetMethodID(ParseTree, "getChild",
			"(I)Lorg/antlr/v4/runtime/tree/ParseTree;");
	env->GetJavaVM(&jvm);

	// Initialize thread pool
	int tc = thread_count;
	pthread_t *threads = (pthread_t*) malloc(tc * sizeof(pthread_t));

	// Initialize AST isomorphism preprocessing threads
	for(int i = 0; i < thread_count; ++i) {
		int *thread_idx = (int*) malloc(sizeof(int));
		*thread_idx = i;
		pthread_create(threads + i, NULL, do_iso_preprocessing, thread_idx);
	}

	// Join AST isomorphism preprocessing threads
	for(int i = 0; i < thread_count; ++i) {
		pthread_join(threads[i], NULL);
	}
	++checks_completed;

	progress = 0;
	current_check = "AST isomorphism comparisons";

	// Initialize AST isomorphism difference threads
	completed = 0;
	for(int i = 0; i < thread_count; ++i) {
		int *thread_idx = (int*) malloc(sizeof(int));
		*thread_idx = i;
		pthread_create(threads + i, NULL, do_iso_difference, thread_idx);
	}

	// Join AST isomorphism difference threads
	for(int i = 0; i < thread_count; ++i) {
		pthread_join(threads[i], NULL);
	}
	++checks_completed;

	// Construct the jintArray to return the data
	jintArray ret = env->NewIntArray(pair_count);
	env->SetIntArrayRegion(ret, 0, pair_count, pair_diffs);

	// Clean up
	for(int i = 0; i < file_count; ++i) {
		free(canonical_names[i]);
	}
	free(canonical_names);
	free(threads);
	free(pair_diffs);

	return ret;
}
