package com.combocheck.algo;

import com.combocheck.lang.GenericNormalizer;
import com.combocheck.lang.TextNormalizer;
import com.combocheck.lang.c.CNormalizer;
import com.combocheck.lang.java.JavaNormalizer;

/**
 * This class contains static functions for analyzing properties of files
 * that pertain to their own programming language
 * 
 * @author Andrew Wilder
 */
public final class LanguageUtils {

	/**
	 * This function will remove all whitespace from a file and convert all
	 * identifiers to a single character.
	 * 
	 * @param filename The file to normalize
	 * @return A string representation of the file
	 */
	public static String GetNormalizedFile(String filename) {
		
		// Determine the type of this file
		GenericNormalizer normalizer = null;
		int i = filename.lastIndexOf('.');
		if (i > 0) {
		    String ext = filename.substring(i + 1);
		    switch(ext) {
			case "c":
				normalizer = new CNormalizer();
				break;
			case "java":
				normalizer = new JavaNormalizer();
				break;
			}
		}
		
		// Fall back on text normalizer on compilation error
		String ret = null;
		if(normalizer != null) {
			ret = normalizer.CreateNormalizedFile(filename);
		}
		if(ret == null) {
			ret = new TextNormalizer().CreateNormalizedFile(filename);
		}
		
		return ret != null ? ret : "";
	}
}