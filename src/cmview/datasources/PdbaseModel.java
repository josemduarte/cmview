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
	
		
	/**
	 * Overloaded constructor to load the data.
	 * @throws SQLException 
	 * @throws PdbCodeNotFoundException 
	 * @throws PdbLoadError 
	 */
	public PdbaseModel(String pdbCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, String db) 
	throws PdbCodeNotFoundException, SQLException, PdbLoadError   {
		this.edgeType = edgeType; 
		this.distCutoff = distCutoff;
		this.minSeqSep = minSeqSep;
		this.maxSeqSep = maxSeqSep;		
		try {
			this.pdb = new PdbasePdb(pdbCode, db, Start.getDbConnection());
		} catch (PdbLoadError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	 * instead of the graph of the specified model only. The Pdb object will still correspond to the given model number.
	 * @param pdbChainCode  pdb chain code of the chain to be loaded
	 * @param modelSerial  a model serial
	 * @param loadEnsembleGraph whether to set the graph in this model to the (weighted) ensemble graph of all models
	 * @throws ModelConstructionError
	 */
	public void load(String pdbChainCode, int modelSerial, boolean loadEnsembleGraph) throws ModelConstructionError {
		// load structure from Pdbase
		try {
			if(pdbChainCode == null) pdbChainCode = this.pdb.getChains()[0]; else
			if(!this.pdb.hasChain(pdbChainCode)) throw new ModelConstructionError("Chain '" + pdbChainCode + "' not found");
			this.pdb.load(pdbChainCode,modelSerial);
			this.secondaryStructure = pdb.getSecondaryStructure(); 	// in case, dssp is n/a, use ss from pdb
			super.checkAndAssignSecondaryStructure(); 				// if dssp is a/, recalculate ss
			if(loadEnsembleGraph == false || this.pdb.getModels().length == 1) {
				this.graph = pdb.getRIGraph(edgeType, distCutoff);
			} else {
				try {
					RIGEnsemble e = new RIGEnsemble(edgeType, distCutoff);
					for(int modNum: this.pdb.getModels()) {
						Pdb p = new PdbasePdb(this.pdb.getPdbCode());
						p.load(pdbChainCode, modNum);
						e.addRIG(p.getRIGraph(edgeType, distCutoff));
					}
					this.graph = e.getAverageGraph();
					if(this.graph == null) {
						throw new ModelConstructionError("Error loading ensembl graph: Graph returned by GraphAverager is null");
					}
					this.graph.setPdbCode(this.pdb.getPdbCode());
					this.graph.setChainCode(pdbChainCode);
					this.setIsGraphWeighted(true);
				} catch (SQLException e) {
					throw new ModelConstructionError("Error loading ensemble graph: " + e.getMessage());
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
