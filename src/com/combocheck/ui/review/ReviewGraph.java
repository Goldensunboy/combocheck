package com.combocheck.ui.review;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import java.util.Arrays;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.combocheck.algo.Algorithm;
import com.combocheck.global.FilePair;

/**
 * This class represents the graph displayed while reviewing pair scores
 * 
 * @author Andrew Wilder
 */
@SuppressWarnings("serial")
public class ReviewGraph extends JPanel {

	/** Global constants */
	private static final int GRAPH_MARGIN = 25;
	private static final int COLUMN_GRANULARITY = 200;
	private static final int TEXT_OFFSET = 5;
	
	/** Information pertaining to drawing the graph */
	private Algorithm algo = null;
	private FilePair selectedPair = null;
	private double avg, dev;
	private int min, max, range;
	
	/**
	 * Construct a new graph object. Only a height is defined by the contained
	 * vertical strut, because the parent container uses BoxLayout.Y_AXIS which
	 * maximizes the width.
	 */
	public ReviewGraph() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createRigidArea(new Dimension(400, 300)));
	}
	
	/**
	 * Populate the data set for this graph
	 * @param algo The algorithm from which to draw scores
	 */
	public void PopulateDataSet(Algorithm algo) {
		this.algo = algo;
		
		// Get the average
		double sum = 0;
		min = Integer.MAX_VALUE;
		max = Integer.MIN_VALUE;
		for(Integer i : algo.getPairScores().values()) {
			sum += i;
			if(i > max) {
				max = i;
			}
			if(i < min) {
				min = i;
			}
		}
		range = max - min + 1;
		avg = sum / algo.getPairScores().size();
		
		// Get the standard deviation
		sum = 0;
		for(Integer i : algo.getPairScores().values()) {
			sum += Math.pow(i - avg, 2);
		}
		dev = Math.sqrt(sum / algo.getPairScores().size());
		
		repaint();
	}
	
	/**
	 * Set the selected pair for the graph
	 * @param fp
	 */
	public void setSelectedPair(FilePair fp) {
		selectedPair = fp;
		repaint();
	}
	
	/**
	 * Draw the graph
	 */
	@Override
	public void paintComponent(Graphics g) {
		if(algo != null) {
			
			// Draw the graph borders
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
			g.setColor(Color.BLACK);
			g.drawRect(GRAPH_MARGIN, GRAPH_MARGIN, getWidth() -
					(GRAPH_MARGIN << 1), getHeight() - (GRAPH_MARGIN << 1));
			
			// Draw the graph legend text
			g.drawString(algo.toString(), 8, 18);
			String Xtitle = "Score";
			String Ytitle = "Frequency";
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D Xrect = fm.getStringBounds(Xtitle, g);
			Rectangle2D Yrect = fm.getStringBounds(Ytitle, g);
			g.drawString(Xtitle, getWidth() / 2 -(int) (Xrect.getWidth() / 2),
					getHeight() - GRAPH_MARGIN + (int) Xrect.getHeight() +
					TEXT_OFFSET);
			Font oldFont = g.getFont();
			AffineTransform rotation = new AffineTransform();
			rotation.rotate(-Math.PI / 2);
			Font rotatedFont = oldFont.deriveFont(rotation);
			g.setFont(rotatedFont);
			g.drawString(Ytitle, GRAPH_MARGIN - TEXT_OFFSET, getHeight() / 2 +
					(int) (Yrect.getWidth() / 2));
			g.setFont(oldFont);
			
			// Draw the graph legend
			String minText = "" + min;
			String maxText = "" + max;
			Rectangle2D minRect = fm.getStringBounds(minText, g);
			Rectangle2D maxRect = fm.getStringBounds(maxText, g);
			g.drawString(minText, GRAPH_MARGIN - (int) (minRect.getWidth() / 2),
					getHeight() - GRAPH_MARGIN + (int) minRect.getHeight() +
					TEXT_OFFSET);
			g.drawString(maxText, getWidth() - GRAPH_MARGIN -
					(int) (maxRect.getWidth() / 2), getHeight() -
					GRAPH_MARGIN + (int) maxRect.getHeight() + TEXT_OFFSET);
			
			// Draw average line
			int avgX = (int) ((avg - min) / range * (getWidth() - 2 *
					GRAPH_MARGIN)) + GRAPH_MARGIN;
			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(avgX, GRAPH_MARGIN + 1, avgX, getHeight() -
					GRAPH_MARGIN - 1);
			g.drawString("Avg: " + (int) avg, avgX + TEXT_OFFSET, getHeight() -
					GRAPH_MARGIN - TEXT_OFFSET);
			
			// Draw the distribution of scores
			g.setColor(Color.BLACK);
			int[] heights = new int[COLUMN_GRANULARITY];
			Arrays.fill(heights, 0);
			for(Map.Entry<FilePair, Integer> e :
					algo.getPairScores().entrySet()) {
				++heights[(e.getValue() - min) * COLUMN_GRANULARITY / range];
			}
			int maxHeight = 0;
			for(int i : heights) {
				maxHeight = Math.max(maxHeight, i);
			}
			int startX = GRAPH_MARGIN + 1;
			int width = getWidth() - (GRAPH_MARGIN << 1) - 1;
			int startY = getHeight() - (GRAPH_MARGIN + 1);
			int height = getHeight() - (GRAPH_MARGIN << 1) - 1;
			Point[] points = new Point[COLUMN_GRANULARITY];
			for(int i = 0; i < COLUMN_GRANULARITY; ++i) {
				points[i] = new Point(startX + i * width /
						(COLUMN_GRANULARITY - 1),
						startY - heights[i] * height / maxHeight);
			}
			for(int i = 1; i < COLUMN_GRANULARITY; ++i) {
				g.drawLine(points[i - 1].x, points[i - 1].y,
						points[i].x, points[i].y);
			}
			
			// Draw the position of the selected pair
			g.setColor(Color.RED);
			int selScore = algo.getPairScores().get(selectedPair);
			int selX = (int) ((selScore - min) / range * (getWidth() - 2 *
					GRAPH_MARGIN)) + GRAPH_MARGIN;
			g.drawLine(selX, GRAPH_MARGIN + 1, selX, getHeight() -
					GRAPH_MARGIN - 1);
			g.drawString("Selected: " + selScore, selX + TEXT_OFFSET,
					getHeight() - GRAPH_MARGIN - TEXT_OFFSET);
		}
	}
}
