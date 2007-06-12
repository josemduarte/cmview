package cmview.datasources;
import proteinstructure.*;
import java.io.IOException;

/** 
 * A contact map data model based on a structure loaded from a PDB file.
 * 
 * @author		Henning Stehr
 * Class: 		PdbFileModel
 * Package: 	cmview.datasources
 * Date:		14/05/2007, last updated: 15/05/2007
 * 
 */
public class PdbFileModel extends Model {

	/**
	 * Overloaded constructor to load the data.
	 */
	public PdbFileModel(String fileName, String chainCode, String edgeType, double distCutoff, int seqSep) {
		
		// load PDB file
		try {
			this.pdb = new Pdb(fileName, chainCode, false); // TODO: add chain code parameter
		} catch (IOException e) {
			System.err.println("Error while loading from PDB file.");
		}
				
		// get contact map
		this.graph = pdb.get_graph(edgeType, distCutoff);

		super.writeTempPdbFile();
		super.initializeContactMap();
		super.filterContacts(seqSep);
		super.printWarnings(chainCode);
	}
	
}
