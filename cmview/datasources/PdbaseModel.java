package cmview.datasources;
//import java.sql.SQLException;

import cmview.Start;

import proteinstructure.*;

/** 
 * A contact map data model based on a structure loaded from Pdbase.
 * 
 * @author		Henning Stehr
 * Class: 		PdbaseModel
 * Package: 	cmview.datasources
 * Date:		14/05/2007, last updated: 14/05/2007
 * 
 */
public class PdbaseModel extends Model {

	/**
	 * Overloaded constructor to load the data.
	 * @throws ModelConstructionError 
	 */
	public PdbaseModel(String pdbCode, String pdbChainCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, String db) throws ModelConstructionError {
		
		// load structure from Pdbase
		try {
			this.pdb = new PdbasePdb(pdbCode, pdbChainCode, db, Start.getDbConnection());
			this.graph = pdb.get_graph(edgeType, distCutoff);
			
			super.writeTempPdbFile();
			super.initializeContactMap();
			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(pdbChainCode);
			
		} catch (PdbaseAcCodeNotFoundError e) {
			System.err.println("Failed to load structure because accession code was not found in Pdbase");
			throw new ModelConstructionError(e);
		} catch (PdbaseInconsistencyError e) {
			System.err.println("Failed to load structure because of inconsistency in Pdbase");
			throw new ModelConstructionError(e);
//		} catch(SQLException e) {
//			System.err.println("Failed to load structure because of database error");
//			throw new ModelConstructionError(e);
		}
	}
}
