package com.combocheck.global;

import com.combocheck.ui.CombocheckFrame;

/**
 * This class represents the main executable class of Combocheck.
 * It also houses static singleton variables, such as UI elements.
 * 
 * @author Andrew Wilder
 */
public class Combocheck {

	/**
	 * Create the Combocheck frame and initialize UI elements.
	 * If command line arguments are passed, handle them here.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Determine if command line arguments were passed
		if(args.length == 1) {
			
			// Normal invocation of Combocheck UI
			new CombocheckFrame().setVisible(true);
			
		} else {
			
			// TODO
			
		}
	}
}
