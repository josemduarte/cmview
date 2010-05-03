package cmview.tinkerAdapter;

import owl.core.runners.tinker.TinkerRunner;
import owl.core.util.actionTools.TinkerStatusNotifier;

import cmview.datasources.Model;

/**
 * A Runnable class to run Tinker asynchronously.
 * @author Matthias Winkelmann
 *
 */

public class TinkerRun implements Runnable {

	private TinkerRunAction action;
	private Model mod;
	private TinkerRunner.PARALLEL parallel;
	private TinkerRunner.REFINEMENT refinement;
	private int models;
	private TinkerRunner runner;
	private boolean gmbp;
	private boolean ss;
	private String tmpDir;

	
	public TinkerRun(TinkerRunAction action, Model mod,
			TinkerRunner.PARALLEL parallel, TinkerRunner.REFINEMENT refinement, int models, boolean gmbp, boolean ss, String tmpDir) {
			this.action = action;
			this.mod = mod;
			this.parallel = parallel;
			this.refinement = refinement;
			this.models = models;
			this.tmpDir = tmpDir;
			this.gmbp = gmbp;
			this.ss = ss;
	}
	
	public void stop() {
		if (parallel == TinkerRunner.PARALLEL.CLUSTER) {
			runner.stop();
		} 
	}
	
	
	public void run() {
		TinkerRunner run = mod.runTinker(new TinkerStatusNotifier(this) { 
			@Override
			public void sendStatus(owl.core.runners.tinker.TinkerRunner.STATE s) {
				action.sendStatus(s);
			}
			@Override
			public void filesDone(int i) {
				action.filesDone(i);
			}
			
			},
			
			parallel, refinement, models,gmbp,ss,tmpDir);
		
		action.sendStatus(TinkerRunner.STATE.LOADING);
		
		action.returnResults(run);
		
		
		
	}
	
}