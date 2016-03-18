package com.combocheck.ui.scan;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * This class represents a file chooser with a regex field
 * 
 * @author Andrew Wilder
 */
public class RegexFileChooser extends JFileChooser {
	
	/** The regex entered by the user */
	private JTextField regexField = new JTextField();
	
	/**
	 * Construct a new regex file chooser
	 */
	public RegexFileChooser(ScanEntry.ScanEntryType type) {
		
		// TODO remove this for production
		File dir = new File("/home/andrew/Documents/Spring_2016/CS 6999/combocheck/test");
		regexField.setText("HW2Bases\\.java");
		setCurrentDirectory(dir);
		
		// Set JFileChooser properties
		setDialogTitle("Select a folder (" + type + ")");
		setFileSelectionMode(type == ScanEntry.ScanEntryType.SingleFile ?
				JFileChooser.FILES_ONLY : JFileChooser.DIRECTORIES_ONLY);
		
		// Change the file types dropdown to a regex field
		JPanel regexPanel = (JPanel) getComponent(3);
		regexPanel = (JPanel) regexPanel.getComponent(2);
		JLabel regexLabel = (JLabel) regexPanel.getComponent(0);
		regexLabel.setText("Regex:");
		regexPanel.remove(1);
		if(type != ScanEntry.ScanEntryType.SingleFile) {
			regexPanel.add(regexField);
		} else {
			regexLabel.setText("");
		}
	}
	
	/**
	 * Retrieves the regex entered by the user
	 * @return The regex string
	 */
	public String getRegex() {
		return regexField.getText();
	}
}
