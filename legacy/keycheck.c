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
#include "keycheck.h"
#include "list.h"
#include "hashmap.h"

static hashmap_t *key_to_files_map, *file_to_keys_map;
static char first_char[256], other_char[256];
static int jobs = 0;
static pthread_mutex_t jobs_mutex;

// String hash function
int string_hash_djb2(const void *data) {
	char *str = (char*) data;
	int hash = 5381;
	char c;
	while((c = *str++)) {
		hash += (hash << 5) + c;
	}
	return hash & 0x7FFFFFFF;
}

// String list freeing function
void string_list_free(void *data) {
	list_destroy(data, free);
}

// String equality function (already defined in combocheck.c)
extern int string_equal(const void *data1, const void *data2);

// Process a token
void process_token(char *token, char *file) {

	// Does this file already exist in the key->file map?
	list_t *token_to_files_list = hashmap_get(key_to_files_map, token);
	if(!token_to_files_list) {
		token_to_files_list = list_create();
		hashmap_put(key_to_files_map, strdup(token), token_to_files_list);
	}
	// Does this file already exist in the list?
	if(!list_contains(token_to_files_list, file, string_equal)) {
		list_push_front(token_to_files_list, strdup(file));
	}

	// Does this token already exist in the file->key map?
	list_t *file_to_tokens_list = hashmap_get(file_to_keys_map, file);
	// Note: This list always exists due to initialization in keycheck()
	if(!list_contains(file_to_tokens_list, token, string_equal)) {
		list_push_front(file_to_tokens_list, strdup(token));
	}
}

// Count the number of unique tokens between 2 files
static int unique_token_diff(char *fname1, char *fname2) {
	list_t *tlist1 = hashmap_get(file_to_keys_map, fname1);
	list_t *tlist2 = hashmap_get(file_to_keys_map, fname2);
	int common = 0;
	typedef struct list_node {
		void *data;
		struct list_node *next;
	} node_t;
	node_t *node = (node_t*) tlist1->head;
	while(node) {
		if(list_contains(tlist2, node->data, string_equal)) {
			++common;
		}
		node = node->next;
	}
	return tlist1->size + tlist2->size - (common << 1);
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
static void *do_eval_pair(void *data) {
	int idx = *(int*) data;
	while(idx < pair_count) {
		pair_t *p = file_pairs + idx;
		p->score[KEYCHECK] = unique_token_diff(p->file[0], p->file[1]);
		pthread_mutex_lock(&jobs_mutex); {
			++jobs;
		} pthread_mutex_unlock(&jobs_mutex);
		idx += THREAD_COUNT;
	}
	free(data);
	return NULL;
}

// Run the keyword check on all files
void keycheck(void) {
	printf("Running check: Keyword Similarity\n");
	test_names[KEYCHECK] = "keycheck";

	// Initialize
	for(int i = 'a'; i <= 'z'; ++i) {
		first_char[i] = other_char[i] = 1;
	}
	for(int i = 'A'; i <= 'Z'; ++i) {
		first_char[i] = other_char[i] = 1;
	}
	for(int i = '0'; i <= '9'; ++i) {
		other_char[i] = 1;
	}
	first_char[(int) '_'] = other_char[(int) '_'] = 1;
	key_to_files_map = hashmap_create(string_hash_djb2, string_equal, free, string_list_free, CP_FREE_OLD_ENTRY);
	file_to_keys_map = hashmap_create(string_hash_djb2, string_equal, free, string_list_free, CP_FREE_OLD_ENTRY);

	// Index all files (a highly serialized operation)
	for(int i = 0; i < file_count; ++i) {
		printf("\tIndexing file %d... (%d%%)\r", i, 100 * i / file_count);
		hashmap_put(file_to_keys_map, strdup(file_index[i]), list_create());
		
		// Read file into buffer
		FILE *f;
		if(!(f = fopen(file_index[i], "r"))) {
			printf("Error opening file: %s\n", strerror(errno));
			return;
		}
		fseek(f, 0, SEEK_END);
		long size = ftell(f);
		fseek(f, 0, SEEK_SET);
		unsigned char *buf = malloc(size);
		fread(buf, size, 1, f);
		fclose(f);

		// Tokenize identifiers and index file
		int sc = 0, ec = 0;
		while(sc < size) {
			if(first_char[(int) buf[sc]]) {
				ec = sc + 1;
				while(ec < size && other_char[(int) buf[ec]]) {
					++ec;
				}
				// Token from sc (inclusive) to ec (exclusive)
				char *token = strndup((void*) (buf + sc), ec - sc);
				process_token(token, file_index[i]);
				free(token);
				sc = ec + 1;
			} else {
				++sc;
			}
		}
		free(buf);
	}
	printf("\tIndexing file %d... (100%%) Done!\n", file_count);

	// Update scores
	pthread_t *print_thread = malloc(sizeof(pthread_t));
	pthread_mutex_init(&jobs_mutex, NULL);
	pthread_create(print_thread, NULL, do_print_jobs, NULL);
	pthread_t *threads = malloc(THREAD_COUNT * sizeof(pthread_t));
	for(int i = 0; i < THREAD_COUNT; ++i) {
		int *thread_index = (int*) malloc(sizeof(int));
		*thread_index = i;
		pthread_create(threads + i, NULL, do_eval_pair, thread_index);
	}
	for(int i = 0; i < THREAD_COUNT; ++i) {
		pthread_join(threads[i], NULL);
	}
	pthread_join(*print_thread, NULL);
	free(print_thread);
	free(threads);

	// Clean up
	hashmap_destroy(key_to_files_map);
	hashmap_destroy(file_to_keys_map);
}

