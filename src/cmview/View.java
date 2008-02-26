package cmview;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.awt.image.BufferedImage;
import javax.imageio.*;
import javax.help.*;

import actionTools.Getter;
import actionTools.GetterError;

import cmview.datasources.*;
import cmview.sadpAdapter.SADPDialog;
import cmview.sadpAdapter.SADPDialogDoneNotifier;
import cmview.sadpAdapter.SADPResult;
import cmview.sadpAdapter.SADPRunner;
import cmview.toolUtils.ToolDialog;
import edu.uci.ics.jung.graph.util.Pair;
import proteinstructure.*;
import proteinstructure.PairwiseSequenceAlignment.PairwiseSequenceAlignmentException;

/**
 * Main GUI window and associated event handling.
 * Multiple instances of this will be shown in separate windows.
 * 
 * Initialized with mod=null, an empty window with the menu bars is shown.
 * Initialized with a valid model, the contact map is displayed in a ContactMapPane.
 * Multiple instances of this class are created to show multiple contact maps.
 */
public class View extends JFrame implements ActionListener {

	/*------------------------------ constants ------------------------------*/
	
	static final long serialVersionUID = 1l;
	private static final boolean FIRST_MODEL = false;
	private static final boolean SECOND_MODEL = true;

	// menu item labels (used in main menu, popup menu and icon bar)
	// File
	private static final String LABEL_FILE_INFO = "Info";
	private static final String LABEL_FILE_PRINT = "Print...";	
	private static final String LABEL_FILE_QUIT = "Quit";
	private static final String LABEL_ALIGNMENT_FILE = "Alignment File...";
	private static final String LABEL_PNG_FILE = "PNG File...";
	private static final String LABEL_CASP_RR_FILE = "CASP RR File...";
	private static final String LABEL_CONTACT_MAP_FILE = "Contact Map File...";
	private static final String LABEL_PDB_FILE = "PDB File...";
	private static final String LABEL_ONLINE_PDB = "Online PDB...";
	private static final String LABEL_MSD = "MSD...";
	private static final String LABEL_PDBASE = "Pdbase...";
	private static final String LABEL_GRAPH_DB = "Graph Database...";
	// Select
	private static final String LABEL_NODE_NBH_SELECTION_MODE = "Neighbourhood Selection Mode";
	private static final String LABEL_DIAGONAL_SELECTION_MODE = "Diagonal Selection Mode";
	private static final String LABEL_FILL_SELECTION_MODE = "Fill Selection Mode";
	private static final String LABEL_SQUARE_SELECTION_MODE = "Square Selection Mode";
	private static final String LABEL_SEL_MODE_COLOR = "Select By Color Mode";
	private static final String LABEL_SHOW_COMMON_NBS_MODE = "Show Common Neighbours Mode";	
	// Action
	private static final String LABEL_DELETE_CONTACTS = "Delete Selected Contacts";
	private static final String LABEL_SHOW_TRIANGLES_3D = "Show Common Neighbour Triangles in 3D";
	private static final String LABEL_SHOW_CONTACTS_3D = "Show Selected Contacts in 3D";
	// Compare
	private static final String LABEL_COMPARE_CM = "Load Second Contact Map From"; 
	private static final String LABEL_SHOW_COMMON = "Show Common Contacts";
	private static final String LABEL_SHOW_FIRST = "Show Contacts Unique to First Structure";
	private static final String LABEL_SHOW_SECOND = "Show Contacts Unique to Second structure";
	protected static final String LABEL_SHOW_PAIR_DIST_3D = "Show Residue Pair (%s,%s) as Edge in 3D";	// used in ContactMapPane.showPopup

	/*--------------------------- member variables --------------------------*/
	
	private static int pymolSelSerial = 1;		 	// for incremental numbering // TODO: Move this to PymolAdaptor
	
	// GUI components in the main frame
	JPanel statusPane; 			// panel holding the status bar (currently not used)
	JLabel statusBar; 			// TODO: Create a class StatusBar
	JToolBar toolBar;			// icon tool bar 
	JPanel cmp; 				// Main panel holding the Contact map pane
	JPanel topRul;				// Panel for top ruler	// TODO: Move this to ContactMapPane?
	JPanel leftRul;				// Panel for left ruler	// TODO: Move this to ContactMapPane?
	JPopupMenu popup; 			// right-click context menu
	JPanel tbPane;				// tool bar panel holding toolBar and cmp (necessary if toolbar is floatable)

	// Tool bar buttons
	JButton tbFileInfo, tbShowSel3D, tbShowComNbh3D,  tbDelete;  
	JToggleButton tbSquareSel, tbFillSel, tbDiagSel, tbNbhSel, tbShowComNbh, tbSelModeColor;
	JToggleButton tbViewPdbResSer, tbViewRuler, tbViewNbhSizeMap, tbViewDistanceMap, tbViewDensityMap, tbShowCommon, tbShowFirst, tbShowSecond;

	// indices of the all main menus in the frame's menu bar
	TreeMap<String, Integer> menu2idx;

	// contains all types of component that shall not be 
	// considered for the right setting of the visibility 
	// mode of a menu
	HashSet<Class<?>> disregardedTypes;

	HashMap<JPopupMenu,JMenu> popupMenu2Parent;

	TreeMap<String,JMenu> smFile;
	TreeMap<String,JMenu> smCompare;

	// Menu items
	// M -> "menu bar"
	JMenuItem sendM, squareM, fillM, comNeiM, triangleM, nodeNbhSelM, rangeM, delEdgesM, mmSelModeColor;
	// P -> "popup menu"
	JMenuItem sendP, squareP, fillP, comNeiP, triangleP, nodeNbhSelP, rangeP,  delEdgesP, popupSendEdge, pmSelModeColor;
	// mm -> "main menu"
	JMenuItem mmLoadGraph, mmLoadPdbase, mmLoadMsd, mmLoadCm, mmLoadCaspRR, mmLoadPdb, mmLoadFtp;
	JMenuItem mmLoadGraph2, mmLoadPdbase2, mmLoadMsd2, mmLoadCm2, mmLoadCaspRR2, mmLoadPdb2, mmLoadFtp2;
	JMenuItem mmSaveGraphDb, mmSaveCmFile, mmSaveCaspRRFile, mmSavePng, mmSaveAli;
	JMenuItem mmViewShowPdbResSers, mmViewHighlightComNbh, mmViewShowDensity, mmViewRulers, mmViewIconBar, mmViewShowDistMatrix;
	JMenuItem mmSelectAll, mmSelectByResNum, mmSelectHelixHelix, mmSelectBetaBeta, mmSelectInterSsContacts, mmSelectIntraSsContacts;
	JMenuItem mmColorReset, mmColorPaint, mmColorChoose;
	JMenuItem mmShowCommon,  mmShowFirst,  mmShowSecond;
	JMenuItem mmToggleDiffDistMap;
	JMenuItem mmSuperposition, mmShowAlignedResidues;
	JMenuItem mmInfo, mmPrint, mmQuit, mmHelpAbout, mmHelpHelp, mmHelpWriteConfig;

	// Data and status variables
	private GUIState guiState;
	private Model mod;
	private Model mod2;
	private Alignment ali;
	public ContactMapPane cmPane;
	public ResidueRuler topRuler;
	public ResidueRuler leftRuler;

	// global icons TODO: replace these by tickboxes?
	ImageIcon icon_selected = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "tick.png"));
	ImageIcon icon_deselected = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "bullet_blue.png"));

	LoadDialog actLoadDialog;

	// invisible notifiers
	SADPDialogDoneNotifier sadpNotifier;

	/*----------------------------- constructors ----------------------------*/

	/** Create a new View object */
	public View(Model mod, String title) {
		super(title);
		Start.viewInstancesCreated();
		this.mod = mod;
		if(mod == null) {
			this.setPreferredSize(new Dimension(Start.INITIAL_SCREEN_SIZE,Start.INITIAL_SCREEN_SIZE));
		}
		this.guiState = new GUIState(this);
		this.initGUI(); 							// build gui tree and show window
		final JFrame parent = this;					// need a final to refer to in the thread below
		EventQueue.invokeLater(new Runnable() {		// execute after other events have been processed
			public void run() {
				parent.toFront();					// bring new window to front
			}
		});
	}
	
	/**
	 * Called when this class is about to be disposed
	 */
	public void dispose() {
		super.dispose();
		if(Start.viewInstanceDisposed() == 0) {
			// no more views open, shut down
			// Note that the shutdown hook for the virtual machine will still be executed
			Start.shutDown(0);
		}
	}

	/*---------------------------- private methods --------------------------*/

	/**
	 * Sets up and returns a new menu item with the given icon and label, adds it to the given JMenu and
	 * registers 'this' as the action listener.
	 * @param label the text of the item in the menu
	 * @param icon an optional icon to be shown in the menu
	 * @param menu the JMenu this item will be added to, or null if the item should be invisible
	 */
	private JMenuItem makeMenuItem(String label, Icon icon, JMenu menu) {
		JMenuItem newItem = new JMenuItem(label, icon);
		newItem.addActionListener(this);
		if(menu != null) menu.add(newItem);
		return newItem;
	}
	
	/**
	 * Sets up a help menu item with the given icon and label, adds it to the given JMenu and registers
	 * an action listener which will open a help window.
	 * @param label the label of the new help menu item
	 * @param icon the icon of the new help menu item
	 * @param menu the parent menu this menu item should be added to
	 * @return the new menu item
	 */
	private JMenuItem makeHelpMenuItem(String label, Icon icon, JMenu menu) {
		JMenuItem newItem = new JMenuItem(label, icon);
		menu.add(newItem);
		try {
			// init helpset
			URL url = this.getClass().getResource(Start.HELPSET);
			HelpSet hs = new HelpSet(null, url);
			HelpBroker hb = hs.createHelpBroker();
			hb.setCurrentID("top");
			
			// register help actions
			hb.enableHelpKey(this.getRootPane(), "top", hs);
			newItem.addActionListener(new CSH.DisplayHelpFromSource(hb));
			
//			// define help ids (couldn't get this to work properly)
//			CSH.setHelpIDString(this.getRootPane(), "top");
//			CSH.setHelpIDString(tbFileInfo, "menu.file.info");
//			
//			CSH.setHelpIDString(tbSquareSel, "menu.select.mode.rect");
//			CSH.setHelpIDString(tbFillSel, "menu.select.mode.fill");
//			CSH.setHelpIDString(tbDiagSel, "menu.select.mode.diag");
//			CSH.setHelpIDString(tbNbhSel, "menu.select.mode.nbh");
//			CSH.setHelpIDString(tbShowComNbh, "menu.select.mode.nbh");
//			CSH.setHelpIDString(tbSelModeColor, "menu.select.mode.color");
//			
//			CSH.setHelpIDString(tbShowSel3D, "menu.action.show3d");
//			CSH.setHelpIDString(tbDelete, "menu.action.delete");
//
//			CSH.setHelpIDString(tbShowCommon, "menu.compare.showcommon");
//			CSH.setHelpIDString(tbShowFirst, "menu.compare.showfirst");
//			CSH.setHelpIDString(tbShowSecond, "menu.compare.showsecond");
			
		} catch(HelpSetException e) {
			System.err.println("Severe error. Could not initialize inline help: " + e.getMessage());
		}
		return newItem;
	}

	/**
	 * Sets up and returns a new popup menu item with the given icon and label, adds it to the given JPopupMenu and
	 * registers 'this' as the action listener.
	 */	
	private JMenuItem makePopupMenuItem(String label, Icon icon, JPopupMenu menu) {
		JMenuItem newItem = new JMenuItem(label, icon);
		newItem.addActionListener(this);
		menu.add(newItem);
		return newItem;		
	}

	/**
	 * Sets up and returns a new tool bar button
	 */
	private JButton makeToolBarButton(ImageIcon icon, String toolTipText) {
		JButton newButton = new JButton(icon);
		newButton.setFocusPainted(false);
		newButton.setToolTipText(toolTipText);
		newButton.addActionListener(this);
		toolBar.add(newButton);
		return newButton;
	}

	/**
	 * Sets up and returns a new tool bar toggle button
	 */
	private JToggleButton makeToolBarToggleButton(ImageIcon icon, String toolTipText, boolean selected, boolean enabled, boolean visible) {
		JToggleButton newButton = new JToggleButton(icon, selected);
		newButton.setFocusPainted(false);
		newButton.setToolTipText(toolTipText);
		newButton.addActionListener(this);
		newButton.setVisible(visible);
		newButton.setEnabled(enabled);
		newButton.setSelected(selected);		
		toolBar.add(newButton);
		return newButton;
	}

	/**
	 * Creates an icon which changes the color with the variable 'currentPaintingColor'.
	 * @return The 'magic' icon
	 */
	private Icon getCurrentColorIcon() {
		return new Icon() {
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Color oldColor = c.getForeground();
				g.setColor(guiState.getPaintingColor());
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
	}

	/**
	 * Creates a black-square icon.
	 * @return The black icon
	 */
	private Icon getBlackSquareIcon() {
		return new Icon() {
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
	}

	/** Initialize and show the main GUI window */
	private void initGUI(){

		// Setting the main layout 
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocation(20,20);

		// Creating the Panels
		tbPane = new JPanel(new BorderLayout());	// toolbar pane, holding only toolbar and cmp (all the rest)
		cmp = new JPanel(new BorderLayout()); 		// pane holding the cmPane and (optionally) the ruler panes
		topRul = new JPanel(new BorderLayout()); 	// pane holding the top ruler
		leftRul = new JPanel(new BorderLayout()); 	// pane holding the left ruler
		statusPane = new JPanel(); 					// pane holding the status bar, TODO: Create a class StatusBar
		statusBar = new JLabel(" ");				// a primitive status bar for testing

		// Icons
		ImageIcon icon_square_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "shape_square.png"));
		ImageIcon icon_fill_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "paintcan.png"));
		ImageIcon icon_diag_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "diagonals.png"));
		ImageIcon icon_nbh_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "group.png"));
		ImageIcon icon_show_sel_cont_3d = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "shape_square_go.png"));
		ImageIcon icon_show_com_nbs_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "shape_flip_horizontal.png"));
		ImageIcon icon_show_triangles_3d = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "shape_rotate_clockwise.png"));
		ImageIcon icon_del_contacts = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "cross.png"));	
		ImageIcon icon_show_pair_dist_3d = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "user_go.png"));
		ImageIcon icon_colorwheel = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "color_wheel.png"));
		ImageIcon icon_file_info = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "information.png"));						
		ImageIcon icon_show_common = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "page_copy.png"));
		ImageIcon icon_show_first = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "page_delete.png"));
		ImageIcon icon_show_second = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "page_add.png"));
		ImageIcon icon_sel_mode_color = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "color_swatch.png"));
		Icon icon_color = getCurrentColorIcon();	// magic icon with current painting color
		Icon icon_black = getBlackSquareIcon();		// black square icon

		// Tool bar
		toolBar = new JToolBar();
		toolBar.setVisible(Start.SHOW_ICON_BAR);
		Dimension separatorDim = new Dimension(30,toolBar.getHeight());
		tbFileInfo = makeToolBarButton(icon_file_info, LABEL_FILE_INFO);
		toolBar.addSeparator(separatorDim);
		tbSquareSel = makeToolBarToggleButton(icon_square_sel_mode, LABEL_SQUARE_SELECTION_MODE, true, true, true);
		tbFillSel = makeToolBarToggleButton(icon_fill_sel_mode, LABEL_FILL_SELECTION_MODE, false, true, true);
		tbDiagSel = makeToolBarToggleButton(icon_diag_sel_mode, LABEL_DIAGONAL_SELECTION_MODE, false, true, true);
		tbNbhSel = makeToolBarToggleButton(icon_nbh_sel_mode, LABEL_NODE_NBH_SELECTION_MODE, false, true, true);
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			tbShowComNbh = makeToolBarToggleButton(icon_show_com_nbs_mode, LABEL_SHOW_COMMON_NBS_MODE, false, true, true);
		}
		tbSelModeColor = makeToolBarToggleButton(icon_sel_mode_color, LABEL_SEL_MODE_COLOR, false, true, true);
		toolBar.addSeparator(separatorDim);		
		tbShowSel3D = makeToolBarButton(icon_show_sel_cont_3d, LABEL_SHOW_CONTACTS_3D);
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			tbShowComNbh3D = makeToolBarButton(icon_show_triangles_3d, LABEL_SHOW_TRIANGLES_3D);
		}
		tbDelete = makeToolBarButton(icon_del_contacts, LABEL_DELETE_CONTACTS);
		toolBar.addSeparator(separatorDim);
		tbShowCommon = makeToolBarToggleButton(icon_show_common, LABEL_SHOW_COMMON, guiState.getShowCommon(), false, false);
		tbShowFirst = makeToolBarToggleButton(icon_show_first, LABEL_SHOW_FIRST, guiState.getShowFirst(), false, false);
		tbShowSecond = makeToolBarToggleButton(icon_show_second, LABEL_SHOW_SECOND, guiState.getShowSecond(), false, false);

		// Toggle buttons in view menu (not being used yet)
		tbViewPdbResSer = new JToggleButton();
		tbViewRuler = new JToggleButton();
		tbViewNbhSizeMap = new JToggleButton();
		tbViewDistanceMap = new JToggleButton();
		tbViewDensityMap = new JToggleButton();
		toolBar.setFloatable(false);	// does currently not work properly if floatable

		// ButtonGroup for selection modes (so upon selecting one, others are deselected automatically)
		ButtonGroup selectionModeButtons = new ButtonGroup();
		selectionModeButtons.add(tbSquareSel);
		selectionModeButtons.add(tbFillSel);
		selectionModeButtons.add(tbDiagSel);
		selectionModeButtons.add(tbSelModeColor);
		selectionModeButtons.add(tbNbhSel);
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			selectionModeButtons.add(tbShowComNbh);
		}

		// Popup menu
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		popup = new JPopupMenu();

		squareP = makePopupMenuItem(LABEL_SQUARE_SELECTION_MODE, icon_square_sel_mode, popup);
		fillP = makePopupMenuItem(LABEL_FILL_SELECTION_MODE, icon_fill_sel_mode, popup);
		rangeP = makePopupMenuItem(LABEL_DIAGONAL_SELECTION_MODE, icon_diag_sel_mode, popup);
		nodeNbhSelP = makePopupMenuItem(LABEL_NODE_NBH_SELECTION_MODE, icon_nbh_sel_mode, popup);
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			comNeiP = makePopupMenuItem(LABEL_SHOW_COMMON_NBS_MODE, icon_show_com_nbs_mode, popup);
		}
		pmSelModeColor = makePopupMenuItem(LABEL_SEL_MODE_COLOR, icon_sel_mode_color, popup);
		if (Start.USE_PYMOL) {
			popup.addSeparator();		
			sendP = makePopupMenuItem(LABEL_SHOW_CONTACTS_3D, icon_show_sel_cont_3d, popup);
			popupSendEdge = makePopupMenuItem(LABEL_SHOW_PAIR_DIST_3D, icon_show_pair_dist_3d, popup);
			if(Start.USE_EXPERIMENTAL_FEATURES) {
				triangleP = makePopupMenuItem(LABEL_SHOW_TRIANGLES_3D, icon_show_triangles_3d, popup);
			}
		}
		popup.addSeparator();		
		delEdgesP = makePopupMenuItem(LABEL_DELETE_CONTACTS, icon_del_contacts, popup);

		// Main menu
		JMenuBar menuBar;
		JMenu menu, submenu;
		menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);
		menu2idx = new TreeMap<String, Integer>();
		smFile = new TreeMap<String, JMenu>();
		smCompare = new TreeMap<String, JMenu>();
		popupMenu2Parent = new HashMap<JPopupMenu, JMenu>();

		// File menu
		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		mmInfo = makeMenuItem(LABEL_FILE_INFO, null, menu);		
		// Load
		submenu = new JMenu("Load from");
		popupMenu2Parent.put(submenu.getPopupMenu(),submenu);
		if(Start.USE_DATABASE && Start.USE_EXPERIMENTAL_FEATURES) {
			mmLoadGraph = makeMenuItem(LABEL_GRAPH_DB,null,submenu);
			mmLoadPdbase = makeMenuItem(LABEL_PDBASE,null,submenu);
			mmLoadMsd = makeMenuItem(LABEL_MSD,null, submenu);
		}		
		mmLoadFtp = makeMenuItem(LABEL_ONLINE_PDB, null, submenu);
		mmLoadPdb = makeMenuItem(LABEL_PDB_FILE, null, submenu);
		mmLoadCm = makeMenuItem(LABEL_CONTACT_MAP_FILE, null, submenu);
		mmLoadCaspRR = makeMenuItem(LABEL_CASP_RR_FILE, null, submenu);
		menu.add(submenu);
		smFile.put("Load", submenu);
		// Save
		submenu = new JMenu("Save to");
		popupMenu2Parent.put(submenu.getPopupMenu(),submenu);
		mmSaveCmFile = makeMenuItem(LABEL_CONTACT_MAP_FILE, null, submenu);
		mmSaveCaspRRFile = makeMenuItem(LABEL_CASP_RR_FILE, null, submenu);
		mmSavePng = makeMenuItem(LABEL_PNG_FILE, null, submenu);
		if(Start.USE_DATABASE && Start.USE_EXPERIMENTAL_FEATURES) {
			mmSaveGraphDb = makeMenuItem(LABEL_GRAPH_DB, null, submenu);
		}
		mmSaveAli = makeMenuItem(LABEL_ALIGNMENT_FILE, null, submenu);
		menu.add(submenu);
		smFile.put("Save", submenu);
		// Print, Quit
		mmPrint = makeMenuItem(LABEL_FILE_PRINT, null, null);					// function disabled
		mmQuit = makeMenuItem(LABEL_FILE_QUIT, null, menu);
		addToJMenuBar(menu);

		// View menu
		menu = new JMenu("View");
		menu.setMnemonic(KeyEvent.VK_V);		
		mmViewShowPdbResSers = makeMenuItem("Show PDB Residue Numbers", icon_deselected, null);  // function disabled
		mmViewRulers = makeMenuItem("Show Rulers", icon_deselected, null);		// function disabled
		mmViewIconBar = makeMenuItem("Show Icon Bar", icon_deselected, null);	// function disabled
		//menu.addSeparator(); // not needed since all functions above are disabled
		if (Start.USE_EXPERIMENTAL_FEATURES) {
			mmViewHighlightComNbh = makeMenuItem("Show Common Neighbourhood Sizes", icon_deselected, menu);
		} 
		mmViewShowDensity = makeMenuItem("Show Contact Density", icon_deselected, menu);
		mmViewShowDistMatrix = makeMenuItem("Show Distance Map", icon_deselected, menu);
		addToJMenuBar(menu);

		// Select menu
		menu = new JMenu("Select");
		menu.setMnemonic(KeyEvent.VK_S);
		submenu = new JMenu("Selection Mode");
		squareM = makeMenuItem(LABEL_SQUARE_SELECTION_MODE, icon_square_sel_mode, submenu);
		fillM = makeMenuItem(LABEL_FILL_SELECTION_MODE, icon_fill_sel_mode, submenu);
		rangeM = makeMenuItem(LABEL_DIAGONAL_SELECTION_MODE,icon_diag_sel_mode, submenu);
		nodeNbhSelM = makeMenuItem(LABEL_NODE_NBH_SELECTION_MODE, icon_nbh_sel_mode, submenu);
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			comNeiM = makeMenuItem(LABEL_SHOW_COMMON_NBS_MODE, icon_show_com_nbs_mode, submenu);
		}
		mmSelModeColor = makeMenuItem(LABEL_SEL_MODE_COLOR, icon_sel_mode_color, submenu);
		menu.add(submenu);
		menu.addSeparator();
		mmSelectAll = makeMenuItem("All Contacts", null, menu);
		mmSelectByResNum = makeMenuItem("By Residue Number...", null, menu);
		menu.addSeparator();
		mmSelectHelixHelix = makeMenuItem("Helix-Helix Contacts", null, menu);
		mmSelectBetaBeta = makeMenuItem("Strand-Strand Contacts", null, menu);
		mmSelectInterSsContacts = makeMenuItem("Contacts Between SS Elements", null, menu);
		mmSelectIntraSsContacts = makeMenuItem("Contacts Within SS Elements", null, menu);		
		addToJMenuBar(menu);

		// Color menu
		menu = new JMenu("Color");
		menu.setMnemonic(KeyEvent.VK_C);
		mmColorChoose = makeMenuItem("Choose Color...", icon_colorwheel, menu);
		mmColorPaint = makeMenuItem("Color Selected Contacts", icon_color, menu);
		mmColorReset= makeMenuItem("Reset Contact Colors to Black", icon_black, menu);
		addToJMenuBar(menu);

		// Action menu
		menu = new JMenu("Action");
		menu.setMnemonic(KeyEvent.VK_A);
		if (Start.USE_PYMOL) {
			sendM = makeMenuItem(LABEL_SHOW_CONTACTS_3D, icon_show_sel_cont_3d, menu);
			if(Start.USE_EXPERIMENTAL_FEATURES) {
				triangleM = makeMenuItem(LABEL_SHOW_TRIANGLES_3D, icon_show_triangles_3d, menu);
			}
			menu.addSeparator();
		}		
		delEdgesM = makeMenuItem(LABEL_DELETE_CONTACTS, icon_del_contacts, menu);
		addToJMenuBar(menu);

		// Comparison Menu
		menu = new JMenu("Compare");
		menu.setMnemonic(KeyEvent.VK_P);
		// Load
		submenu = new JMenu(LABEL_COMPARE_CM);
		menu.add(submenu);
		smCompare.put("Load", submenu);
		if(Start.USE_DATABASE && Start.USE_EXPERIMENTAL_FEATURES) {
			mmLoadGraph2 = makeMenuItem(LABEL_GRAPH_DB,null,submenu);
			mmLoadPdbase2 = makeMenuItem(LABEL_PDBASE,null,submenu);
			mmLoadMsd2 = makeMenuItem(LABEL_MSD,null, submenu);
		}		
		mmLoadFtp2 = makeMenuItem(LABEL_ONLINE_PDB, null, submenu);
		mmLoadPdb2 = makeMenuItem(LABEL_PDB_FILE, null, submenu);
		mmLoadCm2 = makeMenuItem(LABEL_CONTACT_MAP_FILE, null, submenu);
		mmLoadCaspRR2 = makeMenuItem(LABEL_CASP_RR_FILE, null, submenu);
		menu.addSeparator();
		mmShowCommon = makeMenuItem(LABEL_SHOW_COMMON, icon_selected, menu);
		mmShowFirst = makeMenuItem(LABEL_SHOW_FIRST, icon_selected, menu);
		mmShowSecond = makeMenuItem(LABEL_SHOW_SECOND, icon_selected, menu);		
		menu.addSeparator();
		mmToggleDiffDistMap = makeMenuItem("Show Difference Map", icon_deselected, menu);
		menu.addSeparator();
		mmSuperposition = makeMenuItem("Superimpose From Selection",null,menu);
		mmSuperposition.setEnabled(false);
		mmShowAlignedResidues = makeMenuItem("Show Corresponding Residues From Selection",null,menu);
		mmShowAlignedResidues.setEnabled(false);
		addToJMenuBar(menu);

		// Help menu
		menu = new JMenu("Help");
		menu.setMnemonic(KeyEvent.VK_H);	
		mmHelpHelp = makeHelpMenuItem("Help", null, menu);
		
		mmHelpWriteConfig = makeMenuItem("Write Example Configuration File", null, null);		// function disabled
		mmHelpAbout = makeMenuItem("About", null, menu);
		addToJMenuBar(menu);

		// Status bar
//		if (mod!= null){
//		int[] statusRuler = new int[3];
//		statusRuler = cmPane.getRulerCoordinates();
//		String r1 = "" + statusRuler[0] + "";
//		String r2 = "" + statusRuler[1] + "";
//		String r3 = "" + statusRuler[2] + "";


//		statusBar.setText(r1 +" , " + r2 + " , " +r3);
//		statusPane.setLayout(new BorderLayout());
//		statusPane.add(statusBar, BorderLayout.CENTER);
//		statusPane.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
//		}

		// Creating contact map pane if model loaded
		if(mod != null) {
			
			String[] tags = {mod.getLoadedGraphID()};
			String[] seqs = {mod.getSequence()};
			Alignment al = null;
			try {
				al = new Alignment(tags,seqs);
			} catch(AlignmentConstructionError e) {
				//should be safe to ignore the error because it shouldn't happen, if it does we print anyway an error
				System.err.println("Unexpected error, something wrong in alignment construction: "+e.getMessage());
			}
			cmPane = new ContactMapPane(mod, al , this);
			cmp.add(cmPane, BorderLayout.CENTER);
			topRuler = new ResidueRuler(cmPane,mod,this,ResidueRuler.TOP);
			leftRuler = new ResidueRuler(cmPane,mod,this,ResidueRuler.LEFT);
			topRul.add(topRuler);
			leftRul.add(leftRuler);
		}

		// Add everything to the content pane		
		this.tbPane.add(toolBar, BorderLayout.NORTH);			// tbPane is necessary if toolBar is floatable
		this.tbPane.add(cmp,BorderLayout.CENTER);				// otherwise can add these to contentPane directly
		this.getContentPane().add(tbPane, BorderLayout.CENTER); // and get rid of this line
		if(guiState.getShowRulers()) {
			cmp.add(topRul, BorderLayout.NORTH);
			cmp.add(leftRul, BorderLayout.WEST);
		}
		//this.getContentPane().add(statusPane,BorderLayout.SOUTH);

		// menu item types to be disregarded
		disregardedTypes = new HashSet<Class<?>>();
		disregardedTypes.add(JPopupMenu.Separator.class);
		disregardedTypes.add(JToolBar.Separator.class);

		// toggle the visibility of menu-items 
		setAccessibility(initMenuBarAccessibility(mod!=null),true,getJMenuBar(),disregardedTypes);
		setAccessibility(initButtonAccessibility(mod!=null),true,getJMenuBar(),disregardedTypes);

		// Show GUI
		pack();
		setVisible(true);
	}

	/**
	 * Adds a menu to the menubar of this View object. Moreover, some mappings are done:
	 * <ul>
	 * <li>mapping from menu identifier (string value obtained by <code>menu.getText()</code>)</li>
	 * <li>mapping from the menu's JPopupMenu to the menu</li>
	 * </ul> 
	 * @param menu  the menu to be added
	 */
	public void addToJMenuBar(JMenu menu) {
		getJMenuBar().add(menu);
		menu2idx.put(menu.getText(), getJMenuBar().getMenuCount()-1);
		popupMenu2Parent.put(menu.getPopupMenu(),menu);
	}

	/**
	 * Toggles the visibility status of all components in the map. 
	 * @param components  items to be considered
	 * @param parentCheck  enable this to consider the visibility of the parent component which means whenever all child-components of the parent component are invisible make the parent component invisible, too.
	 * @param topLevelComponent  pointer to the top level component where changes to its visibility is prohibited
	 * @param disregardedTypes  collection of component types not to be considered
	 */
	public void setAccessibility(Map<Component,Boolean> components, boolean parentCheck, Component topLevelComponent, Collection<Class<?>> disregardedTypes) {
		for( Component c : components.keySet() ) {
			if( c != null ) {
				setAccessibility(c,components.get(c),parentCheck,topLevelComponent,disregardedTypes);
			}
		}
	}

	/**
	 * Toggles the visibility status of the given component. As its visibility 
	 * might have an effect on its parent component we also have to consider 
	 * the parent component. Nevertheless, the desired treatment of the parent 
	 * component can be set as well. The recursive ascension in the component 
	 * tree stops if the given component is the top level component.
	 * @param comp  component whose visibility shall be changed
	 * @param visible  the new visibility status of <code>comp</code>. Enable 
	 *  this to make it visible
	 * @param parentCheck  toggles the parent check. If this is enabled the 
	 *  parents visibility mode will be adapted with respect to the visibility 
	 *  mode of <code>comp</code>
	 * @param topLevelComponent  the top level component. Stop if 
	 *  <code>comp</code> equals this component.key
	 * @param disregardedTypes  collection of component types not to be 
	 *  considered
	 * @see #setAccessibility(Map, boolean, Component, Collection)
	 */
	public void setAccessibility(Component comp, boolean visible, boolean parentCheck, Component topLevelComponent, Collection<Class<?>> disregardedTypes) {

//		try {
//		System.out.println("set:"+((AbstractButton) comp).getText());
//		} catch(Exception e) {
//		System.out.println("not an abstract button:"+comp.getClass());
//		System.out.println("comp==popup:"+(comp==this.popup));
//		}

		if( comp == topLevelComponent ) {
			return;
		}

		Component parent = comp.getParent();

		if(parent != topLevelComponent) {
			// disable and hide element
			comp.setEnabled(visible);
			//comp.setVisible(visible);
		}

		if( parentCheck ) {

			JMenu menu = popupMenu2Parent.get(parent);
			if( parent == null || menu == null ) {
				return;
			}	    

			if( visible == false && parent.isEnabled() ) {

				// shall hold pointers to the siblings of 'comp', that is the 
				// children of 'parent'
				Component[] siblings;

				// get child-list
				if( parent.getClass() == JPopupMenu.class || parent.getClass() == Container.class ) {
					siblings = ((Container) parent).getComponents();
				} else {
					System.err.println("Cannot handle component type: " + parent.getClass());
					return;
				}

				boolean allDisabled = true;

				// check all sibling components of 'comp'
				for( Component c: siblings ) {
					if( !disregardedTypes.contains(c.getClass()) ) {
						if( c.isEnabled() ) {
							allDisabled = false;
							break;
						}
					}
				}

				if( allDisabled ) {
					if( parent.getClass() == JPopupMenu.class ) { 
						setAccessibility(popupMenu2Parent.get(parent),false,true,topLevelComponent,disregardedTypes);
					} else {
						setAccessibility(parent,false,true,topLevelComponent,disregardedTypes);		    
					}
				}		

			} else if ( visible == true  ) {
				setAccessibility(parent,true,true,topLevelComponent,disregardedTypes);
			}
		}
	}

	/**
	 * Gets all menu items (for the menu-bar as well as for the popup-menu), 
	 * that is {@link JMenuItem} and {@link JMenu} objects. 
	 * @return a map containg all menu item of this View instance 
	 */
	@SuppressWarnings("unused")
	private Map<Component,Boolean> getMenuItemToggleMap() {
		HashMap<Component,Boolean> h = new HashMap<Component, Boolean>();
		Class<?> componentClass;

		try {
			for(Field f : this.getClass().getDeclaredFields() ) {
				componentClass = f.getDeclaringClass();
				if( componentClass == JMenuItem.class ) {
					h.put((JMenuItem) f.get(this),true);
				} else if (componentClass == JMenu.class) {
					h.put((JMenuItem) f.get(this),true);		
				}
			}
		} catch (IllegalAccessException e) {
			System.err.println("Error: " + e.getMessage());
		}	// menu -> View
		return h;
	}

	/**
	 * Gets a map containing accessibility rules for the initialization of the 
	 * menu-bar. Use this map as an input to function 
	 * {@link #setAccessibility(Map, boolean, Component, Collection)}.
	 * @param hasMod  set this to true if the View holds a first model
	 * @return a map containing accessibility rules for menu bar at startup 
	 */
	private Map<Component,Boolean> initMenuBarAccessibility(boolean hasMod) {
		HashMap<Component,Boolean> map = new HashMap<Component, Boolean>();

		// menu -> File
		map.put(smFile.get("Save"), hasMod);
		map.put(mmSaveAli,false);
		map.put(mmPrint,hasMod);	
		map.put(mmInfo,hasMod);
		// menu -> View
		map.put(mmViewShowPdbResSers, hasMod);
		map.put(mmViewRulers, hasMod);
		map.put(mmViewHighlightComNbh, hasMod);
		map.put(mmViewShowDensity, hasMod);
		map.put(mmViewShowDistMatrix, hasMod);
		// menu -> Select
		map.put(mmSelectAll, hasMod);
		map.put(mmSelectByResNum, hasMod);
		map.put(mmSelectHelixHelix, hasMod);
		map.put(mmSelectBetaBeta, hasMod);
		map.put(mmSelectInterSsContacts, hasMod);
		map.put(mmSelectIntraSsContacts, hasMod);
		// menu -> Color
		map.put(mmColorChoose, hasMod);
		map.put(mmColorPaint, hasMod);
		map.put(mmColorReset, hasMod);
		// menu -> Action
		map.put(squareM, hasMod);
		map.put(fillM, hasMod);
		map.put(rangeM, hasMod);
		map.put(nodeNbhSelM, hasMod); 
		map.put(comNeiM, hasMod);
		map.put(mmSelModeColor, hasMod); 
		map.put(sendM, hasMod);
		map.put(triangleM, hasMod); 
		map.put(delEdgesM, hasMod);
		// menu -> Compare
		map.put(smCompare.get("Load"), hasMod);
		map.put(mmShowCommon,false);
		map.put(mmShowFirst,false);
		map.put(mmShowSecond,false);
		map.put(mmToggleDiffDistMap,false);
		//map.put(this.getJMenuBar().getMenu(menu2idx.get("Compare")), hasMod);

		return map;
	}
	
	/**
	 * Gets a map containing accessibility rules for the initialization of the 
	 * tool-bar. Use this map as an input to function    
	 * {@link #setAccessibility(Map, boolean, Component, Collection)}.
	 * @param hasMod  set this to true if the View holds a first model 
	 * @return a map containing accessibility rules for tool bar at startup 
	 */    
	private Map<Component,Boolean> initButtonAccessibility(boolean hasMod) {
		HashMap<Component,Boolean> map = new HashMap<Component, Boolean>();

		map.put(tbShowSel3D, hasMod);
		map.put(tbFileInfo, hasMod);
		map.put(tbDelete, hasMod);
		map.put(tbShowComNbh, hasMod);
		map.put(tbShowComNbh3D, hasMod);
		
		return map;
	}

	/**
	 * Gets the accessibility rules for the popup-menu item  which can be 
	 * applied with {@link #setAccessibility(Map, boolean, Component, Collection)} 
	 * if a second model has been loaded.   
	 * @return a map containing accessibility rules for the popup-menu items in 
	 * the compare mode
	 */
	private Map<Component,Boolean> compareModePopupMenuAccessibility() {
		HashMap<Component, Boolean> map = new HashMap<Component, Boolean>();

		map.put(nodeNbhSelP, false);
		map.put(comNeiP, false);
		map.put(pmSelModeColor, false);
		map.put(triangleP, false);
		map.put(popupSendEdge, false);
		map.put(delEdgesP, false);

		return map;
	}

	/**
	 * Gets the accessibility rules for the menu bar item  which can be 
	 * applied with {@link #setAccessibility(Map, boolean, Component, Collection)} 
	 * if a second model has been loaded.   
	 * @return a map containing accessibility rules for the menu bar item in 
	 * the compare mode
	 */
	private Map<Component,Boolean> compareModeMenuBarAccessibility() {
		HashMap<Component,Boolean> map = new HashMap<Component, Boolean>();

		// menu -> File
		map.put(mmInfo,true);
		map.put(mmSaveCmFile,false);
		map.put(mmSaveCaspRRFile, false);
		map.put(mmSaveGraphDb, false);
		map.put(mmSaveAli, true);
		// menu -> View
		map.put(mmViewShowPdbResSers,true);
		map.put(mmViewRulers,false);
		map.put(mmViewHighlightComNbh,false);
		map.put(mmViewShowDensity,false);
		map.put(mmViewShowDistMatrix,false);
		// menu -> Select
		map.put(mmSelectByResNum,false);
		map.put(mmSelectHelixHelix,false);
		map.put(mmSelectBetaBeta,false);
		map.put(mmSelectInterSsContacts,false);
		map.put(mmSelectIntraSsContacts,false);
		// menu -> Color
		map.put(mmColorChoose,false);
		map.put(mmColorPaint,false);
		map.put(mmColorReset,false);
		// menu -> Action
		map.put(nodeNbhSelM,false);
		map.put(comNeiM,false);
		map.put(mmSelModeColor,false);
		map.put(triangleM,false);
		map.put(delEdgesM, false);
		// menu -> Compare
		map.put(mmShowCommon,true);
		map.put(mmShowFirst,true);
		map.put(mmShowSecond,true);
		map.put(mmToggleDiffDistMap,true);
		map.put(smCompare.get("Load"),true); // now allowing loading of a new second contact map

		return map;
	}

	/**
	 * Gets the accessibility rules for the buttons of the toolbar which can be 
	 * applied with {@link #setAccessibility(Map, boolean, Component, Collection)} 
	 * if a second model has been loaded.   
	 * @return a map containing accessibility rules for the buttons of the 
	 * toolbar in the compare mode
	 */    
	private Map<Component,Boolean> compareModeButtonAccessibility() {
		HashMap<Component,Boolean> map = new HashMap<Component, Boolean>();

		map.put(tbNbhSel, false);
		map.put(tbShowComNbh, false);
		map.put(tbSelModeColor, false);
		map.put(tbShowComNbh3D, false);
		map.put(tbDelete, false);

		map.put(tbShowCommon, true);
		map.put(tbShowFirst, true);
		map.put(tbShowSecond, true);

		// the show common/first/second buttons are a special case: 
		// they've ben inatialised invisible, we need also to make them visible
		tbShowCommon.setVisible(true);
		tbShowFirst.setVisible(true);
		tbShowSecond.setVisible(true);
		
		return map;
	}

	/*------------------------------ event handling -------------------------*/

	/**
	 * Handling action events for all menu items.
	 */
	public void actionPerformed (ActionEvent e) {

		/* ---------- File Menu ---------- */

		// Load
		if(e.getSource() == mmLoadGraph) {
			handleLoadFromGraphDb(FIRST_MODEL);
		}
		if(e.getSource() == mmLoadPdbase) {
			handleLoadFromPdbase(FIRST_MODEL);

		}		  
		if(e.getSource() == mmLoadMsd) {
			handleLoadFromMsd(FIRST_MODEL);
		}			  
		if(e.getSource() == mmLoadPdb) {
			handleLoadFromPdbFile(FIRST_MODEL);
		}
		if(e.getSource() == mmLoadFtp) {
			handleLoadFromFtp(FIRST_MODEL);
		}
		if(e.getSource() == mmLoadCm) {
			handleLoadFromCmFile(FIRST_MODEL);
		}
		if(e.getSource() == mmLoadCaspRR) {
			handleLoadFromCaspRRFile(FIRST_MODEL);
		}

		// Save
		if(e.getSource() == mmSaveGraphDb) {
			handleSaveToGraphDb();
		}		  
		if(e.getSource() == mmSaveCmFile) {
			handleSaveToCmFile();
		}
		if(e.getSource() == mmSaveCaspRRFile) {
			handleSaveToCaspRRFile();
		}
		if(e.getSource() == mmSavePng) {
			handleSaveToPng();
		}
		if(e.getSource() == mmSaveAli) {
			handleSaveAlignment();
		}

		// Info, Print, Quit
		if(e.getSource() == mmInfo || e.getSource() == tbFileInfo) {
			handleInfo();
		}		  		  
		if(e.getSource() == mmPrint) {
			handlePrint();
		}		  
		if(e.getSource() == mmQuit) {
			handleQuit();
		}

		/* ---------- View Menu ---------- */		

		if(e.getSource() == mmViewShowPdbResSers) {
			handleShowPdbResSers();
		}		  
		if(e.getSource() == mmViewRulers) {
			handleShowRulers();
		}	
		if(e.getSource() == mmViewIconBar) {
			handleShowIconBar();
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

		if(e.getSource() == mmSelectAll  ) {
			handleSelectAll();
		}
		if(e.getSource() == mmSelectByResNum  ) {
			handleSelectByResNum();
		}
		if(e.getSource() == mmSelectHelixHelix  ) {
			handleSelectHelixHelix();
		}
		if(e.getSource() == mmSelectBetaBeta  ) {
			handleSelectBetaBeta();
		}
		if(e.getSource() == mmSelectInterSsContacts  ) {
			handleSelectInterSsContacts();
		}
		if(e.getSource() == mmSelectIntraSsContacts  ) {
			handleSelectIntraSsContacts();
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

		// Selection modes

		// square button clicked
		if (e.getSource() == squareM || e.getSource() == squareP || e.getSource() == tbSquareSel ) {
			guiState.setSelectionMode(GUIState.SelMode.RECT);
		}
		// fill button clicked
		if (e.getSource() == fillM || e.getSource() == fillP || e.getSource() == tbFillSel) {
			guiState.setSelectionMode(GUIState.SelMode.FILL);
		}			
		// diagonal selection clicked
		if (e.getSource() == rangeM || e.getSource() == rangeP || e.getSource() == tbDiagSel) {
			guiState.setSelectionMode(GUIState.SelMode.DIAG);
		}
		// node neihbourhood selection button clicked 
		if (e.getSource() == nodeNbhSelM || e.getSource() == nodeNbhSelP || e.getSource() == tbNbhSel) {
			guiState.setSelectionMode(GUIState.SelMode.NBH);
		}		
		// showing com. Nei. button clicked
		if (e.getSource() == comNeiM || e.getSource() == comNeiP || e.getSource() == tbShowComNbh) {
			guiState.setSelectionMode(GUIState.SelMode.COMNBH);
		}
		// color selection mode button clicked
		if (e.getSource() == mmSelModeColor || e.getSource() == pmSelModeColor || e.getSource() == tbSelModeColor) {
			guiState.setSelectionMode(GUIState.SelMode.COLOR);
		}		

		// Actions

		// send selection button clicked
		if (e.getSource() == sendM || e.getSource() == sendP || e.getSource() == tbShowSel3D) {	
			handleShowSelContacts3D();
		}
		// send com.Nei. button clicked
		if(e.getSource()== triangleM || e.getSource()== triangleP || e.getSource() == tbShowComNbh3D) {
			handleShowTriangles3D();
		}		
		// delete selected edges button clicked
		if (e.getSource() == delEdgesM || e.getSource() == delEdgesP || e.getSource() == tbDelete) {
			handleDeleteSelContacts();
		}
		// send current edge (only available in popup menu)
		if(e.getSource() == popupSendEdge) {
			handleShowDistance3D();
		}

		/* ---------- Comparison Menu ---------- */


		/** for the contact map comparison load menu */


		if(e.getSource() == mmLoadGraph2) {
			handleLoadFromGraphDb(SECOND_MODEL);
		}
		if(e.getSource() == mmLoadPdbase2) {
			handleLoadFromPdbase(SECOND_MODEL);
		}		  
		if(e.getSource() == mmLoadMsd2) {
			handleLoadFromMsd(SECOND_MODEL);
		}			  
		if(e.getSource() == mmLoadPdb2) {
			handleLoadFromPdbFile(SECOND_MODEL);
		}
		if(e.getSource() == mmLoadFtp2) {
			handleLoadFromFtp(SECOND_MODEL);
		}
		if(e.getSource() == mmLoadCm2) {
			handleLoadFromCmFile(SECOND_MODEL);
		}
		if(e.getSource() == mmLoadCaspRR2) {
			handleLoadFromCaspRRFile(SECOND_MODEL);
		}

		if(e.getSource() == mmShowCommon || e.getSource() == tbShowCommon) {
			handleShowCommon();
		}

		if(e.getSource() == mmShowFirst || e.getSource() == tbShowFirst) {
			handleShowFirst();
		}

		if(e.getSource() == mmShowSecond || e.getSource() == tbShowSecond) {
			handleShowSecond();
		}

		if(e.getSource() == mmToggleDiffDistMap) {
			handleToggleDiffDistMap();
		}

		if( e.getSource() == mmSuperposition ) {
			handleSuperposition3D(); 
		}

		if( e.getSource() == mmShowAlignedResidues ) {
			handleShowAlignedResidues3D();
		}

		/* ---------- Help Menu ---------- */

		if(e.getSource() == mmHelpAbout) {
			handleHelpAbout();
		}
		if(e.getSource() == mmHelpHelp) {
			// help is now being handled by a different action listener defined in makeHelpMenuItem
		}
		if(e.getSource() == mmHelpWriteConfig) {
			handleHelpWriteConfig();
		}

		/* ---------------- Invisible Notifiers --------------- */
		if( e.getSource() == sadpNotifier && sadpNotifier.notification() == SADPDialog.DONE ) {
			handlePairwiseAlignmentResults(sadpNotifier);
		}

	}


	/* -------------------- File Menu -------------------- */

	private void handleLoadFromGraphDb(boolean secondModel) {
		if(!Start.isDatabaseConnectionAvailable()) {
			showNoDatabaseConnectionWarning();
		} else {
			if (secondModel == SECOND_MODEL && mod == null){
				this.showNoContactMapWarning();
			} else{
				LoadDialog dialog;
				try {
					dialog = new LoadDialog(this, "Load from graph database", new LoadAction(secondModel) {
						public void doit(Object o, String f, String ac, int modelSerial, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
							View view = (View) o;
							view.doLoadFromGraphDb(db, gid, secondModel);
						}
					}, null, null, null, null, null, null, null, null, Start.DEFAULT_GRAPH_DB, "");
					
					actLoadDialog = dialog;
					dialog.showIt();
					
				} catch (LoadDialogConstructionError e) {
					System.err.println("Failed to load the load-dialog.");
				}
			}
		}
	}

	public void doLoadFromGraphDb(String db, int gid, boolean secondModel) {
		System.out.println("Loading from graph database");
		System.out.println("Database:\t" + db);
		System.out.println("Graph Id:\t" + gid);
		try {
			Model mod = new GraphDbModel(gid, db);
			if(secondModel) {
				//handleLoadSecondModel(mod);
				mod2 = mod;
				handlePairwiseAlignment();
			} else {
				this.spawnNewViewWindow(mod);
			}
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}

	private void handleLoadFromPdbase(boolean secondModel) {
		if(!Start.isDatabaseConnectionAvailable()) {
			showNoDatabaseConnectionWarning();
		} else {
			if (secondModel == SECOND_MODEL && mod == null){
				this.showNoContactMapWarning();
			} else{
				try {
					LoadDialog dialog = new LoadDialog(this, "Load from Pdbase", new LoadAction(secondModel) {
						public void doit(Object o, String f, String ac, int modelSerial, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
							View view = (View) o;
							view.doLoadFromPdbase(ac, modelSerial, cc, ct, dist, minss, maxss, db, secondModel);
						}
					}, null, "", "1", "", Start.DEFAULT_CONTACT_TYPE, String.valueOf(Start.DEFAULT_DISTANCE_CUTOFF), "", "", Start.DEFAULT_PDB_DB, null);
					dialog.setChainCodeGetter(new Getter(dialog) {
						public Object get() throws GetterError {
							LoadDialog dialog = (LoadDialog) getObject();
							String pdbCode    = dialog.getSelectedAc();
							String db         = dialog.getSelectedDb();
							try {
								PdbaseModel mod = new PdbaseModel(pdbCode,"",0.0,1,1,db);
								return mod.getChains();
							} catch (PdbCodeNotFoundError e) {
								throw new GetterError("Failed to read chains from pdbase:" + e.getMessage());
							} catch (SQLException e) {
								throw new GetterError("Failed to read chains from pdbase:" + e.getMessage());
							} catch (PdbLoadError e) {
								throw new GetterError("Failed to load chains from pdb object: " + e.getMessage());
							}
						}
					});
					dialog.setModelsGetter(new Getter(dialog) {
						public Object get() throws GetterError {
							LoadDialog dialog = (LoadDialog) getObject();
							String pdbCode    = dialog.getSelectedAc();
							String db         = dialog.getSelectedDb();
							try {
								PdbaseModel mod = new PdbaseModel(pdbCode,"",0.0,1,1,db);
								return mod.getModels();
							} catch (PdbCodeNotFoundError e) {
								throw new GetterError("Failed to read models from pdbase:" + e.getMessage());
							} catch (SQLException e) {
								throw new GetterError("Failed to read models from pdbase:" + e.getMessage());
							} catch (PdbLoadError e) {
								throw new GetterError("Failed to load chains from pdb object: " + e.getMessage());
							}
						}				
					});
					actLoadDialog = dialog;
					dialog.showIt();
				} catch (LoadDialogConstructionError e) {
					System.err.println("Failed to load the load-dialog.");
				}
			}
		}
	}

	public void doLoadFromPdbase(String ac, int modelSerial, String cc, String ct, double dist, int minss, int maxss, String db, boolean secondModel) {
		System.out.println("Loading from Pdbase");
		System.out.println("PDB code:\t" + ac);
		System.out.println("Model serial:\t" + modelSerial);
		System.out.println("Chain code:\t" + cc);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + (minss==-1?"none":minss));
		System.out.println("Max. Seq. Sep.:\t" + (maxss==-1?"none":maxss));
		System.out.println("Database:\t" + db);	
		try{
			Model mod = new PdbaseModel(ac, ct, dist, minss, maxss, db);
			mod.load(cc, modelSerial);
			if(secondModel) {
				//handleLoadSecondModel(mod);
				mod2 = mod;
				handlePairwiseAlignment();
			} else {
				this.spawnNewViewWindow(mod);
			}
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		} catch (PdbCodeNotFoundError e) {	
			showLoadError(e.getMessage());
		} catch (SQLException e) {
			showLoadError(e.getMessage());
		}
	}

	private void handleLoadFromMsd(boolean secondModel) {
		if(!Start.isDatabaseConnectionAvailable()) {
			showNoDatabaseConnectionWarning();
		} else {
			if (secondModel == SECOND_MODEL && mod == null){
				this.showNoContactMapWarning();
			} else{
				try {
					LoadDialog dialog = new LoadDialog(this, "Load from MSD", new LoadAction(secondModel) {
						public void doit(Object o, String f, String ac, int modelSerial, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
							View view = (View) o;
							view.doLoadFromMsd(ac, modelSerial, cc, ct, dist, minss, maxss, db, secondModel);
						}
					}, null, "", "1", "", Start.DEFAULT_CONTACT_TYPE, String.valueOf(Start.DEFAULT_DISTANCE_CUTOFF), "", "", Start.DEFAULT_MSDSD_DB, null);
					dialog.setChainCodeGetter(new Getter(dialog) {
						public Object get() throws GetterError {
							try {
								LoadDialog dialog = (LoadDialog) getObject();
								String pdbCode    = dialog.getSelectedAc();
								String db         = dialog.getSelectedDb();
								MsdsdModel mod    = new MsdsdModel(pdbCode,"",0.0,1,1,db);
								return mod.getChains();
							} catch (PdbLoadError e) {
								throw new GetterError("Failed to load chains from pdb object: " + e.getMessage());
							}
						}
					});
					dialog.setModelsGetter(new Getter(dialog) {
						public Object get() throws GetterError {
							try {
								LoadDialog dialog = (LoadDialog) getObject();
								String pdbCode    = dialog.getSelectedAc();
								String db         = dialog.getSelectedDb();
								MsdsdModel mod    = new MsdsdModel(pdbCode,"",0.0,1,1,db);
								return mod.getModels();
							} catch (PdbLoadError e) {
								throw new GetterError("Failed to load chains from pdb object: " + e.getMessage());
							}
						}
					});
					actLoadDialog = dialog;
					dialog.showIt();
				} catch (LoadDialogConstructionError e) {
					System.err.println("Failed to load load-dialog.");
				}
			}
		}
	}

	public void doLoadFromMsd(String ac, int modelSerial, String cc, String ct, double dist, int minss, int maxss, String db, boolean secondModel) {
		System.out.println("Loading from MSD");
		System.out.println("PDB code:\t" + ac);
		System.out.println("Model index:\t" + modelSerial);
		System.out.println("Chain code:\t" + cc);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + (minss==-1?"none":minss));
		System.out.println("Max. Seq. Sep.:\t" + (maxss==-1?"none":maxss));
		System.out.println("Database:\t" + db);	
		try {
			Model mod = new MsdsdModel(ac, ct, dist, minss, maxss, db);
			mod.load(cc, modelSerial);
			if(secondModel) {
				//handleLoadSecondModel(mod);
				mod2 = mod;
				handlePairwiseAlignment();
			} else {
				this.spawnNewViewWindow(mod);
			}
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}	  

	private void handleLoadFromPdbFile(boolean secondModel) {

		if (secondModel == SECOND_MODEL && mod == null){
			this.showNoContactMapWarning();
		} else{
			try {
				LoadDialog dialog = new LoadDialog(this, "Load from PDB file", new LoadAction(secondModel) {
					public void doit(Object o, String f, String ac, int modelSerial, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
						View view = (View) o;
						view.doLoadFromPdbFile(f, modelSerial, cc, ct, dist, minss, maxss, secondModel);
					}
				}, "", null, "1", "", Start.DEFAULT_CONTACT_TYPE, String.valueOf(Start.DEFAULT_DISTANCE_CUTOFF), "", "", null, null);
				dialog.setChainCodeGetter(new Getter(dialog) {
					public Object get() throws GetterError {
						LoadDialog dialog = (LoadDialog) getObject();
						String pdbFilename    = dialog.getSelectedFileName();
						try {
							PdbFileModel mod = new PdbFileModel(pdbFilename,"",0.0,1,1);
							return mod.getChains();
						} catch (ModelConstructionError e) {
							throw new GetterError("Failed to read chains from ftp:"+e.getMessage());
						} catch (PdbLoadError e) {
							throw new GetterError("Failed to load chains from pdb object: " + e.getMessage());
						}
					}
				});
				dialog.setModelsGetter(new Getter(dialog) {
					public Object get() throws GetterError {
						LoadDialog dialog = (LoadDialog) getObject();
						String pdbFilename    = dialog.getSelectedFileName();
						try {
							PdbFileModel mod = new PdbFileModel(pdbFilename,"",0.0,1,1);
							return mod.getModels();
						} catch (ModelConstructionError e) {
							throw new GetterError("Failed to read models from ftp:"+e.getMessage());
						} catch (PdbLoadError e) {
							throw new GetterError("Failed to load chains from pdb object: " + e.getMessage());
						}
					}
				});
				actLoadDialog = dialog;
				dialog.showIt();
			} catch (LoadDialogConstructionError e1) {
				System.err.println("Failed to load the load-dialog.");
			}
		}
	}

	public void doLoadFromPdbFile(String f, int modelSerial, String cc, String ct, double dist, int minss, int maxss, boolean secondModel) {
		System.out.println("Loading from Pdb file");
		System.out.println("Filename:\t" + f);
		System.out.println("Model serial:\t" + modelSerial);
		System.out.println("Chain code:\t" + cc);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + (minss==-1?"none":minss));
		System.out.println("Max. Seq. Sep.:\t" + (maxss==-1?"none":maxss));
		try {
			Model mod = new PdbFileModel(f, ct, dist, minss, maxss);
			mod.load(cc, modelSerial);
			if(secondModel) {
				//handleLoadSecondModel(mod);
				mod2 = mod;
				handlePairwiseAlignment();
			} else {
				this.spawnNewViewWindow(mod);
			}
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}	

	private void handleLoadFromCmFile(boolean secondModel) {

		if (secondModel == SECOND_MODEL && mod == null){
			this.showNoContactMapWarning();
		} else{
			try {
				LoadDialog dialog = new LoadDialog(this, "Load from CM file", new LoadAction(secondModel) {
					public void doit(Object o, String f, String ac, int modelSerial, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
						View view = (View) o;
						view.doLoadFromCmFile(f, secondModel);
					}
				}, "", null, null, null, null, null, null, null, null, null);
				actLoadDialog = dialog;
				dialog.showIt();
			} catch (LoadDialogConstructionError e) {
				System.err.println("Failed to load the load-dialog.");
			}
		}
	}

	public void doLoadFromCmFile(String f, boolean secondModel) {
		System.out.println("Loading from contact map file "+f);
		try {
			Model mod = new ContactMapFileModel(f);
			if(secondModel == SECOND_MODEL) {
				//handleLoadSecondModel(mod);
				mod2 = mod;
				handlePairwiseAlignment();
			} else {
				this.spawnNewViewWindow(mod);
			}
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}

	private void handleLoadFromCaspRRFile(boolean secondModel) {

		if (secondModel == SECOND_MODEL && mod == null){
			this.showNoContactMapWarning();
		} else{
			try {
				LoadDialog dialog = new LoadDialog(this, "Load from CASP RR file", new LoadAction(secondModel) {
					public void doit(Object o, String f, String ac, int modelSerial, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
						View view = (View) o;
						view.doLoadFromCaspRRFile(f, secondModel);
					}
				}, "", null, null, null, null, null, null, null, null, null);
				actLoadDialog = dialog;
				dialog.showIt();
			} catch (LoadDialogConstructionError e) {
				System.err.println("Failed to load the load-dialog.");
			}
		}
	}

	public void doLoadFromCaspRRFile(String f, boolean secondModel) {
		System.out.println("Loading from CASP RR file "+f);
		try {
			Model mod = new CaspRRFileModel(f);
			if(secondModel == SECOND_MODEL) {
				//handleLoadSecondModel(mod);
				mod2 = mod;
				handlePairwiseAlignment();
			} else {
				this.spawnNewViewWindow(mod);
			}
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}

	private void handleLoadFromFtp(boolean secondModel) {
		if (secondModel == SECOND_MODEL && mod == null){
			this.showNoContactMapWarning();
		} else{
			try {
				LoadDialog dialog = new LoadDialog(this, "Load from online PDB", new LoadAction(secondModel) {
					public void doit(Object o, String f, String ac, int modelSerial, String cc, String ct, double dist, int minss, int maxss, String db, int gid) {
						View view = (View) o;
						view.doLoadFromFtp(ac, modelSerial, cc, ct, dist, minss, maxss, secondModel);
					}
				}, null, "", "1", "", Start.DEFAULT_CONTACT_TYPE, String.valueOf(Start.DEFAULT_DISTANCE_CUTOFF), "", "", null, null);
				dialog.setChainCodeGetter(new Getter(dialog) {
					public Object get() throws GetterError {
						LoadDialog dialog = (LoadDialog) getObject();
						String pdbCode = dialog.getSelectedAc();
						try {
							File localFile = Start.getFilename2PdbCode(pdbCode);
							PdbFtpModel mod = null;
							if( localFile != null ) {
								mod = new PdbFtpModel(localFile,"",0.0,0,0);
							} else {
								mod = new PdbFtpModel(pdbCode,"",0.0,0,0);
								Start.setFilename2PdbCode(pdbCode, mod.getCifFile());
							}
							return mod.getChains();
						} catch (IOException e) {
							throw new GetterError("Failed to load structure from ftp:" + e.getMessage());
						} catch (PdbLoadError e) {
							throw new GetterError("Failed to load chains from pdb object: " + e.getMessage());
						}
					}
				});
				dialog.setModelsGetter(new Getter(dialog) {
					public Object get() throws GetterError {
						LoadDialog dialog = (LoadDialog) getObject();
						String pdbCode = dialog.getSelectedAc();
						try {
							File localFile = Start.getFilename2PdbCode(pdbCode);
							PdbFtpModel mod = null;
							if( localFile != null ) {
								mod = new PdbFtpModel(localFile,"",0.0,0,0);
							} else {
								mod = new PdbFtpModel(pdbCode,"",0.0,0,0);
								Start.setFilename2PdbCode(pdbCode, mod.getCifFile());
							}
							return mod.getModels();
						} catch (IOException e) {
							throw new GetterError("Failed to load structure from ftp:" + e.getMessage());
						} catch (PdbLoadError e) {
							throw new GetterError("Failed to load chains from pdb object: " + e.getMessage());
						}
					}
				});
				actLoadDialog = dialog;
				dialog.showIt();
			} catch (LoadDialogConstructionError e) {
				e.printStackTrace();
			}

		}
	}

	public void doLoadFromFtp(String ac, int modelSerial, String cc, String ct, double dist, int minss, int maxss, boolean secondModel) {
		System.out.println("Loading from online PDB");
		System.out.println("PDB code:\t" + ac);
		System.out.println("Model serial:\t" + modelSerial);
		System.out.println("Chain code:\t" + cc);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + (minss==-1?"none":minss));
		System.out.println("Max. Seq. Sep.:\t" + (maxss==-1?"none":maxss));	
		try{
			File localFile = Start.getFilename2PdbCode(ac);
			Model mod = null;
			if( localFile != null ) {
				mod = new PdbFtpModel(localFile, ct, dist, minss, maxss);
			} else { 
				mod = new PdbFtpModel(ac, ct, dist, minss, maxss);
			}
			mod.load(cc, modelSerial);
			if(secondModel) {
				//handleLoadSecondModel(mod);
				mod2 = mod;
				handlePairwiseAlignment();
			} else {
				this.spawnNewViewWindow(mod);
			}
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		} catch (IOException e) {
			showLoadError(e.getMessage());
		}
	}

	/**
	 * Handles the computation of the pairwise contact map alignment of the
	 * two passed models in a new thread.
	 */
	private void handlePairwiseAlignment() {
		String error = null;

		actLoadDialog.dispose();	
		//Object[] possibilities = {"compute internal structure alignment","load alignment from file","apply greedy residue mapping","compute Needleman-Wunsch sequence alignment"};
		Object[] possibilities = {"compute Needleman-Wunsch sequence alignment", "compute SADP structural alignment","load alignment from FASTA file"};		
		String source = (String) JOptionPane.showInputDialog(this, "Chose alignment source ...", "Pairwise Protein Alignment", JOptionPane.PLAIN_MESSAGE, null, possibilities, possibilities[0]);

		if( source != null ) {
			try {
				if( source == possibilities[0] ) {
					// do a greedy residue-residue alignment
					doPairwiseSequenceAlignment();
				} else if( source == possibilities[1] ) {
					// compute contact map alignment using SADP
					doPairwiseSadpAlignment();
				} else if( source == possibilities[2] ) {
					// load a user provided alignment from an external source
					doLoadPairwiseAlignment();
//					} else if( source == possibilities[3] ) {
//					// do a greedy residue-residue alignment
//					doGreedyPairwiseAlignment();	
				} else {
					System.err.println("Error: Detected unhandled input option for the alignment retrieval!");
					return;
				}
			} catch (AlignmentConstructionError e) {
				error = e.getMessage();
			} catch (FileNotFoundException e) {
				error = e.getMessage();
			} catch (PirFileFormatError e) {
				error = e.getMessage();
			} catch (FastaFileFormatError e) {
				error = e.getMessage();
			} catch (IOException e) {
				error = e.getMessage();
			}
			finally{
				if(error != null) {
					// reset all fields connected to the compare mode and clean up the scene
					mod2 = null; ali = null;
					JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);		    	

				}
			}

		}
	}

	/**
	 * Enables and disables some GUI features for the compare mode.
	 */
	private void setGUIStatusCompareMode() {
		// enable computation of the superposition and the showing of 
		// corresponding residues for both structures
		mmSuperposition.setEnabled(true);	
		mmShowAlignedResidues.setEnabled(true);

		// disable/enable some menu-bar items, popup-menu items and buttons
		setAccessibility(compareModeMenuBarAccessibility(),   true, getJMenuBar(), disregardedTypes);
		setAccessibility(compareModePopupMenuAccessibility(), true, null,          disregardedTypes);
		setAccessibility(compareModeButtonAccessibility(),    true, null,          disregardedTypes);

		// disable rulers
		if(guiState.getShowRulers()) {
			handleShowRulers();
		}

	}

	/**
	 * Loads the pairwise alignment for the given model from a external source.
	 */
	public void doLoadPairwiseAlignment()
	throws IOException, PirFileFormatError, FastaFileFormatError, 
	AlignmentConstructionError {

		// open global file-chooser and get the name the alignment file
		JFileChooser fileChooser = Start.getFileChooser();
		int ret = fileChooser.showOpenDialog(this);
		File source = null;
		if(ret == JFileChooser.APPROVE_OPTION) {
			source = fileChooser.getSelectedFile();
		} else {
			return;
		}

		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
		
		// load alignment
		ali = new Alignment(source.getPath(),"FASTA");

		// prepare expected sequence identifiers of 'mod1' and 'mod2' in 'ali'
		String name1 = mod.getLoadedGraphID();
		String name2 = mod2.getLoadedGraphID();
		// we reset tags in alignment objects to ours. File provided must have the 2 correct sequences in the right order!
		String[] names = {name1, name2};
		ali.resetTags(names);

		// if file provided doesn't have the right sequences we throw exception
		if (!mod.getSequence().equals(ali.getSequenceNoGaps(name1))) {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			System.err.println("First sequence in given alignment file and sequence of contact map "+name1+" differ");
			try {
				// we try to align the 2 missmatching sequences (from the file and the loaded contact map), 
				// so that at least the user gets hopefully a better feeling of what's wrong
				PairwiseSequenceAlignment psa = new PairwiseSequenceAlignment(ali.getSequenceNoGaps(name1),mod.getSequence(),"from file","from CMView");
				System.err.println("This is an alignment of the non-matching sequences: ");
				psa.printAlignment();
			} catch (PairwiseSequenceAlignmentException e) {
				// pairwise alignment didn't succeed, we do nothing 
			}
			throw new AlignmentConstructionError("First sequence from given alignment and sequence of first loaded contact map differ!");
		}
		if (!mod2.getSequence().equals(ali.getSequenceNoGaps(name2))) {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			System.err.println("Second sequence in given alignment file and sequence of contact map "+name2+" differ");
			try {
				// we try to align the 2 missmatching sequences (from the file and the loaded contact map), 
				// so that at least the user gets hopefully a better feeling of what's wrong
				PairwiseSequenceAlignment psa = new PairwiseSequenceAlignment(ali.getSequenceNoGaps(name2),mod2.getSequence(),"from file","from CMView");
				System.err.println("This is an alignment of the non-matching sequences: ");
				psa.printAlignment();
			} catch (PairwiseSequenceAlignmentException e) {
				// pairwise alignment didn't succeed, we do nothing 
			}
			throw new AlignmentConstructionError("Second sequence from given alignment and sequence of second loaded contact map differ!");
		}

		// load stuff onto the contact map pane and the 3D visualizer
		doLoadSecondModel(mod2, ali);
		
		// adapt GUI behavior
		setGUIStatusCompareMode();
		
		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
	}

	/**
	 * Computes the pairwise contact map alignment of the two passed models in 
	 * a new thread.
	 */
	public void doPairwiseSadpAlignment() {
		SADPResult result   = new SADPResult();
		SADPRunner runner   = new SADPRunner(mod,mod2,result);
		// version 1.0 used to be as simple as possible -> no preferences 
		// settings available
		SADPDialog sadpDiag = new SADPDialog(
				this,
				"Pairwise Protein Alignment",
				runner,
				result,
				SADPDialog.CONSTRUCT_WITHOUT_START_AND_PREFERENCES);
		// TODO: to enable the preferences dialog implement the parameter 
		//       retrieval through the preference settings and set construction 
		//       flag to SADPDialog.CONSTRUCT_EVERYTHING
		sadpNotifier = sadpDiag.getNotifier();

		if( sadpDiag.getConstructionStatus() == SADPDialog.CONSTRUCT_WITHOUT_START_AND_PREFERENCES ) {
			sadpDiag.getStartButton().doClick();
		}

		sadpDiag.createGUI();
	}

	/**
	 * Constructs a pairwise sequence alignment using the Needleman-Wunsch algorithm with default parameters.
	 * @throws AlignmentConstructionError
	 */
	public void doPairwiseSequenceAlignment() throws AlignmentConstructionError {
		
		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
		
		String seq1 = mod.getSequence();
		String seq2 = mod2.getSequence();

		if(seq1 == null || seq2 == null || seq1.length() == 0 || seq2.length() == 0) {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			throw new AlignmentConstructionError("No sequence found.");
		}

		String name1 = mod.getLoadedGraphID();
		String name2 = mod2.getLoadedGraphID();

		PairwiseSequenceAlignment jalign = null;
		try {
			jalign = new PairwiseSequenceAlignment(seq1, seq2, name1, name2);
		} catch (PairwiseSequenceAlignmentException e) {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			throw new AlignmentConstructionError("Error during alignment: " + e.getMessage());
		}
		jalign.printAlignment();
		String[] alignedSeqs = jalign.getAlignedSequences();
		String alignedSeq1 = alignedSeqs[0];
		String alignedSeq2 = alignedSeqs[1];

		// create alignement
		String[] names = {name1, name2};
		String[] seqs = {alignedSeq1, alignedSeq2};
		ali = new Alignment(names, seqs);
		//ali.printSimple();


		// load stuff onto the contact map pane and the 3D visualizer
		doLoadSecondModel(mod2, ali);

		// adapt GUI behavior
		setGUIStatusCompareMode();	
		
		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
	}

	/**
	 * Construct a pairwise alignment of the given models in a rather greedy 
	 * manner: The residues of both models are mapped index-wise, i.e. residue 
	 * 1 in the first model is mapped to residue 1 in the second and so on. The 
	 * shorter sequence is being extended with gap characters to accomplish the 
	 * length of the longer sequence. If either (or both) sequence(s) do(es) 
	 * not provide sequence information the sequence length is being estimated 
	 * by size of the underlying graph structure (i.e. the maximum node index). 
	 * In that case, the sequences do only consist of X's.  
	 * @throws AlignmentConstructionError
	 */
	public void doGreedyPairwiseAlignment() 
	throws AlignmentConstructionError, DifferentContactMapSizeError {

		String alignedSeq1 = mod.getSequence();
		String alignedSeq2 = mod2.getSequence();
		int len1,len2,cap;
		StringBuffer s = null;
		char gap = Alignment.getGapCharacter();

		if( alignedSeq1 == null || alignedSeq1.length() == 0 ) {		// changed by HS, hope this is safer

			len1 = mod.getGraph().getFullLength();

			if( alignedSeq2 == null || alignedSeq2.length() == 0) {
				// alignedSeq1 -> NOT present, alignedSeq2 -> NOT present

				len2 = mod2.getGraph().getFullLength();
				cap  = Math.max(len1,len2);
				s    = new StringBuffer(cap);


				// fill dummy string with 'X' characters denoting unobserved 
				// residues in the size of the longer sequence 
				for(int i = 0; i < cap; ++i) {
					s.append('X');
				}

				if( len1 < len2 ) {
					alignedSeq2 = new String(s);

					// replace the different position in the aligned sequences 
					// with gap characters in the shorter one
					for(int i = len1; i < len2; ++i) {
						s.setCharAt(i, gap);
					}

					alignedSeq1 = new String(s);

				} else if( len1 > len2 ){
					alignedSeq1 = new String(s);

					// replace the different position in the aligned sequences 
					// with gap characters in the shorter one
					for(int i = len1; i < len2; ++i) {
						s.setCharAt(i, gap);
					}

					alignedSeq2 = new String(s);
				}
			} else {
				// alignedSeq1 -> NOT present, alignedSeq2 -> present

				len2 = alignedSeq2.length();
				s    = new StringBuffer(Math.max(len1, len2));

				for(int i = 0; i < len1; ++i) {
					s.append('X');
				}

				if( len1 < len2 ) {
					// appends gaps to alignedSeq1
					for(int i = len1; i < len2; ++i) {
						s.append(gap);
					}

					alignedSeq1 = new String(s);

				} else if( len1 > len2 ) {
					alignedSeq1 = new String(s);

					// appends gaps to alignedSeq2
					for(int i = 0; i < len1-len2; ++i) {
						s.setCharAt(i, gap);
					}		    

					alignedSeq2 += s;
				}
			} 
		} else {

			len1 = alignedSeq1.length();

			if( alignedSeq2 == null || alignedSeq2 == "" ) {
				// alignedSeq1 -> present, alignedSeq2 -> NOT present

				len2 = mod2.getGraph().getFullLength();
				s    = new StringBuffer(Math.max(len1, len2));

				for(int i = 0; i < len2; ++i) {
					s.append('X');
				}

				if( len1 < len2 ) {
					alignedSeq2 = new String(s);

					// append gaps to alignedSeq1
					for(int i = 0; i < len2-len1; ++i) {
						s.setCharAt(i, gap);
					}

					alignedSeq1 += s.substring(0,len2-len1);

				} else if( len1 > len2 ) {
					// append gaps to alignedSeq2
					for(int i = len2; i < len1-len2; ++i) {
						s.append(gap);
					}

					alignedSeq2 = new String(s);
				}
			} else {
				// alignedSeq1 -> present, alignedSeq2 -> present

				// append gaps to the shorter of alignedSeq1 and alignedSeq2
				len2 = alignedSeq2.length();

				if( len1 != len2 ) {
					cap = Math.abs(len1-len2);
					s   = new StringBuffer(cap);

					for(int i = 0; i < cap; ++i) {
						s.append(gap);
					}
				}

				if( len1 < len2 ) {
					alignedSeq1 += s;
				} else if( len1 > len2 ) {
					alignedSeq2 += s;
				}
			}
		}

		// create alignment
		String name1 = mod.getLoadedGraphID();
		String name2 = mod2.getLoadedGraphID();
		String[] names = {name1, name2};
		String[] seqs = {alignedSeq1, alignedSeq2};
		ali = new Alignment(names, seqs);

		// load stuff onto the contact map pane and the 3D visualizer
		doLoadSecondModel(mod2, ali);

		// adapt GUI behavior
		setGUIStatusCompareMode();
	}

	/**
	 * Handles the results retrieval of the SADP run which has invoked this 
	 * notifier.
	 * @param notifier  a notifier of an SADP run
	 */
	private void handlePairwiseAlignmentResults(SADPDialogDoneNotifier notifier) {
		try {
			Integer exitStatus = notifier.notification();

			if( exitStatus == ToolDialog.DONE ) {
				//SADPRunner runner = notifier.getRunner();
				SADPResult result = notifier.getResult();
				ali         = result.getAlignment();

				// load aligned models onto contact map pane and 3D visualizer
				doLoadSecondModel(mod2, ali);

				// adapt GUI behavior
				setGUIStatusCompareMode();
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			mod2 = null;
			ali = null;
			System.gc();
		}
	}

	/**
	 * Loads the second model and its alignment to the first onto the contact map panel. 
	 * @param mod2  the second model
	 * @param ali  alignment of residues in <code>mod</code> to residues 
	 *  in <code>mod2</code>. Tags of the sequences are the 
	 *  loadedGraphID of each Model
	 */
	private void doLoadSecondModel(Model mod2, Alignment ali) {

		// re-setting window title
		System.out.println("Second contact map loaded.");
		String title = "Comparing " + mod.getLoadedGraphID() +" and "+mod2.getLoadedGraphID();
		this.setTitle(title);

		// add the second model and update the image buffer
		cmPane.setSecondModel(mod2, ali);
		guiState.setCompareMode(true);
		cmPane.updateScreenBuffer();
		guiState.setSelectionMode(GUIState.INITIAL_SEL_MODE);	// we reset to SQUARE_SEL in case another one (possibly not allowed in compare mode) was switched on
		// finally repaint the whole thing to display the whole set of contacts
		cmPane.repaint();
		
		if (Start.isPyMolConnectionAvailable() && mod2.has3DCoordinates()) {
			// load structure in the 3D visualizer
			Start.getPyMolAdaptor().loadStructure(mod2.getTempPdbFileName(), mod2.getLoadedGraphID(), true);

			if (mod.has3DCoordinates()) {
				// clear the view (disables all previous selections)
				Start.getPyMolAdaptor().setView(mod.getLoadedGraphID(), mod2.getLoadedGraphID());
				
				if(cmPane.getCommonContacts().isEmpty()) {
					Start.getPyMolAdaptor().alignStructures(mod.getLoadedGraphID(), mod2.getLoadedGraphID());
				} else {
					// show superpositioning according to the common contacts in pymol
					TreeSet<Integer> columns = new TreeSet<Integer>();
					cmPane.getAlignmentColumnsFromContacts(cmPane.getCommonContacts(),columns); 
					doSuperposition3D(mod, mod2, ali, columns);
				}
			}
		}
		
	}

	private void handleSuperposition3D() {
		if (!Start.isPyMolConnectionAvailable()){
			showNoPyMolConnectionWarning();
		} else if (!mod.has3DCoordinates()) {
			showNo3DCoordsWarning(mod);
		} else if (!mod2.has3DCoordinates()) {
			showNo3DCoordsWarning(mod2);
		} else {
			// clear the view (disables all previous selections)
			Start.getPyMolAdaptor().setView(mod.getLoadedGraphID(), mod2.getLoadedGraphID());	
			doSuperposition3D(mod, mod2, ali, cmPane.getAlignmentColumnsFromSelectedContacts());
		}
		
	}

	/**
	 * Superimposes the given models with respect to the given alignment, based on
	 * the given alignment columns, defined by a set of selected contacts.  
	 * The residues incident to the contacts in the selection are mapped to alignment 
	 * columns which construct the set of paired residues (matches and/or mismatches) 
	 * to be passed for the superpositioning.
	 * Can only be called if pymol connection is available, must be checked before!
	 * @param mod1  the first model
	 * @param mod2  the second model
	 * @param ali  alignment of residues in <code>mod1</code> to residues in 
	 *  <code>mod2</code>
	 * @param columns  set of alignment columns to be considered
	 */
	public void doSuperposition3D(Model mod1, Model mod2, Alignment ali, TreeSet<Integer> columns) {
		TreeSet<String> projectionTags = new TreeSet<String>();

		// get consecutive sequence chunks in mod1 from positions
		projectionTags.add(mod2.getLoadedGraphID());
		IntervalSet chunks1 = ali.getMatchingBlocks(mod1.getLoadedGraphID(), projectionTags, columns, 1);

		if( chunks1.isEmpty() ) {
			showNo3DCoordFromComparedSelection();
			return;
		}

		// get consecutive sequence chunks in mod2 from positions
		projectionTags.clear();
		projectionTags.add(mod1.getLoadedGraphID());
		IntervalSet chunks2 = ali.getMatchingBlocks(mod2.getLoadedGraphID(), projectionTags, columns, 1);

		if( chunks2.isEmpty() ) {
			// this one should catch at the same time as the one above.
			// this is only to make sure
			showNo3DCoordFromComparedSelection();
			return;
		}

		// we let pymol compute the pairwise fitting
		Start.getPyMolAdaptor().pairFitSuperposition(
				mod1.getLoadedGraphID(), /*identities first model*/
				mod2.getLoadedGraphID(), /*identifies second model*/
				chunks1, chunks2);       /*intervals of corresponding residues*/
	}

	/**
	 * Sends the induced residue-residue alignment of the given contact 
	 * selection to the visualizer. 
	 * Can only be called if a pymol connection is available. 
	 * @param mod1  the first model
	 * @param mod2  the second model
	 * @param ali  alignment of residues in <code>mod1</code> to residues in 
	 *  <code>mod2</code>
	 * @param columns  set of alignment columns to be considered
	 */
	public void doShowAlignedResidues3D(Model mod1, Model mod2, Alignment ali, TreeSet<Integer> columns) {
		// extract the residue sequence indices from the 'columns', do not only 
		// consider (mis)matches
		IntPairSet residuePairs = new IntPairSet();
		int pos1,pos2;
		for(int col : columns) {
			pos1 = ali.al2seq(mod1.getLoadedGraphID(),col);
			pos2 = ali.al2seq(mod2.getLoadedGraphID(),col);

			if( pos1 != -1 && pos2 != -1 ) {
				residuePairs.add(new Pair<Integer>(pos1,pos2));
			}
		}

		if( residuePairs.isEmpty() ) {
			showNo3DCoordFromComparedSelection();
			return;
		}	    

		PyMolAdaptor pymol = Start.getPyMolAdaptor();
		
		// disable all previously made objects and selections only once! 
		if( !residuePairs.isEmpty() ) {
			pymol.setView(mod1.getLoadedGraphID(), mod2.getLoadedGraphID());
		} else {
			// nothing to do!
			return;
		}

		//TODO move this code to a method in PyMolAdaptor
		// prepare selection names
		String topLevelGroup = "Sel" + View.pymolSelSerial;
		String firstObjSel   = mod1.getLoadedGraphID();
		String secondObjSel  = mod2.getLoadedGraphID();
		String edgeSel       = topLevelGroup + "_" + firstObjSel + "_" + secondObjSel + "_AliEdges";
		String nodeSel       = edgeSel + "_Nodes";
		
		// color for the alignment edges
		String aliEdgeColor = "blue";
		
		// send alignment edges to PyMol
		pymol.edgeSelection(firstObjSel, secondObjSel, edgeSel, nodeSel, aliEdgeColor, residuePairs, false);
		
		// group selection in topLevelGroup
		pymol.group(topLevelGroup, edgeSel + " " + nodeSel, null);
		
		++View.pymolSelSerial;
	}

	private void handleSaveToGraphDb() {
		if(this.mod == null) {
			showNoContactMapWarning();
		} else if(!Start.isDatabaseConnectionAvailable()) {
			showNoDatabaseConnectionWarning();
		} else {
			String ret = JOptionPane.showInputDialog(this, (Object) "<html>Name of database to save to:<br></html>");
			if(ret != null) {
				if(ret.length() > 0) {
					doSaveToGraphDb(ret);
				} else {
					JOptionPane.showMessageDialog(this, "No database name given. Contact map not saved.", "Warning", JOptionPane.WARNING_MESSAGE);
				}
			}
		}
	}

	public void doSaveToGraphDb(String dbName) {
		try {
			mod.writeToGraphDb(dbName);
			System.out.println("Saving contact map to database " + dbName + ".");
		} catch (SQLException e) {
			System.err.println("Error when trying to write to database " + dbName + ": " + e.getMessage());
		}
	}

	private void handleSaveToCmFile() {
		if(this.mod == null) {
			//System.out.println("No contact map loaded yet.");
			showNoContactMapWarning();
		} else {
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();
				if (confirmOverwrite(chosenFile)) {
					String path = chosenFile.getPath();
					try {
						this.mod.writeToContactMapFile(path);
					} catch(IOException e) {
						System.err.println("Error writing to file " + path);
					}
				}
			}
		}
	}

	private void handleSaveToCaspRRFile() {
		if(this.mod == null) {
			//System.out.println("No contact map loaded yet.");
			showNoContactMapWarning();
		} else {
			showWriteToRRFileWarning();
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();
				if (confirmOverwrite(chosenFile)) {
					String path = chosenFile.getPath();
					try {
						this.mod.writeToCaspRRFile(path);
					} catch(IOException e) {
						System.err.println("Error writing to file " + path);
					}
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
				if (confirmOverwrite(chosenFile)) {
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
	}

	private void handleSaveAlignment() {
		doSaveAlignment(ali);
	}

	public void doSaveAlignment(Alignment ali) {
		if(ali == null) {
			//System.out.println("No contact map loaded yet.");
			showCannotSaveEmptyAlignment();
		} else {
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();
				if (confirmOverwrite(chosenFile)) {
					try {
						ali.writeFasta(new FileOutputStream(chosenFile), 80, true);
					} catch (IOException e) {
						System.err.println("Error while trying to write to FASTA file " + chosenFile.getPath());
					}
				}
			}
		}	
	}

	private void handleInfo() {
		if(this.mod == null) {
			//System.out.println("No contact map loaded yet.");
			showNoContactMapWarning();
		} else {
			
			JDialog infoDialog = new ContactMapInfoDialog(this, mod, mod2, ali, cmPane);
			infoDialog.setLocationRelativeTo(this);
			infoDialog.setVisible(true);
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
		Start.shutDown(0);
	}

	/* -------------------- View Menu -------------------- */

	/**
	 * 
	 */
	private void handleShowPdbResSers() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if (!mod.has3DCoordinates()){
			showNo3DCoordsWarning(mod);
		} else {
			guiState.setShowPdbSers(!guiState.getShowPdbSers());
			cmPane.updateScreenBuffer();
			if(guiState.getShowPdbSers()) {
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
			guiState.setShowRulers(!guiState.getShowRulers());
			if(guiState.getShowRulers()) {
				cmp.add(topRul, BorderLayout.NORTH);
				cmp.add(leftRul, BorderLayout.WEST);
				mmViewRulers.setIcon(icon_selected);
			} else {
				cmp.remove(topRul);
				cmp.remove(leftRul);
				mmViewRulers.setIcon(icon_deselected);
			}
			this.pack();
			this.repaint();
		}
	}

	/**
	 * 
	 */
	private void handleShowIconBar() {
		guiState.setShowIconBar(!guiState.getShowIconBar());
		if(guiState.getShowIconBar()) {
			toolBar.setVisible(true);
			mmViewIconBar.setIcon(icon_selected);
		} else {
			toolBar.setVisible(false);
			mmViewIconBar.setIcon(icon_deselected);
		}
		this.repaint();		
	}	

	/**
	 * 
	 */
	private void handleShowNbhSizeMap() {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			guiState.setShowNbhSizeMap(!guiState.getShowNbhSizeMap());
			cmPane.toggleNbhSizeMap(guiState.getShowNbhSizeMap());
			if (guiState.getShowNbhSizeMap()) {
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
			guiState.setShowDensityMap(!guiState.getShowDensityMap());
			cmPane.toggleDensityMap(guiState.getShowDensityMap());
			if(guiState.getShowDensityMap()) {
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
			showNo3DCoordsWarning(mod);
		} else {
			guiState.setShowDistanceMap(!guiState.getShowDistanceMap());
			cmPane.toggleDistanceMap(guiState.getShowDistanceMap());
			if(guiState.getShowDistanceMap()) {
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

	private void handleSelectByResNum() {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			String selStr = (String)JOptionPane.showInputDialog(
					this,
					"Enter residue numbers to be selected:\n"
					+ "e.g. 1-5,7,10-11",
			"");
			if (selStr == null) {
				// user clicked cancel
				return;
			} else {
				if(!Interval.isValidSelectionString(selStr)) {
					showInvalidSelectionStringWarning();
					return;
				} else {
					int selectedContacts = cmPane.selectByResNum(selStr);
					System.out.println(selectedContacts + " contacts selected");					
				}
			}
		}
	}

	private void handleSelectHelixHelix() {
		if(mod==null) {
			showNoContactMapWarning();
		} else
			if(!mod.hasSecondaryStructure()) {
				showNoSecondaryStructureWarning();
			} else {
				cmPane.selectHelixHelix();
			}
	}

	private void handleSelectBetaBeta() {
		if(mod==null) {
			showNoContactMapWarning();
		} else
			if(!mod.hasSecondaryStructure()) {
				showNoSecondaryStructureWarning();
			} else {
				cmPane.selectBetaBeta();
			}
	}

	private void handleSelectInterSsContacts() {
		if(mod==null) {
			showNoContactMapWarning();
		} else
			if(!mod.hasSecondaryStructure()) {
				showNoSecondaryStructureWarning();
			} else {
				cmPane.selectInterSsContacts();
			}
	}

	private void handleSelectIntraSsContacts() {
		if(mod==null) {
			showNoContactMapWarning();
		} else
			if(!mod.hasSecondaryStructure()) {
				showNoSecondaryStructureWarning();
			} else {
				cmPane.selectIntraSsContacts();
			}
	}


	/* -------------------- Color menu -------------------- */

	private void handleColorSelect() {	
		JDialog chooseDialog = JColorChooser.createDialog(this, "Choose color", true, Start.getColorChooser(), 
				new ActionListener() {
			public void actionPerformed(ActionEvent e){
				Color c = Start.getColorChooser().getColor();
				guiState.setPaintingColor(c);
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
			cmPane.paintCurrentSelection(guiState.getPaintingColor());
			cmPane.resetSelections();
			cmPane.repaint();
		}
	}

	private void handleColorReset() {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			cmPane.resetUserContactColors();
		}
	}

	/* -------------------- Action menu -------------------- */

	/**
	 * 
	 */
	private void handleShowSelContacts3D() {
		
		// pymol adapter
		PyMolAdaptor pymol = Start.getPyMolAdaptor();
		
		if(mod==null) {
			showNoContactMapWarning();
		} else if(!Start.isPyMolConnectionAvailable()) {
			showNoPyMolConnectionWarning();
		} else if(!mod.has3DCoordinates()) {
			showNo3DCoordsWarning(mod);
		} else if(cmPane.getSelContacts().size() == 0) {
			showNoContactsSelectedWarning();
		} else if (cmPane.hasSecondModel()){
			
			if(!mod2.has3DCoordinates()) {
				showNo3DCoordsWarning(mod2);
				return;
			}
			// chain selection names. The names are only used the access the 
			// right chain. We do not create this selection explicitely!!!
			String firstChainSel  = mod.getLoadedGraphID();
			String secondChainSel = mod2.getLoadedGraphID();

			
			// COMMON:
			//   FIRST -> common contacts in first model -> draw solid yellow lines
			//   SECOND -> "" second ""                  -> draw solid yellow lines
			// ONLY_FIRST:
			//   FIRST -> contacts only pres. in first model -> draw solid red lines
			//   SECOND -> -> draw dashed green lines
			// ONLY_SECOND:
			//   SECOND -> contacts only pres. in sec. model -> draw solid green lines
			//   FIRST -> -> draw dashed red lines
			
			TreeMap<ContactMapPane.ContactSelSet, IntPairSet[]> selMap = cmPane.getSetsOfSelectedContactsFor3D();
			
			// disable all previously made objects and selections only once! 
			if( !selMap.get(ContactMapPane.ContactSelSet.COMMON)[ContactMapPane.FIRST].isEmpty() ||
				!selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.FIRST].isEmpty() ||
				!selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.SECOND].isEmpty() ) {
				
				Start.getPyMolAdaptor().setView(mod.getLoadedGraphID(),
												mod2.getLoadedGraphID());
			} else {
				// nothing to do!
				return;
			}
			
			// groups and edge selection names, this naming convention yields 
			// the following grouping tree in PyMol:
			// topLevelGroup
			//   |--firstModGroup
			//   |    |--presFirstEdgeSel
			//   |    |--presFirstNodeSel
			//   |    |--absFirstEdgeSel
			//   |     `-absFirstNodeSel
			//    `-secondModGroup
			//        |--...
			//        ...
			String topLevelGroup     = "Sel" + View.pymolSelSerial;
			String firstModGroup     = topLevelGroup + "_" + firstChainSel;			
			String secondModGroup    = topLevelGroup + "_" + secondChainSel;
			
			String presFirstEdgeSel  = firstModGroup + "_PresCont";
			String presFirstNodeSel  = firstModGroup + "_PresCont_Nodes";
			String absFirstEdgeSel   = firstModGroup + "_AbsCont";
			String absFirstNodeSel   = firstModGroup + "_AbsCont_Nodes";
			String commonFirstEdgeSel = firstModGroup + "_CommonCont";
			String commonFirstNodeSel = firstModGroup + "_CommonCont_Nodes";
			
			String presSecondEdgeSel = secondModGroup + "_PresCont";
			String presSecondNodeSel = secondModGroup + "_PresCont_Nodes";
			String absSecondEdgeSel  = secondModGroup + "_AbsCont";
			String absSecondNodeSel  = secondModGroup + "_AbsCont_Nodes";
			String commonSecondEdgeSel = secondModGroup + "_CommonCont";
			String commonSecondNodeSel = secondModGroup + "_CommonCont_Nodes";
			
			// for coloring the edges
			String firstColor  = "magenta";
			String secondColor = "green";
			String commonColor = "yellow";
			
			// send common contacts in the first and second model. It suffices 
			// to check size only for one set as both are supposed to be of 
			// same size.			
			if( selMap.get(ContactMapPane.ContactSelSet.COMMON)[ContactMapPane.FIRST].size()  != 0 ) {				
				// send common contacts to the object corresponding to the first model
				pymol.edgeSelection(firstChainSel, firstChainSel, commonFirstEdgeSel, commonFirstNodeSel, 
									commonColor, 
									selMap.get(ContactMapPane.ContactSelSet.COMMON)[ContactMapPane.FIRST], 
									false);

				// send common contacts to the object corresponding to the second model
				pymol.edgeSelection(secondChainSel, secondChainSel, commonSecondEdgeSel, commonSecondNodeSel,
									commonColor, 
									selMap.get(ContactMapPane.ContactSelSet.COMMON)[ContactMapPane.SECOND], 
									false);
				
				// TODO: create node selections from selMap and add them to the	group!!!
				
				// group first and second structure selection
				pymol.group(firstModGroup,  commonFirstEdgeSel,  null);
				pymol.group(firstModGroup,  commonFirstNodeSel,  null);
				pymol.group(secondModGroup, commonSecondEdgeSel, null);
				pymol.group(secondModGroup, commonSecondNodeSel, null);
				
				// and group everything in the topLevelGroup representing the 
				// whole selection
				pymol.group(topLevelGroup, firstModGroup,  null);
				pymol.group(topLevelGroup, secondModGroup, null);
			}
			
			// send contacts present in the first and absent in the second model
			if( selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.FIRST].size() != 0 ) {
				
				// draw true contacts being present in the first model between 
				// the residues of the first model
				pymol.edgeSelection(firstChainSel, firstChainSel, presFirstEdgeSel, presFirstNodeSel,
									firstColor,
									selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.FIRST], 
									false);

				// group selection of present contact in the first structure
				pymol.group(firstModGroup, presFirstEdgeSel, null);
				pymol.group(firstModGroup, presFirstNodeSel, null);
				pymol.group(topLevelGroup, firstModGroup,    null);
				
				if( selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.SECOND].size() != 0 ) {
					// draw "contact" being absent in the second model but 
					// NOT in the first one between the residues of the second 
					// model
					pymol.edgeSelection(secondChainSel, secondChainSel, absSecondEdgeSel, absSecondNodeSel,
										secondColor,
										selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.SECOND], 
										true);
					
					// group selection of absent contact in the second 
					// structure
					pymol.group(secondModGroup, absSecondEdgeSel, null);
					pymol.group(secondModGroup, absSecondNodeSel, null);
					pymol.group(topLevelGroup,  secondModGroup,   null);
				} 
			}
			
			// send contacts present in the first and absent in the second model
			if( selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.SECOND].size() != 0 ) {
				
				// draw true contacts being present in the second model between 
				// the residues of the between model
				pymol.edgeSelection(secondChainSel, secondChainSel, presSecondEdgeSel, presSecondNodeSel,
									secondColor,
									selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.SECOND],
									false);

				// group selection of present contact
				pymol.group(secondModGroup, presSecondEdgeSel, null);
				pymol.group(secondModGroup, presSecondNodeSel, null);
				pymol.group(topLevelGroup,  secondModGroup,    null);
				
				if( selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.FIRST].size() != 0 ) {
					// draw true contact being present in the second model but 
					// NOT in the first one between the residues of the first 
					// model
					pymol.edgeSelection(firstChainSel, firstChainSel, absFirstEdgeSel, absFirstNodeSel, 
										firstColor,
										selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.FIRST], 
										true);
					
					// group selection of absent contact
					pymol.group(firstModGroup, absFirstEdgeSel, null);
					pymol.group(firstModGroup, absFirstNodeSel, null);
					pymol.group(topLevelGroup, firstModGroup,   null);
				} 
			}
		} else {
			IntPairSet contacts   = cmPane.getSelContacts();

			String chainObj       = mod.getLoadedGraphID();
			
			if( contacts.isEmpty() ) {
				return; // nothing to do!
			}

			String topLevelGroup  = "Sel" + View.pymolSelSerial;
			String edgeSel        = topLevelGroup + "_" + chainObj + "_Cont";
			String nodeSel        = topLevelGroup + "_" + chainObj + "_Nodes";
			
			//disable all old objects and selections
			pymol.sendCommand("disable all");
			pymol.sendCommand("enable " + chainObj);

			// send selection
			pymol.edgeSelection(chainObj, chainObj, edgeSel, nodeSel, "magenta", contacts, false);
			
			pymol.group(topLevelGroup,  edgeSel + " " + nodeSel, null);
		}
		
		// and finally increment the serial for the PyMol selections
		View.pymolSelSerial++;
	}


	/**
	 * 
	 */
	private void handleShowDistance3D() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if(!mod.has3DCoordinates()) {
			showNo3DCoordsWarning(mod);
		} else if(!Start.isPyMolConnectionAvailable()) {				
			showNoPyMolConnectionWarning();
		} else {
			Start.getPyMolAdaptor().sendSingleEdge(mod.getLoadedGraphID(), View.pymolSelSerial, cmPane.getRightClickCont());
			View.pymolSelSerial++;
		}
	}

	/**
	 * 
	 */
	private void handleShowTriangles3D() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if(!mod.has3DCoordinates()) {
			showNo3DCoordsWarning(mod);
		} else if(!Start.isPyMolConnectionAvailable()) {				
			showNoPyMolConnectionWarning();
		} else if(cmPane.getCommonNbh() == null) {
			showNoCommonNbhSelectedWarning();
		} else {
			PyMolAdaptor pymol = Start.getPyMolAdaptor();
			// TODO move this code to PyMolAdaptor
			String topLevelGroup    = "Sel" + View.pymolSelSerial;
			String chainObjName     = mod.getLoadedGraphID();
			String triangleBaseName = topLevelGroup + "_" + chainObjName + "_NbhTri";
			String nodeSelName      = triangleBaseName + "_Nodes";
			
			//pymol.showTriangles(mod.getPDBCode(), mod.getChainCode(), cmPane.getCommonNbh(), pymolNbhSerial); 
			Collection<String> triangleSelNames =  pymol.showTriangles(chainObjName, triangleBaseName, nodeSelName, cmPane.getCommonNbh());
			
			String groupMembers = "";
			for( String s : triangleSelNames ) {
				groupMembers += s + " ";
			}
			
			groupMembers += nodeSelName;
			
			pymol.group(topLevelGroup, groupMembers, null);
			
			++View.pymolSelSerial;
		}
	}

	/**
	 * Sends set aligned residues to the visualizer. Calls function 
	 * {@link #doShowAlignedResidues3D(Model, Model, Alignment, TreeSet<Integer>)} 
	 * with the currently selected contacts being set to the selected contacts.
	 */
	private void handleShowAlignedResidues3D() {
		if (!Start.isPyMolConnectionAvailable()) {
			showNoPyMolConnectionWarning();
		} else  if (!mod.has3DCoordinates()){
			showNo3DCoordsWarning(mod);
		} else if (!mod2.has3DCoordinates()) {
			 showNo3DCoordsWarning(mod2);
		} else {
			doShowAlignedResidues3D(mod, mod2, ali, cmPane.getAlignmentColumnsFromSelectedContacts());
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


	/* -------------------- Compare menu -------------------- */

	/**
	 * Handler for the show/hide common contacts button in compare mode
	 */
	private void handleShowCommon(){		
		if(mod==null) {
			showNoContactMapWarning();
		} else
			if(!cmPane.hasSecondModel()) {
				showNoSecondContactMapWarning();
			} else {
				guiState.setShowCommon(!guiState.getShowCommon());
				cmPane.toggleShownContacts(guiState.getShowCommon());
				if(guiState.getShowCommon()) {
					mmShowCommon.setIcon(icon_selected);
				} else {
					mmShowCommon.setIcon(icon_deselected);
				}
			}
		tbShowCommon.setSelected(guiState.getShowCommon());
	}

	/**
	 * Handler for the show/hide unique first structure contacts button in compare mode
	 */	
	private void handleShowFirst(){
		if(mod==null) {
			showNoContactMapWarning();
		} else
			if(!cmPane.hasSecondModel()) {
				showNoSecondContactMapWarning();
			} else {
				guiState.setShowFirst(!guiState.getShowFirst());
				cmPane.toggleShownContacts(guiState.getShowFirst());
				tbShowFirst.setSelected(guiState.getShowFirst());
				if(guiState.getShowFirst()) {
					mmShowFirst.setIcon(icon_selected);
				} else {
					mmShowFirst.setIcon(icon_deselected);
				}
			}
		tbShowFirst.setSelected(guiState.getShowFirst());
	}

	/**
	 * Handler for the show/hide unique second structure contacts button in compare mode
	 */		
	private void handleShowSecond(){
		if(mod==null) {
			showNoContactMapWarning();
		} else
			if(!cmPane.hasSecondModel()) {
				showNoSecondContactMapWarning();
			} else {
				guiState.setShowSecond(!guiState.getShowSecond());
				cmPane.toggleShownContacts(guiState.getShowSecond());
				if(guiState.getShowSecond()) {
					mmShowSecond.setIcon(icon_selected);
				} else {
					mmShowSecond.setIcon(icon_deselected);
				}
			}
		tbShowSecond.setSelected(guiState.getShowSecond());
	}

	/**
	 * Handler for the show/hide difference map overlay in compare mode
	 */	
	private void handleToggleDiffDistMap() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if (!mod.has3DCoordinates()) {
			showNo3DCoordsWarning(mod);
		} else if (!cmPane.hasSecondModel()) {
			showNoSecondContactMapWarning();
		} else if (!mod2.has3DCoordinates()) { // it's ok to use here the original mod2 and not the actual displayed alignMod2 (ContactMapPane.mod2) because both should have (or not) 3D coordinates 
			showNo3DCoordsWarning(mod2);
		} else {
			guiState.setShowDiffDistMap(!guiState.getShowDiffDistMap());
			cmPane.toggleDiffDistMap(guiState.getShowDiffDistMap());
			if(guiState.getShowDiffDistMap()) {
				mmToggleDiffDistMap.setIcon(icon_selected);
			} else {
				mmToggleDiffDistMap.setIcon(icon_deselected);
			}
		}
	}

	/* -------------------- Help menu -------------------- */

	private void handleHelpWriteConfig() {
		File localConfigFile = new File(Start.CONFIG_FILE_NAME);
		if (confirmOverwrite(localConfigFile)) {
			try {
				Start.writeExampleConfigFile(Start.CONFIG_FILE_NAME);
				System.out.println("Writing example configuration file " + new File(Start.CONFIG_FILE_NAME).getAbsolutePath());
			} catch(IOException e) {
				System.err.println("Could not write configuration file " + new File(Start.CONFIG_FILE_NAME).getAbsolutePath() + ": " + e.getMessage());
			}
		}
	}

	private void handleHelpAbout() {
		JOptionPane.showMessageDialog(this,
				"<html><center>" +
				"<p>&nbsp;</p>" +
				"<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
				"CMView v"+Start.VERSION +
				"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</p>" +
				"<p>&nbsp;</p>" +
				"<p>(C) 2008 The Structural Proteomics Group</p>" +
				"<p>Max Planck Institute for Molecular Genetics</p>" +
				"<p>Berlin, Germany</p>" +
				"<p>&nbsp;</p>" +
				"</center></html>",
				"About",
				JOptionPane.PLAIN_MESSAGE);
	}

	/**
	 * Checks for existence of given file and displays a confirm dialog
	 * @param file
	 * @return true if file does not exist or user clicks on Yes, false if file
	 *  exists and user clicks on No
	 */
	private boolean confirmOverwrite(File file) {
		if(file.exists()) {
			String message = "File " + file.getAbsolutePath() + " already exists. Overwrite?";
			int ret = JOptionPane.showConfirmDialog(this,message, "Confirm overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(ret != JOptionPane.YES_OPTION) {
				return false;
			} else {
				return true;
			}
		} 
		return true;
	}
	
	/* -------------------- Warnings -------------------- */

	/** Shows a window with a warning message that no contact map is loaded yet */
	private void showNoContactMapWarning() {
		JOptionPane.showMessageDialog(this, "No contact map loaded yet", "Warning", JOptionPane.INFORMATION_MESSAGE);
	}

	/** Shows a window with a warning message that no secondary structure information is available */
	private void showNoSecondaryStructureWarning() {
		JOptionPane.showMessageDialog(this, "No secondary structure information available", "Warning", JOptionPane.INFORMATION_MESSAGE);
	}	

	/** Shows a window with a warning message that no second contact map is loaded yet (for comparison functions) */
	private void showNoSecondContactMapWarning() {
		JOptionPane.showMessageDialog(this, "No second contact map loaded yet", "Warning", JOptionPane.INFORMATION_MESSAGE);
	}	

	/** Warning dialog to be shown if a function is being called which requires 3D coordinates and they are missing */
	private void showNo3DCoordsWarning(Model mod){
		JOptionPane.showMessageDialog(this, "No 3D coordinates are associated with contact map "+mod.getLoadedGraphID(), "Warning", JOptionPane.INFORMATION_MESSAGE);
	}

	private void showNo3DCoordFromComparedSelection() {
		Object[] message = {"Cannot assign any 3D-coordinates to the given selection.",
		"Please create your contact selection from the set of common contacts only."};
		JOptionPane.showMessageDialog(this, message, "3D Coordinates Error", JOptionPane.ERROR_MESSAGE);
	}

	/** Error dialog to be shown if loading a model failed. */
	private void showLoadError(String message) {
		JOptionPane.showMessageDialog(this, "<html>Failed to load contact map:<br>" + message + "</html>", "Load Error", JOptionPane.ERROR_MESSAGE);
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

	/**
	 * Warning dialog shown if user tries to write a non-Cb contact map to a Casp RR file (which by convention is Cb)
	 */
	private void showWriteToRRFileWarning() {
		JOptionPane.showMessageDialog(this, "<html>Casp RR files by convention specify contacts between C-beta atoms.<br>" +
			 	                            "The current contact map has a different contact type. This information<br>" +
			 	                            "will be lost when writing to the file.</html>", "Warning", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private void showInvalidSelectionStringWarning() {
		JOptionPane.showMessageDialog(this, "Invalid selection string", "Warning", JOptionPane.INFORMATION_MESSAGE);				
	}

	private void showCannotSaveEmptyAlignment() {
		JOptionPane.showMessageDialog(this, "Cannot save empty alignment!", "Save error", JOptionPane.ERROR_MESSAGE);
	}


	/*---------------------------- public methods ---------------------------*/

	/** 
	 * Create and show a new view window, showing a contact map based on the given model and dispose the current one.
	 * TODO: Currently being abused as a constructor for new windows (something the real constructor should do).
	 */
	public void spawnNewViewWindow(Model mod) {
		String title = mod.getLoadedGraphID();
		View view = new View(mod, "Contact Map of " + title);
		if(view == null) {
			JOptionPane.showMessageDialog(this, "Could not initialize contact map window", "Load error", JOptionPane.ERROR_MESSAGE);
		} else {
			if(this.mod == null ) {
				// print the new View directly on top of the previous one if 
				// this was empty
				Rectangle old_r = this.getBounds();
				Rectangle cur_r = view.getBounds();
				Rectangle new_r = new Rectangle(old_r.x,old_r.y,cur_r.width,cur_r.height);
				view.setBounds(new_r);
			}

			System.out.println("Contact map " + title + " loaded.");

			if(ContactMapPane.BG_PRELOADING) {
				view.cmPane.preloadBackgroundMaps();
			}
			
			// if previous window was empty (not showing a contact map) dispose it
			if(this.mod == null) {
				this.setVisible(false);
				this.dispose();
			}
			
			
			// load structure in PyMol (in a new thread so that it doesn't interfere with other stuff happening at the same time)
			// This is to fix a bug in Windows 2000/XP with Java6 (but not Vista): 
			// when loading small structures (~<100 residues) the connection to PyMol is lost in the middle of the loadStructure() call
			// This happens only when it is the first structure loaded in CMView. Thus this seems to be some weird interaction of 
			// the spawning window threads and the pymol communication happening at the same time 
			final Model themodel = mod; // dirty trick to be able to call mod from within the run() of the new thread
			if (Start.isPyMolConnectionAvailable() && mod.has3DCoordinates()) {
				new Thread() {
					public void run() {
						Start.getPyMolAdaptor().loadStructure(themodel.getTempPdbFileName(), themodel.getLoadedGraphID(), false);	
					}
				}.start();
			}		
		}
	}

	/* -------------------- getter methods -------------------- */
	
	/**
	 * Returns the gui state object associated with this View.
	 * @return the gui state object associated with this View
	 */
	protected GUIState getGUIState() {
		return this.guiState;
	}
	
}

