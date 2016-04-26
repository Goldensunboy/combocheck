package com.combocheck.ui.report;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;

import java.io.Serializable;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import com.combocheck.global.FilePair;
import com.combocheck.ui.ComparisonDialog;
import com.combocheck.ui.report.ReportPanel;

/**
 * This class represents a file pair entry object on the reporting screen.
 * It packages a file pair with an annotation and scores.
 * 
 * @author Andrew Wilder
 */
public class ReportEntry extends JPanel {
	private static final long serialVersionUID = 2133403681425949254L;

	/** Global information */
	private static ReportPanel rp = null;
	
	/** Fields for a report entry */
	private FilePair fp;
	private String annotation = "";
	private double score1 = Double.NaN;
	private double score2 = Double.NaN;
	private JTextPane textPane = new JTextPane();
	MouseListener clickListener = new ReportMouseListener();
	ActionListener removeListener = new ReportRemoveListener();
	
	/**
	 * Construct a new pair entry for the report panel
	 * @param fp The associated file pair object
	 */
	public ReportEntry(final FilePair fp) {
		this.fp = fp;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Create the UI for this panel
		textPane.setEditable(false);
		textPane.setOpaque(false);
		textPane.setText("\n\n" + fp.getShortenedFile1() + "\n" +
				fp.getShortenedFile2());
		add(textPane);
		addMouseListener(clickListener);
		textPane.addMouseListener(clickListener);
		
		// Add a button to remove this pair
		JButton removeButton = new JButton("Remove");
		removeButton.addActionListener(removeListener);
		JPanel removeButtonPanel =
				new JPanel(new FlowLayout(FlowLayout.LEADING));
		removeButtonPanel.setOpaque(false);
		removeButtonPanel.add(removeButton);
		add(removeButtonPanel);
		setMaximumSize(new Dimension(Integer.MAX_VALUE,
				getPreferredSize().height));
	}
	
	/**
	 * This is a serializable version of MouseListener
	 * 
	 * @author Andrew Wilder
	 */
	private class ReportMouseListener implements MouseListener, Serializable {
		private static final long serialVersionUID = 6026962895019514172L;
		@Override
		public void mousePressed(MouseEvent me) {
			if(me.getButton() == MouseEvent.BUTTON1) {
				if(ReportEntry.this.equals(rp.getSelectedEntry())) {
					// Open the comparison dialog
					new ComparisonDialog(fp).setVisible(true);
				} else {
					// Set the selected pair
					rp.setSelectedEntry(ReportEntry.this);
					rp.repaint();
				}
			}
		}
		@Override
		public void mouseClicked(MouseEvent arg0) {
		}
		@Override
		public void mouseEntered(MouseEvent arg0) {	
		}
		@Override
		public void mouseExited(MouseEvent arg0) {
		}
		@Override
		public void mouseReleased(MouseEvent arg0) {
		}
	}
	
	/**
	 * This is a serializable version of ActionListener
	 * 
	 * @author Andrew Wilder
	 */
	private class ReportRemoveListener implements ActionListener, Serializable {
		private static final long serialVersionUID = -6930616368240913900L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			rp.removeEntry(ReportEntry.this);
		}
	}
	
	/**
	 * Draw an outline for this panel if it is selected
	 */
	@Override
	public void paintComponent(Graphics g) {
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
		if(equals(rp.getSelectedEntry())) {
			g.setColor(Color.RED);
			g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
			String msg = "Click to compare";
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D msgRect = fm.getStringBounds(msg, g);
			g.drawString(msg, (int) (getWidth() - msgRect.getWidth()) / 2,
					(int) msgRect.getHeight() + 5);
		}
	}
	
	/**
	 * Check if two report entries are equal to one another, done by comparing
	 * the contained file pairs.
	 * @param obj The other object being compared
	 */
	public boolean equals(Object obj) {
		if(obj instanceof ReportEntry) {
			ReportEntry rp = (ReportEntry) obj;
			return fp.equals(rp.fp);
		} else {
			return false;
		}
	}
	
	/**
	 * Set the static instance of ReportPanel for finding the selected pair
	 * @param rp The static ReportPanel instance
	 */
	public static void SetReportPanel(ReportPanel rp) {
		ReportEntry.rp = rp;
	}
	
	/**
	 * Getter for the FilePair object conatained
	 * @return The FilePair
	 */
	public FilePair getFilePair() {
		return fp;
	}
	
	/**
	 * Getter for the annotation field
	 * @return The report pair annotation
	 */
	public String getAnnotation() {
		return annotation;
	}
	
	/**
	 * Setter for the annotation
	 * @param annotation The pair annotation for reporting
	 */
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}
	
	/**
	 * Getter for score1
	 * @return Score 1
	 */
	public double getScore1() {
		return score1;
	}
	
	/**
	 * Setter for score 1
	 * @param score The score
	 */
	public void setScore1(double score) {
		score1 = score;
	}
	
	/**
	 * Getter for score2
	 * @return Score 2
	 */
	public double getScore2() {
		return score2;
	}
	
	/**
	 * Setter for score 2
	 * @param score The score
	 */
	public void setScore2(double score) {
		score2 = score;
	}
}
