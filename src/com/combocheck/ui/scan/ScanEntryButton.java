package com.combocheck.ui.scan;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

/**
 * This class represents a scan entry removal button in the scan panel
 * 
 * @author Andrew Wilder
 */
public class ScanEntryButton extends JButton implements ActionListener {

	/** Global scan entry list for buttons */
	private static ScanEntryListPanel scanEntryListPanel;
	
	/** The entry to remove if this button is clicked */
	private ScanEntry se;
	
	/**
	 * Create the button with a link to the entry for removal
	 * @param scanEntries The collection to remove from
	 * @param se The 
	 */
	public ScanEntryButton(ScanEntryListPanel scanEntryListPanel,
			ScanEntry se) {
		// Set button properties
		super("Remove");
		ScanEntryButton.scanEntryListPanel = scanEntryListPanel;
		this.se = se;
		addActionListener(this);
	}

	/**
	 * Remove the attached scan entry for this button
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		scanEntryListPanel.getEntries().remove(se);
		scanEntryListPanel.revalidate();
		scanEntryListPanel.repaint();
	}
}
