package com.combocheck.algo;

import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

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
	
	/** Enumeration of the normaliztion types */
	public static enum NormalizerType {
		NONE            ("None"           ),
		WHITESPACE_ONLY ("Whitespace only"),
		VARIABLES       ("Variables"      );
		private String desc;
		private NormalizerType(String desc) {
			this.desc = desc;
		}
		public String toString() {
			return desc;
		}
	};
	
	/**
	 * This function will remove all whitespace from a file and convert all
	 * identifiers to a single character.
	 * 
	 * @param filename The file to normalize
	 * @return A string representation of the file
	 */
	public static String GetNormalizedFile(String filename,
			NormalizerType ntype) {
		
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
			ret = normalizer.CreateNormalizedFile(filename, ntype);
		}
		if(ret == null) {
			ret = new TextNormalizer().CreateNormalizedFile(filename, ntype);
		}
		
		return ret != null ? ret : "";
	}
	
	/**
	 * This function will return an ANTLR AST generated for a source code file
	 * 
	 * @param filename The file to generate an AST for
	 * @return The AST
	 */
	public static ParseTree GetAST(String filename) {
		
		// Determine type of file
		int i = filename.lastIndexOf('.');
		if(i > 0) {
			String ext = filename.substring(i + 1);
			switch(ext) {
			case "c":
				return new CNormalizer().CreateAST(filename);
			case "java":
				return new JavaNormalizer().CreateAST(filename);
			}
		}
		
		// No recognized extension
		return null;
	}
	
	/**
	 * This function will return an array of token ID values for a file
	 * @param filename The file to generate tokens for
	 * @return The tokens
	 */
	public static int[] GetTokenIDs(String filename) {
		List<Token> tokens;
		
		// Determine type of file
		int i = filename.lastIndexOf('.');
		if(i > 0) {
			String ext = filename.substring(i + 1);
			switch(ext) {
			case "c":
				tokens = new CNormalizer().GetTokens(filename);
				break;
			case "java":
				tokens = new JavaNormalizer().GetTokens(filename);
				break;
			default:
				return null;
			}
		} else {
			// No recognized extension
			return null;
		}
		
		// Create array
		int[] tokenArr = new int[tokens.size()];
		for(i = 0; i < tokens.size(); ++i) {
			tokenArr[i] = tokens.get(i).getType();
		}
		return tokenArr;
	}
}
