package cmview;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import tools.PymolServerOutputStream;
import java.util.*;

import proteinstructure.*;

/**
 * Encapsulates the code for communication with a PyMol server.   	 
 * TODO: Should be designed such that the visualization frontend can be easily changed (e.g. to JMol). 
 */	
public class PyMolAdaptor {

	/*------------------------------ constants ------------------------------*/
	public static final String 		PYMOLFUNCTIONS_SCRIPT = "cmview.py";	 	// extending pymol with custom functions, previously called graph.py
	public static final String		PYMOL_CALLBACK_FILE = 	"cmview.callback"; 	// file being written by pymol to send messages to this application
	// colors for triangles, one is chosen randomly from this list
	private static final String[] COLORS = {"blue", "red", "yellow", "magenta", "cyan", "tv_blue", "tv_green", "salmon", "warmpink"};

	/*--------------------------- member variables --------------------------*/
	private String url;
	private PrintWriter Out;
	private boolean connected; 		// indicated whether a connection to pymol server had been established already

	/*----------------------------- constructors ----------------------------*/
	
	/**
	 *  Create a new Pymol communication object 
	 */
	public PyMolAdaptor(String pyMolServerUrl){
		this.url=pyMolServerUrl;
		this.connected = false;  // run tryConnectingToPymol() to connect
	}

	/*---------------------------- private methods --------------------------*/

	/**
	 * Construct a pymol object name from a pdb code and chain code.
	 */
	private String getChainObjectName(String pdbCode, String chainCode) {
		return pdbCode + chainCode;
	}
	
	/**
	 * Constructs a name for a selection.
	 * @param chainObjName The name of the chain object
	 * @param selSerial The serial number of the selection
	 * @return The selection name
	 */
	private String getSelObjectName(String chainObjName, int selSerial) {
		return chainObjName + "Sel" + selSerial;
	}
	
	/**
	 * Construct a name for a neighbourhood object.
	 */
	private String getNbhObjectName(String chainObjName, int nbhSerial) {
		return chainObjName + "Nbh" + nbhSerial;
	}
	
	/** Send command to pymol and check for errors */
	private void sendCommand(String cmd) {
		Out.println(cmd);
		if(Out.checkError()) {
			System.err.println("Pymol communication error. The last operation may have failed. Resetting connection.");
			this.Out = new PrintWriter(new PymolServerOutputStream(url),true);
		}
	}
	
	/**
	 * Creates an edge between the C-alpha atoms of the given residues in the given chain. 
	 * The selection in pymol will be names pdbcodeChaincode+"Sel"+selNum 
	 */
	private void setDistance(int i, int j, int pymolSelSerial, String selObjName, String chainObjName, String chainCode){
		String pymolStr;
		pymolStr = "distance "+selObjName +", " 
			+ chainObjName + " and chain " + chainCode + " and resi " + i + " and name ca, " 
			+ chainObjName + " and chain " + chainCode + " and resi " + j + " and name ca"; 
		this.sendCommand(pymolStr);
	}

	/** 
	 * Create a selection of the given residues in pymol.
	 */
	private void createSelectionObject(String selObjName, String chainObjName, String chainCode, ArrayList<Integer> residues) {
		String resString = "";
		int start, last;
		
		// TODO: use NodeSet instead of ArrayList and replace the following by NodeSet.getIntervals()
		Collections.sort(residues);
		last = residues.get(0);
		start = last;
		for(int i:residues) {
			if(i > last+1) {
				resString += "resi " + (last-start == 0?last:(start + "-" + last)) + " or ";
				start = i;
				last = i;
			} else
			if(i == last) {
				// skip
			} else
			if(i == last+1) {
				last = i;
			}
		}
		resString += "resi " + (last-start == 0?last:(start + "-" + last));
		resString = "(" + resString + ")";
		//System.out.println(resString);

		if (resString.length() + 100 < PymolServerOutputStream.PYMOLCOMMANDLENGTHLIMIT) {
			sendCommand("select "+selObjName+", "+chainObjName+" and chain "+chainCode+" and "+resString);
		} else {
			System.err.println("Couldn't create pymol selection. Too many residues.");
		}
	}

	/*---------------------------- public methods ---------------------------*/

	/**
	 * Try connecting to pymol server. Returns true on success, false otherwise.
	 */
	public boolean tryConnectingToPymol(long timeoutMillis) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis()-start < timeoutMillis) {
			try {
				String cmd;
				File f = new File(Start.getResourcePath(PYMOL_CALLBACK_FILE));
				OutputStream test = new PymolServerOutputStream(this.url);
				cmd = "run "+Start.getResourcePath(PYMOLFUNCTIONS_SCRIPT);
				test.write(cmd.getBytes());
				test.flush();
				cmd = "callback "+Start.getResourcePath(PYMOL_CALLBACK_FILE) + ", " + new Date();
				test.write(cmd.getBytes());
				test.flush();
				if(f.exists()) {
					f.deleteOnExit();
					hooray(test);
					return true;
				} else {
					test.close();
					continue;
				}
			} catch (Exception e) {
				continue;
			}
		}
		return false;
	}
	
	/** being called when a connection to pymol has been successfully established */ 
	private void hooray(OutputStream s) {
		this.connected = true;
		this.Out = new PrintWriter(s,true);
		sendCommand("set dash_gap, 0");
		sendCommand("set dash_width, 1.5");
	}
	
	/**
	 * Shuts down the external viewer instance and releases all resources of this Adaptor.
	 * @param url The PyMol server url // TODO: can we get rid of this?
	 */
	public void shutdown(String url) {
		Out.println("quit");
		Out.close();
	}
	
	/**
	 * Send command to the pymol server to load a structure with the given name from the given temporary pdb file.
	 */
	public void loadStructure(String fileName, String pdbCode, String chainCode) {
		String chainObjName = getChainObjectName(pdbCode, chainCode);
		sendCommand("load " + fileName + ", " + chainObjName);
		sendCommand("hide lines");
		sendCommand("show cartoon");		
	}
	
	public void alignStructure(String pdbCodeFirst, String chainCodeFirst,  String pdbCodeSecond, String chainCodeSecond){
		sendCommand("align " + pdbCodeFirst + chainCodeFirst+ ", " + pdbCodeSecond + chainCodeSecond);
	}
	
	/**
	 * Show the given edge neighbourhood as triangles in PyMol
	 */
	public void showTriangles(String pdbCode, String chainCode, EdgeNbh commonNbh, int pymolNbhSerial){
		String chainObjName = getChainObjectName(pdbCode, chainCode);
		String nbhObjName = getNbhObjectName(chainObjName, pymolNbhSerial);
		int trinum=1;
		ArrayList<Integer> residues = new ArrayList<Integer>();
		int i = commonNbh.i_resser;
		int j = commonNbh.j_resser;
		residues.add(i);
		residues.add(j);

		for (int k:commonNbh.keySet()){
						
			Random generator = new Random(trinum/2);
			int random = (Math.abs(generator.nextInt(trinum)) * 23) % trinum;
			
			sendCommand("triangle('"+ nbhObjName +"Tri"+trinum + "', "+ i+ ", "+j +", "+k +", '" + COLORS[random] +"', " + 0.7+")");
			trinum++;
			residues.add(k);	
		}
		sendCommand("zoom");
		createSelectionObject(nbhObjName + "Nodes", chainObjName, chainCode, residues );
	}

	/** Show the contacts in the given contact list as edges in pymol */
	public void edgeSelection(String pdbCode, String chainCode, int pymolSelSerial, EdgeSet selContacts){
		String chainObjName = getChainObjectName(pdbCode, chainCode);
		if (selContacts.size()== 0) return; // if no contacts in selection do nothing
		ArrayList<Integer> residues = new ArrayList<Integer>();
		String selObjName = getSelObjectName(chainObjName,pymolSelSerial);
		for (Edge cont:selContacts){ 
			int i = cont.i;
			int j = cont.j;
			//inserts an edge between the selected residues
			this.setDistance(i, j, pymolSelSerial, selObjName, chainObjName, chainCode);
			residues.add(i);
			residues.add(j);
		}
		sendCommand("hide labels, " + selObjName);
		createSelectionObject(selObjName+"Nodes", chainObjName, chainCode, residues);
	}
	
	/** Show a single contact or non-contact as distance object in pymol */
	public void sendSingleEdge(String pdbCode, String chainCode, int pymolSelSerial, Edge cont) {
		String chainObjName = getChainObjectName(pdbCode, chainCode);
		String selObjName = getSelObjectName(chainObjName,pymolSelSerial);
		setDistance(cont.i, cont.j, pymolSelSerial, selObjName, chainObjName, chainCode);
		ArrayList<Integer> residues = new ArrayList<Integer>();
		residues.add(cont.i);
		residues.add(cont.j);
		sendCommand("color orange, " + selObjName);
		createSelectionObject(selObjName+"Nodes", chainObjName, chainCode, residues);
	}
	
	/**
	 * Returns whether a connection of this Adaptor to the server had been already successfully established.
	 * @return true if connection was established, false otherwise
	 */
	public boolean isConnected() {
		return this.connected;
	}
	
}





