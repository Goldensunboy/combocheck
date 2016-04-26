package com.combocheck.global;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import com.combocheck.algo.Algorithm;
import com.combocheck.ui.report.ReportEntry;

/**
 * This class contains static helper functions for saving and loading scan
 * state information.
 * 
 * @author Andrew Wilder
 */
public class ScanLoader {

	/**
	 * Save scan results to a file
	 * @param filename
	 */
	public static void saveScan(String filename) {
		File outfile = new File(filename);
		try {
			FileOutputStream fos = new FileOutputStream(outfile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			for(Algorithm a : Combocheck.algorithms) {
				if(a.isEnabled()) {
					oos.writeBoolean(true);
					oos.writeObject(a.getPairScores());
				} else {
					oos.writeBoolean(false);
				}
			}
			oos.writeObject(Combocheck.getReportEntries());
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Load scan results from a file
	 * @param filename
	 */
	public static void loadScan(String filename) {
		File infile = new File(filename);
		try {
			FileInputStream fis = new FileInputStream(infile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			for(Algorithm a : Combocheck.algorithms) {
				if(ois.readBoolean()) {
					a.setEnabled(true);
					@SuppressWarnings("unchecked")
					HashMap<FilePair, Integer> pairScores =
							(HashMap<FilePair, Integer>) ois.readObject();
					a.setPairScores(pairScores);
				} else {
					a.setEnabled(false);
				}
			}
			@SuppressWarnings("unchecked")
			List<ReportEntry> entries = (List<ReportEntry>) ois.readObject();
			Combocheck.setReportEntries(entries);
			ois.close();
		} catch(Exception e) {
			JOptionPane.showMessageDialog(Combocheck.Frame,
					"Invalid or corrupt scan file: " + filename + "\n" +
					e.getMessage(), "Error loading scan",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
}
