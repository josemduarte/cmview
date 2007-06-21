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
	// TODO: Remove dependencies
	public PyMolAdaptor(String pyMolServerUrl, String pdbCode, String chainCode, String fileName){
		this.url=pyMolServerUrl;

		this.pdbFileName = fileName;
		this.accessionCode = pdbCode;
		this.chainCode = chainCode;
		this.pymolObjectName = this.accessionCode + this.chainCode;
		
		this.Out = new PrintWriter(new PymolServerOutputStream(url),true);

		// Initialising PyMol
		Out.println("load " + pdbFileName + ", " + this.pymolObjectName);
		Out.println("hide lines");
		Out.println("show cartoon");
		Out.println("set dash_gap, 0");
		Out.println("set dash_width, 1.5");
		//running python script that defines function for creating the triangles for given residues
		Out.println("run "+Start.PYMOLFUNCTIONS_SCRIPT);

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
		this.Out.println(pymolStr);
	}

	/** 
	 * Create an object from a selection of residues in pymol 
	 * Return false on error 
	 */
	private void createSelectionObject(String selObjName, ArrayList<Integer> residues) {
		String resString = "";

		resString += residues.get(0);
		for(int i = 1; i < residues.size(); i++) {
			resString += "+" + residues.get(i);
		}

		Out.println("select "+selObjName+", "+pymolObjectName+" and chain "+chainCode+" and resi "+resString);
	}

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
			
			Out.println("triangle('"+ pymolObjectName +"Nbh"+pymolNbhSerial+"Tri"+trinum + "', "+ i+ ", "+j +", "+k +", '" + COLORS[random] +"', " + 0.7+")");
			trinum++;
			residues.add(k);
			
		}

		createSelectionObject(pymolObjectName+"Nbh"+pymolNbhSerial+"Nodes", residues );
	}


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
		Out.println("cmd.hide('labels')");
		selObjName = pymolObjectName +"Sel"+pymolSelSerial+"Nodes";
		createSelectionObject(selObjName, residues);
	}
}





