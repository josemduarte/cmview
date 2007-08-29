package cmview;
/**
 * Helper class for LoadDialog.
 * This abstract class can be instantianted and passed to a LoadDialog to specify the action that
 * will happen if the ok button is pressed. It contains a constructor
 * and an abstract doit() function which is called by LoadDialog.
 */

public abstract class LoadAction {
	
	boolean secondModel;	// flag which we use to distinguish between first and second model being loaded
	
	public LoadAction(boolean secondModel) {
		this.secondModel = secondModel;
	}
	
	public abstract void doit(Object o, String f, String ac, String cc, String ct, double dist, int minss, int maxss, String db, int gid);

}
