package cmview.datasources;

import java.sql.SQLException;

import owl.core.structure.*;
import owl.core.structure.graphs.DbRIGraph;
import owl.core.structure.graphs.GraphIdNotFoundError;

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
			graph = new DbRIGraph(db, Start.getDbConnection(), graphId);
			this.secondaryStructure = graph.getSecondaryStructure(); // take ss from graph
																	 // will likely be null here
																	 // because we don't store ss in db
			
			// read information about structure from graph object
			String pdbCode = graph.getPdbCode();
			String pdbChainCode = graph.getPdbChainCode();
			int modelSerial = graph.getModel();
			
			// assign a loadedGraphId to this model
			String name = this.graph.getPdbCode()+this.graph.getChainCode();
			if (this.graph.getPdbCode().equals(Pdb.NO_PDB_CODE)) {
				name = DEFAULT_LOADEDGRAPHID;
			} 
			this.loadedGraphID = Start.setLoadedGraphID(name, this);
			
			// load structure from pdbase (to display in Pymol)
			try {
				this.pdb = new PdbasePdb(pdbCode, Start.DEFAULT_PDB_DB, Start.getDbConnection());
				this.pdb.load(pdbChainCode,modelSerial);
				System.out.println("Loaded structure "+pdbCode+" "+pdbChainCode+" from pdbase database "+Start.DEFAULT_PDB_DB);
				super.writeTempPdbFile(); // this doesn't make sense without a pdb object
			} catch (PdbLoadError e) {
				System.err.println("Failed to load structure: "+e.getMessage());
				pdb = null;
			} catch (PdbCodeNotFoundError e) {
				System.err.println("Failed to load structure. Pdb code "+pdbCode+" was not found in database "+Start.DEFAULT_PDB_DB);
				pdb = null;
			}
			// if pdb created failed then pdb=null
			
			// if structure is available, and has secondary structure annotation, use it
			if(this.pdb != null && pdb.getSecondaryStructure() != null) {
				this.secondaryStructure = pdb.getSecondaryStructure(); 
			}
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
