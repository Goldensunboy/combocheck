package com.combocheck.ui.review;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.combocheck.algo.Algorithm;
import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;
import com.combocheck.global.ScanLoader;
import com.combocheck.ui.report.ReportPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JRadioButton;
import javax.swing.JViewport;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * This class represents the panel in which the user reviews similarity metrics
 * for submission pairs and scan statistics
 * 
 * @author Andrew Wilder
 */
@SuppressWarnings("serial")
public class ReviewPanel extends JPanel {

	/** Constants for the scan panel */
	private static final double DIVIDER_RATIO = 0.3;
	private static final int ENTRY_LIMIT = 50;
	private static final int ENTRY_BORDER_SIZE = 3;
	private static final Border ENTRY_BORDER =
			BorderFactory.createEmptyBorder(ENTRY_BORDER_SIZE,
			ENTRY_BORDER_SIZE, ENTRY_BORDER_SIZE, ENTRY_BORDER_SIZE);
	
	/** UI elements and fields for the review panel */
	private JSplitPane contentPanel =
			new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
	private JPanel algoSelectorPanel = new JPanel();
	private JScrollPane pairScrollPane = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JScrollBar scrollPaneBar;
	private JPanel pairScrollPaneContents = new JPanel();
	private JLabel selectedPairLabel = new JLabel("");
	private JLabel selectedFile1Label = new JLabel("");
	private JLabel selectedFile2Label = new JLabel("");
	private JLabel reportButtonLabel = new JLabel("");
	private JButton loadButton, saveButton;
	private JFileChooser jfc = new JFileChooser();
	
	/** Fields for the review panel */
	private List<Algorithm> algos;
	private Algorithm selectedAlgo;
	private List<PairEntry> entries = new ArrayList<PairEntry>();
	private FilePair selectedPair = null;
	private ReviewGraph graph = new ReviewGraph();
	private List<Map.Entry<FilePair, Integer>> scores;
	
	/**
	 * Construct the review panel and its components
	 */
	public ReviewPanel(final ReportPanel rp) {
		PairEntry.SetReviewPanel(this);
		
		// Set the properties for the split pane
		contentPanel.setDividerSize(5);
		pairScrollPaneContents.setLayout(
				new BoxLayout(pairScrollPaneContents, BoxLayout.Y_AXIS));
		pairScrollPaneContents.setBorder(ENTRY_BORDER);
		JViewport jvp = new JViewport();
		jvp.add(pairScrollPaneContents);
		pairScrollPane.setViewportView(jvp);
		scrollPaneBar = pairScrollPane.getVerticalScrollBar();
		scrollPaneBar.setUnitIncrement(6);
		scrollPaneBar.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent ae) {
				if(ae.getValue() + scrollPaneBar.getModel().getExtent() ==
						scrollPaneBar.getMaximum()) {
					populatePairEntryList();
				}
			}
		});
		contentPanel.setLeftComponent(pairScrollPane);
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		contentPanel.setRightComponent(controlPanel);
		
		// Construct load and save buttons
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Combocheck scans (scn)", "scn");
		jfc.setFileFilter(filter);
		ActionListener loadListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int result = -1;
				File loadFile = null;
				// Show dialog until selected file exists, or user cancels
				do {
					if(result == JFileChooser.APPROVE_OPTION) {
						JOptionPane.showMessageDialog(Combocheck.Frame,
								"File \"" + loadFile.getName() +
								"\" doesn't exist", "Error loading file",
								JOptionPane.ERROR_MESSAGE);
					}
					result = jfc.showOpenDialog(Combocheck.Frame);
					loadFile = jfc.getSelectedFile();
				} while(result == JFileChooser.APPROVE_OPTION &&
						!loadFile.exists());
				if(result == JFileChooser.APPROVE_OPTION) {
					String filename = loadFile.getAbsolutePath();
					ScanLoader.loadScan(filename);
					
					// Populate both the review and report panels
					populatePanel();
					rp.populatePanel();
				}
			}
		};
		loadButton = new JButton("Load scan");
		loadButton.addActionListener(loadListener);
		saveButton = new JButton("Save scan");
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int result = -1;
				File saveFile = null;
				// If selected file exists, keep showing choose file dialog as
				// long as user presses NO until they press CANCEL or OK
				do {
					if(result == JFileChooser.APPROVE_OPTION) {
						int result2 = JOptionPane.showConfirmDialog(
								Combocheck.Frame, "File \"" +
								saveFile.getName() + "\" exists. Overwrite?");
						if(result2 == JOptionPane.OK_OPTION) {
							break;
						} else if(result2 == JOptionPane.CANCEL_OPTION) {
							return;
						}
					}
					result = jfc.showSaveDialog(Combocheck.Frame);
					saveFile = jfc.getSelectedFile();
				} while(result == JFileChooser.APPROVE_OPTION &&
						saveFile.exists());
				if(result == JFileChooser.APPROVE_OPTION) {
					if(!Pattern.matches(".*\\.scn", saveFile.toString())) {
						saveFile = new File(saveFile + ".scn");
					}
					String filename = saveFile.getAbsolutePath();
					ScanLoader.saveScan(filename);
				}
			}
		});
		
		// Configure the load/save panel
		JPanel loadSavePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		loadSavePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Load/Save"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		loadSavePanel.add(saveButton);
		loadSavePanel.add(loadButton);
		
		// Configure the algorithm selection panel
		algoSelectorPanel.setLayout(
				new BoxLayout(algoSelectorPanel, BoxLayout.Y_AXIS));
		algoSelectorPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Algorithms"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		
		// Add algorithm selection panel and load/save panels in one row
		JPanel algoLSpanel = new JPanel(new GridLayout(1, 2));
		algoLSpanel.add(algoSelectorPanel);
		algoLSpanel.add(loadSavePanel);
		controlPanel.add(algoLSpanel);
		controlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				controlPanel.getPreferredSize().height));
		
		// Set up graph
		JPanel graphPanel = new JPanel();
		graphPanel.setLayout(new BorderLayout());
		graphPanel.add(graph, BorderLayout.CENTER);
		graph.setAlignmentX(Component.LEFT_ALIGNMENT);
		controlPanel.add(graphPanel);
		
		// Set up pair removal buttons
		JPanel prunePanel = new JPanel();
		prunePanel.setLayout(new BoxLayout(prunePanel, BoxLayout.Y_AXIS));
		prunePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Data set pruning"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		JPanel removePairPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		JButton removePairButton = new JButton("Remove");
		removePairButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				prune(selectedPair);
				setSelectedPair(entries.get(0).getFilePair());
				repopulatePairList(selectedAlgo);
			}
		});
		removePairPanel.add(removePairButton);
		removePairPanel.add(selectedPairLabel);
		JPanel removeFile1Panel =
				new JPanel(new FlowLayout(FlowLayout.LEADING));
		JButton removeFile1Button = new JButton("Remove");
		removeFile1Button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				prune(selectedPair.getFile1());
			}
		});
		removeFile1Panel.add(removeFile1Button);
		removeFile1Panel.add(selectedFile1Label);
		JPanel removeFile2Panel =
				new JPanel(new FlowLayout(FlowLayout.LEADING));
		JButton removeFile2Button = new JButton("Remove");
		removeFile2Button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				prune(selectedPair.getFile2());
			}
		});
		removeFile2Panel.add(removeFile2Button);
		removeFile2Panel.add(selectedFile2Label);
		JPanel reportButtonPanel = new JPanel(
				new FlowLayout(FlowLayout.LEADING));
		JButton reportButton = new JButton("Report");
		reportButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rp.addPair(selectedPair);
				reportButtonLabel.setText("Added pair to report list");
			}
		});
		reportButtonPanel.add(reportButton);
		reportButtonPanel.add(reportButtonLabel);
		prunePanel.add(removePairPanel);
		prunePanel.add(removeFile1Panel);
		prunePanel.add(removeFile2Panel);
		prunePanel.add(reportButtonPanel);
		prunePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				prunePanel.getPreferredSize().height));
		controlPanel.add(prunePanel);
		
		contentPanel.setDividerLocation((int)
				(DIVIDER_RATIO * Combocheck.PROGRAM_WIDTH));
		
		// Message before populating the review panel
		add(new JLabel(
				"You must first perform a scan, or load a previous one:"));
		JButton loadButton2 = new JButton("Load scan");
		loadButton2.addActionListener(loadListener);
		add(loadButton2);
	}
	
	/**
	 * Remove the specified pair
	 * @param fp
	 */
	private void prune(FilePair fp) {
		for(Algorithm a : algos) {
			a.getPairScores().remove(fp);
		}
	}
	
	/**
	 * Remove all pairs containing the specified file
	 * @param fname
	 */
	private void prune(String fname) {
		List<FilePair> toRemove = new ArrayList<FilePair>();
		for(FilePair fp : algos.get(0).getPairScores().keySet()) {
			if(fname.equals(fp.getFile1()) || fname.equals(fp.getFile2())) {
				toRemove.add(fp);
			}
		}
		for(FilePair fp : toRemove) {
			prune(fp);
		}
		setSelectedPair(entries.get(0).getFilePair());
		repopulatePairList(selectedAlgo);
	}
	
	/**
	 * Getter for the selected pair
	 * @return The selected pair
	 */
	public FilePair getSelectedPair() {
		return selectedPair;
	}
	
	/**
	 * Setter for the selected pair, also update UI text
	 * @param fp The pair
	 */
	public void setSelectedPair(FilePair fp) {
		selectedPair = fp;
		graph.setSelectedPair(fp);
		for(PairEntry pe : entries) {
			if(pe.getFilePair().equals(fp)) {
				selectedPairLabel.setText("Remove pair " + pe);
				reportButtonLabel.setText("Add pair " + pe +
						" to reporting list");
				break;
			}
		}
		selectedFile1Label.setText("Remove all pairs with " +
				fp.getShortenedFile1());
		selectedFile2Label.setText("Remove all pairs with " +
				fp.getShortenedFile2());
	}
	
	/**
	 * This function will populate the review panel with the appropriate UI
	 * elements once scanning has completed.
	 */
	public void populatePanel() {
		
		// Populate the algorithm selection
		algoSelectorPanel.removeAll();
		ButtonGroup algoGroup = new ButtonGroup();
		Algorithm firstAlgo = null;
		boolean first = true;
		algos = new ArrayList<Algorithm>();
		for(Algorithm a : Combocheck.algorithms) {
			if(a.isEnabled()) {
				algos.add(a);
				JRadioButton jrb = new JRadioButton(a.toString(), first);
				if(first) {
					first = false;
					firstAlgo = a;
				}
				jrb.addActionListener(new RepopulateListener(a));
				algoGroup.add(jrb);
				JPanel buttonPanel = new JPanel(
						new FlowLayout(FlowLayout.LEADING));
				buttonPanel.add(jrb);
				algoSelectorPanel.add(buttonPanel);
			}
		}
		algoSelectorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				algoSelectorPanel.getPreferredSize().height));
		
		// Repopulate the pair list
		repopulatePairList(firstAlgo);
		
		// Select first pair in list
		setSelectedPair(entries.get(0).getFilePair());
		pairScrollPane.repaint();
		
		// Show the JSplitPane with the review screen's contents
		removeAll();
		setLayout(new BorderLayout());
		add(contentPanel, BorderLayout.CENTER);
	}
	
	/**
	 * Populates an incremental amount of pair entries
	 */
	private void populatePairEntryList() {
		boolean first = entries.size() == 0;
		for(int pairNum = entries.size(), count = 0; pairNum < scores.size() &&
				count < ENTRY_LIMIT; ++count) {
			
			// Add a spacer between entries
			if(first) {
				first = false;
			} else {
				pairScrollPaneContents.add(
						Box.createVerticalStrut(ENTRY_BORDER_SIZE));
			}
			
			// Create and add the pair entry
			Map.Entry<FilePair, Integer> e = scores.get(pairNum++);
			PairEntry pe = new PairEntry(e.getKey(), pairNum, e.getValue());
			pe.setAlignmentX(Component.LEFT_ALIGNMENT);
			entries.add(pe);
			pairScrollPaneContents.add(pe);
		}
		
		// Revalidate the panel so it is redrawn
		pairScrollPaneContents.revalidate();
	}
	
	/**
	 * This method will repopulate the pair list ordered by the scores produced
	 * by the selected algorithm
	 * @param a The algorithm whose scores will order the pair list
	 */
	private void repopulatePairList(Algorithm a) {
		selectedAlgo = a;
		
		// Retrieve and sort the pairs
		scores = new ArrayList<Map.Entry<FilePair, Integer>>(
				a.getPairScores().entrySet());
		Collections.sort(scores,
				new Comparator<Map.Entry<FilePair, Integer>>() {
			@Override
			public int compare(Map.Entry<FilePair, Integer> elem1,
					Map.Entry<FilePair, Integer> elem2) {
				return elem1.getValue() - elem2.getValue();
			}
		});
		setSelectedPair(scores.get(0).getKey());
		
		// Create new entries list
		entries.clear();
		pairScrollPaneContents.removeAll();
		populatePairEntryList();
		
		// Refresh the graph
		graph.PopulateDataSet(a);
		graph.setSelectedPair(selectedPair);
	}
	
	/**
	 * This class represents an ActionListener tied to an Algorithm for
	 * repopulating the pair list ordered by the pair differences reported when
	 * scanning, on a per-algorithm basis. Each JRadioButton has its own.
	 * 
	 * @author Andrew Wilder
	 */
	private class RepopulateListener implements ActionListener {

		/** The instance of Algorithm this repopulate listener is tied to */
		private Algorithm algo;
		
		/**
		 * Construct a new RepopulateListener
		 * @param algo The algorithm to use for population of pairs
		 */
		public RepopulateListener(Algorithm algo) {
			this.algo = algo;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if(selectedAlgo != algo) {
				repopulatePairList(algo);
			}
		}
	}
}
