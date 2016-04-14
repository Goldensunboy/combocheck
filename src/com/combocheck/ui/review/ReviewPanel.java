package com.combocheck.ui.review;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.combocheck.algo.Algorithm;
import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JRadioButton;
import javax.swing.JViewport;
import javax.swing.border.Border;

/**
 * This class represents the panel in which the user reviews similarity metrics
 * for submission pairs and scan statistics
 * 
 * @author Andrew Wilder
 */
public class ReviewPanel extends JPanel {

	/** Constants for the scan panel */
	private static final double DIVIDER_RATIO = 0.3;
	private static final int ENTRY_LIMIT = 50;
	private static final int ENTRY_BORDER_SIZE = 3;
	private static final Border ENTRY_BORDER =
			BorderFactory.createEmptyBorder(ENTRY_BORDER_SIZE,
			ENTRY_BORDER_SIZE, ENTRY_BORDER_SIZE, ENTRY_BORDER_SIZE);
	
	/** UI elements and fields for the review panel */
	private List<Algorithm> algos;
	private Algorithm selectedAlgo;
	private JSplitPane contentPanel =
			new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
	private JPanel algoSelectorPanel = new JPanel();
	private JScrollPane pairScrollPane = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JPanel pairScrollPaneContents = new JPanel();
	private List<PairEntry> entries = new ArrayList<PairEntry>();
	private FilePair selectedPair = null;
	private ReviewGraph graph = new ReviewGraph();
	private JLabel selectedPairLabel = new JLabel("");
	private JLabel selectedFile1Label = new JLabel("");
	private JLabel selectedFile2Label = new JLabel("");
	
	/**
	 * Construct the review panel and its components
	 */
	public ReviewPanel() {
		PairEntry.SetReviewPanel(this);
		
		// Set the properties for the split pane
		contentPanel.setDividerSize(5);
		pairScrollPaneContents.setLayout(
				new BoxLayout(pairScrollPaneContents, BoxLayout.Y_AXIS));
		pairScrollPaneContents.setBorder(ENTRY_BORDER);
		JViewport jvp = new JViewport();
		jvp.add(pairScrollPaneContents);
		pairScrollPane.setViewportView(jvp);
		pairScrollPane.getVerticalScrollBar().setUnitIncrement(6);
		contentPanel.setLeftComponent(pairScrollPane);
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		contentPanel.setRightComponent(controlPanel);
		
		// Configure the algorithm selection panel
		algoSelectorPanel.setLayout(
				new BoxLayout(algoSelectorPanel, BoxLayout.Y_AXIS));
		algoSelectorPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Algorithms:"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		controlPanel.add(algoSelectorPanel);
		controlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				controlPanel.getPreferredSize().height));
		contentPanel.setDividerLocation((int)
				(DIVIDER_RATIO * Combocheck.PROGRAM_WIDTH));
		
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
				BorderFactory.createTitledBorder("Data set pruning:"),
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
		prunePanel.add(removePairPanel);
		prunePanel.add(removeFile1Panel);
		prunePanel.add(removeFile2Panel);
		prunePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				prunePanel.getPreferredSize().height));
		controlPanel.add(prunePanel);
		
		// Message before populating the review panel
		add(new JLabel("You must perform a scan first."));
	}
	
	/**
	 * Remove the specified pair
	 * @param fp
	 */
	private void prune(FilePair fp) {
		for(Algorithm a : algos) {
			a.getFileScores().remove(fp);
		}
	}
	
	/**
	 * Remove all pairs containing the specified file
	 * @param fname
	 */
	private void prune(String fname) {
		List<FilePair> toRemove = new ArrayList<FilePair>();
		for(FilePair fp : algos.get(0).getFileScores().keySet()) {
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
	 * Setter for the selected pair
	 * @param fp The pair
	 */
	public void setSelectedPair(FilePair fp) {
		selectedPair = fp;
		for(PairEntry pe : entries) {
			if(pe.getFilePair().equals(fp)) {
				selectedPairLabel.setText("Remove pair " + pe.getIndex());
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
	 * This method will repopulate the pair list ordered by the scores produced
	 * by the selected algorithm
	 * @param a The algorithm whose scores will order the pair list
	 */
	private void repopulatePairList(Algorithm a) {
		selectedAlgo = a;
		
		// Retrieve and sort the pairs
		List<Map.Entry<FilePair, Integer>> scores =
				new ArrayList<Map.Entry<FilePair, Integer>>();
		for(Map.Entry<FilePair, Integer> e : a.getFileScores().entrySet()) {
			scores.add(e);
		}
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
		int pairNum = 1;
		for(Map.Entry<FilePair, Integer> e : scores) {
			PairEntry pe = new PairEntry(e.getKey(), pairNum++, e.getValue());
			pe.setAlignmentX(Component.LEFT_ALIGNMENT);
			entries.add(pe);
			if(pairNum > ENTRY_LIMIT) {
				break;
			}
		}

		// Re-initialize the scroll pane housing the pair entries
		pairScrollPaneContents.removeAll();
		boolean first = true;
		for(PairEntry pe : entries) {
			if(first) {
				first = false;
			} else {
				pairScrollPaneContents.add(
						Box.createVerticalStrut(ENTRY_BORDER_SIZE));
			}
			pairScrollPaneContents.add(pe);
		}
		pairScrollPane.revalidate();
		pairScrollPane.repaint();
		
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
