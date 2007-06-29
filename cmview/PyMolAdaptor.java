package cmview;

import java.io.PrintWriter;
import tools.PymolServerOutputStream;
import java.util.*;
import proteinstructure.*;


public class PyMolAdaptor {
	/**
	 * Encapsulates the code for communication with a particular PyMol server instance.
	 * TODO: Should be designed such that the output method can be easily changed (e.g. to JMol). 
	 * 
	 * @author Juliane Dinse
	 * @author Henning Stehr
	 * @author Jose Duarte
	 * Class: PyMolAdaptor
	 * Package: cmview
	 * Date: 29/03/2007, last updated: 12/06/2007
	 * 
	 * PyMolAdaptor sends selected data and commands directly to Pymol.
	 * 
	 * - receives edge selections (coming from square or fill select) of contact map and shows them as PyMol distance objects
	 * - receives common neighbours (in an EdgeNbh object) and shows them as triangles (CGO-object
	 *   with integrated transparency parameter.
	 */
	
	// colors for triangles, one is chosen randomly from this list
	private static final String[] COLORS = {"blue", "red", "yellow", "magenta", "cyan", "tv_blue", "tv_green", "salmon", "warmpink"};
	
	private String url;
	private PrintWriter Out;	
	private String pdbFileName;
	private String accessionCode;
	private String chainCode;
	private String pymolObjectName;

	/**
	 *  Create a new PymolCommunication object 
	 */
	public PyMolAdaptor(String pyMolServerUrl, String pdbCode, String chainCode, String fileName){
		this.url=pyMolServerUrl;

		this.pdbFileName = fileName;
		this.accessionCode = pdbCode;
		this.chainCode = chainCode;
		this.pymolObjectName = this.accessionCode + this.chainCode;
		
		this.Out = new PrintWriter(new PymolServerOutputStream(url),true);

		// Initialising PyMol
		sendCommand("set dash_gap, 0");
		sendCommand("set dash_width, 1.5");
		// loading the graph.py (now called cmview.py) script is now done in Start
		
		// Load structure
		loadStructure(pdbFileName, pymolObjectName);
		sendCommand("load " + pdbFileName + ", " + this.pymolObjectName);
		sendCommand("hide lines");
		sendCommand("show cartoon");
	}

	/*---------------------------- private methods --------------------------*/
	
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
	private void setDistance(int i, int j, int pymolSelSerial, String selObjName){
		String pymolStr;
		pymolStr = "distance "+selObjName +", " 
			+ pymolObjectName + " and chain " + this.chainCode + " and resi " + i + " and name ca, " 
			+ pymolObjectName + " and chain " + this.chainCode + " and resi " + j + " and name ca"; 
		this.sendCommand(pymolStr);
	}

	/** 
	 * Create a selection of the given residues in pymol.
	 */
	private void createSelectionObject(String selObjName, ArrayList<Integer> residues) {
		String resString = "";
		int start, last;
		
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
		//System.out.println(resString);

		if (resString.length() + 100 < PymolServerOutputStream.PYMOLCOMMANDLENGTHLIMIT) {
			sendCommand("select "+selObjName+", "+pymolObjectName+" and chain "+chainCode+" and "+resString);
		} else {
			System.err.println("Couldn't create pymol selection. Too many residues.");
		}
	}

	/*---------------------------- public methods ---------------------------*/
	
	/**
	 * Send command to the pymol server to load a structure with the given name from the given temporary pdb file.
	 */
	public void loadStructure(String fileName, String objectName) {
		sendCommand("load " + fileName + ", " + objectName);
		sendCommand("hide lines");
		sendCommand("show cartoon");		
	}
	
	/**
	 * Show the given edge neighbourhood as triangles in PyMol
	 */
	public void showTriangles(EdgeNbh commonNbh, int pymolNbhSerial){
		int trinum=1;
		ArrayList<Integer> residues = new ArrayList<Integer>();
		int i = commonNbh.i_resser;
		int j = commonNbh.j_resser;
		residues.add(i);
		residues.add(j);

		for (int k:commonNbh.keySet()){
						
			Random generator = new Random(trinum/2);
			int random = (Math.abs(generator.nextInt(trinum)) * 23) % trinum;
			
			sendCommand("triangle('"+ pymolObjectName +"Nbh"+pymolNbhSerial+"Tri"+trinum + "', "+ i+ ", "+j +", "+k +", '" + COLORS[random] +"', " + 0.7+")");
			trinum++;
			residues.add(k);	
		}
		createSelectionObject(pymolObjectName+"Nbh"+pymolNbhSerial+"Nodes", residues );
	}

	/** Show the contacts in the given contact list as edges in pymol */
	public void edgeSelection(int pymolSelSerial, ContactList selContacts){
		if (selContacts.size()== 0) return; // if no contacts in selection do nothing
		ArrayList<Integer> residues = new ArrayList<Integer>();
		String selObjName = pymolObjectName +"Sel"+pymolSelSerial;
		for (Contact cont:selContacts){ 
			int i = cont.i;
			int j = cont.j;
			//inserts an edge between the selected residues
			this.setDistance(i, j, pymolSelSerial, selObjName);
			residues.add(i);
			residues.add(j);
		}
		sendCommand("hide labels, " + selObjName);
		selObjName = pymolObjectName +"Sel"+pymolSelSerial+"Nodes";
		createSelectionObject(selObjName, residues);
	}
	
	/** Show a single contact or non-contact as distance object in pymol */
	public void sendSingleEdge(int pymolSelSerial, Contact cont) {
		String selObjName = pymolObjectName +"Sel"+pymolSelSerial;
		this.setDistance(cont.i, cont.j, pymolSelSerial, selObjName);
		sendCommand("color orange, " + selObjName);
	}
	
}





