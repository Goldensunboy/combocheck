/* Andrew Wilder */

#include <stdlib.h>
#include <string.h>
#include "hashmap.h"

#define INITIAL_SIZE 10
#define LOAD_FACTOR_THRESHOLD 0.75

// Definition of a hashmap bucket, which functions as a list with a key-value pair
struct hashmap_bucket {
	void *key;
	void *value;
	struct hashmap_bucket *next;
};

// Definition of hashmap metadata, which contains functions and bucket information
struct hashmap_metadata {
	struct hashmap_bucket *bucket_arr;
	int bucket_count;
	hash_func_t hash_func;
	equal_func_t equal_func;
	free_func_t key_free_func;
	free_func_t value_free_func;
	enum collision_policy_t collision_policy;
};

// Create a new hashmap
struct hashmap *hashmap_create(hash_func_t hfunc, equal_func_t efunc, free_func_t kffunc, free_func_t vffunc, enum collision_policy_t cpolicy) {
	struct hashmap *hmap = calloc(1, sizeof(struct hashmap));
	hmap->meta = malloc(sizeof(struct hashmap_metadata));
	hmap->meta->bucket_arr = calloc(INITIAL_SIZE, sizeof(struct hashmap_bucket));
	hmap->meta->bucket_count = INITIAL_SIZE;
	hmap->meta->hash_func = hfunc;
	hmap->meta->equal_func = efunc;
	hmap->meta->key_free_func = kffunc;
	hmap->meta->value_free_func = vffunc;
	hmap->meta->collision_policy = cpolicy;
	return hmap;
}

// Resize a hashmap by n * 2
static void hashmap_resize(struct hashmap *hmap) {

	// Create a list of elements already in the hashmap
	struct hashmap_bucket head;
	head.next = NULL;
	struct hashmap_bucket *temp_curr = &head;

	// Traverse buckets, free old linked list nodes
	struct hashmap_bucket *buck_curr;
	for(int i = 0; i < hmap->meta->bucket_count; ++i) {
		buck_curr = hmap->meta->bucket_arr[i].next;
		while(buck_curr) {
			temp_curr->next = calloc(1, sizeof(struct hashmap_bucket));
			temp_curr = temp_curr->next;
			temp_curr->key = buck_curr->key;
			temp_curr->value = buck_curr->value;
			struct hashmap_bucket *old_temp_curr = buck_curr;
			buck_curr = buck_curr->next;
			free(old_temp_curr);
		}
	}

	// Update hashmap metadata
	hmap->size = 0;
	hmap->meta->bucket_count <<= 1;
	hmap->meta->bucket_arr = realloc(hmap->meta->bucket_arr, hmap->meta->bucket_count * sizeof(struct hashmap_bucket));
	memset(hmap->meta->bucket_arr, 0, hmap->meta->bucket_count * sizeof(struct hashmap_bucket));

	// Repopulate hashmap, free temp list
	temp_curr = head.next;
	while(temp_curr) {
		hashmap_put(hmap, temp_curr->key, temp_curr->value);
		struct hashmap_bucket *old_temp_curr = temp_curr;
		temp_curr = temp_curr->next;
		free(old_temp_curr);
	}
}

// Put a key-value pair into the hashmap
int hashmap_put(struct hashmap *hmap, void *key, void *value) {
	int collision = 0;

	// Get the bucket this key hashes to
	int index = hmap->meta->hash_func(key) % hmap->meta->bucket_count;
	struct hashmap_bucket *curr = hmap->meta->bucket_arr + index;

	// Traverse to the entry that either contains the key given,
	// or marks the end of the bucket
	while(curr->next) {
		curr = curr->next;
		if(hmap->meta->equal_func(key, curr->key)) {
			collision = 1;
			break;
		}
	}

	// If there is a collision, update existing key-value
	// pair based on collision policy
	if(collision) {
		if(hmap->meta->collision_policy == CP_FREE_OLD_ENTRY) {
			hmap->meta->key_free_func(curr->key);
			hmap->meta->value_free_func(curr->value);
		}
	} else {
		curr = malloc(sizeof(struct hashmap_bucket));
		curr->next = hmap->meta->bucket_arr[index].next;
		hmap->meta->bucket_arr[index].next = curr;
	}

	// Set the new key-value pair
	curr->key = key;
	curr->value = value;

	// If there was no collision, update size and load factor
	if(!collision) {
		if(++hmap->size / (double) hmap->meta->bucket_count > LOAD_FACTOR_THRESHOLD) {
			hashmap_resize(hmap);
		}
		hmap->load_factor = hmap->size / (double) hmap->meta->bucket_count;
	}
	return collision;
}

// Get a value from a hashmap key
void *hashmap_get(struct hashmap *hmap, void *key) {
	void *value = NULL;

	// Get the bucket this key hashes to
	int index = hmap->meta->hash_func(key) % hmap->meta->bucket_count;
	struct hashmap_bucket *curr = hmap->meta->bucket_arr + index;

	// Traverse to the entry that either contains the key given,
	// or marks the end of the bucket
	while(curr->next) {
		curr = curr->next;
		if(hmap->meta->equal_func(key, curr->key)) {
			value = curr->value;
			break;
		}
	}

	// If the bucket contains the key, return its value
	return value;
}

// Destroy the hashmap contents
void hashmap_destroy(struct hashmap *hmap) {

	// Free each bucket
	struct hashmap_bucket *buck_curr;
	for(int i = 0; i < hmap->meta->bucket_count; ++i) {
		buck_curr = hmap->meta->bucket_arr[i].next;
		while(buck_curr) {
			hmap->meta->key_free_func(buck_curr->key);
			hmap->meta->value_free_func(buck_curr->value);
			struct hashmap_bucket *old_temp_curr = buck_curr;
			buck_curr = buck_curr->next;
			free(old_temp_curr);
		}
	}

	// Free data structures in the hashmap
	free(hmap->meta->bucket_arr);
	free(hmap->meta);
	free(hmap);
}

