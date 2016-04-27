# Makefile for building the Combocheck JNI library for unix

LIBNAME = libJNIalgo.so
SRC = $(wildcard *.cpp)
OBJ = $(SRC:%.cpp=%.o)
JNIFLAGS = -I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/linux"
CFLAGS = -std=c++11 -fpic -O2
LFLAGS = -pthread
CC = g++
LD = g++

all : JNIheader $(OBJ)
	@echo "[LD] Linking $(LIBNAME)"
	@$(CC) -shared -fpic $(JNIFLAGS) -o $(LIBNAME) $(OBJ) $(LFLAGS)
	@cp $(LIBNAME) ../..

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

