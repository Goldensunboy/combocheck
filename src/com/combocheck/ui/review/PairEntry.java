package com.combocheck.ui.review;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import com.combocheck.global.FilePair;
import com.combocheck.ui.ComparisonDialog;

/**
 * This class represents the pair entries for the review panel. A file pair is
 * associated with it. If the contained file pair is the one currently selected
 * by the review panel, draw a border around this panel as well.
 * 
 * @author Andrew Wilder
 */
@SuppressWarnings("serial")
public class PairEntry extends JPanel {

	/** Global information */
	private static ReviewPanel rp = null;
	
	/** Data specific to this pair entry */
	private FilePair fp;
	private int index;
	private JTextPane textPane = new JTextPane();
	
	/**
	 * Construct a new pair entry for the review panel
	 * @param fp The associated file pair object
	 * @param index The index of this file pair with the selected algorithm
	 * @param score The score of this file pair with the selected algorithm
	 */
	public PairEntry(final FilePair fp, int index, int score) {
		this.fp = fp;
		this.index = index;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Create the UI for this panel
		textPane.setEditable(false);
		textPane.setOpaque(false);
		textPane.setText("Pair: " + index + "\nScore: " + score + "\n" +
				fp.getShortenedFile1() + "\n" + fp.getShortenedFile2());
		add(textPane);
		setMaximumSize(new Dimension(Integer.MAX_VALUE,
				getPreferredSize().height));
		MouseListener clickListener = new MouseListener() {
			@Override
			public void mousePressed(MouseEvent me) {
				if(me.getButton() == MouseEvent.BUTTON1) {
					if(fp.equals(rp.getSelectedPair())) {
						// Open the comparison dialog
						new ComparisonDialog(fp).setVisible(true);
					} else {
						// Set the selected pair
						rp.setSelectedPair(fp);
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
		};
		addMouseListener(clickListener);
		textPane.addMouseListener(clickListener);
	}
	
	/**
	 * Getter for the stored file pair
	 * @return
	 */
	public FilePair getFilePair() {
		return fp;
	}
	
	/**
	 * Getter for the index of this file pair entry
	 * @return
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * Set the static instance of ReviewPanel for finding the selected pair
	 * @param rp The static ReviewPanel instance
	 */
	public static void SetReviewPanel(ReviewPanel rp) {
		PairEntry.rp = rp;
	}
	
	/**
	 * Draw an outline for this panel if it is selected
	 */
	@Override
	public void paintComponent(Graphics g) {
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
		if(fp.equals(rp.getSelectedPair())) {
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
	 * Return the index for the represented file pair
	 */
	@Override
	public String toString() {
		return index + "";
	}
}
