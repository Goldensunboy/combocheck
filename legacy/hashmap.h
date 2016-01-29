/* Andrew Wilder */

// The metadata structure used by the hashmap. Not visible to the user.
struct hashmap_metadata;

// The hashmap structure visible to the user.
typedef struct hashmap {
	int size;
	double load_factor;
	struct hashmap_metadata *meta;
} hashmap_t;

// The collision policy types for the hashmap.
enum collision_policy_t {
	CP_UPDATE_ENTRY,
	CP_FREE_OLD_ENTRY
};

// Definitions of the function types used by the hashmap, for simplicity.
typedef int (*hash_func_t)(const void*);
typedef int (*equal_func_t)(const void*, const void*);
typedef void (*free_func_t)(void*);

/* create_hashmap
 * Create a new hashmap.
 * @param hfunc The hash function for indexing key values
 * @param efunc The equality function for comparing keys
 * @param kffunc The function for freeing keys
 * @param vffunc The function for freeing values
 * @param cpolicy The policy for when a key already exists in the map
 * @return A pointer to the initialized hashmap structure on the heap
 */
struct hashmap *hashmap_create(hash_func_t hfunc, equal_func_t efunc, free_func_t kffunc, free_func_t vffunc, enum collision_policy_t cpolicy);

/* hashmap_put
 * Put a key-value pair into the hashmap.
 * @param hmap The hashmap structure to put a key-value pair into
 * @param key The key
 * @param value The value
 * @return 1 if the put operation resulted in a collision, 0 if new key
 */
int hashmap_put(struct hashmap *hmap, void *key, void *value);

/* hashmap_get
 * Get a value from the hashmap.
 * @param hmap The hashmap structure to get a value from
 * @param key The key mapping to the value to get
 * @return NULL if not found, or the mapped value if found
 */
void *hashmap_get(struct hashmap *hmap, void *key);

/* hashmap_destroy
 * Free everything in a hashmap and free the hashmap itself.
 * @param hmap The hashmap to destroy
 */
void hashmap_destroy(struct hashmap *hmap);

