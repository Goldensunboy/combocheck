package com.combocheck.ui.scan;

import javax.swing.JSplitPane;

import com.combocheck.global.Combocheck;

/**
 * This class represents the panel in which the user selects files and folders
 * for scanning
 * 
 * @author Andrew Wilder
 */
@SuppressWarnings("serial")
public class ScanPanel extends JSplitPane {
	
	/** Constants for the scan panel */
	private static final double DIVIDER_RATIO = 0.3;
	
	/** Instances of UI components */
	ScanEntryListPanel scanEntryListPanel = new ScanEntryListPanel();
	ScanControlPanel scanControlPanel = new ScanControlPanel(scanEntryListPanel);

	/**
	 * Construct the scan panel and its components
	 */
	public ScanPanel() {
		
		// Configure split pane properties
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		setDividerSize(5);
		
		// Add the UI components
		setLeftComponent(scanEntryListPanel);
		setRightComponent(scanControlPanel);
		setDividerLocation((int) (DIVIDER_RATIO * Combocheck.PROGRAM_WIDTH));
	}
	
	/**
	 * Getter for the instance of ScanEntryListPanel that this holds
	 * @return
	 */
	public ScanEntryListPanel getScanEntryListPanel() {
		return scanEntryListPanel;
	}
	
	/**
	 * Getter for the instance of ScanControlPanel that this holds
	 * @return
	 */
	public ScanControlPanel getScanControlPanel() {
		return scanControlPanel;
	}
}
