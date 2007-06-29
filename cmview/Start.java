package cmview;
import java.io.*;
import java.util.Date;
import java.util.Properties;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import tools.MySQLConnection;
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
	
	// internal constants (not user changable)
	public static final String		APP_NAME = "CM2PyMol";			// name of this application
	public static final String      VERSION = "0.8.1";				// current version
	public static final String		NULL_CHAIN_CODE = 	"NULL"; 	// for input pdbChainCodes
	public static final String		RESOURCE_DIR = "/resources/"; 	// path within the jar archive where resources are located
	public static final String 		PYMOLFUNCTIONS_SCRIPT = "cmview.py";	 // extending pymol with custom functions, previously called graph.py
	public static final String		PYMOL_CALLBACK_FILE = "cmview.callback"; // file being written by pymol to send messages to this application
	public static final String      PYMOL_HOST = 			"localhost";							
	public static final String		PYMOL_SERVER_URL = 		"http://"+PYMOL_HOST+":9123";
	
	// configurable constants, TODO: Move to config file	
	public static String			CONFIG_FILE_NAME = "cmview.cfg";	// default name of config file (can be overridden by cmd line param)
	
	// user customizations
	public static String			TEMP_DIR = System.getProperty("java.io.tmpdir"); // TODO: Check this on Unix/Win/MacOS
	public static int				INITIAL_SCREEN_SIZE = 800;			// initial size of the contactMapPane in pixels
	public static boolean			USE_DATABASE = true; 				// if false, all functions involving a database will be hidden 
	public static boolean			USE_PYMOL = true;					// if false, all pymol specific functionality will be hidden
	public static boolean			PRELOAD_PYMOL = true; 				// if true, pymol is preloaded on startup

	// pymol connection
	public static String			PYMOL_EXECUTABLE = 		"/project/StruPPi/bin/pymol-1.0"; 		
	public static String			PYMOL_PARAMETERS =  	"-R -q";
	public static long 				PYMOL_CONN_TIMEOUT = 	10000; 					// pymol connection time out in milliseconds
	
	// database connection
	public static String			DB_HOST = "white";
	public static String			DB_USER = getUserName();
	public static String			DB_PWD = "nieve";
	
	// default values for loading contact maps
	public static final String		DEFAULT_GRAPH_DB =			"pdb_reps_graph"; 	// shown in load from graph db dialog
	public static String     		DEFAULT_PDB_DB = 			"pdbase";
	public static String			DEFAULT_MSDSD_DB =			"msdsd_00_07_a";
	public static String     		DEFAULT_CONTACT_TYPE = 		"ALL";
	private static final int        DEFAULT_MIN_SEQSEP = 		-1;
	private static final int        DEFAULT_MAX_SEQSEP = 		-1;	
	public static double 			DEFAULT_DISTANCE_CUTOFF = 	4.1; 				// used by main function to preload graph from pdb/chain id
	
	// internal status variables
	protected static boolean		database_found = true;
	protected static boolean		pymol_found = true;
	protected static Properties		currentProperties;
	protected static Properties		defaultProperties;
	private static MySQLConnection	conn;
	
	/** 
	 * Get user name from operating system (for use as database username). 
	 * */
	private static String getUserName() {
		String user = null;
		user = System.getProperty("user.name");
		if(user == null) {
			System.err.println("Could not get user name from operating system.");
		}
		return user;
	}
		
	/**
	 * Returns a property object with the default values for all customizable variables.
	 * TODO: Read this from another (hidden) config file
	 */
	private static Properties getDefaultProperties() {

		Properties d = new Properties();
		d.setProperty("TEMP_DIR",TEMP_DIR);
		d.setProperty("INITIAL_SCREEN_SIZE",new Integer(INITIAL_SCREEN_SIZE).toString());
		d.setProperty("USE_DATABASE",new Boolean(USE_DATABASE).toString());
		d.setProperty("USE_PYMOL",new Boolean(USE_PYMOL).toString());
		d.setProperty("PRELOAD_PYMOL",new Boolean(PRELOAD_PYMOL).toString());
		
		d.setProperty("PYMOL_PARAMETERS",PYMOL_PARAMETERS);
		d.setProperty("PYMOL_EXECUTABLE",PYMOL_EXECUTABLE);		
		d.setProperty("PYMOL_CONN_TIMEOUT",new Long(PYMOL_CONN_TIMEOUT).toString());
		
		d.setProperty("DB_HOST",DB_HOST);
		d.setProperty("DB_USER",DB_USER);
		d.setProperty("DB_PWD",DB_PWD);

		d.setProperty("DEFAULT_PDB_DB",DEFAULT_PDB_DB);
		d.setProperty("DEFAULT_CONTACT_TYPE",DEFAULT_CONTACT_TYPE);
		d.setProperty("DEFAULT_DISTANCE_CUTOFF",new Double(DEFAULT_DISTANCE_CUTOFF).toString());
		
		return d;
	}

	/**
	 * Loads user properties from the given configuration file using the given properties as default values.
	 * Returns null on failure;
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	private static Properties loadUserProperties(String fileName, Properties defaultProperties) throws FileNotFoundException, IOException {
		Properties p = new Properties(defaultProperties);
		p.load(new FileInputStream(fileName));
		return p;
	}

	/**
	 * Overwrite the local constants with the values from the given properties object
	 */
	private static void applyUserProperties(Properties p) {

		TEMP_DIR = p.getProperty("TEMP_DIR");
		INITIAL_SCREEN_SIZE = Integer.valueOf(p.getProperty("INITIAL_SCREEN_SIZE"));
		USE_DATABASE = Boolean.valueOf(p.getProperty("USE_DATABASE"));
		USE_PYMOL = Boolean.valueOf(p.getProperty("USE_PYMOL"));
		PRELOAD_PYMOL = Boolean.valueOf(p.getProperty("PRELOAD_PYMOL"));
		
		PYMOL_PARAMETERS = p.getProperty("PYMOL_PARAMETERS");
		PYMOL_EXECUTABLE = p.getProperty("PYMOL_EXECUTABLE");		
		PYMOL_CONN_TIMEOUT = Long.valueOf(p.getProperty("PYMOL_CONN_TIMEOUT"));
		
		DB_HOST = p.getProperty("DB_HOST");
		DB_USER = p.getProperty("DB_USER");
		DB_PWD = p.getProperty("DB_PWD");

		DEFAULT_PDB_DB = p.getProperty("DEFAULT_PDB_DB");
		DEFAULT_CONTACT_TYPE = p.getProperty("DEFAULT_CONTACT_TYPE");
		DEFAULT_DISTANCE_CUTOFF = Double.valueOf(p.getProperty("DEFAULT_DISTANCE_CUTOFF"));
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
			File f = new File(PYMOL_EXECUTABLE);
			if(!f.exists()) {
				System.err.println(PYMOL_EXECUTABLE + " does not exist.");
				return false;
			}
			Runtime.getRuntime().exec(PYMOL_EXECUTABLE + " " + PYMOL_PARAMETERS);
		} catch(IOException e) {
			return false;
		}
		return true;				
	}
	
	/**
	 * Try connecting to pymol server. Returns true on success.
	 * TODO: Make this a (static?) method of PymolAdaptor.
	 */
	private static boolean tryConnectingToPymol(long timeoutMillis) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis()-start < timeoutMillis) {
			try {
				String cmd;
				File f = new File(Start.getResourcePath(Start.PYMOL_CALLBACK_FILE));
				OutputStream test = new PymolServerOutputStream(PYMOL_SERVER_URL);
				cmd = "run "+Start.getResourcePath(Start.PYMOLFUNCTIONS_SCRIPT);
				test.write(cmd.getBytes());
				test.flush();
				cmd = "callback "+Start.getResourcePath(Start.PYMOL_CALLBACK_FILE) + ", " + new Date();
				test.write(cmd.getBytes());
				test.flush();
				test.close();
				if(f.exists()) {
					f.deleteOnExit();
					return true;
				} else continue;
			} catch (Exception e) {
				continue;
			}
		}
		return false;
	}
	
	/**
	 * Try connecting to the database server. Returns true on success.
	 */
	private static boolean tryConnectingToDb() {
		try {
			conn = new MySQLConnection(DB_HOST, DB_USER, DB_PWD);
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
		if(USE_DATABASE && database_found) {
			String pdbCode = args[0];
			String chainCode;
			if(args.length > 1) {
				chainCode = args[1];
			} else {
				chainCode = NULL_CHAIN_CODE;
			}
			try {
				mod = new PdbaseModel(pdbCode,chainCode, DEFAULT_CONTACT_TYPE, DEFAULT_DISTANCE_CUTOFF, DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP, DEFAULT_PDB_DB);
			} catch(ModelConstructionError e) {
				System.err.println("Could not load structure for given command line parameters:");
				System.err.println(e.getMessage());
			}			
		} else {
			System.err.println("No database. Ignoring command line parameters.");
		}
		return mod;
	}
	
	private static boolean dirIsWritable(String path) {
		File f = new File(path);
		if(!f.isDirectory()) return false;
		if(!f.canWrite()) return false;
		return true;
	}

	/*---------------------------- public methods ---------------------------*/

	/**
	 * Writes a configuration file with default values for all customizable variables.
	 * Note that the variable defaultProperties has to be initialized previously.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void writeDefaultConfigToFile(String fileName) throws FileNotFoundException, IOException {
		Properties d = defaultProperties;
		String comment = "Properties file for " + APP_NAME + " " + VERSION;
		d.store(new FileOutputStream(fileName), comment);
	}
	
	/**
	 * Returns true if a database connection is expected to be available. This is to avoid
	 * trying to connect when it is clear that the trial will fail.
	 */
	public static boolean isDatabaseConnectionAvailable() {
		return Start.USE_DATABASE && Start.database_found;
	}
	
	/**
	 * Returns true if a connection to pymol is expected to be available. This is to avoid
	 * trying to connect when it is clear that the trial will fail.
	 */
	public static boolean isPyMolConnectionAvailable() {
		return Start.USE_PYMOL && Start.pymol_found;
	}

	public static MySQLConnection getDbConnection() {
		return conn;
	}
	
	public static void main(String args[]){
		
		System.out.println("CMView - Interactive contact map viewer");
		
		// TODO: check whether config file is passed as command line parameter, otherwise use default one
		
		// load configuration
		defaultProperties = getDefaultProperties();
		try {
			Properties p = loadUserProperties(CONFIG_FILE_NAME, defaultProperties);
			System.out.println("Loading configuration file " + CONFIG_FILE_NAME);
			currentProperties = p;
			applyUserProperties(currentProperties);
		} catch (FileNotFoundException e) {
			System.out.println("No configuration file found. Using default settings.");
		} catch (IOException e) {
			System.err.println("Error while reading from file " + CONFIG_FILE_NAME + ". Using default settings.");
		}
				
		setLookAndFeel();
		System.out.println("Using temporary directory " + TEMP_DIR);
		if(dirIsWritable(TEMP_DIR)) {
			unpackResources();
		} else {
			System.err.println("Error: Can not write to temporary directory. Some features may not function correctly.");
		}
		
		if(USE_PYMOL) {		
			if(tryConnectingToPymol(100) == true) { // running pymol server found
				System.out.println("PyMol server found. Connected.");
				pymol_found = true;
			} else {
				if(PRELOAD_PYMOL) {
					if(runPymol() == false) {
						//System.err.println("Warning: Failed to start PyMol automatically. Please manually start Pymol with the -R parameter.");	
						System.err.println("Failed. (You can try to restart this application after manually starting pymol with the -R parameter)");
						pymol_found = false;
					} else {
						System.out.println("Connecting to PyMol server...");
						if(tryConnectingToPymol(PYMOL_CONN_TIMEOUT) == false) {
							System.err.println("Failed. (You can try to restart this application after manually starting pymol with the -R parameter)");
							pymol_found = false;
						} else {
							System.out.println("Connected.");
							pymol_found = true;
						}						
					}
				}
			}
			if(!pymol_found) {
				System.err.println("Could not connect to PyMol server. Some functionality will not be available.");
			}
		}
		
		if(USE_DATABASE) {
			System.out.println("Connecting to database...");
			if(tryConnectingToDb() == false) {
				System.err.println("No database found. Some functionality will not be available.");
				database_found = false;
			} else {
				System.out.println("Connected.");
				database_found = true;
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
