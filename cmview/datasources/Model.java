package cmview.datasources;

import java.io.IOException;
import java.io.File;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;

import cmview.Start;
import proteinstructure.*;

/** 
 * A contact map data model. Derived classes have to implement the constructor in which
 * the structure is loaded, the member variables are set appropriately and a temporary
 * pdb file with the atom lines is written. All other methods should be implemented here.
 */
public abstract class Model {
	
	/*--------------------------- member variables --------------------------*/
		
	// structure and contact map data
	protected Pdb pdb;								// this can be null if there are no 3D coordinates available
	protected Graph graph;							// currently every model must have a valid graph object
	protected int matrixSize;						// number of residues (in known sequence)
	protected int unobservedResidues;				// number of unobserved or non-standard AAs
	protected int minSeqSep = -1; 				  	// -1 meaning not set
	protected int maxSeqSep = -1; 					// store this here because the graph object doesn't have it yet
	protected HashMap<Edge,Double> distMatrix; 		// the scaled [0-1] distance matrix
	// The following two variables are pointers to the respective objects in pdb or graph. The only purpose of having them here is to allow
	// us to take the seondary structure either from the graph or from the pdb object. A cleaner way would be to have both of them implement
	// an interface hasSecondaryStructure and then just specify a secondaryStructureSource variable of that interface type.
	private SecondaryStructure secondaryStructure;  // pointer to secondary structure object from either pdb or graph object
	private File tempPdbFile;						// the file with the atomic coordinates to be loaded into pymol
	
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
			pdb.dump2pdbfile(getTempPdbFileName());
		} catch (IOException e) {
			System.err.println("Error writing temporary PDB file");
		}
	}

	/**
	 * Initalizes the member variables matrixSize, unobservedResidues and secondaryStructure
	 * Variable graph has to be initialized previously. Otherwise a null pointer
	 * exception will occur.
	 */
	protected void initializeContactMap() {
		matrixSize = graph.getFullLength();
		unobservedResidues = (graph.getFullLength() - graph.getObsLength());
		if(pdb != null && pdb.hasSecondaryStructure()) {
			secondaryStructure = pdb.getSecondaryStructure();
		}
		else if(graph != null && graph.hasSecondaryStructure()) {
			secondaryStructure = graph.getSecondaryStructure();
		}
		else {
			secondaryStructure = new SecondaryStructure();
		}
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
	
	/** Check whether secondary structure needs to be assigned and run dssp if so */
	protected void checkAndAssignSecondaryStructure() {
		if(has3DCoordinates()) {		// otherwise we can't (re)assign secondary structure
			if(Start.FORCE_DSSP || !pdb.hasSecondaryStructure()) {
				if(!Start.isDsspAvailable()) {
					System.err.println("Can not (re)assign secondary structure because external DSSP is not available");
				} else {
					System.out.println("(Re)assigning secondary structure using DSSP");
					try {
						pdb.runDssp(Start.DSSP_EXECUTABLE, Start.DSSP_PARAMETERS);
						// update secondary structure pointer in model
						secondaryStructure = pdb.getSecondaryStructure();
					} catch (IOException e) {
						System.out.println("Failed to assign secondary structure: " + e.getMessage());
					}
				}
			}
		}
	}
	
	/*---------------------------- public methods ---------------------------*/
	
	/** Returns the size of the data matrix */
	public int getMatrixSize() {
		return this.matrixSize;
	}
	
	/** Returns the contacts as an EdgeSet */
	public EdgeSet getContacts(){
		return this.graph.contacts; // this re-references graph's contacts, no deep copy
	}
	
	/** Returns the graph object */
	public Graph getGraph(){
		return this.graph;
	}
	
	/** Returns the number of contacts */
	public int getNumberOfContacts() {
		return graph.getNumContacts();
	}
	
	/** Returns true if the graph is directed, false otherwise */
	public boolean isDirected() {
		return graph.isDirected();
	}

	/** Returns the pdb code of the underlying structure */
	public String getPDBCode() {
		return graph.getPdbCode();
	}

	/** 
	 * Returns the internal chain code of the underlying structure.
	 * Note that the internal chain code may be different from the pdb chain code given when loading the structure.
	 * For a database model, the internal chain code is the one given by the database (Pdbase or MSD). When loading
	 * from pdb file, the internal chain code is the same as the pdb chain code or 'A' if the pdb chain code is empty.
	 */
	public String getChainCode() {
		return graph.getChainCode(); // gets the internal chain code (may be != pdb chain code)
	}
	
	/**
	 * Returns the contact type 
	 */
	public String getContactType() {
		return graph.getContactType();
	}
	
	/**
	 * Returns the distance cutoff
	 */
	public double getDistanceCutoff() {
		return graph.getCutoff();
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
		return graph.getSequence();
	}
	
	/** 
	 * Returns the temporary pdb-style file with atomic coordinates.
	 * When called for the first time, this method creates the File object and marks it to be deleted on exit. */
	protected File getTempPdbFile() {
		if(tempPdbFile == null) {
			tempPdbFile = new File(Start.TEMP_DIR, getPDBCode() + getChainCode() + ".pdb"); // TODO: Make sure that getChainCode() never returns " "
			tempPdbFile.deleteOnExit(); // will delete the file when the VM is closed
		}
		return tempPdbFile;
	}
	
	/** Returns the name of the temporary pdb file */
	public String getTempPdbFileName(){
		return getTempPdbFile().getAbsolutePath();
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
	
	/** Write the current contact map to a graph database */
	public void writeToGraphDb(String dbName) throws SQLException {
		graph.write_graph_to_db(Start.getDbConnection(), dbName);
	}
	
	/**
	 * Returns the three letter residue type for the given residue serial.
	 * This may only be called if there is sequence information in the graph object, i.e. hasSequence()==true
	 * @param resser
	 * @return A string with the three letter residue type of the residue with serial resser
	 */
	public String getResType(int resser){
		return graph.getResType(resser);
	}
	
	/**
	 * Returns the one letter residue type for the given residue serial.
	 * This may only be called if there is sequence information in the graph object, i.e. hasSequence()==true
	 * @param resser
	 * @return A string with the one letter residue type of the residue with serial resser
	 */	
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
	 * May only be called if has3DCoordinates is true
	 * @param resser
	 * @return
	 */
	public String getPdbResSerial(int resser){
		return pdb.get_pdbresser_from_resser(resser);
	}
			
	public HashMap<Edge,Integer> getAllEdgeNbhSizes(){
		return graph.getAllEdgeNbhSizes();
	}
	
	public void delEdge(Edge cont){
		this.graph.delEdge(cont);
	}
	
	/**
	 * Initialises the distMatrix member. The matrix is scaled to doubles from zero to one.
	 * Returns the scaled value of the current distance cutoff.
	 * May only be called if has3DCoordinates is true.
	 */
	public double initDistMatrix(){
		HashMap<Edge,Double> distMatrixRes = this.pdb.calculate_dist_matrix(Start.DIST_MAP_CONTACT_TYPE);
		double max = Collections.max(distMatrixRes.values());
		double min = Collections.min(distMatrixRes.values());
		distMatrix = new HashMap<Edge, Double>();	// TODO: Use old matrix to save memory
		for (Edge cont:distMatrixRes.keySet()){
			distMatrix.put(cont, (distMatrixRes.get(cont)-min)/(max-min));
		}
		double dist = (graph.getCutoff()-min)/(max-min);
		return dist;
	}
	
	/**
	 * Returns the current distance matrix. Before initDistMatrix has been called this will be null.
	 * @return A map assigning to each edge the corresponding distance (scaled to [0;1]).
	 */
	public HashMap<Edge,Double> getDistMatrix(){
		return distMatrix;
	}
	
	/**
	 * Returns the difference of the distance maps of this model and the given second model. All distances are
	 * based on C-alpha atoms. 
	 * @param secondModel the second model to compare agains
	 * @return A map assigning to each edge the corresponding value in the difference distance matrix or null on error.
	 */
	public HashMap<Edge,Double> getDiffDistMatrix(Model secondModel) {
		/* TODO: Also force c-alpha for simple distance maps? Throw proper exceptions instead of returning null? Use real matrix? */
		double diff, min, max;
		if(!this.has3DCoordinates() || !secondModel.has3DCoordinates()) {
			System.err.println("Failed to compute difference distance map. No 3D coordinates.");
			return null; // distance doesn't make sense without 3D data	
		}
		if(this.getMatrixSize() != secondModel.getMatrixSize()) {
			System.err.println("Failed to compute difference distance map. Matrix sizes do not match.");
			return null; // can only calculate matrix difference if sizes match TODO: use alignment
		}
		
		HashMap<Edge,Double> diffDistMatrix = this.pdb.getDiffDistMap(Start.DIST_MAP_CONTACT_TYPE, secondModel.pdb, Start.DIST_MAP_CONTACT_TYPE);
		
		if(diffDistMatrix == null) {
			System.err.println("Failed to compute difference distance map.");
		} else {
			max = Collections.max(diffDistMatrix.values());
			min = Collections.min(diffDistMatrix.values());
			if(max == min) System.err.println("Failed to scale difference distance matrix. Matrix is empty or all matrix entries are the same.");
			else {
				// scale matrix to [0;1]
				for(Edge e:diffDistMatrix.keySet()) {
					diff = diffDistMatrix.get(e); 
					diffDistMatrix.put(e, (diff-min)/(max-min));
				}
			}
		}
		return diffDistMatrix;
	}
	
	/**
	 * Returns true if this model contains 3D coordinates. Certain other methods can only be used
	 * if 3D coordinates are available.
	 * @return true if this model has 3D coordinates, false otherwise
	 */
	public boolean has3DCoordinates(){
		return (pdb!=null);
	}
	
	/**
	 * Returns true iff there is no sequence information whatsoever
	 * @return true if sequence information is availbale, false otherwise
	 */
	public boolean hasSequence() {
		return graph.getSequence().equals("");
	}
	
	// secondary structure related methods
	
	/** 
	 * Returns true iff this model has secondary structure information.
	 * @return true if secondary structure information is available, false otherwise
	 */
	public boolean hasSecondaryStructure() {
		return !secondaryStructure.isEmpty();
	}
	
	/** 
	 * Returns the secondary structure annotation object of this model. Currently, this can come either from a
	 * graph or from a pdb object.
	 * @return the secondary structure annotation object 
	 */
	public SecondaryStructure getSecondaryStructure() {
		return this.secondaryStructure;
	}

	// end of secondary structure related methods
	
	/**
	 * Calculates and returns the density matrix for the current model.
	 */
	public double[][] getDensityMatrix() {
		int size = getMatrixSize();
		double[][] d = new double[size][size]; // density matrix
		
		// initialize matrix with contacts
		for(Edge cont:graph.getContacts()) {
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