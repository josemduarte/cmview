package cmview.datasources;
import proteinstructure.*;

import java.sql.SQLException;

import cmview.Start;

/** 
 * A contact map data model based on a single_model_graph loaded from the database
 */
public class GraphDbModel extends Model {

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
			int modelSerial = graph.getModel();
			
			// load structure from MSD (to display in Pymol)
			try {
				//TODO eventually we should read from pdbase, new graphs in db are now based on pdbase
				this.pdb = new MsdsdPdb(pdbCode, Start.DEFAULT_MSDSD_DB, Start.getDbConnection());
				this.pdb.load(pdbChainCode,modelSerial);
				System.out.println("Loaded structure "+pdbCode+" "+pdbChainCode+" from MSD database "+Start.DEFAULT_MSDSD_DB);
				super.writeTempPdbFile(); // this doesn't make sense without a pdb object
			} catch (PdbLoadError e) {
				System.err.println("Failed to load structure.");
				pdb = null;
			}
			// if pdb created failed then pdb=null

			super.initializeContactMap();
			//super.filterContacts(seqSep);	// currently not allowed to filter contacts
			super.printWarnings(pdbChainCode);
			
		} catch (GraphIdNotFoundError e) {
			System.err.println("Error: Could not find graph id in database.");
			throw new ModelConstructionError(e.getMessage());
		} catch(SQLException e) {
			System.err.println("Error: Could not read graph from database");
			throw new ModelConstructionError(e.getMessage());
		}		
		
	}
	
	public GraphDbModel(Model mod) {
	    super(mod);
	}
	
	public GraphDbModel copy() {
	    return new GraphDbModel(this);
	}

	/**
	 * The loading of the graph is implemented in the constructor not in this 
	 * function. This function essentially does not do anything!
	 * @param pdbChainCode pdb chain code of the chain to be loaded (ignored!)
	 * @throws ModelConstructionError
	 */
	@Override
	public void load(String pdbChainCode, int modelSerial) throws ModelConstructionError {
		return;
	}

}
