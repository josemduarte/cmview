

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.awt.Dialog.*;

import tools.*;

/**
 * @author:	Juliane Dinse
 * Class: 	Start
 * Package: tools
 * Date:	05/02/2007, updated: 01/03/2007
 * 
 * Start is the starting point of the interaction- visualisation- tool. 
 * Its input parameters are declared by the user and typed in at the right fields of the GUI. 
 * 
 * INPUT: accession_code, chain_pdb_code and CT.
 * 
 * Start includes main-method and initialises other important classes.
 *
 */

public class Start extends JFrame implements ActionListener{


		JButton load;
		JRadioButton backButton, sideButton, allButton;
		JPanel radioPanel, bpl;
		
		static String BB = "BB";
		static String SC = "SC";
		static String ALL = "BB+SC+BB/SC";
	
		private Runtime rt;
		private Process p;
		
		JTextField tf1, tf2;
		public String[] val = new String[3];
		
		public String pdbFileName;

		private Model mod;
		private View view;
		private PaintController pc;
		private PyMol mypymol;
		private static Msdsd2Pdb msd;
		
		private String sqls; 

		
		// constructor 
		public Start(String s){
			super(s);
		}
		
		// initialising the input frame
		public void init(){
			
			
	
			setLayout(new GridLayout(1,2));
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setLocation(300,200);

			
	        //Create the radio buttons.
	        backButton = new JRadioButton(BB);
	        backButton.setActionCommand(BB);
	        backButton.setSelected(true);

	        sideButton = new JRadioButton(SC);
	        sideButton.setActionCommand(SC);
	        
	        allButton = new JRadioButton(ALL);
	        allButton.setActionCommand(ALL);

	        //Group the radio buttons.
	        ButtonGroup group = new ButtonGroup();
	        group.add(backButton);
	        group.add(sideButton);
	        group.add(allButton);
	        
	        //Register a listener for the radio buttons.
	        backButton.addActionListener(this);
	        sideButton.addActionListener(this);
	        allButton.addActionListener(this);
	        	        
	        //Put the radio buttons in a column in a panel.
	        radioPanel = new JPanel(new GridLayout(0, 1));
	        radioPanel.add(backButton);
	        radioPanel.add(sideButton);
	        radioPanel.add(allButton);
	        
	        
			// Creating the Panel for the load-button 
			bpl = new JPanel(new GridLayout(3,1));
	
			// Creating the Button 
			load = new JButton("Load");
			load.setSize(new Dimension(200,200));
			
			// Adding the Action Listener to the button
			load.addActionListener(this);
		
			/** Adding the button to the Panels */
			bpl.add(load);
			
			
			// Creating the vertical Boxes 
			Box verlBox = Box.createVerticalBox();
			
			// Creating TextFields for Input 
			tf1 = new JTextField(); 		// accession_code
			tf2 = new JTextField();			// chain_code

			// Creating textfield labels/headlines and adding them to the box
			verlBox.add(new JLabel("Accession Code"));
			verlBox.add(tf1);
			verlBox.add(new JLabel("Chain Code"));
			verlBox.add(tf2);
			verlBox.add(new JLabel("Contact Type"));
			
			// Adding the panel to the box 
			verlBox.add(radioPanel);
			verlBox.add(bpl);
			
			// Adding all to the ContentPane 
			getContentPane().add(verlBox,  new GridLayout(1,1));
			
			
			// pack matches the size of the window in case of changes 
			pack();
			setVisible(true);
		}
		
		  public void actionPerformed (ActionEvent e) {
			  
			  
			// radio-button selection 
			if (backButton.isSelected() == true){
				val[2] = "BB";
			}  
			if (sideButton.isSelected() == true){
				val[2] = "SC";
			}  
			if (allButton.isSelected() == true){
				val[2] = "BB+SC+BB/SC";
			}  
			
			
			// load-button was clicked
			if (e.getSource()== load){
				/** getting the Input text */
			  val[0] = tf1.getText();
			  val[1] = tf2.getText();
			  val[2] = val[2];
			  
			  
			  // if length of accession code is longer then 4 signs --> wrong code
			  if (val[0].length() > 4){
				  System.out.println("Accession Code is wrong! Note that it has always a length of 4! Please Try again!");
				  return;
			  }
				
			  // if chain_pdb-code in database is "NULL"
				if (val[1].equals("NULL")) {
					
					sqls= this.setSQLString(val[0], "is null",val[2]);
					System.out.println(sqls);
					}
				
				else {sqls = this.setSQLString(val[0], "= '" +val[1] +"' ", val[2]);
				}
			  
			  
			  this.StartInit();
				}
				
			  }
			
	
		  public void StartInit(){
			   
			    /** Initialising the application */ 
			  	// data model
				mod = new Model(this);
				String title = "Contact Map";
				pc = new PaintController(mod, view);
				View view = new View(this, mod, title, pc, mypymol);
				
				msd = new Msdsd2Pdb();
				try{
				msd.export2File(val[0], val[1], "/home/dinse/Desktop/"+val[0] +".pdb", "dinse");
				} catch (Exception e){
					System.out.println(e);
				}
				
	
				
				/** Starting PyMol */
				/**
				try{
					
					rt = Runtime.getRuntime();
					p = rt.exec("/project/StruPPi/PyMolAll/pymol/pymol.exe");

					} 
				
				catch (IOException ex) {
					ex.printStackTrace();
					}*/
		  }
		  
		  /** returns the pdb-filename */
		  public String getPDBString(){
			  
			  //pdbFileName = Dateiname;
			  pdbFileName  = "/home/dinse/Desktop/"+val[0] + ".pdb";
			  System.out.println(pdbFileName);
			  return pdbFileName;
		  }
		  
		  /** returns the complete SQL String*/
		  public String getSQLString(){
			  // String for getting the data out of the db.
			 /** String sql = "select i_num, j_num, single_model_graph.num_nodes, chain_graph.accession_code, chain_graph.chain_pdb_code, "
				  			+"chain_graph.graph_id, single_model_graph.pgraph_id, single_model_graph.graph_id, single_model_graph.CT,"
				  			+" single_model_edge.graph_id "
				  			+" from single_model_edge, single_model_graph, chain_graph "
				  			+ " where chain_graph.accession_code = '" + val[0] + "' and chain_graph.chain_pdb_code = '" + val[1] + 
				  			"' and chain_graph.graph_id = single_model_graph.pgraph_id and single_model_graph.CT = '" + val[2] 
				  			+ "' and single_model_graph.graph_id = single_model_edge.graph_id and i_num > j_num;";
			  */
			  return sqls; 
			  
		  }
		  
		  
		  public String setSQLString(String accession_code, String chain_pdb_code, String CT){
			  //setting the string when chain_pdb_code is NULL
			  
			  //funktioniert noch nicht
			  String sql2 = "select i_num, j_num, single_model_graph.num_nodes, chain_graph.accession_code, chain_graph.chain_pdb_code, "
				  			+"chain_graph.graph_id, single_model_graph.pgraph_id, single_model_graph.graph_id, single_model_graph.CT,"
				  			+" single_model_edge.graph_id "
				  			+" from single_model_edge, single_model_graph, chain_graph "
				  			+ " where chain_graph.accession_code = '" + accession_code + "' and chain_graph.chain_pdb_code "+ chain_pdb_code 
				  			+" and chain_graph.graph_id = single_model_graph.pgraph_id and single_model_graph.CT = '" + CT 
				  			+ "' and single_model_graph.graph_id = single_model_edge.graph_id and i_num > j_num;";
			  
			  return sql2;
		  }

		  public static void main(String[] args){
			  	// main application
				String stitle = "Start";
				Start start = new Start(stitle);
				start.init();
				}
		  }
	



