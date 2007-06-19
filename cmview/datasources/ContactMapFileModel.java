package cmview.datasources;
import proteinstructure.*;

import java.io.IOException;

/** 
 * A contact map data model based on a graph loaded from a Contact map file.
 * 
 * @author		Henning Stehr
 * Class: 		PdbFileModel
 * Package: 	cmview.datasources
 * Date:		15/05/2007, last updated: 15/05/2007
 * 
 */
public class ContactMapFileModel extends Model {
	
	/**
	 * Overloaded constructor to load the data.
	 */
	public ContactMapFileModel(String fileName) {
			
		// load Contact graph from file
		try {
			this.graph = new Graph(fileName);
		} catch (IOException e) {
			System.err.println("Error while trying to load graph from contact map file.");
		}
		
		String pdbCode = graph.accode;
		String chainPdbCode = graph.chaincode;
		
		// check whether sequence info exists
		if(graph.sequence == "") {
			System.err.println("File contains no sequence information. Some features will be unavailable.");
		}
		
		// load structure from pdbase if possible
		if(!pdbCode.equals("") && !chainPdbCode.equals("")) {
			try {
				this.pdb = new Pdb(pdbCode, chainPdbCode); // by default loading from pdbase
				super.writeTempPdbFile();
			} catch (PdbaseAcCodeNotFoundError e) {
				System.err.println("Error: Accession code not found in structure loaded from Pdbase");
			} catch (MsdsdAcCodeNotFoundError e) {
				System.err.println("Error: Accession code not found in structure loaded from MSD");
			} catch (MsdsdInconsistentResidueNumbersError e) {
				System.err.println("Warning: Inconsistent residue numbering in structure loaded from MSD");
			} catch (PdbaseInconsistencyError e) {
				System.err.println("Warning: Inconsistency in structure loaded from Pdbase");
			}
		} else
		{
			System.out.println("No pdb code and/or chain code found. Can not load structure.");
		}
		
		super.initializeContactMap();
		//super.filterContacts(seqSep);	// currently not allowed to filter contacts
		//super.printWarnings(chainCode); // doesn't make sense here
	}
	
}
