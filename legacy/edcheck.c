/* Andrew Wilder
 * Spring 2015
 */

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <pthread.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/ioctl.h>

#include "combocheck.h"

static int jobs = 0;
static pthread_mutex_t jobs_mutex;

// Perform edit distance between two files
static int edit_distance(char *fname1, char *fname2) {
	
	// Read the files into buffers
	FILE *f1, *f2;
	if(!(f1 = fopen(fname1, "r")) || !(f2 = fopen(fname2, "r"))) {
		printf("Error opening files: %s\n", strerror(errno));
		return -1;
	}
	fseek(f1, 0, SEEK_END);
	long size1 = ftell(f1);
	fseek(f1, 0, SEEK_SET);
	char *buf1 = malloc(size1);
	fread(buf1, size1, 1, f1);
	fseek(f2, 0, SEEK_END);
	long size2 = ftell(f2);
	fseek(f2, 0, SEEK_SET);
	char *buf2 = malloc(size2);
	fread(buf2, size2, 1, f2);
	fclose(f1);
	fclose(f2);

	// Perform edit distance on the buffers
	int *D = malloc(((size1 + 1) * sizeof(int)) << 1);
	for(int i = 0; i <= size1; ++i) {
		D[i] = i;
	}
	D[size1 + 1] = 1;
	for(int i = 1; i <= size2; ++i) {
		for(int j = 1; j <= size1; ++j) {
			int sub = D[j - 1];
			if(buf1[j - 1] != buf2[i - 1]) {
				int ins = D[j];
				int del = D[size1 + j];
				sub = sub < ins ? sub : ins;
				sub = sub < del ? sub : del;
				++sub;
			}
			D[size1 + 1 + j] = sub;
		}
		memcpy(D, D + size1 + 1, (size1 + 1) * sizeof(int));
		D[size1 + 1] = *D + 1;
	}
	int dist = D[(size1 << 1) + 1];

	// Clean up
	free(buf1);
	free(buf2);
	free(D);
	return dist;
}

// Print completed jobs
static void *do_print_jobs() {
	int completedjobs = 0;
	while(completedjobs < pair_count) {
		pthread_mutex_lock(&jobs_mutex); {
			if(jobs > completedjobs) {
				completedjobs = jobs;
				printf("\tAnalyzing pair %d... (%d%%)\r", jobs, 100 * jobs / pair_count);
				fflush(stdout);
			}
		} pthread_mutex_unlock(&jobs_mutex);
		pthread_yield();
	}
	printf("\tAnalyzing pair %d... (100%%) Done!\n", jobs);
	return NULL;
}

// Function to run as a thread
static void *do_edit_distance(void *data) {
	int idx = *(int*) data;
	while(idx < pair_count) {
		pair_t *p = file_pairs + idx;
		p->score[EDCHECK] = edit_distance(p->file[0], p->file[1]);
		pthread_mutex_lock(&jobs_mutex); {
			++jobs;
		} pthread_mutex_unlock(&jobs_mutex);
		idx += THREAD_COUNT;
	}
	free(data);
	return NULL;
}

// Run the edit distance check on all files
void edcheck(void) {
	printf("Running check: Edit Distance\n");
	test_names[EDCHECK] = "edcheck";
	pthread_t *print_thread = malloc(sizeof(pthread_t));
	pthread_mutex_init(&jobs_mutex, NULL);
	pthread_create(print_thread, NULL, do_print_jobs, NULL);
	pthread_t *threads = malloc(THREAD_COUNT * sizeof(pthread_t));
	for(int i = 0; i < THREAD_COUNT; ++i) {
		int *thread_index = (int*) malloc(sizeof(int));
		*thread_index = i;
		pthread_create(threads + i, NULL, do_edit_distance, thread_index);
	}
	for(int i = 0; i < THREAD_COUNT; ++i) {
		pthread_join(threads[i], NULL);
	}
	pthread_join(*print_thread, NULL);
	free(print_thread);
	free(threads);
}

