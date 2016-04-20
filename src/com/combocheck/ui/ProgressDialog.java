package com.combocheck.ui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import com.combocheck.algo.Algorithm;
import com.combocheck.algo.JNIFunctions;
import com.combocheck.algo.LanguageUtils;
import com.combocheck.global.Combocheck;

/**
 * This class represents the dialog which houses the progress bars for scanning
 * 
 * @author Andrew Wilder
 */
@SuppressWarnings("serial")
public class ProgressDialog extends JDialog implements ActionListener {

	/** Properties for the progress dialog */
	private static final int REFRESH_RATE_FPS = 15;
	private Timer tm;
	private List<JProgressBar> bars = new ArrayList<JProgressBar>();
	private JLabel currText = new JLabel(" ");
	private int prevProgress = Integer.MAX_VALUE; // 0 - 100% progress
	private int progress_idx = 0; // index of current bar to update
	
	/**
	 * Construct the progress dialog
	 */
	public ProgressDialog() {
		super(Combocheck.Frame, true);
		
		// Set dialog properties
		setTitle("Scan progress");
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				Combocheck.CancelScan(bars.size());
				tm.stop();
				dispose();
			}
		});
		
		// Set up progress bars and UI
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		JPanel currTextPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		currTextPanel.add(currText);
		contentPanel.add(currTextPanel);
		for(Algorithm a : Combocheck.algorithms) {
			if(a.isEnabled()) {
				for(int i = 0; i < 2; ++i) {
					JProgressBar newBar = new JProgressBar();
					Dimension newSize = newBar.getPreferredSize();
					newSize.width = 300;
					newBar.setPreferredSize(newSize);
					newBar.setStringPainted(true);
					bars.add(newBar);
					JPanel barPanel = new JPanel(
							new FlowLayout(FlowLayout.LEADING));
					barPanel.add(newBar);
					contentPanel.add(barPanel);
				}
			}
		}
		
		// Add a cancel button
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Combocheck.CancelScan(bars.size());
				tm.stop();
				dispose();
			}
		});
		JPanel cancelPanel = new JPanel();
		cancelPanel.add(cancelButton);
		contentPanel.add(cancelPanel);
		
		// Position and size the dialog
		add(contentPanel);
		pack();
		setLocationRelativeTo(Combocheck.Frame);
		
		// Establish update timer
		tm = new Timer(1000 / REFRESH_RATE_FPS, this);
		tm.start();
	}

	/**
	 * Update the progress bars
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
		// Get the current progress and name of algorithm stage being done
		int currProgress;
		String msg;
		int checksCompleted;
		if(JNIFunctions.JNIEnabled()) {
			currProgress = JNIFunctions.PollJNIProgress();
			msg = JNIFunctions.GetJNICurrentCheck();
			checksCompleted = JNIFunctions.JNIPollChecksCompleted();
		} else {
			try {
				Algorithm.progressMutex.acquire();
				currProgress = Algorithm.PollProgress();
				Algorithm.progressMutex.release();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return;
			}
			msg = Algorithm.GetCurrentCheck();
			checksCompleted = Algorithm.getChecksCompleted();
		}
		
		// Don't do anything if progress hasn't changed
		if(currProgress != prevProgress) {
			currText.setText(msg);
			if(checksCompleted > progress_idx) {
				// Start updating the next progress bar
				for(int i = progress_idx; i < checksCompleted; ++i) {
					bars.get(i).setValue(100);
					bars.get(i).setString("100%");
				}
				progress_idx = checksCompleted;
			}
			if(checksCompleted == bars.size()) {
				// Stop the timer, close the dialog, switch to review tab
				tm.stop();
				dispose();
				CombocheckTabbedPane ctb = Combocheck.Frame.getTabbedPane();
				while(Algorithm.isProcessing()) {
					Thread.yield();
				}
				ctb.getReviewPanel().populatePanel();
				ctb.switchToReviewPanel();
				LanguageUtils.ClearParseTreeCache();
				return;
			}
			prevProgress = currProgress;
			JProgressBar jpb = bars.get(progress_idx);
			jpb.setValue(currProgress);
			jpb.setString(currProgress + "%");
			revalidate();
		}
	}
}
