package cmview;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.*;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

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
	public static final String      HOST = 				getHostName() ;
	public static final String		PYMOL_SERVER_URL = 	"http://"+HOST+":9123";
	public static final String		DEFAULT_GRAPH_DB =	"pdb_reps_graph"; 								// shown in load from graph db dialog
	public static final String		PYMOL_CMD = 		"/project/StruPPi/PyMolAll/pymol/pymol.exe -R -q"; // TODO: make this customizable, i.e. portable
	public static final String 		PYMOLFUNCTIONS_SCRIPT = "graph.py";
	
	// default values
	private static final String     DEFAULT_EDGETYPE = "ALL";
	private static final String     DEFAULT_PDB_DB   = "pdbase";
	private static final int        DEFAULT_MIN_SEQSEP   = -1;
	private static final int        DEFAULT_MAX_SEQSEP   = -1;	
	private static double 			DEFAULT_DISTANCE_CUTOFF = 4.1; // used by main function to preload graph from pdb/chain id

	/** get host name from operating system (to locate pymol server) */
	private static String getHostName() {
		String host="";
		try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			System.err.println("Couldn't get host name. Exiting");
			System.exit(1);
		}
		return host;
	}

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

			//InputStream inp = ClassLoader.getSystemResourceAsStream(source);
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
		
	public static void main(String args[]){
		
		System.out.println("CMView - Interactive contact map viewer");
		setLookAndFeel();
		System.out.println("Using temporary directory " + TEMP_DIR);
		unpackResources();
		
		if(USE_PYMOL && PRELOAD_PYMOL) {
			// start pymol
			try {
				System.out.println("Starting PyMol...");
				// TODO: check whether pymol is running already
				Process pymolProcess = Runtime.getRuntime().exec(Start.PYMOL_CMD);
				if(pymolProcess == null) {
					throw new IOException("pymolProcess Object is null");
				}
				// TODO: catch output and wait until pymol is loaded
			} catch(IOException e) {
				System.err.println("Warning: Couldn't start PyMol automatically. Please manually start Pymol with the -R parameter.");
			}
		}
					
		// start myself without a model or take pdbCode and chainCode from command line and default values
		String wintitle = "Contact Map Viewer";
		Model mod = null;
		View view = new View(mod, wintitle, Start.PYMOL_SERVER_URL);
		if (args.length>=1 && USE_DATABASE){
			// TODO: guess file type and perform corresponding load operation
			String pdbCode = args[0];
			String chainCode = NULL_CHAIN_CODE;
			if (args.length==2) chainCode = args[1];
			try {
				mod = new PdbaseModel(pdbCode,chainCode,DEFAULT_EDGETYPE,DEFAULT_DISTANCE_CUTOFF, DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP, DEFAULT_PDB_DB);
				view.spawnNewViewWindow(mod);
			} catch(ModelConstructionError e) {
				System.err.println("Could not load structure for given command line parameters:");
				System.err.println(e.getMessage());
			}
		}
	}

}
