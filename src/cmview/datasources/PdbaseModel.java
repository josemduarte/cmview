package cmview.datasources;
import java.sql.SQLException;

import owl.core.structure.*;
import owl.core.structure.graphs.RIGEnsemble;
import owl.core.structure.graphs.RIGGeometry;

import cmview.Start;


/** 
 * A contact map data model based on a structure loaded from Pdbase.
 */
public class PdbaseModel extends Model {
	
	private String pdbCode;
	private String db;
	private PdbaseParser parser;
		
	/**
	 * Overloaded constructor to load the data.
	 * @throws SQLException 
	 * @throws PdbCodeNotFoundException 
	 * @throws PdbLoadException 
	 */
	public PdbaseModel(String pdbCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, String db) 
	throws PdbCodeNotFoundException, SQLException {
		this.pdbCode = pdbCode;
		this.db = db;
		this.edgeType = edgeType; 
		this.distCutoff = distCutoff;
		this.minSeqSep = minSeqSep;
		this.maxSeqSep = maxSeqSep;
		this.parser = new PdbaseParser(this.pdbCode,db,Start.getDbConnection());
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
		load(pdbChainCode, modelSerial, false);
	}

	/**
	 * Loads the chain corresponding to the passed chain code identifier.
	 * loadEnsembleGraph is true, the graph in this model will be the average graph of the ensemble of all models
	 * instead of the graph of the specified model only. The PdbChain object will still correspond to the given model number.
	 * @param pdbChainCode  pdb chain code of the chain to be loaded
	 * @param modelSerial  a model serial
	 * @param loadEnsembleGraph whether to set the graph in this model to the (weighted) ensemble graph of all models
	 * @throws ModelConstructionError
	 */
	public void load(String pdbChainCode, int modelSerial, boolean loadEnsembleGraph) throws ModelConstructionError {

		// load structure from Pdbase
		try {
			PdbAsymUnit fullpdb = new PdbAsymUnit(pdbCode,modelSerial,Start.getDbConnection(),db);
			if(pdbChainCode == null) {
				this.pdb = fullpdb.getFirstChain();
				pdbChainCode = this.pdb.getPdbChainCode();
			} else if(!fullpdb.containsPdbChainCode(pdbChainCode)) {
				throw new ModelConstructionError("Chain '" + pdbChainCode + "' not found");
			} else {
				this.pdb = fullpdb.getChain(pdbChainCode);
			}
			this.secondaryStructure = pdb.getSecondaryStructure(); 	// in case, dssp is n/a, use ss from pdb
			super.checkAndAssignSecondaryStructure(); 				// if dssp is a/, recalculate ss
			if(loadEnsembleGraph == false || this.parser.getModels().length == 1) {
				this.graph = pdb.getRIGraph(edgeType, distCutoff);
			} else {
				try {
					RIGEnsemble e = new RIGEnsemble(edgeType, distCutoff);
					for(int modNum: this.parser.getModels()) {
						PdbAsymUnit fullp = new PdbAsymUnit(this.pdb.getPdbCode(),modNum,Start.getDbConnection(),db);
						PdbChain p = fullp.getChain(pdbChainCode);
						e.addRIG(p.getRIGraph(edgeType, distCutoff));
					}
					this.graph = e.getAverageGraph();
					if(this.graph == null) {
						throw new ModelConstructionError("Error loading ensembl graph: Graph returned by GraphAverager is null");
					}
					this.graph.setPdbCode(this.pdb.getPdbCode());
					this.graph.setChainCode(pdbChainCode);
					this.setIsGraphWeighted(true);
				} catch (PdbCodeNotFoundException e) {
					throw new ModelConstructionError("Error loading ensemble graph: " + e.getMessage());
				}
			}
			
			// this.graph and this.residues are now available
			//TODO 4Corinna compute graph geometry and hand it over to ContactView
			if(Start.USE_CGAP) {
				this.graphGeom = new RIGGeometry(this.graph, this.pdb.getResidues());
				System.out.println("PdbaseModel   GraphGeometry loaded");
				//this.graphGeom.printGeom();
			}
			
			// assign a loadedGraphId to this model
			String name = this.graph.getPdbCode()+this.graph.getChainCode();
			if (this.graph.getPdbCode().equals(PdbAsymUnit.NO_PDB_CODE)) {
				name = DEFAULT_LOADEDGRAPHID;
			} 
			this.loadedGraphID = Start.setLoadedGraphID(name, this);
			super.writeTempPdbFile();

			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(pdbChainCode);
			
		} catch (PdbLoadException e) {
			System.err.println("Failed to load structure");
			throw new ModelConstructionError(e.getMessage());
		} catch (PdbCodeNotFoundException e) {
			System.err.println("Failed to load structure");
			throw new ModelConstructionError(e.getMessage());
		}		
	}

	/**
	 * Gets chain codes for all chains being present in the source.
	 * 
	 * @throws GetterError
	 */
	public String[] getChains() throws PdbLoadException {
		return parser.getChains();
	}

	/**
	 * Gets model indices for all models being present in the source.
	 * 
	 * @return array of model identifiers, null if such thing
	 * @throws GetterError
	 */
	public Integer[] getModels() throws PdbLoadException {
		return parser.getModels();
	}
}
