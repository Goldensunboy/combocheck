package com.combocheck.lang.java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import com.combocheck.algo.LanguageUtils.NormalizerType;
import com.combocheck.lang.GenericNormalizer;
import com.combocheck.lang.TextNormalizer;
import com.combocheck.lang.TokenizationErrorListener;

/**
 * This class represents a source file normalizer for the Java language.
 * Specifically, Java 8
 * 
 * @author Andrew Wilder
 */
public class JavaNormalizer extends JavaBaseListener implements
		GenericNormalizer {

	/** The list of errors encountered during parse tree generation */
	private List<String> errorList = new ArrayList<String>();
	private ANTLRErrorListener errorListener;
	private Collection<Token> replaceTokens = new ArrayList<Token>();
	
	/**
	 * Construct the normalizer and its attached ANTLRErrorListener
	 */
	public JavaNormalizer() {
		errorListener = new TokenizationErrorListener(this);
	}
	
	/**
	 * Create an AST from a file
	 * @param filename the file
	 * @return the AST
	 */
	public ParseTree CreateAST(String filename) {
		
		// Open the file
		ANTLRFileStream input;
		try {
			input = new ANTLRFileStream(filename);
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
		
		// Parse the file, renaming identifier tokens
		Lexer lexer = new JavaLexer(input);
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);
		BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
		JavaParser parser = new JavaParser(tokenStream);
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
		errorList.clear();
		replaceTokens.clear();
		ParserRuleContext tree = parser.compilationUnit();
		if(errorList.size() > 0) {
			System.err.println(filename + ": " + errorList.get(0));
			return null;
		}
		
		return tree;
	}
	
	/**
	 * This function will get the lexical tokens of a file as a list
	 * @param filename The file to get tokens from
	 * @return The token list
	 */
	public List<Token> GetTokens(String filename) {
		
		// Open the file
		ANTLRFileStream input;
		try {
			input = new ANTLRFileStream(filename);
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
		
		// Parse the file, renaming identifier tokens
		Lexer lexer = new JavaLexer(input);
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);
		BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
		JavaParser parser = new JavaParser(tokenStream);
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
		errorList.clear();
		parser.compilationUnit();
		if(errorList.size() > 0) {
			System.err.println(filename + ": " + errorList.get(0));
		}
		
		return tokenStream.getTokens();
	}
	
	/**
	 * This function will take in a file to be walked by ANTLR, remove the
	 * whitespace, and convert all identifiers to a fixed name
	 * 
	 * @param filename The file to be normalized
	 * @return The contents as a String, or null if it couldn't be parsed
	 */
	@Override
	public String CreateNormalizedFile(String filename, NormalizerType ntype) {
		
		// Open the file
		ANTLRFileStream input;
		try {
			input = new ANTLRFileStream(filename);
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
		
		// If the normalizer type doesn't include variables, use text normalizer
		if(ntype != NormalizerType.VARIABLES) {
			return new TextNormalizer().CreateNormalizedFile(filename, ntype);
		}
		
		// Parse the file, renaming identifier tokens
		Lexer lexer = new JavaLexer(input);
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);
		BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
		JavaParser parser = new JavaParser(tokenStream);
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
		errorList.clear();
		replaceTokens.clear();
		ParserRuleContext tree = parser.compilationUnit();
		if(errorList.size() > 0) {
			System.err.println(filename + ": " + errorList.get(0));
			return null;
		}
		
		// Find variable uses to normalize their names
		new ParseTreeWalker().walk(this, tree);
		
		// Convert the token stream into a string, normalize variables
		String fileText = "";
		for(Token t : tokenStream.getTokens()) {
			if(t.getType() == JavaParser.Identifier && replaceTokens.contains(t)) {
				fileText += GenericNormalizer.NORMALIZED_IDENTIFIER;
			} else if(t.getType() != JavaParser.EOF){
				fileText += t.getText();
			}
		}
		return fileText;
	}

	/**
	 * Used by the TokenizationErrorListener to report syntax errors when a
	 * parse tree is generated
	 * @param error The message about what caused the error
	 */
	@Override
	public void addErrorMessage(String error) {
		errorList.add(error);
	}
	
	/**
	 * Normalize all identifiers contained within this context rule
	 * This function is called by every rule in the grammar that contains an
	 * Identifier as one of its possible resolutions, and that use is as a
	 * variable name within expressions or declarations.
	 * @param ctx The rule context containing tokens which may be identifiers
	 */
	private void NormalizeIdentifiers(ParserRuleContext ctx) {
		for(int i = 0; i < ctx.getChildCount(); ++i) {
			ParseTree pt = ctx.getChild(i);
			Object payload = pt.getPayload();
			if(payload instanceof Token) {
				replaceTokens.add((Token) payload);
			}
		}
	}
	
	@Override
	public void enterConstantDeclarator(JavaParser.ConstantDeclaratorContext ctx) {
		NormalizeIdentifiers(ctx);
	}
	
	@Override
	public void enterVariableDeclaratorId(JavaParser.VariableDeclaratorIdContext ctx) {
		NormalizeIdentifiers(ctx);
	}
	
	@Override
	public void enterExpression(JavaParser.ExpressionContext ctx) {
		NormalizeIdentifiers(ctx);
	}
	
	@Override
	public void enterStatementExpression(JavaParser.StatementExpressionContext ctx) {
		NormalizeIdentifiers(ctx);
	}
	
	@Override
	public void enterConstantExpression(JavaParser.ConstantExpressionContext ctx) {
		NormalizeIdentifiers(ctx);
	}
	
	@Override
	public void enterPrimary(JavaParser.PrimaryContext ctx) {
		NormalizeIdentifiers(ctx);
	}
}
