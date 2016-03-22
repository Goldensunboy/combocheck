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
	// dim 2: Which node is left of this index
	private static int[][] L;
	
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
			L = new int[Combocheck.FileList.size()][];
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
				L[index] = new int[nodeCount];
				for(int i = 0; i < keys.size(); ++i) {
					List<Integer> left = new ArrayList<Integer>();
					ParseTree node = ASTnodes[index][keys.get(i)];
					do {
						left.add(reverseMap.get(node));
						node = node.getChild(0);
					} while(node != null);
					int dest = left.get(left.size() - 1);
					for(int j = 0; j < left.size(); ++j) {
						L[index][left.get(j)] = dest;
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
				
				// Calculate tree distance, main loop
				int[][] tdist = new int[ASTnodes[idx1].length + 1]
						[ASTnodes[idx2].length + 1];
				for(int ip = 0; ip < keyroots[idx1].length; ++ip) {
					for(int jp = 0; jp < keyroots[idx2].length; ++jp) {
						int pos1 = keyroots[idx1][ip];
						int pos2 = keyroots[idx2][jp];
						
						// treedist(i,j) algorithm DP array initialization
						// cost of insertion/deletion/mutation is 1
						int bound1 = pos1 - L[idx1][pos1] + 2;
						int bound2 = pos2 - L[idx2][pos2] + 2;
						int[][] fdist = new int[bound1][bound2];
						fdist[0][0] = 0;
						for(int i = 1; i < bound1; ++i) {
							fdist[i][0] = fdist[i - 1][0];
						}
						for(int i = 1; i < bound2; ++i) {
							fdist[0][i] = fdist[0][i - 1];
						}
						
						// Forest distance table population
						for(int k = L[idx1][pos1], i = 1; k <= pos1;
								++k, ++i) {
							for(int l = L[idx2][pos2], j = 1; l <= pos2;
									++l, ++j) {
								if(L[idx1][k] == L[idx1][pos1] &&
										L[idx2][l] == L[idx2][pos2]) {
									int min = fdist[i - 1][j - 1];
									min = Math.min(min, fdist[i - 1][j]);
									min = Math.min(min, fdist[i][j - 1]);
									tdist[k][l] = fdist[i][j] = min;
								} else {
									int m = L[idx1][k] - L[idx1][pos1];
									int n = L[idx2][l] - L[idx2][pos2];
									int min = fdist[m][n] + tdist[k][l];
									min = Math.min(min, fdist[i - 1][j]);
									min = Math.min(min, fdist[i][j - 1]);
									fdist[i][j] = min;
								}
							}
						}
					}
				}
				
				distanceArray[index] = tdist[ASTnodes[idx1].length]
						[ASTnodes[idx2].length];
				System.out.println("Completed: " + index);
			}
		}
	}
}
