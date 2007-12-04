package cmview.datasources;
import proteinstructure.*;
import cmview.Start;

import java.io.IOException;
import java.sql.SQLException;

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
			
			String pdbCode = graph.getPdbCode();
			String pdbChainCode = graph.getPdbChainCode();
			int modelSerial = graph.getModel();
			
			// check whether sequence info exists
			if(graph.getSequence().equals("")) {
				System.err.println("File contains no sequence information. Many features will be unavailable.");
			}
			
			// load structure from pdbase if possible
			if(!pdbCode.equals("") && !pdbChainCode.equals("")) {
				if (!Start.isDatabaseConnectionAvailable()) {
					System.err.println("No database connection. Can't load structure.");					
				} else {
					try {
						this.pdb = new PdbasePdb(pdbCode, Start.DEFAULT_PDB_DB, Start.getDbConnection()); // by default loading from pdbase
						this.pdb.load(pdbChainCode,modelSerial);
						super.writeTempPdbFile(); // this doesn't make sense without a pdb object
					} catch (PdbCodeNotFoundError e) {
						System.err.println("Failed to load structure because accession code was not found in Pdbase");
						pdb = null;
					} catch (PdbLoadError e) {
						System.err.println("Failed to load structure:" + e.getMessage());
						pdb = null;
					} catch(SQLException e) {
						System.err.println("Failed to load structure because of database error");
						pdb = null;
					} 
					// if pdb creation failed then pdb=null
				}
				
			} else {
				System.out.println("No pdb code and/or chain code found. Can not load structure.");
			}
			
			super.initializeContactMap();
			//super.filterContacts(seqSep);	// currently not allowed to filter contacts
			//super.printWarnings(chainCode); // doesn't make sense here
			super.checkAndAssignSecondaryStructure();
			
		} catch (IOException e) {
			System.err.println("Error while trying to load graph from contact map file.");
			throw new ModelConstructionError(e.getMessage());
		} catch (GraphFileFormatError e){
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
