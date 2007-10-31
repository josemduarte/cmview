package cmview.datasources;
import proteinstructure.*;

import java.sql.SQLException;

import cmview.Start;

/** 
 * A contact map data model based on a structure loaded from Pdbase.
 */
public class MsdsdModel extends Model {

	/**
	 * Overloaded constructor to load the data.
	 * @throws ModelConstructionError 
	 */
	public MsdsdModel(String pdbCode, String pdbChainCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, String db) throws ModelConstructionError {
		
		// load structure from MSD
		try {
			// get structure from db
			this.pdb = new MsdsdPdb(pdbCode, pdbChainCode, db, Start.getDbConnection());
			
			// get graph from structure
			this.graph = pdb.get_graph(edgeType, distCutoff);
			
			super.writeTempPdbFile();
			super.initializeContactMap();
			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(pdbChainCode);
			super.checkAndAssignSecondaryStructure();
			
		} catch (PdbCodeNotFoundError e) {
			System.err.println("Failed to load structure because accession code was not found in MSD");
			throw new ModelConstructionError(e.getMessage());
		} catch (MsdsdInconsistentResidueNumbersError e) {
			System.err.println("Failed to load structure because of inconsistent residue numbering in MSD");
			throw new ModelConstructionError(e.getMessage());
		} catch(SQLException e) {
			System.err.println("Failed to load structure because of database error");
			throw new ModelConstructionError(e.getMessage());
		}
	}
	
	public MsdsdModel(Model mod) {
	    super(mod);
	}
	
}
