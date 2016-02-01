package com.combocheck.ui.scan;

import java.util.Collection;

import javax.swing.JPanel;

/**
 * This class represents the control pane for the scan panel
 * @author Andrew Wilder
 */
public class ScanControlPanel extends JPanel {

	/** The same file pair collection from the ScanEntryListPanel */
	private Collection<ScanEntry> scanEntries;
	
	/**
	 * Construct a new scan control panel
	 * @param filePairs Collection of file pairs to work with
	 */
	public ScanControlPanel(Collection<ScanEntry> scanEntries) {
		this.scanEntries = scanEntries;
	}
}
