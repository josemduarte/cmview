import tools.MySQLConnection;
import proteinstructure.*;

import java.io.IOException;

/** 
 * A contact map data model based on a structure loaded from Pdbase.
 * 
 * @author		Henning
 * Class: 		MsdsdModel
 * Package: 	cm2pymol
 * Date:		14/05/2007, last updated: 15/05/2007
 * 
 */
public class MsdsdModel extends Model {

	public MsdsdModel(String pdbCode, String chainCode, String edgeType, double distCutoff, int seqSep, String db) {

		// load structure from MSD
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
