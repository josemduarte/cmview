package cmview.datasources;
import cmview.Start;
import proteinstructure.*;


/** 
 * A contact map data model based on a structure loaded from a PDB file.
 */
public class PdbFileModel extends Model {

	String fileName;
	String edgeType;
	double distCutoff; 
	
	/**
	 * Overloaded constructor to load the data.
	 * @throws ModelConstructionError 
	 */
	public PdbFileModel(String fileName, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep) throws ModelConstructionError {
		this.fileName = fileName;
		this.edgeType = edgeType; 
		this.distCutoff = distCutoff;
		super.setMinSequenceSeparation(minSeqSep);
		super.setMaxSequenceSeparation(maxSeqSep);
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
			throw new ModelConstructionError(e.getMessage());
		}
	}
}
