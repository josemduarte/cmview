package cmview.datasources;
import cmview.Start;
import proteinstructure.*;


/** 
 * A contact map data model based on a structure loaded from a PDB file.
 */
public class PdbFileModel extends Model {

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
	 * @throws ModelConstructionError 
	 */
	public PdbFileModel(String fileName, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep) throws ModelConstructionError {
		this.edgeType = edgeType; 
		this.distCutoff = distCutoff;
		this.minSeqSep = minSeqSep;
		this.maxSeqSep = maxSeqSep;
		this.pdb = new PdbfilePdb(fileName);
	}

	public PdbFileModel(Model mod) {
	    super(mod);
	}
	
	public PdbFileModel copy() {
	    return new PdbFileModel(this);
	}
	
	/**
	 * Loads the chain corresponding to the passed chain code identifier.
	 * @param pdbChainCode  pdb chain code of the chain to be loaded
	 * @throws ModelConstructionError
	 */
	public void load(String pdbChainCode, int modelSerial) throws ModelConstructionError {
		// load PDB file
		try {
			this.pdb.load(pdbChainCode,modelSerial);
			super.checkAndAssignSecondaryStructure();
			this.graph = pdb.get_graph(edgeType, distCutoff);

			// assign a loadedGraphId to this model
			String name = DEFAULT_LOADEDGRAPHID;
			if (!this.graph.getPdbCode().equals(Pdb.NO_PDB_CODE)) {
				name = this.graph.getPdbCode()+this.graph.getChainCode();
			} 
			if (this.graph.getTargetNum()!=0) {
				name = String.format("T%04d",this.graph.getTargetNum());
			}
			this.loadedGraphID = Start.setLoadedGraphID(name, this);

			super.writeTempPdbFile();
			
			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(pdbChainCode);
			
		} catch (PdbLoadError e) {
			throw new ModelConstructionError(e.getMessage());
		}
	}
}
