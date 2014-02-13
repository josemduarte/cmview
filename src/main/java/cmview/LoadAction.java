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
	
	/**
	 * Loads the chain from the given source. Some of these arguments might be 
	 * null according to the chosen source (e.g. if the source is a local pdb 
	 * file the database name does not need to be present).
	 * @param o  parent
	 * @param f  filename
	 * @param ac  accession code
	 * @param modelSerial the model serial
	 * @param loadAllModels whether to load a weighted ensembl graph
	 * @param cc  chain code
	 * @param ct  contact type
	 * @param dist  distance threshold for the contacts
	 * @param minss  minimal sequence separation
	 * @param maxss  maximal sequence separation
	 * @param db  name of the database
	 * @param gid  graph id
	 * @param seq pasted sequence
	 */
	public abstract void doit(Object o, String f, String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, int gid, String seq);

}
