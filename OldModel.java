


import java.sql.*;
import javax.swing.*;import tools.Msdsd2Pdb;

import tools.*;

/** 
 * A contact map data model based on a graph database (single_model_graphs derived from MSD).
 * This is the model that was used in previous versions of CM2Pymol (hence the "old").
 * 
 * @author		Juliane Dinse
 * Class: 		Model
 * Package: 	cm2pymol
 * Date:		20/02/2007, last updated: 10/05/2007
 * 
 */
public class OldModel extends Model {

	/* constants */
	
	/* member variables */
	
	private int pubmsize = 0; // size of data matrix 
	private int[][] pubmatrix = null; // public array for adjacency matrix

	private String pdbCode = null;			// pdb accession code
	private String chainCode = null;		// pdb chain code
	private String edgeType = null;			// contact type (BB, SC, ...)
	private double distCutoff = 0;			// contact distance cutoff
	private int seqSep = 0;					// minimum sequence separation
	private MySQLConnection conn = null;
	private String db;						// the database to read from
	private int unobservedResidues;		// are there any in this structure?
	
	private JFrame f;
	
	/* Create a new model object */
	public OldModel(String pdbCode, String chainCode, String edgeType, double distCutoff, int seqSep, MySQLConnection conn, String db) {
		this.pdbCode = pdbCode;
		this.chainCode = chainCode;
		this.edgeType = edgeType;
		this.distCutoff = distCutoff;
		this.seqSep = 0;
		this.conn = conn;
		this.db = db;
		conn.setDbname(this.db);
		
		this.ModelInit();
	}
	
	/** Return the graphId of the single_model_graph underlying this model */
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

	/** Initialises an empty matrix of the size of the protein sequence */
	private int[][] MatrixInitialiser(){
		int msize = getMatrixSize();
		int m = msize+1;
		int n = msize+1;
		
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
	
	private void ModelInit() {
	
	    int graphId = this.getSingleModelGraphId();
	    int a = 0, b = 0;
	    
		// write temp pdb file
		try{
			Msdsd2Pdb.export2File(this.pdbCode, this.chainCode, this.getTempPDBFileName(), Start.DB_USER);
		} catch (Exception ex){
			System.err.println("Error: Couldn't export PDB file from MSD");
			System.out.println(ex);
		}	
	    
		// load contact map from database
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
				pubmsize = rs.getInt(2); // numbers of rows of the contact map
			}
			
			if(a==b){
				//everything is fine
			}
			else{
				unobservedResidues = b-a;
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

	/* (non-Javadoc)
	 * @see Model#getMatrixSize()
	 */
	public int getMatrixSize(){
		return pubmsize;
	}
	
	/* (non-Javadoc)
	 * @see Model#getMatrix()
	 */
	public int[][] getMatrix(){
		return pubmatrix;
	}
	
	/* (non-Javadoc)
	 * @see Model#getPDBCode()
	 */
	public String getPDBCode() {
		return this.pdbCode;
	}
	
	/* (non-Javadoc)
	 * @see Model#getChainCode()
	 */
	public String getChainCode() {
		return this.chainCode;
	}
	
	public String getContactType() {
		return this.edgeType;
	}
	
	public double getDistanceCutoff() {
		return this.distCutoff;
	}
	
	public int getSequenceSeparation() {
		return this.seqSep;
	}
	
	public String getSequence() {
		return "";
	}
	
	public boolean isDirected() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see Model#getTempPDBFileName()
	 */
	public String getTempPDBFileName(){
		String pdbFileName;
		pdbFileName  = Start.TEMP_PATH + this.pdbCode + ".pdb";
		return pdbFileName;
	}
	
	public int getNumberOfUnobservedResidues() {
		return unobservedResidues;
	}
	
	public int getNumberOfContacts() {
		int num = 0;
		for(int i = 0; i < pubmsize; i++) {
			for(int j = 0; j < pubmsize; j++) {
				if(this.pubmatrix[i][j] > 0) num++;
			}
		}
		return num;
	}
	
	/* (non-Javadoc)
	 * @see Model#writeToContactMapFile()
	 */	
	public void writeToContactMapFile(String fileName) {
		System.err.println("writing to contact map file not implemented in this model");
	}

}
