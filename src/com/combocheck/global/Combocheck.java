package com.combocheck.global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.combocheck.algo.ASTIsomorphismAlgorithm;
import com.combocheck.algo.Algorithm;
import com.combocheck.algo.EditDistanceAlgorithm;
import com.combocheck.algo.JNIFunctions;
import com.combocheck.algo.MossAlgorithm;
import com.combocheck.algo.TokenDistanceAlgorithm;
import com.combocheck.ui.CombocheckFrame;

/**
 * This class represents the main executable class of Combocheck.
 * It also houses static singleton variables, such as UI elements.
 * 
 * @author Andrew Wilder
 */
public class Combocheck {
	
	/** Algorithms */
	public static final Algorithm algorithms[] = {
		new MossAlgorithm(),
		new TokenDistanceAlgorithm(),
		new ASTIsomorphismAlgorithm(),
		new EditDistanceAlgorithm()
	};
	
	/** Combocheck constants */
	public static final String PROGRAM_TITLE = "Combocheck";
	public static final int PROGRAM_WIDTH = 900;
	public static final int PROGRAM_HEIGHT = 700;
	
	/** Combocheck globals */
	private static CombocheckFrame frame = null;
	
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
			frame = new CombocheckFrame();
			frame.setVisible(true);
			
		} else {
			
			for(String s : args) {
				System.out.println(s);
			}
		}
		JNIFunctions.isAvailable(); // Statically pre-loads JNI library
	}
	
	/**
	 * Perform the selected scans over the file pairs
	 */
	public static void performScans() {
		
		long start_time = System.nanoTime();
		
		for(Algorithm a : algorithms) {
			if(a.isEnabled()) {
				a.analyzeFiles();
			}
		}
		
		long end_time = System.nanoTime();
		double difference = (end_time - start_time)/1e9;
		System.out.println("Analysis took: " + difference + " seconds");
		
		HashMap<FilePair, Integer> map = algorithms[1].getFileScores();
		System.out.println("entries: " + map.size());
		List<Map.Entry<FilePair, Integer>> scores = new ArrayList<Map.Entry<FilePair, Integer>>(map.entrySet());
		Collections.sort(scores, new Comparator<Map.Entry<FilePair, Integer>>() {
			@Override
			public int compare(Entry<FilePair, Integer> arg0,
					Entry<FilePair, Integer> arg1) {
				return arg1.getValue() - arg0.getValue();
			}
		});
		int i = 0;
		for(i = scores.size() - 100; i < scores.size(); ++i) {
			Map.Entry<FilePair, Integer> e = scores.get(i);
			System.out.println("Pair " + (scores.size() - i) + ":");
			System.out.println("\t" + e.getKey().getFile1());
			System.out.println("\t" + e.getKey().getFile2());
			System.out.println("\tvalue: " + e.getValue());
		}
		
		// TODO change view to review panel
		
		frame.getTabbedPane().getScanPanel().getScanControlPanel()
				.enableScanButton();
	}
}
