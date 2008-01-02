package cmview.sadpAdapter;

import java.awt.Component;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import cmview.Start;
import cmview.View;
import cmview.datasources.ModelConstructionError;
import cmview.datasources.PdbFileModel;
import cmview.toolUtils.ToolDialog;
import actionTools.Doer;
import actionTools.Retriever;

public class SADPDialog extends ToolDialog {

	static final long serialVersionUID = 1l;
	static final String INFO_TEXT = 
		"<html>" +
		"SADP is a heuristic pairwise structure alignment algorithm.<br>"+
		"</html>";

	private SADPRunner runner             = null;
	@SuppressWarnings("unused")
	private SADPResult result             = null; // set in runner via "actionWhenDone"
	private Retriever  progressBarUpdater = null;
	private Future     runnerTask         = null;
	private SADPDialogDoneNotifier notifier = null;

	public SADPDialog(View view, String title, SADPRunner runner, SADPResult result, int constructionInfo) {
		super(view,title,constructionInfo,true);
		this.result = result;
		this.runner = runner;
		// create the SADP progress information retriever which redirects the
		// current progress status to the dialog's progress bar.
		progressBarUpdater = new Retriever(getProgressBar()) {
			public void retrieve(Object obj) throws ClassCastException {
				//System.out.println(((Double) obj).intValue());
				((JProgressBar) getObject()).setValue(((Double) obj).intValue());
			}
		};
		runner.setProgressRetriever(progressBarUpdater);

		// show progress as percentage
		getProgressBar().setStringPainted(true);

		// create notifier and send notification to the parent frame 'f'
		notifier = new SADPDialogDoneNotifier(result);
		notifier.addActionListener(view);

		// set the runner's action to be performed when the computation of the
		// pairwise alignment is done. This Doer sets the status of the dialog 
		// to DONE. The dialog-state is being checked by the watcher (s.b.).
		Object[] toBeNotified = new Object[2];
		toBeNotified[0] = this;
		toBeNotified[1] = notifier;
		runner.setActionWhenDone(new Doer(toBeNotified) {
			public void doit() {
				SADPDialog             diag     = (SADPDialog)((Object[]) getObject())[0];
				SADPDialogDoneNotifier notifier = (SADPDialogDoneNotifier) ((Object[]) getObject())[1]; 
				
				// set status to DONE as this function is called when the 
				// SADPRunner has finished computation
				diag.setStatus(SADPDialog.DONE);
				
				// get SADPResult object from the runner task. This result is 
				// returned by function call(...) of the runner. 
				try {
					diag.setResult((SADPResult) diag.getRunnerTask().get());
				} catch(ExecutionException e) {
					JOptionPane.showMessageDialog(diag, 
							"An execution execption has been thrown while computing the pairwise structure alignment:\n"+e.getMessage(),
							"Error", 
							JOptionPane.ERROR_MESSAGE);
				} catch(InterruptedException e) {
					JOptionPane.showMessageDialog(diag, 
							"An interrupted exception has been thrown while computing the pairwise structure alignment:\n"+e.getMessage(),
							"Error", 
							JOptionPane.ERROR_MESSAGE);					
				}
				
				// dispose the dialog
				diag.dispose();
				
				// if the runner task has been started before the user pressed 
				// the Cancel button the computation of the structure alignment 
				// procedes. Anyway, we have to take account of the user-sided 
				// cancelation. This is warranted through by the invokation of 
				// secureNotify(int) instead of notify(int).
				notifier.secureNotify(SADPDialog.DONE);
			}
		});

		setResizable(false);
	}

	/**
	 * This function is invoked when the start button has been pressed.
	 * It starts the SADPRunner instance in a new thread. 
	 */
	public void start() {
		// TODO: starts SADP run in a new thread. shows progress in progress bar. closes window at return of function run or whenever the alignment is done.
		getStartButton().setEnabled(false);
		getPreferencesButton().setEnabled(false);

		// compute the alignment in a different thread
		runnerTask = Start.threadPool.submit(runner);
	}

	public void preferences() {
		// TODO: shows new dialog with the advanced options to be set.
	}

	public void cancel() {
		// TODO: kills thread running SADP and cleans up everything.
		if( getStatus() == ToolDialog.STARTED ) {
			notifier.notify(ToolDialog.CANCELED_AT_STARTED);
		} else {
			notifier.notify(ToolDialog.CANCELED_AT_IDLE);
		}
		dispose();
	}

	protected Component infoComponent() {
		JPanel infoPane = new JPanel();
		infoPane.setLayout(new BoxLayout(infoPane,BoxLayout.LINE_AXIS));
		infoPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		infoPane.add(new JLabel(INFO_TEXT));
		return infoPane;
	}

	public void setNotifier(SADPDialogDoneNotifier notifier) {
		this.notifier = notifier;	    
	}

	public SADPDialogDoneNotifier getNotifier() {
		return notifier;
	}

	public Future getRunnerTask() {
		return runnerTask;
	}
	
	public void setResult(SADPResult result) {
		this.result = result;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PdbFileModel mod1,mod2;

		try {
			mod1 = new PdbFileModel(args[0],"Ca",8.0,0,20);
			mod1.load("A",1);
		} catch (ModelConstructionError e) {
			System.err.println("Error: Construction of first model failed! ("+e.getMessage()+")");
			return;
		}

		try {
			mod2 = new PdbFileModel(args[1],"Ca",8.0,0,20);
			mod1.load(" ",1);
		} catch (ModelConstructionError e) {
			System.err.println("Error: Construction of second model failed! ("+e.getMessage()+")");
			return;
		}

//		JFrame frame = new JFrame();
//		frame.setVisible(true);
//		frame.pack();
		SADPResult result     = new SADPResult();
		SADPRunner runner     = new SADPRunner(mod1,mod2,result);
		SADPDialog sadpDialog = new SADPDialog(new View(null,"bla"),"Pairwise Protein Alignment",runner,result,SADPDialog.CONSTRUCT_EVERYTHING);
		sadpDialog.createGUI();
//		Future<Integer> futureDialog = Start.threadPool.submit((Callable<Integer>) sadpDialog);
//		Integer exitStatus = 0;
//		try {
//			exitStatus = futureDialog.get();
//		} catch(InterruptedException e) {
//			System.err.println("Thread running sadpDialog has been interrupted:"+e.getMessage());
//		} catch(ExecutionException e) {
//			System.err.println("Execution of sadpDialog raise a ExecutionException:"+e.getMessage());
//
//		}
//		System.out.println("exitStatus"+exitStatus);
//		frame.dispose();
	}

}
