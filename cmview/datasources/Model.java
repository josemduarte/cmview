package cmview.datasources;


import java.io.IOException;
import proteinstructure.Graph;
import proteinstructure.Pdb;

/** 
 * A contact map data model. Derived classes have to implement the constructor in which
 * the structure is loaded, the member variables are set appropriately and a temporary
 * pdb file with the atom lines is written.
 * 
 * @author		Henning Stehr
 * Class: 		Model
 * Package: 	cm2pymol
 * Date:		15/05/2007, last updated: 15/05/2007
 * 
 */
public abstract class Model {

	public static final String		TEMP_PATH =			"/scratch/local/"; // for temp pdb files
	
	/*--------------------------- member variables --------------------------*/
		
	// structure and contact map data
	protected Pdb pdb;
	protected Graph graph;
	protected int[][] dataMatrix;
	protected int matrixSize;
	protected int unobservedResidues;
	protected int seqSep = -1; // store this here because the graph object doesn't have it yet
	
	/*----------------------------- constructors ----------------------------*/
	
	// no constructors here, see derived classes for implemented constructors
	// In the constructor, the variables pdb and graph have to be initialized
	// and the following private methods have to be called in turn.
	
	/*---------------------------- private methods --------------------------*/
	
	/** Write temporary PDB file with atom lines for the current structure */
	protected void writeTempPdbFile() {
		try {
			pdb.dump2pdbfile(getTempPDBFileName());
		} catch (IOException e) {
			System.err.println("Error writing temporary PDB file");
		}
	}

	/**
	 * Initalizes the member variables cm, dataMatrix, matrixSize and unobservedResidues.
	 * Variable graph has to be initialized previously. Otherwise a null pointer
	 * exception will be thrown.
	 */
	protected void initializeContactMap() {
		dataMatrix = graph.getIntMatrix();
		matrixSize = graph.fullLength;
		unobservedResidues = (graph.fullLength - graph.obsLength);
	 }	
	
	/** 
	 * Filter out unwanted contacts and initializes the seqSep variable. 
	 * Note: this causes trouble for directed graphs 
	 */
	protected void filterContacts(int seqSep) {
		for(int i=0; i < matrixSize; i++) {
			for(int j=0; j < matrixSize; j++) {
				if(Math.abs(j-i) < seqSep) dataMatrix[i][j] = 0;
			}
		}
		this.seqSep = seqSep;
		
		// hack: transpose matrix until displaying is fixed
	    for (int row = 0; row < matrixSize; row++)
        {
            for (int col = 0; col < row; col++)
            {
                int temp = dataMatrix[row][col];
                dataMatrix[row][col] = dataMatrix[col][row];
                dataMatrix[col][row] = temp;
            }
        }
	    
	}
	
	/** Print some warnings if necessary */
	protected void printWarnings(String oldChainCode) {
		if(unobservedResidues > 0) 
			System.out.println("Warning: " + unobservedResidues + " unobserved or non-standard residues");
		if(!oldChainCode.equals(getChainCode())) System.out.println("Warning: Chain " + oldChainCode + " is now called " + getChainCode());
	}
	
	/*---------------------------- public methods ---------------------------*/
	
	/** Returns the size of the data matrix */
	public int getMatrixSize() {
		return this.matrixSize;
	}
	/** Returns the data matrix */
	public int[][] getMatrix(){
		return this.dataMatrix;
	}
	
	public int getNumberOfContacts() {
//		int num = 0;
//		for(int i = 0; i < getMatrixSize(); i++) {
//			for(int j = 0; j < getMatrixSize(); j++) {
//				if(this.dataMatrix[i][j] > 0) num++;
//			}
//		}
//		return num;
		return graph.numContacts;
	}
	
	public boolean isDirected() {
		return graph.directed;
	}

	/** Returns the pdb code of the underlying structure */
	public String getPDBCode() {
		return graph.accode;
	}

	/** Returns the chain code of the underlying structure */
	public String getChainCode() {
		return graph.chain; // gets the internal chain code (may be != pdb chain code)
	}
	
	public String getContactType() {
		return graph.ct;
	}
	
	public double getDistanceCutoff() {
		return graph.cutoff;
	}
	
	public int getSequenceSeparation() {
		return this.seqSep;
	}

	/** 
	 * Returns the sequence for the current structure or an empty string
	 * if no sequence information is available.
	 */
	public String getSequence() {
		return graph.sequence;
	}
	
	/** Returns the name of the temporary pdb file */
	public String getTempPDBFileName(){
		String pdbFileName;
		pdbFileName  = TEMP_PATH + getPDBCode() + ".pdb";
		return pdbFileName;
	}

	/** 
	 * Returns the number of unsoberved or non-standard residues
	 * in the structure. The contacts of these residues are ignored. */
	public int getNumberOfUnobservedResidues() {
		return unobservedResidues;
	}
	
	/** Write the current contact map to a contact map file */
	public void writeToContactMapFile(String fileName) throws IOException {
		try {
			this.graph.write_graph_to_file(fileName);
		} catch (IOException e) {
			System.err.println("Error when trying to write contact map file");
			throw e;
		}
	}	
	
}