package com.combocheck.algo;

import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.combocheck.global.FilePair;

/**
 * This interface provides the guarantee of particular comparison functions
 * and parameter option dialogs on a per-algorithm basis.
 *
 * @author Andrew Wilder
 */
public abstract class Algorithm {
	
	/** Whether or not this algorithm is enabled */
	protected boolean enabled;
	private JComponent settingsDialog = new JPanel();
	protected HashMap<FilePair, Integer> fileScores = new HashMap<FilePair,
			Integer>();
	
	/**
	 * Perform edit distance on two int arrays
	 * @param arr1 First array
	 * @param arr2 Second array
	 * @return The edit distance
	 */
	public static int EditDistance(int[] arr1, int[] arr2) {
		
		// Preprocessing
		int[][] D = new int[2][arr1.length + 1];
		for(int i = 0; i <= arr1.length; ++i) {
			D[0][i] = i;
		}
		D[1][0] = 1;
		
		// Do the algorithm
		for(int i = 1; i <= arr2.length; ++i) {
			for(int j = 1; j <= arr1.length; ++j) {
				int sub = D[(i + 1) & 1][j - 1];
				if(arr1[j - 1] != arr2[i - 1]) {
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
		return D[arr2.length & 1][arr1.length];
	}
	
	/**
	 * Return the components for this algorithm's settings dialog
	 */
	public JComponent getSettingsDialog() {
		return settingsDialog;
	}

	/**
	 * Return the enabled status for this algorithm
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled status for this algorithm
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	/**
	 * @return The mapping of file pairs onto analysis scores
	 */
	public HashMap<FilePair, Integer> getFileScores() {
		return fileScores;
	}
	
	/**
	 * Analyze the file pairs using this algorithm
	 */
	public abstract void analyzeFiles();
}
