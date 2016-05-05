package com.combocheck.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;

import com.combocheck.algo.Algorithm;
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
	private static final Color MATCH_COLOR = Color.WHITE;
	private static final Color UNIQUE_COLOR = new Color(0xFFEFEF);
	private static final Color EMPTY_COLOR = Color.WHITE;

	/**
	 * Construct a new comparison dialog
	 * @param fp
	 */
	public ComparisonDialog(FilePair fp) {
		super(new DummyFrame("Comparison"), true);
		setTitle("Comparing \"" + fp.getShortenedFile1() + "\" and \"" +
				fp.getShortenedFile2() + "\"");
		
		// Process files
		List<String> lines1 = new ArrayList<String>();
		List<String> lines2 = new ArrayList<String>();
		Algorithm.ComputeLineMatches(fp.getFile1(), fp.getFile2(),
				lines1, lines2);
		
		// Construct UI
		JSplitPane splitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT, true);
		JPanel side1 = new JPanel();
		side1.setLayout(new BoxLayout(side1, BoxLayout.Y_AXIS));
		JPanel side2 = new JPanel();
		side2.setLayout(new BoxLayout(side2, BoxLayout.Y_AXIS));
		splitPane.setLeftComponent(side1);
		splitPane.setRightComponent(side2);
		JLabel fileLabel1 = new JLabel(fp.getShortenedFile1());
		fileLabel1.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				fileLabel1.getPreferredSize().height));
		JLabel fileLabel2 = new JLabel(fp.getShortenedFile2());
		fileLabel2.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				fileLabel2.getPreferredSize().height));
		JScrollPane scrollPane1 = new JScrollPane(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JScrollPane scrollPane2 = new JScrollPane(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		side1.add(fileLabel1);
		side1.add(scrollPane1);
		side2.add(fileLabel2);
		side2.add(scrollPane2);
		
		// Populate both sides
		JPanel pane1 = new JPanel();
		pane1.setBackground(Color.WHITE);
		pane1.setLayout(new BoxLayout(pane1, BoxLayout.Y_AXIS));
		JPanel pane2 = new JPanel();
		pane2.setBackground(Color.WHITE);
		pane2.setLayout(new BoxLayout(pane2, BoxLayout.Y_AXIS));
		String htmlHead = "<html><pre>";
		String htmlTail = "</pre></html>";
		Dimension maxSizeDim = new Dimension(Integer.MAX_VALUE,
				new JLabel("test").getPreferredSize().height);
		int digits1 = (int) Math.log10(lines1.size()) + 1;
		int digits2 = (int) Math.log10(lines2.size()) + 1;
		int lineNum1 = 0, lineNum2 = 0;
		if(lines1.size() != lines2.size()) {
			System.err.println("Error in line matching calculation: {" +
					fp.getShortenedFile1() + ", " + fp.getShortenedFile2() +
					"}\n" +
					"This shouldn't occur. Let the author of combocheck know!");
			setModal(false);
			//return;
		}
		int lineNum = Math.min(lines1.size(), lines2.size());
		for(int i = 0; i < lineNum; ++i) {
			String line1 = lines1.get(i);
			String line2 = lines2.get(i);
			JLabel label1 = new JLabel();
			JLabel label2 = new JLabel();
			label1.setFont(CD_FONT);
			label2.setFont(CD_FONT);
			label1.setOpaque(true);
			label2.setOpaque(true);
			label1.setMaximumSize(maxSizeDim);
			label2.setMaximumSize(maxSizeDim);
			if(line1 != null) {
				String stripped2 = line2 == null ? null :
						line2.replaceAll(" |\t", "");
				if(line1.replaceAll(" |\t", "").equals(stripped2)) {
					label1.setBackground(MATCH_COLOR);
				} else {
					label1.setBackground(UNIQUE_COLOR);
				}
				String formatted = String.format(
						"%0" + digits1 + "d: ", lineNum1++) + line1;
				formatted = StringEscapeUtils.escapeHtml4(formatted);
				label1.setText(htmlHead + formatted + htmlTail); 
			} else {
				label1.setBackground(EMPTY_COLOR);
			}
			if(line2 != null) {
				String stripped1 = line1 == null ? null :
						line1.replaceAll(" |\t", "");
				if(line2.replaceAll(" |\t", "").equals(stripped1)) {
					label2.setBackground(MATCH_COLOR);
				} else {
					label2.setBackground(UNIQUE_COLOR);
				}
				String formatted = String.format(
						"%0" + digits2 + "d: ", lineNum2++) + line2;
				formatted = StringEscapeUtils.escapeHtml4(formatted);
				label2.setText(htmlHead + formatted + htmlTail); 
			} else {
				label2.setBackground(EMPTY_COLOR);
			}
			pane1.add(label1);
			pane2.add(label2);
		}
		
		// Configure the viewports and scrollbar
		JViewport jvp1 = new JViewport();
		jvp1.add(pane1);
		scrollPane1.setViewportView(jvp1);
		JViewport jvp2 = new JViewport();
		jvp2.add(pane2);
		scrollPane2.setViewportView(jvp2);
		JScrollBar jsb = scrollPane1.getVerticalScrollBar();
		jsb.setUnitIncrement(5);
		scrollPane2.setVerticalScrollBar(jsb);
		
		// Finalize the UI
		splitPane.setPreferredSize(new Dimension(Combocheck.PROGRAM_WIDTH + 100,
				Combocheck.PROGRAM_HEIGHT + 100));
		add(splitPane);
		pack();
		splitPane.setDividerLocation(0.5);
	}
	
	/**
	 * Hide the dummy JFrame when the dialog is closed
	 */
	@Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible) {
            ((DummyFrame) getParent()).dispose();
        }
    }
    
	/**
	 * This class represents a dummy JFrame for giving the comparison dialog a
	 * taskbar entry.
	 * 
	 * @author Andrew Wilder
	 */
    private static class DummyFrame extends JFrame {
    	
    	/**
    	 * Construct a new dummy frame for the comparison dialog's taskbar entry
    	 * @param title The title for the frame
    	 */
    	public DummyFrame(String title) {
            super(title);
            setUndecorated(true);
            setVisible(true);
            setLocationRelativeTo(null);
        }
    }
}
