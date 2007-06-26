package cmview;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.*;
import java.util.HashMap;

import cmview.datasources.ContactMapFileModel;
import cmview.datasources.GraphDbModel;
import cmview.datasources.Model;
import cmview.datasources.ModelConstructionError;
import cmview.datasources.MsdsdModel;
import cmview.datasources.PdbFileModel;
import cmview.datasources.PdbaseModel;
import proteinstructure.*;

/**
 * The main GUI window.
 * 
 * Initialized with mod=null, an empty window with the menu bars is shown.
 * Initialized with a valid model, the contact map is displayed in a ContactMapPane.
 * Multiple instances of this class are created to show multiple contact maps.
 * 
 * @author	Juliane Dinse
 * @author	Henning Stehr
 * @author	Jose Duarte
 * 
 * Class: 	View
 * Package:	cmview
 * Date:	20/02/2007 last update 12/06/2007
 *
 */
public class View extends JFrame implements ActionListener {

	static final long serialVersionUID = 1l;
	protected static final int SQUARE_SEL = 1;
	protected static final int FILL_SEL = 2;
	protected static final int NODE_NBH_SEL = 3;
	protected static final int SHOW_COMMON_NBH = 4;
	protected static final int RANGE_SEL = 5;
	
	// GUI components
	JLabel statusPane; 			// status bar
	JPanel cmp; 				// contact Map Panel
	JLayeredPane layers;		
	JPanel topRul;
	JPanel leftRul;
	JPopupMenu popup; 		// context menu
	JFileChooser fileChooser;
	JColorChooser colorChooser;

	// Menu items
	JMenuItem sendM, squareM, fillM, loadPDBM, comNeiM, triangleM, nodeNbhSelM, rangeM, delEdgesM;
	JMenuItem sendP, squareP, fillP, loadPDBP, comNeiP, triangleP, nodeNbhSelP, rangeP, delEdgesP, popupSendEdge;
	JMenuItem mmLoadGraph, mmLoadPdbase, mmLoadMsd, mmLoadCm, mmLoadPdb;
	JMenuItem mmSaveGraph, mmSaveCm, mmSavePng;
	JMenuItem mmViewShowPdbResSers, mmViewHighlightComNbh, mmViewShowDensity, mmViewRulers, mmViewShowDistMatrix;
	JMenuItem mmSelectAll;
	JMenuItem mmColorReset, mmColorPaint, mmColorChoose;
	JMenuItem mmInfo, mmPrint, mmQuit, mmHelpAbout, mmHelpHelp;

	// Data and status variables
	private Model mod;
	public ContactMapPane cmPane;
	public ResidueRuler topRuler;
	public ResidueRuler leftRuler;
	private PyMolAdaptor pymolAdaptor;
	private String pyMolServerUrl;
	private int currentAction;
	private int pymolSelSerial;		 	// for incrementation numbering TODO: move to Model
	private int pymolNbhSerial;
//	private int topLayer;				// top layer in JLayeredPane

	private boolean doShowPdbSers;
	private boolean highlightComNbh;
	private boolean showRulers;
	private boolean showDensityMatrix;	// if true: show density matrix as background in contact map window
	private boolean showDistMatrix;
	private Color currentPaintingColor;

	private HashMap<Contact,Integer> comNbhSizes;
	
	ImageIcon icon_selected = new ImageIcon(this.getClass().getResource("/icons/tick.png"));
	ImageIcon icon_deselected = new ImageIcon(this.getClass().getResource("/icons/bullet_blue.png"));

	/** Create a new View object */
	public View(Model mod, String title, String pyMolServerUrl) {
		super(title);
		this.mod = mod;
		this.pyMolServerUrl=pyMolServerUrl;
		if(mod == null) {
			this.setPreferredSize(new Dimension(Start.INITIAL_SCREEN_SIZE,Start.INITIAL_SCREEN_SIZE));
		}
		this.initGUI();
		this.currentAction = SQUARE_SEL;
		this.pymolSelSerial = 1;
		this.pymolNbhSerial = 1;
//		this.topLayer = 1;
		this.doShowPdbSers = false;
		this.highlightComNbh = false;
		this.showRulers=false;
		this.showDensityMatrix=false;
		this.showDistMatrix=false;
		this.currentPaintingColor = Color.blue;
		
		fileChooser = new JFileChooser();
		colorChooser = new JColorChooser();
		colorChooser.setPreviewPanel(new JPanel()); // removing the preview panel
	}

	/** Initialize and show the main GUI window */
	private void initGUI(){

		// Setting the main layout 
		setLayout(new BorderLayout());
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocation(20,20);

		// Creating the Panels
		statusPane = new JLabel("Click right mouse button for context menu");
		cmp = new JPanel(new BorderLayout()); // Contact Map Panel
		layers = new JLayeredPane(); // Layered Pane holding multiple contact maps

		topRul = new JPanel(new BorderLayout());
		leftRul = new JPanel(new BorderLayout());
			
		// Icons
		ImageIcon icon1 = new ImageIcon(this.getClass().getResource("/icons/shape_square.png"));
		ImageIcon icon2 = new ImageIcon(this.getClass().getResource("/icons/paintcan.png"));
		ImageIcon icon3 = new ImageIcon(this.getClass().getResource("/icons/diagonals.png"));
		ImageIcon icon4 = new ImageIcon(this.getClass().getResource("/icons/group.png"));
		ImageIcon icon5 = new ImageIcon(this.getClass().getResource("/icons/shape_square_go.png"));
		ImageIcon icon6 = new ImageIcon(this.getClass().getResource("/icons/shape_flip_horizontal.png"));
		ImageIcon icon7 = new ImageIcon(this.getClass().getResource("/icons/shape_rotate_clockwise.png"));
		ImageIcon icon8 = new ImageIcon(this.getClass().getResource("/icons/cross.png"));	
		ImageIcon icon9 = new ImageIcon(this.getClass().getResource("/icons/user_go.png"));
		ImageIcon icon_colorwheel = new ImageIcon(this.getClass().getResource("/icons/color_wheel.png"));
		
		// square icon with current painting color
		Icon icon_color = new Icon() {
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Color oldColor = c.getForeground();
				g.setColor(currentPaintingColor);
				g.translate(x, y);
				g.fillRect(2,2,12,12);
				g.translate(-x, -y);  //Restore Graphics object
				g.setColor(oldColor);
			}			
			public int getIconHeight() {
				return 16;
			}
			public int getIconWidth() {
				return 16;
			}
		};
		
		// black square icon
		Icon icon_black = new Icon() {
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Color oldColor = c.getForeground();
				g.setColor(Color.black);
				g.translate(x, y);
				g.fillRect(2,2,12,12);
				g.translate(-x, -y);  //Restore Graphics object
				g.setColor(oldColor);
			}	
			public int getIconHeight() {
				return 16;
			}
			public int getIconWidth() {
				return 16;
			}
		};
		
		// Popup menu
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		popup = new JPopupMenu();
		
		squareP = new JMenuItem("Square Selection Mode", icon1);
		fillP = new JMenuItem("Fill Selection Mode", icon2);
		rangeP = new JMenuItem("Diagonal Selection Mode", icon3);
		nodeNbhSelP = new JMenuItem("Node Neighbourhood Selection Mode", icon4);
		sendP = new JMenuItem("Show Selected Contacts in PyMol", icon5);
		popupSendEdge = new JMenuItem("Show residue pair as edge in PyMol", icon9);
		comNeiP = new JMenuItem("Show Common Neighbours Mode", icon6);
		triangleP = new JMenuItem("Show Common Neighbour Triangles in PyMol", icon7);
		delEdgesP = new JMenuItem("Delete selected contacts", icon8);

		squareP.addActionListener(this);
		fillP.addActionListener(this);
		rangeP.addActionListener(this);
		nodeNbhSelP.addActionListener(this);
		comNeiP.addActionListener(this);
		sendP.addActionListener(this);
		triangleP.addActionListener(this);
		delEdgesP.addActionListener(this);
		popupSendEdge.addActionListener(this);

		popup.add(squareP);
		popup.add(fillP);
		popup.add(rangeP);
		popup.add(nodeNbhSelP);
		if (Start.USE_PYMOL) {
			popup.addSeparator();
			popup.add(sendP);
			popup.add(popupSendEdge);
		}		
		popup.addSeparator();	
		popup.add(comNeiP);
		if (Start.USE_PYMOL) {
			popup.add(triangleP);
		}		
		popup.addSeparator();
		popup.add(delEdgesP);

		if(mod != null) {
			cmPane = new ContactMapPane(mod, this);			
			cmp.add(cmPane);
			//layers.add(cmPane,topLayer);
			topRuler = new ResidueRuler(cmPane,mod,this,ResidueRuler.TOP);
			leftRuler = new ResidueRuler(cmPane,mod,this,ResidueRuler.LEFT);
			topRul.add(topRuler);
			leftRul.add(leftRuler);
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

		if(Start.USE_DATABASE) {
			submenu.add(mmLoadGraph);
			submenu.add(mmLoadPdbase);
			submenu.add(mmLoadMsd);
		}
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
		mmViewShowPdbResSers = new JMenuItem("Toggle show PDB residue numbers", icon_deselected);
		mmViewHighlightComNbh = new JMenuItem("Toggle highlight of cells by common neighbourhood size", icon_deselected);
		mmViewShowDensity = new JMenuItem("Toggle show contact density", icon_deselected);
		mmViewRulers = new JMenuItem("Toggle rulers", icon_deselected);
		mmViewShowDistMatrix = new JMenuItem("Toggle show distance matrix", icon_deselected);
		menu.add(mmViewShowPdbResSers);
		menu.add(mmViewRulers);
		menu.addSeparator();
		menu.add(mmViewHighlightComNbh);
		menu.add(mmViewShowDensity);
		menu.add(mmViewShowDistMatrix);
		mmViewShowPdbResSers.addActionListener(this);
		mmViewHighlightComNbh.addActionListener(this);
		mmViewRulers.addActionListener(this);
		mmViewShowDensity.addActionListener(this);
		mmViewShowDistMatrix.addActionListener(this);
		menuBar.add(menu);

		// Select menu
		menu = new JMenu("Select");
		menu.setMnemonic(KeyEvent.VK_S);
		mmSelectAll = new JMenuItem("All contacts");
		menu.add(mmSelectAll);
		mmSelectAll.addActionListener(this);
		menuBar.add(menu);
		
		// Color menu
		menu = new JMenu("Color");
		menu.setMnemonic(KeyEvent.VK_C);
		mmColorReset= new JMenuItem("Reset contact colors to black", icon_black);
		mmColorChoose = new JMenuItem("Choose painting color", icon_colorwheel);
		mmColorPaint = new JMenuItem("Color selected contacts", icon_color);
		menu.add(mmColorChoose);
		menu.add(mmColorPaint);
		menu.add(mmColorReset);
		mmColorReset.addActionListener(this);
		mmColorPaint.addActionListener(this);
		mmColorChoose.addActionListener(this);
		menuBar.add(menu);
		
		// Action menu
		menu = new JMenu("Action");
		menu.setMnemonic(KeyEvent.VK_A);

		squareM = new JMenuItem("Square Selection Mode", icon1);
		fillM = new JMenuItem("Fill Selection Mode", icon2);
		rangeM = new JMenuItem("Diagonal Selection Mode",icon3);
		nodeNbhSelM = new JMenuItem("Node Neighbourhood Selection Mode", icon4);
		sendM = new JMenuItem("Show Selected Contacts in PyMol", icon5);
		comNeiM = new JMenuItem("Show Common Neighbours Mode", icon6);
		triangleM = new JMenuItem("Show Common Neighbour Triangles in PyMol", icon7);
		delEdgesM = new JMenuItem("Delete selected contacts", icon8);

		squareM.addActionListener(this);
		fillM.addActionListener(this);
		rangeM.addActionListener(this);
		nodeNbhSelM.addActionListener(this);
		comNeiM.addActionListener(this);
		sendM.addActionListener(this);
		triangleM.addActionListener(this);
		delEdgesM.addActionListener(this);

		menu.add(squareM);
		menu.add(fillM);
		menu.add(rangeM);
		menu.add(nodeNbhSelM);
		if (Start.USE_PYMOL) {
			menu.addSeparator();
			menu.add(sendM);
		}		
		menu.addSeparator();		
		menu.add(comNeiM);
		if (Start.USE_PYMOL) {
			menu.add(triangleM);
		}		
		menu.addSeparator();
		menu.add(delEdgesM);

		menuBar.add(menu);

		// Help menu
		menu = new JMenu("Help");
		menu.setMnemonic(KeyEvent.VK_H);	
		mmHelpAbout = new JMenuItem("About");
		mmHelpHelp = new JMenuItem("Help");
		mmHelpAbout.addActionListener(this);
		mmHelpHelp.addActionListener(this);
		menu.add(mmHelpHelp);
		menu.add(mmHelpAbout);
		menuBar.add(menu);

		this.setJMenuBar(menuBar);
		this.getContentPane().add(cmp,BorderLayout.CENTER);
		//this.getContentPane().add(layers,BorderLayout.CENTER);
		if(showRulers) {
			this.getContentPane().add(topRul, BorderLayout.NORTH);
			this.getContentPane().add(leftRul, BorderLayout.WEST);
		}
		//this.getContentPane().add(statusPane,BorderLayout.SOUTH);

		// Show GUI
		pack();
		setVisible(true);

	}

	/*------------------------------ event handling -------------------------*/

	public void actionPerformed (ActionEvent e) {

		// Action menu

		// square button clicked
		if (e.getSource() == squareM || e.getSource() == squareP) {

			currentAction = SQUARE_SEL;

		}
		// fill button clicked
		if (e.getSource() == fillM || e.getSource() == fillP) {

			currentAction = FILL_SEL;
		}
		// range selection clicked
		if (e.getSource() == rangeM || e.getSource() == rangeP) {

			currentAction = RANGE_SEL;
		}
		// node neihbourhood selection button clicked 
		if (e.getSource() == nodeNbhSelM || e.getSource() == nodeNbhSelP ) {

			currentAction = NODE_NBH_SEL;
		}		
		// showing com. Nei. button clicked
		if (e.getSource() == comNeiM || e.getSource() == comNeiP) {

			currentAction = SHOW_COMMON_NBH;
		}
		// send selection button clicked
		if (e.getSource() == sendM || e.getSource() == sendP) {	
			if(mod==null) {
				showNoContactMapWarning();
			} else if(!mod.has3DCoordinates()) {
				showNo3DCoordsWarning();
			} else if(!Start.isPyMolConnectionAvailable()) {
				showNoPyMolConnectionWarning();
			} else if(cmPane.getSelContacts().size() == 0) {
				showNoContactsSelectedWarning();
			} else {
				pymolAdaptor.edgeSelection(pymolSelSerial, cmPane.getSelContacts());
				this.pymolSelSerial++;
			}
			
		}
		// send com.Nei. button clicked
		if(e.getSource()== triangleM || e.getSource()== triangleP) {
			if(mod==null) {
				showNoContactMapWarning();
			} else if(!mod.has3DCoordinates()) {
				showNo3DCoordsWarning();
			} else if(!Start.isPyMolConnectionAvailable()) {				
				showNoPyMolConnectionWarning();
			} else if(cmPane.getCommonNbh() == null) {
				showNoCommonNbhSelectedWarning();
			} else {
				pymolAdaptor.showTriangles(cmPane.getCommonNbh(),pymolNbhSerial);
				this.pymolNbhSerial++;					
			}
		}
		// delete selected edges button clicked
		if (e.getSource() == delEdgesM || e.getSource() == delEdgesP ) {
			if(mod==null) {
				showNoContactMapWarning();
			} else if(cmPane.getSelContacts().size() == 0) {
				showNoContactsSelectedWarning();
			} else {
				for (Contact cont:cmPane.getSelContacts()){
					mod.delEdge(cont);
				}
				cmPane.reloadContacts();
				cmPane.resetSelContacts();
				cmPane.repaint();
			}
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


		// Color menu
		if(e.getSource() == mmColorReset) {
			handleViewReset();
		}
		if(e.getSource() == mmColorPaint) {
			handleViewColor();
		}
		if(e.getSource() == mmColorChoose) {
			handleViewSelectColor();
		}
		
		// View Menu		
		if(e.getSource() == mmViewShowPdbResSers) {
			if(mod==null) {
				showNoContactMapWarning();
			} else if (!mod.has3DCoordinates()){
				showNo3DCoordsWarning();
			} else {
				doShowPdbSers = !doShowPdbSers;
				if(doShowPdbSers) {
					mmViewShowPdbResSers.setIcon(icon_selected);
				} else {
					mmViewShowPdbResSers.setIcon(icon_deselected);
				}
			}
		}		  
		if(e.getSource() == mmViewRulers) {
			if(mod==null) {
				showNoContactMapWarning();
			} else {
				toggleRulers();
			}
		}	
		if(e.getSource() == mmViewHighlightComNbh) {
			if(mod==null) {
				showNoContactMapWarning();
			} else {
				highlightComNbh = !highlightComNbh;
				if (highlightComNbh) {
					comNbhSizes = mod.getAllEdgeNbhSizes();
					mmViewHighlightComNbh.setIcon(icon_selected);
				} else {
					mmViewHighlightComNbh.setIcon(icon_deselected);
				}
				cmPane.repaint();
			}
		}
		if(e.getSource() == mmViewShowDensity) {
			if(mod==null) {
				showNoContactMapWarning();
			} else {
				cmPane.toggleDensityMatrix();
				if(showDensityMatrix) {
					mmViewShowDensity.setIcon(icon_selected);
				} else {
					mmViewShowDensity.setIcon(icon_deselected);
				}
			}
		}
			  
		if(e.getSource() == mmViewShowDistMatrix) {
			if(mod==null) {
				showNoContactMapWarning();
			} else if (!mod.has3DCoordinates()){
				showNo3DCoordsWarning();
			} else if (!AA.isValidSingleAtomCT(mod.getContactType())) {
				showCantShowDistMatrixWarning();
			} else {
				showDistMatrix = !showDistMatrix;
				if(showDistMatrix) {
					mmViewShowDistMatrix.setIcon(icon_selected);
				} else {
					mmViewShowDistMatrix.setIcon(icon_deselected);
				}
				if (mod.getDistMatrix()==null) mod.initDistMatrix(); //this initalises Model's distMatrix member the first time
				cmPane.repaint();
			}
		}
		
		
		// Select menu
		if(e.getSource() == mmSelectAll) {
			handleSelectAll();
		}		
		
		// Help Menu
		if(e.getSource() == mmHelpAbout) {
			handleHelpAbout();
		}
		if(e.getSource() == mmHelpHelp) {
			handleHelpHelp();
		}	
		
		// Popup actions
		// send current edge
		if(e.getSource() == popupSendEdge) {
			if(mod==null) {
				showNoContactMapWarning();
			} else if(!mod.has3DCoordinates()) {
				showNo3DCoordsWarning();
			} else if(!Start.isPyMolConnectionAvailable()) {				
				showNoPyMolConnectionWarning();
			} else {
				pymolAdaptor.sendSingleEdge(pymolSelSerial, cmPane.getRightClickCont());
				this.pymolSelSerial++;
			}
			
		}

	}

	private void handleLoadFromGraphDb() {
		if(!Start.isDatabaseConnectionAvailable()) {
			showNoDatabaseConnectionWarning();
		} else {
			LoadDialog dialog = new LoadDialog(this, "Load from graph database", new LoadAction() {
				public void doit(Object o, String f, String ac, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
					View view = (View) o;
					view.doLoadFromGraphDb(db, gid);
				}
			}, null, null, null, null, null, null, null, Start.DEFAULT_GRAPH_DB, "");
			dialog.showIt();
		}
	}

	public void doLoadFromGraphDb(String db, int gid) {
		System.out.println("Loading from graph database");
		System.out.println("Database:\t" + db);
		System.out.println("Graph Id:\t" + gid);
		try {
			Model mod = new GraphDbModel(gid, db);
			this.spawnNewViewWindow(mod);
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}


	private void handleLoadFromPdbase() {
		if(!Start.isDatabaseConnectionAvailable()) {
			showNoDatabaseConnectionWarning();
		} else {
	//		String pdbCode = "1tdr";
	//		String chainCode = "B";
	//		String edgeType = "Ca";
	//		double distCutoff = 8.0;
	//		int seqSep = 0;
			LoadDialog dialog = new LoadDialog(this, "Load from Pdbase", new LoadAction() {
				public void doit(Object o, String f, String ac, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
					View view = (View) o;
					view.doLoadFromPdbase(ac, cc, ct, dist, minss, maxss, db);
				}
			}, null, "", "", "", "", "", "", null, null);
			dialog.showIt();
		}

	}

	public void doLoadFromPdbase(String ac, String cc, String ct, double dist, int minss, int maxss, String db) {
		System.out.println("Loading from Pdbase");
		System.out.println("PDB code:\t" + ac);
		System.out.println("Chain code:\t" + cc);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + minss);
		System.out.println("Max. Seq. Sep.:\t" + maxss);
		db = "pdbase";
		System.out.println("Database:\t" + db);	
		try{
			Model mod = new PdbaseModel(ac, cc, ct, dist, minss, maxss, db);
			this.spawnNewViewWindow(mod);
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}

	private void handleLoadFromMsd() {
//		String pdbCode = "1tdr";
//		String chainCode = "B";
//		String edgeType = "Ca";
//		double distCutoff = 8.0;
//		int seqSep = 0;
		if(!Start.isDatabaseConnectionAvailable()) {
			showNoDatabaseConnectionWarning();
		} else {
			LoadDialog dialog = new LoadDialog(this, "Load from MSD", new LoadAction() {
				public void doit(Object o, String f, String ac, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
					View view = (View) o;
					view.doLoadFromMsd(ac, cc, ct, dist, minss, maxss, db);
				}
			}, null, "", "", "", "", "", "", null, null);
			dialog.showIt();
		}
	}

	public void doLoadFromMsd(String ac, String cc, String ct, double dist, int minss, int maxss, String db) {
		System.out.println("Loading from MSD");
		System.out.println("PDB code:\t" + ac);
		System.out.println("Chain code:\t" + cc);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + minss);
		System.out.println("Max. Seq. Sep.:\t" + maxss);
		db = "msdsd_00_07_a";
		System.out.println("Database:\t" + db);	
		try {
			Model mod = new MsdsdModel(ac, cc, ct, dist, minss, maxss, db);
			this.spawnNewViewWindow(mod);
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}	  

	private void handleLoadFromPdbFile() {
//		String chainCode = "A";
//		String edgeType = "Ca";
//		double distCutoff = 8.0;
//		int seqSep = 0;
		LoadDialog dialog = new LoadDialog(this, "Load from Pdb file", new LoadAction() {
			public void doit(Object o, String f, String ac, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
				View view = (View) o;
				view.doLoadFromPdbFile(f, cc, ct, dist, minss, maxss);
			}
		}, "", null, "", "", "", "", "", null, null);
		dialog.showIt();
	}

	public void doLoadFromPdbFile(String f, String cc, String ct, double dist, int minss, int maxss) {
		System.out.println("Loading from Pdb file");
		System.out.println("Filename:\t" + f);
		System.out.println("Chain code:\t" + cc);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + minss);
		System.out.println("Max. Seq. Sep.:\t" + maxss);
		try {
			Model mod = new PdbFileModel(f, cc, ct, dist, minss, maxss);
			this.spawnNewViewWindow(mod);
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}	

	private void handleLoadFromCmFile() {
		LoadDialog dialog = new LoadDialog(this, "Load from Contact map file", new LoadAction() {
			public void doit(Object o, String f, String ac, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
				View view = (View) o;
				view.doLoadFromCmFile(f);
			}
		}, "", null, null, null, null, null, null, null, null);
		dialog.showIt();		  
	}

	public void doLoadFromCmFile(String f) {
		System.out.println("Loading from contact map file "+f);
		try {
			Model mod = new ContactMapFileModel(f);
			this.spawnNewViewWindow(mod);
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}		  

	private void handleSaveToGraphDb() {
		System.out.println("Saving to graph db not implemented yet");
	}

	public void doSaveToGraphDb() {
		// TODO
	}

	private void handleSaveToCmFile() {
		if(this.mod == null) {
			//System.out.println("No contact map loaded yet.");
			showNoContactMapWarning();
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
			//System.out.println("No contact map loaded yet.");
			showNoContactMapWarning();
		} else {
			int ret = fileChooser.showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = fileChooser.getSelectedFile();

				// Create a buffered image in which to draw
				BufferedImage bufferedImage = new BufferedImage(cmPane.getWidth(), cmPane.getHeight(), BufferedImage.TYPE_INT_RGB);

				// Create a graphics contents on the buffered image
				Graphics2D g2d = bufferedImage.createGraphics();

				// Draw the current contact map window to Image
				cmPane.paintComponent(g2d);

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
			//System.out.println("No contact map loaded yet.");
			showNoContactMapWarning();
		} else {
			String seq = mod.getSequence();
			String s = seq.length() <= 10?(seq.length()==0?"Unknown":seq):seq.substring(0,10) + "...";
			String message = "Pdb code: " + mod.getPDBCode() + "\n"
			+ "Chain code: " + mod.getChainCode() + "\n"
			+ "Contact type: " + mod.getContactType() + "\n"
			+ "Distance cutoff: " + mod.getDistanceCutoff() + "\n"
			+ "Min Seq Sep: " + (mod.getMinSequenceSeparation()<1?"none":mod.getMinSequenceSeparation()) + "\n"
			+ "Max Seq Sep: " + (mod.getMaxSequenceSeparation()<1?"none":mod.getMaxSequenceSeparation()) + "\n"
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
		if(this.mod == null) {
			//System.out.println("No contact map loaded yet.");
			showNoContactMapWarning();
		} else {
			PrintUtil.printComponent(this.cmPane);
		}
	}

	private void handleQuit() {
		System.exit(0);
	}

	private void handleViewReset() {
		//System.out.println("View:Reset not implemented yet");
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			cmPane.resetColorMap();
		}
	}	  

	private void handleViewColor() {
		//System.out.println("View:Color by type not implemented yet");
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			cmPane.paintCurrentSelection(currentPaintingColor);
			cmPane.resetSelContacts();
			cmPane.repaint();
		}
	}
	
	private void handleViewSelectColor() {	
		JDialog chooseDialog = JColorChooser.createDialog(this, "Choose color", true, colorChooser, 
				new ActionListener() {
					public void actionPerformed(ActionEvent e){
							Color c = colorChooser.getColor();
							currentPaintingColor = c;
							System.out.println("Color changed to " + c);
					}
				} , null); // no handler for cancel button
		chooseDialog.setVisible(true);
	}	

	private void handleSelectAll() {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			cmPane.selectAllContacts();
		}
	}
	
	private void handleHelpAbout() {
		JOptionPane.showMessageDialog(this,
				"<html><center>" +
				"<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
				"Contact Map Viewer v"+Start.VERSION +
				"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</p>" + 
				"<p>(C) AG Lappe 2007</p>" +
				"</center></html>",
				"About",
				JOptionPane.PLAIN_MESSAGE);
	}
	
	private void handleHelpHelp() {
		JOptionPane.showMessageDialog(this,
				"<html>" +
				"General<br>" +
				"- Click right mouse button in contact map for a context menu of available actions<br>" +
				"<br>" +
				"Square selection mode<br>" +
				"- Click on a contact to select it<br>" +
				"- Drag the mouse to select a rectangular area of contacts<br>" +
				"- Hold 'Ctrl' while selecting to add to the current selection<br>" +
				"- Click on a non-contact to reset the current selection<br>" +
				"<br>" +
				"Fill selection mode<br>" +
				"- Click on a contact to start a fill selection from that contact<br>" +
				"- Hold 'Ctrl' while selecting to add to the current selection<br>" +
				"<br>" +
				"Diagonal selection mode<br>" +
				"- Click to select all contacts along a diagonal<br>" +
				"- Click and drag to select multiple diagonals<br>" +
				"- Hold 'Ctrl' while selecting to add to the current selection<br>" +
				"<br>" +				
				"Node neighbourhood selection mode<br>" +
				"- Click on a residue in the ruler or in the diagonal to select its contacts<br>" +
				"- Click on a cell in the upper half to select all contacts of that pair of residues<br>" +
				"- Hold 'Ctrl' while selecting to add to the current selection<br>" +				
				"<br>" +
				"Show selected contacts in pymol<br>" +
				"- Shows the currently selected contacts as edges in Pymol<br>" +
				"<br>" +
				"Show common neigbours<br>" +
				"- Click on a contact or non-contact to see the common neighbours for that pair of residues<br>" +
				"<br>" +
				"Show common neighbours in pymol<br>" +
				"- Shows the last shown common neighbours as triangles in pymol<br>" +
				"<br>" +
				"Delete selected contacts<br>" +
				"- Permanently deletes the selected contacts from the contact map<br>" +
				"</html>",
				"Help",
				JOptionPane.PLAIN_MESSAGE);
	}	
	
	/** Shows a window with a warning message that no contact map is loaded yet */
	private void showNoContactMapWarning() {
		JOptionPane.showMessageDialog(this, "No contact map loaded yet", "Warning", JOptionPane.INFORMATION_MESSAGE);
	}

	/** Shows a window with a warning message that we can't show distance matrix for this contact type */
	private void showCantShowDistMatrixWarning() {
		JOptionPane.showMessageDialog(this, "Can't show distance matrix for multi atom graph models", "Warning", JOptionPane.INFORMATION_MESSAGE);
	}
	
	/** Warning dialog to be shown if a function is being called which required 3D coordinates and they are missing */
	private void showNo3DCoordsWarning(){
		JOptionPane.showMessageDialog(this, "No 3D coordinates are associated with this contact map", "Warning", JOptionPane.INFORMATION_MESSAGE);
	}
	
	/** Error dialog to be shown if loading a model failed. */
	private void showLoadError(String message) {
		JOptionPane.showMessageDialog(this, "<html>Failed to load structure:<br>" + message + "</html>", "Load Error", JOptionPane.ERROR_MESSAGE);
	}
	
	/** Error dialog to be shown when trying to do a db operation without a db connection */
	private void showNoDatabaseConnectionWarning() {
		JOptionPane.showMessageDialog(this, "Failed to perform operation. No database connection available.", "Warning", JOptionPane.INFORMATION_MESSAGE);
	}
	
	/** Error dialog to be shown when trying to do a db operation without a db connection */
	private void showNoPyMolConnectionWarning() {
		JOptionPane.showMessageDialog(this, "Failed to perform operation. No Communication with PyMol available", "Warning", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private void showNoCommonNbhSelectedWarning() {
		JOptionPane.showMessageDialog(this, "No common neighbourhood selected (Use Show Common Neighbours Mode)", "Warning", JOptionPane.INFORMATION_MESSAGE);		
	}
	
	private void showNoContactsSelectedWarning() {
		JOptionPane.showMessageDialog(this, "No contacts selected", "Warning", JOptionPane.INFORMATION_MESSAGE);				
	}

	/*---------------------------- public methods ---------------------------*/

	/** show/hide rulers and toggle the value of showRulers */
	private void toggleRulers() {
		showRulers = !showRulers;
		if(showRulers) {
			this.getContentPane().add(topRul, BorderLayout.NORTH);
			this.getContentPane().add(leftRul, BorderLayout.WEST);
			mmViewRulers.setIcon(icon_selected);
		} else {
			this.getContentPane().remove(topRul);
			this.getContentPane().remove(leftRul);
			mmViewRulers.setIcon(icon_deselected);
		}
		this.pack();
		this.repaint();
	}
	
	/** Create and show a new view window, showing a contact map based on the given model (and dispose the current one) */
	public void spawnNewViewWindow(Model mod) {
		String wintitle = "Contact Map of " + mod.getPDBCode() + " " + mod.getChainCode();
		View view = new View(mod, wintitle, Start.PYMOL_SERVER_URL);
		if(view == null) {
			System.err.println("Error: Couldn't initialize contact map window");
			//System.exit(-1);
		}
		System.out.println("Contact map " + mod.getPDBCode() + " " + mod.getChainCode() + " loaded.");

		if (Start.isPyMolConnectionAvailable() && mod.has3DCoordinates()) {
			// load structure in pymol
			view.pymolAdaptor = new PyMolAdaptor(this.pyMolServerUrl, 
					mod.getPDBCode(), mod.getChainCode(), mod.getTempPDBFileName());
		}		
		// if previous window was empty (not showing a contact map) dispose it
		if(this.mod == null) {
			this.setVisible(false);
			this.dispose();
		}
//		setModel(mod);
	}
	
//	/** Experimental: Add a new ContactMapPane to the current view */
//	private void setModel(Model mod) {
//		ContactMapPane cmPane = new ContactMapPane(mod, this);
//		layers.add(cmPane,++topLayer);
//		this.pack();
//		this.setVisible(true);
//	}

	/** Returns the status variable currentAction which contains the currently selected actions */
	public int getCurrentAction(){
		return currentAction;
	}
	
//	/** Returns the current painting color set by the user */
//	public Color getCurrentPaintingColor() {
//		return currentPaintingColor;
//	}
	
	/** Returns the status variable doShowPdbSers which indicates whether PDB serials should be displayed */
	public boolean getShowPdbSers() {
		return doShowPdbSers;
	}

	/** Returns the status variable highlightComNbh which indicates whether cells 
	 * should be displayed in different colors based on common neighbourhoods sizes
	 */
	public boolean getHighlightComNbh() {
		return highlightComNbh;
	}
	
	/** Returns the status of the variable showDensityMatrix */
	protected boolean getShowDensityMatrix() {
		return this.showDensityMatrix;
	}
	
	/** Sets the status of the variable showDensityMatrix */
	protected void setShowDensityMatrix(boolean val) {
		this.showDensityMatrix = val;
	}

	/** Returns the status of the variable showDistMatrix */
	protected boolean getShowDistMatrix() {
		return this.showDistMatrix;
	}

	public void setHighlightComNbh(boolean highlightComNbh) {
		this.highlightComNbh=highlightComNbh;
	}
	
	public HashMap<Contact,Integer> getComNbhSizes() {
		return comNbhSizes;
	}
	
}

