package cmview.datasources;
import java.io.File;
import java.io.IOException;

import owl.core.structure.*;
import owl.core.structure.graphs.RIGEnsemble;
import owl.core.structure.graphs.RIGGeometry;


import cmview.Start;

/** 
 * A contact map data model based on a structure loaded from a CIF file downloaded from pdb's ftp
 */
public class PdbFtpModel extends Model {
    
	
	private File cifFile;		// the cached cif file downloaded from the PDB
		
	/**
	 * Overloaded constructor to load the data.
	 * @throws IOException 
	 */
	public PdbFtpModel(String pdbCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep) throws IOException  {
		this.edgeType = edgeType; 
		this.distCutoff = distCutoff;
		this.minSeqSep = minSeqSep;
		this.maxSeqSep = maxSeqSep;
		this.pdb = new CiffilePdb(pdbCode, Start.PDB_FTP_URL);
		this.cifFile = ((CiffilePdb) this.pdb).getCifFile();
	}
	
	public PdbFtpModel(File cifFile, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep) throws IOException  {
		this.cifFile = cifFile;
		this.edgeType = edgeType; 
		this.distCutoff = distCutoff;
		this.minSeqSep = minSeqSep;
		this.maxSeqSep = maxSeqSep;
		this.pdb = new CiffilePdb(cifFile);
	}
	
	public PdbFtpModel(Model mod) {
	    super(mod);
	}
	
	public PdbFtpModel copy() {
	    return new PdbFtpModel(this);
	}

	public File getCifFile() {
		return cifFile;
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
	 * If loadEnsembleGraph is true, the graph in this model will be the average graph of the ensemble of all models
	 * instead of the graph of the specified model only. The Pdb object will still correspond to the given model number.
	 * @param pdbChainCode  pdb chain code of the chain to be loaded
	 * @param modelSerial  a model serial
	 * @param loadEnsembleGraph whether to set the graph in this model to the (weighted) ensemble graph of all models
	 * @throws ModelConstructionError
	 */
	public void load(String pdbChainCode, int modelSerial, boolean loadEnsembleGraph) throws ModelConstructionError {
		// load CIF file from online pdb
		try {
			if(pdbChainCode == null) pdbChainCode = this.pdb.getChains()[0]; else
			if(!this.pdb.hasChain(pdbChainCode)) throw new ModelConstructionError("Chain '" + pdbChainCode + "' not found");
			this.pdb.load(pdbChainCode,modelSerial);
			this.secondaryStructure = pdb.getSecondaryStructure();	// in case, dssp is n/a, use ss from pdb
			super.checkAndAssignSecondaryStructure();				// if dssp is a/, recalculate ss
			if(loadEnsembleGraph == false || this.pdb.getModels().length == 1) {
				this.graph = pdb.getRIGraph(edgeType, distCutoff);
			} else {
				RIGEnsemble e = new RIGEnsemble(edgeType, distCutoff);
				try {
					e.loadFromMultiModelFile(this.cifFile);
					this.graph = e.getAverageGraph();
					this.graph.setPdbCode(this.pdb.getPdbCode());
					this.graph.setChainCode(pdbChainCode);
					this.setIsGraphWeighted(true);
				} catch (IOException e1) {
					throw new ModelConstructionError("Error loading ensemble graph: " + e1.getMessage());
				}
			}
			
			// this.graph and this.residues are now available
			//TODO 4Corinna compute graph geometry and hand it over to ContactView
			if(Start.USE_CGAP) {
				this.graphGeom = new RIGGeometry(this.graph, this.pdb.getResidues());
				System.out.println("PdbFtpModel   GraphGeometry loaded");
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
			System.err.println("Failed to load structure.");
			throw new ModelConstructionError(e.getMessage());
		}
	}
}
