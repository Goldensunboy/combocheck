package com.combocheck.ui.scan;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.combocheck.algo.Algorithm;
import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;

/**
 * This class represents the control pane for the scan panel
 * 
 * @author Andrew Wilder
 */
public class ScanControlPanel extends JPanel {

	/** Global scan entry list panel for buttons */
	private static ScanEntryListPanel scanEntryListPanel;
	private JButton scanButton;
	
	/**
	 * Construct a new scan control panel
	 * @param filePairs Collection of file pairs to work with
	 */
	public ScanControlPanel(final ScanEntryListPanel scanEntryListPanel) {
		this.scanEntryListPanel = scanEntryListPanel;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Add scan entry buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		buttonPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Add scan entries:"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		JButton regularFolderButton = new JButton("Normal folder");
		regularFolderButton.addActionListener(new AddFolderButtonListener(
				ScanEntry.ScanEntryType.Normal));
		buttonPanel.add(regularFolderButton);
		JButton oldSemesterButton = new JButton("Old Semester");
		oldSemesterButton.addActionListener(new AddFolderButtonListener(
				ScanEntry.ScanEntryType.OldSemester));
		buttonPanel.add(oldSemesterButton);
		JButton singleFileButton = new JButton("Single File");
		singleFileButton.addActionListener(new AddFolderButtonListener(
				ScanEntry.ScanEntryType.SingleFile));
		buttonPanel.add(singleFileButton);
		buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				buttonPanel.getPreferredSize().height));
		add(buttonPanel);
		
		// Add algorithm selections
		JPanel algorithmPanel = new JPanel();
		algorithmPanel.setLayout(new BoxLayout(algorithmPanel,
				BoxLayout.Y_AXIS));
		algorithmPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Algorithms:"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		for(final Algorithm a : Combocheck.algorithms) {
			JPanel algorithmSubPanel = new JPanel(new FlowLayout(
					FlowLayout.LEADING));
			JCheckBox checkBox = new JCheckBox(a.toString(), a.isEnabled());
			checkBox.addActionListener(new CheckBoxListener(checkBox, a));
			algorithmSubPanel.add(checkBox);
			if(a.getSettingsPanel() != null) {
				JButton settingsButton = new JButton("Settings");
				settingsButton.addActionListener(new SettingsButtonListener(a));
				algorithmSubPanel.add(settingsButton);
			}
			algorithmPanel.add(algorithmSubPanel);
		}
		algorithmPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				algorithmPanel.getPreferredSize().height));
		add(algorithmPanel);
		
		// Add the scan button
		JPanel scanButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		scanButton = new JButton("Start scan");
		scanButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				// Make sure at least one algorithm is selected
				boolean selected = false;
				for(Algorithm a : Combocheck.algorithms) {
					if(a.isEnabled()) {
						selected = true;
						break;
					}
				}
				if(!selected) {
					JOptionPane.showMessageDialog(ScanControlPanel.this,
							"No algorithms selected", "Could not start scan",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// Generate pairs of files
				scanEntryListPanel.genPairs();
				List<FilePair> filePairs = Combocheck.FilePairs;
				if(filePairs == null || filePairs.size() == 0) {
					JOptionPane.showMessageDialog(ScanControlPanel.this,
							"No file pairs found", "Could not start scan",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// Initiate the scan process
				setEnabled(false);
				Combocheck.performScans();
			}
		});
		scanButtonPanel.add(scanButton);
		add(scanButtonPanel);
	}
	
	/**
	 * Used to re-enable the scan button after a scan has completed
	 */
	public void enableScanButton() {
		scanButton.setEnabled(true);
	}
	
	/**
	 * This class represents the action listener used for the add folder
	 * dialog
	 * 
	 * @author Andrew Wilder
	 */
	private class AddFolderButtonListener implements ActionListener {
		
		/** The type of this listener */
		private ScanEntry.ScanEntryType type;
		
		/** Create a new listener object with predefined type */
		public AddFolderButtonListener(ScanEntry.ScanEntryType type) {
			this.type = type;
		}
		
		/**
		 * Use a dialog box to find the folder
		 */
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			// Open dialog for choosing a folder
			RegexFileChooser RFchooser = new RegexFileChooser(type);
			if(RFchooser.showOpenDialog(getParent()) ==
					JFileChooser.APPROVE_OPTION) {
				String dir = RFchooser.getSelectedFile().getAbsolutePath();
				String regex = RFchooser.getRegex();
				boolean isRegex = RFchooser.isRegexType();
				ScanEntry entry = new ScanEntry(dir, regex, type, isRegex);
				scanEntryListPanel.getEntries().add(entry);
				scanEntryListPanel.revalidate();
				scanEntryListPanel.repaint();
			}
		}
	}
	
	/**
	 * This class represents a listener for the checkboxes for each algorithm
	 * type, which is used to set that algorithm's enabled status
	 * 
	 * @author Andrew Wilder
	 */
	private class CheckBoxListener implements ActionListener {
		
		/** The attached checkbox and algorithm for this listener */
		private JCheckBox checkBox;
		private Algorithm algorithm;
		
		/**
		 * Construct a new instance of this checkbox listener
		 * @param algorithm
		 * @param checkbox
		 */
		public CheckBoxListener(JCheckBox checkBox, Algorithm algorithm) {
			this.checkBox = checkBox;
			this.algorithm = algorithm;
		}

		/**
		 * Set the value for the attached Algorithm type to the checkbox value
		 * @param e
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			algorithm.setEnabled(checkBox.isSelected());
		}
	}
	
	/**
	 * This class represents a button listener meant to show the settings
	 * dialog for an algorithm when the associated button is pressed.
	 * 
	 * @author Andrew Wilder
	 */
	private class SettingsButtonListener implements ActionListener {

		/** The JDialog to display when the button is pressed */
		private JDialog dialog;
		
		/**
		 * Construct the instance of this settings button listener
		 * @param algorithm The associated algorithm
		 */
		public SettingsButtonListener(Algorithm algorithm) {
			dialog = new JDialog(Combocheck.Frame, true);
			dialog.setTitle(algorithm + " settings");
			dialog.add(algorithm.getSettingsPanel());
			dialog.pack();
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			dialog.setLocationRelativeTo(Combocheck.Frame);
			dialog.setVisible(true);
		}
	}
}
