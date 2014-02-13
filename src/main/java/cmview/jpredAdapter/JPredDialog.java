package cmview.jpredAdapter;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cmview.Start;

import owl.core.connections.JPredProgressRetriever;
import owl.core.connections.JPredStopNotifier;
import owl.core.structure.features.SecondaryStructure;

public class JPredDialog extends JDialog implements ActionListener {

	// constants
	private static final long serialVersionUID = 1L;
	private static final String WINDOW_TITLE="Running JPred...";
	private static final String LABEL_CONNECTING="Connecting to server";
	private static final String LABEL_WAITING="Job waiting in queue";
	private static final String LABEL_RUNNING="Job running";
	private static final String LABEL_DONE="Job finished";
	private static final String LABEL_ERROR="Connection Error";
	
	// gui components
	JFrame parent;
	JButton cancelButton;
	ImageIcon iconOpen, iconProgress, iconDone, iconWait, iconError;
	JLabel labelConnecting, labelWaiting, labelRunning, labelDone, labelError;
	
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
	public JPredDialog(JFrame parent) {
		
		super(parent, WINDOW_TITLE, true);
		
		// init members

		this.parent = parent;
		this.result = null;
		jpredStopNotifier = new JPredStopNotifier();
		
		// init gui
		
		this.setResizable(false);
		this.setModal(true);
		
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);

		JPanel inputPane = new JPanel();
		
		iconDone = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "tick.png"));
		iconProgress = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "cog.png"));
		iconOpen = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "lightning.png"));
		iconWait = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "cup.png"));
		iconError = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "exclamation.png"));
		
		labelConnecting = new JLabel(LABEL_CONNECTING,iconOpen,JLabel.LEFT);
		labelWaiting = new JLabel(LABEL_WAITING,iconOpen,JLabel.LEFT);
		labelRunning = new JLabel(LABEL_RUNNING,iconOpen,JLabel.LEFT);
		labelDone = new JLabel(LABEL_DONE,iconOpen,JLabel.LEFT);
		labelError = new JLabel(LABEL_ERROR,iconError,JLabel.LEFT);
		
		labelConnecting.setEnabled(false);
		labelWaiting.setEnabled(false);
		labelRunning.setEnabled(false);
		labelDone.setEnabled(false);
		labelError.setVisible(false);
		
		inputPane.add(Box.createRigidArea(new Dimension(0, 5)));
		inputPane.add(labelConnecting);
		inputPane.add(Box.createRigidArea(new Dimension(0, 5)));
		inputPane.add(labelWaiting);
		inputPane.add(Box.createRigidArea(new Dimension(0, 5)));
		inputPane.add(labelRunning);
		inputPane.add(Box.createRigidArea(new Dimension(0, 5)));
		inputPane.add(labelDone);
		inputPane.add(Box.createRigidArea(new Dimension(0, 8)));
		inputPane.add(labelError);

		inputPane.setLayout(new BoxLayout(inputPane,BoxLayout.PAGE_AXIS));

		JPanel selectionPane = new JPanel();
		selectionPane.setLayout(new BoxLayout(selectionPane, BoxLayout.PAGE_AXIS));
		
		// selectionPane.add(labelPane);
		inputPane.setAlignmentX(CENTER_ALIGNMENT);
		selectionPane.add(inputPane);
		selectionPane.add(Box.createRigidArea(new Dimension(250, 10)));
		selectionPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Lay out the buttons from left to right.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(cancelButton);

		// Put everything together, using the content pane's BorderLayout.
		Container contentPane = getContentPane();
		contentPane.add(selectionPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);
			
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
		
		switch(stat) {
		case SENDING:
			labelConnecting.setEnabled(true);
			labelConnecting.setIcon(iconProgress);
			break;
		case WAITING:
			labelConnecting.setIcon(iconDone);
			labelWaiting.setEnabled(true);
			labelWaiting.setIcon(iconWait);
			break;
		case RUNNING:
			labelWaiting.setIcon(iconDone);
			labelRunning.setEnabled(true);
			labelRunning.setIcon(iconProgress);
			break;
		case RESULT:
			labelRunning.setIcon(iconDone);
			labelDone.setEnabled(true);
			labelDone.setIcon(iconProgress);
		}
		this.repaint();
	}
	
	public void setError(Throwable e) {
		labelError.setVisible(true);
		this.repaint();
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
		// TODO: This is apparently never called. Do we have a memory leak here?
		System.out.println("JPredDialog will be Garbage Collected");
	}
	
	// test method
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setTitle("Debugging frame");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(false);
		
		JPredDialog diag = new JPredDialog(frame);
		//diag.runJPred(JPredConnection.SAMPLE_QUERY);
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
