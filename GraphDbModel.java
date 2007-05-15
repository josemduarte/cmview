import proteinstructure.*;

/** 
 * A contact map data model based on a single_model_graph loaded from the database
 * 
 * @author		Henning
 * Class: 		GraphDbModel
 * Package: 	cm2pymol
 * Date:		14/05/2007, last updated: 15/05/2007
 * 
 */
public class GraphDbModel extends Model {
	
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
	
	/** Create a GraphDbModel given the id of a single model graph */
	public GraphDbModel(int graphId, String db) {
		
		// load graph from graph database
		// if possible:
		// 		load structure from MSD
		// 		super.writeTempPdbFile();
		// super.initializeContactMap();
		// seqSep = 0;
		// super.filterContacts();
		// super.printWarnings();
		
		System.err.println("Loading from Graph database using graph id not implemented yet.");
	}

}
