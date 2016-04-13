package com.combocheck.algo;

import java.util.Arrays;
import java.util.HashMap;

import org.antlr.v4.runtime.tree.ParseTree;

import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;

/**
 * This class represents the AST isomorphism algorithm.
 * Based off the Aho Hopcroft Ullman algorithm for rooted tree isomorphism
 * 
 * @author Andrew Wilder
 */
public class ASTIsomorphismAlgorithm extends Algorithm {

	/** Metadata about all AST tree traversals */
	private static ParseTree[] ASTroots;
	private static String[] CanonicalNames;
	
	/**
	 * Construct the default instance of ASTIsomorphismAlgorithm
	 */
	public ASTIsomorphismAlgorithm(boolean enabled) {
		super(enabled);
		settingsPanel = null;
	}
	
	/**
	 * Get the String representation of this algorithm for JComponents
	 */
	@Override
	public String toString() {
		return "AST Isomorphism";
	}
	
	/**
	 * Create the canonical name representing this tree structure
	 * @param node The node from which the name is being calculated
	 * @return The canonical name (see AHU paper)
	 */
	private static String GetCanonicalName(ParseTree node) {
		String ret = "";
		String[] children = new String[node.getChildCount()];
		for(int i = 0; i < children.length; ++i) {
			children[i] = GetCanonicalName(node.getChild(i));
		}
		Arrays.sort(children);
		for(int i = 0; i < children.length; ++i) {
			ret += children[i];
		}
		return "1" + ret + "0";
	}

	/**
	 * Analyze the file pairs using this algorithm
	 */
	@Override
	public void analyzeFiles() {
		int[] distanceArray;
		
		// Use the JNI implementation if it is available
		if(JNIFunctions.JNIEnabled()) {
			distanceArray = JNIFunctions.JNIASTIsomorphism();
		} else {
			distanceArray = new int[Combocheck.FilePairs.size()];
			
			// Preprocess the files
			completed = 0;
			progress = 0;
			updateCurrentCheckName("AST isomorphism preprocessing");
			
			ASTroots = new ParseTree[Combocheck.FileList.size()];
			CanonicalNames = new String[Combocheck.FileList.size()];
			Thread[] threadPool = new Thread[Combocheck.ThreadCount];
			
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new IsoPreprocessingThread(i);
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
			
			// Perform AST isomorphism check on pairs
			completed = 0;
			progress = 0;
			updateCurrentCheckName("AST isomorphism comparisons");
			
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new IsoComparisonThread(distanceArray, i);
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
		fileScores = new HashMap<FilePair, Integer>();
		for(int i = 0; i < distanceArray.length; ++i) {
			fileScores.put(Combocheck.PairOrdering.get(i), distanceArray[i]);
		}
	}
	
	/**
	 * This class represents the runnable thread implementation of preprocessing
	 * files for the AST isomorphism algorithm.
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class IsoPreprocessingThread extends Thread {
		
		/** Locals specific to this thread object */
		private int initialIndex;
		
		/**
		 * Construct a new isomorphism preprocessing thread
		 * @param initialIndex Start index for striped processing
		 */
		public IsoPreprocessingThread(int initialIndex) {
			this.initialIndex = initialIndex;
		}
		
		/**
		 * Perform ast preprocessing on files
		 */
		@Override
		public void run() {
			int fileCount = Combocheck.FileList.size();
			for(int index = initialIndex; index < fileCount;
					index += Combocheck.ThreadCount) {
				
				// Get the AST for this file
				ParseTree root = LanguageUtils.GetAST(
						Combocheck.FileOrdering.get(index));
				ASTroots[index] = root;
				if(root == null) {
					continue; // if the file didn't compile
				}
				
				// Get the canonical name for this AST
				CanonicalNames[index] = GetCanonicalName(root);
				
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
	 * files for the AST isomorphism algorithm.
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class IsoComparisonThread extends Thread {
		
		/** Locals specific to this thread object */
		private int[] distanceArray;
		private int initialIndex;
		
		/**
		 * Construct a new AST preprocessing thread
		 * @param distanceArray Array of comparison results per pair
		 * @param initialIndex Start index for striped processing
		 */
		public IsoComparisonThread(int[] distanceArray, int initialIndex) {
			this.initialIndex = initialIndex;
			this.distanceArray = distanceArray;
		}
		
		/**
		 * Perform comparison of ASTs for all pairs
		 */
		@Override
		public void run() {
			int pairCount = Combocheck.FilePairs.size();
			for(int index = initialIndex; index < pairCount;
					index += Combocheck.ThreadCount) {
				
				// Get the AST indices
				int idx1 = Combocheck.FilePairInts[index << 1];
				int idx2 = Combocheck.FilePairInts[(index << 1) + 1];
				if(ASTroots[idx1] == null || ASTroots[idx2] == null) {
					distanceArray[index] = Integer.MAX_VALUE;
					continue; // Infinite distance if no compile
				}
				String str1 = CanonicalNames[idx1];
				String str2 = CanonicalNames[idx2];
				
				// Calculate similarity as longest common substring
				distanceArray[index] = str1.equals(str2) ? 0 : 1;
				
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
