package com.combocheck.ui.scan;

import javax.swing.JSplitPane;

/**
 * This class represents the panel in which the user selects files and folders
 * for scanning
 * 
 * @author Andrew Wilder
 */
public class ScanPanel extends JSplitPane {
	
	/** Constants for the scan panel */
	public static final double DIVIDER_RATIO = 0.3;

	/**
	 * Construct the scan panel and its components
	 */
	public ScanPanel() {
		
		// Configure split pane properties
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		setDividerSize(5);
		
		// Add the UI components
		ScanEntryListPanel selp = new ScanEntryListPanel();
		setLeftComponent(selp);
		setRightComponent(new ScanControlPanel(selp));
	}
}
