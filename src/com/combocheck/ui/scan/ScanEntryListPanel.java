package com.combocheck.ui.scan;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.Border;

import com.combocheck.global.FilePair;

/**
 * This class represents the panel on the left side of the scan panel, where
 * the scan entries are housed.
 * 
 * @author Andrew Wilder
 */
public class ScanEntryListPanel extends JScrollPane {

	/** Visual properties of the scan entry list panel */
	private static final int ENTRY_BORDER_SIZE = 6;
	private static final Border ENTRY_BORDER =
			BorderFactory.createEmptyBorder(ENTRY_BORDER_SIZE,
			ENTRY_BORDER_SIZE, ENTRY_BORDER_SIZE, ENTRY_BORDER_SIZE);
	
	/** The list of scan entries housed in this scan panel component */
	private Collection<ScanEntry> scanEntries = new ArrayList<ScanEntry>();
	private JPanel contentPane = new JPanel();
	
	/**
	 * Construct the panel
	 */
	public ScanEntryListPanel() {
		super(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		contentPane.setBorder(ENTRY_BORDER);
		setViewportView(contentPane);
		revalidate();
	}
	
	/**
	 * Getter for the scan entries as a Collection
	 * @return The entries
	 */
	public Collection<ScanEntry> getEntries() {
		return scanEntries;
	}
	
	/**
	 * Draw the entry list panel
	 */
	@Override
	public void revalidate() {
		
		// Empty the panel
		if(contentPane != null) {
			contentPane.removeAll();
		} else {
			return;
		}
		
		// Special case if there's no entries yet
		if(scanEntries.size() == 0) {
			JTextPane emptyPane = new JTextPane();
			emptyPane.setEditable(false);
			emptyPane.setText("No scan entries");
			emptyPane.setBorder(ENTRY_BORDER);
			contentPane.add(emptyPane);
		} else {
			// Add the scan entries
			boolean first = true;
			for(ScanEntry se : scanEntries) {
				
				// Add border between entries
				if(first) {
					first = false;
				} else {
					contentPane.add(Box.createVerticalStrut(ENTRY_BORDER_SIZE));
				}
				
				// Create the panel representing this entry
				JPanel entryPanel = new JPanel();
				entryPanel.setLayout(new BoxLayout(entryPanel,
						BoxLayout.Y_AXIS));
				JTextPane entryTextPane = new JTextPane();
				entryTextPane.setEditable(false);
				entryTextPane.setText("Type: " + se.getType().toString() +
						"\nPath: " + se.getPath() + "\nRegex: " +
						se.getRegex());
				JButton removeButton = new ScanEntryButton(this, se);
				removeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
				// TODO try to left justify the remove button
				entryPanel.add(entryTextPane);
				entryPanel.add(removeButton);
				entryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
						entryPanel.getPreferredSize().height));
				contentPane.add(entryPanel);
			}
		}
		
		// Refresh the panel
		super.revalidate();
	}
	
	/**
	 * Generate the file pairs based on the entries provided
	 * @return Collection of file pairs
	 */
	public Collection<FilePair> genPairs() {
		
		/* Collections used by this generation algorithm
		 * All matched files go in the left set. Regular scans also go in the
		 * right set. All pairings of left elements onto right elements are
		 * made, and the set functionality of the underlying container removes
		 * duplicate entries.
		 */
		HashSet<FilePair> pairSet = new HashSet<FilePair>();
		ArrayList<String> left = new ArrayList<String>();
		ArrayList<String> right = new ArrayList<String>();
		
		// Process each entry
		for(ScanEntry se : scanEntries) {
			if(se.getType() == ScanEntry.ScanEntryType.SingleFile) {
				left.add(se.getPath());
				right.add(se.getPath());
			} else {
				// Recurse through folders to find files matching the regex
				ArrayDeque<File> folders = new ArrayDeque<File>();
				folders.add(new File(se.getPath()));
				do {
					File dir = folders.pollFirst();
					for(File f : dir.listFiles()) {
						if(f.isDirectory()) {
							folders.add(f);
						} else if(Pattern.matches(se.getRegex(), f.getName())) {
							left.add(f.getAbsolutePath());
							if(se.getType() == ScanEntry.
									ScanEntryType.Normal) {
								right.add(f.getAbsolutePath());
							}
						}
					}
				} while(!folders.isEmpty());
			}
		}
		
		// Match up the entries
		for(String file1 : left) {
			for(String file2 : right) {
				if(!file1.equals(file2)) {
					pairSet.add(new FilePair(file1, file2));
				}
			}
		}
		
		return pairSet;
	}
}
