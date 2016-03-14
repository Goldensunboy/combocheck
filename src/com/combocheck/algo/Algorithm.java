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
