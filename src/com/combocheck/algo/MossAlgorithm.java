package com.combocheck.algo;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.combocheck.algo.Algorithm;
import com.combocheck.global.Combocheck;
import com.combocheck.global.FilePair;
import com.combocheck.algo.LanguageUtils.NormalizerType;

/**
 * This class represents the MOSS (Measure of Software Similarity) algorithm.
 * 
 * @author Andrew Wilder
 */
public class MossAlgorithm extends Algorithm {
	
	/** Moss parameters */
	private static int K = 15; // K-gram size
	private static int W = 8; // Winnowing window size
	private static NormalizerType Normalization = NormalizerType.VARIABLES;
	
	/**
	 * Construct the default instance of MossAlgorithm
	 */
	public MossAlgorithm(boolean enabled) {
		super(enabled);
		
		// Construct the settings dialog
		settingsPanel = new JPanel();
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
		JPanel kpanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		kpanel.add(new JLabel("K-gram size:"));
		JTextField ktf = new JTextField(4);
		ktf.setText("" + K);
		kpanel.add(ktf);
		settingsPanel.add(kpanel);
		JPanel wpanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		wpanel.add(new JLabel("Winnow size:"));
		JTextField wtf = new JTextField(4);
		wtf.setText("" + W);
		wpanel.add(wtf);
		settingsPanel.add(wpanel);
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
		okbutton.addActionListener(new MossOKListener(ktf, wtf, ncb));
		bpanel.add(okbutton);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new MossCancelListener(ktf, wtf, ncb));
		bpanel.add(cancelButton);
		settingsPanel.add(bpanel);
	}
	
	/**
	 * This class is used to set the algorithm's parameters from the setting
	 * dialog.
	 * 
	 * @author Andrew Wilder
	 */
	private class MossOKListener implements ActionListener {
		
		private JTextField ktf, wtf;
		private JComboBox<NormalizerType> ncb;
		
		/**
		 * Construct a new instance of the listener
		 * @param ktf Text field with K parameter
		 * @param wtf Text field with W parameter
		 * @param ncb Combobox for selected normalization type
		 */
		public MossOKListener(JTextField ktf, JTextField wtf,
				JComboBox<NormalizerType> ncb) {
			this.ktf = ktf;
			this.wtf = wtf;
			this.ncb = ncb;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			// Verify that the K and W numbers are valid
			if(!Pattern.matches("\\d+", ktf.getText())) {
				JOptionPane.showMessageDialog(Combocheck.Frame,
						"K is not a valid number", "Input error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(!Pattern.matches("\\d+", wtf.getText())) {
				JOptionPane.showMessageDialog(Combocheck.Frame,
						"W is not a valid number", "Input error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			// Set the algorithm parameters
			K = Integer.parseInt(ktf.getText());
			W = Integer.parseInt(wtf.getText());
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
	private class MossCancelListener implements ActionListener {
		
		private JTextField ktf, wtf;
		private JComboBox<NormalizerType> ncb;
		
		/**
		 * Construct a new instance of the listener
		 * @param ktf Text field with K parameter
		 * @param wtf Text field with W parameter
		 * @param ncb Combobox for selected normalization type
		 */
		public MossCancelListener(JTextField ktf, JTextField wtf,
				JComboBox<NormalizerType> ncb) {
			this.ktf = ktf;
			this.wtf = wtf;
			this.ncb = ncb;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			// Set the algorithm parameters
			ktf.setText("" + K);
			wtf.setText("" + W);
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
		return "Moss";
	}

	/**
	 * Analyze the file pairs using this algorithm
	 */
	@Override
	public void analyzeFiles() {
		int[] scoreArray;
		
		// Use the JNI implementation if it is available
		if(JNIFunctions.JNIEnabled()) {
			scoreArray = JNIFunctions.JNIMoss();
		} else {
			scoreArray = new int[Combocheck.FilePairs.size()];
			
			// Preprocess the files
			completed = 0;
			progress = 0;
			updateCurrentCheckName("Moss preprocessing");
			
			@SuppressWarnings("unchecked")
			List<Integer>[] fingerprints =
					new ArrayList[Combocheck.FileList.size()];
			Thread[] threadPool = new Thread[Combocheck.ThreadCount];
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new MossPreprocessingThread(fingerprints, i);
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
			
			// Compare fingerprints for all pairs
			completed = 0;
			progress = 0;
			updateCurrentCheckName("Moss fingerprint comparisons");
			
			for(int i = 0; i < Combocheck.ThreadCount; ++i) {
				threadPool[i] = new MossComparisonThread(scoreArray,
						fingerprints, i);
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
		for(int i = 0; i < scoreArray.length; ++i) {
			pairScores.put(Combocheck.PairOrdering.get(i), scoreArray[i]);
		}
	}
	
	/**
	 * This class represents the runnable thread implementation of preprocessing
	 * files for the Moss algorithm.
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class MossPreprocessingThread extends Thread {
		
		/** Locals specific to this thread object */
		private List<Integer>[] fingerprints;
		private int initialIndex;
		
		/**
		 * Construct a new moss preprocessing thread
		 * @param fingerprints Fingerprints for all files
		 * @param initialIndex Start index for striped processing
		 */
		public MossPreprocessingThread(List<Integer>[] fingerprints,
				int initialIndex) {
			this.fingerprints = fingerprints;
			this.initialIndex = initialIndex;
		}
		
		/**
		 * Perform Moss preprocessing on files
		 */
		@Override
		public void run() {
			int fileCount = Combocheck.FileList.size();
			for(int index = initialIndex; index < fileCount;
					index += Combocheck.ThreadCount) {
				String fileString = LanguageUtils.GetNormalizedFile(
						Combocheck.FileOrdering.get(index), Normalization);
				List<Integer> fingerprint = new ArrayList<Integer>();
				
				// If file size is less than K, fingerprint contains one val
				if(fileString.length() <= K) {
					fingerprint.add(fileString.hashCode());
				} else {
					
					// Create the k-gram hashes
					int[] kgrams = new int[fileString.length() - K + 1];
					for(int i = 0; i < kgrams.length; ++i) {
						kgrams[i] = fileString.substring(i, i + K).hashCode();
					}
					
					// Create fingerprint
					int smallest = kgrams[0];
					int smallestIdx = 0;
					for(int i = 1; i < W; ++i) {
						if(kgrams[i] < smallest) {
							smallest = kgrams[i];
							smallestIdx = i;
						}
					}
					fingerprint.add(smallest);
					int current = smallest;
					int currentIdx = smallestIdx;
					for(int i = 1; i < kgrams.length - W + 1; ++i) {
						smallest = kgrams[i];
						smallestIdx = i;
						for(int j = 1; j < W; ++j) {
							if(kgrams[i + j] < smallest) {
								smallest = kgrams[i + j];
								smallestIdx = i + j;
							}
						}
						if(current > smallest || currentIdx <= i - W) {
							fingerprint.add(smallest);
							current = smallest;
							currentIdx = smallestIdx;
						}
					}
				}
				
				// Add the fingerprint to the array being constructed
				Collections.sort(fingerprint);
				fingerprints[index] = fingerprint;
				
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
	 * This class represents the runnable thread implementation of comparing
	 * fingerprints for file pairs
	 * 
	 * The order in which the file pairs are processed is striped to prevent
	 * concurrency issues, or the need for mutexes.
	 * 
	 * @author Andrew Wilder
	 */
	private static class MossComparisonThread extends Thread {
		
		/** Locals specific to this thread object */
		private int[] scoreArray;
		private List<Integer>[] fingerprints;
		private int initialIndex;
		
		/**
		 * Construct a new edit distance calculation thread
		 * @param scoreArray The difference score for a pair's fingerprints
		 * @param pairDistance Results mapping of pairs onto edit distance
		 * @param initialIndex Start index for striped processing
		 */
		public MossComparisonThread(int[] scoreArray,
				List<Integer>[] fingerprints, int initialIndex) {
			this.scoreArray = scoreArray;
			this.fingerprints = fingerprints;
			this.initialIndex = initialIndex;
		}
		
		/**
		 * Perform comparison of fingerprints for all pairs
		 */
		@Override
		public void run() {
			int pairCount = Combocheck.FilePairs.size();
			for(int index = initialIndex; index < pairCount;
					index += Combocheck.ThreadCount) {
				
				// Get the data arrays
				int idx1 = Combocheck.FilePairInts[index << 1];
				int idx2 = Combocheck.FilePairInts[(index << 1) + 1];
				List<Integer> fp1 = fingerprints[idx1];
				List<Integer> fp2 = fingerprints[idx2];
				
				// Edit distance of the token array
				int[] a1 = new int[fp1.size()];
				for(int i = 0; i < fp1.size(); ++i) {
					a1[i] = fp1.get(i);
				}
				int[] a2 = new int[fp2.size()];
				for(int i = 0; i < fp2.size(); ++i) {
					a2[i] = fp2.get(i);
				}
				scoreArray[index] = Algorithm.EditDistance(a1, a2);
				
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
