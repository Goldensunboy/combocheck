/* Andrew Wilder
 * Spring 2015
 */

#include <stdio.h>
#include <stdlib.h>
#include <dirent.h>
#include <errno.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include "combocheck.h"
#include "list.h"
#include "edcheck.h"
#include "keycheck.h"

#define SEARCHDIRNAME(x) strcmp(x, ".") ? x : "this directory"

static char *single_file_name = NULL;
static char *outname = "differences.txt";
int pair_count, file_count;
pair_t *file_pairs;
char **file_index;

// Functions to run
void (*tests[TEST_COUNT])(void) = {
	edcheck,
	keycheck
};

// Names for the tests
const char *test_names[TEST_COUNT];

// String comparison function
int string_equal(const void *data1, const void *data2) {
	return strcmp(data1, data2) == 0;
}

// Sort pairs of files
int compare_pairs(const void *data1, const void *data2) {
	pair_t *p1 = (pair_t*) data1;
	pair_t *p2 = (pair_t*) data2;
	// keycheck is not a well-defined metric yet
	return p1->score[EDCHECK] - p2->score[EDCHECK];
}

// Find files recursively matching a search name and add them to a list
void find_files_recursively(char *rootpath, char *searchname, list_t *list) {

	// Attempt to open this directory
	DIR *dir = opendir(rootpath);
	if(dir) {

		// Directory entry for traversal of files in this directory
		struct dirent *dent;
		do {

			// Read next file in this directory
			dent = readdir(dir);
			if(dent) {

				// Create full relative path for stat function
				char *filepath;
				if(*rootpath != '.') {
					filepath = malloc(strlen(rootpath) + strlen(dent->d_name) + 2);
					strcpy(filepath, rootpath);
					strcat(filepath, "/");
					strcat(filepath, dent->d_name);
				} else {
					filepath = strdup(dent->d_name);
				}

				// Get file properties
				struct stat pathstat;
				if(!stat(filepath, &pathstat)) {
					if(S_ISDIR(pathstat.st_mode)) {
						// Recurse if this is a directory
						if(*dent->d_name != '.') {
							if(*rootpath == '.') {
								find_files_recursively(dent->d_name, searchname, list);
							} else {
								find_files_recursively(filepath, searchname, list);
							}
						}
					} else {
						// Add file to list if it matches search file
						if(!strcmp(searchname, dent->d_name)) {
							list_push_front(list, strdup(filepath));
						}
					}
				} else {
					printf("%s: %s\n", strerror(errno), filepath);
				}
				free(filepath);
			}
		} while(dent);
		closedir(dir);
	} else {
		printf("%s: %s\n", strerror(errno), rootpath);
	}
}

// Find all files recursively in the current directory matching the argument's name
int main(int argc, char **argv) {

	// Sanitize args
	char *search_file = NULL;
	char *search_dir = ".";
	char c;
	while((c = getopt(argc, argv, "hs:d:o:")) != -1) {
		switch(c) {
		case 'h':
			printf("Options: %s <search-file>\n"
				"\t-h                 Help\n"
				"\t-s <single-file>   Compare against single file\n"
				"\t-d <search-dir>    Search directory (defaults to current)\n"
				"\t-o <outfile-name>  Name for outfile file (default: differences.txt)\n",
				argv[0]);
			return 0;
		case 's':
			if(single_file_name) {
				printf("Must provide a single file for comparison!\n");
				return 0;
			}
			single_file_name = optarg;
			if(access(single_file_name, F_OK) == -1) {
				printf("File %s does not exist\n", single_file_name);
				return 0;
			} else {
				printf("Comparing against file: %s\n", single_file_name);
			}
			break;
		case 'd':
			if(strcmp(".", search_dir)) {
				printf("Must provide a single directory for searching!\n");
				return 0;
			}
			search_dir = optarg;
			struct stat s;
			if(stat(search_dir, &s) == -1) {
				printf("Directory %s does not exist\n", search_dir);
				return 0;
			} else if(!S_ISDIR(s.st_mode)) {
				printf("%s is not a directory\n", search_dir);
				return 0;
			} else {
				
			}
			break;
		case 'o':
			outname = optarg;
			break;
		case '?':
			printf("Invalid parameter: %s\n", argv[optind]);
			return 0;
		}
	}
	if(optind >= argc) {
		printf("Must provide a file to search directories for!\n"
			"Use -h to see usage options.\n");
		return 0;
	} else if(optind < argc - 1) {
		printf("Multiple non-command parameters!\n");
		return 0;
	} else {
		search_file = argv[optind];
		printf("Searching directories in %s for files named %s...\n",
			SEARCHDIRNAME(search_dir), search_file);
	}

	// Create list of files matching the input name
	list_t *file_list = list_create();
	find_files_recursively(search_dir, search_file, file_list);
	if(single_file_name && !list_contains(file_list, single_file_name, string_equal)) {
		list_push_front(file_list, strdup(single_file_name));
	}
	file_count = file_list->size;
	pair_count = single_file_name ? file_list->size - 1 : (file_list->size * (file_list->size - 1)) >> 1;
	if(!file_list->size) {
		printf("No files called \"%s\" found\n", search_file);
		exit(1);
	} else {
		printf("Found %d files in %s named \"%s\" (%d pairs)\n", file_list->size, SEARCHDIRNAME(search_dir), search_file, pair_count);
	}

	// Create matching of all file pairs
	file_pairs = malloc(pair_count * sizeof(pair_t));
	file_index = malloc(sizeof(char*) * file_list->size);
	typedef struct list_node {
		void *data;
		struct list_node *next;
	} node_t;
	node_t *node = (node_t*) file_list->head;
	int idx = 0;
	while(node) {
		file_index[idx++] = node->data;
		node = node->next;
	}
	idx = 0;
	if(single_file_name) {
		for(int i = 0; i < file_list->size; ++i) {
			if(strcmp(single_file_name, file_index[i])) {
				pair_t *p = file_pairs + idx++;
				p->file[0] = single_file_name;
				p->file[1] = file_index[i];
			}
		}
	} else {
		for(int i = 0; i < file_list->size; ++i) {
			for(int j = i + 1; j < file_list->size; ++j) {
				pair_t *p = file_pairs + idx++;
				p->file[0] = file_index[i];
				p->file[1] = file_index[j];
			}
		}
	}

	// Perform tests on each pair
	for(int i = 0; i < TEST_COUNT; ++i) {
		tests[i]();
	}

	// Sort pair scores
	qsort(file_pairs, pair_count, sizeof(pair_t), compare_pairs);

	// Print information about tests
	FILE *outfile = fopen(outname, "w");
	for(int i = 0; i < pair_count; ++i) {
		pair_t *p = file_pairs + i;
		fprintf(outfile, "Pair %d:\n", i);
		fprintf(outfile, "\t%s\n\t%s\n", p->file[0], p->file[1]);
		for(int j = 0; j < TEST_COUNT; ++j) {
			fprintf(outfile, "\t%s:\t%d\n", test_names[j], p->score[j]);
		}
	}
	fclose(outfile);
	printf("Wrote output to %s\n", outname);

	// Free up data structures and exit
	free(file_pairs);
	free(file_index);
	list_destroy(file_list, free);
	return 0;
}

