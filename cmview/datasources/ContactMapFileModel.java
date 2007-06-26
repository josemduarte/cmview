package cmview.datasources;
import proteinstructure.*;
import cmview.Start;

import java.io.IOException;
import java.sql.SQLException;

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
	 * Overloaded constructor to load the data from file.
	 * @throws ModelConstructionError 
	 */
	public ContactMapFileModel(String fileName) throws ModelConstructionError {
			
		// load Contact graph from file
		try {
			
			this.graph = new Graph(fileName);
			
			String pdbCode = graph.getPdbCode();
			String chainPdbCode = graph.getPdbChainCode();
			
			// check whether sequence info exists
			if(graph.getSequence().equals("")) {
				System.err.println("File contains no sequence information. Many features will be unavailable.");
			}
			
			// load structure from pdbase if possible
			if(!pdbCode.equals("") && !chainPdbCode.equals("")) {
				if (!Start.isDatabaseConnectionAvailable()) {
					System.err.println("No database connection. Can't load structure.");					
				} else {
					try {
						this.pdb = new Pdb(pdbCode, chainPdbCode); // by default loading from pdbase
						super.writeTempPdbFile(); // this doesn't make sense without a pdb object
					} catch (PdbaseAcCodeNotFoundError e) {
						System.err.println("Failed to load structure because accession code was not found in Pdbase");
					} catch (MsdsdAcCodeNotFoundError e) {
						System.err.println("Failed to load structure because accession code was not found in MSD");
					} catch (MsdsdInconsistentResidueNumbersError e) {
						System.err.println("Failed to load structure because of inconsistent residue numbering in MSD");
					} catch (PdbaseInconsistencyError e) {
						System.err.println("Failed to load structure because of inconsistency in Pdbase");
					} catch(SQLException e) {
						System.err.println("Failed to load structure because of database error");
					}
					// if pdb creation failed then pdb=null
				}
				
			} else
			{
				System.out.println("No pdb code and/or chain code found. Can not load structure.");
			}
			
			super.initializeContactMap();
			//super.filterContacts(seqSep);	// currently not allowed to filter contacts
			//super.printWarnings(chainCode); // doesn't make sense here
			
		} catch (IOException e) {
			System.err.println("Error while trying to load graph from contact map file.");
			throw new ModelConstructionError(e);
		}
		

	}
	
}
