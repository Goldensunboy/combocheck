package com.combocheck.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.combocheck.algo.Algorithm;
import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;

/**
 * This class represents the MOSS (Measure of Software Similarity) algorithm.
 * 
 * @author Andrew Wilder
 */
public class MossAlgorithm extends Algorithm {
	
	/** Moss parameters */
	private static int K = 15; // K-gram size
	private static int W = 8; // Winnowing window size
	
	/**
	 * Construct the default instance of MossAlgorithm
	 */
	public MossAlgorithm() {
		enabled = false;
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
		int[] scoreArray;
		
		// Use the JNI implementation if it is available
		if(JNIFunctions.isAvailable() && false) {
			scoreArray = JNIFunctions.JNIMoss();
		} else {
			scoreArray = new int[Combocheck.FilePairs.size()];
			
			// Preprocess the files
			List<Integer>[] fingerprints =
					new ArrayList[Combocheck.FileList.size()];
			Thread[] threadPool = new Thread[Combocheck.ThreadCount];
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new MossPreprocessingThread(fingerprints, i);
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
			
			// Compare fingerprints for all pairs
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new MossComparisonThread(scoreArray,
						fingerprints, i);
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
		for(int i = 0; i < scoreArray.length; ++i) {
			fileScores.put(Combocheck.PairOrdering.get(i), scoreArray[i]);
		}
	}
	
	/**
	 * This class represents the runnable thread implementation of preprocessing
	 * files for the Moss algorithm.
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class MossPreprocessingThread extends Thread {
		
		/** Locals specific to this thread object */
		private List<Integer>[] fingerprints;
		private int initialIndex;
		
		/**
		 * Construct a new moss preprocessing thread
		 * @param fingerprints Fingerprints for all files
		 * @param initialIndex Start index for striped processing
		 */
		public MossPreprocessingThread(List<Integer>[] fingerprints,
				int initialIndex) {
			this.fingerprints = fingerprints;
			this.initialIndex = initialIndex;
		}
		
		/**
		 * Perform Moss preprocessing on files
		 */
		@Override
		public void run() {
			for(int index = initialIndex; index < Combocheck.FileList.size();
					index += Combocheck.ThreadCount) {
				String fileString = LanguageUtils.GetNormalizedFile(
						Combocheck.FileOrdering.get(index));
				List<Integer> fingerprint = new ArrayList<Integer>();
				
				// If file size is less than K, fingerprint contains one val
				if(fileString.length() < K) {
					fingerprint.add(fileString.hashCode());
				} else {
					
					// Create the k-gram hashes
					int[] kgrams = new int[fileString.length() - K + 1];
					for(int i = 0; i < kgrams.length; ++i) {
						kgrams[i] = fileString.substring(i, i + K).hashCode();
					}
					
					// Create fingerprint
					int smallest = kgrams[0];
					int smallestIdx = 0;
					for(int i = 1; i < W; ++i) {
						if(kgrams[i] < smallest) {
							smallest = kgrams[i];
							smallestIdx = i;
						}
					}
					fingerprint.add(smallest);
					int current = smallest;
					int currentIdx = smallestIdx;
					for(int i = 1; i < kgrams.length - W + 1; ++i) {
						smallest = kgrams[i];
						smallestIdx = i;
						for(int j = 1; j < W; ++j) {
							if(kgrams[i + j] < smallest) {
								smallest = kgrams[i + j];
								smallestIdx = i + j;
							}
						}
						if(current > smallest || currentIdx <= i - W) {
							fingerprint.add(smallest);
							current = smallest;
							currentIdx = smallestIdx;
						}
					}
				}
				
				// Add the fingerprint to the array being constructed
				fingerprints[index] = fingerprint;
			}
		}
	}
	
	/**
	 * This class represents the runnable thread implementation of comparing
	 * fingerprints for file pairs
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class MossComparisonThread extends Thread {
		
		/** Locals specific to this thread object */
		private int[] scoreArray;
		private List<Integer>[] fingerprints;
		private int initialIndex;
		
		/**
		 * Construct a new edit distance calculation thread
		 * @param scoreArray The difference score for a pair's fingerprints
		 * @param pairDistance Results mapping of pairs onto edit distance
		 * @param initialIndex Start index for striped processing
		 */
		public MossComparisonThread(int[] scoreArray,
				List<Integer>[] fingerprints, int initialIndex) {
			this.scoreArray = scoreArray;
			this.fingerprints = fingerprints;
			this.initialIndex = initialIndex;
		}
		
		/**
		 * Perform comparison of fingerprints for all pairs
		 */
		@Override
		public void run() {
			for(int index = initialIndex; index < Combocheck.FilePairs.size();
					index += Combocheck.ThreadCount) {
				
				// Get the data arrays
				int idx1 = Combocheck.FilePairInts[index << 1];
				int idx2 = Combocheck.FilePairInts[(index << 1) + 1];
				List<Integer> fp1 = fingerprints[idx1];
				List<Integer> fp2 = fingerprints[idx2];
				
//				// Find number of fingerprint values in common
//				int score = 0;
//				for(int i = 0; i < fp1.size(); ++i) {
//					for(int j = 0; j < fp2.size(); ++j) {
//						if((int) fp1.get(i) == (int) fp2.get(j)) {
//							++score;
//						}
//					}
//				}
//				scoreArray[index] = score;
				
				// Find percentage difference
				Map<Integer, Integer> countMap =
						new HashMap<Integer, Integer>();
				for(Integer i : fp1) {
					Integer prev = countMap.get(i);
					countMap.put(i, prev == null ? 1 : prev + 1);
				}
				int unique2 = 0;
				for(Integer i : fp2) {
					Integer curr = countMap.get(i);
					if(curr == null || curr == 0) {
						++unique2;
					} else {
						countMap.put(i, curr - 1);
					}
				}
				int unique1 = 0;
				for(Integer i : countMap.values()) {
					unique1 += i;
				}
				int common = (fp1.size() + fp2.size() - unique1 - unique2) >> 1;
				double diff1 = (double) (common + unique1) / (common + unique1 +
						unique2);
				double diff2 = (double) (common + unique2) / (common + unique1 +
						unique2);
				scoreArray[index] = (int) ((diff1 + diff2) * 1000000000);
			}
		}
	}
}
