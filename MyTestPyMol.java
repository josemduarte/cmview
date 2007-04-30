
import java.io.PrintWriter;
import tools.PyMol;
import tools.PymolServerOutputStream;
import java.util.*;


public class MyTestPyMol {
	

/**
 * Test class for the PyMol class (our java to PyMol API) 
 * @param file where PyMol script will be written
 * @author duarte, updated to MyTestPyMol by: Juliane Dinse, Date: 29/03/2007
 * 
 * MyTestPyMol sends selected data and commands directly to Pymol.
 * 
 * - recieves (square) selections of contact map and illustrates them as PyMol-distance objects.
 * - recieves coordinates of comman neighbours of selected residues and illustrates them as triangles (CGO-object)
 *   with integrated transparency parameter). 
 *
		 */
		private Start start;
		private Model mod;
		private View view;
		private PaintController pc;
		private String url;
		public PrintWriter Out = null;	
		public PyMol pymol;

		public String pdbFileName;
		public String accessionCode;
		public String chaincode;
		public int trinum;
		public String selectionType;
	
		public int[][] matrix = new int[0][];
		public int[][] selmatrix = new int[0][];
		public int[][] triangle= new int[1][];
		public int[] selrec = new int[4];
		
		// constructor
		public MyTestPyMol(Start start, Model mod, View view, PaintController pc, PyMol pymol, String pyMolServerUrl){
			this.start=start;
			this.mod = mod;
			this.view=view;
			this.pc=pc;
			this.pymol=pymol;
			this.url=pyMolServerUrl;
		}
		
		public void MyTestPyMolInit(){
			String[] args = {"",""};
			String file = "";
			boolean server=false;
			if (args.length<1){
				System.err.println("Give at least one file argument");
				System.exit(1);
			}		
			file = args[0];
			if (args.length>1){ // two arguments: output both file and server
				server=true;
			}
			
			pdbFileName = start.getPDBString();

			accessionCode= start.getAccessionCode();
			chaincode = start.getChainCode();

			// to output only to server we would need the following PrintWriter
			Out = new PrintWriter(new PymolServerOutputStream(url),true);
			
			/** Initialising PyMol */
			
			pymol = new PyMol(Out);		
			pymol.loadPDB(pdbFileName);
			pymol.myHide("lines");
			pymol.myShow("cartoon");
			pymol.set("dash_gap", 0, "", true);
			pymol.set("dash_width", 2.5, "", true);
   
		}
		
	    /** Creates an edge between the C-alpha atoms of the given residues in the given chain. 
	     *  The selection in pymol will be names pdbFileName+"Sel"+selNum 
	     */
	    public void setDistance(int resi1, int resi2, String pdbFilename, int selNum, String chain_pdb_code){   	
	    	//pymol.setDistance(resi1, resi2, pdbFilename, selNum, chain_pdb_code);
	    	this.Out.println("distance "+ pdbFilename+"Sel"+selNum+" , chain "+chain_pdb_code+" and resi " + resi1 + " and name ca, chain "+chain_pdb_code+" and resi " + resi2 + " and name ca;");
	    }
	    
	    /** Creates an edge between the C-alpha atoms of the given residues.
	     *  Use this variant if there is only one unnamed chain in the current structure.
	     *  The selection in pymol will be names pdbFileName+"Sel"+selNum 
	     */
	    public void setDistance(int resi1, int resi2, String pdbFilename, int selNum){
	    	//pymol.setDistance(resi1, resi2, pdbFilename, selNum);
	    	this.Out.println("distance "+ pdbFilename+"Sel"+selNum+" , resi " + resi1 + " and name ca, resi " + resi2 + " and name ca;");
	    }
		
			// more pymol commands

		public void SquareCommands(){
			int i,j;
			int k = view.getSelNum();
			selectionType = view.getSelectionType();
			matrix = pc.getSelectMatrix();	
			selrec = pc.getSelectRect();

			
			int xs = selrec[0]; //starting point: upper left, x-direction
			int ys = selrec[1]; //starting point: upper left, y-direction
			int rw = selrec[2]; //endpoint lower right, x-direction
			int rh = selrec[3]; //endpoint lower right, y-direction
			
			
			for (i = xs; i<= rw; i++){
				for (j = ys; j<= rh; j++){
					
					if (matrix[i][j] ==5){
						
						int resi1 = i;
						int resi2 = j;
						System.out.println("i: "+ i + " j: "+j);
						//inserts an edge between the selected residues 
						if(this.chaincode.equals(Start.NULL_CHAIN_CODE)) {
							this.setDistance(resi1, resi2,accessionCode+ selectionType, k);							
						} else {
							this.setDistance(resi1, resi2,accessionCode+ selectionType, k, chaincode);
						}
					}
				}
			}
			
			Out.println("cmd.hide('labels')");
		}
		
		public void showTriangles(){
			//running python script for creating the triangles with the given residues
			Out.println("run /amd/white/2/project/StruPPi/PyMolAll/pymol/scripts/ioannis/graph.py");

			trinum = pc.getTriangleNumber();
			triangle = pc.getResidues();
			int[] selectresi = new int[triangle.length+2];

			Random generator = new Random(trinum/2);
			
			String[] color = {"blue", "red", "yellow", "magenta", "cyan", "tv_blue", "tv_green", "salmon", "warmpink"};
			for (int i =0; i< trinum; i++){
				
				int res1 = triangle[i][0];
				int res2 = triangle[i][1];
				int res3 = triangle[i][2];
				
				int random = (Math.abs(generator.nextInt(trinum)) * 23) % trinum;
				Out.println("triangle('"+ accessionCode+"Triangle"+i + "', "+ res1+ ", "+res2 +", "+res3 +", '" + color[random] +"', " + 0.7+")");
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
			
		}
		
		public void FillCommands(){
			int i,j;
			int [] size = mod.getMatrixSize();
			int k = view.getSelNum();
			selectionType = view.getSelectionType();
			selmatrix = pc.getSelectMatrix();	

			
			int dim1 = size[0];
			int dim2 = size[1];
			
			
			for (i = 0; i<= dim1; i++){
				for (j = 0; j<= dim2; j++){
					
					if (selmatrix[i][j] ==10){
						
						int resi1 = i;
						int resi2 = j;
						//inserts an edge between the selected residues
						if(this.chaincode.equals(Start.NULL_CHAIN_CODE)) {
							this.setDistance(resi1, resi2, accessionCode+selectionType, k);
						} else {
							this.setDistance(resi1, resi2, accessionCode+selectionType, k, chaincode);
						}
					}
				}
			}

			Out.println("cmd.hide('labels')");
		}
}





