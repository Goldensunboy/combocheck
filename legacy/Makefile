# Makefile for Edit Distance Checker

PROGNAME = combocheck
SRC = $(wildcard *.c)
OBJ = $(SRC:%.c=%.o)
CFLAGS = -Wall -Werror -Wextra -std=gnu99 -pedantic -D _GNU_SOURCE -O3
LFLAGS = -pthread

all : $(OBJ)
	@echo "[LINK] Linking $(PROGNAME)"
	@gcc -o $(PROGNAME) $^ $(LFLAGS)

debug : CFLAGS += -DDEBUG -g
debug : all
	@echo "!! Built using debug flags !!"

%.o : %.c
	@echo "[COMPILE] Compiling $@"
	@gcc $(CFLAGS) -c $< -o $@

clean :
	@echo "[CLEAN] Removing all compiled files"
	@rm -f *.o *.txt $(PROGNAME)
