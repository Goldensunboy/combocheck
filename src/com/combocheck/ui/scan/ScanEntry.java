package com.combocheck.ui.scan;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * This class represents an entry for locating files in the scan tab.
 * An entry consists of a path to the folder in which to find files, and a
 * POSIX regular expression that describes the files to locate. If a single
 * file is specified, the regex member variable is unused.
 * 
 * @author Andrew Wilder
 */
public class ScanEntry {

	/**
	 * Enumerate the types of scan entries
	 *   Normal: All files found against each other
	 *   OldSemester: All other files against these as well
	 *   SingleFile: One file
	 */
	public static enum ScanEntryType {
		Normal,
		OldSemester,
		SingleFile
	}
	
	/** The path to locate files for this scan entry */
	private String path = null;
	
	/** The regular expression corresponding to the files to locate */
	private String regex = null;
	
	/** The type of scan entry this represents */
	private ScanEntryType type = null;
	
	/**
	 * Construct a new scan entry
	 * @param path Path to the files to scan for
	 * @param regex The regular expression matching files to search for
	 * @param type The type of scan entry this object represents
	 */
	public ScanEntry(String path, String regex, ScanEntryType type) {
		this.path = path;
		this.regex = regex;
		this.type = type;
	}
	
	/**
	 * Getter for the path for this entry
	 * @return The path
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * Getter for the regular expression for this entry
	 * @return The regex
	 */
	public String getRegex() {
		return regex;
	}
	
	/**
	 * Getter for the type for this entry
	 * @return The type (normal, old semester, single file)
	 */
	public ScanEntryType getType() {
		return type;
	}
	
	/**
	 * Retrieve the files that this entry represents
	 * @return
	 */
	public Collection<String> getFiles() {
		
		// Holds the files found for returning
		ArrayList<String> files = new ArrayList<String>();
		
		// If this is a single file, add it and return
		if(type == ScanEntryType.SingleFile) {
			files.add(path);
		} else {
			
			// Holds folders that still need to be traversed
			ArrayDeque<File> folders = new ArrayDeque<File>();
			folders.add(new File(path));
			
			// Traverse folder structure for files and more folders
			do {
				File folder = folders.pop();
				for(File entry : folder.listFiles()) {
					if(entry.isDirectory()) {
						folders.add(entry);
					} else {
						if(Pattern.matches(regex, entry.getName())) {
							files.add(entry.getPath());
						}
					}
				}
			} while(!folders.isEmpty());
		}
		
		return files;
	}
}
