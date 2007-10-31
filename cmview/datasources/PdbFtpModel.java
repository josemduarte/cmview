package cmview.datasources;
import java.io.IOException;

import proteinstructure.*;

import cmview.Start;

/** 
 * A contact map data model based on a structure loaded from a CIF file downloaded from pdb's ftp
 */
public class PdbFtpModel extends Model {
    
	/**
	 * Overloaded constructor to load the data.
	 * @throws ModelConstructionError 
	 */
	public PdbFtpModel(String pdbCode, String pdbChainCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep) throws ModelConstructionError {
	
		// load CIF file from online pdb
		try {
			this.pdb = new CiffilePdb(pdbCode, pdbChainCode, Start.PDB_FTP_URL);
			this.graph = pdb.get_graph(edgeType, distCutoff);

			super.writeTempPdbFile();
			super.initializeContactMap();
			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(pdbChainCode);
			super.checkAndAssignSecondaryStructure();
			
		} catch (IOException e) {
			System.err.println("Error while reading from CIF file.");
			throw new ModelConstructionError(e.getMessage());
		} catch (CiffileFormatError e){
			System.err.println("Failed to load structure from CIF file. Wrong file format");
			throw new ModelConstructionError(e.getMessage());		
		} catch (PdbChainCodeNotFoundError e){
			System.err.println("Failed to load structure. Chain code not found in CIF file.");
			throw new ModelConstructionError(e.getMessage());
		}

				
	}
	
	public PdbFtpModel(Model mod) {
	    super(mod);
	}
	
}
