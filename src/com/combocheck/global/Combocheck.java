package com.combocheck.global;

import java.util.HashMap;
import java.util.List;

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
			Algorithm.checksCompleted = 0;
		}
		new ScanThread().start();
		
		// Create progress dialog
		JDialog pd = new ProgressDialog();
		pd.setVisible(true);
		Combocheck.Frame.revalidate();
	}
	
	/**
	 * This function will cancel a running scan.
	 */
	public static void CancelScan() {
		// TODO this will be tricky since Thread.stop() is deprecated
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
}
