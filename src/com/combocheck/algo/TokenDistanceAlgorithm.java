package com.combocheck.algo;

import java.util.HashMap;

import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;

public class TokenDistanceAlgorithm extends Algorithm {
	
	/** Holds the ANTLR-generated token ID arrays for all files */
	private static int[][] tokenArr;

	/**
	 * Construct the default instance of ASTDistanceAlgorithm
	 */
	public TokenDistanceAlgorithm(boolean enabled) {
		super(enabled);
		settingsPanel = null;
	}
	
	/**
	 * Get the String representation of this algorithm for JComponents
	 */
	@Override
	public String toString() {
		return "Token Distance";
	}

	/**
	 * Analyze the file pairs using this algorithm
	 */
	@Override
	public void analyzeFiles() {
		int[] distanceArray;
		
		// Use the JNI implementation if it is available
		if(JNIFunctions.JNIEnabled()) {
			distanceArray = JNIFunctions.JNITokenDistance();
		} else {
			distanceArray = new int[Combocheck.FilePairs.size()];
			
			// Preprocess the files
			tokenArr = new int[Combocheck.FileList.size()][];
			Thread[] threadPool = new Thread[Combocheck.ThreadCount];
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new TokenPreprocessingThread(i);
				threadPool[i].start();
			}
			try {
				for(int i = 0; i < Combocheck.ThreadCount; ++i) {
					threadPool[i].join();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			
			// Perform tree edit distance on pairs
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new TokenComparisonThread(distanceArray, i);
				threadPool[i].start();
			}
			try {
				for(int i = 0; i < Combocheck.ThreadCount; ++i) {
					threadPool[i].join();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
		
		// Construct the pair scores mapping
		fileScores = new HashMap<FilePair, Integer>();
		for(int i = 0; i < distanceArray.length; ++i) {
			fileScores.put(Combocheck.PairOrdering.get(i), distanceArray[i]);
		}
	}
	
	/**
	 * This class represents the runnable thread implementation of preprocessing
	 * files for the token distance algorithm.
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class TokenPreprocessingThread extends Thread {
		
		/** Locals specific to this thread object */
		private int initialIndex;
		
		/**
		 * Construct a new Token preprocessing thread
		 * @param initialIndex Start index for striped processing
		 */
		public TokenPreprocessingThread(int initialIndex) {
			this.initialIndex = initialIndex;
		}
		
		/**
		 * Perform token preprocessing on files
		 */
		@Override
		public void run() {
			for(int index = initialIndex; index < Combocheck.FileList.size();
					index += Combocheck.ThreadCount) {
				tokenArr[index] = LanguageUtils.GetTokenIDs(
						Combocheck.FileList.get(index));
			}
		}
	}
	
	/**
	 * This class represents the runnable thread implementation of comparing
	 * files for the token distance algorithm.
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class TokenComparisonThread extends Thread {
		
		/** Locals specific to this thread object */
		private int[] distanceArray;
		private int initialIndex;
		
		/**
		 * Construct a new token preprocessing thread
		 * @param initialIndex Start index for striped processing
		 */
		public TokenComparisonThread(int[] distanceArray, int initialIndex) {
			this.initialIndex = initialIndex;
			this.distanceArray = distanceArray;
		}
		
		/**
		 * Perform comparison of tokens for all pairs
		 */
		@Override
		public void run() {
			for(int index = initialIndex; index < Combocheck.FilePairs.size();
					index += Combocheck.ThreadCount) {
				
				// Get the data arrays
				int idx1 = Combocheck.FilePairInts[index << 1];
				int idx2 = Combocheck.FilePairInts[(index << 1) + 1];
				
				// Do edit distance
				distanceArray[index] = Algorithm.EditDistance(
						tokenArr[idx1], tokenArr[idx2]);
			}
		}
	}
}
