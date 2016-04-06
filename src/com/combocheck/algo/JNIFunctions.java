package com.combocheck.algo;

/**
 * This class represents the interface with libraries that implement the
 * algorithms
 *  
 * @author Andrew Wilder
 */
public final class JNIFunctions {

	/** Whether or not the JNI library is available */
	private static boolean libraryLoaded = false;
	
	// Load the JNI algorithm library
	static {
		try {
			System.loadLibrary("JNIalgo");
			libraryLoaded = true;
			System.out.println("Loaded JNI algorithm library");
		} catch(UnsatisfiedLinkError e) {
			System.out.println("Could not find JNI algorithm library");
		}
	}
	
	/**
	 * @return Whether or not the library for JNI functions is loaded
	 */
	public static boolean isAvailable() {
		return libraryLoaded;
	}
	
	/**
	 * Set the file names and pair data for the files to analyze in JNI
	 * @param files List of filename paths
	 * @param pairs Integers representing the indices of files to compare
	 *              Every pair of 2 numbers represents one file pair
	 */
	public static native void SetJNIFilePairData(String[] files, int[] pairs);
	
	/**
	 * Set the number of threads for the library to use for calculations
	 * @param threadCount
	 */
	public static native void SetJNIThreads(int threadCount);
	
	/**
	 * Poll the completion of the current algorithm
	 * @return Value 0 - 100 indicating progress percentage for current algo
	 */
	public static native int PollJNIProgress();
	
	/**
	 * Edit distance implementation
	 * @return Metrics per pair
	 */
	public static native int[] JNIEditDistance();
	
	/**
	 * Moss implementation
	 * @param K the k-gram size
	 * @param W the winnowing window size
	 * @return Metrics per pair
	 */
	public static native int[] JNIMoss(int K, int W);
	
	/**
	 * AST distance implementation
	 * @return Metrics per pair
	 */
	public static native int[] JNIASTDistance();
}
