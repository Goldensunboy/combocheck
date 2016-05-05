package com.combocheck.ui.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import com.combocheck.algo.Algorithm;
import com.combocheck.global.FilePair;

/**
 * This class represents the report exporting code which creates annotated HTML
 * 
 * @author Andrew Wilder
 */
public class ReportExporter {

	/** Fields for the ReportExporter */
	private File dir;
	private List<ReportEntry> entries;
	
	/**
	 * Construct the new ReportExporter object
	 * @param dir The directory to export to
	 * @param entries The ReportEntry objects to export
	 */
	public ReportExporter(File dir, List<ReportEntry> entries) {
		this.dir = dir;
		this.entries = entries;
	}
	
	/**
	 * Normalizes 2 file names by creating formatted names based off the first
	 * directory in the path that differs.
	 * @param fname1 First file name
	 * @param fname2 Second file name
	 * @return An array with 2 indices representing the normalized names
	 */
	private String[] NormalizeNames(String fname1, String fname2) {
		String[] names = new String[2];
		Path path1 = Paths.get(fname1);
		Path path2 = Paths.get(fname2);
		String name1 = path1.iterator().next().toString();
		String name2 = path2.iterator().next().toString();
		names[0] = "";
		names[1] = "";
		for(int i = 0; i < name1.length(); ++i) {
			String c = name1.charAt(i) + "";
			if(Pattern.matches("\\p{Alnum}", c)) {
				names[0] += c.toLowerCase();
			} else if(" ".equals(c)) {
				names[0] += "_";
			}
		}
		for(int i = 0; i < name2.length(); ++i) {
			String c = name2.charAt(i) + "";
			if(Pattern.matches("\\p{Alnum}", c)) {
				names[1] += c.toLowerCase();
			} else if(" ".equals(c)) {
				names[1] += "_";
			}
		}
		return names;
	}
	
	/**
	 * Export the entries to the directory given upon construction
	 */
	public void exportEntries() {
		
		// Create the top-level directory for the export files
		File top;
		if(dir.exists()) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			top = new File(dir, "Combocheck Report " + df.format(new Date()));
		} else {
			top = dir;
		}
		top.mkdir();
		
		// For each entry...
		for(ReportEntry entry : entries) {
			FilePair fp = entry.getFilePair();
			
			// Create folder for contents
			String[] names = NormalizeNames(fp.getShortenedFile1(),
					fp.getShortenedFile2());
			File entryDir = new File(top, names[0] + "_" + names[1]);
			entryDir.mkdir();
			
			// Copy files to the folder
			Path dest1 = Paths.get(entryDir.getAbsolutePath(),
					names[0] + "_" + new File(fp.getFile1()).getName());
			Path dest2 = Paths.get(entryDir.getAbsolutePath(),
					names[1] + "_" + new File(fp.getFile2()).getName());
			try {
				Files.copy(Paths.get(fp.getFile1()), dest1);
				Files.copy(Paths.get(fp.getFile2()), dest2);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			// Write the annotation to a file
			Path outfilePath = Paths.get(entryDir.getAbsolutePath(),
					"Comments.txt");
			File outfile = new File(outfilePath.toString());
			try {
				PrintWriter pw = new PrintWriter(outfile);
				int len = Math.max(fp.getShortenedFile1().length(),
						fp.getShortenedFile2().length());
				String line = String.format("%-" + len + "s   Score:  %s\n",
						fp.getShortenedFile1(), "" + entry.getScore1());
				pw.write(line);
				line = String.format("%-" + len + "s   Score:  %s\n\n",
						fp.getShortenedFile2(), "" + entry.getScore2());
				pw.write(line);
				pw.write(entry.getAnnotation() + "\n");
				pw.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			// Generate the HTML file
			InputStream is = getClass().getResourceAsStream(
					"/res/template.html");
			Scanner sc = new Scanner(is, "UTF-8");
			sc.useDelimiter("\\A");
			String templateText = sc.next();
			sc.close();
			
			// Perform basic replacements first
			templateText = templateText.replaceAll(
					"\\$TITLE", "Combocheck report");
			String annotation = StringEscapeUtils.escapeHtml4(
					entry.getAnnotation());
			annotation = annotation.replaceAll("\n", "<br>");
			templateText = templateText.replaceAll("\\$COMMENTS", annotation);
			templateText = templateText.replaceAll(
					"\\$FILE1", fp.getShortenedFile1());
			templateText = templateText.replaceAll(
					"\\$FILE2", fp.getShortenedFile2());
			templateText = templateText.replaceAll(
					"\\$SCORE1", "" + entry.getScore1());
			templateText = templateText.replaceAll(
					"\\$SCORE2", "" + entry.getScore2());
			
			// Generate and replace line information
			List<String> lines1 = new ArrayList<String>();
			List<String> lines2 = new ArrayList<String>();
			Algorithm.ComputeLineMatches(fp.getFile1(), fp.getFile2(),
					lines1, lines2);
			int digits1 = (int) Math.log10(lines1.size()) + 1;
			int digits2 = (int) Math.log10(lines2.size()) + 1;
			int lineNum1 = 0, lineNum2 = 0;
			String htmlLines1 = "";
			String htmlLines2 = "";
			int lineNum = Math.min(lines1.size(), lines2.size());
			for(int i = 0; i < lineNum; ++i) {
				String line1 = lines1.get(i);
				String line2 = lines2.get(i);
				if(line1 != null) {
					String stripped2 = line2 == null ? null :
							line2.replaceAll(" |\t", "");
					boolean match = line1.replaceAll(" |\t", "")
							.equals(stripped2);
					String formatted = String.format(
							"%0" + digits1 + "d: ", lineNum1++) + line1;
					formatted = StringEscapeUtils.escapeHtml4(formatted);
					htmlLines1 += "<p class=\"" + (match ? "match" : "unique") +
							"\">" + formatted + "</p>\n";
				} else {
					htmlLines1 += "<p class=\"empty\"><br></p>\n";
				}
				if(line2 != null) {
					String stripped1 = line1 == null ? null :
							line1.replaceAll(" |\t", "");
					boolean match = line2.replaceAll(" |\t", "")
						.equals(stripped1);
					String formatted = String.format(
							"%0" + digits2 + "d: ", lineNum2++) + line2;
					formatted = StringEscapeUtils.escapeHtml4(formatted);
					htmlLines2 += "<p class=\"" + (match ? "match" : "unique") +
							"\">" + formatted + "</p>\n";
				} else {
					htmlLines2 += "<p class=\"empty\"><br></p>\n";
				}
			}
			templateText = templateText.replaceAll("\\$LINES1", htmlLines1);
			templateText = templateText.replaceAll("\\$LINES2", htmlLines2);
			
			// Write HTML file
			String htmlName = "diff_" + names[0] + "_" + names[1] + ".html";
			Path htmlPath = Paths.get(entryDir.getAbsolutePath(), htmlName);
			try {
				PrintWriter pw = new PrintWriter(htmlPath.toFile());
				pw.write(templateText);
				pw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
