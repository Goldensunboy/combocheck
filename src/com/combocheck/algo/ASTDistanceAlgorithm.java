package com.combocheck.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.tree.ParseTree;

import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;

/**
 * This class represents the AST distance algorithm.
 * Based off the tree distance algorithm by Zhang and Shasha, 1989
 * 
 * @author Andrew Wilder
 */
public class ASTDistanceAlgorithm extends Algorithm {

	/** Metadata about all AST tree traversals */
	// dim 1: Which file's AST
	private static ParseTree[] ASTroots;
	// dim 1: Which file's nodes
	// dim 2: Which node, post-order traversal
	private static ParseTree[][] ASTnodes;
	// dim 1: Which file's key roots
	// dim 2: Which key root, index into ASTnodes
	private static int[][] keyroots;
	// dim 1: Which file's left sets
	// dim 2: Which left set, size is |keyroots[index]|
	// dim 3: Which node in the left set, index into ASTnodes
	private static int[][][] L;
	
	/**
	 * Construct the default instance of ASTDistanceAlgorithm
	 */
	public ASTDistanceAlgorithm() {
		enabled = true;
		// TODO construct settings dialog
	}
	
	/**
	 * Get the String representation of this algorithm for JComponents
	 */
	@Override
	public String toString() {
		return "AST Distance";
	}
	
	/**
	 * Get the size of an AST as a count of nodes
	 * @param node The starting node from which to measure size recursively
	 * @return The number of nodes in this AST
	 */
	private static int TreeSize(ParseTree node) {
		int sum = 1;
		for(int i = 0; i < node.getChildCount(); ++i) {
			sum += TreeSize(node.getChild(i));
		}
		return sum;
	}
	
	/**
	 * Populate the node array for a file in a postorder traversal
	 * @param node The starting node from which to traverse
	 * @param index The index into the AST list for which file this is
	 * @param start The starting index into the node array to insert found nodes
	 * @return The new starting index, used for recursion
	 */
	private static int PopulateNodes(ParseTree node, int index, int start,
			Map<ParseTree, Integer> reverseMap) {
		for(int i = 0; i < node.getChildCount(); ++i) {
			start = PopulateNodes(node.getChild(i), index, start, reverseMap);
		}
		ASTnodes[index][start] = node;
		if(reverseMap.containsKey(node)) {
			System.err.println(node.getText());
		}
		reverseMap.put(node, start);
		return start + 1;
	}

	/**
	 * Analyze the file pairs using this algorithm
	 */
	@Override
	public void analyzeFiles() {
		int[] distanceArray;
		
		// Use the JNI implementation if it is available
		if(JNIFunctions.isAvailable() && false) {
			distanceArray = JNIFunctions.JNIASTDistance();
		} else {
			distanceArray = new int[Combocheck.FilePairs.size()];
			
			// Preprocess the files
			ASTroots = new ParseTree[Combocheck.FileList.size()];
			ASTnodes = new ParseTree[Combocheck.FileList.size()][];
			keyroots = new int[Combocheck.FileList.size()][];
			L = new int[Combocheck.FileList.size()][][];
			Thread[] threadPool = new Thread[Combocheck.ThreadCount];
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new ASTPreprocessingThread(i);
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
			
			// Perform tree edit distance on pairs
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new ASTComparisonThread(distanceArray, i);
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
	 * This class represents the runnable thread implementation of preprocessing
	 * files for the AST distance algorithm.
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class ASTPreprocessingThread extends Thread {
		
		/** Locals specific to this thread object */
		private int initialIndex;
		
		/**
		 * Construct a new AST preprocessing thread
		 * @param roots The root nodes for the file ASTs
		 * @param keyroots The key root nodes for the tree distance algorithm
		 * @param initialIndex Start index for striped processing
		 */
		public ASTPreprocessingThread(int initialIndex) {
			this.initialIndex = initialIndex;
		}
		
		/**
		 * Perform ast preprocessing on files
		 */
		@Override
		public void run() {
			for(int index = initialIndex; index < Combocheck.FileList.size();
					index += Combocheck.ThreadCount) {
				
				// Get the AST for this file
				ParseTree root = LanguageUtils.GetAST(
						Combocheck.FileOrdering.get(index));
				ASTroots[index] = root;
				if(root == null) {
					continue; // if the file didn't compile
				}
				
				// Create post-ordering of nodes
				int nodeCount = TreeSize(root);
				ASTnodes[index] = new ParseTree[nodeCount];
				Map<ParseTree, Integer> reverseMap =
						new HashMap<ParseTree, Integer>();
				PopulateNodes(root, index, 0, reverseMap);
				
				// Get the key roots
				List<Integer> keys = new ArrayList<Integer>();
				for(int i = 0; i < ASTnodes[index].length; ++i) {
					ParseTree node = ASTnodes[index][i];
					if(node.getParent() == null ||
							node.getParent().getChild(0) != node) {
						keys.add(i);
					}
				}
				keyroots[index] = new int[keys.size()];
				for(int i = 0; i < keys.size(); ++i) {
					keyroots[index][i] = keys.get(i);
				}
				
				// Get the left sets
				L[index] = new int[keys.size()][];
				for(int i = 0; i < keys.size(); ++i) {
					List<Integer> left = new ArrayList<Integer>();
					ParseTree node = ASTnodes[index][keys.get(i)];
					do {
						left.add(reverseMap.get(node));
						node = node.getChild(0);
					} while(node != null);
					L[index][i] = new int[left.size()];
					for(int j = 0; j < left.size(); ++j) {
						L[index][i][j] = left.get(left.size() - j - 1);
					}
				}
				if(index == 0) {
					System.out.println(keys.size());
				}
			}
		}
	}
	
	/**
	 * This class represents the runnable thread implementation of comparing
	 * files for the AST distance algorithm.
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class ASTComparisonThread extends Thread {
		
		/** Locals specific to this thread object */
		private int[] distanceArray;
		private int initialIndex;
		
		/**
		 * Construct a new AST preprocessing thread
		 * @param roots The root nodes for the file ASTs
		 * @param keyroots The key root nodes for the tree distance algorithm
		 * @param initialIndex Start index for striped processing
		 */
		public ASTComparisonThread(int[] distanceArray, int initialIndex) {
			this.initialIndex = initialIndex;
			this.distanceArray = distanceArray;
		}
		
		/**
		 * Perform comparison of ASTs for all pairs
		 */
		@Override
		public void run() {
			for(int index = initialIndex; index < Combocheck.FilePairs.size();
					index += Combocheck.ThreadCount) {
				
				// Get the AST indices
				int idx1 = Combocheck.FilePairInts[index << 1];
				int idx2 = Combocheck.FilePairInts[(index << 1) + 1];
				if(ASTroots[idx1] == null || ASTroots[idx2] == null) {
					distanceArray[index] = Integer.MAX_VALUE;
					continue; // Infinite distance if no compile
				}
				
				
				
				
				
				
				distanceArray[index] = 0;
			}
		}
	}
}
