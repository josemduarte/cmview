package cmview;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import cmview.datasources.Model;
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
	
	/* Constants, TODO: Move to configuration file */
	
	public static final String      VERSION = "0.7.1";
	
	public static final String      HOST = 				getHostName() ;
	public static final String		PYMOL_SERVER_URL = 	"http://"+HOST+":9123";
	public static final String		DEFAULT_GRAPH_DB =	"pdb_reps_graph"; // we set the default here, but can be reset from first argument in command line
	public static final String		PYMOL_CMD = 		"/project/StruPPi/PyMolAll/pymol/pymol.exe -R";
	public static final String 		PYMOLFUNCTIONS_SCRIPT = "/project/StruPPi/PyMolAll/pymol/scripts/ioannis/graph.py";
	public static final String		NULL_CHAIN_CODE = 	"NULL"; // value important for Msdsd2Pdb
	
	public static final String      DEFAULT_EDGETYPE = "ALL";
	public static final String      DEFAULT_PDB_DB   = "pdbase";
	public static final int         DEFAULT_MIN_SEQSEP   = -1;
	public static final int         DEFAULT_MAX_SEQSEP   = -1;	
	
	public static double 			DEFAULT_DISTANCE_CUTOFF = 4.1; // for now, assume all graphs are like this
																	// later, let user choose (add text field)
	public static String 			graphDb = 			DEFAULT_GRAPH_DB;
	public static boolean			DO_LOAD_PYMOL = 	true; // if true then pymol is loaded on startup

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
		
		System.out.println("CM2PyMol - Interactive contact map viewer");
		setLookAndFeel();
		
		if(DO_LOAD_PYMOL) {
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
		if (args.length>=1){
			String pdbCode = args[0];
			String chainCode = NULL_CHAIN_CODE;
			if (args.length==2) chainCode = args[1]; 
			mod = new PdbaseModel(pdbCode,chainCode,DEFAULT_EDGETYPE,DEFAULT_DISTANCE_CUTOFF, DEFAULT_MIN_SEQSEP, DEFAULT_MAX_SEQSEP, DEFAULT_PDB_DB);
			view.spawnNewViewWindow(mod);
		}
	}

}
