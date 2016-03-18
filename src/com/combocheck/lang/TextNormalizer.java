package com.combocheck.lang;

import java.io.FileReader;
import java.io.InputStreamReader;

/**
 * This class represents the plaintext version of the file normalizer, for use
 * when a file type not supported by Combocheck is supplied to LanguageUtils.
 * 
 * @author Andrew Wilder
 */
public class TextNormalizer implements GenericNormalizer {

	/**
	 * Remove whitespace, convert characters to lowercase
	 */
	@Override
	public String CreateNormalizedFile(String filename) {
		
		// Instance of StringBuilder used to construct our modified file text
		StringBuilder sb = new StringBuilder();
		try {
			InputStreamReader in = new FileReader(filename);
			char c = (char) in.read();
			while(c != 0xFFFF) {
				switch(c) {
				case ' ':
				case '\n':
				case '\r':
				case '\t':
					break;
				default:
					if(c >= 'A' && c <= 'Z') {
						c += 'a' - 'A';
					}
					sb.append(c);
				}
				c = (char) in.read();
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return sb.toString();
	}

	/**
	 * Unused
	 */
	@Override
	public void addErrorMessage(String error) {
		// TODO Auto-generated method stub
	}
}
