package com.combocheck.algo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.combocheck.global.FilePair;

/**
 * This interface provides the guarantee of particular comparison functions
 * and parameter option dialogs on a per-algorithm basis.
 *
 * @author Andrew Wilder
 */
public abstract class Algorithm {
	
	/** Global progress information */
	protected static int progress = 0;
	protected static int completed;
	private static String currentCheck = "";
	public static Semaphore progressMutex = new Semaphore(1);
	protected static int checksCompleted;
	protected static boolean processing = false;
	protected static boolean halt = false;
	
	/** Whether or not this algorithm is enabled */
	protected boolean enabled;
	protected JPanel settingsPanel = null;
	protected HashMap<FilePair, Integer> pairScores = new HashMap<FilePair,
			Integer>();
	
	/**
	 * Construct the Algorithm with an enabled value
	 * @param enabled Whether or not this algorithm will be used in the scan
	 */
	protected Algorithm(boolean enabled) {
		this.enabled = enabled;
	}
	
	/**
	 * Update the name of the current check being performed
	 * @param msg The name
	 */
	public static void updateCurrentCheckName(String msg) {
		currentCheck = msg;
	}
	
	/**
	 * Poll the progress of the current algorithm being run
	 * @return Percentage completion of the current algorithm stage
	 */
	public static int PollProgress() {
		return progress;
	}
	
	/**
	 * Get the name of the current algorithm check stage
	 * @return The name
	 */
	public static String GetCurrentCheck() {
		return currentCheck;
	}
	
	/**
	 * Tells if an algorithm is currently processing
	 * @return
	 */
	public static boolean isProcessing() {
		return processing;
	}
	
	/**
	 * Return the number of checks that have been completed by the scan
	 * @return How many checks have been completed
	 */
	public static int getChecksCompleted() {
		return checksCompleted;
	}
	
	/**
	 * Set the number of checks completed (usually to zero)
	 * @param checksCompleted Number of checks
	 */
	public static void setChecksCompleted(int checksCompleted) {
		Algorithm.checksCompleted = checksCompleted;
	}
	
	/**
	 * Halt the analysis.
	 * @param numScans Number of scans that must complete to cleanly exit
	 */
	public static void HaltAnalysis(int numScans) {
		// JNI or java implementation we are halting
		if(JNIFunctions.JNIEnabled()) {
			JNIFunctions.JNISetHalt(true);
			while(JNIFunctions.JNIPollChecksCompleted() < numScans ||
					processing) {
				Thread.yield();
			}
			JNIFunctions.JNISetHalt(false);
		} else {
			halt = true;
			while(checksCompleted < numScans || processing) {
				Thread.yield();
			}
			halt = false;
		}
	}
	
	/**
	 * Perform edit distance on two int arrays
	 * @param arr1 First array
	 * @param arr2 Second array
	 * @return The edit distance
	 */
	public static int EditDistance(int[] arr1, int[] arr2) {
		
		// Preprocessing
		int[][] D = new int[2][arr1.length + 1];
		for(int i = 0; i <= arr1.length; ++i) {
			D[0][i] = i;
		}
		D[1][0] = 1;
		
		// Do the algorithm
		for(int i = 1; i <= arr2.length; ++i) {
			for(int j = 1; j <= arr1.length; ++j) {
				int sub = D[(i + 1) & 1][j - 1];
				if(arr1[j - 1] != arr2[i - 1]) {
					int ins = D[(i + 1) & 1][j];
					int del = D[i & 1][j - 1];
					sub = sub < ins ? sub : ins;
					sub = sub < del ? sub : del;
					++sub;
				}
				D[i & 1][j] = sub;
			}
			D[(i + 1) & 1][0] = D[i & 1][0] + 1;
		}
		return D[arr2.length & 1][arr1.length];
	}
	public static int EditDistance(byte[] arr1, byte[] arr2) {
		
		// Preprocessing
		int[][] D = new int[2][arr1.length + 1];
		for(int i = 0; i <= arr1.length; ++i) {
			D[0][i] = i;
		}
		D[1][0] = 1;
		
		// Do the algorithm
		for(int i = 1; i <= arr2.length; ++i) {
			for(int j = 1; j <= arr1.length; ++j) {
				int sub = D[(i + 1) & 1][j - 1];
				if(arr1[j - 1] != arr2[i - 1]) {
					int ins = D[(i + 1) & 1][j];
					int del = D[i & 1][j - 1];
					sub = sub < ins ? sub : ins;
					sub = sub < del ? sub : del;
					++sub;
				}
				D[i & 1][j] = sub;
			}
			D[(i + 1) & 1][0] = D[i & 1][0] + 1;
		}
		return D[arr2.length & 1][arr1.length];
	}
	public static int EditDistance(String str1, String str2) {
		
		// Preprocessing
		int len1 = str1.length();
		int len2 = str2.length();
		int[][] D = new int[2][len1 + 1];
		for(int i = 0; i <= len1; ++i) {
			D[0][i] = i;
		}
		D[1][0] = 1;
		
		// Do the algorithm
		for(int i = 1; i <= len2; ++i) {
			for(int j = 1; j <= len1; ++j) {
				int sub = D[(i + 1) & 1][j - 1];
				if(str1.charAt(j - 1) != str2.charAt(i - 1)) {
					int ins = D[(i + 1) & 1][j];
					int del = D[i & 1][j - 1];
					sub = sub < ins ? sub : ins;
					sub = sub < del ? sub : del;
					++sub;
				}
				D[i & 1][j] = sub;
			}
			D[(i + 1) & 1][0] = D[i & 1][0] + 1;
		}
		return D[len2 & 1][len1];
	}
	
	/**
	 * Find length of the longest common substring of two strings
	 * @param str1 First string
	 * @param str2 Second string
	 * @return LCS length for the pair of strings
	 */
	public static int LCSLength(String str1, String str2) {
		
		// Preprocessing
		int len1 = str1.length();
		int len2 = str2.length();
		int ret = 0;
		int[][] D = new int[2][len2 + 1];
		Arrays.fill(D[0], 0);
		
		// Do the algorithm
		for(int i = 1; i <= len1; ++i) {
			D[i & 1][0] = 0;
			for(int j = 1; j <= len2; ++j) {
				if(str1.charAt(i - 1) == str2.charAt(j - 1)) {
					D[i & 1][j] = D[(i - 1) & 1][j - 1] + 1;
					ret = Math.max(ret, D[i & 1][j]);
				} else {
					D[i & 1][j] = 0;
				}
			}
		}
		return ret;
	}
	
	/**
	 * Return the components for this algorithm's settings dialog
	 */
	public JComponent getSettingsPanel() {
		return settingsPanel;
	}

	/**
	 * Return the enabled status for this algorithm
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled status for this algorithm
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	/**
	 * @return The mapping of file pairs onto analysis scores
	 */
	public HashMap<FilePair, Integer> getPairScores() {
		return pairScores;
	}
	
	/**
	 * Used in loading scans
	 * @param fileScores Saved pair score mapping
	 */
	public void setPairScores(HashMap<FilePair, Integer> pairScores) {
		this.pairScores = pairScores;
	}
	
	/**
	 * Analyze the file pairs using this algorithm
	 */
	public abstract void analyzeFiles();
}
