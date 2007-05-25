import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.*;

/**
 * The main GUI window.
 * 
 * @author	Juliane Dinse
 * Class: 	View
 * Package:	cm2pymol
 * Date:	20/02/2007 last update 22/05/2007
 *
 */
public class View extends JFrame implements ActionListener{
	
	static final long serialVersionUID = 1l;
		
	// GUI components
	JLabel bpl; 			// status bar
	JPanel cmp; 			// contact Map Panel
	JPopupMenu popup; 		// context menu
	JFileChooser fileChooser = new JFileChooser();

	// Menu items
	JMenuItem sendM, squareM, fillM, loadPDBM, comNeiM, triangleM;
	JMenuItem sendP, squareP, fillP, loadPDBP, comNeiP, triangleP;
	JMenuItem mmLoadGraph, mmLoadPdbase, mmLoadMsd, mmLoadPdb, mmLoadCm;
	JMenuItem mmSaveGraph, mmSaveCm, mmSavePng;
	JMenuItem mmInfo, mmPrint, mmQuit, mmViewReset, mmViewColor, mmHelpAbout;
	
	
	// Data and status variables
	
	private Model mod;
	//public PaintController pc;      // used by Start (load structure in pymol)
	public ContactMapPane pc;
	private PyMolAdaptor tpm;
	private String pyMolServerUrl;
	private int selval;
	private int selINK=0;		 	// for incrementation numbering TODO: move to Model
	
	private String pdbFileName;
	private String selectionType;
	
	// obsolete variables (keep for some time just in case)
	//JButton send, square, fill, loadPDB, comNei, triangle;
	//JPanel bpl; // Button Panel
	//JTextField tx, ty;
	//private int[] pos = new int[2];
	//private String[] text = new String[2];
	//private String s1, s2;
	//private MouseEvent evt;
	//private boolean sendpy;

	/** Create a new View object */
	public View(Model mod, String title, String pyMolServerUrl) {
		super(title);
		this.mod = mod;
		this.pyMolServerUrl=pyMolServerUrl;
		//this.pdbCode = pdbCode;
		//this.chainCode = chainCode;
		if(mod == null) {
			this.setPreferredSize(new Dimension(800,800));
		}
		this.initGUI();

	}
	
	/** Initialize and show the main GUI window */
	private void initGUI(){
		
		// Setting the main layout 
		setLayout(new BorderLayout());
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocation(20,20);

		// Creating the Panels
		//bpl = new JPanel(new GridLayout(2,3)); // Button Panel
		bpl = new JLabel("Click right mouse button for context menu");
		bpl.setAlignmentX(SwingConstants.LEFT);
		cmp= new JPanel(new BorderLayout()); // Contact Map Panel

//		/** Creating the Buttons */
//		square = new JButton("Square Selection on Map");
//		fill = new JButton("Fill Selection on Map");
//		loadPDB = new JButton("Load PDB File in PyMol");
//		send = new JButton("Send Selection to PyMol");
//		comNei = new JButton("Show Common Neighbours on Map");
//		triangle = new JButton("Send Common Neighbours to PyMol");
//
//		/** Adding the ActionListener */
//		square.addActionListener(this);
//		fill.addActionListener(this);
//		comNei.addActionListener(this);
//		loadPDB.addActionListener(this);
//		send.addActionListener(this);
//		triangle.addActionListener(this);
//		
//		/** Adding the Buttons to the Panels */
//
//		bpl.add(square);
//		bpl.add(fill);
//		bpl.add(comNei);
//		bpl.add(loadPDB);
//		bpl.add(send);
//		bpl.add(triangle);
		
		// Adding the context menu
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		popup = new JPopupMenu();
		
		ImageIcon icon1 = new ImageIcon("icons/shape_square.png");
		ImageIcon icon2 = new ImageIcon("icons/paintcan.png");
		ImageIcon icon3 = new ImageIcon("icons/shape_square_go.png");
		ImageIcon icon4 = new ImageIcon("icons/shape_flip_horizontal.png");
		ImageIcon icon5 = new ImageIcon("icons/shape_rotate_clockwise.png");
		ImageIcon icon6 = new ImageIcon("icons/picture_go.png");

//		ImageIcon icon7 = new ImageIcon("icons/information.png");
//		ImageIcon icon8 = new ImageIcon("icons/printer.png");
//		ImageIcon icon9 = new ImageIcon("icons/door_open.png");

		
		squareP = new JMenuItem("Square Selection on Map", icon1);
		fillP = new JMenuItem("Fill Selection on Map", icon2);
		sendP = new JMenuItem("Send Selection to PyMol", icon3);
		comNeiP = new JMenuItem("Show Common Neighbours", icon4);
		triangleP = new JMenuItem("Send Common Neighbours", icon5);
		loadPDBP = new JMenuItem("Load PDB File in PyMol", icon6);
		
		squareP.addActionListener(this);
		fillP.addActionListener(this);
		comNeiP.addActionListener(this);
		loadPDBP.addActionListener(this);
		sendP.addActionListener(this);
		triangleP.addActionListener(this);		
		
		popup.add(squareP);
		popup.add(fillP);
		popup.add(sendP);
		popup.add(comNeiP);
		popup.add(triangleP);
		popup.add(loadPDBP);
		
		if(mod != null) {
			this.pdbFileName = mod.getTempPDBFileName();
			//pc = new PaintController(mod, this);
			pc = new ContactMapPane(mod, this);			
			cmp.add(pc);
		}
		
		// Creating the menu bar
		JMenuBar menuBar;
		JMenu menu, submenu;
		
		menuBar = new JMenuBar();
		
		// File menu
		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		mmInfo = new JMenuItem("Info");
		mmInfo.addActionListener(this);
		menu.add(mmInfo);
		submenu = new JMenu("Load from");
		submenu.setMnemonic(KeyEvent.VK_L);
		
			mmLoadGraph = new JMenuItem("Graph database");
			mmLoadPdbase = new JMenuItem("Pdbase");
			mmLoadMsd = new JMenuItem("MSD");
			mmLoadPdb = new JMenuItem("PDB file");
			mmLoadCm = new JMenuItem("Contact map file");
			
			submenu.add(mmLoadGraph);
			submenu.add(mmLoadPdbase);
			submenu.add(mmLoadMsd);
			submenu.add(mmLoadPdb);
			submenu.add(mmLoadCm);
			
			mmLoadGraph.addActionListener(this);
			mmLoadPdbase.addActionListener(this);			
			mmLoadMsd.addActionListener(this);			
			mmLoadPdb.addActionListener(this);			
			mmLoadCm.addActionListener(this);			
			
		menu.add(submenu);
		submenu = new JMenu("Save to");
		submenu.setMnemonic(KeyEvent.VK_S);
		
			mmSaveGraph = new JMenuItem("Graph database");			
			mmSaveCm = new JMenuItem("Contact map file");
			mmSavePng = new JMenuItem("PNG file");

			//submenu.add(mmSaveGraph);
			submenu.add(mmSaveCm);			
			submenu.add(mmSavePng);
			
			mmSaveGraph.addActionListener(this);			
			mmSaveCm.addActionListener(this);			
			mmSavePng.addActionListener(this);	
			
		menu.add(submenu);
		mmPrint = new JMenuItem("Print");
		mmPrint.addActionListener(this);
		menu.add(mmPrint);
		mmQuit = new JMenuItem("Quit");
		mmQuit.addActionListener(this);
		menu.add(mmQuit);
		menuBar.add(menu);
		
		// View menu
		menu = new JMenu("View");
		menu.setMnemonic(KeyEvent.VK_V);
		mmViewReset= new JMenuItem("Reset");
		mmViewColor = new JMenuItem("Color by contact type");
		menu.add(mmViewReset);
		menu.add(mmViewColor);
		mmViewReset.addActionListener(this);
		mmViewColor.addActionListener(this);		
		//menuBar.add(menu);
		
		// Action menu
		menu = new JMenu("Action");
		menu.setMnemonic(KeyEvent.VK_A);
		
		squareM = new JMenuItem("Square Selection on Map", icon1);
		fillM = new JMenuItem("Fill Selection on Map", icon2);
		sendM = new JMenuItem("Send Selection to PyMol", icon3);
		comNeiM = new JMenuItem("Show Common Neighbours", icon4);
		triangleM = new JMenuItem("Send Common Neighbours", icon5);
		loadPDBM = new JMenuItem("Load PDB File in PyMol", icon6);

		squareM.addActionListener(this);
		fillM.addActionListener(this);
		comNeiM.addActionListener(this);
		loadPDBM.addActionListener(this);
		sendM.addActionListener(this);
		triangleM.addActionListener(this);			
		
		menu.add(squareM);
		menu.add(fillM);
		menu.add(sendM);
		menu.add(comNeiM);
		menu.add(triangleM);
		menu.add(loadPDBM);
		
		menuBar.add(menu);
		
		// Help menu
		menu = new JMenu("Help");
		menu.setMnemonic(KeyEvent.VK_H);	
		mmHelpAbout = new JMenuItem("About");
		mmHelpAbout.addActionListener(this);
		menu.add(mmHelpAbout);
		menuBar.add(menu);
		
		this.setJMenuBar(menuBar);
			
		// Creating the vertical Boxes
		Box verlBox = Box.createVerticalBox();
		verlBox.add(cmp, BorderLayout.CENTER);
		//verlBox.add(bpl, BorderLayout.SOUTH);
		getContentPane().add(verlBox);
		
		// Show GUI
		pack();
		setVisible(true);
		
	}

	/*------------------------------ event handling -------------------------*/
	
	  public void actionPerformed (ActionEvent e) {
		  
		  // Action menu
		  
		  // square button clicked
		  if (e.getSource() == squareM || e.getSource() == squareP) {
			  
				selval = 1;
				selINK = selINK +1;
				selectionType = "Squa";
				
		  }
		  // fill button clicked
		  if (e.getSource() == fillM || e.getSource() == fillP) {
			  
				selval = 2;
				selINK = selINK +1;
				selectionType = "Fill";
		  }
		  // showing com. Nei. button clicked
		  if (e.getSource() == comNeiM || e.getSource() == comNeiP) {
			  
			  	selval = 3;
		  }
		  // loading pdb button clicked
		  if (e.getSource() == loadPDBM || e.getSource() == loadPDBP) {
		
				// TODO: Move object creation to somewhere else
				tpm = new PyMolAdaptor(this.pyMolServerUrl,
									  mod.getPDBCode(), mod.getChainCode(), this.pdbFileName);
						              //this.pdbCode, this.chainCode, this.pdbFileName);
				tpm.MyTestPyMolInit();
				
				   }
		  // send selection button clicked
		  if (e.getSource() == sendM || e.getSource() == sendP) {
			  
			  	   //sendpy =true;	
				   int selval = this.getValue();
				   switch(selval){		   
				   
				   case 1: tpm.SquareCommands(this.getSelNum(), pc.getSelectMatrix(), pc.getSelectRect());
				   case 2: tpm.FillCommands(this.getSelNum(), pc.getSelectMatrix(), mod.getMatrixSize());
				   }
		  }
		  // send com.Nei. button clicked
		  if(e.getSource()== triangleM || e.getSource()== triangleP) {
			  
			  tpm.showTriangles(pc.getTriangleNumber(), pc.getResidues());
		  }
		  
		  // File Menu
		  // Load
		  if(e.getSource() == mmLoadGraph) {
			  handleLoadFromGraphDb();
		  }
		  if(e.getSource() == mmLoadPdbase) {
			  handleLoadFromPdbase();
		  }		  
		  if(e.getSource() == mmLoadMsd) {
			  handleLoadFromMsd();
		  }			  
		  if(e.getSource() == mmLoadPdb) {
			  handleLoadFromPdbFile();
		  }
		  if(e.getSource() == mmLoadCm) {
			  handleLoadFromCmFile();
		  }
		  // Save
		  if(e.getSource() == mmSaveGraph) {
			  handleSaveToGraphDb();
		  }		  
		  if(e.getSource() == mmSaveCm) {
			  handleSaveToCmFile();
		  }
		  if(e.getSource() == mmSavePng) {
			  handleSaveToPng();
		  }	
		  // Info, Print, Quit
		  if(e.getSource() == mmInfo) {
			  handleInfo();
		  }		  		  
		  if(e.getSource() == mmPrint) {
			  handlePrint();
		  }		  
		  if(e.getSource() == mmQuit) {
			  handleQuit();
		  }
		  
		  // View Menu
		  if(e.getSource() == mmViewReset) {
			  handleViewReset();
		  }
		  if(e.getSource() == mmViewColor) {
			  handleViewColor();
		  }		  
		  
		  // Help Menu
		  if(e.getSource() == mmHelpAbout) {
			  handleHelpAbout();
		  }		  
		  
	  }

	  private void handleLoadFromGraphDb() {
		  LoadDialog dialog = new LoadDialog(this, "Load from graph database", new LoadAction() {
			  public void doit(Object o, String f, String ac, String cc, String ct, double dist, int ss, String db, int gid) {
				  View view = (View) o;
				  view.doLoadFromGraphDb(db, gid);
			  }
		  }, null, null, null, null, null, null, "pdb_reps_graph", "");
		  dialog.showIt();
	  }
	  
	  public void doLoadFromGraphDb(String db, int gid) {
		  System.out.println("Loading from graph database");
		  System.out.println("Database:\t" + db);
		  System.out.println("Graph Id:\t" + gid);
		  Model mod = new GraphDbModel(gid, db);
		  this.setModel(mod);
	  }

	  private void handleLoadFromPdbase() {
//		  String pdbCode = "1tdr";
//		  String chainCode = "B";
//		  String edgeType = "Ca";
//		  double distCutoff = 8.0;
//		  int seqSep = 0;
		  LoadDialog dialog = new LoadDialog(this, "Load from Pdbase", new LoadAction() {
			  public void doit(Object o, String f, String ac, String cc, String ct, double dist, int ss, String db, int gid) {
				  View view = (View) o;
				  view.doLoadFromPdbase(ac, cc, ct, dist, ss, db);
			  }
		  }, null, "", "", "", "", "", null, null);
		  dialog.showIt();		  
	  
	  }
	  
	  public void doLoadFromPdbase(String ac, String cc, String ct, double dist, int ss, String db) {
		  System.out.println("Loading from Pdbase");
		  System.out.println("PDB code:\t" + ac);
		  System.out.println("Chain code:\t" + cc);
		  System.out.println("Contact type:\t" + ct);
		  System.out.println("Dist. cutoff:\t" + dist);	
		  System.out.println("Min. Seq. Sep.:\t" + ss);
		  db = "pdbase";
		  System.out.println("Database:\t" + db);	
		  Model mod = new PdbaseModel(ac, cc, ct, dist, ss, db);
		  this.setModel(mod);			  
	  }
	  
	  private void handleLoadFromMsd() {
//		  String pdbCode = "1tdr";
//		  String chainCode = "B";
//		  String edgeType = "Ca";
//		  double distCutoff = 8.0;
//		  int seqSep = 0;
		  LoadDialog dialog = new LoadDialog(this, "Load from MSD", new LoadAction() {
			  public void doit(Object o, String f, String ac, String cc, String ct, double dist, int ss, String db, int gid) {
				  View view = (View) o;
				  view.doLoadFromMsd(ac, cc, ct, dist, ss, db);
			  }
		  }, null, "", "", "", "", "", null, null);
		  dialog.showIt();
	  }
	  
	  public void doLoadFromMsd(String ac, String cc, String ct, double dist, int ss, String db) {
		  System.out.println("Loading from MSD");
		  System.out.println("PDB code:\t" + ac);
		  System.out.println("Chain code:\t" + cc);
		  System.out.println("Contact type:\t" + ct);
		  System.out.println("Dist. cutoff:\t" + dist);	
		  System.out.println("Min. Seq. Sep.:\t" + ss);
		  db = "msdsd_00_07_a";
		  System.out.println("Database:\t" + db);	
		  Model mod = new MsdsdModel(ac, cc, ct, dist, ss, db);
		  this.setModel(mod);			  
	  }	  
	  
	  private void handleLoadFromPdbFile() {
//		  int ret = fileChooser.showOpenDialog(this);
//		  if(ret == JFileChooser.APPROVE_OPTION) {
//			  File chosenFile = fileChooser.getSelectedFile();
//			  String chainCode = "A";
//			  String edgeType = "Ca";
//			  double distCutoff = 8.0;
//			  int seqSep = 0;
		  LoadDialog dialog = new LoadDialog(this, "Load from Pdb file", new LoadAction() {
			  public void doit(Object o, String f, String ac, String cc, String ct, double dist, int ss, String db, int gid) {
				  View view = (View) o;
				  view.doLoadFromPdbFile(f, cc, ct, dist, ss);
			  }
		  }, "", null, "", "", "", "", null, null);
		  dialog.showIt();
	  }

	  public void doLoadFromPdbFile(String f, String cc, String ct, double dist, int ss) {
		  System.out.println("Loading from Pdb file");
		  System.out.println("Filename:\t" + f);
		  System.out.println("Chain code:\t" + cc);
		  System.out.println("Contact type:\t" + ct);
		  System.out.println("Dist. cutoff:\t" + dist);	
		  System.out.println("Min. Seq. Sep.:\t" + ss);
		  Model mod = new PdbFileModel(f, cc, ct, dist, ss);
		  this.setModel(mod);
	  }	
	  
	  private void handleLoadFromCmFile() {
//		  int ret = fileChooser.showOpenDialog(this);
//		  if(ret == JFileChooser.APPROVE_OPTION) {
//			  File chosenFile = fileChooser.getSelectedFile();
//			  Model mod = new ContactMapFileModel(chosenFile.getPath());
//			  this.setModel(mod);
//		  }
		  LoadDialog dialog = new LoadDialog(this, "Load from Contact map file", new LoadAction() {
			  public void doit(Object o, String f, String ac, String cc, String ct, double dist, int ss, String db, int gid) {
				  View view = (View) o;
				  view.doLoadFromCmFile(f);
			  }
		  }, "", null, null, null, null, null, null, null);
		  dialog.showIt();		  
	  }
	  
	  public void doLoadFromCmFile(String f) {
		  System.out.println("Loading from Pdb file");
		  System.out.println("Filename:\t" + f);
		  Model mod = new ContactMapFileModel(f);
		  this.setModel(mod);
	  }		  
	  
	  private void handleSaveToGraphDb() {
		  System.out.println("Saving to graph db not implemented yet");
	  }
	  
	  public void doSaveToGraphDb() {
		  // TODO
	  }
	  
	  private void handleSaveToCmFile() {
		  if(this.mod == null) {
			  System.out.println("No contact map loaded yet.");
		  } else {
			  int ret = fileChooser.showSaveDialog(this);
			  if(ret == JFileChooser.APPROVE_OPTION) {
				  File chosenFile = fileChooser.getSelectedFile();
				  String path = chosenFile.getPath();
				  try {
					  this.mod.writeToContactMapFile(path);
				  } catch(IOException e) {
					  System.err.println("Error writing to file " + path);
				  }
			  }
		  }
	  }
	  
	  private void handleSaveToPng() {
		  if(this.mod == null) {
			  System.out.println("No contact map loaded yet.");
		  } else {
			  int ret = fileChooser.showSaveDialog(this);
			  if(ret == JFileChooser.APPROVE_OPTION) {
				  File chosenFile = fileChooser.getSelectedFile();
				  
			        // Create a buffered image in which to draw
			        BufferedImage bufferedImage = new BufferedImage(pc.getWidth(), pc.getHeight(), BufferedImage.TYPE_INT_RGB);
			    
			        // Create a graphics contents on the buffered image
			        Graphics2D g2d = bufferedImage.createGraphics();
			        
			        // Draw the current contact map window to Image
			        pc.paintComponent(g2d);
			        
			        try {
			        	ImageIO.write(bufferedImage, "png", chosenFile);
			        	System.out.println("File " + chosenFile.getPath() + " saved.");
			        } catch (IOException e) {
			        	System.err.println("Error while trying to write to PNG file " + chosenFile.getPath());
			        }
			  }
		  }
	  }
	  
	  private void handleInfo() {
		  if(this.mod == null) {
			  System.out.println("No contact map loaded yet.");
		  } else {
			  String seq = mod.getSequence();
			  String s = seq.length() <= 10?(seq.length()==0?"Unknown":seq):seq.substring(0,10) + "...";
			  String message = "Pdb code: " + mod.getPDBCode() + "\n"
					  + "Chain code: " + mod.getChainCode() + "\n"
					  + "Contact type: " + mod.getContactType() + "\n"
					  + "Distance cutoff: " + mod.getDistanceCutoff() + "\n"
					  + "Min Seq Sep: " + mod.getSequenceSeparation() + "\n"
					  + "\n"
					  + "Contact map size: " + mod.getMatrixSize() + "\n"
					  + "Unobserved Residues: " + mod.getNumberOfUnobservedResidues() + "\n"
					  + "Number of contacts: " + mod.getNumberOfContacts() + "\n"
					  + "Directed: " + (mod.isDirected()?"Yes":"No")
					  + "\n"
					  + "Sequence: " + s;
			  JOptionPane.showMessageDialog(this,
					  message,
					  "Contact map info",
					  JOptionPane.PLAIN_MESSAGE);
		  }
	  }
	  
	  private void handlePrint() {
		  PrintUtil.printComponent(this.pc);
		  //System.out.println("Printing not implemented yet");
	  }
	  
	  private void handleQuit() {
		  System.exit(0);
	  }
	  
	  private void handleViewReset() {
		  System.out.println("View:Reset not implemented yet");
	  }	  
	  
	  private void handleViewColor() {
		  System.out.println("View:Color by type not implemented yet");
	  }
	  
	  private void handleHelpAbout() {
			JOptionPane.showMessageDialog(this,
				    "           Contact Map Viewer v0.4\n" + 
				    "               (C) AG Lappe 2007",
				    "About",
				    JOptionPane.PLAIN_MESSAGE);
	  }
	  
	  /*---------------------------- public methods ---------------------------*/
	  
	  /** Set the underlying contact map data model */
	  public void setModel(Model mod) {
			String wintitle = "Contact Map of " + mod.getPDBCode() + " " + mod.getChainCode();
			View view = new View(mod, wintitle, Start.PYMOL_SERVER_URL);
			if(view == null) {
				System.err.println("Error: Couldn't initialize contact map window");
				//System.exit(-1);
			}
			System.out.println("Contact map " + mod.getPDBCode() + " " + mod.getChainCode() + " loaded.");
			
			// load structure in pymol
			view.loadPDBM.doClick();
			//view.pc.showContactMap();
			if(this.mod == null) {
				this.setVisible(false);
				this.dispose();
			}
	  }
	  
	  public int getValue(){
		  return selval;
	  }

	  public int getSelNum(){
		  return selINK;
	  }

	  public String getSelectionType(){
		  return selectionType;
	  }
	  

}

