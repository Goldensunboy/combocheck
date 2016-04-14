package com.combocheck.ui;

import java.awt.Dimension;

import javax.swing.JTabbedPane;

import com.combocheck.global.Combocheck;
import com.combocheck.ui.report.ReportPanel;
import com.combocheck.ui.review.ReviewPanel;
import com.combocheck.ui.scan.ScanPanel;

/**
 * This class represents the tabbed pane which controls the screen shown in
 * Combocheck.
 * 
 * @author Andrew Wilder
 */
public class CombocheckTabbedPane extends JTabbedPane {
	
	/** Instances of the panels */
	private ScanPanel scanPanel = new ScanPanel();
	private ReviewPanel reviewPanel = new ReviewPanel();
	private ReportPanel reportPanel = new ReportPanel();
	
	/**
	 * Construct the main tabbed pane. This is also where the size of the main
	 * application's frame is set indirectly due to JFrame packing.
	 */
	public CombocheckTabbedPane() {
		
		// Set the size of the tabbed pane
		setPreferredSize(new Dimension(Combocheck.PROGRAM_WIDTH,
				Combocheck.PROGRAM_HEIGHT));
		
		// Add the tabs
		addTab("Scan", scanPanel);
		addTab("Review", reviewPanel);
		addTab("Report", reportPanel);
	}
	
	/**
	 * Getter for the instance of ScanPanel that this holds
	 * @return
	 */
	public ScanPanel getScanPanel() {
		return scanPanel;
	}
	
	/**
	 * Getter for the instance of ReviewPanel that this holds
	 * @return
	 */
	public ReviewPanel getReviewPanel() {
		return reviewPanel;
	}
	
	/**
	 * Getter for the instance of ReportPanel that this holds
	 * @return
	 */
	public ReportPanel getReportPanel() {
		return reportPanel;
	}
	
	/**
	 * Automatically switch to viewing the review panel
	 */
	public void switchToReviewPanel() {
		this.setSelectedComponent(reviewPanel);
	}
}
