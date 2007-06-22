package cmview.datasources;
import proteinstructure.*;

import java.sql.SQLException;

/** 
 * A contact map data model based on a structure loaded from Pdbase.
 * 
 * @author		Henning Stehr
 * Class: 		MsdsdModel
 * Package: 	cmview.datasources
 * Date:		14/05/2007, last updated: 15/05/2007
 * 
 */
public class MsdsdModel extends Model {

	/**
	 * Overloaded constructor to load the data.
	 * @throws ModelConstructionError 
	 */
	public MsdsdModel(String pdbCode, String chainCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, String db) throws ModelConstructionError {
		
		// load structure from MSD
		try {
			// get structure from db
			this.pdb = new Pdb(pdbCode, chainCode, db);
			
			// get graph from structure
			this.graph = pdb.get_graph(edgeType, distCutoff);
			
			super.writeTempPdbFile();
			super.initializeContactMap();
			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(chainCode);
			
		} catch (PdbaseAcCodeNotFoundError e) {
			System.err.println("Failed to load structure because accession code was not found in Pdbase");
			throw new ModelConstructionError(e);
		} catch (MsdsdAcCodeNotFoundError e) {
			System.err.println("Failed to load structure because accession code was not found in MSD");
			throw new ModelConstructionError(e);
		} catch (MsdsdInconsistentResidueNumbersError e) {
			System.err.println("Failed to load structure because of inconsistent residue numbering in MSD");
			throw new ModelConstructionError(e);
		} catch (PdbaseInconsistencyError e) {
			System.err.println("Failed to load structure because of inconsistency in Pdbase");
			throw new ModelConstructionError(e);
		} catch(SQLException e) {
			System.err.println("Failed to load structure because of database error");
			throw new ModelConstructionError(e);
		}
	}
	
}
