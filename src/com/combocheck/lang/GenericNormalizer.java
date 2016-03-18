package com.combocheck.lang;

/**
 * This interface represents the generic base listener for all ANTLR grammars
 * that provides functions on a per-language basis to LanguageUtils
 * 
 * @author Andrew Wilder
 */
public interface GenericNormalizer {
	
	/** The name to convert variables to */
	public static final String NORMALIZED_IDENTIFIER = "VAR";

	/**
	 * This function will take in a file to be walked by ANTLR, remove the
	 * whitespace, and convert all variables to a fixed name
	 * 
	 * @param filename The file to be normalized
	 * @return The contents as a String, or null if it couldn't be parsed
	 */
	public abstract String CreateNormalizedFile(String filename);
	
	/**
	 * Used by the TokenizationErrorListener to report syntax errors when a
	 * parse tree is generated
	 * @param error The message about what caused the error
	 */
	public void addErrorMessage(String error);
}
