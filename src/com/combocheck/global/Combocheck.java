package com.combocheck.global;

import java.util.Collection;
import java.util.HashMap;

import com.combocheck.algo.Algorithm;
import com.combocheck.algo.EditDistanceAlgorithm;
import com.combocheck.algo.MossAlgorithm;
import com.combocheck.ui.CombocheckFrame;

/**
 * This class represents the main executable class of Combocheck.
 * It also houses static singleton variables, such as UI elements.
 * 
 * @author Andrew Wilder
 */
public class Combocheck {
	
	/** Combocheck constants */
	public static final String PROGRAM_TITLE = "Combocheck";
	public static final int PROGRAM_WIDTH = 900;
	public static final int PROGRAM_HEIGHT = 700;
	
	/** Combocheck globals */
	// List of all file pairs
	public static Collection<FilePair> FilePairs = null;
	
	// List of all files
	public static Collection<String> FileList = null;
	
	// Mapping of ints onto pairs for array-based optimizations
	public static HashMap<Integer, FilePair> FileOrdering = null;
	
	// List of algorithms
	public static final Algorithm algorithms[] = {
		new MossAlgorithm(),
		new EditDistanceAlgorithm()
	};
	
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
			new CombocheckFrame().setVisible(true);
			
		} else {
			
			for(String s : args) {
				System.out.println(s);
			}
		}
	}
	
	/**
	 * Perform the selected scans over the file pairs
	 */
	public static void performScans() {
		for(Algorithm a : algorithms) {
			if(a.isEnabled()) {
				a.analyzeFiles();
			}
		}
		// TODO change view to review panel
	}
}
