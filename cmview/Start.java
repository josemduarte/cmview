package cmview;
import java.io.*;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import tools.PymolServerOutputStream;

import cmview.datasources.Model;
import cmview.datasources.ModelConstructionError;
import cmview.datasources.PdbaseModel;

/**
 * Main class to start contact map viewer application.
 * Contains static main function, constants and some helper functions.
 * 
 * @author		Juliane Dinse
 * @author		Henning Stehr
 * @author		Jose Duarte 
 * Class: 		Start
 * Package: 	cmview
 * Date:		20/02/2007, updated: 12/06/2007
 */

public class Start {

	static final long serialVersionUID = 1l;
	
	/* Constants, TODO: Move to config file */
	
	// internal constants (not user changable)
	public static final String      VERSION = "0.8";				// current version
	public static final String		NULL_CHAIN_CODE = 	"NULL"; 	// value important for Msdsd2Pdb
	public static final String		RESOURCE_DIR = "/resources/"; 	// path within the jar archive where resources are located 

	// environment (set by install script?)
	public static final String		TEMP_DIR = System.getProperty("java.io.tmpdir"); // TODO: Check this on Unix/Win/MacOS
	
	// user customizations
	public static final int			INITIAL_SCREEN_SIZE = 800;	// initial size of the contactMapPane in pixels
	public static boolean			USE_DATABASE = true; 		// if false, all functions involving a database will be hidden 
	public static boolean			USE_PYMOL = true;			// if false, all pymol specific functionality will be hidden
	public static boolean			PRELOAD_PYMOL = true; 		// if true, pymol is preloaded on startup

	// pymol connection
	public static final String      HOST = 				"localhost";
	public static final String		PYMOL_SERVER_URL = 	"http://"+HOST+":9123";
	public static final String		DEFAULT_GRAPH_DB =	"pdb_reps_graph"; 								// shown in load from graph db dialog
	public static final String		PYMOL_CMD = 		"/project/StruPPi/bin/pymol-1.0 -R -q"; 		// TODO: make this customizable, i.e. portable
	public static final String 		PYMOLFUNCTIONS_SCRIPT = "graph.py";
	
	// default values
	private static final String     DEFAULT_EDGETYPE = "ALL";
	private static final String     DEFAULT_PDB_DB   = "pdbase";
	private static final int        DEFAULT_MIN_SEQSEP   = -1;
	private static final int        DEFAULT_MAX_SEQSEP   = -1;	
	private static double 			DEFAULT_DISTANCE_CUTOFF = 4.1; // used by main function to preload graph from pdb/chain id
	private static final String     DEFAULT_PDBCODE = "1tdr"; // only for testing database connection
	private static final String     DEFAULT_CHAINCODE   = "B";    // only for testing database connection
	
	// internal status variables
	protected static boolean		database_found = true;
	protected static boolean		pymol_found = true;
	
	/** Copy external resources (data files and executables) from the jar archive to a temp directory.
	 * The files are marked to be deleted on exit. */
	private static void unpackResources() {
		
		unpackResource(PYMOLFUNCTIONS_SCRIPT);
        
	}	
	
	/** Copy the resource file 'resource' from the resource dir in the jar file to the temp directory */
	private static void unpackResource(String resource) {
		
		String source = RESOURCE_DIR + resource;
		File target = new File(TEMP_DIR, resource);

		try{
			try {
				target.createNewFile();
			} catch (IOException e) {
				System.err.println("Failed to create file " + target);
				throw e;
			}

			InputStream inp = Runtime.getRuntime().getClass().getResourceAsStream(source);
			BufferedInputStream in = new BufferedInputStream(inp);
			FileOutputStream out = new FileOutputStream(target);

			try {
				int ch;            
				while((ch = in.read()) != -1)
					out.write(ch);
				in.close();
				out.close();
			} catch (IOException e) {
				System.err.println("Failed to read from " + source);
				throw e;
			}
		}
		catch(IOException e) {
			System.err.println("Severe error: Failed to create resource " + target);
		}
		finally {
			target.deleteOnExit();
		}
        
	}
	
	/** Return the absolute path to the unpacked resource with the given name.
	 * Unpacking has to be done using unpackResource() */
	protected static String getResourcePath(String resource) {
		return new File(TEMP_DIR, resource).getAbsolutePath();
	}
	
	/** Set native host look and feel (is possible) */
	private static void setLookAndFeel() {
		try {
		    // Set System L&F
	        UIManager.setLookAndFeel(
	            //looks[2].getClassName());
	        	UIManager.getSystemLookAndFeelClassName());
	    } 
	    catch (UnsupportedLookAndFeelException e) {
	       System.out.println(e);
	    }
	    catch (ClassNotFoundException e) {
		       System.out.println(e);	       // handle exception
	    }
	    catch (InstantiationException e) {
		       System.out.println(e);	       // handle exception
	    }
	    catch (IllegalAccessException e) {
		       System.out.println(e);	       // handle exception
	    }
	}
	
	/**
	 * Runs external pymol executable if possible. Return true on success.
	 */
	private static boolean runPymol() {
		try {
			System.out.println("Starting PyMol...");
//			File f = new File(Start.PYMOL_CMD);
//			if(!f.exists()) {
//				System.err.println(PYMOL_CMD + " does not exist.");
//				return false;
//			}
			Process pymolProcess = Runtime.getRuntime().exec(Start.PYMOL_CMD);
			// TODO: catch output and wait until pymol is loaded
//			BufferedInputStream in = new BufferedInputStream(pymolProcess.getInputStream());
//			try {
//				int ch;            
//				while((ch = in.read()) != -1)
//					System.out.write(ch);
//				in.close();
//			} catch (IOException e) {
//				System.err.println("Failed to read from pymol");
//				throw e;
//			}
		} catch(IOException e) {
//			System.err.println("Failed to run " + PYMOL_CMD);
			return false;
		}
		return true;				
	}
	
	/**
	 * Try connecting to pymol server. Returns true on success.
	 * TODO: Make this a (static?) method of PymolAdaptor.
	 */
	private static boolean tryConnectingToPymol() {
		try {
			OutputStream test = new PymolServerOutputStream(PYMOL_SERVER_URL);
			test.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Try connecting to the database server. Returns true on success.
	 * TODO: Make this a (static?) method of Model.
	 */
	private static boolean tryConnectingToDb() {
		try {
			new PdbaseModel(DEFAULT_PDBCODE,DEFAULT_CHAINCODE,DEFAULT_EDGETYPE,DEFAULT_DISTANCE_CUTOFF, DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP, DEFAULT_PDB_DB);
		}
		catch(Exception e) {
			return false;
		}
		return true;
	}	
	
	/**
	 * Preload a model based on the command line parameters.
	 * Returns the model or null on failure.
	 */
	private static Model preloadModel(String[] args) {
		Model mod = null;
		// parameters should be pdb code and chain code
		String pdbCode = args[0];
		String chainCode;
		if(args.length > 1) {
			chainCode = args[1];
		} else {
			chainCode = NULL_CHAIN_CODE;
		}
		try {
			mod = new PdbaseModel(pdbCode,chainCode,DEFAULT_EDGETYPE,DEFAULT_DISTANCE_CUTOFF, DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP, DEFAULT_PDB_DB);
		} catch(ModelConstructionError e) {
			System.err.println("Could not load structure for given command line parameters:");
			System.err.println(e.getMessage());
		}			
		return mod;
	}
		
	public static void main(String args[]){
		
		System.out.println("CMView - Interactive contact map viewer");
		setLookAndFeel();
		System.out.println("Using temporary directory " + TEMP_DIR);
		unpackResources();
		
		if(USE_PYMOL) {
			if(PRELOAD_PYMOL) {
				if(runPymol() == false) {
					//System.err.println("Warning: Failed to start PyMol automatically. Please manually start Pymol with the -R parameter.");	
					System.err.println("Warning: Failed to start PyMol automatically. You can try to restart this application after manually starting pymol with the -R parameter.");
				}
			}
			if(tryConnectingToPymol() == false) {
				System.err.println("No PyMol server found. Some functionality will not be available.");
				pymol_found = false;
			}
		}
		
		if(USE_DATABASE) {
			if(tryConnectingToDb() == false) {
				System.err.println("No database found. Some functionality will not be available.");
				database_found = false;
			}
		}
					
		// start myself without a model or preload contact map based on command line parameters
		String wintitle = "Contact Map Viewer";
		Model mod = null;
		View view = new View(mod, wintitle, Start.PYMOL_SERVER_URL);
		if (args.length>=1){
			mod = preloadModel(args);
			if(mod != null) {
				view.spawnNewViewWindow(mod);
			}
		}
	}

}
