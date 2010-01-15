package cmview.tinkerAdapter;

import java.io.File;
import java.io.IOException;

import tinker.TinkerRunner;
import cmview.Start;
import cmview.View;
import cmview.datasources.Model;
import cmview.datasources.ModelConstructionError;
import cmview.datasources.PdbFileModel;

/**
 * The main controller for running Tinker. Currently called by the View after the users
 * has completed the preference dialog. Creates threads for the TinkerRun and a TinkerWatcher 
 * that passes messages.
 * @author Matthias Winkelmann
 */

public class TinkerRunAction {

	private TinkerProgressDialog dialog;
	private TinkerRun tinkerRun;
	private Thread thread;
	private TinkerWatcher watcher;
	private Thread watcherThread;
	private View view;
	private TinkerTable tableView;
	public TinkerRunAction(View view, Model mod,
			TinkerRunner.PARALLEL parallel, TinkerRunner.REFINEMENT refinement,
			int models) {
		this.view = view;
		dialog = new TinkerProgressDialog(view, this, models);
		String tmpDir;
		try {
			tmpDir = createTmpDir(Start.TEMP_DIR, "tinker");
			tinkerRun = new TinkerRun(this, mod, parallel, refinement, models,
					tmpDir);

			thread = new Thread(tinkerRun);
			thread.start();

			watcher = new TinkerWatcher(tmpDir, this, tinkerRun);
			watcherThread = new Thread(watcher);
			watcherThread.start();
			dialog.createGUI();
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}

	}

	public void sendStatus(TinkerRunner.STATE s) {
		dialog.setState(s);
	}

	public void cancel() {
		watcher.stop();
	}


	
	public void returnResults(TinkerRunner run) {
		dialog.dispose();
		tableView = new TinkerTable(run,this,view);
		tableView.setLocationRelativeTo(view);
	
		
	}

	public void filesDone(int done) {
		dialog.filesDone(done);

	}

	private static String createTmpDir(String tempdir, String prefix)
			throws IOException {
		final File temp;

		temp = new File(tempdir + "/" + prefix
				+ Long.toString(System.nanoTime()));

		if (!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: "
					+ temp.getAbsolutePath());
		}
		temp.deleteOnExit();
		return (temp.getAbsolutePath());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PdbFileModel mod1;

		try {
			mod1 = new PdbFileModel(args[0], "Ca", 8.0, 0, 20);
			mod1.load("A", 1);
		} catch (ModelConstructionError e) {
			System.err.println("Error: Construction of first model failed! ("
					+ e.getMessage() + ")");
			return;
		}

		new TinkerRunAction(null, mod1, TinkerRunner.PARALLEL.NONE,
				TinkerRunner.REFINEMENT.MINIMIZATION, 1);

	}

}
