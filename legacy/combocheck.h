/* Andrew Wilder
 * Summer 2015
 */

#ifndef COMBOCHECK_H
#define COMBOCHECK_H

#define THREAD_COUNT 8

enum test_type {
	EDCHECK = 0,
	KEYCHECK,
	TEST_COUNT
};

typedef struct {
	char *file[2];
	int score[TEST_COUNT];
} pair_t;

extern int pair_count, file_count;
extern pair_t *file_pairs;
extern char **file_index;
extern const char *test_names[TEST_COUNT];

#endif // COMBOCHECK_H

