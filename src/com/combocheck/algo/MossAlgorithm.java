package com.combocheck.algo;

import java.util.HashMap;

import com.combocheck.algo.Algorithm;
import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;

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
		int[] scoreArray;
		
		// Use the JNI implementation if it is available
		if(JNIFunctions.isAvailable() && false) {
			scoreArray = JNIFunctions.JNIMoss();
		} else {
			scoreArray = new int[Combocheck.FilePairs.size()];
			
			// Preprocess the files
			// TODO actually this should be an array of fingerprints
			String[] normalizedFiles = new String[Combocheck.FileList.size()];
			Thread[] threadPool = new Thread[Combocheck.ThreadCount];
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new MossPreprocessingThread(normalizedFiles, i);
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
			HashMap<String, String> normalizationMap = new HashMap<String,
					String>();
			for(int i = 0; i < Combocheck.FileList.size(); ++i) {
				normalizationMap.put(Combocheck.FileOrdering.get(i),
						normalizedFiles[i]);
			}
			
			// TODO compare fingerprints for all pairs
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
		private String[] normalizedFiles;
		private int initialIndex;
		
		/**
		 * Construct a new edit distance calculation thread
		 * @param pairDistance Results mapping of pairs onto edit distance
		 * @param initialIndex Start index for striped processing
		 */
		public MossPreprocessingThread(String[] normalizedFiles, int initialIndex) {
			this.normalizedFiles = normalizedFiles;
			this.initialIndex = initialIndex;
		}
		
		/**
		 * Perform Moss preprocessing on files
		 */
		@Override
		public void run() {
			for(int index = initialIndex; index < Combocheck.FileList.size();
					index += Combocheck.ThreadCount) {
				normalizedFiles[index] = LanguageUtils.GetNormalizedFile(
						Combocheck.FileOrdering.get(index));
			}
		}
	}
}
