package cmview;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.*;
import javax.swing.border.*;

import cmview.datasources.ContactMapFileModel;
import cmview.datasources.GraphDbModel;
import cmview.datasources.Model;
import cmview.datasources.ModelConstructionError;
import cmview.datasources.MsdsdModel;
import cmview.datasources.PdbFileModel;
import cmview.datasources.PdbaseModel;
import proteinstructure.*;

/**
 * Main GUI window and associated event handling.
 * Multiple instances of this will be shown in separate windows.
 * 
 * Initialized with mod=null, an empty window with the menu bars is shown.
 * Initialized with a valid model, the contact map is displayed in a ContactMapPane.
 * Multiple instances of this class are created to show multiple contact maps.
 */
public class View extends JFrame implements ActionListener {

	// constants
	static final long serialVersionUID = 1l;
	protected static final int SQUARE_SEL = 1;
	protected static final int FILL_SEL = 2;
	protected static final int NODE_NBH_SEL = 3;
	protected static final int SHOW_COMMON_NBH = 4;
	protected static final int RANGE_SEL = 5;
	
	// menu item labels
	private static final String LABEL_FILE_INFO = "Info";
	private static final String LABEL_FILE_PRINT = "Print";	
	private static final String LABEL_FILE_QUIT = "Quit";	
	private static final String LABEL_DELETE_CONTACTS = "Delete selected contacts";
	private static final String LABEL_SHOW_TRIANGLES_3D = "Show Common Neighbour Triangles in 3D";
	private static final String LABEL_SHOW_COMMON_NBS_MODE = "Show Common Neighbours Mode";
	private static final String LABEL_SHOW_CONTACTS_3D = "Show Selected Contacts in 3D";
	private static final String LABEL_NODE_NBH_SELECTION_MODE = "Node Neighbourhood Selection Mode";
	private static final String LABEL_DIAGONAL_SELECTION_MODE = "Diagonal Selection Mode";
	private static final String LABEL_FILL_SELECTION_MODE = "Fill Selection Mode";
	private static final String LABEL_SQUARE_SELECTION_MODE = "Square Selection Mode";
	protected static final String LABEL_SHOW_PAIR_DIST_3D = "Show residue pair (%s,%s) as edge in 3D";
	
	// GUI components in the main frame
	JPanel statusPane; 			// panel holding the status bar (currently not used)
	JLabel statusBar; 			// TODO: Create a class StatusBar
	JToolBar toolBar;			// icon tool bar (currently not used)
	JPanel cmp; 				// Main panel holding the Contact map pane
	JLayeredPane cmp2; // added for testing
	JPanel topRul;				// Panel for top ruler	// TODO: Move this to ContactMapPane?
	JPanel leftRul;				// Panel for left ruler	// TODO: Move this to ContactMapPane?
	JPopupMenu popup; 			// right-click context menu

	// Tool bar buttons
	JButton tbFileInfo, tbFilePrint, tbFileQuit, tbSquareSel, tbFillSel, tbDiagSel, tbNbhSel, tbShowSel3D, tbShowComNbh, tbShowComNbh3D, tbDelete;
	JToggleButton tbViewPdbResSer, tbViewRuler, tbViewNbhSizeMap, tbViewDistanceMap, tbViewDensityMap;
	
	// Menu items
	JMenuItem sendM, squareM, fillM, comNeiM, triangleM, nodeNbhSelM, rangeM, delEdgesM;
	JMenuItem sendP, squareP, fillP, comNeiP, triangleP, nodeNbhSelP, rangeP, delEdgesP, popupSendEdge;
	JMenuItem mmLoadGraph, mmLoadPdbase, mmLoadMsd, mmLoadCm, mmLoadPdb;
	JMenuItem mmSaveGraph, mmSaveCm, mmSavePng;
	JMenuItem mmViewShowPdbResSers, mmViewHighlightComNbh, mmViewShowDensity, mmViewRulers, mmViewShowDistMatrix;
	JMenuItem mmSelectAll;
	JMenuItem mmColorReset, mmColorPaint, mmColorChoose;
	JMenuItem mmInfo, mmPrint, mmQuit, mmHelpAbout, mmHelpHelp, mmHelpWriteConfig;

	// Data and status variables
	private Model mod;
	public ContactMapPane cmPane;
	public ResidueRuler topRuler;
	public ResidueRuler leftRuler;
	private int pymolSelSerial;		 	// for incremental numbering // TODO: Move this to PymolAdaptor
	private int pymolNbhSerial;			// for incremental numbering // TODO: Move this to PymolAdaptor

	// current gui state
	private int currentAction;			// currently selected action (see constants above)
	private boolean showPdbSers;		// whether showing pdb serials is switched on
	private boolean showRulers;			// whether showing residue rulers is switched on
	private boolean showNbhSizeMap;		// whether showing the common neighbourhood size map is switched on 
	private boolean showDensityMap;		// whether showing the density map is switched on
	private boolean showDistanceMap;	// whether showing the distance map is switched on
	private Color currentPaintingColor;	// current color for coloring contacts selected by the user
	
	// global icons TODO: replace these by tickboxes
	ImageIcon icon_selected = new ImageIcon(this.getClass().getResource("/icons/tick.png"));
	ImageIcon icon_deselected = new ImageIcon(this.getClass().getResource("/icons/bullet_blue.png"));

	/** Create a new View object */
	public View(Model mod, String title) {
		super(title);
		this.mod = mod;
		if(mod == null) {
			this.setPreferredSize(new Dimension(Start.INITIAL_SCREEN_SIZE,Start.INITIAL_SCREEN_SIZE));
		}
		this.currentAction = SQUARE_SEL;
		this.pymolSelSerial = 1;
		this.pymolNbhSerial = 1;
		this.showPdbSers = false;
		this.showNbhSizeMap = false;
		this.showRulers=false;
		this.showDensityMap=false;
		this.showDistanceMap=false;
		this.currentPaintingColor = Color.blue;
		
		this.initGUI(); // build gui tree and show window
		this.toFront(); // bring window to front
	}

	/**
	 * Sets up and returns a new tool bar button
	 */
	private JButton makeToolBarButton(ImageIcon icon, String toolTipText) {
		JButton newButton = new JButton(icon);
		newButton.setToolTipText(toolTipText);
		newButton.addActionListener(this);
		toolBar.add(newButton);
		return newButton;
	}
	
//	/**
//	 * Sets up and returns a new tool bar toggle button
//	 */
//	private JToggleButton makeToolBarToggleButton(ImageIcon icon, String toolTipText) {
//		JToggleButton newButton = new JToggleButton(icon);
//		newButton.setToolTipText(toolTipText);
//		newButton.addActionListener(this);
//		toolBar.add(newButton);
//		return newButton;
//	}
	
	/** Initialize and show the main GUI window */
	private void initGUI(){

		// Setting the main layout 
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocation(20,20);

		// Creating the Panels
		statusPane = new JPanel(); // TODO: Create a class StatusBar
		statusBar = new JLabel(" ");
		statusPane.setLayout(new BorderLayout());
		statusPane.add(statusBar, BorderLayout.CENTER);
		statusPane.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		
		toolBar = new JToolBar();
		cmp = new JPanel(new BorderLayout()); // Contact Map Panel
		cmp2 = new JLayeredPane(); // added for testing
		topRul = new JPanel(new BorderLayout());
		leftRul = new JPanel(new BorderLayout());
			
		// Icons
		ImageIcon icon_square_sel_mode = new ImageIcon(this.getClass().getResource("/icons/shape_square.png"));
		ImageIcon icon_fill_sel_mode = new ImageIcon(this.getClass().getResource("/icons/paintcan.png"));
		ImageIcon icon_diag_sel_mode = new ImageIcon(this.getClass().getResource("/icons/diagonals.png"));
		ImageIcon icon_nbh_sel_mode = new ImageIcon(this.getClass().getResource("/icons/group.png"));
		ImageIcon icon_show_sel_cont_3d = new ImageIcon(this.getClass().getResource("/icons/shape_square_go.png"));
		ImageIcon icon_show_com_nbs_mode = new ImageIcon(this.getClass().getResource("/icons/shape_flip_horizontal.png"));
		ImageIcon icon_show_triangles_3d = new ImageIcon(this.getClass().getResource("/icons/shape_rotate_clockwise.png"));
		ImageIcon icon_del_contacts = new ImageIcon(this.getClass().getResource("/icons/cross.png"));	
		ImageIcon icon_show_pair_dist_3d = new ImageIcon(this.getClass().getResource("/icons/user_go.png"));
		ImageIcon icon_colorwheel = new ImageIcon(this.getClass().getResource("/icons/color_wheel.png"));
		ImageIcon icon_file_info = new ImageIcon(this.getClass().getResource("/icons/information.png"));
		ImageIcon icon_file_print = new ImageIcon(this.getClass().getResource("/icons/printer.png"));		
		ImageIcon icon_file_quit = new ImageIcon(this.getClass().getResource("/icons/door_open.png"));		
		
		// square icon with current painting color
		Icon icon_color = new Icon() {
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Color oldColor = c.getForeground();
				g.setColor(currentPaintingColor);
				g.translate(x, y);
				g.fillRect(2,2,12,12);
				g.translate(-x, -y);  // Restore Graphics object
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
				g.translate(-x, -y);  // Restore Graphics object
				g.setColor(oldColor);
			}	
			public int getIconHeight() {
				return 16;
			}
			public int getIconWidth() {
				return 16;
			}
		};
		
		// Tool bar
		tbFileInfo = makeToolBarButton(icon_file_info, LABEL_FILE_INFO);
		tbFilePrint = makeToolBarButton(icon_file_print, LABEL_FILE_PRINT);
		tbFileQuit = makeToolBarButton(icon_file_quit, LABEL_FILE_QUIT);
		toolBar.addSeparator();
		tbSquareSel = makeToolBarButton(icon_square_sel_mode, LABEL_SQUARE_SELECTION_MODE);
		tbFillSel = makeToolBarButton(icon_fill_sel_mode, LABEL_FILL_SELECTION_MODE);
		tbDiagSel = makeToolBarButton(icon_diag_sel_mode, LABEL_DIAGONAL_SELECTION_MODE);
		tbNbhSel = makeToolBarButton(icon_nbh_sel_mode, LABEL_NODE_NBH_SELECTION_MODE);
		toolBar.addSeparator();		
		tbShowSel3D = makeToolBarButton(icon_show_sel_cont_3d, LABEL_SHOW_CONTACTS_3D);
		toolBar.addSeparator();
		tbShowComNbh = makeToolBarButton(icon_show_com_nbs_mode, LABEL_SHOW_COMMON_NBS_MODE);
		tbShowComNbh3D = makeToolBarButton(icon_show_triangles_3d, LABEL_SHOW_TRIANGLES_3D);
		toolBar.addSeparator();
		tbDelete = makeToolBarButton(icon_del_contacts, LABEL_DELETE_CONTACTS);
		tbViewPdbResSer = new JToggleButton();
		tbViewRuler = new JToggleButton();
		tbViewNbhSizeMap = new JToggleButton();
		tbViewDistanceMap = new JToggleButton();
		tbViewDensityMap = new JToggleButton();
		toolBar.setFloatable(false);
		
		// Popup menu
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		popup = new JPopupMenu();
		
		squareP = new JMenuItem(LABEL_SQUARE_SELECTION_MODE, icon_square_sel_mode);
		fillP = new JMenuItem(LABEL_FILL_SELECTION_MODE, icon_fill_sel_mode);
		rangeP = new JMenuItem(LABEL_DIAGONAL_SELECTION_MODE, icon_diag_sel_mode);
		nodeNbhSelP = new JMenuItem(LABEL_NODE_NBH_SELECTION_MODE, icon_nbh_sel_mode);
		sendP = new JMenuItem(LABEL_SHOW_CONTACTS_3D, icon_show_sel_cont_3d);
		popupSendEdge = new JMenuItem(LABEL_SHOW_PAIR_DIST_3D, icon_show_pair_dist_3d);
		comNeiP = new JMenuItem(LABEL_SHOW_COMMON_NBS_MODE, icon_show_com_nbs_mode);
		triangleP = new JMenuItem(LABEL_SHOW_TRIANGLES_3D, icon_show_triangles_3d);
		delEdgesP = new JMenuItem(LABEL_DELETE_CONTACTS, icon_del_contacts);

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

			// testing: use two separate layers for contacts and interaction
//			TestCmBackgroundPane cmPaneBg = new TestCmBackgroundPane(mod, this);
//			TestCmInteractionPane cmPaneInt = new TestCmInteractionPane(mod, this, cmPaneBg);
//			cmp.setLayout(new OverlayLayout(cmp));
//			cmp.add(cmPaneInt);
//			cmp.add(cmPaneBg);

			// alternative: use JLayeredPane
//			TestCmInteractionPane cmPaneInt = new TestCmInteractionPane(mod, this);
//			TestCmBackgroundPane cmPaneBg = new TestCmBackgroundPane(mod, this);
//			cmp2.setLayout(new OverlayLayout(cmp2));
//			cmp2.add(cmPaneInt, new Integer(2));
//			cmp2.add(cmPaneBg, new Integer(1));
			 //add cmp2 to contentPane
			
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
		mmInfo = new JMenuItem(LABEL_FILE_INFO);
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
		mmPrint = new JMenuItem(LABEL_FILE_PRINT);
		mmPrint.addActionListener(this);
		menu.add(mmPrint);
		mmQuit = new JMenuItem(LABEL_FILE_QUIT);
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

		squareM = new JMenuItem(LABEL_SQUARE_SELECTION_MODE, icon_square_sel_mode);
		fillM = new JMenuItem(LABEL_FILL_SELECTION_MODE, icon_fill_sel_mode);
		rangeM = new JMenuItem(LABEL_DIAGONAL_SELECTION_MODE,icon_diag_sel_mode);
		nodeNbhSelM = new JMenuItem(LABEL_NODE_NBH_SELECTION_MODE, icon_nbh_sel_mode);
		sendM = new JMenuItem(LABEL_SHOW_CONTACTS_3D, icon_show_sel_cont_3d);
		comNeiM = new JMenuItem(LABEL_SHOW_COMMON_NBS_MODE, icon_show_com_nbs_mode);
		triangleM = new JMenuItem(LABEL_SHOW_TRIANGLES_3D, icon_show_triangles_3d);
		delEdgesM = new JMenuItem(LABEL_DELETE_CONTACTS, icon_del_contacts);

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
		mmHelpWriteConfig = new JMenuItem("Write example configuration file");
		mmHelpAbout.addActionListener(this);
		mmHelpHelp.addActionListener(this);
		mmHelpWriteConfig.addActionListener(this);
		menu.add(mmHelpHelp);
		menu.add(mmHelpWriteConfig);
		menu.add(mmHelpAbout);
		menuBar.add(menu);

		this.setJMenuBar(menuBar);
		//this.getContentPane().add(toolBar, BorderLayout.NORTH);
		this.getContentPane().add(cmp,BorderLayout.CENTER);
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

	/**
	 * Handling action events for all menu items.
	 */
	public void actionPerformed (ActionEvent e) {

		/* ---------- File Menu ---------- */
		
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
		if(e.getSource() == mmInfo || e.getSource() == tbFileInfo) {
			handleInfo();
		}		  		  
		if(e.getSource() == mmPrint || e.getSource() == tbFilePrint) {
			handlePrint();
		}		  
		if(e.getSource() == mmQuit || e.getSource() == tbFileQuit) {
			handleQuit();
		}
		
		/* ---------- View Menu ---------- */		
		
		if(e.getSource() == mmViewShowPdbResSers) {
			handleShowPdbResSers();
		}		  
		if(e.getSource() == mmViewRulers) {
			handleShowRulers();
		}	
		if(e.getSource() == mmViewHighlightComNbh) {
			handleShowNbhSizeMap();
		}
		if(e.getSource() == mmViewShowDensity) {
			handleShowDensityMap();
		}
		if(e.getSource() == mmViewShowDistMatrix) {
			handleShowDistanceMap();
		}
		
		/* ---------- Select menu ---------- */
		
		if(e.getSource() == mmSelectAll) {
			handleSelectAll();
		}	
		
		/* ---------- Color menu ---------- */
		
		if(e.getSource() == mmColorReset) {
			handleColorReset();
		}
		if(e.getSource() == mmColorPaint) {
			handleColorPaint();
		}
		if(e.getSource() == mmColorChoose) {
			handleColorSelect();
		}

		/* ---------- Action menu ---------- */

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
			handleShowSelContacts3D();
		}
		// send current edge (only available in context menu)
		if(e.getSource() == popupSendEdge) {
			handleShowDistance3D();
		}
		// send com.Nei. button clicked
		if(e.getSource()== triangleM || e.getSource()== triangleP) {
			handleShowTriangles3D();
		}
		// delete selected edges button clicked
		if (e.getSource() == delEdgesM || e.getSource() == delEdgesP ) {
			handleDeleteSelContacts();
		}			
		
		/* ---------- Help Menu ---------- */
		
		if(e.getSource() == mmHelpAbout) {
			handleHelpAbout();
		}
		if(e.getSource() == mmHelpHelp) {
			handleHelpHelp();
		}
		if(e.getSource() == mmHelpWriteConfig) {
			handleHelpWriteConfig();
		}
	}

	/* -------------------- File Menu -------------------- */
	
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
			LoadDialog dialog = new LoadDialog(this, "Load from Pdbase", new LoadAction() {
				public void doit(Object o, String f, String ac, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
					View view = (View) o;
					view.doLoadFromPdbase(ac, cc, ct, dist, minss, maxss, db);
				}
			}, null, "", "", Start.DEFAULT_CONTACT_TYPE, String.valueOf(Start.DEFAULT_DISTANCE_CUTOFF), "", "", null, null);
			dialog.showIt();
		}

	}

	public void doLoadFromPdbase(String ac, String cc, String ct, double dist, int minss, int maxss, String db) {
		System.out.println("Loading from Pdbase");
		System.out.println("PDB code:\t" + ac);
		System.out.println("Chain code:\t" + cc);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + (minss==-1?"none":minss));
		System.out.println("Max. Seq. Sep.:\t" + (maxss==-1?"none":maxss));
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
		if(!Start.isDatabaseConnectionAvailable()) {
			showNoDatabaseConnectionWarning();
		} else {
			LoadDialog dialog = new LoadDialog(this, "Load from MSD", new LoadAction() {
				public void doit(Object o, String f, String ac, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
					View view = (View) o;
					view.doLoadFromMsd(ac, cc, ct, dist, minss, maxss, db);
				}
			}, null, "", "", Start.DEFAULT_CONTACT_TYPE, String.valueOf(Start.DEFAULT_DISTANCE_CUTOFF), "", "", null, null);
			dialog.showIt();
		}
	}

	public void doLoadFromMsd(String ac, String cc, String ct, double dist, int minss, int maxss, String db) {
		System.out.println("Loading from MSD");
		System.out.println("PDB code:\t" + ac);
		System.out.println("Chain code:\t" + cc);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + (minss==-1?"none":minss));
		System.out.println("Max. Seq. Sep.:\t" + (maxss==-1?"none":maxss));
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
		LoadDialog dialog = new LoadDialog(this, "Load from Pdb file", new LoadAction() {
			public void doit(Object o, String f, String ac, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
				View view = (View) o;
				view.doLoadFromPdbFile(f, cc, ct, dist, minss, maxss);
			}
		}, "", null, "", Start.DEFAULT_CONTACT_TYPE, String.valueOf(Start.DEFAULT_DISTANCE_CUTOFF), "", "", null, null);
		dialog.showIt();
	}

	public void doLoadFromPdbFile(String f, String cc, String ct, double dist, int minss, int maxss) {
		System.out.println("Loading from Pdb file");
		System.out.println("Filename:\t" + f);
		System.out.println("Chain code:\t" + cc);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + (minss==-1?"none":minss));
		System.out.println("Max. Seq. Sep.:\t" + (maxss==-1?"none":maxss));
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
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();
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
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();

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

	/* -------------------- View Menu -------------------- */
	
	/**
	 * 
	 */
	private void handleShowPdbResSers() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if (!mod.has3DCoordinates()){
			showNo3DCoordsWarning();
		} else {
			showPdbSers = !showPdbSers;						// TODO: Move this to CMPane?
			cmPane.updateScreenBuffer();
			if(showPdbSers) {
				mmViewShowPdbResSers.setIcon(icon_selected);
			} else {
				mmViewShowPdbResSers.setIcon(icon_deselected);
			}
		}
	}
	
	/**
	 * 
	 */
	private void handleShowRulers() {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
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
	}
	
	/**
	 * 
	 */
	private void handleShowNbhSizeMap() {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			showNbhSizeMap = !showNbhSizeMap;
			cmPane.toggleNbhSizeMap(showNbhSizeMap);
			if (showNbhSizeMap) {
				mmViewHighlightComNbh.setIcon(icon_selected);
			} else {
				mmViewHighlightComNbh.setIcon(icon_deselected);
			}
		}
	}

	/**
	 * 
	 */
	private void handleShowDensityMap() {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			showDensityMap = !showDensityMap;
			cmPane.toggleDensityMap(showDensityMap);
			if(showDensityMap) {
				mmViewShowDensity.setIcon(icon_selected);
			} else {
				mmViewShowDensity.setIcon(icon_deselected);
			}
		}
	}

	/**
	 * 
	 */
	private void handleShowDistanceMap() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if (!mod.has3DCoordinates()){
			showNo3DCoordsWarning();
		} else if (!AA.isValidSingleAtomCT(mod.getContactType())) {
			showCantShowDistMatrixWarning();
		} else {
			showDistanceMap = !showDistanceMap;
			cmPane.toggleDistanceMap(showDistanceMap);
			if(showDistanceMap) {
				mmViewShowDistMatrix.setIcon(icon_selected);
			} else {
				mmViewShowDistMatrix.setIcon(icon_deselected);
			}
		}
	}

	/* -------------------- Select menu -------------------- */
	
	private void handleSelectAll() {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			cmPane.selectAllContacts();
		}
	}

	/* -------------------- Color menu -------------------- */
	
	private void handleColorSelect() {	
		JDialog chooseDialog = JColorChooser.createDialog(this, "Choose color", true, Start.getColorChooser(), 
				new ActionListener() {
					public void actionPerformed(ActionEvent e){
							Color c = Start.getColorChooser().getColor();
							currentPaintingColor = c;
							System.out.println("Color changed to " + c);
					}
				} , null); // no handler for cancel button
		chooseDialog.setVisible(true);
	}

	private void handleColorPaint() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if(cmPane.getSelContacts().size() == 0) {
			showNoContactsSelectedWarning();
		} else {
			cmPane.paintCurrentSelection(currentPaintingColor);
			cmPane.resetSelections();
			cmPane.repaint();
		}
	}

	private void handleColorReset() {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			cmPane.resetColorMap();
		}
	}

	/* -------------------- Action menu -------------------- */
	
	/**
	 * 
	 */
	private void handleShowSelContacts3D() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if(!mod.has3DCoordinates()) {
			showNo3DCoordsWarning();
		} else if(!Start.isPyMolConnectionAvailable()) {
			showNoPyMolConnectionWarning();
		} else if(cmPane.getSelContacts().size() == 0) {
			showNoContactsSelectedWarning();
		} else {
			Start.getPyMolAdaptor().edgeSelection(mod.getPDBCode(), mod.getChainCode(), pymolSelSerial, cmPane.getSelContacts());
			this.pymolSelSerial++;
		}
	}

	/**
	 * 
	 */
	private void handleShowDistance3D() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if(!mod.has3DCoordinates()) {
			showNo3DCoordsWarning();
		} else if(!Start.isPyMolConnectionAvailable()) {				
			showNoPyMolConnectionWarning();
		} else {
			Start.getPyMolAdaptor().sendSingleEdge(mod.getPDBCode(), mod.getChainCode(), pymolSelSerial, cmPane.getRightClickCont());
			this.pymolSelSerial++;
		}
	}

	/**
	 * 
	 */
	private void handleShowTriangles3D() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if(!mod.has3DCoordinates()) {
			showNo3DCoordsWarning();
		} else if(!Start.isPyMolConnectionAvailable()) {				
			showNoPyMolConnectionWarning();
		} else if(cmPane.getCommonNbh() == null) {
			showNoCommonNbhSelectedWarning();
		} else {
			Start.getPyMolAdaptor().showTriangles(mod.getPDBCode(), mod.getChainCode(), cmPane.getCommonNbh(), pymolNbhSerial); // TODO: get rid of NbhSerial
			this.pymolNbhSerial++;					
		}
	}

	/**
	 * 
	 */
	private void handleDeleteSelContacts() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if(cmPane.getSelContacts().size() == 0) {
			showNoContactsSelectedWarning();
		} else {
			cmPane.deleteSelectedContacts();
		}
	}

	/* -------------------- Help menu -------------------- */
	
	private void handleHelpWriteConfig() {
		try {
			Start.writeExampleConfigFile(Start.CONFIG_FILE_NAME);
			System.out.println("Writing example configuration file " + new File(Start.CONFIG_FILE_NAME).getAbsolutePath());
		} catch(IOException e) {
			System.err.println("Could not write configuration file " + new File(Start.CONFIG_FILE_NAME).getAbsolutePath());
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
	
	/* -------------------- Warnings -------------------- */
	
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
	
	/** 
	 * Create and show a new view window, showing a contact map based on the given model and dispose the current one.
	 * TODO: Currently being abused as a constructor for new windows (something the real constructor should do).
	 */
	public void spawnNewViewWindow(Model mod) {
		String wintitle = "Contact Map of " + mod.getPDBCode() + " " + mod.getChainCode();
		View view = new View(mod, wintitle);
		if(view == null) {
			System.err.println("Error: Couldn't initialize contact map window");
			//System.exit(-1);
		}
		System.out.println("Contact map " + mod.getPDBCode() + " " + mod.getChainCode() + " loaded.");
		if(ContactMapPane.BACKGROUND_LOADING) {
			view.cmPane.preloadBackgroundMaps();
		}

		if (Start.isPyMolConnectionAvailable() && mod.has3DCoordinates()) {
			// load structure in pymol
			Start.getPyMolAdaptor().loadStructure(mod.getTempPdbFileName(), mod.getPDBCode(), mod.getChainCode());
		}		
		// if previous window was empty (not showing a contact map) dispose it
		if(this.mod == null) {
			this.setVisible(false);
			this.dispose();
		}
	}
	
	/* -------------------- getter methods -------------------- */
	// TODO: Make all these parameters of calls to CMPane
	/** 
	 * Returns the currently selected action 
	 */
	public int getCurrentAction(){
		return currentAction;
	}
	
	/** 
	 * Returns whether showing the pdb residue serials is switched on 
	 */
	public boolean getShowPdbSers() {
		return showPdbSers;
	}

	/**  
	 * Returns whether showing the common neighbourhood size map is switched on
	 */
	public boolean getShowNbhSizeMap() {
		return showNbhSizeMap;
	}
	
	/** 
	 * Returns whether showing the density map is switched on
	 */
	public boolean getShowDensityMap() {
		return this.showDensityMap;
	}

	/** 
	 * Returns whether showing the distance map is switched on 
	 */
	public boolean getShowDistMap() {
		return this.showDistanceMap;
	}
	
}

