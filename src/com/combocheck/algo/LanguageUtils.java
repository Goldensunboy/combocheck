package com.combocheck.algo;

import com.combocheck.lang.NormalizationListener;
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
		NormalizationListener nl;
		int i = filename.lastIndexOf('.');
		if (i > 0) {
		    String ext = filename.substring(i + 1);
		    switch(ext) {
			case "c":
				nl = new CNormalizer();
				break;
			case "java":
				nl = new JavaNormalizer();
				break;
			default:
				nl = new TextNormalizer();
			}
		} else {
			// No extension
			nl = new TextNormalizer();
		}
		
		return nl.CreateNormalizedFile(filename);
	}
}
