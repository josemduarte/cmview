

import java.sql.*;
import javax.swing.*;
import tools.*;

/**
 * 
 * @author:		Juliane Dinse
 * Class: 		Model
 * Package: 	tools
 * Date:		20/02/2007, updated: 01/03/2007
 * 
 * tasks:
 * - getting the data out of the database
 * - initialising size of the contact map
 * - creating the binary contact-map-matrix: 0 - no contact, 1 - contact
 * - if selected pdb structure contains unobserved residues: Pop-up-window with warning message
 * 
 */

public class Model  {

	public int[] pubmsize = new int[2]; // public array for matrix size 
	public int[][] pubmatrix = MatrixInitialiser(); // public array for adjacency matrix

	private Start start;
	private JFrame f;
	public int a,b;
	
	//constructor
	public Model(Start start){
		this.start = start;
	}
	
	public void ModelInit(){
		
	/** SQL preparation */
		
    Statement  st = null;
    ResultSet  rs = null; 	// getting the data for the size of the Contact Map
    ResultSet  rss = null;	// getting the data of the contacts

    try {
    
		/** SQL-String takes the data out of the DB */
		String sql = start.getSQLString();
		String ac = start.getAccessionCode();
		String ct = start.getSelectedCT();
		String size = "select min(num), max(num),count(num), max(num)-min(num)+1"
						+ " from single_model_node, single_model_graph where single_model_graph.accession_code = '" +ac
						+ "' and single_model_graph.CT = '" + ct +"' and single_model_graph.graph_id = single_model_node.graph_id;";
		
		/** Database Connection */
		MySQLConnection con = new MySQLConnection(Start.DB_HOST,Start.DB_USER,Start.DB_PWD,Start.GRAPH_DB);
		st = con.createStatement();
		
		rs = st.executeQuery(size);
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
		
		
		rss = st.executeQuery(sql);
		// initialising an empty matrix
		pubmatrix = MatrixInitialiser();
		while (rss.next()){
			int a,b;
			/** Insert the contacts */
			a = rss.getInt(1); // 1st column
			b = rss.getInt(2); // 2nd column

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
