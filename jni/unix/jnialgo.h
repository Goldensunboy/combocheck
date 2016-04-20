/* jnialgo.h
 * This file contains definitions common between the source files for JNIalgo
 */

#include <pthread.h>
#include <vector>

using namespace std;

// Variables for incremental algorithm progress
extern int thread_count;
extern volatile int progress;
extern pthread_mutex_t progress_mutex;
extern int completed;
extern const char *current_check;
extern int checks_completed;
extern unsigned char halt;

// Variables for file lists accessible to algorithms
extern int file_count;
extern int pair_count;
extern char **file_names;
extern int *file_pairs;

// Prototypes for helper functions
int edit_distance_char(char *buf1, int size1, char *buf2, int size2);
int edit_distance_int(int *arr1, int size1, int *arr2, int size2);
vector<int> get_moss_fingerprint(char *buf, int size, int K, int W);
