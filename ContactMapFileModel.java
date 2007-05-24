import proteinstructure.*;

import java.io.IOException;

/** 
 * A contact map data model based on a graph loaded from a Contact map file.
 * 
 * @author		Henning
 * Class: 		PdbFileModel
 * Package: 	cm2pymol
 * Date:		15/05/2007, last updated: 15/05/2007
 * 
 */
public class ContactMapFileModel extends Model {
	
	public ContactMapFileModel(String fileName) {
		
		// load Contact graph from file
		try {
			this.graph = new Graph(fileName);
		} catch (IOException e) {
			System.err.println("Error while trying to load graph from contact map file.");
		}
		
		String pdbCode = graph.accode;
		//String chainCode = graph.chain;
		String chainPdbCode = graph.chaincode;
//		String contactType = graph.ct;
//		double distCutoff = graph.cutoff;
		int seqSep = 0;
		
		// check whether sequence info exists
		if(graph.sequence == "") {
			System.err.println("File contains no sequence information. Cannot show contact map.");
		}
		
		// load structure from pdbase if possible
		if(!pdbCode.equals("") && !chainPdbCode.equals("")) {
			try {
				this.pdb = new Pdb(pdbCode, chainPdbCode); // by default loading from pdbase
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
		} else
		{
			System.out.println("No pdb code and/or chain code found. Can not load structure.");
		}
		
		super.initializeContactMap();
		super.filterContacts(seqSep);
		//super.printWarnings(chainCode); // doesn't make sense here
	}
	
}
