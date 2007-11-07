package cmview.datasources;
import proteinstructure.*;
import java.io.IOException;

/** 
 * A contact map data model based on a structure loaded from a PDB file.
 */
public class PdbFileModel extends Model {

	/**
	 * Overloaded constructor to load the data.
	 * @throws ModelConstructionError 
	 */
	public PdbFileModel(String fileName, String pdbChainCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep) throws ModelConstructionError {
		
		// load PDB file
		try {
			this.pdb = new PdbfilePdb(fileName, pdbChainCode);
			this.graph = pdb.get_graph(edgeType, distCutoff);

			super.writeTempPdbFile();
			super.initializeContactMap();
			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(pdbChainCode);
			super.checkAndAssignSecondaryStructure();
			
		} catch (IOException e) {
			System.err.println("Error while reading from PDB file.");
			throw new ModelConstructionError(e.getMessage());
		} catch (PdbfileFormatError e){
			System.err.println("Failed to load structure from PDB file. Wrong file format");
			throw new ModelConstructionError(e.getMessage());		
		} catch (PdbChainCodeNotFoundError e){
			System.err.println("Failed to load structure. Chain code not found in PDB file.");
			throw new ModelConstructionError(e.getMessage());
		}

				
	}
	
	public PdbFileModel(Model mod) {
	    super(mod);
	}
	
	public PdbFileModel copy() {
	    return new PdbFileModel(this);
	}
}
