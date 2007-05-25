package cmview;
/**
 * This class is passed to a LoadDialog instance to specify the action that
 * will happen if the ok button is pressed. It contains only the constructor
 * and an abstract doit() function which is called by LoadDialog().
 */

public abstract class LoadAction {
	
	public LoadAction() {
	}
	
	public abstract void doit(Object o, String f, String ac, String cc, String ct, double dist, int ss, String db, int gid);

}
