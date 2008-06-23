package cmview.datasources;
import java.sql.SQLException;

import cmview.Start;

import proteinstructure.*;

/** 
 * A contact map data model based on a structure loaded from Pdbase.
 */
public class PdbaseModel extends Model {
	
	/* These members' only purpose is to temporarily store some values passed to the constructor and use them in load().
	   Eventually these should be parameters of load directly, but for the moment we keep them here in order to keep
	   the interface unchanged.
	*/
	private String edgeType;
	private double distCutoff;
	private int minSeqSep;
	private int maxSeqSep;
		
	/**
	 * Overloaded constructor to load the data.
	 * @throws SQLException 
	 * @throws PdbCodeNotFoundError 
	 */
	public PdbaseModel(String pdbCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, String db) 
	throws PdbCodeNotFoundError, SQLException   {
		this.edgeType = edgeType; 
		this.distCutoff = distCutoff;
		this.minSeqSep = minSeqSep;
		this.maxSeqSep = maxSeqSep;		
		this.pdb = new PdbasePdb(pdbCode, db, Start.getDbConnection());
	}
	
	public PdbaseModel(Model mod) {
	    super(mod);
	}
	
	public PdbaseModel copy() {
	    return new PdbaseModel(this);
	}


	/**
	 * Loads the chain corresponding to the passed chain code identifier.
	 * @param pdbChainCode  pdb chain code of the chain to be loaded
	 * @param modelSerial  a model serial
	 * @throws ModelConstructionError
	 */
	@Override
	public void load(String pdbChainCode, int modelSerial) throws ModelConstructionError {
		// load structure from Pdbase
		try {
			this.pdb.load(pdbChainCode,modelSerial);
			super.checkAndAssignSecondaryStructure();
			this.graph = pdb.get_graph(edgeType, distCutoff);
			
			// assign a loadedGraphId to this model
			String name = this.graph.getPdbCode()+this.graph.getChainCode();
			if (this.graph.getPdbCode().equals(Pdb.NO_PDB_CODE)) {
				name = DEFAULT_LOADEDGRAPHID;
			} 
			this.loadedGraphID = Start.setLoadedGraphID(name, this);
			super.writeTempPdbFile();

			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(pdbChainCode);
			
		} catch (PdbLoadError e) {
			System.err.println("Failed to load structure");
			throw new ModelConstructionError(e.getMessage());
		}		
	}
}
