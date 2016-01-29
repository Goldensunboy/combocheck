/* Andrew Wilder
 * Spring 2015
 */

#ifndef LIST_H
#define LIST_H

// Prototype of a linked list node struct
struct list_node;

// Definition of a linked list
typedef struct {
	struct list_node *head;
	int size;
} list_t;

// Create a linked list
list_t *list_create();

// Push data to front of a linked list
void list_push_front(list_t *list, void *data);

// Free a linked list and its data
void list_destroy(list_t *list, void (*free_func)(void*));

// Determine if a linked list contains an element
int list_contains(list_t *list, void *data, int (*equal_func)(const void*, const void*));

#endif // LIST_H

