package cmview;
import gnu.getopt.Getopt;

import java.io.*;
import java.sql.SQLException;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import owl.core.structure.Pdb;
import owl.core.structure.PdbCodeNotFoundError;
import owl.core.structure.graphs.ProtStructGraph;
import owl.core.util.FileTypeGuesser;
import owl.core.util.MySQLConnection;

import cmview.datasources.*;

/**
 * Main class to start contact map viewer application.
 * Contains static main function, constants and some helper functions.
 */
public class Start {

	/*------------------------------ constants ------------------------------*/
	
	static final long serialVersionUID = 1l;

	/* internal constants (not user changeable) */
	public static final String		APP_NAME = 				"CMView";			// name of this application
	public static final String		VERSION = 				"1.2.1";			// current version of this application (should match manifest)
	public static final String		RESOURCE_DIR = 			"/resources/"; 		// path within the jar archive where resources are located
	public static final String		HELPSET =               "/resources/help/jhelpset.hs"; // the path to the inline help set
	public static final String		ICON_DIR = 				"/resources/icons/";	// the directory containing the icons
	public static final String		FCT_ICON_DIR = 			"/resources/fctIcons/";	// the directory containing the icons for different transfer functions
	public static final String		SPHOXEL_DIR = 			"/src/resources/sphoxelBG/";	// the directory containing the pre-calculated sphoxel backgrounds
	public static final String		TRASH_LOGFILE =			"cmview_jaligner.log";	// for redirecting unwanted Jaligner output (coming from a Logger)
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

	/* internal settings */
	public static String			CONFIG_FILE_NAME = "cmview.cfg";	// default name of config file
	public static String			DIST_MAP_CONTACT_TYPE = "Ca";		// contact type to be used for distance map calculation (only single atom type allowed)
	public static String 			PDB_FTP_URL = "ftp://ftp.wwpdb.org/pub/pdb/data/structures/all/mmCIF/";
	
	// TODO: alignment parameters
	// TODO: DSSP three or four states
	
	/* gui settings */
	public static int				INITIAL_SCREEN_SIZE = 650;			// initial size of the contactMapPane in pixels
	public static boolean			SHOW_RULERS = true;					// if true, rulers will be shown by default
	public static boolean			SHOW_ICON_BAR = true;				// if true, icon bar is used
	public static boolean 			SHOW_ALIGNMENT_COORDS = false;		// if true, alignment coordinates also shown in bottom left corner of contact map
	public static boolean 			SHOW_PDB_RES_NUMS = true;			// if true, pdb residue numbers also shown in bottom left corner of contact map
	public static boolean 			SHOW_WEIGHTED_CONTACTS = true; 		// if true, weighted contacts will be shown as shades of grey/color graded (experimental feature)
	public static boolean			SHOW_WEIGHTS_IN_COLOR = false;		// if true, weighted contacts will be shown in colors, otherwise as shades of grey (experimental feature)
	
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
	public static String			PYMOL_EXECUTABLE = 		 ""; 		  // to start pymol automatically
	public static String			PYMOL_LOGFILE = 		 "CMView_pymol.log";
	public static String			PYMOL_PARAMETERS =  	 "-q -p"; 	  // listen to standard input and skip splash screen (plus -s in main())
	public static boolean			PYMOL_SHUTDOWN_ON_EXIT = true;		  // if true, pymol will be shut down on exit
	
	/* external programs: DALI */
	public static String			DALI_EXECUTABLE = 		"";
	
	/* external programs: TINKER */
	public static String			TINKER_BINPATH = 		"";
	public static String			TINKER_TEMP_DIR =		null;
	public static String			TINKER_FORCEFIELD = 	"";
	/* database connection */
	public static String			DELTA_RANK_DB = "mw";
	
	/* default values for loading contact maps */
	public static String			DEFAULT_GRAPH_DB =			""; 				// shown in load from graph db dialog
	public static String     		DEFAULT_PDB_DB = 			"";					// for loading from command line
	
	public static String     		DEFAULT_CONTACT_TYPE = 		"Ca";				// loading from command line and shown in LoadDialog
	public static double 			DEFAULT_DISTANCE_CUTOFF = 	8.0; 								// dito
	private static int        		DEFAULT_MIN_SEQSEP = 		ProtStructGraph.NO_SEQ_SEP_VAL;		// dito
	private static int        		DEFAULT_MAX_SEQSEP = 		ProtStructGraph.NO_SEQ_SEP_VAL;		// dito	

	public static String 			DEFAULT_FILE_PATH = ""; // "/Users/vehlow/Documents/workspace/PDBs/";
	
	/*--------------------------- member variables --------------------------*/

	// global session variables (use getter methods)
	private static boolean			database_found = false;
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
	public static int viewInstancesCreated() {
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
	public static void shutDown(int exitCode) {
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
		p.setProperty("DALI_EXECUTABLE", DALI_EXECUTABLE);
		p.setProperty("TINKER_BINPATH",TINKER_BINPATH);
		p.setProperty("TINKER_TEMP_DIR",TINKER_TEMP_DIR);
		p.setProperty("TINKER_FORCEFIELD",TINKER_FORCEFIELD);
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
		try {
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
			SHOW_WEIGHTED_CONTACTS = Boolean.valueOf(p.getProperty("SHOW_WEIGHTED_CONTACTS",Boolean.toString(SHOW_WEIGHTED_CONTACTS)));
			SHOW_WEIGHTS_IN_COLOR = Boolean.valueOf(p.getProperty("SHOW_WEIGHTS_IN_COLOR",Boolean.toString(SHOW_WEIGHTS_IN_COLOR)));

			// enabling/disabling features
			USE_DATABASE = Boolean.valueOf(p.getProperty("USE_DATABASE", new Boolean(USE_DATABASE).toString()));
			USE_PYMOL = Boolean.valueOf(p.getProperty("USE_PYMOL", new Boolean(USE_PYMOL).toString()));
			USE_EXPERIMENTAL_FEATURES = Boolean.valueOf(p.getProperty("USE_EXPERIMENTAL_FEATURES", new Boolean(USE_EXPERIMENTAL_FEATURES).toString()));
			USE_DSSP = Boolean.valueOf(p.getProperty("USE_DSSP", new Boolean(USE_DSSP).toString()));

			// external programs: pymol
			PYMOL_EXECUTABLE = p.getProperty("PYMOL_EXECUTABLE", PYMOL_EXECUTABLE);		
			PYMOL_LOGFILE = p.getProperty("PYMOL_LOGFILE",PYMOL_LOGFILE);
			PYMOL_PARAMETERS = p.getProperty("PYMOL_PARAMETERS", PYMOL_PARAMETERS);
			PYMOL_SHUTDOWN_ON_EXIT = Boolean.valueOf(p.getProperty("PYMOL_SHUTDOWN_ON_EXIT", new Boolean(PYMOL_SHUTDOWN_ON_EXIT).toString()));

			// external programs: dssp
			DSSP_EXECUTABLE = p.getProperty("DSSP_EXECUTABLE",DSSP_EXECUTABLE);
			DSSP_PARAMETERS = p.getProperty("DSSP_PARAMETERS",DSSP_PARAMETERS);
			
			//external programs: DALI
			DALI_EXECUTABLE = p.getProperty("DALI_EXECUTABLE",DALI_EXECUTABLE);
			
			// external programs: TINKER
			TINKER_BINPATH = p.getProperty("TINKER_BINPATH",TINKER_BINPATH);
			TINKER_TEMP_DIR = p.getProperty("TINKER_TEMP_DIR",TINKER_TEMP_DIR);
			TINKER_FORCEFIELD = p.getProperty("TINKER_FORCEFIELD",TINKER_FORCEFIELD);

			// default setting for loading contact maps
			DEFAULT_GRAPH_DB = p.getProperty("DEFAULT_GRAPH_DB", DEFAULT_GRAPH_DB);
			DEFAULT_PDB_DB = p.getProperty("DEFAULT_PDB_DB", DEFAULT_PDB_DB);

			DEFAULT_CONTACT_TYPE = p.getProperty("DEFAULT_CONTACT_TYPE", DEFAULT_CONTACT_TYPE);
			DEFAULT_DISTANCE_CUTOFF = Double.valueOf(p.getProperty("DEFAULT_DISTANCE_CUTOFF", new Double(DEFAULT_DISTANCE_CUTOFF).toString()));
			DEFAULT_MIN_SEQSEP = Integer.valueOf(p.getProperty("DEFAULT_MIN_SEQSEP",Integer.toString(DEFAULT_MIN_SEQSEP)));
			DEFAULT_MAX_SEQSEP = Integer.valueOf(p.getProperty("DEFAULT_MAX_SEQSEP",Integer.toString(DEFAULT_MAX_SEQSEP)));
			
			DEFAULT_FILE_PATH = p.getProperty("DEFAULT_FILE_PATH");
		} catch (NumberFormatException e) {
			System.err.println("A numerical value in the config file was incorrectly specified: "+e.getMessage()+". Please check the config file.");
			System.exit(1);
		}
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
		p.setProperty("SHOW_WEIGHTED_CONTACTS",Boolean.toString(SHOW_WEIGHTED_CONTACTS));		// doc?
		p.setProperty("SHOW_WEIGHTS_IN_COLOR",Boolean.toString(SHOW_WEIGHTS_IN_COLOR));			// doc?

		// feature settings
		p.setProperty("USE_DATABASE", Boolean.toString(USE_DATABASE));							// doc?
		p.setProperty("USE_PYMOL", Boolean.toString(USE_PYMOL));								// doc
		p.setProperty("USE_EXPERIMENTAL_FEATURES",  Boolean.toString(USE_EXPERIMENTAL_FEATURES));	// doc?																					
		p.setProperty("USE_DSSP",Boolean.toString(USE_DSSP));									// doc
		
		// external programs: pymol
		p.setProperty("PYMOL_EXECUTABLE",PYMOL_EXECUTABLE);										// doc!!!!
		p.setProperty("PYMOL_LOGFILE",PYMOL_LOGFILE);											// doc
		p.setProperty("PYMOL_PARAMETERS",PYMOL_PARAMETERS);										// doc?
		p.setProperty("PYMOL_SHUTDOWN_ON_EXIT", Boolean.toString(PYMOL_SHUTDOWN_ON_EXIT));		// doc

		// external programs: dssp
		p.setProperty("DSSP_EXECUTABLE",DSSP_EXECUTABLE);										// doc!
		p.setProperty("DSSP_PARAMETERS",DSSP_PARAMETERS);										// doc
		
		//external programs: DALI
		p.setProperty("DALI_EXECUTABLE",DALI_EXECUTABLE);										// doc?
		
		// external programs: TINKER
		p.setProperty("TINKER_TEMP_DIR",TINKER_TEMP_DIR);
		p.setProperty("TINKER_BINPATH",TINKER_BINPATH);
		p.setProperty("TINKER_FORCEFIELD",TINKER_FORCEFIELD);
		
		// default values for loading contact maps
		p.setProperty("DEFAULT_GRAPH_DB",DEFAULT_GRAPH_DB);										// doc?
		p.setProperty("DEFAULT_PDB_DB",DEFAULT_PDB_DB);											// doc?
		
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
	 * Try connecting to the database server. Returns true on success, false otherwise.
	 */
	private static boolean tryConnectingToDb() {
		try {
			conn = new MySQLConnection();
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
	private static Model preloadModel(String pdbCode, String inFile, String pdbChainCode, String contactType, double cutoff) {
		Model mod = null;
		if(pdbChainCode==null) {
			pdbChainCode = Pdb.NULL_CHAIN_CODE;
		}
		if (contactType == null) contactType = DEFAULT_CONTACT_TYPE;
		if (cutoff == 0.0) cutoff = DEFAULT_DISTANCE_CUTOFF;
		// parameters should be pdb code and chain code
		if (pdbCode!=null) {
			if(isDatabaseConnectionAvailable()) {
				// load from pdbase
				try {
					mod = new PdbaseModel(pdbCode, contactType, cutoff, DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP, DEFAULT_PDB_DB);
					mod.load(pdbChainCode, 1);
				} catch(ModelConstructionError e) {
					System.err.println("Could not load structure for given command line parameters:");
					System.err.println(e.getMessage());
					return null;
				} catch (PdbCodeNotFoundError e) {
					System.err.println("Could not load structure for given command line parameters:");
					System.err.println(e.getMessage());
					return null;
				} catch (SQLException e) {
					System.err.println("Could not load structure for given command line parameters:");
					System.err.println(e.getMessage());
					return null;
				}			
			} else {
				// load from online pdb
				try {
					mod = new PdbFtpModel(pdbCode, contactType, cutoff, DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP);
					mod.load(pdbChainCode, 1);
				} catch (IOException e) {
					System.err.println("Could not load structure for given command line parameters:");
					System.err.println(e.getMessage());
					return null;
				} catch (ModelConstructionError e) {
					System.err.println("Could not load structure for given command line parameters:");
					System.err.println(e.getMessage());
					return null;
				}				
			}
		} else if (inFile!=null) {
			try {
				int fileType = FileTypeGuesser.guessFileType(new File(inFile));
				switch(fileType) {
				case FileTypeGuesser.PDB_FILE:
				case FileTypeGuesser.CASP_TS_FILE:
					mod = new PdbFileModel(inFile,contactType,cutoff,DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP);
					mod.load(pdbChainCode, 1);					
					break;
				case FileTypeGuesser.OWL_CM_FILE: 
					mod = new ContactMapFileModel(inFile);
					break;
				case FileTypeGuesser.CASP_RR_FILE: 
					mod = new CaspRRFileModel(inFile);
					break;
				default:
					System.err.println("Could not recognize file type of " + inFile);
					return null;
				}
			} catch (ModelConstructionError e) {
				System.err.println("Could not load structure or contact map for given command line parameters:");
				System.err.println(e.getMessage());
				return null;
			} catch (FileNotFoundException e) {
				System.err.println("File " + inFile + " not found.");
				return null;
			} catch (IOException e) {
				System.err.println("Error reading from file " + inFile);
				return null;
			}
		} else {
			System.err.println("Unexpected error in preloadModel. Please submit a bug report.");
		}
		return mod;
	}
	
	/**
	 * Returns true if the given path is a directory which is writable.
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
		// checking for Start.USE_DATABASE should not be necessary here, but to be safe we keep it here.
	}
	
	/**
	 * Returns true if a connection to the 3D viewer is available, false otherwise.
	 * Use this for checking before calling any 3D specific functions.
	 */
	public static boolean isPyMolConnectionAvailable() {
		return pymolAdaptor != null && pymolAdaptor.isConnected();
		//return Start.USE_PYMOL && Start.pymol_found;
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
	 
	 /*--------------------------------- main --------------------------------*/
	 
	/**
	 * Main method to start CMView application
	 * @param args command line arguments
	 */
	public static void main(String args[]){
		

		String help = "Usage: \n" +
		APP_NAME+" [-f <file>] [-p <pdb code>] [-c <pdb chain code>] [-t <contact type>] [-d <distance cutoff>] [-o <config file>] [-I <image file>] [-Y]\n" +
			"File can be a PDB file, CMView contact map file, Casp TS file or Casp RR file.\n" +
			"If the -o  option is used, the given config file will override settings from system-wide or user's config file\n" +
			"If the -I option is given, a png image with the current contact map will be written instead of starting CMView.\n"+
			"With the -Y option, pymol will not be started.";
		String pdbCode = null;
		String inFile = null;
		String pdbChainCode = null;
		String contactType = null;
		String cmdLineConfigFile = null;
		String debugConfigFile = null;
		String imageFile = null;
		boolean doPreload = false;
		boolean noPymol = false;
		double cutoff = 0.0;
		Getopt g = new Getopt(APP_NAME, args, "p:f:c:t:d:o:I:vYg:h?");
		int c;
		while ((c = g.getopt()) != -1) {
			switch(c){
			case 'p':
				pdbCode = g.getOptarg();
				doPreload = true;
				break;
			case 'f':
				inFile = g.getOptarg();
				doPreload = true;
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
			// undocumented or new options:
			case 'g':											// write current config parameters to a file (for debugging)
				debugConfigFile = g.getOptarg();
				break;
			case 'v':
				System.out.println(APP_NAME+" "+VERSION);
				System.exit(0);
				break;
			case 'Y':
				noPymol = true;	// don't load pymol on startup
				break;
			case 'I':
				imageFile = g.getOptarg();
				noPymol = true; // don't need pymol for image writing
				break;
			case 'h':
			case '?':
				System.out.println(help);
				System.exit(0);
				break; // getopt() already printed an error
			}
		}

		// check command line parameters
		if(pdbCode != null && inFile != null) {
			System.err.println("Options -p and -f are exclusive. Exiting.");
			System.exit(1);
		}
		if(imageFile != null && pdbCode == null && inFile == null) {
			System.err.println("-I options requires -p or -f");
			System.exit(1);
		}
		
		System.out.println("Starting " + APP_NAME + " " + VERSION);
		
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
			System.exit(1);
		}
		
		// setting the dummy file to trash all unwanted output from jaligner (done through a java Logger)
		File trashLogFile = new File(TEMP_DIR, TRASH_LOGFILE);
		trashLogFile.deleteOnExit();
		System.setProperty("java.util.logging.config.file",trashLogFile.getAbsolutePath());
					
		// connect to pymol
		if(USE_PYMOL && noPymol==false) {
		
			PrintWriter pymolLog = null; 
			File pymolLogFile = new File(TEMP_DIR,PYMOL_LOGFILE);
			
			System.out.println("Connecting to PyMol...");
			try {
				try {
					pymolLog = new PrintWriter(pymolLogFile);
				} catch (FileNotFoundException e) {
					System.err.println("Can't write to pymol log file "+pymolLogFile.getAbsolutePath()+": "+e.getMessage()+". Exiting");
					System.exit(1);
				}
				pymolAdaptor = new PyMolAdaptor(pymolLog);			
				pymolAdaptor.startup();
				pymolAdaptor.initialize();
				System.out.println("Connected.");
			} catch(IOException e) {						
				System.err.println("Failed: "+e.getMessage());
			}
		}
		
		// connect to database
		if(USE_DATABASE && USE_EXPERIMENTAL_FEATURES) {
			System.out.println("Connecting to database...");
			if(tryConnectingToDb() == false) {
				System.err.println("No database found. Some functionality will not be available.");
				database_found = false;
			} else {
				System.out.println("Connected.");
				database_found = true;
			}
		} else {
			database_found = false;
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
		if (DEFAULT_FILE_PATH !="")
			fileChooser = new JFileChooser(DEFAULT_FILE_PATH);
		else
			fileChooser = new JFileChooser();
		colorChooser = new JColorChooser();
		colorChooser.setPreviewPanel(new JPanel()); // removing the preview panel
		
		// start gui without a model or preload contact map based on command line parameters
		String wintitle = "Contact Map Viewer";
		Model mod = null;
		if(doPreload) {
			mod = preloadModel(pdbCode, inFile, pdbChainCode, contactType, cutoff);
			if(mod == null) {
				System.exit(1);
			}
		}
		if (mod!=null) wintitle = "Contact Map of " + mod.getLoadedGraphID();
		if(imageFile != null) {
			View view = new View(mod);
			view.writeImageAndExit(imageFile);
		} else {
			new View(mod, wintitle);
		}
		if (mod!=null && Start.isPyMolConnectionAvailable() && mod.has3DCoordinates()) {
			// load structure in PyMol
			Start.getPyMolAdaptor().loadStructure(mod.getTempPdbFileName(), mod.getLoadedGraphID(), false);
		}		

	}
}
