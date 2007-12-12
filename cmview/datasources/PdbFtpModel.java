package cmview.datasources;
import java.io.File;
import java.io.IOException;

import proteinstructure.*;

import cmview.Start;

/** 
 * A contact map data model based on a structure loaded from a CIF file downloaded from pdb's ftp
 */
public class PdbFtpModel extends Model {
    
	File cifFile;
	String edgeType;
	double distCutoff;
	
	/**
	 * Overloaded constructor to load the data.
	 * @throws IOException 
	 */
	public PdbFtpModel(String pdbCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep) throws IOException  {
		this.edgeType = edgeType; 
		this.distCutoff = distCutoff;
		super.setMinSequenceSeparation(minSeqSep);
		super.setMaxSequenceSeparation(maxSeqSep);
		this.pdb = new CiffilePdb(pdbCode, Start.PDB_FTP_URL);
		this.cifFile = ((CiffilePdb) this.pdb).getCifFile();
	}
	
	public PdbFtpModel(File cifFile, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep) throws IOException  {
		this.cifFile = cifFile;
		this.edgeType = edgeType; 
		this.distCutoff = distCutoff;
		super.setMinSequenceSeparation(minSeqSep);
		super.setMaxSequenceSeparation(maxSeqSep);
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
		// load CIF file from online pdb
		try {
			this.pdb.load(pdbChainCode,modelSerial);
			this.graph = pdb.get_graph(edgeType, distCutoff);

			super.writeTempPdbFile();
			super.initializeContactMap();
			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(pdbChainCode);
			super.checkAndAssignSecondaryStructure();
			
		} catch (PdbLoadError e) {
			System.err.println("Failed to load structure.");
			throw new ModelConstructionError(e.getMessage());
		}
	}
}
