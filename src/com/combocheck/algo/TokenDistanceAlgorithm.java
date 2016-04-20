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
		processing = true;
		int[] distanceArray;
		
		// Use the JNI implementation if it is available
		if(JNIFunctions.JNIEnabled()) {
			distanceArray = JNIFunctions.JNITokenDistance();
		} else {
			distanceArray = new int[Combocheck.FilePairs.size()];
			
			// Preprocess the files
			completed = 0;
			progress = 0;
			updateCurrentCheckName("Token distance preprocessing");
			
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
			++checksCompleted;
			
			// Perform token distance on pairs
			completed = 0;
			progress = 0;
			updateCurrentCheckName("Token distance comparisons");
			
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
			++checksCompleted;
		}
		
		// Construct the pair scores mapping
		pairScores = new HashMap<FilePair, Integer>();
		for(int i = 0; i < distanceArray.length; ++i) {
			pairScores.put(Combocheck.PairOrdering.get(i), distanceArray[i]);
		}
		processing = false;
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
			int fileCount = Combocheck.FileList.size();
			for(int index = initialIndex; !halt && index < fileCount;
					index += Combocheck.ThreadCount) {
				
				// Create the token ID array
				tokenArr[index] = LanguageUtils.GetTokenIDs(
						Combocheck.FileList.get(index));
				
				// Update progress
				try {
					progressMutex.acquire();
					progress = 100 * ++completed / fileCount;
					progressMutex.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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
			int pairCount = Combocheck.FilePairs.size();
			for(int index = initialIndex; !halt && index < pairCount;
					index += Combocheck.ThreadCount) {
				
				// Get the data arrays
				int idx1 = Combocheck.FilePairInts[index << 1];
				int idx2 = Combocheck.FilePairInts[(index << 1) + 1];
				
				// Do edit distance
				distanceArray[index] = Algorithm.EditDistance(
						tokenArr[idx1], tokenArr[idx2]);
				
				// Update progress
				try {
					progressMutex.acquire();
					progress = 100 * ++completed / pairCount;
					progressMutex.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
