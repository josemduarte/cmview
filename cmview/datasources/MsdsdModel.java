package cmview.datasources;
import proteinstructure.*;

import cmview.Start;

/** 
 * A contact map data model based on a structure loaded from Pdbase.
 */
public class MsdsdModel extends Model {

	String pdbCode;
	String edgeType;
	double distCutoff;
	
	/**
	 * Overloaded constructor to load the data.
	 * @throws ModelConstructionError 
	 */
	public MsdsdModel(String pdbCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, String db) {
		this.pdbCode = pdbCode;
		this.edgeType = edgeType;
		this.distCutoff = distCutoff;
		super.setMinSequenceSeparation(minSeqSep);
		super.setMaxSequenceSeparation(maxSeqSep);
		this.pdb = new MsdsdPdb(pdbCode, db, Start.getDbConnection());
	}
	
	public MsdsdModel(Model mod) {
	    super(mod);
	}
	
	public MsdsdModel copy() {
	    return new MsdsdModel(this);
	}

	/**
	 * Loads the given chain from MSDSD.
	 * @param pdbChainCode  pdb chain code of the chain to be loaded
	 * @throws ModelConstructionError
	 */
	@Override
	public void load(String pdbChainCode, int modelSerial) throws ModelConstructionError {
		// load structure from MSD
		try {
			// get structure from db
			this.pdb.load(pdbChainCode,modelSerial);
			
			// get graph from structure
			this.graph = pdb.get_graph(edgeType, distCutoff);
			
			super.writeTempPdbFile();
			String name = this.graph.getPdbCode()+this.graph.getChainCode();
			if (this.graph.getPdbCode().equals(Pdb.NO_PDB_CODE)) {
				name = DEFAULT_LOADEDGRAPHID;
			} 
			this.loadedGraphID = Start.setLoadedGraphID(name, this);

			super.initializeContactMap();
			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(pdbChainCode);
			super.checkAndAssignSecondaryStructure();
			
		} catch (PdbLoadError e) {
			System.err.println("Failed to load structure");
			throw new ModelConstructionError(e.getMessage());
		}		
	}
}
