package com.combocheck.ui.report;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.combocheck.global.FilePair;

/**
 * This class represents the report exporting code which creates annotated HTML
 * 
 * @author Andrew Wilder
 */
public class ReportExporter {

	/** Fields for the ReportExporter */
	private File dir;
	private List<ReportEntry> entries;
	
	/**
	 * Construct the new ReportExporter object
	 * @param dir The directory to export to
	 * @param entries The ReportEntry objects to export
	 */
	public ReportExporter(File dir, List<ReportEntry> entries) {
		this.dir = dir;
		this.entries = entries;
	}
	
	/**
	 * Normalizes 2 file names by creating formatted names based off the first
	 * directory in the path that differs.
	 * @param fname1 First file name
	 * @param fname2 Second file name
	 * @return An array with 2 indices representing the normalized names
	 */
	private String[] NormalizeNames(String fname1, String fname2) {
		String[] names = new String[2];
		Path path1 = Paths.get(fname1);
		Path path2 = Paths.get(fname2);
		String name1 = path1.iterator().next().toString();
		String name2 = path2.iterator().next().toString();
		names[0] = "";
		names[1] = "";
		for(int i = 0; i < name1.length(); ++i) {
			String c = name1.charAt(i) + "";
			if(Pattern.matches("\\p{Alnum}", c)) {
				names[0] += c.toLowerCase();
			} else if(" ".equals(c)) {
				names[0] += "_";
			}
		}
		for(int i = 0; i < name2.length(); ++i) {
			String c = name2.charAt(i) + "";
			if(Pattern.matches("\\p{Alnum}", c)) {
				names[1] += c.toLowerCase();
			} else if(" ".equals(c)) {
				names[1] += "_";
			}
		}
		return names;
	}
	
	/**
	 * Export the entries to the directory given upon construction
	 */
	public void exportEntries() {
		
		// Create the top-level directory for the export files
		File top;
		if(dir.exists()) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			top = new File(dir, "Combocheck Report " + df.format(new Date()));
		} else {
			top = dir;
		}
		top.mkdir();
		
		// For each entry...
		for(ReportEntry entry : entries) {
			FilePair fp = entry.getFilePair();
			
			// Create folder for contents
			String[] names = NormalizeNames(fp.getShortenedFile1(),
					fp.getShortenedFile2());
			File entryDir = new File(top, names[0] + "_" + names[1]);
			entryDir.mkdir();
			
			// Copy files to the folder
			Path dest1 = Paths.get(entryDir.getAbsolutePath(),
					names[0] + "_" + new File(fp.getFile1()).getName());
			Path dest2 = Paths.get(entryDir.getAbsolutePath(),
					names[1] + "_" + new File(fp.getFile2()).getName());
			try {
				Files.copy(Paths.get(fp.getFile1()), dest1);
				Files.copy(Paths.get(fp.getFile2()), dest2);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			// Generate the HTML file
			// TODO
		}
	}
}
