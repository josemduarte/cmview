package cmview.datasources;
import cmview.Start;

import java.io.IOException;
import java.sql.SQLException;

import owl.core.structure.*;
import owl.core.structure.graphs.FileRIGraph;
import owl.core.util.FileFormatException;

/** 
 * A contact map data model based on a graph loaded from a Contact map file.
 */
public class ContactMapFileModel extends Model {
	
	/**
	 * Overloaded constructor to load the data from file.
	 * @throws ModelConstructionError 
	 */
	public ContactMapFileModel(String fileName) throws ModelConstructionError {
			
		// load Contact graph from file
		try {
			
			this.graph = new FileRIGraph(fileName);
			this.isGraphWeighted = graph.hasWeightedEdges();
			this.secondaryStructure = graph.getSecondaryStructure(); // take ss from graph
			
			String pdbCode = graph.getPdbCode();
			String pdbChainCode = graph.getPdbChainCode();
			int modelSerial = graph.getModel();
			
			// check whether sequence info exists
			if(graph.getSequence().equals("")) { //TODO this shouldn't happen since we don't allow blank sequences in CM files, but we keep as another check doesn't harm
				throw new ModelConstructionError("File contains no sequence information.");
			}
			
			// assign a loadedGraphId to this model
			String name = this.graph.getPdbCode()+this.graph.getChainCode();
			if (this.graph.getPdbCode().equals(Pdb.NO_PDB_CODE)) {
				name = DEFAULT_LOADEDGRAPHID;
			} 
			this.loadedGraphID = Start.setLoadedGraphID(name, this);
			
			// load structure from pdbase/online if possible
			if(!pdbCode.equals(Pdb.NO_PDB_CODE) && !pdbChainCode.equals(Pdb.NO_PDB_CHAIN_CODE)) {
				if (Start.isDatabaseConnectionAvailable()) {
					try {
						this.pdb = new PdbasePdb(pdbCode, Start.DEFAULT_PDB_DB, Start.getDbConnection()); // by default loading from pdbase
						this.pdb.load(pdbChainCode,modelSerial);
						super.writeTempPdbFile(); // this doesn't make sense without a pdb object
					} catch (PdbCodeNotFoundException e) {
						System.err.println("Failed to load structure because accession code was not found in Pdbase");
						pdb = null;
					} catch (PdbLoadException e) {
						System.err.println("Failed to load structure:" + e.getMessage());
						pdb = null;
					} catch(SQLException e) {
						System.err.println("Failed to load structure because of database error");
						pdb = null;
					} 
					// if pdb creation failed then pdb=null
				} else { // we try to load from online cif file
					try {
						this.pdb = new CiffilePdb(pdbCode);
						this.pdb.load(pdbChainCode,modelSerial);
						super.writeTempPdbFile(); // this doesn't make sense without a pdb object
					} catch (PdbLoadException e) {
						System.err.println("Failed to load structure:" + e.getMessage());
						pdb = null;
					} catch(IOException e) {
						System.err.println("Failed to load structure because of error while downloading/reading the CIF file: "+e.getMessage());
						pdb = null;
					} 
					// if pdb creation failed then pdb=null					
				}
				
				
				// if structure is available, and has secondary structure annotation, use it
				if(this.pdb != null && pdb.getSecondaryStructure() != null) {
					this.secondaryStructure = pdb.getSecondaryStructure(); 
				}				
			} else {
				System.out.println("No pdb code and/or chain code found. Can not load structure.");
			} 
			
			//super.filterContacts(seqSep);	// currently not allowed to filter contacts
			//super.printWarnings(chainCode); // doesn't make sense here
			super.checkAndAssignSecondaryStructure();
			
		} catch (IOException e) {
			System.err.println("Error while trying to load graph from contact map file.");
			throw new ModelConstructionError(e.getMessage());
		} catch (FileFormatException e){
			System.err.println("Error while trying to load graph from contact map file. Wrong graph file format.");
			throw new ModelConstructionError(e.getMessage());			
		}
		

	}
	
	public ContactMapFileModel(Model mod) {
	    super(mod);
	}
	
	public ContactMapFileModel copy() {
	    return new ContactMapFileModel(this);
	}

	/**
	 * The loading of the contact map is implemented in the constructor not in 
	 * this function. This function essentially does'nt do anything!
	 * @param pdbChainCode pdb chain code of the chain to be loaded (ignored!)
	 * @param modelSerial  a model serial
	 * @throws ModelConstructionError
	 */
	@Override
	public void load(String pdbChainCode, int modelSerial) throws ModelConstructionError {
		return;
	}
	
}
