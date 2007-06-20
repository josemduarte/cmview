package cmview.datasources;


import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Collections;
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
	protected TreeMap<Contact,Double> distMatrix; // the scaled [0-1] distance matrix
	
	/*----------------------------- constructors ----------------------------*/
	
	// no constructors here, see derived classes for implemented constructors
	// In the constructor, the variables pdb and graph have to be initialized
	// and the following private methods have to be called in turn.
	
	/*---------------------------- private methods --------------------------*/
	
	/** Write temporary PDB file with atom lines for the current structure.
	 *  has3DCoordinates() must be true before calling this (i.e. pdb not null) 
	 */
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
	
	/**
	 * To get pdb residue serial from internal residue serial
	 * Can only be called if has3DCoordinates is true
	 * @param resser
	 * @return
	 */
	public String getPdbResSerial(int resser){
		return pdb.get_pdbresser_from_resser(resser);
	}
	
	public HashMap<Contact,Integer> getAllEdgeNbhSizes(){
		return graph.getAllEdgeNbhSizes();
	}
	
	public void delEdge(Contact cont){
		this.graph.delEdge(cont);
	}
	
	/**
	 * Initialises the distMatrix member
	 * Can only be called if has3DCoordinates is true
	 *
	 */
	public void initDistMatrix(){
		TreeMap<Contact,Double> distMatrixAtoms = this.pdb.calculate_dist_matrix(this.getContactType());
		TreeMap<Contact,Double> distMatrixRes = new TreeMap<Contact, Double>();
		for (Contact cont: distMatrixAtoms.keySet()){
			int i_resser = this.pdb.get_resser_from_atomser(cont.i);
			int j_resser = this.pdb.get_resser_from_atomser(cont.j);
			distMatrixRes.put(new Contact(i_resser,j_resser), distMatrixAtoms.get(cont));
		}
		double max = Collections.max(distMatrixRes.values());
		double min = Collections.min(distMatrixRes.values());
		distMatrix = new TreeMap<Contact, Double>();
		for (Contact cont:distMatrixRes.keySet()){
			distMatrix.put(cont, (distMatrixRes.get(cont)-min)/(max-min));
		}
	}
	
	public TreeMap<Contact,Double> getDistMatrix(){
		return distMatrix;
	}
	
	public boolean has3DCoordinates(){
		return (pdb!=null);
	}
	
	public double[][] getDensityMatrix() {
		int size = getMatrixSize();
		double[][] d = new double[size][size]; // density matrix
		
		// initialize matrix with contacts
		for(Contact cont:graph.getContacts()) {
			d[cont.i-1][cont.j-1] = 1;
		}
		
		// fill diagonal - avoids artefacts near main diagonal
		for(int i=0; i < size; i++) {
			d[i][i] = 1;
		}
		
		// starting from second diagonal, fill uppper matrix with contact counts
		// and calculate diagonal averages
		double[] avg = new double[size];
		double[] std = new double[size];
		for(int j = 2; j < size; j++) {
			double sum = 0;
			for(int i = 0; i < size-j; i++) {
				d[i][i+j] = d[i+1][i+j] + d[i][i+j-1] - d[i+1][i+j-1] + d[i][i+j];
				//          down          left          left+down       contact
				sum += d[i][i+j]; // sum of diagonal values
			}
			avg[j] = sum / (size-j); // diagonal average
			
			// standard deviation
			sum = 0;
			for(int i = 0; i < size-j; i++) {
				sum += Math.pow(d[i][i+j] - avg[j],2);
			}
			std[j] = Math.sqrt(sum / (size-j));
			//System.out.println("avg(" + j + ") = " + avg[j]);
			//System.out.println("std(" + j + ") = " + std[j]);

		}
		
		// diagonal average and standard deviation for first off-diagonal
		double sum = 0;
		for(int i = 0; i < size-1; i++) {
			sum = sum + d[i][i+1];
		}
		avg[1] = sum / (size-1);
		sum = 0;
		for(int i = 0; i < size-1; i++) {
			sum += Math.pow(d[i][i+1] - avg[1],2);
		}
		std[1] = Math.sqrt(sum / (size-1));
		
		// calculate z-score density
		for(int i = 0; i < size-1; i++) {
			for(int j = i+1; j < size; j++) {
				if(std[j-i] == 0) {
					d[i][j] = 0;
				} else {
					d[i][j] = (d[i][j] - avg[j-i]) / std[j-i];
				}
			}
		}
		
		// reset diagonal
		for(int i=0; i < size; i++) {
			d[i][i] = 0;
		}
		
		// map values to [0,1]
		double max = 0;
		double min = d[0][0];		
		for(int i = 0; i<size; i++) {
			for(int j = 0; j < size; j++) {
				if(d[i][j] > max) max = d[i][j];
				if(d[i][j] < min) min = d[i][j];				
			}
		}
		max = Math.min(max, 3);  // cut off at 3
		min = Math.max(min, -3); // cut off at -3
		assert(max >= 0);
		assert(min <= 0);
		if(max-min > 0) {
			for(int i = 0; i<size; i++) {
				for(int j = i; j < size; j++) {
					d[i][j] = (d[i][j]-min) / (max-min);
				}
			}			
		}
		return d;
	}
}