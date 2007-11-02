package cmview.sadpAdapter;

import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
    private Retriever  progressBarUpdater = null;
    private Future     runnerTask         = null;
    private SADPDialogDoneNotifier notifier = null;

    public SADPDialog(View view, String title, SADPRunner runner, int constructionInfo) {
	super(view,title,constructionInfo,true);
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
	notifier = new SADPDialogDoneNotifier(runner);
	notifier.addActionListener(view);
	
	// set the runner's action to be performed when the computation of the
	// pairwise alignment is done. This Doer sets the status of the dialog 
	// to DONE. The dialog-state is being checked by the watcher (s.b.).
	Object[] toBeNotified = new Object[2];
	toBeNotified[0] = this;
	toBeNotified[1] = notifier;
	runner.setActionWhenDone(new Doer(toBeNotified) {
	    public void doit() {
		((SADPDialog)             ((Object[]) getObject())[0]).setStatus(SADPDialog.DONE);
		((SADPDialog)             ((Object[]) getObject())[0]).dispose();
		((SADPDialogDoneNotifier) ((Object[]) getObject())[1]).notify(SADPDialog.DONE);
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
	System.out.println("SADPDialog: START button pressed");
	getStartButton().setEnabled(false);
	getPreferencesButton().setEnabled(false);

	// compute the alignment in a different thread
	runnerTask = Start.threadPool.submit(runner);
    }

    public void preferences() {
	// TODO: shows new dialog with the advanced options to be set.
	System.out.println("SADPDialog: PREFERENCES button pressed");
    }

    public void cancel() {
	// TODO: kills thread running SADP and cleans up everything.
	System.out.println("SADPDialog: CANCEL button pressed");
	stopRunner(); // stop runner if it already has been started
	if( getStatus() == ToolDialog.STARTED ) {
	    notifier.notify(ToolDialog.CANCELED_AT_STARTED);
	} else {
	    notifier.notify(ToolDialog.CANCELED_AT_IDLE);
	}
	dispose();
    }

    /**
     * Tries to stop runner if it is running.
     * */
    public void stopRunner() {
	if( runnerTask != null && !runnerTask.isDone() ) {
	    System.out.println("cancel runnerTask success:"+runnerTask.cancel(true));
	}
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
    
    /**
     * @param args
     */
    public static void main(String[] args) {
	PdbFileModel mod1,mod2;
		
	try {
	    mod1 = new PdbFileModel(args[0],"A","ALL",5.0,0,20);
	} catch (ModelConstructionError e) {
	    System.err.println("Error: Construction of first model failed! ("+e.getMessage()+")");
	    return;
	}
	
	try {
	    mod2 = new PdbFileModel(args[1],"A","ALL",5.0,0,20);
	} catch (ModelConstructionError e) {
	    System.err.println("Error: Construction of second model failed! ("+e.getMessage()+")");
	    return;
	}
	
	JFrame frame = new JFrame();
	frame.setVisible(true);
	frame.pack();
	SADPRunner runner     = new SADPRunner(mod1,mod2);
	SADPDialog sadpDialog = new SADPDialog(new View(null,"bla"),"Pairwise Protein Alignment",runner,SADPDialog.CONSTRUCT_EVERYTHING);
	Future<Integer> futureDialog = Start.threadPool.submit((Callable<Integer>) sadpDialog);
	Integer exitStatus = 0;
	try {
	    exitStatus = futureDialog.get();
	} catch(InterruptedException e) {
	    System.err.println("Thread running sadpDialog has been interrupted:"+e.getMessage());
	} catch(ExecutionException e) {
	    System.err.println("Execution of sadpDialog raise a ExecutionException:"+e.getMessage());
	    
	}
	System.out.println("exitStatus"+exitStatus);
	frame.dispose();
    }

}
