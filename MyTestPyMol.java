

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.FileOutputStream;

import tools.PyMol;
import tools.PymolServerOutputStream;
import tools.MultiOutputStream;


public class MyTestPyMol {
	

/**
 * Test class for the PyMol class (our java to PyMol API) 
 * @param file where PyMol script will be written
 * @author duarte, updated by: Juliane Dinse, Date: 01/03/2007
 * 
 * MyTestPyMol sends commands directly to Pymol.
 *
		 */
		private Start start;
		private Model mod;
		private View view;
		public PrintWriter Out = null;	
		public PyMol mypymol;
	

		public String pdbFileName;
	
		private PaintController pc;
		public int[][] matrix = new int[0][];
		public int[] selrec = new int[4];
		//public String[] mulOb = pc.getMultiSelection();
		
		// constructor
		public MyTestPyMol(Start start, Model mod, View view, PaintController pc, PyMol mypymol){
			this.start=start;
			this.mod = mod;
			this.view=view;
			this.pc=pc;
			this.mypymol=mypymol;
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
			
			if(start == null) System.out.println("Start is null");
			pdbFileName = start.getPDBString();

			//pdbFileName = "/project/StruPPi/jose/tinker/benchmarking/1bxy_A.pdb";

			// to output only to server we would need the following PrintWriter
			Out = new PrintWriter(new PymolServerOutputStream(url),true);
			
			/** Initialising PyMol */
			
			mypymol = new PyMol(Out);		
			mypymol.loadPDB(pdbFileName);
			mypymol.myHide("lines");
			mypymol.myShow("cartoon");
			mypymol.set("dash_gap", 0, "", true);
			mypymol.set("dash_width", 2.5, "", true);
			
		       
		}
			
			// more pymol commands
			
		
		
		public void SquareCommands(){
			int i,j;
			int k = view.getSelNum();
			matrix = mod.getMatrix();	
			if(pc == null) System.out.println("Paint is null");
			selrec = pc.getSelectRect();
	
			int xs = selrec[0]; //starting point: upper left, x-direction
			int ys = selrec[1]; //starting point: upper left, y-direction
			int rw = selrec[2]; //endpoint lower right, x-direction
			int rh = selrec[3]; //endpoint lower right, y-direction
			
			for (i = xs; i<= rw; i++){
				for (j = ys; j<= rh; j++){
					
					if (matrix[i][j] ==1){
						
						int resi1 = i;
						int resi2 = j;
						System.out.println("i: "+ i + " j: "+j);
						
						//inserts an edge between the selected resiues 
						mypymol.setDistance(resi1, resi2,k);
						
					}
					
				}
				
			}
			
			
			Out.println("cmd.hide('labels')");
		}
		
		public void FillCommands(){
			int i,j;
/*
			
			if(pc == null) System.out.println("Paint is null");
			matrix = pc.getSelectMatrix();	
			int [] size = mod.getMatrixSize();
			int dim1 = size[0];
			int dim2 = size[1];
			
			
			for (i = 0; i<= dim1; i++){
				for (j = 0; j<= dim2; j++){
					
					if (matrix[i][j] ==10){
						
						int resi1 = i;
						int resi2 = j;
						//inserts an edge between the selected residues 
						mypymol.setDistance(resi1, resi2);
						
					}
					
				}
				
			}

			Out.println("cmd.hide('labels')");
			*/
			
		}
		
		//public static void main(String[] args) {
			
			
			
//			if (server){
//				OutputStream os1 = new PymolServerOutputStream(url);
//				OutputStream os2 = null;
//				try {
//					os2 = new FileOutputStream(file);
//				} catch (FileNotFoundException e) {
//					e.printStackTrace();
//					System.exit(2);
//				}
//				OutputStream multi = new MultiOutputStream(os1, os2);
//				Out = new PrintWriter(multi, true);
//			} else {
//				try {
//					Out = new PrintWriter(new FileOutput/home/dinse/test.logStream(file),true);
//				} catch (FileNotFoundException e) {
//					e.printStackTrace();
//					System.exit(1);
//				}
//			}
			
			//testPyMol tpm = new testPyMol();
			//tpm.testPyMolInit();
		

		}

//	}



