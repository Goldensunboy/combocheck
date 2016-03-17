package com.combocheck.lang;

import java.util.BitSet;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

/**
 * This class's purpose is to throw an exception for a NormalizationListener to
 * catch in case a file being parsed has a syntax error.
 * 
 * @author Andrew Wilder
 */
public class TokenizationErrorListener implements ANTLRErrorListener {

	/** The instance of NormalizationListener to set errors for */
	private NormalizationListener listener;
	
	/**
	 * Construct the error listener with a link to the attached Normalization
	 * Listener to which error messages should be added
	 * @param listener
	 */
	public TokenizationErrorListener(NormalizationListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Throw an exception on syntax error
	 */
	@Override
	public void syntaxError(Recognizer<?, ?> arg0, Object arg1, int arg2,
			int arg3, String arg4, RecognitionException arg5) {
		listener.addErrorMessage(arg4);
	}
	
	@Override
	public void reportAmbiguity(Parser arg0, DFA arg1, int arg2, int arg3,
			boolean arg4, BitSet arg5, ATNConfigSet arg6) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportAttemptingFullContext(Parser arg0, DFA arg1, int arg2,
			int arg3, BitSet arg4, ATNConfigSet arg5) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportContextSensitivity(Parser arg0, DFA arg1, int arg2,
			int arg3, int arg4, ATNConfigSet arg5) {
		// TODO Auto-generated method stub

	}
}
