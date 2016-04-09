package com.combocheck.algo;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.combocheck.algo.Algorithm;
import com.combocheck.algo.LanguageUtils.NormalizerType;
import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;

/**
 * This class represents the edit distance algorithm.
 * 
 * @author Andrew Wilder
 */
public class EditDistanceAlgorithm extends Algorithm {
	
	/** Normalizer option for edit distance */
	NormalizerType Normalization = NormalizerType.WHITESPACE_ONLY;
	
	/**
	 * Construct the default instance of EditDistanceAlgorithm
	 */
	public EditDistanceAlgorithm() {
		enabled = false;
		
		// Construct the settings dialog
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
		JPanel npanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		npanel.add(new JLabel("Normalization:"));
		NormalizerType[] ntypes = {
			NormalizerType.NONE,
			NormalizerType.WHITESPACE_ONLY
		};
		JComboBox<NormalizerType> ncb = new JComboBox<NormalizerType>(ntypes);
		ncb.setSelectedItem(Normalization);
		npanel.add(ncb);
		settingsPanel.add(npanel);
		JPanel bpanel = new JPanel();
		JButton okbutton = new JButton("OK");
		okbutton.addActionListener(new EditDistanceOKListener(ncb));
		bpanel.add(okbutton);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new EditDistanceCancelListener(ncb));
		bpanel.add(cancelButton);
		settingsPanel.add(bpanel);
	}
	
	/**
	 * This class is used to set the algorithm's parameters from the setting
	 * dialog.
	 * 
	 * @author Andrew Wilder
	 */
	private class EditDistanceOKListener implements ActionListener {
		
		private JComboBox<NormalizerType> ncb;
		
		/**
		 * Construct a new instance of the listener
		 * @param ncb Combobox for selected normalization type
		 */
		public EditDistanceOKListener(JComboBox<NormalizerType> ncb) {
			this.ncb = ncb;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			// Set the algorithm parameters
			Normalization = (NormalizerType) ncb.getSelectedItem();
			
			// Exit the dialog
			Container jc = settingsPanel;
			while(!(jc instanceof JDialog)) {
				jc = jc.getParent();
			}
			JDialog jd = (JDialog) jc;
			jd.dispose();
		}
	}
	
	/**
	 * This class is used to reset the dialog's fields when canceling the dialog
	 * 
	 * @author Andrew Wilder
	 */
	private class EditDistanceCancelListener implements ActionListener {
		
		private JComboBox<NormalizerType> ncb;
		
		/**
		 * Construct a new instance of the listener
		 * @param ncb Combobox for selected normalization type
		 */
		public EditDistanceCancelListener(JComboBox<NormalizerType> ncb) {
			this.ncb = ncb;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			// Set the algorithm parameters
			ncb.setSelectedItem(Normalization);
			
			// Exit the dialog
			Container jc = settingsPanel;
			while(!(jc instanceof JDialog)) {
				jc = jc.getParent();
			}
			JDialog jd = (JDialog) jc;
			jd.dispose();
		}
	}
	
	/**
	 * Get the String representation of this algorithm for JComponents
	 */
	@Override
	public String toString() {
		return "Edit Distance";
	}

	/**
	 * Analyze the file pairs using this algorithm
	 */
	@Override
	public void analyzeFiles() {
		int[] distanceArray;
		
		// Use the JNI implementation if it is available
		if(JNIFunctions.isAvailable()) {
			distanceArray = JNIFunctions.JNIEditDistance();
		} else {
			distanceArray = new int[Combocheck.FilePairs.size()];
			
			// Run the Java implementation in several threads
			Thread[] threadPool = new Thread[Combocheck.ThreadCount];
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new EditDistanceThread(distanceArray, i);
				threadPool[i].start();
			}
			try {
				for(int i = 0; i < Combocheck.ThreadCount; ++i) {
					threadPool[i].join();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
		
		// Construct the pair scores mapping
		fileScores = new HashMap<FilePair, Integer>();
		for(int i = 0; i < distanceArray.length; ++i) {
			fileScores.put(Combocheck.PairOrdering.get(i), distanceArray[i]);
		}
	}
	
	/**
	 * This class represents the runnable thread implementation of running the
	 * edit distance algorithm on several file pairs.
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class EditDistanceThread extends Thread {
		
		/** Locals specific to this thread object */
		private int[] distanceArray;
		private int initialIndex;
		
		/**
		 * Construct a new edit distance calculation thread
		 * @param pairDistance Results mapping of pairs onto edit distance
		 * @param initialIndex Start index for striped processing
		 */
		public EditDistanceThread(int[] distanceArray, int initialIndex) {
			this.distanceArray = distanceArray;
			this.initialIndex = initialIndex;
		}
		
		/**
		 * Perform edit distance on file pairs
		 */
		@Override
		public void run() {
			for(int index = initialIndex; index < Combocheck.FilePairs.size();
					index += Combocheck.ThreadCount) {
				
				// Read the data as a byte array from the 2 files
				FilePair pair = Combocheck.PairOrdering.get(index);
				byte[] file1, file2;
				try {
					file1 = Files.readAllBytes(new File(
							pair.getFile1()).toPath());
					file2 = Files.readAllBytes(new File(
							pair.getFile2()).toPath());
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
				
				// Calculate edit distance
				distanceArray[index] = Algorithm.EditDistance(file1, file2);
			}
		}
	}
}
