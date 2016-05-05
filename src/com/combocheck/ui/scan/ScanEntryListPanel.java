package com.combocheck.ui.scan;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.io.File;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.Border;

import com.combocheck.algo.JNIFunctions;
import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;

/**
 * This class represents the panel on the left side of the scan panel, where
 * the scan entries are housed.
 * 
 * @author Andrew Wilder
 */
@SuppressWarnings("serial")
public class ScanEntryListPanel extends JScrollPane {

	/** Visual properties of the scan entry list panel */
	private static final int ENTRY_BORDER_SIZE = 6;
	private static final Border ENTRY_BORDER =
			BorderFactory.createEmptyBorder(ENTRY_BORDER_SIZE,
			ENTRY_BORDER_SIZE, ENTRY_BORDER_SIZE, ENTRY_BORDER_SIZE);
	
	/** The list of scan entries housed in this scan panel component */
	private List<ScanEntry> scanEntries = new ArrayList<ScanEntry>();
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
				String type = se.isRegex() ? "Regex" : "Filename";
				entryTextPane.setText("Type: " + se.getType().toString() +
						"\nPath: " + se.getPath() + "\n" + type + ": " +
						se.getRegex());
				JPanel removeButtonPanel = new JPanel();
				removeButtonPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
				JButton removeButton = new ScanEntryButton(this, se);
				removeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
				entryPanel.add(entryTextPane);
				removeButtonPanel.add(removeButton);
				entryPanel.add(removeButtonPanel);
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
	 */
	public void genPairs() {
		
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
					File subFiles[] = dir.listFiles();
					if(subFiles == null) {
						continue;
					}
					for(File f : subFiles) {
						if(f.isDirectory()) {
							folders.add(f);
						} else if(se.isRegex()) {
							if(Pattern.matches(se.getRegex(), f.getName())) {
								left.add(f.getAbsolutePath());
								if(se.getType() == ScanEntry.
										ScanEntryType.Normal) {
									right.add(f.getAbsolutePath());
								}
							}
						} else {
							if(se.getRegex().equals(f.getName())) {
								left.add(f.getAbsolutePath());
								if(se.getType() == ScanEntry.
										ScanEntryType.Normal) {
									right.add(f.getAbsolutePath());
								}
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
		
		// Set the globals in Combocheck for preprocessing-based algos
		if(pairSet.size() > 0) {
			Combocheck.FileList = left;
			Combocheck.FilePairs = new ArrayList<FilePair>();
			Combocheck.FileOrdering = new HashMap<Integer, String>();
			Combocheck.PairOrdering = new HashMap<Integer, FilePair>();
			int n = 0;
			for(FilePair fp : pairSet) {
				Combocheck.PairOrdering.put(n++, fp);
				Combocheck.FilePairs.add(fp);
			}
			n = 0;
			for(String file : Combocheck.FileList) {
				Combocheck.FileOrdering.put(n++, file);
			}
			
			// Create primitive version of pair data
			Map<String, Integer> reverseMap =
					new HashMap<String, Integer>();
			int index = 0;
			for(String file : Combocheck.FileList) {
				reverseMap.put(file, index++);
			}
			List<Integer> filePairList = new ArrayList<Integer>();
			for(FilePair fp : pairSet) {
				filePairList.add(reverseMap.get(fp.getFile1()));
				filePairList.add(reverseMap.get(fp.getFile2()));
			}
			int[] filePairsArray = new int[filePairList.size()];
			for(int i = 0; i < filePairList.size(); ++i) {
				filePairsArray[i] = filePairList.get(i);
			}
			Combocheck.FilePairInts = filePairsArray;
			
			// If the JNI algorithm library is available, set metadata in it
			if(JNIFunctions.JNIEnabled()) {
				String[] fileListArray = new String[Combocheck.FileList.size()];
				index = 0;
				for(String filename : Combocheck.FileList) {
					fileListArray[index++] = filename;
				}
				JNIFunctions.SetJNIFilePairData(fileListArray, filePairsArray);
			}
		}
	}
}
