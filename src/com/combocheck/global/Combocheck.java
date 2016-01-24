package com.combocheck.global;

import com.combocheck.ui.CombocheckFrame;

/**
 * This class represents the main executable class of Combocheck.
 * It also houses static singleton variables, such as UI elements.
 * 
 * @author Andrew Wilder
 */
public class Combocheck {
	
	// Program constants
	public static final String PROGRAM_TITLE = "Combocheck";
	public static final int PROGRAM_WIDTH = 900;
	public static final int PROGRAM_HEIGHT = 700;

	/**
	 * Create the Combocheck frame and initialize UI elements.
	 * If command line arguments are passed, handle them here.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Determine if command line arguments were passed
		if(args.length < 2) {
			
			// Normal invocation of Combocheck UI
			new CombocheckFrame().setVisible(true);
			
		} else {
			
			for(String s : args) {
				System.out.println(s);
			}
		}
	}
}
