package com.combocheck.lang.c;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.CharStream;

import com.combocheck.algo.LanguageUtils.NormalizerType;
import com.combocheck.lang.GenericNormalizer;
import com.combocheck.lang.TextNormalizer;
import com.combocheck.lang.TokenizationErrorListener;

/**
 * This class represents a source file normalizer for the C language.
 * 
 * @author Andrew Wilder
 */
public class CNormalizer extends CBaseListener implements
		GenericNormalizer {

	/** The list of errors encountered during parse tree generation */
	private List<String> errorList = new ArrayList<String>();
	private ANTLRErrorListener errorListener;
	private Collection<Token> replaceTokens = new ArrayList<Token>();
	
	/** Static data related to processing files once */
	private static Map<String, ParseTree> pmap =
			new HashMap<String, ParseTree>();
	private static Map<String, BufferedTokenStream> tmap =
			new HashMap<String, BufferedTokenStream>();
	private static Set<String> errorSet = new HashSet<String>();
	
	/**
	 * Construct the normalizer and its attached ANTLRErrorListener
	 */
	public CNormalizer() {
		errorListener = new TokenizationErrorListener(this);
	}
	
	/**
	 * Get a ParseTree from a filename. It is only done once per scan
	 * @param filename The file to create a ParseTree from
	 * @return The ParseTree, or null if there was a parsing error
	 */
	private ParseTree getParseTree(String filename) {
		if(errorSet.contains(filename)) {
			// If previous processing of file errored, return null
			return null;
		} else if(tmap.containsKey(filename)) {
			// If file has been processed before, return ParseTree
			return pmap.get(filename);
		} else {
			// Preprocess the file
			String pfile = PreprocessFile(filename);
			
			// Parse the file
			CharStream stream = new ANTLRInputStream(pfile);
			Lexer lexer = new CLexer(stream);
			lexer.removeErrorListeners();
			lexer.addErrorListener(errorListener);
			BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
			CParser parser = new CParser(tokenStream);
			parser.removeErrorListeners();
			parser.addErrorListener(errorListener);
			errorList.clear();
			ParserRuleContext tree = parser.compilationUnit();
			tmap.put(filename, tokenStream);
			if(errorList.size() > 0) {
				errorSet.add(filename);
				System.err.println(filename + ": " + errorList.get(0));
				return null;
			} else {
				pmap.put(filename, tree);
				return tree;
			}
		}
	}
	
	/**
	 * Clear the cached ParseTrees for all files
	 */
	public static void clearCachedParseTrees() {
		pmap.clear();
		tmap.clear();
		errorSet.clear();
	}
	
	/**
	 * Open and preprocess a file
	 * This function doesn't do any real preprocessing, rather it just removes
	 * all preprocessor directive lines from the source file
	 * @param filename The file to preprocess
	 * @return The preprocessed file
	 */
	private static String PreprocessFile(String filename) {
		
		// Read the file
		String file;
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(filename));
			file = new String(encoded);
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
		
		// Concatenate lines ending in the continuation character
		file = file.replaceAll("\\\\\n", "");
		
		// Remove preprocessor directive lines, tracking the #defines
		// TODO make this better. It only replaces non-parameterized #defines
		String strippedFile = "";
		Scanner sc = new Scanner(file);
		Map<String, String> defines = new HashMap<String, String>();
		while(sc.hasNext()) {
			String s = sc.nextLine().trim();
			if(Pattern.matches("#.*", s)) {
				if(s.indexOf("#define") == 0) {
					String defText = s.substring(8).trim();
					if(!Pattern.matches("\\w+\\(.*", defText)) {
						String[] parts = defText.split("\\s+", 2);
						String symbol = parts[0], target = "";
						if(parts.length > 1) {
							target = parts[1];
						}
						defines.put(symbol, target);
					}
				}
			} else {
				strippedFile += s;
			}
			strippedFile += '\n';
		}
		sc.close();
		
		// Perform substitutions for #define
		for(String symbol : defines.keySet()) {
			strippedFile = strippedFile.replaceAll("\\Q" + symbol + "\\E",
					defines.get(symbol));
		}
		
		return strippedFile;
	}
	
	/**
	 * Create an AST from a file
	 * @param filename the file
	 * @return the AST
	 */
	public ParseTree CreateAST(String filename) {
		return getParseTree(filename);
	}
	
	/**
	 * This function will get the lexical tokens of a file as a list
	 * @param filename The file to get tokens from
	 * @return The token list
	 */
	public List<Token> GetTokens(String filename) {
		getParseTree(filename);
		return tmap.get(filename).getTokens();
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
		
		// Get the ParseTree if it exists
		ParseTree tree = getParseTree(filename);
		if(tree == null) {
			return new TextNormalizer().CreateNormalizedFile(filename, ntype);
		}
		BufferedTokenStream bts = tmap.get(filename);
		
		// Find variable uses to normalize their names
		new ParseTreeWalker().walk(this, tree);
		
		// Convert the token stream into a string, normalize variables
		String fileText = "";
		for(Token t : bts.getTokens()) {
			if(ntype == NormalizerType.VARIABLES && t.getType() ==
					CParser.Identifier && replaceTokens.contains(t)) {
				fileText += GenericNormalizer.NORMALIZED_IDENTIFIER;
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
