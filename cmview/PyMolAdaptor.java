package cmview;

import java.io.PrintWriter;
import tools.PyMol;
import tools.PymolServerOutputStream;
import java.util.*;


public class PyMolAdaptor {
	

/**
 * Encapsulates the code for communication with a particular PyMol server instance. 
 * 
 * @author Jose Duarte
 * @author Juliane Dinse
 * Class: MyTestPyMol
 * Package: cm2pymol
 * Date: 29/03/2007, last updated: 10/05/2007
 * 
 * MyTestPyMol sends selected data and commands directly to Pymol.
 * 
 * - recieves (square) selections of contact map and illustrates them as PyMol-distance objects.
 * - recieves coordinates of comman neighbours of selected residues and illustrates them as triangles (CGO-object)
 *   with integrated transparency parameter). 
 *
		 */
		//private Model mod;
		//private View view;
		//private PaintController pc;
		private String url;
		public PrintWriter Out = null;	
		public PyMol pymol;

		public String pdbFileName;
		public String accessionCode;
		public String chaincode;
		//public int trinum;
		//public String selectionType;
	
		public int[][] matrix = new int[0][];
		public int[][] selmatrix = new int[0][];
		//public int[][] triangle= new int[1][];
		public int[] selrec = new int[4];
		
		/** Create a new PymolCommunication object */
		// TODO: Remove dependencies
		public PyMolAdaptor(String pyMolServerUrl,
				           String pdbCode, String chainCode, String fileName){
			//this.mod = mod;
			//this.view=view;
			//this.pc=pc;
			this.url=pyMolServerUrl;
			
			this.pdbFileName = fileName;
			this.accessionCode = pdbCode;
			this.chaincode = chainCode;
		}
		
		public void MyTestPyMolInit(){

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
   
		}
		
		/** Returns the name of the object for the current chain in pymol */
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
		
		/** Return the pymol selection string for the current chain */
		public String getChainSelector() {
			String selStr;
			if(this.chaincode.equals(Start.NULL_CHAIN_CODE)) {
				selStr = getChainObjectName();
			} else {
				selStr = getChainObjectName() + " and chain " + this.chaincode;
			}
			return selStr;			
		}
		
	    /** Creates an edge between the C-alpha atoms of the given residues in the given chain. 
	     *  The selection in pymol will be names pdbcodeChaincode+"Sel"+selNum 
	     */
	    public void setDistance(int resi1, int resi2, int selNum){   	
	    	//pymol.setDistance(resi1, resi2, pdbFilename, selNum, chain_pdb_code);
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
	    	//System.out.println(pymolStr);
	    	this.Out.println(pymolStr);
	    }
		
		/** Create an object from a selection of residues in pymol 
		 * Return false on error */
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
	    
			// more pymol commands

		//public void SquareCommands(){
		public void SquareCommands(int selNum, int[][] matrix, int[] selrec){
			int i,j;
			//int k = view.getSelNum();
			//selectionType = view.getSelectionType();
			//matrix = pc.getSelectMatrix();	
			//selrec = pc.getSelectRect();
			
			int xs = selrec[0]; //starting point: upper left, x-direction
			int ys = selrec[1]; //starting point: upper left, y-direction
			int rw = selrec[2]; //endpoint lower right, x-direction
			int rh = selrec[3]; //endpoint lower right, y-direction
				
			for (i = xs; i<= rw; i++){
				for (j = ys; j<= rh; j++){
					
					if (matrix[i][j] == 5){
						
						int resi1 = i;
						int resi2 = j;
						//System.out.println("i: "+ i + " j: "+j);
						//inserts an edge between the selected residues 
						this.setDistance(resi1, resi2, selNum);							
					}
				}
			}
			
			Out.println("cmd.hide('labels')");
			// TODO: create selection of participating residues
		}
		
		//public void showTriangles(){
		public void showTriangles(int trinum, int[][] triangle){
			
			if(trinum == 0) {
				System.err.println("Warning: No common neighbourhood selected");
				return;
			}
			
			//running python script for creating the triangles with the given residues
			// TODO: Can this be run once on startup? + move constant to Start
			Out.println("run /amd/white/2/project/StruPPi/PyMolAll/pymol/scripts/ioannis/graph.py");
			
			//trinum = pc.getTriangleNumber();
			//triangle = pc.getResidues();
			int[] selectresi = new int[triangle.length+2];

			Random generator = new Random(trinum/2);
			
			String[] color = {"blue", "red", "yellow", "magenta", "cyan", "tv_blue", "tv_green", "salmon", "warmpink"};
			for (int i =0; i< trinum; i++){
				
				int res1 = triangle[i][0];
				int res2 = triangle[i][1];
				int res3 = triangle[i][2];
				
				int random = (Math.abs(generator.nextInt(trinum)) * 23) % trinum;
				Out.println("triangle('"+ getChainObjectName() +"Triangle"+i + "', "+ res1+ ", "+res2 +", "+res3 +", '" + color[random] +"', " + 0.7+")");
			}
			
			selectresi[0] = triangle[0][0];
			selectresi[1] = triangle[0][1];
			
			for (int i =2; i<trinum;i++){
				int resi = triangle[i][2];
				selectresi[i]=resi;
			}
			
			String resi_num = ""+ selectresi[0];
			
			for(int i=1; i<trinum; i++){
				resi_num = resi_num + "+"+selectresi[i];
			}
			
			pymol.select("Sele: "+accessionCode, resi_num);
			// TODO: Call createSelectionObject instead
		
		}
		
		//public void FillCommands() {
		public void FillCommands(int selNum, int[][] selmatrix, int matrixSize){
			int i,j;
			//int [] size = mod.getMatrixSize();
			//int k = view.getSelNum();
			//selectionType = view.getSelectionType();
			//selmatrix = pc.getSelectMatrix();	
		
			int dim1 = matrixSize;
			int dim2 = matrixSize;
			
			for (i = 0; i < dim1; i++){
				for (j = 0; j < dim2; j++){
					
					if (selmatrix[i][j] ==10){
						
						int resi1 = i;
						int resi2 = j;
						//inserts an edge between the selected residues
						this.setDistance(resi1, resi2, selNum);
					}
				}
			}

			Out.println("cmd.hide('labels')");
			// TODO: Create selection of participating residues
		}
}





