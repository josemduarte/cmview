
import java.io.PrintWriter;
import tools.PyMol;
import tools.PymolServerOutputStream;
import java.util.*;


public class MyTestPyMol {
	

/**
 * Test class for the PyMol class (our java to PyMol API) 
 * @param file where PyMol script will be written
 * @author duarte, updated to MyTestPyMol by: Juliane Dinse, Date: 01/03/2007
 * 
 * MyTestPyMol sends commands directly to Pymol.
 * 
 * - recieves (square) selections of contact map and illustrates them as PyMol-distance objects.
 * - recieves coordinates of comman neighbours of selected residues and illustrates them as triangles. 
 *
		 */
		private Start start;
		private Model mod;
		private View view;
		public PrintWriter Out = null;	
		public PyMol pymol;
	

		public String pdbFileName;
		public String accessionCode;
		public String chaincode;
		public int trinum;
		public String selectionType;
	
		private PaintController pc;
		public int[][] matrix = new int[0][];
		public int[][] selmatrix = new int[0][];
		public int[][] triangle= new int[1][];
		public int[] selrec = new int[4];
		
		
		// constructor
		public MyTestPyMol(Start start, Model mod, View view, PaintController pc, PyMol pymol){
			this.start=start;
			this.mod = mod;
			this.view=view;
			this.pc=pc;
			this.pymol=pymol;
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
			
		
			String url = "http://mauve:9123";
			
			pdbFileName = start.getPDBString();
			System.out.println(pdbFileName);
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
			
			// more pymol commands

		public void SquareCommands(){
			int i,j;
			int k = view.getSelNum();
			matrix = pc.getSelectMatrix();	
			if(pc == null) System.out.println("Paint is null");
			selrec = pc.getSelectRect();
			selectionType = view.getSelectionType();
			
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
						pymol.setDistance(resi1, resi2,accessionCode+ selectionType, k, chaincode);
					}
				}
			}
			
			Out.println("cmd.hide('labels')");
		}
		
		public void showTriangles(){
			//running python script for creating the triangles with the given residues
			Out.println("run /amd/white/2/project/StruPPi/PyMolAll/pymol/scripts/ioannis/graph.py");
			
			if(pc == null) System.out.println("Paint is null");
			triangle = pc.getResidues();
			int[] selectresi = new int[triangle.length+2];
			trinum = pc.getTriangleNumber();
			Random generator = new Random(trinum/2);
			
			String[] color = {"blue", "red", "yellow", "magenta", "cyan", "tv_blue", "tv_green", "salmon", "warmpink"};
			for (int i =0; i< trinum; i++){
				
				int res1 = triangle[i][0];
				int res2 = triangle[i][1];
				int res3 = triangle[i][2];
				
				int random = (Math.abs(generator.nextInt(trinum)) * 23) % trinum;
				System.out.println("RN: "+ random);
				
				Out.println("triangle "+ accessionCode+"Triangle"+i + ", "+ res1+ ", "+res2 +", "+res3 +", " + color[random]);
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
			int k = view.getSelNum();
			selectionType = view.getSelectionType();
			
			if(pc == null) System.out.println("Paint is null");
			selmatrix = pc.getSelectMatrix();	
			int [] size = mod.getMatrixSize();
			int dim1 = size[0];
			int dim2 = size[1];
			
			
			for (i = 0; i<= dim1; i++){
				for (j = 0; j<= dim2; j++){
					
					if (selmatrix[i][j] ==10){
						
						int resi1 = i;
						int resi2 = j;
						//inserts an edge between the selected residues 
						pymol.setDistance(resi1, resi2, accessionCode+selectionType, k, chaincode);
						
					}
					
				}
				
			}

			Out.println("cmd.hide('labels')");
			
			
		}
		


}





