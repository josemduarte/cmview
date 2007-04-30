
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
import tools.PyMol;
/**
 * 
 * @author		Juliane Dinse
 * @author		Henning Stehr
 * Class: 		Start
 * Package: 	CM2PyMol
 * Date:		20/02/2007, updated: 29/03/2007
 * 				2007-04-27 updated by HS
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
 * - Remove back references to start -> allow multiple CM windows (feature)
 * - Move database connection to Start/main function (style)
 * - make config file (usability)
 * - add combo box for distance threshold (feature)
 * - update selection rectangle and coordinates while dragging (usability)
 * - why is the structure in pymol not being loaded automatically? (usability)
 */

public class Start extends JFrame implements ActionListener, ItemListener {

	/* Constants, TODO: Move to configuration file */

	static final String		DB_HOST =	"white";
	static final String		DB_USER =	getUserName();
	static final String		DB_PWD =	"nieve";
	// Note: the database needs to have chain_graph and single_model_graph tables
	// table and column names are currently hard-coded, TODO: use pdb-file/pdbase as input
	static final String		TEMP_PATH =	"/scratch/local/"; // for temp pdb files
	static final String     HOST = getHostName() ;
	static final String		PYMOL_SERVER_URL = "http://"+HOST+":9123";
	// Note: The pymol server has to be running (by starting pymol -R)
	static String		    GRAPH_DB =	"pdb_reps_graph"; // we set the default here, but can be reset from first argument in command line
	static String			PYMOL_CMD = "pymol -R";
	static String			NULL_CHAIN_CODE = "NULL"; // value important for Msdsd2Pdb
	
	/* Declaration */
	
	private LayoutManager Layout;

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

	private Font SansSerif;
	JButton load, check;
	JPanel loadpanel, selpanel;
	private JTextField tf;
	public String[] val = new String[4];

	public String sql;
	public String pdbFileName;

	private Model mod;
	private View view;
	private PaintController pc;
	private PyMol mypymol;
	
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
	}
	
	/** main function to initialize and show GUI */
	public void PreStartInit() {
		/* Layout settings */
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocation(300,200);

		/* Instantiation */
		SansSerif = new Font ("SansSerif", Font.BOLD, 14);
		Layout = new FlowLayout ();

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
	
	// TODO: Add combo box for distance threshold
	
	/** initialize or refresh the values for the accession code combo box */
	public void fillACChoiceBox() {
		
		/** SQL preparation */
		MySQLConnection con = null;
		Statement  st = null;
		ResultSet  rsacs = null; 	// getting the data for the size of the Contact Map
		ResultSet  rsac = null;	    // getting the data of the accession codes

		/* clear current items for a clean refill */
		ComboSelAc.removeAllItems();
		
		try {

			/** SQL-String takes the data out of the DB */
			String straccesscode = "select distinct accession_code from single_model_graph;";

			/** Database Connection */
			con = new MySQLConnection(DB_HOST,DB_USER,DB_PWD,GRAPH_DB);
			st = con.createStatement();

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
		finally {
			con.close();
		}

	}
	
	/** initialize or refresh the values for the chain code combo box */
	public void fillCCChoiceBox(){

		MySQLConnection con;
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
			con = new MySQLConnection(DB_HOST,DB_USER,DB_PWD,GRAPH_DB);
			st = con.createStatement();

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
			con.close();

		}catch ( Exception ex ) {
			System.out.println( ex );
		}
	}

	/** initialize or refresh the values for the contact type combo box */
	public void fillCTChoiceBox(){
		MySQLConnection con;
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
			con = new MySQLConnection(DB_HOST,DB_USER,DB_PWD,GRAPH_DB);
			st = con.createStatement();

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
			con.close();

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
			val[0]=this.getSelectedAC();
			val[1]=this.getSelectedCC();
			val[2]=this.getSelectedCT();
			val[3]=tf.getText();

			if (val[1].equals(NULL_CHAIN_CODE)){
				sql = this.setSQLString(val[0], "is null", val[2], val[3]);
			}
			else {
				sql = this.setSQLString(val[0], "= '"+val[1] +"'", val[2], val[3]);
			}

			this.Init();
		}

	}

	/** load the contact map window */
	public void Init(){

		/** Initialising the application */ 
		// data
		mod = new Model(this);
		// paint controller 
		pc = new PaintController(this, mod, view);
		// view
		String wintitle = "Contact Map of " + this.getAccessionCode();
		view = new View(this, mod, wintitle, pc, mypymol, PYMOL_SERVER_URL);

		try{
			Msdsd2Pdb.export2File(val[0], val[1], TEMP_PATH + val[0] +".pdb", DB_USER);
		} catch (Exception ex){
			System.out.println(ex);
		}
	}


	/** setting the complete SQL- String */
	public String setSQLString(String accession_code, String chain_pdb_code, String CT, String mindist){

		String sql2 = "select i_num, j_num, single_model_graph.num_nodes, chain_graph.accession_code, chain_graph.chain_pdb_code, "
			+"chain_graph.graph_id, single_model_graph.pgraph_id, single_model_graph.graph_id, single_model_graph.CT,"
			+" single_model_edge.graph_id "
			+" from single_model_edge, single_model_graph, chain_graph "
			+ " where chain_graph.accession_code = '" + accession_code + "' and chain_graph.chain_pdb_code "+ chain_pdb_code 
			+" and chain_graph.graph_id = single_model_graph.pgraph_id and single_model_graph.CT = '" + CT 
			+ "' and single_model_graph.graph_id = single_model_edge.graph_id and i_num > j_num"
			+" and i_num-j_num> '" + mindist + "' order by i_num, j_num;";

		return sql2;
	}

	public String getSQLString(){
		return sql; 
	}

	/** returns the pdb accession code */
	public String getAccessionCode(){
		String ac = val[0];
		return ac;
	}
	/** returns the pdb chain code */
	public String getChainCode(){
		String cc = val[1];
		return cc;
	}

	/** returns the pdb-filename */
	public String getPDBString(){

		pdbFileName  = TEMP_PATH + val[0] + ".pdb";
		System.out.println(pdbFileName);
		return pdbFileName;
	}


	public static void main(String args[]){
		
		// set parameters
		if (args.length>0){
			GRAPH_DB = args[0];
		}
		
		// start pymol
		try {
			// TODO: check whether pymol is running already
			Process pymolProcess = Runtime.getRuntime().exec(PYMOL_CMD);
			// TODO: catch output and wait until pymol is loaded
		} catch(IOException e) {
			System.err.println("Warning: Couldn't start Pymol automatically. Please manually start Pymol with the -R parameter.");
		}
		
		// start gui
		String title = "CM2PyMol";
		Start pstart = new Start(title);
		pstart.PreStartInit();

	}

}
