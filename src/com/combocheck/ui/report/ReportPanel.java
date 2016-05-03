package com.combocheck.ui.report;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;
import com.combocheck.global.ScanLoader;

/**
 * This class represents the panel in which the user generates reports for
 * pairs of assignments scanned by Combocheck.
 * 
 * @author Andrew Wilder
 */
@SuppressWarnings("serial")
public class ReportPanel extends JPanel {

	/** Constants for the report panel */
	private static final double DIVIDER_RATIO = 0.3;
	private static final int ENTRY_BORDER_SIZE = 3;
	private static final Border ENTRY_BORDER =
			BorderFactory.createEmptyBorder(ENTRY_BORDER_SIZE,
			ENTRY_BORDER_SIZE, ENTRY_BORDER_SIZE, ENTRY_BORDER_SIZE);
	
	/** Fields for the report panel */
	private List<ReportEntry> entries = new ArrayList<ReportEntry>();
	private ReportEntry selectedEntry = null;
	
	/** UI for the report panel */
	private JSplitPane contentPanel =
			new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
	private JScrollPane pairScrollPane = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JPanel pairScrollPaneContents = new JPanel() {
		@Override
		public void revalidate() {
			removeAll();
			if(entries.size() == 0) {
				add(new JLabel("No entries"));
			} else {
				boolean first = true;
				for(ReportEntry entry : entries) {
					if(first) {
						first = false;
					} else {
						add(Box.createVerticalStrut(ENTRY_BORDER_SIZE));
					}
					add(entry);
				}
			}
			super.revalidate();
			repaint();
		}
	};
	private JTextArea annotationField = new JTextArea() {
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		}
	};
	private JTextField score1Field = new JTextField(6);
	private JLabel score1Label = new JLabel("");
	private JTextField score2Field = new JTextField(6);
	private JLabel score2Label = new JLabel("");
	private JFileChooser jfc = new JFileChooser();
	private JButton loadButton, saveButton, exportButton;
	
	/**
	 * Construct the report panel and its components
	 */
	public ReportPanel() {
		ReportEntry.SetReportPanel(this);
		
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
		controlPanel.setLayout(
				new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		contentPanel.setRightComponent(controlPanel);
		
		// Configure the save/load buttons
		loadButton = new JButton("Load scan");
		loadButton.addActionListener(new ActionListener() {
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
					Combocheck.Frame.getTabbedPane().getReviewPanel().
							populatePanel();
				}
			}
		});
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
		JPanel loadSavePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		loadSavePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Load/Save"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		loadSavePanel.add(saveButton);
		loadSavePanel.add(loadButton);
		loadSavePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				loadSavePanel.getPreferredSize().height));
		controlPanel.add(loadSavePanel);
		
		// Configure annotation fields
		JPanel annotationPanel = new JPanel();
		annotationPanel.setLayout(new BoxLayout(annotationPanel,
				BoxLayout.Y_AXIS));
		annotationPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Annotation"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		JPanel score1Panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		JPanel score2Panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		score1Panel.add(score1Field);
		score1Panel.add(score1Label);
		score2Panel.add(score2Field);
		score2Panel.add(score2Label);
		score1Field.getDocument().addDocumentListener(new DocumentListener() {
			private void update(DocumentEvent de) {
				// Update score 1 for selected entry
				if(selectedEntry != null && Pattern.matches("\\d+(\\.\\d+)?",
						score1Field.getText())) {
					selectedEntry.setScore1(Double.parseDouble(
							score1Field.getText()));
				}
			}
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				update(arg0);
			}
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				update(arg0);
			}
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				update(arg0);
			}
		});
		score2Field.getDocument().addDocumentListener(new DocumentListener() {
			private void update(DocumentEvent de) {
				// Update score 1 for selected entry
				if(selectedEntry != null && Pattern.matches("\\d+(\\.\\d+)?",
						score2Field.getText())) {
					selectedEntry.setScore2(Double.parseDouble(
							score2Field.getText()));
				}
			}
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				update(arg0);
			}
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				update(arg0);
			}
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				update(arg0);
			}
		});
		annotationField.getDocument().addDocumentListener(
				new DocumentListener() {
			private void update(DocumentEvent de) {
				// Update annotation for selected entry
				if(selectedEntry != null) {
					selectedEntry.setAnnotation(annotationField.getText());
				}
			}
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				update(arg0);
			}
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				update(arg0);
			}
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				update(arg0);
			}
		});
		score1Panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				score1Panel.getPreferredSize().height));
		score2Panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				score2Panel.getPreferredSize().height));
		annotationField.setBorder(ENTRY_BORDER);
		annotationPanel.add(score1Panel);
		annotationPanel.add(score2Panel);
		annotationPanel.add(annotationField);
		controlPanel.add(annotationPanel);
		
		// Add the export button
		JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		exportButton = new JButton("Export files and annotations");
		exportButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if(jfc.showSaveDialog(Combocheck.Frame) ==
						JFileChooser.APPROVE_OPTION) {
					File dir = jfc.getSelectedFile();
					ReportExporter re = new ReportExporter(dir, entries);
					re.exportEntries();
				}
			}
		});
		exportPanel.add(exportButton);
		exportPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				exportPanel.getPreferredSize().height));
		controlPanel.add(exportPanel);
		
		contentPanel.setDividerLocation((int)
				(DIVIDER_RATIO * Combocheck.PROGRAM_WIDTH));
		
		// Message before populating the review panel
		add(new JLabel(
				"You must first perform a scan, or load a previous one:"));
		JButton loadButton = new JButton("Load scan");
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Combocheck scans (scn)", "scn");
		jfc.setFileFilter(filter);
		loadButton.addActionListener(new ActionListener() {
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
					Combocheck.Frame.getTabbedPane().getReviewPanel().
							populatePanel();
					populatePanel();
				}
			}
		});
		add(loadButton);
	}
	
	/**
	 * Adds a file pair to the report panel screen
	 * @param fp The FilePair object
	 */
	public void addPair(FilePair fp) {
		ReportEntry rp = new ReportEntry(fp);
		if(entries.isEmpty()) {
			setSelectedEntry(rp);
		}
		if(!entries.contains(rp)) {
			entries.add(rp);
		}
		revalidate();
	}
	
	/**
	 * Set the selected entry
	 * @param rp The ReportEntry object
	 */
	public void setSelectedEntry(ReportEntry rp) {
		selectedEntry = rp;
		if(rp == null) {
			score1Label.setText("");
			score2Label.setText("");
			score1Field.setText("");
			score2Field.setText("");
			annotationField.setText("");
		} else {
			FilePair fp = rp.getFilePair();
			score1Label.setText("Score for " + fp.getShortenedFile1());
			score2Label.setText("Score for " + fp.getShortenedFile2());
			for(ReportEntry entry : entries) {
				if(entry.getFilePair().equals(fp)) {
					DecimalFormat df = new DecimalFormat("0.##");
					Double D1 = new Double(entry.getScore1());
					Double D2 = new Double(entry.getScore2());
					String score1msg = D1.equals(Double.NaN) ? "" :
						df.format(entry.getScore1());
					String score2msg = D2.equals(Double.NaN) ? "" :
						df.format(entry.getScore2());
					score1Field.setText(score1msg);
					score2Field.setText(score2msg);
					annotationField.setText(entry.getAnnotation());
					break;
				}
			}
		}
		revalidate();
	}
	
	/**
	 * Getter for the selected entry
	 * @return The selected ReportENtry object
	 */
	public ReportEntry getSelectedEntry() {
		return selectedEntry;
	}
	
	/**
	 * Populate the report panel once there is content to do so
	 */
	public void populatePanel() {
		
		// Show the contents for the panel
		removeAll();
		setLayout(new BorderLayout());
		add(contentPanel, BorderLayout.CENTER);
	}
	
	/**
	 * Remove a report entry from the list
	 * @param entry The ReportEntry object to remove
	 */
	public void removeEntry(ReportEntry entry) {
		entries.remove(entry);
		if(entry.equals(selectedEntry)) {
			if(entries.isEmpty()) {
				setSelectedEntry(null);
			} else {
				setSelectedEntry(entries.get(0));
			}
		} else {
			revalidate(); // setSelectedPair revalidates in other cases
		}
	}
	
	/**
	 * Getter for report entries
	 * @return A list of the entries
	 */
	public List<ReportEntry> getReportEntries() {
		return entries;
	}
	
	/**
	 * Setter for report entries
	 * @param entries The entries to set
	 */
	public void setReportEntries(List<ReportEntry> entries) {
		this.entries.clear();
		this.entries.addAll(entries);
		if(entries.isEmpty()) {
			setSelectedEntry(null);
		} else {
			setSelectedEntry(entries.get(0));
		}
		revalidate();
	}
	
	/**
	 * Calls custom revalidate on the pair scroll pane in addition to regular
	 */
	@Override
	public void revalidate() {
		if(pairScrollPaneContents != null) {
			pairScrollPaneContents.revalidate();
		}
		super.revalidate();
	}
}
