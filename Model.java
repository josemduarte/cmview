

import java.sql.*;
import javax.swing.*;
import tools.*;

/**
 * 
 * @author		Juliane Dinse
 * Class: 		Model
 * Package: 	cm2pymol
 * Date:		20/02/2007, last updated: 10/05/2007
 * 
 */

public class Model  {

	/* constants */
	
	/* member variables */
	
	public int[] pubmsize = new int[2]; // public array for matrix size 
	public int[][] pubmatrix = MatrixInitialiser(); // public array for adjacency matrix

	public String pdbCode = null;		// pdb accession code
	public String chainCode = null;		// pdb chain code
	private String edgeType = null;		// contact type (BB, SC, ...)
	private float distCutoff = 0;		// contact distance cutoff
	private float seqSep = 0;			// minimum sequence separation
	
	private MySQLConnection conn = null;
	
	private JFrame f;
	public int a,b;
	
	//constructor
	public Model(String pdbCode, String chainCode, String edgeType, float distCutoff, int seqSep, MySQLConnection conn) {
		this.pdbCode = pdbCode;
		this.chainCode = chainCode;
		this.edgeType = edgeType;
		this.distCutoff = 4.1f;
		this.seqSep = 0;
		this.conn = conn;
	}
	
	// Return the graphId of the single model graph underlying this model
	private int getSingleModelGraphId() {
		String query;
		int chainGraphId;
		int smGraphId;
		// get chain graph id
		query = "SELECT graph_id FROM chain_graph "
			  + "WHERE accession_code = '" + this.pdbCode + "' "
			  + "AND chain_pdb_code " + (chainCode.equals(Start.NULL_CHAIN_CODE)?"is null":("= '" + chainCode + "'")) + " "
			  + "AND dist = " + this.distCutoff + ";";
		chainGraphId = this.conn.getIntFromDb(query);
		// get single model graph id
		query = "SELECT graph_id FROM single_model_graph "
			  + "WHERE pgraph_id = " + chainGraphId + " "
		      + "AND graph_type = 'chain' "
		      + "AND dist = " + this.distCutoff + " "
		      + "AND CT = '" + edgeType + "'";
		smGraphId = this.conn.getIntFromDb(query);
		return smGraphId;
	}
	
	public void ModelInit() {
	
	    int graphId = this.getSingleModelGraphId();
	    
	    try {
	    	
	    	/** SQL preparation */		
	        Statement  st = null;
	        ResultSet  rs = null;
	        String	   query = null;
			
	        st = conn.createStatement();    	
		
			// count number of nodes
			query = "SELECT min(num), max(num),count(num), max(num)-min(num)+1 "
				  + "FROM single_model_node "
				  + "WHERE graph_id = " + graphId;
					
			rs = st.executeQuery(query);
			
			while (rs.next()){
				a = rs.getInt(3);
				b = rs.getInt(4);
				
				/**** Hier */ 
				int m = rs.getInt(2); // numbers of rows of the contact map
				int n = rs.getInt(2); // numbers of columns of the contact map
				int[] msiz = {m,n};
				pubmsize = msiz;
			}
			
			if(a==b){
				//everything is fine
			}
			else{
	
				// warning pop-up if unobserved residues occur
				JOptionPane.showMessageDialog(f,
				    "Be careful: some unobserved residues!",
				    "Unobserved Residue Warning",
				    JOptionPane.WARNING_MESSAGE);
	
			}
			rs.close();
						
			// initialising an empty matrix
			pubmatrix = MatrixInitialiser();
			
			// fill the matrix with contacts
			query = "SELECT i_num, j_num "
				  + "FROM single_model_edge "
				  + "WHERE graph_id = " + graphId + " "
				  + "AND i_num > j_num "
				  + "AND abs(i_num - j_num) >= " + seqSep + " "
				  + "ORDER BY i_num, j_num";
			
			rs = st.executeQuery(query);			
			
			while (rs.next()){
				int a,b;
				/** Insert the contacts */
				a = rs.getInt(1); // 1st column
				b = rs.getInt(2); // 2nd column
	
				pubmatrix[a][b]=1;
	
				}
		}
		catch ( Exception ex ) {
	        System.out.println( ex );
	        
		}

    }

	/** Returns matrix dimension */
	public int[] getMatrixSize(){
		return pubmsize;
	}
	
	
	/** Returns the matrix */
	public int[][] getMatrix(){
		return pubmatrix;
	}
	
	
	/** Initialises an empty matrix of the size of the protein sequence */
	public int[][] MatrixInitialiser(){
		int [] msize = getMatrixSize();
		int m = msize[0]+1;
		int n = msize[1]+1;
		
		int[][] matrix = new int[m][];
		
		for (int x=0; x< m; x++){
			/** create rows */ 
			matrix[x]= new int[n];
			
			for (int y=0; y< n; y++){
			 /** create columns */
				matrix [x][y]=0;
			
			}
		}
		return matrix;
	}

}
