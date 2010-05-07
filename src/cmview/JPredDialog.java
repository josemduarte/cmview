package cmview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import owl.core.connections.JPredConnection;
import owl.core.connections.JPredProgressRetriever;
import owl.core.connections.JPredStopNotifier;
import owl.core.structure.features.SecondaryStructure;

public class JPredDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	// gui components
	JFrame parent;
	JPanel progressPanel;
	JPanel buttonBar;
	JButton cancelButton;
	JProgressBar progressBar;
	JLabel label;
	
	// JPred related veriables
	JPredStopNotifier jpredStopNotifier;	// object to notify JPredConnection to stop
	SecondaryStructure result;				// holds the result of the Jpred run
											// will be null unless a call to
											// runJPred was successfully completed
	
	/**
	 * Creates a new Jpred dialog. Dialog needs to be shown with showGui() and JPred needs to be started with runJPred().
	 * @param parent the parent frame
	 * @param title the dialog title (actually ignored)
	 * @param sequence the sequence for which secondary structure prediction will be performed
	 */
	public JPredDialog(JFrame parent, String title) {
		
		super(parent, title, true);
		
		// init gui
		
		this.result = null;
		jpredStopNotifier = new JPredStopNotifier();
		this.parent = parent;
		
		this.setTitle("Secondary Structure Prediction");
		
		label = new JLabel("Idle");
		progressBar = new JProgressBar(0,JPredProgressRetriever.Status.values().length-1);

		progressPanel = new JPanel();
		progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.PAGE_AXIS));
		progressPanel.add(Box.createRigidArea(new Dimension(500, 20)));
		progressPanel.add(label);
		progressPanel.add(Box.createRigidArea(new Dimension(0, 20)));
		progressPanel.add(progressBar);
		progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setAlignmentX(RIGHT_ALIGNMENT);
		
		buttonBar = new JPanel();
		buttonBar.setLayout(new BoxLayout(buttonBar, BoxLayout.LINE_AXIS));
		buttonBar.setAlignmentX(RIGHT_ALIGNMENT);
		buttonBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		buttonBar.add(cancelButton);
			
		// Put everything together, using the content pane's BorderLayout.
		Container contentPane = getContentPane();
		contentPane.add(progressPanel, BorderLayout.CENTER);
		contentPane.add(buttonBar, BorderLayout.PAGE_END);	
			
	}

	/**
	 * Shows the dialog on screen and blocks until the cancel button has been pressed or
	 * the JPredCalculation has finished.
	 */
	public void showGui() {
		setLocationRelativeTo(getParent());
		pack();
		setVisible(true);
	}
	
	/**
	 * Runs JPred in a thread. The resulting resulting secondary structure object
	 * (or null if something went wrong or execution was cancelled by the user)
	 * will be stored in a member variable and can be retrieved using getResult()
	 * Since this dialog is modal, there is no need to notify the calling object
	 * on completion. When showGui returns, the result can be retrieved.
	 */
	public void runJPred(String seq) {
		JPredProgressRetriever progressRetriever = new JPredProgressRetriever(this) {
			public void setStatus(Status stat) {
				((JPredDialog) parent).updateStatus(stat);
			}
			
			public void setError(Throwable e) {
				((JPredDialog) parent).setError(e);
			}
			
			public void setResult(SecondaryStructure ss) {
				((JPredDialog) parent).setResult(ss);
			}
		};
		//SecondaryStructure result = null;
		JPredRunner jpred = new JPredRunner(seq, progressRetriever, jpredStopNotifier);
		Start.getThreadPool().submit(jpred);		// because jpred is a Callable and not Runnable

		// The following code does work but was abandoned because it is blocking:		
		//		Future<SecondaryStructure> runnerTask = Start.getThreadPool().submit(jpred);
//		try {
//			result = runnerTask.get();
//		} catch (InterruptedException e1) {
//			this.setError(e1);
//		} catch (ExecutionException e1) {
//			this.setError(e1);
//		}
//		this.result = result;
//		this.dispose();
	}

	/**
	 * Returns the result of the JPred run. The calling order should be
	 * 1. runJPred(seq) - starts the JPred connection in a background thread
	 * 2. showGui() - shows progress of the connection in the dialog
	 * 3. getResult() - returns the result of the connection (or null)
	 * @return the secondary structure predicted by JPred or null on error or cancel
	 */
	public SecondaryStructure getResult() {
		return this.result;
	}
	
	// callback methods
	
	public void updateStatus(JPredProgressRetriever.Status stat) {
		this.label.setText(stat.toString());
		this.progressBar.setValue(stat.ordinal());
	}
	
	public void setError(Throwable e) {
		this.label.setForeground(Color.red);
		this.label.setText("Error: \n" + e.getMessage());
		this.progressBar.setForeground(Color.red);
		System.err.println(e.getMessage());
	}
	
	public void setResult(SecondaryStructure ss) {
		this.result = ss;
		this.dispose();
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == cancelButton) {
			System.out.println("Cancel button pressed");
			jpredStopNotifier.setStop(true);
			this.dispose();
		}
	}
	
	@Override
	public void finalize() {
		System.out.println("JPredDialog will be Garbage Collected");
	}
	
	// test method
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setTitle("Debugging frame");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(false);
		
		JPredDialog diag = new JPredDialog(frame, "Test");
		diag.runJPred(JPredConnection.SAMPLE_QUERY);
		diag.showGui();
		SecondaryStructure ss = diag.getResult();
		if(ss == null) {
			System.out.println("JPred run was unsuccessfull. Exiting.");
		} else  {
			System.out.println("Predicted secondary structure is: ");
			System.out.println(ss);
		}
	}

}
