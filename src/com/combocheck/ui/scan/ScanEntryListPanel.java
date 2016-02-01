package com.combocheck.ui.scan;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JPanel;

/**
 * This class represents the panel on the left side of the scan panel, where
 * the scan entries are housed.
 * 
 * @author Andrew Wilder
 */
public class ScanEntryListPanel extends JPanel {

	/** The list of scan entries housed in this scan panel component */
	private Collection<ScanEntry> scanEntries = new ArrayList<ScanEntry>();
	
	/**
	 * Construct the panel
	 */
	public ScanEntryListPanel() {
		// nothing to do here
	}
	
	/**
	 * Getter for the scan entries as a Collection
	 * @return The entries
	 */
	public Collection<ScanEntry> getEntries() {
		return scanEntries;
	}
	
	/**
	 * Draw the 
	 */
	@Override
	public void revalidate() {
		
	}
}
