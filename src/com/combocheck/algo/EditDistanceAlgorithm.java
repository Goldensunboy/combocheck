package com.combocheck.algo;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
	private static NormalizerType Normalization = NormalizerType.VARIABLES;
	
	/**
	 * Construct the default instance of EditDistanceAlgorithm
	 */
	public EditDistanceAlgorithm(boolean enabled) {
		super(enabled);
		
		// Construct the settings dialog
		settingsPanel = new JPanel();
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
		JPanel npanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		npanel.add(new JLabel("Normalization:"));
		NormalizerType[] ntypes = {
			NormalizerType.NONE,
			NormalizerType.WHITESPACE_ONLY,
			NormalizerType.VARIABLES
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
		if(JNIFunctions.JNIEnabled()) {
			distanceArray = JNIFunctions.JNIEditDistance();
		} else {
			distanceArray = new int[Combocheck.FilePairs.size()];
			
			// Preprocess the files
			completed = 0;
			progress = 0;
			updateCurrentCheckName("Edit distance preprocessing");
			
			String[] normalizedFiles = new String[Combocheck.FileList.size()];
			Thread[] threadPool = new Thread[Combocheck.ThreadCount];
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new EDPreprocessingThread(normalizedFiles, i);
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
			++checksCompleted;
			
			// Compare normalized files
			completed = 0;
			progress = 0;
			updateCurrentCheckName("Edit distance comparisons");
			
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new EDComparisonThread(distanceArray,
						normalizedFiles, i);
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
			++checksCompleted;
		}
		
		// Construct the pair scores mapping
		pairScores = new HashMap<FilePair, Integer>();
		for(int i = 0; i < distanceArray.length; ++i) {
			pairScores.put(Combocheck.PairOrdering.get(i), distanceArray[i]);
		}
	}
	
	/**
	 * This class represents the runnable thread implementation of preprocessing
	 * for the edit distance algorithm.
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class EDPreprocessingThread extends Thread {
		
		/** Locals specific to this thread object */
		private String[] normalizedFiles;
		private int initialIndex;
		
		/**
		 * Construct a new edit distance preprocessing thread
		 * @param normalizedFiles Array of normalized file strings
		 * @param initialIndex Start index for striped processing
		 */
		public EDPreprocessingThread(String[] normalizedFiles,
				int initialIndex) {
			this.normalizedFiles = normalizedFiles;
			this.initialIndex = initialIndex;
		}
		
		/**
		 * Perform edit distance on file pairs
		 */
		@Override
		public void run() {
			int fileCount = Combocheck.FileList.size();
			for(int index = initialIndex; index < fileCount;
					index += Combocheck.ThreadCount) {
				
				// Normalize the file
				String filename = Combocheck.FileList.get(index);
				normalizedFiles[index] = LanguageUtils.GetNormalizedFile(
						filename, Normalization);
				
				// Update progress
				try {
					progressMutex.acquire();
					progress = 100 * ++completed / fileCount;
					progressMutex.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
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
	private static class EDComparisonThread extends Thread {
		
		/** Locals specific to this thread object */
		private int[] distanceArray;
		private int initialIndex;
		private String[] normalizedFiles;
		
		/**
		 * Construct a new edit distance calculation thread
		 * @param distanceArray Results from edit distance
		 * @param normalizedFiles The normalized file string array
		 * @param initialIndex Start index for striped processing
		 */
		public EDComparisonThread(int[] distanceArray, String[] normalizedFiles,
				int initialIndex) {
			this.distanceArray = distanceArray;
			this.initialIndex = initialIndex;
			this.normalizedFiles = normalizedFiles;
		}
		
		/**
		 * Perform edit distance on file pairs
		 */
		@Override
		public void run() {
			int pairCount = Combocheck.FilePairs.size();
			for(int index = initialIndex; index < pairCount;
					index += Combocheck.ThreadCount) {
				
				// Get the normalized file strings
				int idx1 = Combocheck.FilePairInts[index << 1];
				int idx2 = Combocheck.FilePairInts[(index << 1) + 1];
				String file1 = normalizedFiles[idx1];
				String file2 = normalizedFiles[idx2];
				
				// Calculate edit distance
				distanceArray[index] = Algorithm.EditDistance(file1, file2);
				
				// Update progress
				try {
					progressMutex.acquire();
					progress = 100 * ++completed / pairCount;
					progressMutex.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
