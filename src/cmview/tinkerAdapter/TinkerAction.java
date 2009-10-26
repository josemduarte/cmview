package cmview.tinkerAdapter;

import tinker.TinkerRunner;

public abstract class TinkerAction {
	
	public abstract void doit(TinkerRunner.PARALLEL parallel, TinkerRunner.REFINEMENT refinement, int models);
}
