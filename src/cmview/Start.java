package cmview;
import gnu.getopt.Getopt;

import java.io.*;
import java.sql.SQLException;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import proteinstructure.PdbCodeNotFoundError;

import tools.MySQLConnection;

import cmview.datasources.Model;
import cmview.datasources.ModelConstructionError;
import cmview.datasources.PdbFileModel;
import cmview.datasources.PdbFtpModel;
import cmview.datasources.PdbaseModel;

/**
 * Main class to start contact map viewer application.
 * Contains static main function, constants and some helper functions.
 */
public class Start {

	/*------------------------------ constants ------------------------------*/
	
	static final long serialVersionUID = 1l;

	/* internal constants (not user changeable) */
	public static final String		APP_NAME = 				"CMView";			// name of this application
	public static final String		VERSION = 				"0.9.3";			// current version of this application (should match manifest)
	public static final String		NULL_CHAIN_CODE = 		"NULL"; 			// used by Pdb/Graph objects for the empty pdbChainCode
	public static final int			NO_SEQ_SEP_VAL =		-1;					// default seq sep value indicating that no seq sep has been specified
	public static final String		NO_SEQ_SEP_STR =		"none";				// text output if some seqsep variable equals NO_SEQ_SEP_VAL
	public static final String		RESOURCE_DIR = 			"/resources/"; 		// path within the jar archive where resources are located
	public static final String		HELPSET =               "/resources/help/jhelpset.hs"; // the path to the inline help set
	public static final String		ICON_DIR = 				"/resources/icons/";	// the directory containing the icons
	
	/*---------------------------- configuration ---------------------------*/
	
	/*
	  Configuration variables
	  
	  The following 'constants' can be overwritten by settings in config files. In the code, they are being used as if they were (final) constants
	  and the only time when they may change is during startup. Note that each variable that ought to be user changeable, i.e. read from cfg file
	  needs to be processed in both in the 'applyUserProperties' and the 'getLocalProperties' method. The preassigned values are the default and
	  being used if no value was found in the config files.
	  Additionally, values that should appear in the example config file should be added to the getSelectedProperties method.
	 
	  Initialization

	  Initialization of these properties happends in the following order:
	  1a. 	Initialize most properties to hard-coded default values (in case that master config file is missing)
	  1b.	Initialize some properties to values specified at runtime (e.g. temp dir, user name)
	  2.	Load all properties except 1b from master config file (in resources), this defines the 'release configuration'
	  3.	Load config files from the following locations, possibly overwriting previous settings:
	  3a.	Current directory
	  3b.	User's home directory
	  3c.	Location specififed by command line parameter
	*/
	
	/* environment (should be set at runtime and not in master config file but can be overwritten in user config file) */
	public static String			TEMP_DIR = System.getProperty("java.io.tmpdir");
	public static String			DB_USER = getUserName();						// guess user name

	/* internal settings */
	public static String			CONFIG_FILE_NAME = "cmview.cfg";	// default name of config file
	public static String			DIST_MAP_CONTACT_TYPE = "Ca";		// contact type to be used for distance map calculation (only single atom type allowed)
	public static String 			PDB_FTP_URL = "ftp://ftp.wwpdb.org/pub/pdb/data/structures/all/mmCIF/";
	// TODO: alignment parameters
	// TODO: DSSP three or four states
	
	/* gui settings */
	public static int				INITIAL_SCREEN_SIZE = 800;			// initial size of the contactMapPane in pixels
	public static boolean			SHOW_RULERS = true;					// if true, rulers will be shown by default
	public static boolean			SHOW_ICON_BAR = true;				// if true, icon bar is used
	public static boolean 			SHOW_ALIGNMENT_COORDS = false;		// if true, alignment coordinates also shown in bottom left corner of contact map
	public static boolean 			SHOW_PDB_RES_NUMS = true;			// if true, pdb residue numbers also shown in bottom left corner of contact map

	/* enable/disable features */
	public static boolean			USE_DATABASE = true; 				// if false, all functions involving a database will be hidden 
	public static boolean			USE_PYMOL = true;					// if false, all pymol specific functionality will be hidden
	public static boolean			USE_DSSP = true;					// if true, secondary structure will be always taken from DSSP (if available)
	public static boolean           USE_EXPERIMENTAL_FEATURES = false; 	// this flag shall indicate strongly experimental stuff, use it to disable features in release versions
																		// currently: common nbh related things, directed graph	
	/* external programs: dssp */
	public static String			DSSP_EXECUTABLE = ""; 				
	public static String			DSSP_PARAMETERS = "--";	
	
	/* external programs: pymol */
	public static String			PYMOL_HOST = 			 "localhost"; // currently, the XMLRPC server in Pymol only supports localhost
	public static String			PYMOL_PORT =			 "9123";	  // default port, if port is blocked, pymol will increase automatically
	public static String			PYMOL_SERVER_URL = 		 "http://"+PYMOL_HOST+":"+PYMOL_PORT;
	public static String			PYMOL_EXECUTABLE = 		 ""; 		  // to start pymol automatically
	public static String			PYMOL_LOGFILE =			 TEMP_DIR + File.separator + "CMView_pymol.log";
	public static String			PYMOL_CMDBUFFER_FILE =	 TEMP_DIR + File.separator + "CMView_pymol.cmd";
	public static String			PYMOL_PARAMETERS =  	 "-R -q -s " + PYMOL_LOGFILE; // run xmlrpc server and skip splash screen
	public static long 				PYMOL_CONN_TIMEOUT = 	 15000; 	  // pymol connection time out in milliseconds
	public static boolean			PYMOL_LOAD_ON_START =    true; 		  // if true, pymol will be preloaded on startup
	public static boolean			PYMOL_SHUTDOWN_ON_EXIT = true;		  // if true, pymol will be shut down on exit

	/* database connection */
	public static String			DB_HOST = "localhost";							
	public static String			DB_PWD = "tiger";
	
	/* default values for loading contact maps */
	public static String			DEFAULT_GRAPH_DB =			""; 				// shown in load from graph db dialog
	public static String     		DEFAULT_PDB_DB = 			"";					// for loading from command line
	public static String			DEFAULT_MSDSD_DB =			"";					// used when loading structures for cm file graphs
	
	public static String     		DEFAULT_CONTACT_TYPE = 		"Ca";				// loading from command line and shown in LoadDialog
	public static double 			DEFAULT_DISTANCE_CUTOFF = 	8.0; 				// dito
	private static int        		DEFAULT_MIN_SEQSEP = 		NO_SEQ_SEP_VAL;		// dito
	private static int        		DEFAULT_MAX_SEQSEP = 		NO_SEQ_SEP_VAL;		// dito
	
	/*--------------------------- member variables --------------------------*/

	// global session variables (use getter methods)
	private static boolean			database_found = false;
	private static boolean			pymol_found = false;
	private static boolean			dssp_found = false;
	
	private static MySQLConnection 	conn;
	private static JFileChooser 	fileChooser;
	private static JColorChooser 	colorChooser;
	private static PyMolAdaptor 	pymolAdaptor;
	private static int 				viewInstances = 0;		// for counting instances of view class (=open windows)
	
	// the thread pool
	private static ThreadPoolExecutor threadPool =  (ThreadPoolExecutor) Executors.newCachedThreadPool();
	
	// mapping pdb-code to mmCIF files in the tmp-directory, only to be used for ftp loading
	private static TreeMap<String, File> pdbCode2file = new TreeMap<String, File>();
	
	// map of loadedGraphIDs (see member in Model) to original user-loaded Models (the members of View)
	private static TreeMap<String, Model> loadedGraphs = new TreeMap<String, Model>();
		
	/**
	 * Increase the counter of view instances.
	 * @return the new number of view instances after increasing the count.
	 */
	protected static int viewInstancesCreated() {
		return ++viewInstances;
	}
	
	/**
	 * Decreases the counter of view instances.
	 * @return the new number of view instances after decreasing the count.
	 */
	protected static int viewInstanceDisposed() {
		return --viewInstances;
	}
	
	/**
	 * Cleans up and exits CMView.
	 * @param exitCode the exit code to return to the operating system
	 */
	protected static void shutDown(int exitCode) {
		// Note that the shutdown hook of the virtual machine will still be executed.
		System.exit(exitCode);
	}
	
	/**
	 * Sets the loadedGraphID and returns it, also putting it to the loadedGraphs map
	 * @param name
	 * @param mod
	 * @return
	 */
	public static String setLoadedGraphID(String name, Model mod) {
		String id = name;
		if (loadedGraphs.containsKey(name)) {
			int idSerial = 1;
			while (loadedGraphs.containsKey(name+"_"+idSerial)) {
				idSerial++;
			}
			id = name+"_"+idSerial;
		}
		loadedGraphs.put(id, mod);
		return id;
	}
	
	/**
	 * Gets the filename of the local copy of the structure file corresponding 
	 * to the given pdb code. 
	 * @param pdbCode  pdb code
	 * @return  path to the file of the given pdb code. Returns null if there 
	 *  is no such file. 
	 */
	public static File getFilename2PdbCode(String pdbCode) {
		return pdbCode2file.get(pdbCode.toLowerCase());
	}
	
	/**
	 * Sets the name of the local copy of the structure file corresponding to 
	 * the given pdb code.
	 * @param pdbCode  pdb code
	 * @param filename  name of the file corresponding to <code>pdbCode</code>
	 */
	public static void setFilename2PdbCode(String pdbCode, File file) {
		pdbCode2file.put(pdbCode.toLowerCase(), file);
	}	
	
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
	
	/*-------------- properties and config files --------------*/
	
	/**
	 * Loads user properties from the given configuration file.
	 * Returns null on failure;
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	private static Properties loadConfigFile(String fileName) throws FileNotFoundException, IOException {
		Properties p = new Properties();
		p.load(new FileInputStream(fileName));
		return p;
	}
	
	/**
	 * Writes the given properties object to a file using the default property file format.
	 * @param p the properties object to be written to file
	 * @param fileName the output file
	 * @param comments the comment header to be written to the file
	 * @throws IOException if writing to the output file failed
	 */
	private static void saveConfigFile(Properties p, String fileName) throws IOException {
		String comments = "Properties file for " + APP_NAME + " " + VERSION;
		p.store(new FileOutputStream(fileName), comments);
	}
	
	/**
	 * Writes an example configuration file with the default values for selected user
	 * customizable variables. The file will be written to the current directory.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void writeExampleConfigFile(String fileName) throws FileNotFoundException, IOException {
		Properties p = new Properties();
		
		p.setProperty("PYMOL_EXECUTABLE",PYMOL_EXECUTABLE);
		p.setProperty("DSSP_EXECUTABLE", DSSP_EXECUTABLE);
		p.setProperty("DEFAULT_CONTACT_TYPE",DEFAULT_CONTACT_TYPE);
		p.setProperty("DEFAULT_DISTANCE_CUTOFF",new Double(DEFAULT_DISTANCE_CUTOFF).toString());
		
		saveConfigFile(p, fileName);
	}

	/**
	 * Overwrite the local constants with the values from the given properties object
	 */
	private static void applyUserProperties(Properties p) {

		/* The logic here is: First, take the value from the user config file,
		   if that is not found, keep the variable value unchanged.
		   Note that any value in the user config file that is not being processed here is ignored. 
		*/
		
		TEMP_DIR = p.getProperty("TEMP_DIR",TEMP_DIR);
		CONFIG_FILE_NAME = p.getProperty("CONFIG_FILE_NAME", CONFIG_FILE_NAME);
		PDB_FTP_URL = p.getProperty("PDB_FTP_URL", PDB_FTP_URL);
		DIST_MAP_CONTACT_TYPE = p.getProperty("DIST_MAP_CONTACT_TYPE",DIST_MAP_CONTACT_TYPE);

		// gui settings
		INITIAL_SCREEN_SIZE = Integer.valueOf(p.getProperty("INITIAL_SCREEN_SIZE", new Integer(INITIAL_SCREEN_SIZE).toString()));
		SHOW_RULERS = Boolean.valueOf(p.getProperty("SHOW_RULERS", new Boolean(SHOW_RULERS).toString()));
		SHOW_ICON_BAR = Boolean.valueOf(p.getProperty("SHOW_ICON_BAR",Boolean.toString(SHOW_ICON_BAR)));
		SHOW_ALIGNMENT_COORDS = Boolean.valueOf(p.getProperty("SHOW_ALIGNMENT_COORDS",Boolean.toString(SHOW_ALIGNMENT_COORDS)));
		SHOW_PDB_RES_NUMS = Boolean.valueOf(p.getProperty("SHOW_PDB_RES_NUMS",Boolean.toString(SHOW_PDB_RES_NUMS)));

		// enabling/disabling features
		USE_DATABASE = Boolean.valueOf(p.getProperty("USE_DATABASE", new Boolean(USE_DATABASE).toString()));
		USE_PYMOL = Boolean.valueOf(p.getProperty("USE_PYMOL", new Boolean(USE_PYMOL).toString()));
		USE_EXPERIMENTAL_FEATURES = Boolean.valueOf(p.getProperty("USE_EXPERIMENTAL_FEATURES", new Boolean(USE_EXPERIMENTAL_FEATURES).toString()));
		USE_DSSP = Boolean.valueOf(p.getProperty("USE_DSSP", new Boolean(USE_DSSP).toString()));
		
		// external programs: pymol
		PYMOL_HOST = p.getProperty("PYMOL_HOST", PYMOL_HOST);
		PYMOL_PORT = p.getProperty("PYMOL_PORT", PYMOL_PORT);
		PYMOL_SERVER_URL = p.getProperty("PYMOL_SERVER_URL",PYMOL_SERVER_URL);
		PYMOL_EXECUTABLE = p.getProperty("PYMOL_EXECUTABLE", PYMOL_EXECUTABLE);		
		PYMOL_LOGFILE = p.getProperty("PYMOL_LOGFILE",PYMOL_LOGFILE);
		PYMOL_CMDBUFFER_FILE = p.getProperty("PYMOL_CMDBUFFER_FILE",PYMOL_CMDBUFFER_FILE);
		PYMOL_PARAMETERS = p.getProperty("PYMOL_PARAMETERS", PYMOL_PARAMETERS);
		PYMOL_CONN_TIMEOUT = Long.valueOf(p.getProperty("PYMOL_CONN_TIMEOUT",new Long(PYMOL_CONN_TIMEOUT).toString()));
		PYMOL_LOAD_ON_START = Boolean.valueOf(p.getProperty("PYMOL_LOAD_ON_START", new Boolean(PYMOL_LOAD_ON_START).toString()));
		PYMOL_SHUTDOWN_ON_EXIT = Boolean.valueOf(p.getProperty("PYMOL_SHUTDOWN_ON_EXIT", new Boolean(PYMOL_SHUTDOWN_ON_EXIT).toString()));

		// external programs: dssp
		DSSP_EXECUTABLE = p.getProperty("DSSP_EXECUTABLE",DSSP_EXECUTABLE);
		DSSP_PARAMETERS = p.getProperty("DSSP_PARAMETERS",DSSP_PARAMETERS);
		
		// database connection		
		DB_HOST = p.getProperty("DB_HOST", DB_HOST);
		DB_USER = p.getProperty("DB_USER", DB_USER);
		DB_PWD = p.getProperty("DB_PWD", DB_PWD);

		// default setting for loading contact maps
		DEFAULT_GRAPH_DB = p.getProperty("DEFAULT_GRAPH_DB", DEFAULT_GRAPH_DB);
		DEFAULT_PDB_DB = p.getProperty("DEFAULT_PDB_DB", DEFAULT_PDB_DB);
		DEFAULT_MSDSD_DB = p.getProperty("DEFAULT_MSDSD_DB", DEFAULT_MSDSD_DB);
		
		DEFAULT_CONTACT_TYPE = p.getProperty("DEFAULT_CONTACT_TYPE", DEFAULT_CONTACT_TYPE);
		DEFAULT_DISTANCE_CUTOFF = Double.valueOf(p.getProperty("DEFAULT_DISTANCE_CUTOFF", new Double(DEFAULT_DISTANCE_CUTOFF).toString()));
		DEFAULT_MIN_SEQSEP = Integer.valueOf(p.getProperty("DEFAULT_MIN_SEQSEP",Integer.toString(DEFAULT_MIN_SEQSEP)));
		DEFAULT_MAX_SEQSEP = Integer.valueOf(p.getProperty("DEFAULT_MAX_SEQSEP",Integer.toString(DEFAULT_MAX_SEQSEP)));
	}
	
	/*
	 * Returns a properties objects with all current properties. Use this for debugging and for
	 * writing the master config file. For the master file, TEMP_DIR and DB_USER should be
	 * removed afterwards because they are better initialized at runtime.
	 */
	private static Properties getCurrentProperties() {
		Properties p = new Properties();
		
		p.setProperty("TEMP_DIR", TEMP_DIR);													// doc
		p.setProperty("CONFIG_FILE_NAME", CONFIG_FILE_NAME);									// doc?
		p.setProperty("PDB_FTP_URL",PDB_FTP_URL);												// doc!		
		p.setProperty("DIST_MAP_CONTACT_TYPE",DIST_MAP_CONTACT_TYPE);							// doc?
		
		// gui settings
		p.setProperty("INITIAL_SCREEN_SIZE", Integer.toString(INITIAL_SCREEN_SIZE));			// doc
		p.setProperty("SHOW_RULERS", Boolean.toString(SHOW_RULERS));							// doc?
		p.setProperty("SHOW_ICON_BAR",Boolean.toString(SHOW_ICON_BAR));							// doc?
		p.setProperty("SHOW_ALIGNMENT_COORDS",Boolean.toString(SHOW_ALIGNMENT_COORDS));			// doc
		p.setProperty("SHOW_PDB_RES_NUMS",Boolean.toString(SHOW_PDB_RES_NUMS));					// doc?

		// feature settings
		p.setProperty("USE_DATABASE", Boolean.toString(USE_DATABASE));							// doc?
		p.setProperty("USE_PYMOL", Boolean.toString(USE_PYMOL));								// doc
		p.setProperty("USE_EXPERIMENTAL_FEATURES",  Boolean.toString(USE_EXPERIMENTAL_FEATURES));	// doc?																					
		p.setProperty("USE_DSSP",Boolean.toString(USE_DSSP));									// doc
		
		// external programs: pymol
		p.setProperty("PYMOL_HOST",PYMOL_HOST);													// doc?
		p.setProperty("PYMOL_PORT",PYMOL_PORT);													// doc?
		p.setProperty("PYMOL_SERVER_URL",PYMOL_SERVER_URL);										// doc?
		p.setProperty("PYMOL_EXECUTABLE",PYMOL_EXECUTABLE);										// doc!!!!
		p.setProperty("PYMOL_LOGFILE",PYMOL_LOGFILE);											// doc
		p.setProperty("PYMOL_CMDBUFFER_FILE",PYMOL_CMDBUFFER_FILE);								// doc???
		p.setProperty("PYMOL_PARAMETERS",PYMOL_PARAMETERS);										// doc?
		p.setProperty("PYMOL_CONN_TIMEOUT",Long.toString(PYMOL_CONN_TIMEOUT));					// doc?
		p.setProperty("PYMOL_LOAD_ON_START", Boolean.toString(PYMOL_LOAD_ON_START));			// doc?
		p.setProperty("PYMOL_SHUTDOWN_ON_EXIT", Boolean.toString(PYMOL_SHUTDOWN_ON_EXIT));		// doc

		// external programs: dssp
		p.setProperty("DSSP_EXECUTABLE",DSSP_EXECUTABLE);										// doc!
		p.setProperty("DSSP_PARAMETERS",DSSP_PARAMETERS);										// doc
		
		// database connection
		p.setProperty("DB_HOST",DB_HOST);														// doc?
		p.setProperty("DB_USER",DB_USER);														// doc?
		p.setProperty("DB_PWD",DB_PWD);															// doc?
		
		// default values for loading contact maps
		p.setProperty("DEFAULT_GRAPH_DB",DEFAULT_GRAPH_DB);										// doc?
		p.setProperty("DEFAULT_PDB_DB",DEFAULT_PDB_DB);											// doc?
		p.setProperty("DEFAULT_MSDSD_DB",DEFAULT_MSDSD_DB);										// doc?
		
		p.setProperty("DEFAULT_CONTACT_TYPE",DEFAULT_CONTACT_TYPE);								// doc!
		p.setProperty("DEFAULT_DISTANCE_CUTOFF",Double.toString(DEFAULT_DISTANCE_CUTOFF));		// doc!
		p.setProperty("DEFAULT_MIN_SEQSEP",Integer.toString(DEFAULT_MIN_SEQSEP));				// doc!
		p.setProperty("DEFAULT_MAX_SEQSEP",Integer.toString(DEFAULT_MAX_SEQSEP));				// doc!
		
		return p;
	}
	
	/*----------------- resources -----------------*/
	
	/** 
	 * Copy external resources (data files and executables) from the jar archive to a temp directory.
	 * The files are marked to be deleted on exit. Use getResourcePath() to access resources later.
	 * */
	private static void unpackResources() {		
		unpackResource(PyMolAdaptor.PYMOLFUNCTIONS_SCRIPT);
	}	
	
	/** 
	 * Copy the resource file 'resource' from the resource dir in the jar file to the temp directory.
	 * The resource will be marked as to be deleted. Use getResourcePath() to access resource later. 
	 */
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

			InputStream inp = Start.class.getResourceAsStream(source);
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
	
	/** 
	 * Return the absolute path to the unpacked resource with the given name.
	 * Will always return a file object but the resource may not exist unless
	 * it has been created with unpackResource() previously. 
	 */
	protected static String getResourcePath(String resource) {
		return new File(TEMP_DIR, resource).getAbsolutePath();
	}
	
	/** 
	 * Set native OSlook and feel (is possible). 
	 */
	private static void setLookAndFeel() {
		try {
		    // Set System L&F
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } 
	    catch (UnsupportedLookAndFeelException e) {
	       System.err.println(e);
	    }
	    catch (ClassNotFoundException e) {
		       System.err.println(e);	   
	    }
	    catch (InstantiationException e) {
		       System.err.println(e);	  
	    }
	    catch (IllegalAccessException e) {
		       System.err.println(e);	   
	    }
	    
	    /* In MacOS X, the menu bar is usually on top of the screen, while the default Java behaviour is to have it in the application window.
	       The MacOS specific system property apple.laf.useScreenMenuBar=true is supposed to make swing application behave "the Mac way".
	       However, System.setProperty("apple.laf.useScreenMenuBar", "true"); currently, causes a NullPointerException in pack()...paintIcon().
	       So we do not use it here until we can fix this. TODO
	    */
	}
	
	/**
	 * Runs external pymol executable if possible.
	 * Tries to determine the port the PyMol server is running on.  
	 * @return true on success, false otherwise.
	 */
	private static boolean runPymol() {
		try {
			System.out.println("Starting PyMol...");
			File f = new File(PYMOL_EXECUTABLE);
			if(!f.exists()) {
				System.err.println(PYMOL_EXECUTABLE + " does not exist.");
				// try to start pymol anyways because on Mac f.exists() returns false even though the file is there
			}
			Process pymolProcess = Runtime.getRuntime().exec(f.getCanonicalPath() + " " + PYMOL_PARAMETERS);
			
			// determine the rpc port from PyMol's output stream as it might be
			// different from 9123 which is used to be the default port. 
			// However, if the connection to this port fails, PyMol tries 
			// a certain number of different ports for starting the server. 
			// Therefore, we have to determine the possibly new server port.
			BufferedReader in = new BufferedReader(new InputStreamReader(pymolProcess.getInputStream()));
			String pymolOut = null;
			// the group of wild-cards accepting digitals is for getting the 
			// port number 
			Pattern portPattern = Pattern.compile("^.*port\\s(\\d+).*");
			Matcher matchPortPattern = null; 
			while( true ) {
				// TODO: we hopefully won't get stuck in this line if the stream is empty ?!?!?! maybe some Timer functionality would do ...
				pymolOut = in.readLine();  
				matchPortPattern = portPattern.matcher(pymolOut);
				if( matchPortPattern.matches() ) {
					PYMOL_PORT = matchPortPattern.group(1);
					setPyMolServerUrl(PYMOL_HOST, PYMOL_PORT);
					System.out.println("Found PyMol server running on port " + PYMOL_PORT + ".");
					break;
				}
			}
		} catch(IOException e) {
			System.err.println(e.getMessage());
			return false;
		}
		return true;				
	}
	
	/**
	 * Try connecting to the database server. Returns true on success, false otherwise.
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
	private static Model preloadModel(String pdbCode, String pdbFile, String pdbChainCode, String contactType, double cutoff) {
		Model mod = null;
		if(pdbChainCode==null) {
			pdbChainCode = NULL_CHAIN_CODE;
		}
		if (contactType == null) contactType = DEFAULT_CONTACT_TYPE;
		if (cutoff == 0.0) cutoff = DEFAULT_DISTANCE_CUTOFF;
		// parameters should be pdb code and chain code
		if (pdbCode!=null) {
			if(USE_DATABASE && database_found) {
				try {
					mod = new PdbaseModel(pdbCode, contactType, cutoff, DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP, DEFAULT_PDB_DB);
					mod.load(pdbChainCode, 1);
				} catch(ModelConstructionError e) {
					System.err.println("Could not load structure for given command line parameters:");
					System.err.println(e.getMessage());
				} catch (PdbCodeNotFoundError e) {
					System.err.println("Could not load structure for given command line parameters:");
					System.err.println(e.getMessage());
				} catch (SQLException e) {
					System.err.println("Could not load structure for given command line parameters:");
					System.err.println(e.getMessage());
				}			
			} else {
				try {
					mod = new PdbFtpModel(pdbCode, contactType, cutoff, DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP);
					mod.load(pdbChainCode, 1);
				} catch (IOException e) {
					System.err.println("Could not load structure for given command line parameters:");
					System.err.println(e.getMessage());
				} catch (ModelConstructionError e) {
					System.err.println("Could not load structure for given command line parameters:");
					System.err.println(e.getMessage());
				}				
			}
		} else if (pdbFile!=null) {
			try {
				mod = new PdbFileModel(pdbFile,contactType,cutoff,DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP);
				mod.load(pdbChainCode, 1);
			} catch (ModelConstructionError e) {
				System.err.println("Could not load structure for given command line parameters:");
				System.err.println(e.getMessage());
			}
		}
		return mod;
	}
	
	/**
	 * Returns true iff the given path is a directory which is writable.
	 */
	private static boolean isWritableDir(String path) {
		File f = new File(path);
		if(!f.isDirectory()) return false;
		if(!f.canWrite()) return false;
		return true;
	}

	/*---------------------------- public methods ---------------------------*/
	
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
	
	/**
	 * Enables/disables use of pymol. 
	 * e.g.: called after pymol communication is lost
	 * @param usePymol
	 */
	public static void setUsePymol (boolean usePymol) {
		pymol_found = usePymol;
	}
	
	/**
	 * Returns true if external dssp application is available.
	 */
	public static boolean isDsspAvailable() {
		return dssp_found;
	}

	/**
	 * Return the global db connection for this session.
	 * @return The MySQLConnection object
	 */
	public static MySQLConnection getDbConnection() {
		return conn;
	}
	
	/**
	 * Return the global fileChooser for this session.
	 * @return A JFileChooser to be used whenever possible.
	 */	
	public static JFileChooser getFileChooser() {
		return fileChooser;
	}
	 
	 /**
	  * Returns the global colorChooser for this session.
	  * @return A JColorChooser to be used whenever possible.
	  */	
	 public static JColorChooser getColorChooser() {
		  return colorChooser;
	  }
	
	 /** 
	  * Returns the global pymolAdaptor of this session.
	  * @return The PyMolAdaptor object
	  */
	 public static PyMolAdaptor getPyMolAdaptor() {
		 return pymolAdaptor;
	 }
	 
	 /**
	  * Returns the global threadPool of this session. 
	  * @return the thread pool
	  */
	 public static ThreadPoolExecutor getThreadPool() {
		 return threadPool;
	 }
	 
	 public static void setPyMolServerUrl(String host, String port) {
		 PYMOL_SERVER_URL = "http://"+PYMOL_HOST+":"+PYMOL_PORT;
	 }

	 /*--------------------------------- main --------------------------------*/
	 
	/**
	 * Main method to start CMView application
	 * @param args command line arguments
	 */
	public static void main(String args[]){
		

		String help = "Usage: \n" +
		APP_NAME+" [-p <pdb code>] [-f <pdb file>] [-c <pdb chain code>] [-t <contact type>] [-d <distance cutoff>] [-o <config file>] \n" +
				"The given config file will override settings from system-wide or user's config file\n";

		String pdbCode = null;
		String pdbFile = null;
		String pdbChainCode = null;
		String contactType = null;
		String cmdLineConfigFile = null;
		String debugConfigFile = null;
		double cutoff = 0.0;

		Getopt g = new Getopt(APP_NAME, args, "p:f:c:t:d:o:vg:h?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch(c){
			case 'p':
				pdbCode = g.getOptarg();
				break;
			case 'f':
				pdbFile = g.getOptarg();
				break;				
			case 'c':
				pdbChainCode = g.getOptarg();
				break;
			case 't':
				contactType = g.getOptarg();
				break;
			case 'd':
				cutoff = Double.parseDouble(g.getOptarg());
				break;
			case 'o':
				cmdLineConfigFile = g.getOptarg();
				break;
			case 'g':											// write current config parameters to a file (for debugging)
				debugConfigFile = g.getOptarg();
				break;
			case 'v':
				System.out.println(APP_NAME+" "+VERSION);
				System.exit(0);
				break;				
			case 'h':
			case '?':
				System.out.println(help);
				System.exit(0);
				break; // getopt() already printed an error
			}
		}

		System.out.println("Starting " + APP_NAME + " " + VERSION + " - Interactive contact map viewer");
		
		// load configuration
		boolean configFileFound = false;
		Properties userProperties = new Properties();
		
		// loading from current directory
		File currentDirConfigFile = new File(CONFIG_FILE_NAME);
		try {
			if(currentDirConfigFile.exists()) {
				Properties p = loadConfigFile(CONFIG_FILE_NAME);
				System.out.println("Loading configuration file " + CONFIG_FILE_NAME);
				userProperties.putAll(p);
				applyUserProperties(userProperties);
				configFileFound = true;
			}
		} catch (IOException e) {
			System.err.println("Error while reading from file " + CONFIG_FILE_NAME + ": " + e.getMessage());
		}
		
		// loading from user's home directory
		File userConfigFile = new File(System.getProperty("user.home"),CONFIG_FILE_NAME);  
		try {
			if (userConfigFile.exists()) {
				System.out.println("Loading user configuration file " + userConfigFile.getAbsolutePath());
				userProperties.putAll(loadConfigFile(userConfigFile.getAbsolutePath()));
				applyUserProperties(userProperties);
				configFileFound = true;
			}
		} catch (IOException e) {
			System.err.println("Error while reading from file " + userConfigFile.getAbsolutePath() + ": " + e.getMessage());
		}
		
		// loading from file given as command line parameter
		try {
			if (cmdLineConfigFile!=null) {
				if(new File(cmdLineConfigFile).exists()) {
					System.out.println("Loading command line configuration file " + cmdLineConfigFile);
					userProperties.putAll(loadConfigFile(cmdLineConfigFile));
					applyUserProperties(userProperties);
					configFileFound = true;
				}
			}
		} catch (IOException e) {
			System.err.println("Error while reading from file " + cmdLineConfigFile + ": " + e.getMessage());
		}
		if(!configFileFound) {
			System.out.println("No configuration file found. Using default options.");
		}
				
		// write current configuration to file (for debugging)
		if(debugConfigFile != null) {
			Properties p = getCurrentProperties();
			try {
					saveConfigFile(p, debugConfigFile);
					System.out.println("Writing current configuration to " + debugConfigFile);
				} catch (IOException e) {
					System.err.println("Error writing local setting to config file " + debugConfigFile + ": " + e.getMessage());
					System.exit(1);
				}
			System.exit(0);
		}
			
		// check temp directory
		System.out.println("Using temporary directory " + TEMP_DIR);
		if(isWritableDir(TEMP_DIR)) {
			unpackResources();
		} else {
			System.err.println("Error: Can not write to temporary directory. Some features may not function correctly.");
		}
		
		// connect to pymol
		if(USE_PYMOL) {
			pymolAdaptor = new PyMolAdaptor(PYMOL_SERVER_URL);
			if(pymolAdaptor.tryConnectingToPymol(100) == true) { // running pymol server found
				System.out.println("PyMol server found. Connected.");
				// do not shutdown View's possessions!!! 
				PYMOL_SHUTDOWN_ON_EXIT = false;
				pymol_found = true;
			} else {
				if(PYMOL_LOAD_ON_START) {
					if(runPymol() == false) {
						//System.err.println("Warning: Failed to start PyMol automatically. Please manually start Pymol with the -R parameter.");	
						System.err.println("Failed. (You can try to restart this application after manually starting pymol with the -R parameter)");
						pymol_found = false;
					} else {
						System.out.println("Connecting to PyMol server...");
						
						// re-set url in case function runPymol detected that 
						// PyMOL is running on a different port 
						pymolAdaptor.setPyMolServerUrl(PYMOL_SERVER_URL);
						
						if(pymolAdaptor.tryConnectingToPymol(PYMOL_CONN_TIMEOUT) == false) {
							System.err.println("Failed. (You can try to restart this application after manually starting pymol with the -R parameter)");
							pymol_found = false;
						} else {
							System.out.println("Connected.");
							pymol_found = true;
						}						
					}
				} else {
					pymol_found = false;
				}
			}
			if(!pymol_found) {
				System.err.println("Could not connect to PyMol server. Some functionality will not be available.");
			}
		} else {
			pymol_found = false;
		}
		
		// connect to database
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

		// check dssp
		if(USE_DSSP) {
			File dssp = new File(Start.DSSP_EXECUTABLE);
			dssp_found = dssp.canRead();
			if(dssp_found) {
				System.out.println("Using DSSP executable " + Start.DSSP_EXECUTABLE);
			} else {
				System.out.println("No DSSP executable found.");
			}
		} else {
			dssp_found = false;
		}
		
		
		// add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutting down");
                if(isPyMolConnectionAvailable() && PYMOL_SHUTDOWN_ON_EXIT) {
                	pymolAdaptor.shutdown();
                }
            }
        });
		
		setLookAndFeel();
		
		// initialize session variables
		fileChooser = new JFileChooser();
		colorChooser = new JColorChooser();
		colorChooser.setPreviewPanel(new JPanel()); // removing the preview panel
		
		// start gui without a model or preload contact map based on command line parameters
		String wintitle = "Contact Map Viewer";
		Model mod = preloadModel(pdbCode, pdbFile, pdbChainCode, contactType, cutoff);
		if (mod!=null) wintitle = "Contact Map of " + mod.getLoadedGraphID();
		new View(mod, wintitle);
		if (mod!=null && Start.isPyMolConnectionAvailable() && mod.has3DCoordinates()) {
			// load structure in PyMol
			Start.getPyMolAdaptor().loadStructure(mod.getTempPdbFileName(), mod.getLoadedGraphID(), false);
		}		

	}
}
