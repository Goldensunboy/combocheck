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
	 * Construct the default instance of MossAlgorithm
	 */
	public EditDistanceAlgorithm() {
		enabled = true;
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
				
				// Order them such that file 1 is larger
				if(file2.length > file1.length) {
					byte[] temp = file1;
					file1 = file2;
					file2 = temp;
				}
				
				// Do edit distance
				int[][] D = new int[2][file1.length + 1];
				for(int i = 0; i <= file1.length; ++i) {
					D[0][i] = i;
				}
				D[1][0] = 1;
				for(int i = 1; i <= file2.length; ++i) {
					for(int j = 1; j <= file1.length; ++j) {
						int sub = D[(i + 1) & 1][j - 1];
						if(file1[j - 1] != file2[i - 1]) {
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
				
				// Edit distance is at the end of the array
				distanceArray[index] = D[file2.length & 1][file1.length];
			}
		}
	}
}
