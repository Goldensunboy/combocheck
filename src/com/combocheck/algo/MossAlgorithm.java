package com.combocheck.algo;

import com.combocheck.algo.Algorithm;

/**
 * This class represents the MOSS (Measure of Software Similarity) algorithm.
 * 
 * @author Andrew Wilder
 */
public class MossAlgorithm extends Algorithm {
	
	/**
	 * Construct the default instance of MossAlgorithm
	 */
	public MossAlgorithm() {
		enabled = true;
		// TODO construct settings dialog
	}
	
	/**
	 * Get the String representation of this algorithm for JComponents
	 */
	@Override
	public String toString() {
		return "Moss";
	}

	/**
	 * Analyze the file pairs using this algorithm
	 */
	@Override
	public void analyzeFiles() {
		// TODO if DLL exists, use JNI version
		// TODO algorithm 
	}
}
