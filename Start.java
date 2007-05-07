
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.IOException;

import javax.swing.*;

import tools.Msdsd2Pdb;
import tools.MySQLConnection;
/**
 * 
 * @author		Juliane Dinse
 * @author		Henning Stehr
 * Class: 		Start
 * Package: 	CM2PyMol
 * Date:		20/02/2007, updated: 29/03/2007
 * 				2007-05-04 updated by HS
 * 
 * tasks:
 * - initialising the application window
 * - getting the input parameters (accession code, chain code, contact type, minimum distance) 
 * 	 by Choice Boxes (Selection Lists)
 * - initiating other programs
 * - setting the complete SQL-String
 * 
 * TODO:
 * - Fix problems with null chain codes (bug) [done]
 * - Remove back references to start -> allow multiple CM windows (feature) [done]
 * - Move database connection to Start/main function (style)
 * - make config file (usability)
 * - add combo box for distance threshold (feature)
 * - update selection rectangle and coordinates while dragging (usability) [done]
 * - why is the structure in pymol not being loaded automatically? (usability)
 * - when sending edges to pymol, also create a selection of the respective residues (feature)
 */

public class Start extends JFrame implements ActionListener, ItemListener {

	static final long serialVersionUID = 1l;
	
	/* Constants, TODO: Move to configuration file */

	// Note: the database needs to have chain_graph and single_model_graph tables
	// table and column names are currently hard-coded, TODO: use pdb-file/pdbase as input
	
	public static final String		DB_HOST =			"white";
	public static final String		DB_USER =			getUserName();
	public static final String		DB_PWD =			"nieve";
	public static final String		TEMP_PATH =			"/scratch/local/"; // for temp pdb files
	public static final String      HOST = 				getHostName() ;
	public static final String		PYMOL_SERVER_URL = 	"http://"+HOST+":9123";
	public static final String		DEFAULT_GRAPH_DB =	"pdb_reps_graph"; // we set the default here, but can be reset from first argument in command line
	public static final String		PYMOL_CMD = 		"pymol -R";
	public static final String		NULL_CHAIN_CODE = 	"NULL"; // value important for Msdsd2Pdb
	
	public static float 			DEFAULT_DISTANCE_CUTOFF = 4.1f; // for now, assume all graphs are like this
																	// later, let user choose (add text field)
	
	public static String 			graphDb = DEFAULT_GRAPH_DB;
	
	/* Declaration */
	
	//private LayoutManager Layout;

	private JComboBox ComboSelAc;	// Selector for accession code
	private JComboBox ComboSelCc;	// Selector for chain pdb code
	private JComboBox ComboSelCt;	// selector for contact type

	public int numac, rownumac;		// getting the number of rows out of the DB in accession_code Column
	public int numcc, rownumcc;		// getting the number of rows out of the DB in chain-pdb-code Column
	public int numct, rownumct;		// getting the number of rows out of the DB in chain-pdb-code Column

	public String Selectac, Selectcc, Selectct;
	public String [] ACodeList;
	public String [] CCodeList;
	public String [] CTList;

	//private Font SansSerif;
	JButton load, check;
	JPanel loadpanel, selpanel;
	private JTextField tf;

	public String sql;
	public String pdbFileName;

	private Model mod;
	private View view;
	//private PaintController pc;
	
	private MySQLConnection conn = null;

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
	
	/** get user name from operating system (for use as database username) */
	private static String getUserName() {
		String user = null;
		user = System.getProperty("user.name");
		if(user == null) {
			System.err.println("Could not get user name from operating system. Exiting");
			System.exit(1);
		}
		return user;
	}

	// helper function for filling combo boxes
	private Object makeObj(final String item)  {
		return new Object() { public String toString() { return item; } };
	}

	/** construct a new start object with the given title */
	public Start(String title){
		super(title);
		
		// open database connection
		try {
			this.conn = new MySQLConnection(DB_HOST, DB_USER, DB_PWD, graphDb);
		} catch (Exception e) {
			System.err.println("Error: Could not open database connection");
			e.printStackTrace();
		}
	}
	
	/** main function to initialize and show GUI */
	public void PreStartInit() {
		
		/* Layout settings */
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocation(300,200);

		/* Instantiation */
		//SansSerif = new Font ("SansSerif", Font.BOLD, 14);
		//Layout = new FlowLayout ();

		ComboSelAc= new JComboBox();		// ComboBox for Accession codes
		ComboSelCc= new JComboBox();		// ComboBox for chain codes
		ComboSelCt= new JComboBox();		// ComboBox for contact types

		/* creating panel */
		loadpanel = new JPanel(new GridLayout(2,1));		// Panel for load-button
		selpanel = new JPanel(new GridLayout(5,1));		// panel for selectors

		/* creating button */
		load = new JButton("Load");
		/* adding ActionListener to button */
		load.addActionListener(this);
		/* adding button to panel */
		loadpanel.add(load, new GridLayout(2,1));

//		/* creating button for accession code check */
//		check = new JButton("Check Database");
//		/*adding ActionListener to button */
//		check.addActionListener(this);
//		/* adding button to panel */
//		loadpanel.add(check, new GridLayout(1,1));

		/* Creating the Selectors */                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
//		Selectorac.setBackground (Color.gray);
//		Selectorac.setForeground (Color.lightGray);
//		Selectorac.setFont (SansSerif);
//
//		Selectorcc.setBackground (Color.gray);
//		Selectorcc.setForeground (Color.lightGray);
//		Selectorcc.setFont (SansSerif);
//
//		Selectorct.setBackground (Color.gray);
//		Selectorct.setForeground (Color.lightGray);
//		Selectorct.setFont (SansSerif);

		/* creating textfield */
		tf = new JTextField();
		tf.setText("0");

		/* initialize combo boxes */
		/* Note: this has to happen before adding the ItemListeners */
		fillACChoiceBox();
		fillCCChoiceBox();
		fillCTChoiceBox();
				
		/* Adding selectors and textlabels to panel */
		//selpanel.add(new JLabel("Search Access. C.:"));
		//selpanel.add(tfac);
		selpanel.add(new JLabel("Accession Code:"));
		//selpanel.add(Selectorac);
		selpanel.add(ComboSelAc);
		//selpanel.add(ComboSelAc);
		selpanel.add(new JLabel("Chain Code:"));
		//selpanel.add(Selectorcc);
		selpanel.add(ComboSelCc);
		//selpanel.add(ComboSelCc);
		selpanel.add(new JLabel("Contact Type:"));
		//selpanel.add(Selectorct);
		selpanel.add(ComboSelCt);
		//selpanel.add(ComboSelCt);
		selpanel.add(new JLabel("Minimum Distance:"));
		selpanel.add(tf);

		/* Adding ItemListener to the Selectors */
		ComboSelAc.addItemListener (this);
		ComboSelCc.addItemListener (this);
		ComboSelCt.addItemListener (this);

//		ComboSelAc.addActionListener (this);
//		ComboSelCc.addActionListener (this);
//		ComboSelCt.addActionListener (this);

		/* creating the front frame */
		Box verlBox = Box.createVerticalBox();
		/* adding the panels contens to the frame */
		verlBox.add(selpanel, BorderLayout.CENTER);
		verlBox.add(loadpanel, BorderLayout.SOUTH);
		getContentPane().add(verlBox);
		pack();
		setVisible(true);
	}

	/** return the currently selected accession code */
	public String getSelectedAC() {
		String Selectac = ComboSelAc.getSelectedItem().toString();
		return Selectac;
	}

	/** return the currently selected chain code */
	public String getSelectedCC() {
		String Selectcc = ComboSelCc.getSelectedItem().toString();
		return Selectcc;
	}

	/** return the currently selected contact type */
	public String getSelectedCT() {
		String Selectct = ComboSelCt.getSelectedItem().toString();
		return Selectct;
	}
	
	/* return the chosen contact distance cutoff */
	public float getSelectedDistanceCutoff() {
		return DEFAULT_DISTANCE_CUTOFF;
	}
	
	/* return the chosen minimum sequence separation */
	public int getSelectedMinimumSequenceSeparation() {
		int seqSep = Integer.valueOf(tf.getText());
		return seqSep;
	}
	
	// TODO: Add combo box for distance threshold
	
	/** initialize or refresh the values for the accession code combo box */
	public void fillACChoiceBox() {
		
		/** SQL preparation */
		Statement  st = null;
		ResultSet  rsacs = null; 	// getting the data for the size of the Contact Map
		ResultSet  rsac = null;	    // getting the data of the accession codes

		/* clear current items for a clean refill */
		ComboSelAc.removeAllItems();
		
		try {

			/** SQL-String takes the data out of the DB */
			String straccesscode = "select distinct accession_code from single_model_graph;";

			/** Database Connection */
			st = conn.createStatement();

			/** initialising the Selector for Accession Codes */
			rsacs = st.executeQuery(straccesscode);
			while (rsacs.next()){
				numac = rsacs.getRow();
			}


			ACodeList = new String [numac];
			//ComboListAc = new Object[numac+1];

			rsac = st.executeQuery(straccesscode);
			int k =0;
			while (rsac.next()){
				/* adding database output to object */
				String acccode = rsac.getString(1);
				//Object acode= rsac.getObject(1);
				ACodeList[k]= acccode;
				//ComboListAc[k]= acode;
				k++;
			}
			/* adding object content to selector to represent it */
			for (int i = 0; i < ACodeList.length; i++) {
				//Selectorac.insert (ACodeList [i], i);
				ComboSelAc.addItem(makeObj(ACodeList[i]));
			}
			st.close();
		
		}
		catch ( Exception ex ) {
			System.out.println( ex );
		}

	}
	
	/** initialize or refresh the values for the chain code combo box */
	public void fillCCChoiceBox(){
		Statement  st = null;  
		ResultSet  rsccs = null;	    // getting the data of the size of the chain pdb codes
		ResultSet  rscc = null;	    	// getting the data of the chain pdb codes

		String accession_code = this.getSelectedAC();

		/* clear current items for a clean refill */
		ComboSelCc.removeAllItems();
		
		try {
			int n=0;
			/** SQL-String takes the data out of the DB */
			String strchainpdb = "select distinct chain_pdb_code from chain_graph where accession_code = '"+accession_code+"' ;";

			/** Database Connection */
			//con = new MySQLConnection(DB_HOST,DB_USER,DB_PWD,graphDb);
			st = this.conn.createStatement();

			/** initialising the selector for PDB chain Codes */
			rsccs = st.executeQuery(strchainpdb);
			while (rsccs.next()){
				numcc = rsccs.getRow();
			}

			CCodeList = new String [numcc];
			rscc = st.executeQuery(strchainpdb);
			while (rscc.next()){
				/* adding database output to object */
				String cccode = rscc.getString(1);
				/* Exception handling for chain_code = "null" */
				if(cccode == null) {cccode = NULL_CHAIN_CODE;}
				CCodeList[n]= cccode;
				n++;
			}
			/* adding object content to selector to represent it */
			for (int i = 0; i < CCodeList.length; i++) {
				ComboSelCc.addItem(makeObj(CCodeList[i]));
			}
			//Selectorcc.select(0);

			st.close();

		}catch ( Exception ex ) {
			System.out.println( ex );
		}
	}

	/** initialize or refresh the values for the contact type combo box */
	public void fillCTChoiceBox(){
		Statement  st = null;  
		ResultSet  rscts = null;	// getting the data of the size of the chain pdb codes
		ResultSet  rsct = null;	    // getting the data of the chain pdb codes
		
		String accession_code = this.getSelectedAC();
		String chain_pdb_code = this.getSelectedCC();
		
		/* clear current items for a clean refill */
		ComboSelCt.setEnabled(false);
		ComboSelCt.removeAllItems();
		ComboSelCt.setEnabled(true);
		
		try {
			int n=0;
			/** SQL-String takes the data out of the DB */
			if (chain_pdb_code.equals(NULL_CHAIN_CODE)){
				chain_pdb_code = "is null";
			}
			else {
				chain_pdb_code= "='"+chain_pdb_code + "'";
			}
			String strct = "select distinct single_model_graph.CT from chain_graph inner join single_model_graph "
				+ "on chain_graph.graph_id = single_model_graph.pgraph_id "
				+ "where chain_graph.accession_code = '" + accession_code + "' " 
				+ "and chain_graph.chain_pdb_code "
				+ chain_pdb_code;
				//+ (chain_pdb_code.equals(NULL_CHAIN_CODE)?"is null":("= " + chain_pdb_code));

			/** Database Connection */
			//con = new MySQLConnection(DB_HOST,DB_USER,DB_PWD, graphDb);
			st = conn.createStatement();

			/** initialising the selector for PDB contact types */
			rscts = st.executeQuery(strct);
			while (rscts.next()){
				numct = rscts.getRow();
			}

			CTList = new String [numct];
			rsct = st.executeQuery(strct);
			while (rsct.next()){
				/* adding database output to object */
				String ctcode = rsct.getString(1);
				CTList[n]= ctcode;
				n++;
			}
			/* adding object content to selector to represent it */
			for (int i = 0; i < CTList.length; i++) {
				//Selectorct.insert (CTList [i], i);
				ComboSelCt.addItem(makeObj(CTList[i]));
			}
			//Selectorct.select(0);

			st.close();

		}catch ( Exception ex ) {
			System.out.println( ex );
		}
	}

	/** action listener for combo boxes */
	public void itemStateChanged(ItemEvent e) {

		if(e.getSource() == ComboSelAc && ComboSelAc.getItemCount() > 0 && e.getStateChange() == ItemEvent.SELECTED) {  
			Selectac = ComboSelAc.getSelectedItem().toString();
			System.out.println(Selectac);

			this.fillCCChoiceBox();
			// Note: Calling fillCCChoice box will fire an ItemEvent which in turn causes
			// fillCTChoiceBox to be called.
		} else

		if(e.getSource()== ComboSelCc && ComboSelCc.getItemCount() > 0 && e.getStateChange() == ItemEvent.SELECTED) {
			Selectcc = ComboSelCc.getSelectedItem().toString();
			System.out.println(Selectcc);

			this.fillCTChoiceBox();
		} else

		if(e.getSource()== ComboSelCt && ComboSelCt.getItemCount() > 0 && e.getStateChange() == ItemEvent.SELECTED) {
			Selectct = ComboSelCt.getSelectedItem().toString();
			System.out.println(Selectct);
		}
	} 

	/** action listener for load button */
	public void actionPerformed (ActionEvent e) {
		
		/* load button */
		if (e.getSource()== load){
//			val[0]=this.getSelectedAC();
//			val[1]=this.getSelectedCC();
//			val[2]=this.getSelectedCT();
			//val[3]=tf.getText();

//			if (val[1].equals(NULL_CHAIN_CODE)){
//				sql = this.setSQLString(val[0], "is null", val[2], val[3]);
//			}
//			else {
//				sql = this.setSQLString(val[0], "= '"+val[1] +"'", val[2], val[3]);
//			}

			this.Init();
		}

	}

	/** load the contact map window */
	public void Init(){

		/** Initialising the application */ 
		// data
		mod = new Model(this.getSelectedAC(), this.getSelectedCC(), this.getSelectedCT(),
				        this.getSelectedDistanceCutoff(), this.getSelectedMinimumSequenceSeparation(),
				        this.conn);
		// paint controller --> will be initialized in view.ViewInit()
		// pc = new PaintController(mod, view);
		// view
		String wintitle = "Contact Map of " + this.getSelectedAC() + " " + this.getSelectedCC();
		view = new View(mod, wintitle, PYMOL_SERVER_URL, this.getSelectedAC(), this.getSelectedCC(), this.getTempPDBFileName());
		if(view == null) {
			System.out.println("Couldn't initialize view. Exiting");
			System.exit(-1);
		}

		try{
			Msdsd2Pdb.export2File(this.getSelectedAC(), this.getSelectedCC(), this.getTempPDBFileName(), DB_USER);
		} catch (Exception ex){
			System.err.println("Error: Couldn't export PDB file from MSD");
			System.out.println(ex);
		}
	}

	/* returns the name of the temporary pdb file */
	public String getTempPDBFileName(){

		pdbFileName  = TEMP_PATH + this.getSelectedAC() + ".pdb";
		return pdbFileName;
	}


	public static void main(String args[]){
		
		System.out.println("CM2PyMol - Interactive contact map viewer");
		// set parameters
		if (args.length>0){
			graphDb = args[0];
			System.out.println("Using database " + args[0]);
		} else {
			System.out.println("Using default graph database.");
			System.out.println("(You can specify a different graph database as a command line parameter)");
		}
		
		// start pymol
		try {
			System.out.println("Starting PyMol...");
			// TODO: check whether pymol is running already
			Process pymolProcess = Runtime.getRuntime().exec(PYMOL_CMD);
			if(pymolProcess == null) {
				throw new IOException("pymolProcess Object is null");
			}
			// TODO: catch output and wait until pymol is loaded
		} catch(IOException e) {
			System.err.println("Warning: Couldn't start PyMol automatically. Please manually start Pymol with the -R parameter.");
		}
		
		// start gui
		String title = "CM2PyMol";
		Start pstart = new Start(title);
		pstart.PreStartInit();

	}

}
