package cmview.datasources;
import proteinstructure.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.*;

import cmview.Start;

/** 
 * A contact map data model based on a structure loaded from a CIF file downloaded from pdb's ftp
 */
public class PdbFtpModel extends Model {

	/**
	 * Overloaded constructor to load the data.
	 * @throws ModelConstructionError 
	 */
	public PdbFtpModel(String pdbCode, String pdbChainCode, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep) throws ModelConstructionError {
		String gzCifFileName = pdbCode+".cif.gz";
		File gzCifFile = new File(Start.TEMP_DIR,gzCifFileName);
		gzCifFile.deleteOnExit();
		File cifFile = new File(Start.TEMP_DIR,pdbCode + ".cif");
		cifFile.deleteOnExit();
		String cifFileName = cifFile.getAbsolutePath();
		
		// getting gzipped cif file from ftp
		try {
			URL url = new URL(Start.PDB_FTP_URL+gzCifFileName);
			URLConnection urlc = url.openConnection();
			InputStream is = urlc.getInputStream();
			FileOutputStream os = new FileOutputStream(gzCifFile);
			int b;
			while ( (b=is.read())!=-1) {
				os.write(b);
			}
			is.close();
			os.close();
		} catch (IOException e) {
			System.err.println("Error while reading from ftp site "+Start.PDB_FTP_URL);
			throw new ModelConstructionError(e.getMessage());
		}
		
		// unzipping downloaded file
		try {
			GZIPInputStream zis = new GZIPInputStream(new FileInputStream(gzCifFile));
			FileOutputStream os = new FileOutputStream(cifFileName);
			int b;
			while ( (b=zis.read())!=-1) {
				os.write(b);
			}
			zis.close();
			os.close();
		} catch (IOException e) {
			System.err.println("Error while unzipping the cif.gz file.");
			throw new ModelConstructionError(e.getMessage());
		}
		
		// load CIF file
		try {
			this.pdb = new CiffilePdb(cifFileName, pdbChainCode);
			this.graph = pdb.get_graph(edgeType, distCutoff);

			super.writeTempPdbFile();
			super.initializeContactMap();
			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(pdbChainCode);
			super.checkAndAssignSecondaryStructure();
			
		} catch (IOException e) {
			System.err.println("Error while reading from CIF file.");
			throw new ModelConstructionError(e.getMessage());
		} catch (CiffileFormatError e){
			System.err.println("Failed to load structure from CIF file. Wrong file format");
			throw new ModelConstructionError(e.getMessage());		
		} catch (PdbChainCodeNotFoundError e){
			System.err.println("Failed to load structure. Chain code not found in CIF file.");
			throw new ModelConstructionError(e.getMessage());
		}

				
	}
	
}
