package com.combocheck.algo;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.combocheck.algo.Algorithm;

/**
 * This class represents the MOSS (Measure of Software Similarity) algorithm.
 * 
 * @author Andrew Wilder
 */
public class MossAlgorithm implements Algorithm {

	/** Whether or not this algorithm is enabled */
	private boolean enabled;
	private static JComponent settingsDialog = new JPanel();
	
	/**
	 * Construct the default instance of MossAlgorithm
	 */
	public MossAlgorithm() {
		enabled = true;
		// TODO construct settings dialog
	}
	
	/**
	 * Return the components for this algorithm's settings dialog
	 */
	@Override
	public JComponent getSettingsDialog() {
		return settingsDialog;
	}

	/**
	 * Return the enabled status for this algorithm
	 */
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled status for this algorithm
	 */
	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public String toString() {
		return "Moss";
	}

	/**
	 * Analyze the file pairs using this algorithm
	 */
	@Override
	public void analyzePairs() {
		// TODO if DLL exists, use JNI version
		// TODO algorithm
	}
}
