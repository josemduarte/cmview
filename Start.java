
import java.awt.*;
import java.awt.event.*;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.*;

import tools.Msdsd2Pdb;
import tools.MySQLConnection;
import tools.PyMol;
/**
 * 
 * @author:		Juliane Dinse
 * Class: 		Start
 * Package: 	CM2PyMol
 * Date:		20/02/2007, updated: 29/03/2007
 * 
 * tasks:
 * - initialising the application window
 * - getting the input parameters (accession code, chain code, contact type, minimum distance) 
 * 	 by Choice Boxes (Selection Lists)
 * - initiating other programs
 * - setting the complete SQL-String
 */

public class Start extends JFrame implements ItemListener, ActionListener {
  
	/* Declaration */
  private LayoutManager Layout;
  private Choice Selectorac;	// Selector for accession code
  private Choice Selectorcc;	// Selector for chain pdb code
  private Choice Selectorct;	// selector for contact type
  
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
  
 
  
  public Object [] ComboListAc;
  
  private Font SansSerif;
  JButton load, check;
  JPanel loadpanel, selpanel;
  private JTextField tf, tfac;
  private JFrame f;
  public String[] val = new String[4];

  public String sql;
  public String pdbFileName;
  
  private Model mod;
  private View view;
  private PaintController pc;
  private PyMol mypymol;
  private static Msdsd2Pdb msd;
  
  public Start(String title){
	  super(title);
  }
  
  public void PreStartInit() {
	/* Layout settings */
	setLayout(new BorderLayout());
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	setLocation(300,200);
	
    /* Instantiation */
    SansSerif = new Font ("SansSerif", Font.BOLD, 14);
    Layout = new FlowLayout ();
    Selectorac = new Choice ();
    Selectorcc = new Choice ();
    Selectorct = new Choice ();
    
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
	
//	
//	/* creating button for accession code check */
//	check = new JButton("Check Database");
//    /*adding ActionListener to button */
//	check.addActionListener(this);
//	/* adding button to panel */
//	loadpanel.add(check, new GridLayout(1,1));
	
	/* Creating the Selectors */                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
    Selectorac.setBackground (Color.gray);
    Selectorac.setForeground (Color.lightGray);
    Selectorac.setFont (SansSerif);

    Selectorcc.setBackground (Color.gray);
    Selectorcc.setForeground (Color.lightGray);
    Selectorcc.setFont (SansSerif);
    
    Selectorct.setBackground (Color.gray);
    Selectorct.setForeground (Color.lightGray);
    Selectorct.setFont (SansSerif);

    /* creating textfield */
    tf = new JTextField();
   // tfac = new JTextField();
            
    /* Adding selectors and textlabels to panel */
	//selpanel.add(new JLabel("Search Access. C.:"));
	//selpanel.add(tfac);
	selpanel.add(new JLabel("Accession Code:"));
    selpanel.add(Selectorac);
	//selpanel.add(ComboSelAc);
    selpanel.add(new JLabel("Chain Code:"));
    selpanel.add(Selectorcc);
    //selpanel.add(ComboSelCc);
    selpanel.add(new JLabel("Contact Type:"));
    selpanel.add(Selectorct);
    //selpanel.add(ComboSelCt);
    selpanel.add(new JLabel("Minimum Distance:"));
    selpanel.add(tf);

    /* Adding ItemListener to the Selectors */
    Selectorac.addItemListener (this);
    Selectorcc.addItemListener (this);
    Selectorct.addItemListener (this);
    
    ComboSelAc.addItemListener (this);
    ComboSelCc.addItemListener (this);
    ComboSelCt.addItemListener (this);
    
    
    /* creating the front frame */
	Box verlBox = Box.createVerticalBox();
	/* adding the panels contens to the frame */
	verlBox.add(selpanel, BorderLayout.CENTER);
	verlBox.add(loadpanel, BorderLayout.SOUTH);
	getContentPane().add(verlBox);
	pack();
	setVisible(true);
    
	/** SQL preparation */
	String user ="dinse";
    Statement  st = null;
    ResultSet  rsacs = null; 	// getting the data for the size of the Contact Map
    ResultSet  rsac = null;	    // getting the data of the accession codes
    
    
    try {
    
		/** SQL-String takes the data out of the DB */
    	String straccesscode = "select distinct accession_code from single_model_graph;";

		/** Database Connection */
		MySQLConnection con = new MySQLConnection("white",user,"nieve","pdb_reps_graph");
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
			Object acode= rsac.getObject(1);
			ACodeList[k]= acccode;
			//ComboListAc[k]= acode;
			k++;
		}
		/* adding object content to selector to represent it */
	    for (int i = 0; i < ACodeList.length; i++) {
	        Selectorac.insert (ACodeList [i], i);
	    	//ComboSelAc.addItem(ComboListAc [i]);
	    	
	    
	    }
	  
	}
	catch ( Exception ex ) {
        System.out.println( ex );
	}
  }
  
  /** initialising the ChoiceBox for the Cchain code */
  public void fillCCChoiceBox(String accession_code){
		String user ="dinse";
        Statement  st = null;  
	   
	    ResultSet  rsccs = null;	    // getting the data of the size of the chain pdb codes
	    ResultSet  rscc = null;	    	// getting the data of the chain pdb codes
	    
	    try {
	    	 int n=0;
			/** SQL-String takes the data out of the DB */
	    	String strchainpdb = "select distinct chain_pdb_code from chain_graph where accession_code = '"+accession_code+"' ;";
	   
			/** Database Connection */
			MySQLConnection con = new MySQLConnection("white",user,"nieve","pdb_reps_graph");
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
				if(cccode ==null){cccode = "is null";}
				CCodeList[n]= cccode;
				n++;
			}
			/* adding object content to selector to represent it */
		    for (int i = 0; i < CCodeList.length; i++) {
		        Selectorcc.insert (CCodeList [i], i);
		    	//ComboSelCc.add (CCodeList [i], ComboSelCc);
		    }
		    Selectorcc.select(0);
	    }catch ( Exception ex ) {
	        System.out.println( ex );
		}
  }
  
  /** initialising the ChoiceBox for the Contact Types */
  public void fillCTChoiceBox(String accession_code, String chain_pdb_code){
	  String user ="dinse";
      Statement  st = null;  

	  ResultSet  rscts = null;	    // getting the data of the size of the chain pdb codes
	  ResultSet  rsct = null;	    // getting the data of the chain pdb codes
	    
	    try {
	  	  int n=0;
			/** SQL-String takes the data out of the DB */
	    	if (chain_pdb_code == "is null"){
	    		chain_pdb_code = "is null";
	    	}
	    	else {
	    		chain_pdb_code= "='"+chain_pdb_code + "'";
	    	}
	    	String strct = "select single_model_graph.CT from chain_graph, single_model_graph where chain_graph.accession_code = '" + accession_code 
	    					+ "' and chain_graph.chain_pdb_code "+ chain_pdb_code 
	    					+" and chain_graph.graph_id = single_model_graph.pgraph_id;";
	   
			/** Database Connection */
			MySQLConnection con = new MySQLConnection("white",user,"nieve","pdb_reps_graph");
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
		        Selectorct.insert (CTList [i], i);
		    	//ComboSelCt.add (CTList [i], ComboSelCt);
		    }
		    Selectorct.select(0);
	    }catch ( Exception ex ) {
	        System.out.println( ex );
		}
}
  
  
  public void itemStateChanged(ItemEvent e) {
	 
	
	if(e.getSource() == Selectorac){  
    int selacindex = Selectorac.getSelectedIndex();
    Selectac = Selectorac.getItem(selacindex);
    System.out.println(Selectac);
   
    this.fillCCChoiceBox(Selectac);
	}
	
	
	if(e.getSource()== Selectorcc){
    int selccindex = Selectorcc.getSelectedIndex();
    Selectcc = Selectorcc.getItem(selccindex);
    System.out.println(Selectcc);
    
    String acc = this.getSelectedAC();
    
    this.fillCTChoiceBox(acc, Selectcc);
	}
    
	if(e.getSource()== Selectorct){
	    int selctindex = Selectorct.getSelectedIndex();
	    Selectct = Selectorct.getItem(selctindex);
	    System.out.println(Selectct);
		}
  } 
  
  public String getSelectedAC(){
	  return Selectac;
  }
  
  public String getSelectedCC(){
	  return Selectcc;
  }
  
  public String getSelectedCT(){
	  return Selectct;
  }
  
  
  public void actionPerformed (ActionEvent e) {
	  
	  if (e.getSource()== load){
		  val[0]=this.getSelectedAC();
		  val[1]=this.getSelectedCC();
		  val[2]=this.getSelectedCT();
		  val[3]=tf.getText();
		  
		  if (val[1] == "is null"){
			  sql = this.setSQLString(val[0], val[1], val[2], val[3]);
		  }
		  else {
			  sql = this.setSQLString(val[0], "= '"+val[1] +"'", val[2], val[3]);
		  }
	
		  this.Init();
	  }
	  
	  if (e.getSource() == check){
		  
		  // comparing the input string of the user with the database
		  // handling searching on time ... ex.: searching for 12as ... but just typed 12a_ with missing s
	  }
	  
  }
  
  public void Init(){
	  
	    /** Initialising the application */ 
	  	// data
		mod = new Model(this);
		// paint controller 
		pc = new PaintController(this, mod, view);
		// view
		String wintitle = "Contact Map of " + this.getAccessionCode();
		view = new View(this, mod, wintitle, pc, mypymol);
		
		msd = new Msdsd2Pdb();
		try{
		msd.export2File(val[0], val[1], "/home/dinse/pdb/"+val[0] +".pdb", "dinse");
		} catch (Exception ex){
			System.out.println(ex);
		}
  }
  

  /* setting the complete SQL- String */
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

	  pdbFileName  = "/home/dinse/pdb/"+val[0] + ".pdb";
	  System.out.println(pdbFileName);
	  return pdbFileName;
  }
  
  
  public static void main(String args[]){
	  String title = "Starting Application";
	  Start pstart = new Start(title);
	  pstart.PreStartInit();
	  
	  String s = pstart.getSQLString();
	  System.out.println(s);
	  
  }
  
}
