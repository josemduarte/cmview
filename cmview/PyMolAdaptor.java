package cmview;

import java.io.PrintWriter;
import tools.PyMol;
import tools.PymolServerOutputStream;
import java.util.*;
import proteinstructure.*;


public class PyMolAdaptor {


	/**
	 * Encapsulates the code for communication with a particular PyMol server instance. 
	 * 
	 * @author Jose Duarte
	 * @author Juliane Dinse
	 * Class: PyMolAdaptor
	 * Package: cm2pymol
	 * Date: 29/03/2007, last updated: 10/05/2007
	 * 
	 * PyMolAdaptor sends selected data and commands directly to Pymol.
	 * 
	 * - receives edge selections (coming from square or fill select) of contact map and shows them as PyMol distance objects
	 * - receives coordinates of common neighbours (in an EdgeNbh object) of selected residues and shows them as triangles (CGO-object
	 *   with integrated transparency parameter)
	 *
	 */
	
	// colors for triangles, one is chosen randomly from this list
	private static final String[] COLORS = {"blue", "red", "yellow", "magenta", "cyan", "tv_blue", "tv_green", "salmon", "warmpink"};
	private static final String PYMOLFUNCTIONS_SCRIPT= "/project/StruPPi/PyMolAll/pymol/scripts/ioannis/graph.py";
	
	private String url;
	private PrintWriter Out = null;	
	private PyMol pymol;

	private String pdbFileName;
	private String accessionCode;
	private String chaincode;

	

	/**
	 *  Create a new PymolCommunication object 
	 */
	// TODO: Remove dependencies
	public PyMolAdaptor(String pyMolServerUrl,
			String pdbCode, String chainCode, String fileName){
		this.url=pyMolServerUrl;

		this.pdbFileName = fileName;
		this.accessionCode = pdbCode;
		this.chaincode = chainCode;
	}

	public void pyMolAdaptorInit(){

		// to output only to server we would need the following PrintWriter
		Out = new PrintWriter(new PymolServerOutputStream(url),true);

		/** Initialising PyMol */

		pymol = new PyMol(Out);
		this.Out.println("load " + pdbFileName + ", " + getChainObjectName());
		//pymol.loadPDB(pdbFileName);
		pymol.myHide("lines");
		pymol.myShow("cartoon");
		pymol.set("dash_gap", 0, "", true);
		pymol.set("dash_width", 2.5, "", true);

		//running python script that defines function for creating the triangles for given residues
		Out.println("run "+PYMOLFUNCTIONS_SCRIPT);

	}

	/** 
	 * Returns the name of the object for the current chain in pymol 
	 */
	// TODO: Move these two functions to calling object to make this independent of chain
	public String getChainObjectName() {
		String objName;		
		if(this.chaincode.equals(Start.NULL_CHAIN_CODE)) {
			objName = this.accessionCode;
		} else {
			objName = this.accessionCode + this.chaincode;
		}
		return objName;
	}

	/**
	 *  Return the pymol selection string for the current chain 
	 */
	public String getChainSelector() {
		String selStr;
		if(this.chaincode.equals(Start.NULL_CHAIN_CODE)) {
			selStr = getChainObjectName();
		} else {
			selStr = getChainObjectName() + " and chain " + this.chaincode;
		}
		return selStr;			
	}

	/**
	 * Creates an edge between the C-alpha atoms of the given residues in the given chain. 
	 * The selection in pymol will be names pdbcodeChaincode+"Sel"+selNum 
	 */
	public void setDistance(int resi1, int resi2, int selNum){
		String pymolStr;
		if(this.chaincode.equals(Start.NULL_CHAIN_CODE)) {
			pymolStr = "distance "+ getChainObjectName() +"Sel"+selNum+", " 
			+ getChainObjectName() + " and resi " + resi1 + " and name ca, " 
			+ getChainObjectName() + " and resi " + resi2 + " and name ca";	    		  		
		} else {
			pymolStr = "distance "+ getChainObjectName() +"Sel"+selNum+", " 
			+ getChainObjectName() + " and chain " + this.chaincode + " and resi " + resi1 + " and name ca, " 
			+ getChainObjectName() + " and chain " + this.chaincode + " and resi " + resi2 + " and name ca"; 
		}
		this.Out.println(pymolStr);
	}

	/** 
	 * Create an object from a selection of residues in pymol 
	 * Return false on error 
	 */
	public boolean createSelectionObject(String objName, String parent, int[] residues) {
		String resString = "";
		int res;
		if(residues.length == 0) return false;

		resString += residues[0];
		for(int i = 1; i < residues.length; i++) {
			res = residues[i];
			resString += "," + res;
		}

		Out.println("create " + objName + ", (" + parent + " and resi " + resString + " and name ca)");
		return true;
	}

	public void showTriangles(EdgeNbh commonNbh){
		int trinum=1;
		String resi_num = "";
		for (int commonResser:commonNbh.keySet()){
			int res1 = commonNbh.i_resser;
			int res2 = commonNbh.j_resser;
			int res3 = commonResser;
						
			Random generator = new Random(trinum/2);
			int random = (Math.abs(generator.nextInt(trinum)) * 23) % trinum;
			
			Out.println("triangle('"+ getChainObjectName() +"Triangle"+trinum + "', "+ res1+ ", "+res2 +", "+res3 +", '" + COLORS[random] +"', " + 0.7+")");
			trinum++;
			
			resi_num += res1+"+"+res2+"+"+res3+"+";
		}
		
		// getting rid of the last "+"
		resi_num = resi_num.substring(0, resi_num.lastIndexOf('+'));

		// selecting all residues part of the common neighbourhood stored in the resi_num string
		pymol.select("Sele: "+accessionCode, resi_num);
		// TODO: Call createSelectionObject instead
	}


	public void edgeSelection(int selNum, ContactList selContacts){

		for (Contact cont:selContacts){ 
			int resi1 = cont.i;
			int resi2 = cont.j;
			//inserts an edge between the selected residues
			this.setDistance(resi1, resi2, selNum);			
		}
		Out.println("cmd.hide('labels')");
		// TODO: Create selection of participating residues
	}
}





