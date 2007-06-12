package cmview.datasources;
import proteinstructure.*;

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
	 */
	public GraphDbModel(String pdbCode, String chainCode, String edgeType,
						double distCutoff, int seqSep, String db) {
		
		// load contact graph from user specified graph database
		try {
			graph = new Graph(db, pdbCode, chainCode, distCutoff, edgeType);
		} catch (GraphIdNotFoundError e) {
			System.err.println("Error: Failed to load graph from database.");
		}
		
		// load structure from MSD (to display in Pymol)
		try {
			this.pdb = new Pdb(pdbCode, chainCode, "msdsd_00_07_a");
		} catch (PdbaseAcCodeNotFoundError e) {
			System.err.println("Error: Accession code not found in structure loaded from Pdbase");
		} catch (MsdsdAcCodeNotFoundError e) {
			System.err.println("Error: Accession code not found in structure loaded from MSD");
		} catch (MsdsdInconsistentResidueNumbersError e) {
			System.err.println("Warning: Inconsistent residue numbering in structure loaded from MSD");
		} catch (PdbaseInconsistencyError e) {
			System.err.println("Warning: Inconsistency in structure loaded from Pdbase");
		}
				
		super.writeTempPdbFile();
		super.initializeContactMap();
		super.filterContacts(seqSep);
		super.printWarnings(chainCode);
		
	}
	
	/**
	 * Overloaded constructor to load the data from a graph database,
	 * given the id of a single model graph. 
	 */
	public GraphDbModel(int graphId, String db) {
		
		// load contact graph from user specified graph database
		try {
			graph = new Graph(db, graphId);
		} catch (GraphIdNotFoundError e) {
			System.err.println("Error: Failed to load graph from database.");
		}

		// read information about structure from graph object
		String pdbCode = graph.accode;
		String chainCode = graph.chaincode;
		System.out.println("pdb_code=" + pdbCode);
		System.out.println("chain_code=" + chainCode);		
		int seqSep = 0; // for the moment don't allow to change this
		
		// TODO: check whether loading from MSD makes sense
		
		// load structure from MSD (if possible)
		try {
			this.pdb = new Pdb(pdbCode, chainCode, "msdsd_00_07_a");
		} catch (PdbaseAcCodeNotFoundError e) {
			System.err.println("Error: Accession code not found in structure loaded from Pdbase");
		} catch (MsdsdAcCodeNotFoundError e) {
			System.err.println("Error: Accession code not found in structure loaded from MSD");
		} catch (MsdsdInconsistentResidueNumbersError e) {
			System.err.println("Warning: Inconsistent residue numbering in structure loaded from MSD");
		} catch (PdbaseInconsistencyError e) {
			System.err.println("Warning: Inconsistency in structure loaded from Pdbase");
		}
				
		super.writeTempPdbFile();
		super.initializeContactMap();
		super.filterContacts(seqSep);
		super.printWarnings(chainCode);		
		
	}

}
