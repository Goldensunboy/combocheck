package com.combocheck.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import com.combocheck.global.Combocheck;

/**
 * This class represents the frame in which the application is housed.
 * 
 * @author Andrew Wilder
 */
public class CombocheckFrame extends JFrame {

	/**
	 * Construct the Combocheck frame
	 */
	public CombocheckFrame() {
		
		// Set the window properties
		super(Combocheck.PROGRAM_TITLE);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				// TODO
				System.out.println("User closed Combocheck");
				System.exit(0);
			}
		});
		
		// Add the content to the window
		add(new CombocheckTabbedPane());
		pack();
	}
}
