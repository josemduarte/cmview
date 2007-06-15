package cmview.datasources;


import java.io.IOException;
import java.util.HashMap;
import proteinstructure.*;

/** 
 * A contact map data model. Derived classes have to implement the constructor in which
 * the structure is loaded, the member variables are set appropriately and a temporary
 * pdb file with the atom lines is written.
 * 
 * @author		Henning Stehr
 * Class: 		Model
 * Package: 	cmview.datasources
 * Date:		15/05/2007, last updated: 12/06/2007
 * 
 */
public abstract class Model {

	public static final String		TEMP_PATH =			"/scratch/local/"; // for temp pdb files
	
	/*--------------------------- member variables --------------------------*/
		
	// structure and contact map data
	protected Pdb pdb;
	protected Graph graph;
	protected int matrixSize;
	protected int unobservedResidues;
	protected int minSeqSep = -1; // -1 meaning not yet set
	protected int maxSeqSep = -1; // store this here because the graph object doesn't have it yet	
	
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
		matrixSize = graph.fullLength;
		unobservedResidues = (graph.fullLength - graph.obsLength);
	 }	
	
	/** 
	 * Filter out unwanted contacts and initializes the seqSep variable. 
	 * Note: this causes trouble for directed graphs 
	 */
	protected void filterContacts(int minSeqSep, int maxSeqSep) {
		if(minSeqSep > 0) {
			this.graph.restrictContactsToMinRange(minSeqSep);
		}
		if(maxSeqSep > 0) {
			this.graph.restrictContactsToMaxRange(maxSeqSep);
		}
		this.minSeqSep = minSeqSep; // remember values for later (info screen)
		this.maxSeqSep = maxSeqSep; 
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
	
	/** Returns the contacts as a ContactList */
	public ContactList getContacts(){
		return this.graph.contacts; // this re-references graph's ContactList, no deep copy
	}
	
	/** Returns the graph object */
	public Graph getGraph(){
		return this.graph;
	}
	
	/** Returns the number of contacts */
	public int getNumberOfContacts() {
		return graph.numContacts;
	}
	
	/** Returns true if the graph is directed, false otherwise */
	public boolean isDirected() {
		return graph.directed;
	}

	/** Returns the pdb code of the underlying structure */
	public String getPDBCode() {
		return graph.accode;
	}

	/** 
	 * Returns the internal chain code of the underlying structure.
	 * Note that the internal chain code may be different from the pdb chain code given when loading the structure.
	 */
	public String getChainCode() {
		return graph.chain; // gets the internal chain code (may be != pdb chain code)
	}
	
	/**
	 * Returns the contact type 
	 */
	public String getContactType() {
		return graph.ct;
	}
	
	/**
	 * Returns the distance cutoff
	 */
	public double getDistanceCutoff() {
		return graph.cutoff;
	}
	
	/** Returns the sequence separation of the current graph */
	public int getMinSequenceSeparation() {
		return this.minSeqSep;
	}
	
	/** Returns the sequence separation of the current graph */
	public int getMaxSequenceSeparation() {
		return this.maxSeqSep;
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
	
	public String getResType(int resser){
		return graph.getResType(resser);
	}
	
	public String getResType1Letter(int resser){
		return AA.threeletter2oneletter(graph.getResType(resser));
	}	
	
	public NodeNbh getNodeNbh(int i_resser){
		return graph.getNodeNbh(i_resser);
	}
	
	public EdgeNbh getEdgeNbh(int i_resser, int j_resser){
		return graph.getEdgeNbh(i_resser, j_resser);
	}
	
	public String getPdbResSerial(int resser){
		return pdb.get_pdbresser_from_resser(resser);
	}
	
	public HashMap<Contact,Integer> getAllEdgeNbhSizes(){
		return graph.getAllEdgeNbhSizes();
	}
}