package com.combocheck.algo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import com.combocheck.algo.Algorithm;
import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;

/**
 * This class represents the edit distance algorithm.
 * 
 * @author Andrew Wilder
 */
public class EditDistanceAlgorithm extends Algorithm {
	
	/**
	 * Construct the default instance of EditDistanceAlgorithm
	 */
	public EditDistanceAlgorithm() {
		enabled = false;
		// TODO construct settings dialog
	}
	
	/**
	 * Get the String representation of this algorithm for JComponents
	 */
	@Override
	public String toString() {
		return "Edit Distance";
	}

	/**
	 * Analyze the file pairs using this algorithm
	 */
	@Override
	public void analyzeFiles() {
		int[] distanceArray;
		
		// Use the JNI implementation if it is available
		if(JNIFunctions.isAvailable()) {
			distanceArray = JNIFunctions.JNIEditDistance();
		} else {
			distanceArray = new int[Combocheck.FilePairs.size()];
			
			// Run the Java implementation in several threads
			Thread[] threadPool = new Thread[Combocheck.ThreadCount];
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new EditDistanceThread(distanceArray, i);
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
	 * This class represents the runnable thread implementation of running the
	 * edit distance algorithm on several file pairs.
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class EditDistanceThread extends Thread {
		
		/** Locals specific to this thread object */
		private int[] distanceArray;
		private int initialIndex;
		
		/**
		 * Construct a new edit distance calculation thread
		 * @param pairDistance Results mapping of pairs onto edit distance
		 * @param initialIndex Start index for striped processing
		 */
		public EditDistanceThread(int[] distanceArray, int initialIndex) {
			this.distanceArray = distanceArray;
			this.initialIndex = initialIndex;
		}
		
		/**
		 * Perform edit distance on file pairs
		 */
		@Override
		public void run() {
			for(int index = initialIndex; index < Combocheck.FilePairs.size();
					index += Combocheck.ThreadCount) {
				
				// Read the data as a byte array from the 2 files
				FilePair pair = Combocheck.PairOrdering.get(index);
				byte[] file1, file2;
				try {
					file1 = Files.readAllBytes(new File(
							pair.getFile1()).toPath());
					file2 = Files.readAllBytes(new File(
							pair.getFile2()).toPath());
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
				
				// Calculate edit distance
				distanceArray[index] = Algorithm.EditDistance(file1, file2);
			}
		}
	}
}
