package com.combocheck.global;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * This class represents a file pair which is run through Combocheck
 * 
 * @author Andrew Wilder
 */
public class FilePair implements Serializable {
	private static final long serialVersionUID = -2038579416703885903L;
	
	/** The files that this object represents */
	private String file1;
	private String file2;
	private String shortenedName1;
	private String shortenedName2;
	
	/**
	 * Construct a new file pair
	 * The 2 files are ordered lexicographically
	 * @param file1
	 * @param file2
	 */
	public FilePair(String file1, String file2) {
		
		// Order files lexicographically
		if(file1.compareTo(file2) < 0) {
			this.file1 = file1;
			this.file2 = file2;
		} else {
			this.file1 = file2;
			this.file2 = file1;
		}
		
		// Determine portion of file paths which differs
		Iterator<Path> itr1 = Paths.get(file1).iterator();
		Iterator<Path> itr2 = Paths.get(file2).iterator();
		Path lastPath1 = null;
		Path lastPath2 = null;
		while(itr1.hasNext() && itr2.hasNext()) {
			lastPath1 = itr1.next();
			lastPath2 = itr2.next();
			if(!lastPath1.equals(lastPath2)) {
				break;
			}
		}
		while(itr1.hasNext()) {
			lastPath1 = lastPath1.resolve(itr1.next());
		}
		while(itr2.hasNext()) {
			lastPath2 = lastPath2.resolve(itr2.next());
		}
		shortenedName1 = lastPath1.toString();
		shortenedName2 = lastPath2.toString();
	}
	
	/**
	 * Getter for file 1
	 * @return File 1
	 */
	public String getFile1() {
		return file1;
	}
	
	/**
	 * Getter for file 2
	 * @return File 2
	 */
	public String getFile2() {
		return file2;
	}
	
	/**
	 * Gets file 1, truncated to only that which is different in the filename
	 * @return
	 */
	public String getShortenedFile1() {
		return shortenedName1;
	}
	
	/**
	 * Gets file 2, truncated to only that which is different in the filename
	 * @return
	 */
	public String getShortenedFile2() {
		return shortenedName2;
	}
	
	/**
	 * Tell if two pairs are equal
	 * @param obj The other pair to compare against
	 * @return true if two pairs are equal
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FilePair) {
			FilePair pair = (FilePair) obj;
			/* Only check file1 against file1 and file2 against file2 due to
			 * lexicographical ordering of files in constructor
			 */
			return file1.equals(pair.file1) && file2.equals(pair.file2);
		} else {
			return false;
		}
	}
	
	/**
	 * Makes the file pair hashable
	 * @return XOR of the files' hashcodes
	 */
	@Override
	public int hashCode() {
		return file1.hashCode() ^ file2.hashCode();
	}
	
	/**
	 * Names of the files
	 */
	@Override
	public String toString() {
		return "{" + file1 + ", " + file2 + "}";
	}
}
