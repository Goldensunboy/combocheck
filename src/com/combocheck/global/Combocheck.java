package com.combocheck.global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

import javax.swing.JDialog;

import com.combocheck.algo.ASTIsomorphismAlgorithm;
import com.combocheck.algo.Algorithm;
import com.combocheck.algo.EditDistanceAlgorithm;
import com.combocheck.algo.JNIFunctions;
import com.combocheck.algo.MossAlgorithm;
import com.combocheck.algo.TokenDistanceAlgorithm;
import com.combocheck.ui.CombocheckFrame;
import com.combocheck.ui.ProgressDialog;

/**
 * This class represents the main executable class of Combocheck.
 * It also houses static singleton variables, such as UI elements.
 * 
 * @author Andrew Wilder
 */
public class Combocheck {
	
	/** Algorithms */
	public static final Algorithm algorithms[] = {
		new MossAlgorithm(true),
		new TokenDistanceAlgorithm(false),
		new ASTIsomorphismAlgorithm(true),
		new EditDistanceAlgorithm(false)
	};
	
	/** Combocheck constants */
	public static final String PROGRAM_TITLE = "Combocheck";
	public static final int PROGRAM_WIDTH = 1100;
	public static final int PROGRAM_HEIGHT = 700;
	
	/** Combocheck globals */
	public static CombocheckFrame Frame = null;
	
	// List of all file pairs
	public static List<FilePair> FilePairs = null;
	public static int[] FilePairInts = null;
	
	// List of all files
	public static List<String> FileList = null;
	
	// Mapping of ints onto files for array-based optimizations
	public static HashMap<Integer, String> FileOrdering = null;
	
	// Mapping of ints onto pairs for array-based optimizations
	public static HashMap<Integer, FilePair> PairOrdering = null;
	
	// How many threads to run concurrently for analysis
	public static int ThreadCount = 8;

	/**
	 * Create the Combocheck frame and initialize UI elements.
	 * If command line arguments are passed, handle them here.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Determine if command line arguments were passed
		if(args.length < 2) {
			
			// Normal invocation of Combocheck UI
			Frame = new CombocheckFrame();
			Frame.setVisible(true);
			
		} else {
			
			for(String s : args) {
				System.out.println(s);
			}
		}
		JNIFunctions.JNIEnabled(); // Statically pre-loads JNI library
	}
	
	/**
	 * Perform the selected scans over the file pairs
	 */
	public static void PerformScans() {
		
		// Start scans
		if(JNIFunctions.JNIEnabled()) {
			JNIFunctions.JNIClearChecksCompleted();
			JNIFunctions.SetJNIThreads(ThreadCount);
		} else {
			Algorithm.setChecksCompleted(0);
		}
		new ScanThread().start();
		
		// Create progress dialog
		JDialog pd = new ProgressDialog();
		pd.setVisible(true);
		Combocheck.Frame.revalidate();
	}
	
	/**
	 * This function will cancel a running scan.
	 * @param numScans Number of scans to complete
	 */
	public static void CancelScan(int numScans) {
		Algorithm.HaltAnalysis(numScans);
	}
	
	/**
	 * This is a class meant to run the scans on a different thread from
	 * Combocheck's UI
	 * 
	 * @author Andrew Wilder
	 */
	private static class ScanThread extends Thread {
		@Override
		public void run() {
			for(Algorithm a : algorithms) {
				if(a.isEnabled()) {
					long start_time = System.nanoTime();
					a.analyzeFiles();
					long end_time = System.nanoTime();
					double difference = (end_time - start_time) / 1e9;
					System.out.println("Completed scan \"" + a.toString() +
							"\" in " + difference + " seconds");
				}
			}
		}
	}
	
	/**
	 * Print score statistics, ordered by a sum of squares of algorithm scores
	 * after normalization per algorithm for equal weighting
	 */
	public static void printStatistics() {
		
		// How many algorithms are enabled?
		int enabled = 0;
		for(Algorithm a : algorithms) {
			if(a.isEnabled()) {
				++enabled;
			}
		}
		
		// Get the algorithm averages
		double[] avg = new double[enabled];
		int idx = 0;
		for(Algorithm a : algorithms) {
			if(a.isEnabled()) {
				double sum = 0;
				for(Map.Entry<FilePair, Integer> e :
						a.getPairScores().entrySet()) {
					sum += e.getValue();
				}
				avg[idx++] = sum / a.getPairScores().entrySet().size();
			}
		}
		
		// Get the normalization factors
		double[] norm = new double[enabled];
		double avgSum = 0;
		for(double d : avg) {
			avgSum += d;
		}
		double avgavg = avgSum / avg.length;
		for(int i = 0; i < norm.length; ++i) {
			norm[i] = avgavg / avg[i];
		}
		
		// Sort the file pairs
		List<FilePair> fps = new ArrayList<FilePair>(FilePairs);
		Comparator<FilePair> comp = new SquareComparator(norm);
		Collections.sort(fps, comp);
		
		// Print out the statistics for the files
		idx = 0;
		for(FilePair fp : fps) {
			System.out.println("Pair " + idx++ + ":");
			System.out.println("\t" + fp.getShortenedFile1());
			System.out.println("\t" + fp.getShortenedFile2());
			for(Algorithm a : algorithms) {
				if(a.isEnabled()) {
					Map<FilePair, Integer> m = a.getPairScores();
					System.out.println("\t" + a + ": " + m.get(fp));
				}
			}
			System.out.println();
		}
	}
	
	/**
	 * This class represents a Comparator that will use squares of scores to
	 * compare files, after normalizing scores using a normalization factor per
	 * algorithm.
	 * 
	 * @author Andrew Wilder
	 */
	private static class SquareComparator implements Comparator<FilePair> {

		/** Normalization factors */
		private double[] norm;
		
		/**
		 * Create a new SqaureComparator with a given normalization vector
		 * @param norm The normalization vector
		 */
		public SquareComparator(double[] norm) {
			this.norm = norm;
		}
		
		/**
		 * Compare two FilePair objects using their scores from the algorithms
		 */
		@Override
		public int compare(FilePair fp1, FilePair fp2) {
			double score1 = 0, score2 = 0;
			int idx = 0;
			for(Algorithm a : algorithms) {
				if(a.isEnabled()) {
					Map<FilePair, Integer> m = a.getPairScores();
					score1 += Math.pow(m.get(fp1) * norm[idx], 2);
					score2 += Math.pow(m.get(fp2) * norm[idx++], 2);
				}
			}
			return (int) (score1 - score2);
		}
	}
}
