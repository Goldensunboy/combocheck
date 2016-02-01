package com.combocheck.ui.scan;

import javax.swing.JSplitPane;

/**
 * This class represents the panel in which the user selects files and folders
 * for scanning
 * 
 * @author Andrew Wilder
 */
public class ScanPanel extends JSplitPane {

	/**
	 * Construct the scan panel and its components
	 */
	public ScanPanel() {
		
		// Split vertically, continuous layout when resizing
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		
		// Add the UI components
		ScanEntryListPanel selp = new ScanEntryListPanel();
		setLeftComponent(selp);
		setRightComponent(new ScanControlPanel(selp.getEntries()));
	}
}
