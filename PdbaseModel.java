import proteinstructure.*;

/** 
 * A contact map data model based on a structure loaded from Pdbase.
 * 
 * @author		Henning
 * Class: 		PdbaseModel
 * Package: 	cm2pymol
 * Date:		14/05/2007, last updated: 14/05/2007
 * 
 */
public class PdbaseModel extends Model {
	
	public PdbaseModel(String pdbCode, String chainCode, String edgeType, double distCutoff, int seqSep, String db) {
		
		// load structure from Pdbase
		try {
			this.pdb = new Pdb(pdbCode, chainCode, db);
		} catch (PdbaseAcCodeNotFoundError e) {
			System.err.println("Error: Accession code not found in structure loaded from Pdbase");
		} catch (MsdsdAcCodeNotFoundError e) {
			System.err.println("Error: Accession code not found in structure loaded from MSD");
		} catch (MsdsdInconsistentResidueNumbersError e) {
			System.err.println("Warning: Inconsistent residue numbering in structure loaded from MSD");
		} catch (PdbaseInconsistencyError e) {
			System.err.println("Warning: Inconsistency in structure loaded from Pdbase");
		}
			
		// get graph from structure
		this.graph = pdb.get_graph(edgeType, distCutoff);
		
		super.writeTempPdbFile();
		super.initializeContactMap();
		super.filterContacts(seqSep);
		super.printWarnings(chainCode);
		
	}


}
