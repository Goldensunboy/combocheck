package com.combocheck.ui.review;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

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
public class ReviewGraph extends JPanel {

	/** Global constants */
	private static final int GRAPH_HEIGHT = 300;
	
	/** Information pertaining to drawing the graph */
	private Algorithm algo = null;
	private FilePair selectedPair = null;
	private double avg, dev;
	private int min, max;
	
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
		for(Integer i : algo.getFileScores().values()) {
			sum += i;
			if(i > max) {
				max = i;
			}
			if(i < min) {
				min = i;
			}
		}
		avg = sum / algo.getFileScores().size();
		
		// Get the standard deviation
		sum = 0;
		for(Integer i : algo.getFileScores().values()) {
			sum += Math.pow(i - avg, 2);
		}
		dev = Math.sqrt(sum / algo.getFileScores().size());
		
		System.out.println("Min: " + min);
		System.out.println("Max: " + max);
		System.out.println("Avg: " + avg);
		System.out.println("Dev: " + dev);
		repaint();
		System.out.println("W: " + getWidth() + "\nH: " + getHeight());
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
			g.setColor(Color.WHITE);
			g.fillRect(1, 1, getWidth() - 3, getHeight() - 3);
			g.setColor(Color.GREEN);
			g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
			g.setColor(Color.BLACK);
			g.drawString(algo.toString(), 10, 20);
		}
	}
}
