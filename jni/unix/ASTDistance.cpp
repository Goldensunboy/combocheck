/* ASTDistance.cpp
 * Calculates AST distance file pairs
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
static jclass LanguageUtils;
static jmethodID GetNormalizedFile;
static JavaVM *jvm;

// Spawn AST distance function threads
JNIEXPORT jintArray JNICALL Java_com_combocheck_algo_JNIFunctions_JNIASTDistance(
	JNIEnv *env, jclass cls) {

	// progress = 0;
	// current_check = "AST distance preprocessing";

	// // Initialize the arrays for calculating differences
	// pair_diffs = (int*) malloc(sizeof(int) * pair_count);

	// // Create global references for callback parameters
	// LanguageUtils = env->FindClass("com/combocheck/algo/LanguageUtils");
	// GetNormalizedFile = env->GetStaticMethodID(LanguageUtils,
	// 		"GetNormalizedFile", "(Ljava/lang/String;)Ljava/lang/String;");
	// env->GetJavaVM(&jvm);

}

