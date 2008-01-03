package cmview.datasources;
import proteinstructure.*;

import java.io.IOException;

/** 
 * A contact map data model based on a graph loaded from a CASP RR file.
 */
public class CaspRRFileModel extends Model {
	
	/**
	 * Overloaded constructor to load the data from file.
	 * @throws ModelConstructionError 
	 */
	public CaspRRFileModel(String fileName) throws ModelConstructionError {
			
		// load graph from the CASP RR file
		try {
			
			this.graph = new CaspRRFileRIGraph(fileName);
			
			// check whether sequence info exists
			if(graph.getSequence().equals("")) {
				System.err.println("File contains no sequence information. Many features will be unavailable.");
			}
						
			
			super.initializeContactMap();
			//super.filterContacts(seqSep);	// currently not allowed to filter contacts
			//super.printWarnings(chainCode); // doesn't make sense here
			super.checkAndAssignSecondaryStructure();
			
		} catch (IOException e) {
			System.err.println("Error while trying to load graph from CASP RR file.");
			throw new ModelConstructionError(e.getMessage());
		} catch (GraphFileFormatError e){
			System.err.println("Error while trying to load graph from CASP RR file. Wrong CASP file format.");
			throw new ModelConstructionError(e.getMessage());			
		}
		

	}
	
	public CaspRRFileModel(Model mod) {
	    super(mod);
	}
	
	public CaspRRFileModel copy() {
	    return new CaspRRFileModel(this);
	}

	/**
	 * The loading of the contact map is implemented in the constructor not in 
	 * this function. This function essentially doesn't do anything!
	 * @param pdbChainCode pdb chain code of the chain to be loaded (ignored!)
	 * @param modelSerial  a model serial (NMR)
	 * @throws ModelConstructionError
	 */
	@Override
	public void load(String pdbChainCode, int modelSerial) throws ModelConstructionError {
		return;
	}
	
}
