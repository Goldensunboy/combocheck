package com.combocheck.global;

/**
 * This class represents a file pair which is run through Combocheck
 * 
 * @author Andrew Wilder
 */
public class FilePair {

	/** The files that this object represents */
	private String file1 = null;
	private String file2 = null;
	
	/**
	 * Construct a new file pair
	 * The 2 files are ordered lexicographically
	 * @param file1
	 * @param file2
	 */
	public FilePair(String file1, String file2) {
		if(file1.compareTo(file2) < 0) {
			this.file1 = file1;
			this.file2 = file2;
		} else {
			this.file1 = file2;
			this.file2 = file1;
		}
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
