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

		// Read files into buffers
		FILE *f1, *f2;
		if(!(f1 = fopen(fname1, "r")) || !(f2 = fopen(fname2, "r"))) {
			fprintf(stderr, "Error opening file: %s\n", strerror(errno));
			if(!f1) {
				fprintf(stderr, "File: %s\n", fname1);
			} else {
				fprintf(stderr, "File: %s\n", fname2);
			}
			pair_distances[idx] = 0x7FFFFFFF;
		} else {
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

			// Get the edit distance
			pair_distances[idx] = edit_distance_char(buf1, size1, buf2, size2);

			// Clean up
			free(buf1);
			free(buf2);
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

// Spawn edit distance function threads
JNIEXPORT jintArray JNICALL Java_com_combocheck_algo_JNIFunctions_JNIEditDistance(
		JNIEnv *env, jclass cls) {

	progress = 0;
	current_check = "Edit distance";

	// The array for the file pair metrics
	pair_distances = (int*) malloc(pair_count * sizeof(int));

	// Initialize thread pool
	int tc = thread_count;
	pthread_t *threads = (pthread_t*) malloc(tc * sizeof(pthread_t));

	// Initialize edit distance threads
	completed = 0;
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
