package com.combocheck.algo;

import javax.swing.JComponent;

/**
 * This interface provides the guarantee of particular comparison functions
 * and parameter option dialogs on a per-algorithm basis.
 *
 * @author Andrew Wilder
 */
public interface Algorithm {
	
	/**
	 * Return the settings dialog for this algorithm
	 * @return
	 */
	public abstract JComponent getSettingsDialog();
	
	/**
	 * Return the static value of whether this algorithm is enabled
	 */
	public abstract boolean isEnabled();
	
	/**
	 * Set the enabled value for this Algorithm
	 */
	public abstract void setEnabled(boolean enabled);
	
	/**
	 * Analyze the file pairs using this algorithm
	 */
	public abstract void analyzePairs();
}
