package cmview;
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
import cmview.datasources.PdbaseModel;

/**
 * Main class to start contact map viewer application.
 * Contains static main function, constants and some helper functions.
 */
public class Start {

	static final long serialVersionUID = 1l;

	public static String			PYMOL_PORT =			"9123";				// default port, if port is blocked, pymol will increase automatically
	
	// internal constants (not user changeable)
	public static final String		APP_NAME = 				"CMView";			// name of this application
	public static final String		VERSION = 				"0.8.6";			// current version of this application (should match manifest)
	public static final String		NULL_CHAIN_CODE = 		"NULL"; 			// used by Pdb/Graph objects for the empty pdbChainCode
	public static final int			NO_SEQ_SEP_VAL =		-1;					// default seq sep value indicating that no seq sep has been specified
	public static final String		NO_SEQ_SEP_STR =		"none";				// text output if some seqsep variable equals NO_SEQ_SEP_VAL
	public static final String		RESOURCE_DIR = 			"/resources/"; 		// path within the jar archive where resources are located
	public static final String		PYMOL_HOST = 			"localhost";		// currently, the XMLRPC server in Pymol only supports localhost
	public static final String		PYMOL_SERVER_URL = 		"http://"+PYMOL_HOST+":"+PYMOL_PORT; // TODO: set this later so that the two above may change
		
	// The following config file name may be overwritten by a command line switch
	public static String			CONFIG_FILE_NAME = 		"cmview.cfg";		// default name of config file (can be overridden by cmd line param)
	
	// The following 'constants' can be overwritten by the user's config file. In the code, they are being used as if they were (final) constants
	// and the only time when they may change is during startup. Note that for each variable that ought to be user changeable, i.e. read from cfg file
	// there has to be a line in the applyUserProperties method. The preassigned values are the default and being used unless overwritten by the user.
	// Additioanlly, values that should appear in the example config file should be added to the getSelectedProperties method.

	// environment
	public static String			TEMP_DIR = System.getProperty("java.io.tmpdir");
	
	// user customizations
	public static int			INITIAL_SCREEN_SIZE = 800;			// initial size of the contactMapPane in pixels
	public static boolean			USE_DATABASE = true; 				// if false, all functions involving a database will be hidden 
	public static boolean			USE_PYMOL = true;					// if false, all pymol specific functionality will be hidden
	public static boolean                   INCLUDE_GROUP_INTERNALS = true; // this flag shall indicate strongly experimental stuff, use it to disable features in release versions
	public static boolean			PRELOAD_PYMOL = true; 				// if true, pymol is preloaded on startup
	public static boolean			SHUTDOWN_PYMOL_ON_EXIT = true;		// if true, pymol is shutdown on exit
	
	public static boolean			SHOW_RULERS_ON_STARTUP = true;		// if true, rulers will be shown by default
	public static boolean			FORCE_DSSP = false;					// if true, secondary structure will be always taken from DSSP (if available)
	public static String			DSSP_EXECUTABLE = "/project/StruPPi/Software/dssp/dsspcmbi";
	public static String			DSSP_PARAMETERS = "--";
	public static String 			PDB_FTP_URL = "ftp://ftp.wwpdb.org/pub/pdb/data/structures/all/mmCIF/";
	
	// constants not in config file yet
	public static String			DIST_MAP_CONTACT_TYPE = "Ca";	// contact type to be used for distance map calculation (only single atom allowed)
	public static boolean			SHOW_ICON_BAR = true;				// if true, icon bar is used
	public static boolean			ICON_BAR_FLOATABLE = false;			// if true, icon bar can be dragged out of the window (buggy, don't use)
	
	// pymol connection
	public static String			PYMOL_EXECUTABLE = 		"/project/StruPPi/bin/pymol-1.0"; // to start pymol automatically
	public static String			PYMOL_PARAMETERS =  	"-R -q";				// run xmlrpc server and skip splash screen
	public static long 				PYMOL_CONN_TIMEOUT = 	15000; 					// pymol connection time out in milliseconds
	
	// database connection
	public static String			DB_HOST = "white";								// TODO: change to dummy name
	public static String			DB_USER = getUserName();						// guess user name
	public static String			DB_PWD = "nieve";								// TODO: change to tiger
	
	// default values for loading contact maps
	public static String			DEFAULT_GRAPH_DB =			"pdb_reps_graph"; 	// shown in load from graph db dialog
	public static String     		DEFAULT_PDB_DB = 			"pdbase";			// for loading from command line
	public static String			DEFAULT_MSDSD_DB =			"msdsd_00_07_a";	// used when loading structures for cm file graphs
	public static String     		DEFAULT_CONTACT_TYPE = 		"Ca";				// loading from command line and shown in LoadDialog
	public static double 			DEFAULT_DISTANCE_CUTOFF = 	8.0; 				// dito
	private static final int        DEFAULT_MIN_SEQSEP = 		NO_SEQ_SEP_VAL;		// dito, but not user changeable at the moment
	private static final int        DEFAULT_MAX_SEQSEP = 		NO_SEQ_SEP_VAL;		// dito, but not user changeable at the moment
	
	// internal status variables
	protected static boolean		database_found = true;
	protected static boolean		pymol_found = true;
	protected static boolean		dssp_found = true;			// check later whether dssp can be used
	protected static Properties		userProperties;				// properties read from the user's config file
	protected static Properties		selectedProperties;			// selected default properties for the example config file
	
	// global session variables (use getter methods)
	private static MySQLConnection conn;
	private static JFileChooser fileChooser;
	private static JColorChooser colorChooser;
	private static PyMolAdaptor pymolAdaptor;
	
	// the thread pool
	public static ThreadPoolExecutor threadPool =  (ThreadPoolExecutor) Executors.newCachedThreadPool();
	
	// mapping pdb-code to mmCIF files in the tmp-directory, only to be used for ftp loading
	private static TreeMap<String, File> pdbCode2file = new TreeMap<String, File>();
	
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
		
	/**
	 * Returns a property object with the default values for selected customizable variables.
	 * These are the variables that we expect users to commonly change. They are written
	 * when the users selects the 'write example config file' from the Help menu.
	 * Other possible customizable variables should be mentioned somewhere in the manual.
	 */
	private static Properties getSelectedProperties() {

		Properties d = new Properties();
		
		// properties which the user will have to change
		d.setProperty("PYMOL_EXECUTABLE",PYMOL_EXECUTABLE);		
		d.setProperty("DB_HOST",DB_HOST);
		d.setProperty("DB_USER",DB_USER);
		d.setProperty("DB_PWD",DB_PWD);
				
		// properties which the user may want to change
		d.setProperty("INITIAL_SCREEN_SIZE",new Integer(INITIAL_SCREEN_SIZE).toString());
		d.setProperty("USE_DATABASE",new Boolean(USE_DATABASE).toString());
		d.setProperty("USE_PYMOL",new Boolean(USE_PYMOL).toString());
		d.setProperty("PRELOAD_PYMOL",new Boolean(PRELOAD_PYMOL).toString());
		d.setProperty("SHUTDOWN_PYMOL_ON_EXIT",new Boolean(SHUTDOWN_PYMOL_ON_EXIT).toString());
		d.setProperty("DEFAULT_CONTACT_TYPE",DEFAULT_CONTACT_TYPE);
		d.setProperty("DEFAULT_DISTANCE_CUTOFF",new Double(DEFAULT_DISTANCE_CUTOFF).toString());
		d.setProperty("DSSP_EXECUTABLE", DSSP_EXECUTABLE);
		d.setProperty("FORCE_DSSP", new Boolean(FORCE_DSSP).toString());
		d.setProperty("SHOW_RULERS_ON_STARTUP", new Boolean(SHOW_RULERS_ON_STARTUP).toString());
		
		// properties which will become obsolete when loading from online pdb is implemented
		d.setProperty("DEFAULT_PDB_DB",DEFAULT_PDB_DB);
		d.setProperty("DEFAULT_GRAPH_DB", DEFAULT_GRAPH_DB);
		d.setProperty("DEFAULT_MSDSD_DB",DEFAULT_MSDSD_DB);
		
		// properties that should be changed only if problems arise
		// these will be mentioned in the documentation somewhere but not in the example config file
		//d.setProperty("TEMP_DIR",TEMP_DIR);
		//d.setProperty("PYMOL_PARAMETERS",PYMOL_PARAMETERS);
		//d.setProperty("PYMOL_CONN_TIMEOUT",new Long(PYMOL_CONN_TIMEOUT).toString());
		//d.setProperty("DSSP_PARAMETERS", DSSP_PARAMETERS);
		
		return d;
	}

	/**
	 * Loads user properties from the given configuration file.
	 * Returns null on failure;
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	private static Properties loadUserProperties(String fileName) throws FileNotFoundException, IOException {
		Properties p = new Properties();
		p.load(new FileInputStream(fileName));
		return p;
	}

	/**
	 * Overwrite the local constants with the values from the given properties object
	 */
	private static void applyUserProperties(Properties p) {

		// The logic here is: First, take the value from the user config file,
		// if that is not found, keep the variable value unchanged.
		// Note that any value in the user config file that is not being processed here is ignored.
		
		TEMP_DIR = p.getProperty("TEMP_DIR",TEMP_DIR);
		INITIAL_SCREEN_SIZE = Integer.valueOf(p.getProperty("INITIAL_SCREEN_SIZE", new Integer(INITIAL_SCREEN_SIZE).toString()));
		USE_DATABASE = Boolean.valueOf(p.getProperty("USE_DATABASE", new Boolean(USE_DATABASE).toString()));
		USE_PYMOL = Boolean.valueOf(p.getProperty("USE_PYMOL", new Boolean(USE_PYMOL).toString()));
		PRELOAD_PYMOL = Boolean.valueOf(p.getProperty("PRELOAD_PYMOL", new Boolean(PRELOAD_PYMOL).toString()));
		SHUTDOWN_PYMOL_ON_EXIT = Boolean.valueOf(p.getProperty("SHUTDOWN_PYMOL_ON_EXIT", new Boolean(SHUTDOWN_PYMOL_ON_EXIT).toString()));
		SHOW_RULERS_ON_STARTUP = Boolean.valueOf(p.getProperty("SHOW_RULERS_ON_STARTUP", new Boolean(SHOW_RULERS_ON_STARTUP).toString()));
		
		PYMOL_PARAMETERS = p.getProperty("PYMOL_PARAMETERS", PYMOL_PARAMETERS);
		PYMOL_EXECUTABLE = p.getProperty("PYMOL_EXECUTABLE", PYMOL_EXECUTABLE);		
		PYMOL_CONN_TIMEOUT = Long.valueOf(p.getProperty("PYMOL_CONN_TIMEOUT",new Long(PYMOL_CONN_TIMEOUT).toString()));
		
		DSSP_EXECUTABLE = p.getProperty("DSSP_EXECUTABLE",DSSP_EXECUTABLE);
		DSSP_PARAMETERS = p.getProperty("DSSP_PARAMETERS",DSSP_PARAMETERS);
		FORCE_DSSP = Boolean.valueOf(p.getProperty("FORCE_DSSP", new Boolean(FORCE_DSSP).toString()));
		
		DB_HOST = p.getProperty("DB_HOST", DB_HOST);
		DB_USER = p.getProperty("DB_USER", DB_USER);
		DB_PWD = p.getProperty("DB_PWD", DB_PWD);

		DEFAULT_PDB_DB = p.getProperty("DEFAULT_PDB_DB", DEFAULT_PDB_DB);
		DEFAULT_CONTACT_TYPE = p.getProperty("DEFAULT_CONTACT_TYPE", DEFAULT_CONTACT_TYPE);
		DEFAULT_DISTANCE_CUTOFF = Double.valueOf(p.getProperty("DEFAULT_DISTANCE_CUTOFF", new Double(DEFAULT_DISTANCE_CUTOFF).toString()));
		
		DEFAULT_GRAPH_DB = p.getProperty("DEFAULT_GRAPH_DB", DEFAULT_GRAPH_DB);
		DEFAULT_MSDSD_DB = p.getProperty("DEFAULT_MSDSD_DB", DEFAULT_MSDSD_DB);

	}
	
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
	
	/** 
	 * Return the absolute path to the unpacked resource with the given name.
	 * Will always return a file object but the resource may not exists unless
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
					System.out.println("Found PyMol server running on port " + PYMOL_PORT + ".");
					break;
				}
			}
		} catch(IOException e) {
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
	 * TODO: Do proper command line parsing with switches to preload files or structures from online pdb
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
				mod = new PdbaseModel(pdbCode, DEFAULT_CONTACT_TYPE, DEFAULT_DISTANCE_CUTOFF, DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP, DEFAULT_PDB_DB);
				mod.load(chainCode, 1);
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
			System.err.println("No database. Ignoring command line parameters.");
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
	 * Writes an example configuration file with the default values for selected user
	 * customizable variables. The file will be written in the current directory.
	 * The values are taken from the variable selectedProperties which has to be
	 * initialized previously using getSelectedProperties().
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void writeExampleConfigFile(String fileName) throws FileNotFoundException, IOException {
		Properties d = selectedProperties;
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
	  * Return the global colorChooser for this session.
	  * @return A JColorChooser to be used whenever possible.
	  */	
	 public static JColorChooser getColorChooser() {
		  return colorChooser;
	  }
	
	 /** 
	  * Return the global pymolAdaptor of this session.
	  * @return The PyMolAdaptor object
	  */
	 public static PyMolAdaptor getPyMolAdaptor() {
		 return pymolAdaptor;
	 }
	 
	
	/**
	 * Main method to start CMView application
	 * @param args command line arguments
	 */
	public static void main(String args[]){
		
		System.out.println("Starting " + APP_NAME + " " + VERSION + " - Interactive contact map viewer");
		
		// TODO: check whether config file is passed as command line parameter, otherwise use default one
		
		// load configuration
		selectedProperties = getSelectedProperties();
		try {
			Properties p = loadUserProperties(CONFIG_FILE_NAME);
			System.out.println("Loading configuration file " + CONFIG_FILE_NAME);
			userProperties = p;
			applyUserProperties(userProperties);
		} catch (FileNotFoundException e) {
			System.out.println("No configuration file found. Using default settings.");
		} catch (IOException e) {
			System.err.println("Error while reading from file " + CONFIG_FILE_NAME + ". Using default settings.");
		}
				
		// TODO: apply command line arguments here
				
		// add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutting down");
                if(isPyMolConnectionAvailable() && SHUTDOWN_PYMOL_ON_EXIT) {
                	pymolAdaptor.shutdown(PYMOL_SERVER_URL);
                }
            }
        });
		
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
				pymol_found = true;
			} else {
				if(PRELOAD_PYMOL) {
					if(runPymol() == false) {
						//System.err.println("Warning: Failed to start PyMol automatically. Please manually start Pymol with the -R parameter.");	
						System.err.println("Failed. (You can try to restart this application after manually starting pymol with the -R parameter)");
						pymol_found = false;
					} else {
						System.out.println("Connecting to PyMol server...");
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

		setLookAndFeel();
		
		// initialize session variables
		fileChooser = new JFileChooser();
		colorChooser = new JColorChooser();
		colorChooser.setPreviewPanel(new JPanel()); // removing the preview panel
		
		// start gui without a model or preload contact map based on command line parameters
		String wintitle = "Contact Map Viewer";
		Model mod = null;
		View view = new View(mod, wintitle);
		if (args.length>=1){
			mod = preloadModel(args);
			if(mod != null) {
				view.spawnNewViewWindow(mod);
			}
		}
	}
}
