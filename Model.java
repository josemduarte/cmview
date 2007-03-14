

import java.sql.*;

import tools.*;

/**
 * 
 * @author:		Juliane Dinse
 * Class: 		Model
 * Package: 	tools
 * Date:		20/02/2007, updated: 01/03/2007
 * 
 * Model ONLY handles the DB-data and prepares them in right order to process them 
 * in different other methods. 
 * Its independent from all other classes which represent data for user-interaction.
 *
 */

public class Model {

	public int[] pubmsize = new int[2]; // public array for matrix size 
	public int[][] pubmatrix = MatrixInitialiser(); // public array for adjacency matrix

	private Start start;
	
	//constructor
	public Model(Start start){
		this.start = start;
	}
	
	public void ModelInit(){
		
	/** SQL preparation */
		
	String user ="dinse";
    Statement  st = null;
    ResultSet  rs = null; 	// getting the data for the size of the Contact Map
    ResultSet  rss = null;	// getting the data of the contacts

    try {
    
		/** SQL-String takes the data out of the DB */
		String sql = start.getSQLString();
		System.out.println(sql);
		
		/** Database Connection */
		MySQLConnection con = new MySQLConnection("white",user,"nieve","pdb_reps_graph");
		st = con.createStatement();
		
		rs = st.executeQuery(sql);
		while (rs.next()){
			int m = rs.getInt(3); // numbers of rows of the contact map
			int n = rs.getInt(3); // numbers of columns of the contact map
			int[] msiz = {m,n};
			pubmsize = msiz;
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
