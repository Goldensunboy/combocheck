package com.combocheck.ui.scan;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayDeque;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * This class represents a file chooser with a regex field
 * 
 * @author Andrew Wilder
 */
@SuppressWarnings("serial")
public class RegexFileChooser extends JFileChooser {
	
	/** The regex entered by the user */
	private static final String[] TYPES = {"Filename", "Regex"};
	private JLabel regexLabel;
	private JTextField fileField;
	private JTextField regexField = new JTextField();
	private JComboBox<String> typeComboBox = new JComboBox<String>(TYPES);
	
	/**
	 * Construct a new regex file chooser
	 */
	public RegexFileChooser(ScanEntry.ScanEntryType type) {
		
		// TODO remove this for production
		File dir = new File("/home/andrew/Documents/Spring_2016/CS 6999/combocheck/test");
		regexField.setText("bmptoc.c");
		setCurrentDirectory(dir);
		
		// Set JFileChooser properties
		setDialogTitle("Select a folder (" + type + ")");
		setFileSelectionMode(type == ScanEntry.ScanEntryType.SingleFile ?
				JFileChooser.FILES_ONLY : JFileChooser.DIRECTORIES_ONLY);
		
		// Change the file types dropdown to a regex field
		JPanel selectionPanel = (JPanel) getComponent(3);
		JPanel filePanel = (JPanel) selectionPanel.getComponent(0);
		fileField = (JTextField) filePanel.getComponent(1);
		JPanel regexPanel = (JPanel) selectionPanel.getComponent(2);
		regexLabel = (JLabel) regexPanel.getComponent(0);
		regexLabel.setText("Filename:");
		regexPanel.remove(1);
		JPanel buttonPanel = (JPanel) selectionPanel.getComponent(3);
		JButton openButton = (JButton) buttonPanel.getComponent(0);
		if(type == ScanEntry.ScanEntryType.SingleFile) {
			regexLabel.setText("");
		} else {
			regexPanel.add(regexField);
			
			// Add the filename/regex combobox
			typeComboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String type = (String) typeComboBox.getSelectedItem();
					regexLabel.setText(type + ":");
				}
			});
			buttonPanel.add(typeComboBox, 0);
		}
		
		// Add the no files found failsafe to open button
		ActionListener orig = openButton.getActionListeners()[0];
		openButton.removeActionListener(orig);
		openButton.addActionListener(new RFCListener(orig, type));
	}
	
	/**
	 * Retrieves the regex entered by the user
	 * @return The regex string
	 */
	public String getRegex() {
		return regexField.getText();
	}
	
	/**
	 * Returns true if the selected search type is "regex"
	 * @return
	 */
	public boolean isRegexType() {
		return "Regex".equals(typeComboBox.getSelectedItem());
	}
	
	/**
	 * Check to see if any files are found before allowing the user to make this
	 * selection
	 * 
	 * @author Andrew Wilder
	 */
	private class RFCListener implements ActionListener {
		
		/** Instance data for this RFC listener */
		private ActionListener orig;
		private ScanEntry.ScanEntryType type;
		
		/**
		 * Create a new regex file chooser action listener
		 * @param orig
		 */
		public RFCListener(ActionListener orig, ScanEntry.ScanEntryType type) {
			this.orig = orig;
			this.type = type;
		}

		/**
		 * Check to see if any files are found by this selection
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			
			// Does the file exist?
			File f = new File(fileField.getText());
			if(!f.exists()) {
				JOptionPane.showMessageDialog(RegexFileChooser.this,
						"File \"" + f.getName() + "\" not found",
						"Could not add scan entry", JOptionPane.ERROR_MESSAGE);
			} else if(type == ScanEntry.ScanEntryType.SingleFile) {
				orig.actionPerformed(e);
			} else {
				
				// Iterate through the directory tree trying to find a file
				ArrayDeque<File> folders = new ArrayDeque<File>();
				folders.add(f);
				do {
					File dir = folders.pollFirst();
					File subFiles[] = dir.listFiles();
					if(subFiles == null) {
						continue;
					}
					for(File f2 : subFiles) {
						if(f2.isDirectory()) {
							folders.add(f2);
						} else if(isRegexType()) {
							if(Pattern.matches(regexField.getText(),
									f2.getName())) {
								orig.actionPerformed(e);
								return;
							}
						} else {
							if(f2.getName().equals(regexField.getText())) {
								orig.actionPerformed(e);
								return;
							}
						}
					}
				} while(!folders.isEmpty());
			}
			
			// No files found
			String msg = "No files matching " + (isRegexType() ? "regex" :
					"name") + " \"" + regexField.getText() + "\" found";
			JOptionPane.showMessageDialog(RegexFileChooser.this,
					msg, "Could not add scan entry", JOptionPane.ERROR_MESSAGE);
		}
	}
}
