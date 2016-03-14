# Makefile for building the Combocheck JNI library for unix

LIBNAME = libJNIalgo.so
SRC = $(wildcard *.cpp)
OBJ = $(SRC:%.cpp=%.o)
JAVAHOME = /usr/lib/jvm/java-7-openjdk
JNIFLAGS = -I"$(JAVAHOME)/include" -I"$(JAVAHOME)/include/linux"
CFLAGS = -std=c++11 -fpic -O3
LFLAGS = -pthread
CC = g++
LD = g++

all : JNIheader $(OBJ)
	@echo "[LD] Linking $(LIBNAME)"
	@$(CC) -shared -fpic $(JNIFLAGS) -o $(LIBNAME) $(OBJ) $(LFLAGS)

JNIheader :
	@echo "[HD] Generating JNI header"
	@bash -c "cd ../../bin; javah com.combocheck.algo.JNIFunctions"
	@mv ../../bin/com_combocheck_algo_JNIFunctions.h .

%.o : %.cpp
	@echo "[CC] Compiling $@"
	@$(CC) $(JNIFLAGS) $(CFLAGS) -c $< -o $@

clean :
	@echo "[CL] Removing all compiled files"
	@rm -f com_combocheck_algo_JNIFunctions.h $(OBJ) $(LIBNAME)

rebuild : clean all
	sudo mv libJNIalgo.so /usr/lib

