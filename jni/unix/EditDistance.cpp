/* EditDistance.cpp
 * Calculates edit distance for a list of file pairs
 */

#include "com_combocheck_algo_JNIFunctions.h"
#include "jnialgo.h"

#include <pthread.h>
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <cerrno>

using namespace std;

int *pair_distances;

// Edit distance function
static int edit_distance(char *fname1, char *fname2) {

	// Read files into buffers
	FILE *f1, *f2;
	if(!(f1 = fopen(fname1, "r")) || !(f2 = fopen(fname2, "r"))) {
		fprintf(stderr, "Error opening files: %s\n", strerror(errno));
		if(!f1) {
			fprintf(stderr, "File: %s\n", fname1);
		} else {
			fprintf(stderr, "File: %s\n", fname2);
		}
		return 0x7FFFFFFF;
	}
	fseek(f1, 0, SEEK_END);
	long size1 = ftell(f1);
	fseek(f1, 0, SEEK_SET);
	char *buf1 = (char*) malloc(size1);
	fread(buf1, size1, 1, f1);
	fseek(f2, 0, SEEK_END);
	long size2 = ftell(f2);
	fseek(f2, 0, SEEK_SET);
	char *buf2 = (char*) malloc(size2);
	fread(buf2, size2, 1, f2);
	fclose(f1);
	fclose(f2);

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
	int dist = D[(size2 & 1) * rowlen + size1];

	// Clean up
	free(buf1);
	free(buf2);
	free(D);

	return dist;
}

// Function to be run by the threads
static void *do_edit_distance(void *data) {

	// Starting index in the striped distance array computation
	int idx = *(int*) data;
	while(idx < pair_count) {

		// Get the names of the files for the next calculation
		int idx1 = file_pairs[idx << 1];
		int idx2 = file_pairs[(idx << 1) + 1];
		char *fname1 = file_names[idx1];
		char *fname2 = file_names[idx2];
		pair_distances[idx] = edit_distance(fname1, fname2);
		idx += thread_count;
	}
	free(data);
	return NULL;
}

// Spawn edit distance function threads
JNIEXPORT jintArray JNICALL Java_com_combocheck_algo_JNIFunctions_JNIEditDistance(
		JNIEnv *env, jclass cls) {

	// The array for the file pair metrics
	pair_distances = (int*) malloc(pair_count * sizeof(int));

	// Initialize thread pool
	int tc = thread_count;
	pthread_t *threads = (pthread_t*) malloc(tc * sizeof(pthread_t));
	
	// Initialize edit distance threads
	for(int i = 0; i < thread_count; ++i) {
		int *thread_idx = (int*) malloc(sizeof(int));
		*thread_idx = i;
		pthread_create(threads + i, NULL, do_edit_distance, thread_idx);
	}

	// Join edit distance threads
	for(int i = 0; i < thread_count; ++i) {
		pthread_join(threads[i], NULL);
	}

	// Construct the jintArray to return the data
	jintArray ret = env->NewIntArray(pair_count);
	env->SetIntArrayRegion(ret, 0, pair_count, pair_distances);

	// Clean up
	free(threads);
	free(pair_distances);

	return ret;
}

