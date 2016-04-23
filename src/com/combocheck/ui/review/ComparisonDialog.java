package com.combocheck.ui.review;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;

import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * This class represents the dialog that shows two files side by side
 * 
 * @author Andrew Wilder
 */
@SuppressWarnings("serial")
public class ComparisonDialog extends JDialog {
	
	/** Global properties of the comparison dialog */
	private static final Font CD_FONT = new Font("Courier New", Font.PLAIN, 12);

	/**
	 * Construct a new comparison dialog
	 * @param fp
	 */
	public ComparisonDialog(FilePair fp) {
		setTitle("Comparing \"" + fp.getShortenedFile1() + "\" and \"" +
				fp.getShortenedFile2() + "\"");
		
		// Process files
		List<Integer> hashes1 = new ArrayList<Integer>();
		List<Integer> hashes2 = new ArrayList<Integer>();
		List<String> lines1 = new ArrayList<String>();
		List<String> lines2 = new ArrayList<String>();
		try {
			File f1 = new File(fp.getFile1());
			File f2 = new File(fp.getFile2());
			Scanner sc = new Scanner(f1);
			while(sc.hasNext()) {
				String line = sc.nextLine();
				hashes1.add(line.hashCode());
				lines1.add(line);
			}
			sc.close();
			sc = new Scanner(f2);
			while(sc.hasNext()) {
				String line = sc.nextLine();
				hashes2.add(line.hashCode());
				lines2.add(line);
			}
			sc.close();
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}
		
		// Construct UI
		JSplitPane splitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT, true);
		JPanel side1 = new JPanel();
		side1.setLayout(new BoxLayout(side1, BoxLayout.Y_AXIS));
		JPanel side2 = new JPanel();
		side2.setLayout(new BoxLayout(side2, BoxLayout.Y_AXIS));
		splitPane.setLeftComponent(side1);
		splitPane.setRightComponent(side2);
		JLabel label1 = new JLabel(fp.getShortenedFile1());
		label1.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				label1.getPreferredSize().height));
		JLabel label2 = new JLabel(fp.getShortenedFile2());
		label2.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				label2.getPreferredSize().height));
		JScrollPane scrollPane1 = new JScrollPane(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JScrollPane scrollPane2 = new JScrollPane(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		side1.add(label1);
		side1.add(scrollPane1);
		side2.add(label2);
		side2.add(scrollPane2);
		label1.setAlignmentX(Component.LEFT_ALIGNMENT);
		label2.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		// Populate side 1
		// TODO format the text better
		JPanel pane1 = new JPanel();
		pane1.setBackground(Color.WHITE);
		pane1.setLayout(new BoxLayout(pane1, BoxLayout.Y_AXIS));
		String htmlHead = "<html><pre>";
		String htmlTail = "</pre></html>";
		Dimension maxSizeDim = new Dimension(Integer.MAX_VALUE,
				new JLabel("test").getPreferredSize().height);
		int digits = (int) Math.log10(lines1.size()) + 1;
		int lineNum = 0;
		for(String line : lines1) {
			line = String.format("%0" + digits + "d: ", lineNum++) + line;
			line = StringEscapeUtils.escapeHtml4(line);
			JLabel label = new JLabel(htmlHead + line + htmlTail);
			label.setFont(CD_FONT);
			if((lineNum & 1) == 0) {
				label.setOpaque(true);
				label.setBackground(Color.YELLOW);
			}
			label.setMaximumSize(maxSizeDim);
			pane1.add(label);
			label.setAlignmentX(Component.LEFT_ALIGNMENT);
		}
		JViewport jvp1 = new JViewport();
		jvp1.add(pane1);
		scrollPane1.setViewportView(jvp1);
		
		// Populate side 2
		JPanel pane2 = new JPanel();
		pane2.setBackground(Color.WHITE);
		pane2.setLayout(new BoxLayout(pane2, BoxLayout.Y_AXIS));
		digits = (int) Math.log10(lines2.size()) + 1;
		lineNum = 0;
		for(String line : lines2) {
			line = String.format("%0" + digits + "d: ", lineNum++) + line;
			line = StringEscapeUtils.escapeHtml4(line);
			JLabel label = new JLabel(htmlHead + line + htmlTail);
			label.setFont(CD_FONT);
			label.setMaximumSize(maxSizeDim);
			pane2.add(label);
			label.setAlignmentX(Component.LEFT_ALIGNMENT);
		}
		JViewport jvp2 = new JViewport();
		jvp2.add(pane2);
		scrollPane2.setViewportView(jvp2);
		
		// Set the listeners for the scrollbars to sync
		JScrollBar vs1 = scrollPane1.getVerticalScrollBar();
		JScrollBar vs2 = scrollPane2.getVerticalScrollBar();
		CDScrollListener listener1 = new CDScrollListener(vs2);
		CDScrollListener listener2 = new CDScrollListener(vs1);
		vs1.setUnitIncrement(5);
		vs2.setUnitIncrement(5);
		vs1.addAdjustmentListener(listener1);
		vs2.addAdjustmentListener(listener2);
		
		// Finalize the UI
		splitPane.setPreferredSize(new Dimension(Combocheck.PROGRAM_WIDTH + 100,
				Combocheck.PROGRAM_HEIGHT + 100));
		add(splitPane);
		pack();
		splitPane.setDividerLocation(0.5);
	}
	
	/**
	 * This class represents the listener which syncs scroll movements of the
	 * scroll bars for both viewing panes.
	 * 
	 * @author Andrew Wilder
	 */
	private class CDScrollListener implements AdjustmentListener {

		/** The other listener */
		private JScrollBar other;
		private boolean initiator = true; // prevents infinite recursion
		
		/**
		 * Construct a new listener with another paired listener
		 * @param other null if first listener, or the listener to pair
		 */
		public CDScrollListener(JScrollBar other) {
			this.other = other;
		}
		
		/**
		 * Sync the movements of the scroll bars
		 */
		@Override
		public void adjustmentValueChanged(AdjustmentEvent ae) {
			if(initiator) {
				JScrollBar jsb = (JScrollBar) ae.getAdjustable();
				int max = jsb.getMaximum() - jsb.getModel().getExtent();
				double scrollPercent = (double) jsb.getValue() / max;
				CDScrollListener cdsl = (CDScrollListener)
						other.getAdjustmentListeners()[0];
				cdsl.initiator = false;
				int otherMax = other.getMaximum() -
						other.getModel().getExtent();
				other.setValue((int) (scrollPercent * otherMax));
				cdsl.initiator = true;
			}
		}
	}
}
