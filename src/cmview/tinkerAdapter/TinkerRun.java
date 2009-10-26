package cmview.tinkerAdapter;


import java.io.IOException;

import proteinstructure.Pdb;
import actionTools.TinkerStatusNotifier;

import tinker.TinkerRunner;
import cmview.datasources.Model;


public class TinkerRun implements Runnable {

	TinkerRunAction action;
	Model mod;
	TinkerRunner.PARALLEL parallel;
	TinkerRunner.REFINEMENT refinement;
	int models;
	TinkerRunner runner;
	String tmpDir;
	public TinkerRun(TinkerRunAction action, Model mod,
			TinkerRunner.PARALLEL parallel, TinkerRunner.REFINEMENT refinement, int models, String tmpDir) {
			this.action = action;
			this.mod = mod;
			this.parallel = parallel;
			this.refinement = refinement;
			this.models = models;
			this.tmpDir = tmpDir;
			
	}
	
	public void stop() {
		if (parallel == TinkerRunner.PARALLEL.CLUSTER) {
			runner.stop();
		} 
	}
	
	public void run() {
		Pdb pdb = mod.runTinker(new TinkerStatusNotifier(this) { 
			public void sendStatus(tinker.TinkerRunner.STATE s) {
				action.sendStatus(s);
			}
			public void filesDone(int i) {
				action.filesDone(i);
			}
			},
			
			parallel, refinement, models,tmpDir);
		
		action.sendStatus(TinkerRunner.STATE.LOADING);
		
			try {
				pdb.dump2pdbfile(tmpDir+"/selected");
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
			action.returnResult(tmpDir+"/selected");
		
		
		
	}
	
}