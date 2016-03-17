package com.combocheck.lang.c;

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

import com.combocheck.lang.NormalizationListener;
import com.combocheck.lang.TokenizationErrorListener;

/**
 * This class represents a source file normalizer for the C language.
 * 
 * @author Andrew Wilder
 */
public class CNormalizer extends CBaseListener implements
		NormalizationListener {

	/** The list of errors encountered during parse tree generation */
	private List<String> errorList = new ArrayList<String>();
	private ANTLRErrorListener errorListener;
	private Collection<Token> replaceTokens = new ArrayList<Token>();
	
	/**
	 * Construct the normalizer and its attached ANTLRErrorListener
	 */
	public CNormalizer() {
		errorListener = new TokenizationErrorListener(this);
	}
	
	/**
	 * This function will take in a file to be walked by ANTLR, remove the
	 * whitespace, and convert all identifiers to a fixed name
	 * 
	 * @param filename The file to be normalized
	 * @return The contents as a String, or null if it couldn't be parsed
	 */
	@Override
	public String CreateNormalizedFile(String filename) {
		
		// Open the file
		ANTLRFileStream input;
		try {
			input = new ANTLRFileStream(filename);
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
		
		// Parse the file, renaming identifier tokens
		Lexer lexer = new CLexer(input);
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);
		BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
		CParser parser = new CParser(tokenStream);
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
		errorList.clear();
		replaceTokens.clear();
		ParserRuleContext tree = parser.compilationUnit();
		
		// TODO perform preprocessing on C. C.g4 doesn't preprocess the file
//		if(errorList.size() > 0) {
//			System.err.println("Error encountered while processing" + filename
//					+ ": " + errorList.get(0));
//			return null;
//		}
		
		// Find variable uses to normalize their names
		new ParseTreeWalker().walk(this, tree);
		
		// Convert the token stream into a string, normalize variables
		String fileText = "";
		for(Token t : tokenStream.getTokens()) {
			if(t.getType() == CParser.Identifier && replaceTokens.contains(t)) {
				fileText += NormalizationListener.NORMALIZED_IDENTIFIER;
			} else if(t.getType() != CParser.EOF) {
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
	public void enterPrimaryExpression(CParser.PrimaryExpressionContext ctx) {
		NormalizeIdentifiers(ctx);
	}
	
	@Override
	public void enterPostfixExpression(CParser.PostfixExpressionContext ctx) {
		NormalizeIdentifiers(ctx);
	}
	
	@Override
	public void enterUnaryExpression(CParser.UnaryExpressionContext ctx) {
		NormalizeIdentifiers(ctx);
	}
	
//	@Override
//	public void enterStructOrUnionSpecifier(CParser.StructOrUnionSpecifierContext ctx) {
//		NormalizeIdentifiers(ctx);
//	}
//	
//	@Override
//	public void enterEnumSpecifier(CParser.EnumSpecifierContext ctx) {
//		NormalizeIdentifiers(ctx);
//	}
//	
//	@Override
//	public void enterEnumerationConstant(CParser.EnumerationConstantContext ctx) {
//		NormalizeIdentifiers(ctx);
//	}
//	
//	@Override
//	public void enterFunctionSpecifier(CParser.FunctionSpecifierContext ctx) {
//		NormalizeIdentifiers(ctx);
//	}
	
	@Override
	public void enterDirectDeclarator(CParser.DirectDeclaratorContext ctx) {
		NormalizeIdentifiers(ctx);
	}
	
//	@Override
//	public void enterIdentifierList(CParser.IdentifierListContext ctx) {
//		NormalizeIdentifiers(ctx);
//	}
//	
//	@Override
//	public void enterTypedefName(CParser.TypedefNameContext ctx) {
//		NormalizeIdentifiers(ctx);
//	}
//	
//	@Override
//	public void enterDesignator(CParser.DesignatorContext ctx) {
//		NormalizeIdentifiers(ctx);
//	}
//	
//	@Override
//	public void enterLabeledStatement(CParser.LabeledStatementContext ctx) {
//		NormalizeIdentifiers(ctx);
//	}
//	
//	@Override
//	public void enterJumpStatement(CParser.JumpStatementContext ctx) {
//		NormalizeIdentifiers(ctx);
//	}
}
