package cmview.datasources;
import proteinstructure.*;

import java.io.IOException;

import cmview.Start;

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
				throw new ModelConstructionError("File contains no sequence information.");
			}
			
			// assign a loadedGraphId to this model
			String name = String.format("T%04d",this.graph.getTargetNum());
			if (this.graph.getTargetNum()==0) {
				name = DEFAULT_LOADEDGRAPHID;
			} 
			this.loadedGraphID = Start.setLoadedGraphID(name, this);
			
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
