package cmview.datasources;
import proteinstructure.*;

//import java.sql.SQLException;

import cmview.Start;

/** 
 * A contact map data model based on a single_model_graph loaded from the database
 * 
 * @author		Henning Stehr
 * Class: 		GraphDbModel
 * Package: 	cmview.datasources
 * Date:		14/05/2007, last updated: 15/05/2007
 * 
 */
public class GraphDbModel extends Model {

	/**
	 * Overloaded constructor to load the data from a graph database, where
	 * the graph id will be looked up based on the given graph details.
	 * @throws ModelConstructionError 
	 */
	public GraphDbModel(String pdbCode, String pdbChainCode, String edgeType,
						double distCutoff, String db) throws ModelConstructionError {
		
		// load contact graph from user specified graph database
		try {
			graph = new DbGraph(db, Start.getDbConnection(), pdbCode, pdbChainCode, distCutoff, edgeType);
			
			// load structure from MSD (to display in Pymol)
			if (!Start.isDatabaseConnectionAvailable()) {
				System.err.println("No database connection. Can't load structure.");					
			} else {
				try {
					this.pdb = new MsdsdPdb(pdbCode, pdbChainCode, Start.DEFAULT_MSDSD_DB, Start.getDbConnection());
					super.writeTempPdbFile(); // this doesn't make sense without a pdb object
				} catch (MsdsdAcCodeNotFoundError e) {
					System.err.println("Failed to load structure because accession code was not found in MSD");
				} catch (MsdsdInconsistentResidueNumbersError e) {
					System.err.println("Failed to load structure because of inconsistent residue numbering in MSD");
//				} catch(SQLException e) {
//					System.err.println("Failed to load structure because of database error");
				}
				// if pdb created failed then pdb=null
			}
			super.initializeContactMap();
			//super.filterContacts(seqSep);	// currently not allowed to filter contacts
			super.printWarnings(pdbChainCode);
			
		} catch (GraphIdNotFoundError e) {
			System.err.println("Error: Could not find graph id in database.");
			throw new ModelConstructionError(e);
//		} catch(SQLException e) {
//			System.err.println("Error: Could not read graph from database");
//			throw new ModelConstructionError(e);
		}	
	}
	
	/**
	 * Overloaded constructor to load the data from a graph database,
	 * given the id of a single model graph. 
	 * @throws ModelConstructionError 
	 */
	public GraphDbModel(int graphId, String db) throws ModelConstructionError {
		
		// load contact graph from user specified graph database
		try {
			graph = new DbGraph(db, Start.getDbConnection(), graphId);
			
			// read information about structure from graph object
			String pdbCode = graph.getPdbCode();
			String pdbChainCode = graph.getPdbChainCode();
			
			// load structure from MSD (to display in Pymol)
			try {
				this.pdb = new MsdsdPdb(pdbCode, pdbChainCode, Start.DEFAULT_MSDSD_DB, Start.getDbConnection());
				super.writeTempPdbFile(); // this doesn't make sense without a pdb object
			} catch (MsdsdAcCodeNotFoundError e) {
				System.err.println("Failed to load structure because accession code was not found in MSD");
			} catch (MsdsdInconsistentResidueNumbersError e) {
				System.err.println("Failed to load structure because of inconsistent residue numbering in MSD");
//			} catch(SQLException e) {
//				System.err.println("Failed to load structure because of database error");
			}
			// if pdb created failed then pdb=null

			super.initializeContactMap();
			//super.filterContacts(seqSep);	// currently not allowed to filter contacts
			super.printWarnings(pdbChainCode);
			
		} catch (GraphIdNotFoundError e) {
			System.err.println("Error: Could not find graph id in database.");
			throw new ModelConstructionError(e);
//		} catch(SQLException e) {
//			System.err.println("Error: Could not read graph from database");
//			throw new ModelConstructionError(e);
		}		
		
	}

}
