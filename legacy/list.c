/* Andrew Wilder
 * Spring 2015
 */

#include <stdlib.h>

#include "list.h"

// Definition of a linked list node
struct list_node {
	void *data;
	struct list_node *next;
};

// Create a linked list node
static struct list_node *list_node_create(void *data) {
	struct list_node *n = malloc(sizeof(struct list_node));
	n->data = data;
	return n;
}

// Create a linked list
list_t *list_create() {
	list_t *l = malloc(sizeof(list_t));
	l->head = NULL;
	l->size = 0;
	return l;
}

// Push data to front of a linked list
void list_push_front(list_t *list, void *data) {
	struct list_node *n = list_node_create(data);
	n->next = list->head;
	list->head = n;
	++list->size;
}

// Free a linked list and its data
void list_destroy(list_t *list, void (*free_func)(void*)) {
	while(list->head) {
		struct list_node *n = list->head;
		list->head = list->head->next;
		free_func(n->data);
		free(n);
	}
	free(list);
}

// Does this list contain an element?
int list_contains(list_t *list, void *data, int (*equal_func)(const void*, const void*)) {
	int result = 0;
	struct list_node *n = list->head;
	while(n) {
		if((result = equal_func(n->data, data))) {
			break;
		} else {
			n = n->next;
		}
	}
	return result;
}

