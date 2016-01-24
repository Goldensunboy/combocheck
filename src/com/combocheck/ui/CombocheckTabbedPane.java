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

	/**
	 * Construct the main tabbed pane. This is also where the size of the main
	 * application's frame is set indirectly due to JFrame packing.
	 */
	public CombocheckTabbedPane() {
		
		// Set the size of the tabbed pane
		setPreferredSize(new Dimension(Combocheck.PROGRAM_WIDTH, Combocheck.PROGRAM_HEIGHT));
		
		// Add the tabs
		addTab("Scan", new ScanPanel());
		addTab("Review", new ReviewPanel());
		addTab("Report", new ReportPanel());
	}
}
