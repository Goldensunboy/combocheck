package com.combocheck.global;

import java.awt.GridLayout;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This class represents the tool for automatically fetching the current version
 * of combocheck, and verifying that it is up-to-date.
 * 
 * @author Andrew Wilder
 */
public final class VersionChecker {

	/** URL for version checking */
	private static final String CHANGELOG_STRING = "https://github.gatech.edu/raw/awilder6/combocheck/master/CHANGELOG?token=AAAQFR8Y75jcIqTLOF1qcjzUlC6BnAM5ks5XJwO3wA%3D%3D";
	private static URL CHANGELOG_URL = null;
	static {
		try {
			CHANGELOG_URL = new URL(CHANGELOG_STRING);
		} catch(MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This function will check the version of combocheck against that hosted at
	 * the URL, and if combocheck is out-of-date, alert the user with a
	 * changelog.
	 */
	public static void DoVersionCheck() {
		new CheckVersionThread().start();
	}
	
	/**
	 * This thread will perform the networking task in a separate thread from
	 * the main UI so that any network-related stalls or errors are not visible
	 * to the user.
	 * 
	 * @author Andrew Wilder
	 */
	private static class CheckVersionThread extends Thread {
		
		/**
		 * Check version, alert user if combocheck is out-of-date
		 */
		@Override
		public void run() {
			try {
				// Get the changelog file
				Scanner sc = new Scanner(CHANGELOG_URL.openStream());
				List<Version> versions = new ArrayList<Version>();
				
				// Parse versions from the changelog
				// There must be at least one change per version the way this is
				// written to parse the changelog
				String line = sc.nextLine();
				if(!Pattern.matches("\\d+\\.\\d+\\.\\d+", line)) {
					// Got a response that was not the changelog
					System.out.println(line);
					while(sc.hasNext()) {
						System.out.println(sc.nextLine());
					}
					System.err.println("Unable to retrieve changelog.");
					sc.close();
					return;
				}
				while(sc.hasNext()) {
					
					// Get version
					String versionString = line;
					List<String> changes = new ArrayList<String>();
					line = sc.nextLine();
					
					// Get changes for the version
					changes.add(line);
					while(sc.hasNext() && !Pattern.matches("\\d+\\.\\d+\\.\\d+",
							line)) {
						line = sc.nextLine();
						if(!"".equals(line)) {
							changes.add(line);
						}
					}

					versions.add(new Version(versionString, changes));
				}
				sc.close();
				
				// Compare current version with that of changelog
				Version currentVersion = new Version(Combocheck.VERSION, null);
				Version toDateVersion = versions.get(0);
				if(!currentVersion.equals(toDateVersion)) {
					
					// Get how far behind combocheck is
					int behind = toDateVersion.compareTo(currentVersion);
					String behindType =
							toDateVersion.major == currentVersion.major ?
									"major" :
							toDateVersion.minor == currentVersion.minor ?
									"minor" : "release";
					String behindMsg = behind + " " + behindType;
					System.out.println("This version of combocheck is behind by " + behindMsg + " versions");
					
					// Get the versions ahead of this one
					List<Version> versionLog = new ArrayList<Version>();
					int index = 0;
					do {
						versionLog.add(versions.get(index++));
					} while(!currentVersion.equals(versions.get(index)));
					
					// Diaplay the changelog frame
					ChangelogFrame cf =
							new ChangelogFrame(behindMsg, versionLog);
					cf.setVisible(true);
				}
			} catch(UnknownHostException e) {
				System.err.println("Unable to fetch changelog (no internet)");
			} catch(FileNotFoundException e) {
				System.err.println("Changelog not found at the usual URL: " +
						CHANGELOG_STRING);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This class represents the window which appears to notify the user that
	 * combocheck is out-of-date.
	 * 
	 * @author Andrew Wilder
	 */
	private static class ChangelogFrame extends JFrame {
		
		/**
		 * Construct and display the changelog frame
		 * @param behindMsg How many versions behind and which version type
		 * @param versions The versions ahead of the current one
		 */
		public ChangelogFrame(String behindMsg, List<Version> versions) {
			super("New version available!");
			setLayout(new GridLayout(1, 1));
			
			// Configure main content panel and message
			JPanel contentPanel = new JPanel();
			contentPanel.setLayout(new BoxLayout(
					contentPanel, BoxLayout.Y_AXIS));
			JLabel msgLabel = new JLabel(
					"This version of combocheck is behind by " + behindMsg +
					" version(s).\nChangelog:");
			contentPanel.add(msgLabel);
			
			// TODO configure the changelog display
			
			// Add contents and size the frame appropriately
			add(contentPanel);
			pack();
		}
	}
	
	/**
	 * This inner class allows for simpler packaging of releases with changelog
	 * comments.
	 * 
	 * @author Andrew Wilder
	 */
	private static class Version implements Comparable<Version> {
		
		/** Fields that define a Version */
		public int major, minor, release;
		public List<String> changes;
		
		/**
		 * Construct a Version object from 
		 * @param version
		 * @param changes
		 */
		public Version(String version, List<String> changes) {
			this.changes = changes;
			String[] parts = version.split("\\.");
			major = Integer.parseInt(parts[0]);
			minor = Integer.parseInt(parts[1]);
			release = Integer.parseInt(parts[2]);
		}
		
		/**
		 * Check if two versions are equal
		 */
		@Override
		public boolean equals(Object o) {
			if(o instanceof Version) {
				Version v = (Version) o;
				return v.major == major &&
						v.minor == minor &&
						v.release == release;
			} else {
				return false;
			}
		}

		/**
		 * Compare a version to another one
		 * @param v The other version
		 * @return
		 */
		@Override
		public int compareTo(Version v) {
			if(major != v.major) {
				return major - v.major;
			} else if(minor != v.minor) {
				return minor - v.minor;
			} else {
				return release - v.release;
			}
		}
	}
}
