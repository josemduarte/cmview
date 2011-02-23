package cmview;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
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

import cmview.datasources.*;
import cmview.gmbp.ContactView;
import cmview.jpredAdapter.JPredDialog;
import cmview.sadpAdapter.SADPDialog;
import cmview.sadpAdapter.SADPDialogDoneNotifier;
import cmview.sadpAdapter.SADPResult;
import cmview.sadpAdapter.SADPRunner;
import cmview.tinkerAdapter.TinkerAction;
import cmview.tinkerAdapter.TinkerPreferencesDialog;
import cmview.tinkerAdapter.TinkerRunAction;
import cmview.toolUtils.ToolDialog;
import edu.uci.ics.jung.graph.util.Pair;

import owl.core.runners.DaliRunner;
import owl.core.runners.tinker.TinkerRunner;
import owl.core.sequence.alignment.AlignmentConstructionException;
import owl.core.sequence.alignment.MultipleSequenceAlignment;
import owl.core.sequence.alignment.PairwiseSequenceAlignment;
import owl.core.sequence.alignment.PairwiseSequenceAlignment.PairwiseSequenceAlignmentException;
import owl.core.structure.*;
import owl.core.structure.features.SecondaryStructure;
import owl.core.structure.scoring.ResidueContactScoringFunction;
import owl.core.util.FileFormatError;
import owl.core.util.IntPairSet;
import owl.core.util.Interval;
import owl.core.util.IntervalSet;
import owl.core.util.actionTools.Getter;
import owl.core.util.actionTools.GetterError;

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
	// aliases
	private static final boolean FIRST_MODEL = false;
	private static final boolean SECOND_MODEL = true;
	private static final boolean PDB_FILE = false;
	private static final boolean TS_FILE = true;	

	// text shown in window title
	private static final String TITLE_DEFAULT = Start.APP_NAME+" "+Start.VERSION;
	private static final String TITLE_PREFIX = "";
	private static final String TITLE_MODIFIED = " (modified)";
	private static final String TITLE_COMPARING = "Comparing ";
	private static final String TITLE_TO = " to ";
	
	private static final String LOUP_WINDOW_TITLE = "Loupe";
	
	// Menu item labels (used in main menu, popup menu and icon bar)
	// File
	private static final String LABEL_FILE_INFO = "Info";
	private static final String LABEL_FILE_PRINT = "Print...";	
	private static final String LABEL_FILE_QUIT = "Quit";
	private static final String LABEL_ALIGNMENT_FILE = "FASTA Alignment...";
	private static final String LABEL_PNG_FILE = "PNG File...";
	private static final String LABEL_CASP_RR_FILE = "CASP RR File...";
	private static final String LABEL_CONTACT_MAP_FILE = "Contact Map File...";
	private static final String LABEL_PDB_FILE = "PDB File...";
	private static final String LABEL_TS_FILE = "CASP TS File...";
	private static final String LABEL_ONLINE_PDB = "Online PDB...";
	private static final String LABEL_PDBASE = "Pdbase...";
	private static final String LABEL_GRAPH_DB = "Graph Database...";
	private static final String LABEL_LOAD_SEQ = "Sequence...";
	private static final String LABEL_CASP_SERVER_MOD = "CASP Server Models...";
	private static final String LABEL_LOUPE = "Show Loupe";
	
	// Select
	private static final String LABEL_NODE_NBH_SELECTION_MODE = "Neighbourhood Selection Mode";
	private static final String LABEL_DIAGONAL_SELECTION_MODE = "Diagonal Selection Mode";
	private static final String LABEL_FILL_SELECTION_MODE = "Fill Selection Mode";
	private static final String LABEL_SQUARE_SELECTION_MODE = "Square Selection Mode";
	private static final String LABEL_SEL_MODE_COLOR = "Select By Color Mode";
	private static final String LABEL_SHOW_COMMON_NBS_MODE = "Show Common Neighbours Mode";	
	private static final String LABEL_ADD_REMOVE_CONTACTS = "Add/Remove Contacts";
	
	// Action
	private static final String LABEL_DELETE_CONTACTS = "Delete Selected Contacts";
	private static final String LABEL_SHOW_TRIANGLES_3D = "Show Common Neighbour Triangles in 3D";
	private static final String LABEL_SHOW_CONTACTS_3D = "Show Selected Contacts in 3D";
	private static final String LABEL_SHOW_SPHERES_3D = "Show Threshold Spheres for Selected Contacts in 3D";
	protected static final String LABEL_SHOW_SPHERES_POPUP_3D = "Show Threshold Spheres for Residue Pair (%s,%s) in 3D";
	private static final String LABEL_SHOW_SHELL_NBRS = "Show 1st Shell Neighbour-Relationships";
	private static final String LABEL_SHOW_SEC_SHELL = "Show 2nd Shell";
	private static final String LABEL_SHOW_SPHOXEL = "Explore Contact Geometry";
	private static final String LABEL_RUN_TINKER = "Run Distance Geometry";
	private static final String LABEL_MIN_SET = "Run Cone Peeling Algorithm";
	private static final String LABEL_JPRED = "Predict Secondary Structure";
	
	// Compare
	private static final String LABEL_COMPARE_CM = "Load Second Contact Map from"; 
	private static final String LABEL_SHOW_COMMON = "Show Common Contacts";
	private static final String LABEL_SHOW_FIRST = "Show Contacts Unique to First Structure";
	private static final String LABEL_SHOW_SECOND = "Show Contacts Unique to Second structure";
	private static final String LABEL_SHOW_DIFF = "Show Difference Map";
	private static final String LABEL_SUPER_SEL = "Superimpose from Selection";
	private static final String LABEL_SHOW_ALI = "Show Corresponding Residues from Selection";
	private static final String LABEL_SWAP_MODELS = "Swap Models";
	private static final String LABEL_COPY_CONTACTS = "Copy Selected Contacts from Second to First";
	protected static final String LABEL_SHOW_PAIR_DIST_3D = "Show Residue Pair (%s,%s) as Contact in 3D";	// used in ContactMapPane.showPopup
	
	/*--------------------------- member variables --------------------------*/

	private boolean	database_found;
	
	// GUI components in the main frame
	JToolBar toolBar;			// icon tool bar
	JPanel cmp; 				// Main panel holding the Contact map pane
	JPanel topRul;				// Panel for top ruler	// TODO: Move this to ContactMapPane?
	JPanel leftRul;				// Panel for left ruler	// TODO: Move this to ContactMapPane?
	JPopupMenu popup; 		 	// right-click context menu
	JPanel tbPane;				// tool bar panel holding toolBar and cmp (necessary if toolbar is floatable)
	StatusBar statusBar;		// A status bar with metainformation on the right
	TransferFunctionDialog tfDialog;  // Dialogue to change the customizable transfer function
	DeltaRankBar deltaRankBar;	// A Bar at the bottom of the contact map showing delta rank information for the sequence
	
	// Tool bar buttons
	JButton tbFileInfo, tbShowSel3D, tbShowComNbh3D,  tbDelete, tbShowSph3D, tbRunTinker, tbMinSubset, tbLoupe;  
	JToggleButton tbSquareSel, tbFillSel, tbDiagSel, tbNbhSel, tbShowComNbh, tbSelModeColor, tbToggleContacts;
	JToggleButton tbViewPdbResSer, tbViewRuler, tbViewNbhSizeMap, tbViewDistanceMap, tbViewDensityMap, tbShowCommon, tbShowFirst, tbShowSecond;
		
	// indices of the all main menus in the frame's menu bar
	TreeMap<String, Integer> menu2idx;

	// background overlay types
	public enum BgOverlayType {
		//NONE_SELECTED("None selected"),
		DISTANCE("Distance Map"), 
		DENSITY("Contact Density"), 
		COMMON_NBH("Common Nbhd"), 
		DELTA_RANK("Delta Rank"), 
		DIFF_DIST("Difference Map"), 
		ENERGY("Pairwise Energy"),
		TF_FUNC("Variable Transfer Function");
		
		String label;
		
		BgOverlayType(String label) {
			this.label = label;
		}
		
		public String toString() {
			return label;
		}
		
		public Object getItem() {
			return (Object)this;
		}
	};
	
	// contains all types of component that shall not be 
	// considered for the right setting of the visibility 
	// mode of a menu
	HashSet<Class<?>> disregardedTypes;

	HashMap<JPopupMenu,JMenu> popupMenu2Parent;

	TreeMap<String,JMenu> smFile;
	TreeMap<String,JMenu> smCompare;

	// Menu items
	// M -> "menu bar"
	JMenuItem sendM, sphereM, squareM, fillM, comNeiM, triangleM, nodeNbhSelM, rangeM, delEdgesM, minSubsetM, mmSelModeColor, mmSelModeAddRemove;
	// P -> "popup menu"
	JMenuItem sendP, sphereP, squareP, fillP, comNeiP, triangleP, nodeNbhSelP, rangeP,  delEdgesP, popupSendEdge, pmSelModeColor, pmShowShell, pmShowSecShell, pmShowSphoxel;
	// mm -> "main menu"
	JMenuItem mmLoadGraph, mmLoadPdbase, mmLoadCm, mmLoadCaspRR, mmLoadPdb, mmLoadTs, mmLoadFtp, mmLoadSeq, mmLoadCaspServerMods;
	JMenuItem mmLoadGraph2, mmLoadPdbase2, mmLoadCm2, mmLoadCaspRR2, mmLoadPdb2, mmLoadTs2, mmLoadFtp2, mmLoadCaspServerMods2;
	JMenuItem mmSaveGraphDb, mmSaveCmFile, mmSaveCaspRRFile, mmSavePng, mmSaveAli;
	JMenuItem mmViewShowPdbResSers, mmViewHighlightComNbh, mmViewShowDensity, mmViewShowDeltaRank, mmViewShowDistMatrix;
	JMenuItem mmSelectAll, mmSelectByResNum, mmSelectHelixHelix, mmSelectBetaBeta, mmSelectInterSsContacts, mmSelectIntraSsContacts;
	JMenuItem mmColorReset, mmColorPaint, mmColorChoose;
	JMenuItem mmShowCommon,  mmShowFirst,  mmShowSecond;
	JMenuItem mmToggleDiffDistMap;
	JMenuItem mmSuperposition, mmShowAlignedResidues,mmSwapModels,mmCopyContacts,mmJPred,mmRunTinker;
	JMenuItem mmInfo, mmPrint, mmQuit, mmHelpAbout, mmHelpHelp, mmHelpWriteConfig, mmLoupe;

	// Data and status variables
	private GUIState guiState;
	private Model mod;
	private Model mod2;
	private MultipleSequenceAlignment ali;
	public ContactMapPane cmPane;
	public ResidueRuler topRuler;
	public ResidueRuler leftRuler;
	
	ImageIcon icon_selected = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "tick.png"));
	ImageIcon icon_deselected = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "bullet_blue.png"));

	// Windows/Dialogs
	LoadDialog actLoadDialog;
	JDialog loupeWindow;
	LoupePanel loupePanel;
	public ContactView contView;

	// Tinker stuff
	TinkerPreferencesDialog tinkerDialog;
	TinkerRunAction tinkerRunner;
	// invisible notifiers
	SADPDialogDoneNotifier sadpNotifier;

	/*----------------------------- constructors ----------------------------*/

	/** Create a new View object */
	public View(Model mod, String title) {
		super(title);
		Start.viewInstancesCreated();
		this.database_found = Start.isDatabaseConnectionAvailable();
		this.mod = mod;
		this.updateTitle();
		if(mod == null) {
			this.setPreferredSize(new Dimension(Start.INITIAL_SCREEN_SIZE,Start.INITIAL_SCREEN_SIZE));
		}
		this.guiState = new GUIState(this);
		this.initGUI(); 							// build gui tree and pack
		
		// show status bar groups
		if(mod != null) {
			statusBar.showCoordinatesGroup(true);
			statusBar.showOverlayGroup(true);
			if(mod != null && mod.isGraphWeighted()) {
				System.out.println("Graph is weighted");
				statusBar.showMultiModelGroup(true, mod);
			}
			statusBar.revalidate();
		}
		
		setVisible(true);							// show GUI									
		
		final JFrame parent = this;					// need a final to refer to in the thread below
		EventQueue.invokeLater(new Runnable() {		// execute after other events have been processed
			public void run() {
				parent.toFront();					// bring new window to front
			}
		});
	}
	
	public View(Model mod, Model mod2, MultipleSequenceAlignment ali,String title) {
		View v = new View(mod,title);
		v.doLoadSecondModelFromModel(mod2);
	}
	
	/** 
	 * Create a 'silent' view object, i.e. one that is not visible (to be used for batch processing)
	 */
	public View(Model mod) {
		super("Batch mode");
		this.mod = mod;
		this.guiState = new GUIState(this);
		this.initGUI();
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
		statusBar= new StatusBar(this);				// pass reference to 'this' for handling gui actions
		deltaRankBar = new DeltaRankBar();
		
		// init loupe function
		loupeWindow = new JDialog(this, LOUP_WINDOW_TITLE);
		loupePanel = new LoupePanel();
		//loupePanel.add(new JLabel("Test"));
		loupeWindow.getContentPane().add(loupePanel);
		loupeWindow.pack();
		loupeWindow.setLocationRelativeTo(this);
		loupeWindow.setVisible(false);
		
		// Icons
//		System.out.println("PNG: "+this.getClass().getResource(Start.ICON_DIR + "shape_square.png").getFile());
//		System.out.println("PNG: "+this.getClass().getResource(Start.ICON_DIR + "shape_square.png").getPath());
//		File file = new File(this.getClass().getResource(Start.ICON_DIR + "shape_square.png").getPath());
//		ImageIcon icon = new ImageIcon(file.getPath());
		ImageIcon icon_square_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "shape_square.png"));
		ImageIcon icon_fill_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "paintcan.png"));
		ImageIcon icon_diag_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "diagonals.png"));
		ImageIcon icon_nbh_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "group.png"));
		ImageIcon icon_show_sel_cont_3d = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "shape_square_go.png"));
		ImageIcon icon_show_sph_3d = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "spheres.png"));
		ImageIcon icon_show_com_nbs_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "shape_flip_horizontal.png"));
		ImageIcon icon_show_triangles_3d = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "shape_rotate_clockwise.png"));
		ImageIcon icon_del_contacts = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "cross.png"));	
		ImageIcon icon_show_pair_dist_3d = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "user_go.png"));
		ImageIcon icon_colorwheel = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "color_wheel.png"));
		ImageIcon icon_file_info = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "information.png"));						
		ImageIcon icon_loupe = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "zoom.png"));
		ImageIcon icon_show_common = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "page_copy.png"));
		ImageIcon icon_show_first = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "page_delete.png"));
		ImageIcon icon_show_second = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "page_add.png"));
		ImageIcon icon_sel_mode_color = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "color_swatch.png"));
		ImageIcon icon_toggle_contacts = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "toggle.png"));
		ImageIcon icon_run_tinker = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "plugin_go.png"));
		ImageIcon icon_min_set  = new ImageIcon(getClass().getResource(Start.ICON_DIR+"arrow_in.png"));	//icon to run cone peeling algorithm
//		ImageIcon icon_sphoxel_traces = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "sphoxel_traces.png"));
		ImageIcon icon_sphoxel_traces = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "sphere_st.png"));
		Icon icon_color = getCurrentColorIcon();	// magic icon with current painting color
		Icon icon_black = getBlackSquareIcon();		// black square icon

		// Tool bar
		toolBar = new JToolBar();
		toolBar.setVisible(Start.SHOW_ICON_BAR);

		Dimension separatorDim = new Dimension(30,toolBar.getHeight());
		tbFileInfo = makeToolBarButton(icon_file_info, LABEL_FILE_INFO);
		tbLoupe = makeToolBarButton(icon_loupe, LABEL_LOUPE);
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
		tbShowSph3D = makeToolBarButton(icon_show_sph_3d, LABEL_SHOW_SPHERES_3D);
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			tbShowComNbh3D = makeToolBarButton(icon_show_triangles_3d, LABEL_SHOW_TRIANGLES_3D);
		}
				
		toolBar.addSeparator(separatorDim);
		tbToggleContacts = makeToolBarToggleButton(icon_toggle_contacts,LABEL_ADD_REMOVE_CONTACTS,false,true,true);
		tbDelete = makeToolBarButton(icon_del_contacts, LABEL_DELETE_CONTACTS);
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			tbRunTinker = makeToolBarButton(icon_run_tinker,LABEL_RUN_TINKER);
			
		}		
		tbMinSubset = makeToolBarButton(icon_min_set,LABEL_MIN_SET);	
		
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
		selectionModeButtons.add(tbToggleContacts);
		
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
			//sendP = makePopupMenuItem(LABEL_SHOW_CONTACTS_3D, icon_show_sel_cont_3d, popup);
			popupSendEdge = makePopupMenuItem(LABEL_SHOW_PAIR_DIST_3D, icon_show_pair_dist_3d, popup);			
			sphereP = makePopupMenuItem(LABEL_SHOW_SPHERES_POPUP_3D, icon_show_sph_3d, popup);
			if(Start.USE_EXPERIMENTAL_FEATURES) {
				triangleP = makePopupMenuItem(LABEL_SHOW_TRIANGLES_3D, icon_show_triangles_3d, popup);
				pmShowShell = makePopupMenuItem(LABEL_SHOW_SHELL_NBRS, icon_nbh_sel_mode, popup);
				pmShowSecShell = makePopupMenuItem(LABEL_SHOW_SEC_SHELL, icon_nbh_sel_mode, popup);
			}
		}
		if(Start.USE_CGAP && Start.getCgapSphoxelFile() != null){
			popup.addSeparator();
			pmShowSphoxel = makePopupMenuItem(LABEL_SHOW_SPHOXEL, icon_sphoxel_traces, popup);	
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
		mmLoupe = makeMenuItem(LABEL_LOUPE, null, menu);
		// Load
		submenu = new JMenu("Load from");
		popupMenu2Parent.put(submenu.getPopupMenu(),submenu);
		if(Start.USE_DATABASE && Start.USE_EXPERIMENTAL_FEATURES) {
			mmLoadGraph = makeMenuItem(LABEL_GRAPH_DB,null,submenu);
			mmLoadPdbase = makeMenuItem(LABEL_PDBASE,null,submenu);
		}		
		mmLoadFtp = makeMenuItem(LABEL_ONLINE_PDB, null, submenu);
		mmLoadPdb = makeMenuItem(LABEL_PDB_FILE, null, submenu);
		mmLoadTs = makeMenuItem(LABEL_TS_FILE, null, submenu);
		mmLoadCaspRR = makeMenuItem(LABEL_CASP_RR_FILE, null, submenu);
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			mmLoadSeq = makeMenuItem(LABEL_LOAD_SEQ, null, submenu);
			mmLoadCaspServerMods = makeMenuItem(LABEL_CASP_SERVER_MOD, null, submenu);
		}
		mmLoadCm = makeMenuItem(LABEL_CONTACT_MAP_FILE, null, submenu);
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
		submenu.addSeparator();
		mmSelModeAddRemove = makeMenuItem(LABEL_ADD_REMOVE_CONTACTS, icon_toggle_contacts, submenu);
		
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
			sphereM = makeMenuItem(LABEL_SHOW_SPHERES_3D, icon_show_sph_3d, menu);
			if(Start.USE_EXPERIMENTAL_FEATURES) {
				triangleM = makeMenuItem(LABEL_SHOW_TRIANGLES_3D, icon_show_triangles_3d, menu);
			}
			menu.addSeparator();
		}		
		delEdgesM = makeMenuItem(LABEL_DELETE_CONTACTS, icon_del_contacts, menu);
		minSubsetM = makeMenuItem(LABEL_MIN_SET, icon_min_set, menu);
		if(Start.USE_EXPERIMENTAL_FEATURES) {		
			mmRunTinker = makeMenuItem(LABEL_RUN_TINKER,icon_run_tinker, menu);
			mmJPred = makeMenuItem(LABEL_JPRED,null,menu);
		}
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
		}		
		
		mmLoadFtp2 = makeMenuItem(LABEL_ONLINE_PDB, null, submenu);
		mmLoadPdb2 = makeMenuItem(LABEL_PDB_FILE, null, submenu);
		mmLoadTs2 = makeMenuItem(LABEL_TS_FILE, null, submenu);		
		mmLoadCaspRR2 = makeMenuItem(LABEL_CASP_RR_FILE, null, submenu);
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			mmLoadCaspServerMods2 = makeMenuItem(LABEL_CASP_SERVER_MOD, null, submenu);
		}
		mmLoadCm2 = makeMenuItem(LABEL_CONTACT_MAP_FILE, null, submenu);
		menu.addSeparator();
		mmShowCommon = makeMenuItem(LABEL_SHOW_COMMON, icon_selected, menu);
		mmShowFirst = makeMenuItem(LABEL_SHOW_FIRST, icon_selected, menu);
		mmShowSecond = makeMenuItem(LABEL_SHOW_SECOND, icon_selected, menu);		
		menu.addSeparator();
		mmToggleDiffDistMap = makeMenuItem(LABEL_SHOW_DIFF, icon_deselected, menu);
		menu.addSeparator();
		mmSuperposition = makeMenuItem(LABEL_SUPER_SEL,null,menu);
		mmSuperposition.setEnabled(false);
		mmShowAlignedResidues = makeMenuItem(LABEL_SHOW_ALI,null,menu);
		mmShowAlignedResidues.setEnabled(false);
		if(Start.USE_EXPERIMENTAL_FEATURES) {
			menu.addSeparator();
			mmCopyContacts = makeMenuItem(LABEL_COPY_CONTACTS,null,menu);
			mmCopyContacts.setEnabled(false);
			mmSwapModels = makeMenuItem(LABEL_SWAP_MODELS,null,menu);
			mmSwapModels.setEnabled(false);
		}
		addToJMenuBar(menu);

		// Help menu
		menu = new JMenu("Help");
		menu.setMnemonic(KeyEvent.VK_H);	
		mmHelpHelp = makeHelpMenuItem("Help", null, menu);
		
		mmHelpWriteConfig = makeMenuItem("Write Example Configuration File", null, null);		// function disabled
		mmHelpAbout = makeMenuItem("About", null, menu);
		addToJMenuBar(menu);


		// Creating contact map pane if model loaded
		if(mod != null) {
			
			String[] tags = {mod.getLoadedGraphID()};
			String[] seqs = {mod.getSequence()};
			MultipleSequenceAlignment al = null;
			try {
				al = new MultipleSequenceAlignment(tags,seqs);
			} catch(AlignmentConstructionException e) {
				//should be safe to ignore the error because it shouldn't happen, if it does we print anyway an error
				System.err.println("Unexpected error, something wrong in alignment construction: "+e.getMessage());
			}
			cmPane = new ContactMapPane(mod, al , this);
			cmPane.setStatusBar(statusBar);
			cmPane.setDeltaRankBar(deltaRankBar);
			cmp.add(cmPane, BorderLayout.CENTER);
			topRuler = new ResidueRuler(cmPane,mod,this,ResidueRuler.TOP);
			leftRuler = new ResidueRuler(cmPane,mod,this,ResidueRuler.LEFT);
			topRul.add(topRuler);
			leftRul.add(leftRuler);
		}

		// Add everything to the content pane		
		this.tbPane.add(toolBar, BorderLayout.NORTH);			// tbPane is necessary if toolBar is floatable
		
		this.tbPane.add(cmp,BorderLayout.CENTER);				// otherwise can add these to contentPane directly
		this.tbPane.add(statusBar,BorderLayout.EAST);
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

		pack();
		
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
	 * @see setAccessibility(Map, boolean, Component, Collection)
	 */
	public void setAccessibility(Component comp, boolean visible, boolean parentCheck, Component topLevelComponent, Collection<Class<?>> disregardedTypes) {


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
	 * {@link setAccessibility(Map, boolean, Component, Collection)}.
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
		map.put(mmLoupe,hasMod);
		// menu -> View
		map.put(mmViewShowPdbResSers, hasMod);
		map.put(mmViewHighlightComNbh, hasMod);
		map.put(mmViewShowDensity, hasMod);
		map.put(mmViewShowDeltaRank, hasMod);
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
		map.put(mmSelModeAddRemove, hasMod);
		map.put(sendM, hasMod);
		map.put(sphereM, hasMod);		
		map.put(triangleM, hasMod); 
		map.put(delEdgesM, hasMod);
		map.put(minSubsetM, hasMod);
		map.put(mmJPred, hasMod);
		map.put(mmRunTinker, hasMod);
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

		map.put(tbFileInfo, hasMod);

		map.put(tbSquareSel, hasMod);
		map.put(tbFillSel, hasMod);
		map.put(tbDiagSel, hasMod);
		map.put(tbNbhSel, hasMod);
		map.put(tbSelModeColor, hasMod);		
		map.put(tbToggleContacts, hasMod);
		map.put(tbShowComNbh, hasMod);		// experimental
		map.put(tbShowComNbh3D, hasMod);	// experimental

		map.put(tbShowSel3D, hasMod);
		map.put(tbShowSph3D, hasMod);

		map.put(tbDelete, hasMod);
		map.put(tbMinSubset, hasMod);
		map.put(tbRunTinker, hasMod); 		// experimental
		

		
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
		map.put(sphereP, false);
		map.put(popupSendEdge, false);
		map.put(delEdgesP, false);
		map.put(pmShowShell, false);
		map.put(pmShowSecShell, false);
		map.put(pmShowSphoxel, false);	
		
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
		map.put(mmLoupe,false);		
		// menu -> View
		map.put(mmViewShowPdbResSers,true);
		map.put(mmViewHighlightComNbh,false);
		map.put(mmViewShowDensity,false);
		map.put(mmViewShowDeltaRank,false);
		map.put(mmViewShowDistMatrix,false);
		// menu -> Select
		map.put(mmSelectByResNum,false);
		map.put(mmSelectHelixHelix,false);
		map.put(mmSelectBetaBeta,false);
		map.put(mmSelectInterSsContacts,false);
		map.put(mmSelectIntraSsContacts,false);
		map.put(mmSelModeAddRemove,false);
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
		map.put(minSubsetM, false);
		map.put(mmJPred, false);
		map.put(mmRunTinker, false);
		// menu -> Compare
		map.put(mmShowCommon,true);
		map.put(mmShowFirst,true);
		map.put(mmShowSecond,true);
		map.put(mmToggleDiffDistMap,true);
		map.put(smCompare.get("Load"),true); // now allowing loading of a new second contact map
		map.put(mmSwapModels,true);
		map.put(mmCopyContacts,true);

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
		map.put(tbMinSubset, false);
		map.put(tbToggleContacts, false);
		map.put(tbRunTinker, false);

		map.put(tbShowCommon, true);
		map.put(tbShowFirst, true);
		map.put(tbShowSecond, true);

		// the show common/first/second buttons are a special case: 
		// they've been initialized invisible, we need also to make them visible
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
		if(e.getSource() == mmLoadPdb) {
			handleLoadFromPdbFile(FIRST_MODEL, PDB_FILE);
		}
		if(e.getSource() == mmLoadTs) {
			handleLoadFromPdbFile(FIRST_MODEL, TS_FILE);
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
		if(e.getSource() == mmLoadSeq) {
			handleLoadFromSequence(FIRST_MODEL);
		}
		if(e.getSource() == mmLoadCaspServerMods) {
			handleLoadFromCaspServerMods(FIRST_MODEL);
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

		// Info, Print, Quit, Loupe
		if(e.getSource() == mmInfo || e.getSource() == tbFileInfo) {
			handleInfo();
		}		  		  
		if(e.getSource() == mmPrint) {
			handlePrint();
		}		  
		if(e.getSource() == mmQuit) {
			handleQuit();
		}
		if(e.getSource() == mmLoupe || e.getSource() == tbLoupe) {
			handleLoupe();
		}		

		/* ---------- View Menu ---------- */		

		if(e.getSource() == mmViewShowPdbResSers) {
			handleShowPdbResSers();
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
		// node neighbourhood selection button clicked 
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
		
		// toggle contacts selection mode button clicked
		if (e.getSource() == tbToggleContacts || e.getSource() == mmSelModeAddRemove) {
			showAddDeleteContacts();
			guiState.setSelectionMode(GUIState.SelMode.TOGGLE);
		}
		
		// Start Tinker Run
		if (e.getSource() == tbRunTinker || e.getSource() == mmRunTinker) {
			handleRunTinker();
		}
		
		// Actions

		// send selection button clicked
		if (e.getSource() == sendM || e.getSource() == sendP || e.getSource() == tbShowSel3D) {	
			handleShowSelContacts3D();
		}
		// show spheres button clicked
		if (e.getSource() == sphereM || e.getSource() == sphereP || e.getSource() == tbShowSph3D) {	
			handleShowSpheres3D(e.getSource() == sphereP);
		}
		// send com.Nei. button clicked
		if(e.getSource()== triangleM || e.getSource()== triangleP || e.getSource() == tbShowComNbh3D) {
			handleShowTriangles3D();
		}		
		// delete selected edges button clicked
		if (e.getSource() == delEdgesM || e.getSource() == delEdgesP || e.getSource() == tbDelete) {
			handleDeleteSelContacts();
		}
		// show nbd-relations button clicked
		if(e.getSource()== pmShowShell) {
			handleShowShellRels();
		}
		// show 2nd shell button clicked
		if(e.getSource()== pmShowSecShell) {
			handleShowSecShell();
		}
		// show sphoxel and neighbourhood-traces
		if(e.getSource()== pmShowSphoxel) {
			handleShowSphoxel();
		}
		// send current edge (only available in popup menu)
		if(e.getSource() == popupSendEdge) {
			handleShowDistance3D();
		}
		// Predict secondary structure
		if(e.getSource() == mmJPred) {
			handleJPred();
		}

		/* ---------- Comparison Menu ---------- */


		/** for the contact map comparison load menu */


		if(e.getSource() == mmLoadGraph2) {
			handleLoadFromGraphDb(SECOND_MODEL);
		}
		if(e.getSource() == mmLoadPdbase2) {
			handleLoadFromPdbase(SECOND_MODEL);
		}		  
		if(e.getSource() == mmLoadPdb2) {
			handleLoadFromPdbFile(SECOND_MODEL, PDB_FILE);
		}
		if(e.getSource() == mmLoadTs2) {
			handleLoadFromPdbFile(SECOND_MODEL, TS_FILE);
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
		if(e.getSource() == mmLoadCaspServerMods2) {
			handleLoadFromCaspServerMods(SECOND_MODEL);
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
			handleShowDiffDistMapFromMenu(); // true=show in bottom left half
		}

		if( e.getSource() == mmSuperposition ) {
			handleSuperposition3D(); 
		}

		if( e.getSource() == mmShowAlignedResidues ) {
			handleShowAlignedResidues3D();
		}
		if (e.getSource() == mmSwapModels) {
			handleSwapModels();
		}
		if (e.getSource() == mmCopyContacts) {
			handleCopyContacts();
		}		
		if(e.getSource() == tbMinSubset || e.getSource() == minSubsetM){
			handleMinimalSet();
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
						public void doit(Object o, String f, String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, int gid, String seq) {
							View view = (View) o;
							view.doLoadFromGraphDb(db, gid, secondModel);
						}
					}, null, null, null, null, null, null, null, null, null, Start.DEFAULT_GRAPH_DB, "", null);
					
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
						public void doit(Object o, String f, String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, int gid, String seq) {
							View view = (View) o;
							view.doLoadFromPdbase(ac, modelSerial, loadAllModels, cc, ct, dist, minss, maxss, db, secondModel);
						}
					}, null, "", "1", "", "", Start.DEFAULT_CONTACT_TYPE, String.valueOf(Start.DEFAULT_DISTANCE_CUTOFF), "", "", Start.DEFAULT_PDB_DB, null, null);
					dialog.setChainCodeGetter(new Getter(dialog) {
						public Object get() throws GetterError {
							LoadDialog dialog = (LoadDialog) getObject();
							String pdbCode    = dialog.getSelectedAc();
							String db         = dialog.getSelectedDb();
							try {
								PdbaseModel mod = new PdbaseModel(pdbCode,"",0.0,1,1,db);
								return mod.getChains();
							} catch (PdbCodeNotFoundException e) {
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
							} catch (PdbCodeNotFoundException e) {
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

	public void doLoadFromPdbase(String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, boolean secondModel) {
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
			PdbaseModel mod = new PdbaseModel(ac, ct, dist, minss, maxss, db);
			mod.load(cc, modelSerial, loadAllModels);
			if(secondModel) {
				//handleLoadSecondModel(mod);
				mod2 = mod;
				handlePairwiseAlignment();
			} else {
				this.spawnNewViewWindow(mod);
			}
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		} catch (PdbCodeNotFoundException e) {	
			showLoadError(e.getMessage());
		} catch (SQLException e) {
			showLoadError(e.getMessage());
		} catch (PdbLoadError e) {
			showLoadError(e.getMessage());
		}
	}

	private void handleLoadFromPdbFile(boolean secondModel, boolean tsFile) {

		if (secondModel == SECOND_MODEL && mod == null){
			this.showNoContactMapWarning();
		} else{
			try {
				String title = tsFile?"Load from Casp TS file":"Load from PDB file";
				LoadDialog dialog = new LoadDialog(this, title, new LoadAction(secondModel) {
					public void doit(Object o, String f, String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, int gid, String seq) {
						View view = (View) o;
						view.doLoadFromPdbFile(f, modelSerial, loadAllModels, cc, ct, dist, minss, maxss, secondModel);
					}
				}, "", null, "1", "", "", Start.DEFAULT_CONTACT_TYPE, String.valueOf(Start.DEFAULT_DISTANCE_CUTOFF), "", "", null, null, null);
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

	public void doLoadFromPdbFile(String f, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, boolean secondModel) {
		System.out.println("Loading from Pdb file");
		System.out.println("Filename:\t" + f);
		System.out.println("Model serial:\t" + modelSerial);
		System.out.println("Chain code:\t" + cc);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + (minss==-1?"none":minss));
		System.out.println("Max. Seq. Sep.:\t" + (maxss==-1?"none":maxss));
		try {
			PdbFileModel mod = new PdbFileModel(f, ct, dist, minss, maxss);
			try {
				mod.load(cc, modelSerial, loadAllModels);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
	
	public void doLoadSecondModelFromModel(Model m) {
		mod2 = m;
		try {
			doPairwiseSequenceAlignment();
		} catch (AlignmentConstructionException e) {
			showLoadError(e.getMessage());
		}
		updateTitle();
	}
	
	public void doLoadSecondModelFromPdbFile(String string) {
		
		try {
			Model newmod = new PdbFileModel(string,mod.edgeType,mod.distCutoff,mod.minSeqSep,mod.maxSeqSep);
			newmod.load("NULL", 1);
			mod2 = newmod;
			doPairwiseSequenceAlignment();
			
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		} catch (AlignmentConstructionException e) {
			showLoadError(e.getMessage());
		}
		updateTitle();
	}
	
	private void handleLoadFromCmFile(boolean secondModel) {

		if (secondModel == SECOND_MODEL && mod == null){
			this.showNoContactMapWarning();
		} else{
			try {
				LoadDialog dialog = new LoadDialog(this, "Load from CM file", new LoadAction(secondModel) {
					public void doit(Object o, String f, String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, int gid, String seq) {
						View view = (View) o;
						view.doLoadFromCmFile(f, secondModel);
					}
				}, "", null, null, null, null, null, null, null, null, null, null, null);
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
	
	/**
	 * This method is being called when 'Load from sequence' was chosen in the menu.
	 * @param secondModel whether the function was invoked from the 'Load second contact map' menu.
	 */
	private void handleLoadFromSequence(boolean secondModel) {
		if (secondModel == SECOND_MODEL && mod == null){
			this.showNoContactMapWarning();
		} else{
			try {
				LoadDialog dialog = new LoadDialog(this, "Load from sequence", new LoadAction(secondModel) {
					public void doit(Object o, String f, String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, int gid, String seq) {
						actLoadDialog.dispose();
						View view = (View) o;
						view.doLoadFromSequence(f, seq, secondModel);
					}
				}, "", null, null, null, null, null, null, null, null, null, null, "");
				actLoadDialog = dialog;
				dialog.showIt();
			} catch (LoadDialogConstructionError e) {
				System.err.println("Failed to load the load-dialog.");
			}			
		}
	}
	
	/**
	 * Actually loads a new contact map given a sequence. The contact map will contain
	 * no contacts. If seq is empty, the file name is used, otherwise seq.
	 * @param f fasta file name from which to load sequence
	 * @param seq actual sequence to load
	 * @param secondModel whether this is called to create a second model
	 */
	private void doLoadFromSequence(String f, String seq, boolean secondModel) {
		Model mod;
		try {
			
			// load new model
			if(seq.length() > 0) {
				System.out.println("Loading from sequence "+seq);
				mod = new SequenceModel(seq);
			} else {
				System.out.println("Loading from sequence file "+f);
				mod = new SequenceModel(new File(f));
			}
			
			String msg = "\nDo you want to assign predicted secondary structure using JPred?\n\n" +
					     "(This requires an active internet connection and may take some\n" +
					     " time to connect to the JPred server and retrieve the result)";
			String title = "Secondary Structure";
			if(JOptionPane.showConfirmDialog(this,msg,title,JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				
				// show JPred dialog and run secondary structure prediction
				JPredDialog jpredDialog = new JPredDialog(this);
				SecondaryStructure result = null;
				jpredDialog.runJPred(mod.getSequence()); // start JPred connection in thread
				jpredDialog.showGui();	// this will block until dialog is disposed
				result = jpredDialog.getResult();
				if(result != null) mod.setSecondaryStructure(result);
			}
			
			// apply new model
			if(secondModel == SECOND_MODEL) {
				//handleLoadSecondModel(mod);
				mod2 = mod;
				handlePairwiseAlignment();
			} else {
				this.spawnNewViewWindow(mod);
			}		
		} catch (ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}
	
	private void handleLoadFromCaspServerMods(boolean secondModel) {
		if (secondModel == SECOND_MODEL && mod == null){
			this.showNoContactMapWarning();
		} else{
			try {
				Start.getFileChooser().setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				LoadDialog dialog = new LoadDialog(this, "Load Casp Server Models", new LoadAction(secondModel) {
					public void doit(Object o, String f, String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, int gid, String seq) {
						actLoadDialog.dispose();
						View view = (View) o;
						view.doLoadFromCaspServerModels(f, ct, dist, minss, maxss, secondModel);
					}
				}, "", null, null, null, null, Start.DEFAULT_CONTACT_TYPE, String.valueOf(Start.DEFAULT_DISTANCE_CUTOFF), "", "", null, null, null);
				actLoadDialog = dialog;
				dialog.showIt();
			} catch (LoadDialogConstructionError e) {
				System.err.println("Failed to load the load-dialog.");
			} finally {
				Start.getFileChooser().setFileSelectionMode(JFileChooser.FILES_ONLY);
			}
		}
	}
	
	public void doLoadFromCaspServerModels(String f, String ct, double dist, int minss, int maxss, boolean secondModel) {
		boolean firstModOnly = true;
		double consSSThresh = 0.5;

		String msg = "\nDo you want to assign consensus secondary structure from the server models?\n" +
	     "(This requires DSSP to be properly set up in your cmview.cfg)\n\n";
		String title = "Secondary Structure";
		
		if(JOptionPane.showConfirmDialog(this,msg,title,JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
			consSSThresh = 0.0;
		}
		
		System.out.println("Loading Casp Server Models");
		System.out.println("Directory:\t" + f);
		System.out.println("Contact type:\t" + ct);
		System.out.println("Dist. cutoff:\t" + dist);	
		System.out.println("Min. Seq. Sep.:\t" + (minss==-1?"none":minss));
		System.out.println("Max. Seq. Sep.:\t" + (maxss==-1?"none":maxss));
		System.out.println("Load only first models:\t" + (firstModOnly?"yes":"no"));
		System.out.println("Consensus secondary structure threshold:\t" + (consSSThresh > 0?consSSThresh:"none"));		
		
		try {
			Model mod = new CaspServerPredictionsModel(new File(f), ct, dist, minss, maxss, firstModOnly,consSSThresh);
			if(secondModel == SECOND_MODEL) {
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
					public void doit(Object o, String f, String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, int gid, String seq) {
						View view = (View) o;
						view.doLoadFromCaspRRFile(f, secondModel);
					}
				}, "", null, null, null, null, null, null, null, null, null, null, null);
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
					public void doit(Object o, String f, String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, int gid, String seq) {
						View view = (View) o;
						view.doLoadFromFtp(ac, modelSerial, loadAllModels, cc, ct, dist, minss, maxss, secondModel);
					}
				}, null, "", "1", "", "", Start.DEFAULT_CONTACT_TYPE, String.valueOf(Start.DEFAULT_DISTANCE_CUTOFF), "", "", null, null, null);
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

	public void doLoadFromFtp(String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, boolean secondModel) {
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
			PdbFtpModel mod = null;
			if( localFile != null ) {
				mod = new PdbFtpModel(localFile, ct, dist, minss, maxss);
			} else { 
				mod = new PdbFtpModel(ac, ct, dist, minss, maxss);
			}
			mod.load(cc, modelSerial, loadAllModels);
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

		Object[] possibilitiesWithDali = {"compute Needleman-Wunsch sequence alignment", "compute SADP structural alignment", "load alignment from FASTA file","compute DALI structural alignment"};
		Object[] possibilitiesWithoutDali = {"compute Needleman-Wunsch sequence alignment", "compute SADP structural alignment", "load alignment from FASTA file"};
		String source;
		if (Start.dali_found) {
			source = (String) JOptionPane.showInputDialog(this, "Chose alignment source ...", "Pairwise Protein Alignment", JOptionPane.PLAIN_MESSAGE, null, possibilitiesWithDali, possibilitiesWithDali[0]);
		} else {
			source = (String) JOptionPane.showInputDialog(this, "Chose alignment source ...", "Pairwise Protein Alignment", JOptionPane.PLAIN_MESSAGE, null, possibilitiesWithoutDali, possibilitiesWithoutDali[0]);
			
		}
		if( source != null ) {
			try {
				if( source == possibilitiesWithDali[0] ) {
					// do a greedy residue-residue alignment
					doPairwiseSequenceAlignment();
				} else if( source == possibilitiesWithDali[1] ) {
					// compute contact map alignment using SADP
					doPairwiseSadpAlignment();
				} else if( source == possibilitiesWithDali[3]) {
					doDALIAlignment();
				} else if( source == possibilitiesWithDali[2] ) {
					// load a user provided alignment from an external source
					doLoadPairwiseAlignment(MultipleSequenceAlignment.FASTAFORMAT);
				//} else if( source == possibilities[4]) {
				//	doLoadPairwiseAlignment(Alignment.DALIFORMAT);
//					} else if( source == possibilities[3] ) {
//					// do a greedy residue-residue alignment
//					doGreedyPairwiseAlignment();	
				} else {
					System.err.println("Error: Detected unhandled input option for the alignment retrieval!");
					return;
				}
			} catch (AlignmentConstructionException e) {
				error = e.getMessage();
			} catch (FileNotFoundException e) {
				error = e.getMessage();
			} catch (FileFormatError e) {
				error = e.getMessage();
			} catch (IOException e) {
				error = e.getMessage();
			}
			finally{
				if(error != null) {
					setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
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
		statusBar.enableDifferenceMapOverlay();
	}
	
	public void doLoadPairwiseAlignment(String format) throws IOException, FileFormatError, AlignmentConstructionException {
		
		// open global file-chooser and get the name the alignment file
		JFileChooser fileChooser = Start.getFileChooser();
		int ret = fileChooser.showOpenDialog(this);
		File source = null;
		if(ret == JFileChooser.APPROVE_OPTION) {
			source = fileChooser.getSelectedFile();
		} else {
			return;
		}
		
		doLoadPairwiseAlignment(format, source.getPath());
	}
	
	/**
	 * Loads the pairwise alignment for the given model from an external source.
	 */
	
	public void doLoadPairwiseAlignment(String format, String source)
	throws IOException, FileFormatError, 
	AlignmentConstructionException {

		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
		
		// load alignment
		ali = new MultipleSequenceAlignment(source,format);

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
				// we try to align the 2 mismatching sequences (from the file and the loaded contact map), 
				// so that at least the user gets hopefully a better feeling of what's wrong
				PairwiseSequenceAlignment psa = new PairwiseSequenceAlignment(ali.getSequenceNoGaps(name1),mod.getSequence(),"from file","from CMView");
				System.err.println("This is an alignment of the non-matching sequences: ");
				psa.printAlignment();

			} catch (PairwiseSequenceAlignmentException e) {
				// pairwise alignment didn't succeed, we do nothing 
			}
			throw new AlignmentConstructionException("First sequence from given alignment and sequence of first loaded contact map differ!");
		}
		if (!mod2.getSequence().equals(ali.getSequenceNoGaps(name2))) {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			System.err.println("Second sequence in given alignment file and sequence of contact map "+name2+" differ");
			try {
				// we try to align the 2 mismatching sequences (from the file and the loaded contact map), 
				// so that at least the user gets hopefully a better feeling of what's wrong
				PairwiseSequenceAlignment psa = new PairwiseSequenceAlignment(ali.getSequenceNoGaps(name2),mod2.getSequence(),"from file","from CMView");
				System.err.println("This is an alignment of the non-matching sequences: ");
				psa.printAlignment();
			} catch (PairwiseSequenceAlignmentException e) {
				// pairwise alignment didn't succeed, we do nothing 
			}
			throw new AlignmentConstructionException("Second sequence from given alignment and sequence of second loaded contact map differ!");
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
	 * Performs a DALI structural alignment. 
	 * Unfortunately, DALI only produces html output. We create a temporary folder, write/copy the
	 * PDBs there, perform the alignment, parse the html output and delete everything.
	 * @throws IOException
	 * @throws AlignmentConstructionException 
	 * @throws FileFormatError 
	 */
	
	private void doDALIAlignment() throws IOException, FileFormatError, AlignmentConstructionException {
		
		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
		
		try {
			DaliRunner dali = new DaliRunner(mod.getPdb(), mod2.getPdb(),Start.DALI_EXECUTABLE,Start.TEMP_DIR);
			doLoadPairwiseAlignment(MultipleSequenceAlignment.CLUSTALFORMAT,dali.getClustalFile());
		} catch (InterruptedException e) {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			throw new AlignmentConstructionException("Could not construct alignment: Execution Interrupted");
		}
		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
		
	}
	
	
	/**
	 * Constructs a pairwise sequence alignment using the Needleman-Wunsch algorithm with default parameters.
	 * @throws AlignmentConstructionException
	 */
	public void doPairwiseSequenceAlignment() throws AlignmentConstructionException {
		
		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
		
		String seq1 = mod.getSequence();
		String seq2 = mod2.getSequence();

		if(seq1 == null || seq2 == null || seq1.length() == 0 || seq2.length() == 0) {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			throw new AlignmentConstructionException("No sequence found.");
		}

		String name1 = mod.getLoadedGraphID();
		String name2 = mod2.getLoadedGraphID();

		PairwiseSequenceAlignment jalign = null;
		try {
			jalign = new PairwiseSequenceAlignment(seq1, seq2, name1, name2);
		} catch (PairwiseSequenceAlignmentException e) {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			throw new AlignmentConstructionException("Error during alignment: " + e.getMessage());
		}
		jalign.printAlignment();
		String[] alignedSeqs = jalign.getAlignedSequences();
		String alignedSeq1 = alignedSeqs[0];
		String alignedSeq2 = alignedSeqs[1];

		// create alignement
		String[] names = {name1, name2};
		String[] seqs = {alignedSeq1, alignedSeq2};
		ali = new MultipleSequenceAlignment(names, seqs);
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
	 * @throws AlignmentConstructionException
	 */
	public void doGreedyPairwiseAlignment() 
	throws AlignmentConstructionException, DifferentContactMapSizeError {

		String alignedSeq1 = mod.getSequence();
		String alignedSeq2 = mod2.getSequence();
		int len1,len2,cap;
		StringBuffer s = null;
		char gap = MultipleSequenceAlignment.getGapCharacter();

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
		ali = new MultipleSequenceAlignment(names, seqs);

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
	private void doLoadSecondModel(Model mod2, MultipleSequenceAlignment ali) {
		
		// this has to come first, as it forces the background to be redrawn, which triggers the error it is supposed to avoid if the second
		// model is already loaded
		
		disableAllBackgroundMaps();
		// TODO: (because it is not clear what the following would refer to)
		// disable background overlay menu (except diff dist map)
		// disable secondary structure view
		
		// re-setting window title
		System.out.println("Second contact map loaded.");
		
		// disable residue rulers
		this.cmp.remove(leftRul);
		this.cmp.remove(topRul);
		
		updateTitle();
//		String title = "Comparing " + mod.getLoadedGraphID() +" and "+mod2.getLoadedGraphID();
//		this.setTitle(title);

		// add the second model and update the image buffer
		cmPane.setSecondModel(mod2, ali);
		guiState.setCompareMode(true);

		cmPane.updateScreenBuffer();
		guiState.setSelectionMode(GUIState.INITIAL_SEL_MODE);	// we reset to SQUARE_SEL in case another one (possibly not allowed in compare mode) was switched on
		// finally repaint the whole thing to display the whole set of contacts
		cmPane.repaint();
		statusBar.getCoordinatesPanel().setCompareMode(true);
		statusBar.reInitialize();		// Trying to fix bug that coordinatesPanel does not resize - not yet successful
		
		if (Start.isPyMolConnectionAvailable() && mod2.has3DCoordinates()) {
			// load structure in the 3D visualizer
			Start.getPyMolAdaptor().loadStructure(mod2.getTempPdbFileName(), mod2.getLoadedGraphID(), true);

			if (mod.has3DCoordinates()) {
				// clear the view (disables all previous selections)
				Start.getPyMolAdaptor().showStructureHideOthers(mod.getLoadedGraphID(), mod2.getLoadedGraphID());
				
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

	private void disableAllBackgroundMaps() {
	
		// update GUI
		mmToggleDiffDistMap.setIcon(icon_deselected);
		statusBar.resetOverlayStatus(true);
		statusBar.resetOverlayStatus(false);		
		
		if (guiState.getShowDeltaRankMap()) {
			handleShowDeltaRankMap(false);
		}
		if(guiState.getShowBottomDeltaRankMap()) {
			handleShowDeltaRankMap(true);
		}
		if(guiState.getShowDensityMap()) {
			handleShowDensityMap(false);
		}
		if(guiState.getShowBottomDensityMap()) {
			handleShowDensityMap(true);
		}
		if(guiState.getShowDistanceMap()) {
			handleShowDistanceMap(false);
		}
		if(guiState.getShowBottomDistanceMap()) {
			handleShowDistanceMap(true);
		}
		if(guiState.getShowNbhSizeMap()) {
			handleShowNbhSizeMap(false);
		}
		if(guiState.getShowBottomNbhSizeMap()) {
			handleShowNbhSizeMap(true);
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
			Start.getPyMolAdaptor().showStructureHideOthers(mod.getLoadedGraphID(), mod2.getLoadedGraphID());	
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
	public void doSuperposition3D(Model mod1, Model mod2, MultipleSequenceAlignment ali, TreeSet<Integer> columns) {
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
	public void doShowAlignedResidues3D(Model mod1, Model mod2, MultipleSequenceAlignment ali, TreeSet<Integer> columns) {
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
			pymol.showStructureHideOthers(mod1.getLoadedGraphID(), mod2.getLoadedGraphID());
		} else {
			// nothing to do!
			return;
		}

		// send alignment edges to PyMol
		pymol.showMatchingResidues(mod1, mod2, residuePairs);
		
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

	public void doSaveAlignment(MultipleSequenceAlignment ali) {
		if(ali == null) {
			//System.out.println("No contact map loaded yet.");
			showCannotSaveEmptyAlignment();
		} else {
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();
				if (confirmOverwrite(chosenFile)) {
					try {
						ali.writeFasta(new PrintStream(chosenFile), 80, true);
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
	
	private void handleLoupe() {
		if(this.mod == null) {
			//System.out.println("No contact map loaded yet.");
			showNoContactMapWarning();
		} else {
			loupeWindow.setVisible(!loupeWindow.isVisible());
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
	private void handleShowNbhSizeMap(boolean secondView) {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			if (secondView) {
				guiState.setShowBottomNbhSizeMap(!guiState.getShowBottomNbhSizeMap());
				cmPane.toggleNbhSizeMap(guiState.getShowBottomNbhSizeMap());
			} else {
				guiState.setShowNbhSizeMap(!guiState.getShowNbhSizeMap());
				cmPane.toggleNbhSizeMap(guiState.getShowNbhSizeMap());
			}

	
		}
	}

	/**
	 * 
	 */
	private void handleShowDensityMap(boolean secondView) {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			if (secondView) {
				guiState.setShowBottomDensityMap(!guiState.getShowBottomDensityMap());
				cmPane.toggleDensityMap(guiState.getShowBottomDensityMap());
			} else {
				guiState.setShowDensityMap(!guiState.getShowDensityMap());
				cmPane.toggleDensityMap(guiState.getShowDensityMap());
			}

			
		}
	}

	/**
	 * @throws InterruptedException 
	 * 
	 */
	private void handleShowDeltaRankMap(boolean secondView){
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			if (secondView) {
				guiState.setShowBottomDeltaRankMap(!guiState.getShowBottomDeltaRankMap());
				cmPane.toggleDeltaRankMap(guiState.getShowBottomDeltaRankMap());
			} else {
				guiState.setShowDeltaRankMap(!guiState.getShowDeltaRankMap());
				cmPane.toggleDeltaRankMap(guiState.getShowDeltaRankMap());
			}
			if(guiState.getShowBottomDeltaRankMap() || guiState.getShowDeltaRankMap()) {
				deltaRankBar.setActive(true);
				this.tbPane.add(deltaRankBar,BorderLayout.SOUTH);
				this.cmPane.revalidate();
			} else {
				deltaRankBar.setActive(false);
				this.tbPane.remove(deltaRankBar);
				this.cmPane.revalidate();
				
			}
			
		}
	}
	
	@SuppressWarnings("unused")
	private void handleShowResidueScoringMap(boolean secondView) {
		if(mod == null) {
			showNoContactMapWarning();
			return;
		}
		
		if(secondView) {
			guiState.setShowBottomResidueScoringMap(!guiState.getShowBottomResidueScoringMap());
			if (guiState.getShowBottomResidueScoringMap()) {
				cmPane.getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
				String fn = guiState.getResidueScoringFunctionName(secondView);
				ResidueContactScoringFunction f = statusBar.getScoringFunctionWithName(fn);
				f.init(null, mod.getGraph(), mod.getSecondaryStructure(), mod.getPdb(), Start.getDbConnection());
				cmPane.getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			}
		} else {
			guiState.setShowResidueScoringMap(!guiState.getShowResidueScoringMap());
			if (guiState.getShowResidueScoringMap()) {
				cmPane.getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
				String fn = guiState.getResidueScoringFunctionName(secondView);
				ResidueContactScoringFunction f = statusBar.getScoringFunctionWithName(fn);
				f.init(null, mod.getGraph(), mod.getSecondaryStructure(), mod.getPdb(), Start.getDbConnection());
				cmPane.getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			}
		}
		
		
		cmPane.revalidate();
		cmPane.updateScreenBuffer();
	}
	
	private void updateScoringFunctions() {
		if (guiState.getShowResidueScoringMap() || guiState.getShowBottomResidueScoringMap()) {
			if (guiState.getShowResidueScoringMap()) {
				cmPane.getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
				String fn = guiState.getResidueScoringFunctionName(false);
				ResidueContactScoringFunction f = statusBar.getScoringFunctionWithName(fn);
				f.updateData(null,mod.getGraph(), mod.getSecondaryStructure(), mod.getPdb());
				cmPane.getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			}
			if (guiState.getShowBottomResidueScoringMap()) {
				cmPane.getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
				String fn = guiState.getResidueScoringFunctionName(true);
				ResidueContactScoringFunction f = statusBar.getScoringFunctionWithName(fn);
				f.updateData(null,mod.getGraph(), mod.getSecondaryStructure(), mod.getPdb());
				cmPane.getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			}
			cmPane.revalidate();
			cmPane.updateScreenBuffer();
		}
	}
	
	/**
	 * 
	 */
	private void handleShowDistanceMap(boolean secondView) {
		if(mod==null) {
			showNoContactMapWarning();
		} else if (!mod.has3DCoordinates()){
			showNo3DCoordsWarning(mod);
		} else {
			if (secondView) {
				guiState.setShowBottomDistanceMap(!guiState.getShowBottomDistanceMap());
				cmPane.toggleDistanceMap(guiState.getShowBottomDistanceMap());
			} else {
				guiState.setShowDistanceMap(!guiState.getShowDistanceMap());
				cmPane.toggleDistanceMap(guiState.getShowDistanceMap());
			}
		}
	}
	
	/**
	 * Handles the show/hide diff dist map item in the compare menu. To
	 * keep this is sync with the status bar, this method simple updates
	 * the drop down list which in turn triggers the actual update.
	 */
	private void handleShowDiffDistMapFromMenu() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if (!mod.has3DCoordinates()){
			showNo3DCoordsWarning(mod);
		} else if (!cmPane.hasSecondModel()) {
			showNoSecondContactMapWarning();
		} else if(mod2== null || !mod2.has3DCoordinates()){
			showNo3DCoordsWarning(mod2);
		} else {
			if(!guiState.getShowDiffDistMap()) {
				statusBar.setOverlayStatus(View.BgOverlayType.DIFF_DIST, false);
			} else {
				statusBar.resetOverlayStatus(false);
			}
		}
	}
	
	/**
	 * Handler for the show/hide difference map overlay in compare mode
	 */
	private void handleShowDiffDistMap(boolean secondView) {
		if(mod==null) {
			showNoContactMapWarning();
		} else if (!mod.has3DCoordinates()){
			showNo3DCoordsWarning(mod);
		} else if (!cmPane.hasSecondModel()) {
			showNoSecondContactMapWarning();
		} else if(mod2== null || !mod2.has3DCoordinates()){
			showNo3DCoordsWarning(mod2);
		} else {
			if (secondView) {
				guiState.setShowBottomDiffDistMap(!guiState.getShowBottomDiffDistMap());	// set the flag
				cmPane.toggleDiffDistMap(guiState.getShowBottomDiffDistMap());				// show the actual map			
			} else {
				guiState.setShowDiffDistMap(!guiState.getShowDiffDistMap());
				cmPane.toggleDiffDistMap(guiState.getShowDiffDistMap());
			}
			// update GUI
			if(guiState.getShowDiffDistMap()) {								
				mmToggleDiffDistMap.setIcon(icon_selected);								// set the icon in menu
			} else {
				mmToggleDiffDistMap.setIcon(icon_deselected);
			}
		}
	}
	
	public void handleShowTFbasedMap(boolean secondView) {
		if(mod==null) {
			showNoContactMapWarning();
		} else if (!mod.has3DCoordinates()){
			showNo3DCoordsWarning(mod);
		} else {
			if (secondView) {
				guiState.setShowBottomTFFctMap(true); //!guiState.getShowBottomTFFctMap());
				cmPane.toggleTFFctMap(guiState.getShowBottomTFFctMap());
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

	private void handleRunTinker() {
		if(mod==null) {
			showNoContactMapWarning();
		} else {		
			final View v = this;
			 tinkerDialog = new TinkerPreferencesDialog(this, new TinkerAction() {
					public void doit(TinkerRunner.PARALLEL parallel, TinkerRunner.REFINEMENT refinement, int models, boolean gmbp,boolean ss) {
						tinkerDialog.dispose();
						tinkerRunner = new TinkerRunAction(v,mod,parallel,refinement,models,gmbp,ss);
					}
				},mod.hasGMBPConstraints());
			 
			 tinkerDialog.createGUI();
		}
	}
	
	private void handleJPred() {
		if(mod==null) {
			showNoContactMapWarning();
		} else {
			showJPredMessage();
			mod.assignJPredSecondaryStructure();
		}
	}
	
	protected void handleMinimalSet (){
		if(mod==null) 
			showNoContactMapWarning();
		else {
			showConePeelingMessage();
			//mod.computeMinimalSubset("dummy");
			try{
			//Model modl = mod.copy();
			mod2 = new PdbFtpModel(mod);
			mod2.computeMinimalSubset();
			mod2.setLoadedGraphID("MinSet");
			this.doGreedyPairwiseAlignment();
			doLoadSecondModel(mod2, ali);
			}
			catch(AlignmentConstructionException e){
				System.err.println("Error running Cone Peeling: "+e.getMessage());
			} catch (DifferentContactMapSizeError e) {
				System.err.println("Error running Cone Peeling: "+e.getMessage());
			}
		}
	}

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

			
			TreeMap<ContactMapPane.ContactSelSet, IntPairSet[]> selMap = cmPane.getSetsOfSelectedContactsFor3D();
			
			// disable all previously made objects and selections only once! 
			if( !selMap.get(ContactMapPane.ContactSelSet.COMMON)[ContactMapPane.FIRST].isEmpty() ||
				!selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.FIRST].isEmpty() ||
				!selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.SECOND].isEmpty() ) {
				
				pymol.showStructureHideOthers(mod.getLoadedGraphID(), mod2.getLoadedGraphID());
				
			} else {
				// nothing to do!
				return;
			}

			pymol.showEdgesPairwiseMode(mod, mod2, selMap);
			
		} else {
			IntPairSet contacts   = cmPane.getSelContacts();
			
			if( contacts.isEmpty() ) {
				return; // nothing to do!
			}
			
			String structureId       = mod.getLoadedGraphID();
			//disable all old objects and selections
			pymol.showStructureHideOthers(structureId, structureId);

			// send selection
			pymol.showEdgesSingleMode(mod, contacts);
		}
	}
	/**
	 * 
	 */
	private void handleShowSpheres3D(boolean fromContextMenu) {
		
		//pymol adapter
		PyMolAdaptor pymol = Start.getPyMolAdaptor();
		
		if(mod==null) {
			showNoContactMapWarning();
		} else if(!mod.has3DCoordinates()) {
			showNo3DCoordsWarning(mod);
		} else if(!Start.isPyMolConnectionAvailable()) {				
			showNoPyMolConnectionWarning();
		} else if(fromContextMenu) {//If no contacts are selected, spheres are shown for the current mouse position
			Pair<Integer> residuePair = cmPane.getmousePos();
			if( residuePair.isEmpty() ) {
				showNoContactsSelectedWarning();
				return;
			}
			pymol.showSpheres(mod, residuePair);
		} else {
			
			IntPairSet selRes = cmPane.getSelContacts();	
			if( selRes.isEmpty() ) {
				showNoContactsSelectedWarning();
				return;
			}
			
			//String structureId       = mod.getLoadedGraphID();
			//disable all old objects and selections
			//pymol.showStructureHideOthers(structureId, structureId);
			pymol.showSpheres(mod, selRes);
		}
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
			PyMolAdaptor pymol = Start.getPyMolAdaptor();
			pymol.showSingleDistance(mod, cmPane.getRightClickCont());
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
			pymol.showTriangles(mod, cmPane.getCommonNbh());			
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
	 * Delete currently selected contacts.
	 */
	private void handleDeleteSelContacts() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if(cmPane.getSelContacts().size() == 0) {
			showNoContactsSelectedWarning();
		} else {
			if(reallyDeleteContacts()) {
				for (Pair<Integer> cont:cmPane.getSelContacts()){
					mod.removeEdge(cmPane.mapContactAl2Seq(mod.getLoadedGraphID(), cont));
					//if (hasSecondModel()) {
					//	mod2.removeEdge(mapContactAl2Seq(mod2.getLoadedGraphID(), cont));
					//}
				}
				cmPane.resetSelections();
				cmPane.reloadContacts();	// will update screen buffer and repaint
				updateScoringFunctions();
				updateTitle();
			}
		}
	}

	/**
	 * Inserts/deletes a contact. 
	 * @param cont the contact to toggle
	 */
	public void handleToggleContact(Pair<Integer> cont) {
		if (mod.containsEdge(cmPane.mapContactAl2Seq(mod.getLoadedGraphID(),cont))) {
			mod.removeEdge(cmPane.mapContactAl2Seq(mod.getLoadedGraphID(), cont));
		} else {
			mod.addEdge(cmPane.mapContactAl2Seq(mod.getLoadedGraphID(), cont));
		}
		cmPane.resetSelections();
		cmPane.reloadContacts();
		updateScoringFunctions();
		updateTitle();
	}
	
	private void handleShowShellRels(){
		//pymol adapter
		PyMolAdaptor pymol = Start.getPyMolAdaptor();
		
		if(mod==null) {
			showNoContactMapWarning();
		} else if(!mod.has3DCoordinates()) {
			showNo3DCoordsWarning(mod);
		} else if(!Start.isPyMolConnectionAvailable()) {				
			showNoPyMolConnectionWarning();
		} else {
			// Get first shell neighbours of involved residues
			Pair<Integer> currentResPair = cmPane.getmousePos();
			int first = currentResPair.getFirst();
			int second = currentResPair.getSecond();
			if(first==second){
				IntPairSet firstShellNbrs1 = cmPane.getfirstShellNbrRels(mod, first);
				
				if( firstShellNbrs1.isEmpty() ) {
					return; // nothing to do!
				}				
				pymol.showShellRels(mod, firstShellNbrs1);
			} else{
				IntPairSet firstShellNbrs1 = cmPane.getfirstShellNbrRels(mod, first);
				IntPairSet firstShellNbrs2 = cmPane.getfirstShellNbrRels(mod, second);
				
				if( firstShellNbrs1.isEmpty() || firstShellNbrs2.isEmpty()) {
					return; // nothing to do!
				}
				pymol.showShellRels(mod, firstShellNbrs1);
				pymol.showShellRels(mod, firstShellNbrs2);
			}
		}
	}
	
	private void handleShowSecShell(){
		//pymol adapter
		PyMolAdaptor pymol = Start.getPyMolAdaptor();
		
		if(mod==null) {
			showNoContactMapWarning();
		} else if(!mod.has3DCoordinates()) {
			showNo3DCoordsWarning(mod);
		} else if(!Start.isPyMolConnectionAvailable()) {				
			showNoPyMolConnectionWarning();
		} else {
			IntPairSet completeSecShell = new IntPairSet();
			int res;
			
			Pair<Integer> currentResPair = cmPane.getmousePos();
			int first = currentResPair.getFirst();
			int second = currentResPair.getSecond();
			
			if(first==second){
				IntPairSet firstShellNbrs = cmPane.getfirstShellNbrs(mod, first);
							
				if( firstShellNbrs.isEmpty() ) {
					return; // nothing to do!
				}
				for (Pair<Integer> pair:firstShellNbrs){
					if(pair.getFirst()==first){
						res = pair.getSecond();
					} else {
						res = pair.getFirst();
					}
					IntPairSet secondShellNbrs = cmPane.getfirstShellNbrs(mod, res);
					// But these secondShellNbrs1 also include first shell contacts. We remove them here
					if(pair.getFirst()==first || pair.getSecond()==first){
						secondShellNbrs.remove(pair);
					}
					completeSecShell.addAll(secondShellNbrs);
				}				
			} else{
				IntPairSet firstShellNbrs1 = cmPane.getfirstShellNbrs(mod, first);
				IntPairSet firstShellNbrs2 = cmPane.getfirstShellNbrs(mod, second);
							
				if( firstShellNbrs1.isEmpty() || firstShellNbrs2.isEmpty()) {
					return; // nothing to do!
				}
				for (Pair<Integer> pair:firstShellNbrs1){
					if(pair.getFirst()==first){
						res = pair.getSecond();
					} else {
						res = pair.getFirst();
					}
					IntPairSet secondShellNbrs1 = cmPane.getfirstShellNbrs(mod, res);
					// But these secondShellNbrs1 also include first shell contacts. We remove them here
					if(pair.getFirst()==first || pair.getSecond()==first){
						secondShellNbrs1.remove(pair);
					}					
					completeSecShell.addAll(secondShellNbrs1);
				}
				for (Pair<Integer> pair:firstShellNbrs2){
					if(pair.getFirst()==second){
						res = pair.getSecond();
					} else {
						res = pair.getFirst();
					}
					IntPairSet secondShellNbrs2 = cmPane.getfirstShellNbrs(mod, res);
					// But these secondShellNbrs1 also include first shell contacts. We remove them here
					if(pair.getFirst()==second || pair.getSecond()==second){
						secondShellNbrs2.remove(pair);
					}
					completeSecShell.addAll(secondShellNbrs2);
				}
			}
			
			pymol.showSecShell(mod, completeSecShell);
			
		}
	}
	
	private void handleShowSphoxel(){
		if(mod==null) {
			showNoContactMapWarning();
		} else if (!mod.has3DCoordinates()) {
			showNo3DCoordsWarning(mod);
		} else {
			if (this.mod2 != null)
				if(!mod2.has3DCoordinates()) {
					showNo3DCoordsWarning(mod2);
				} else {
					contView = new ContactView(mod, mod2, "Sphoxel-NbhsTraces Representation", cmPane);
				}
			else
				contView = new ContactView(mod,"Contact Geometry Analysis", cmPane);
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

//	/**
//	 * Handler for the show/hide difference map overlay in compare mode
//	 */	
//	private void handleToggleDiffDistMap() {
//		if(mod==null) {
//			showNoContactMapWarning();
//		} else if (!mod.has3DCoordinates()) {
//			showNo3DCoordsWarning(mod);
//		} else if (!cmPane.hasSecondModel()) {
//			showNoSecondContactMapWarning();
//		} else if (!mod2.has3DCoordinates()) { // it's ok to use here the original mod2 and not the actual displayed alignMod2 (ContactMapPane.mod2) because both should have (or not) 3D coordinates 
//			showNo3DCoordsWarning(mod2);
//		} else {
//			guiState.setShowDiffDistMap(!guiState.getShowDiffDistMap());	// set the flag
//			cmPane.toggleDiffDistMap(guiState.getShowDiffDistMap());		// show the actual map
//			if(guiState.getShowDiffDistMap()) {								// set the icon in menu
//				mmToggleDiffDistMap.setIcon(icon_selected);
//			} else {
//				mmToggleDiffDistMap.setIcon(icon_deselected);
//			}
//		}
//	}
	
	/**
	 * Handler for the swap models action in the comparison menu. Swaps first and
	 * second model.
	 */
	private void handleSwapModels() {
		new View(mod2,mod,ali,null);
		this.dispose();
	}
	
	/**
	 * Handler for the Copy Contacts action in the compare menu. Copies selected contacts
	 * from second to first contact map.
	 */
	private void handleCopyContacts() {
		if(mod==null) {
			showNoContactMapWarning();
		} else if(mod2==null) {
			showNoSecondContactMapWarning();
		} else if(cmPane.getSelContacts().size() == 0) {
			showNoContactsSelectedWarning();
		} else {
			int numContacts = 0;
			for(Pair<Integer> c : cmPane.getSelContacts()) {
				if(mod2.containsEdge(c) && !mod.containsEdge(c)) {
					mod.addEdge(c);
					numContacts++;
				}
			}
			cmPane.resetSelections();
			cmPane.reloadContacts();
			// showContactsCopiedMessage(numContacts)
			updateTitle();
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
				"<p>(C) 2011 The CMView development team</p>" +
				"<p>&nbsp;</p>" +				
				"<p>http://www.bioinformatics.org/cmview/</p>" +
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
	
	/* -------------------- Messages -------------------- */
	
	/** Shown when the button "Predict Secondary Structure" is pressed. */
	private void showJPredMessage() {
		JOptionPane.showMessageDialog(this, "Contacting JPred server. This may take a while...", "Running JPred", JOptionPane.INFORMATION_MESSAGE);
	}
	
	/** Shown when the button "Run Cone Peeling Algorithm" is pressed. */
	private void showConePeelingMessage() {
		String msg = "<html>This will run the 'Cone Peeling Algorithm' described in<br>" +
					 "<br>" +
					 "Sathyapriya et al., Defining an Essence of Structure Determining Residue<br>" +
					 "Contacts in Proteins. PLoS Computational Biology 5(12): e1000584 (2009).<br>" +
				     "<br>" +
				     "The algorithm attempts to calculate a minimal subset of contacts which<br>" +
				     "keep the protein fold intact. This will open the pairwise comparison mode. <br>" +
				     "The minimal subset is shown in black and the original contact map in magenta.</html>";
		JOptionPane.showMessageDialog(this, msg, "Run Cone Peeling Algorithm", JOptionPane.INFORMATION_MESSAGE);
	}
	
	/** Shown when "Add/remove contacts" is selected. */
	private void showAddDeleteContacts() {
		String msg = "<html>In this mode, individual contacts can be added and deleted<br>" +
				     "by clicking on the respective cell in the contact map.<br>" +
				     "<br>" +
				     "To exit this mode, choose one of the other selection modes.</html>";
		JOptionPane.showMessageDialog(this, msg, "Add/Delete Contacts", JOptionPane.INFORMATION_MESSAGE);
	}	

	/* -------------------- Questions ----------------- */
	
	private boolean reallyDeleteContacts() {
		int ret = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the selected contacts?","Confirm Delete", JOptionPane.YES_NO_OPTION);
		return(ret == JOptionPane.YES_OPTION);
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

	/**
	 * Update the title of this window with the
	 * appropriate text for
	 * - no contact map
	 * - single contact map
	 * - single contact map (modified)
	 * - comparing two contact maps
	 * See TITLE_constants.
	 */
	public void updateTitle() {
		String title;
		if(this.mod == null) {
			title = TITLE_DEFAULT;
		} else
		if(this.mod2 == null) {
			title = TITLE_PREFIX + mod.getLoadedGraphID();
			if(mod.isModified()) title += TITLE_MODIFIED;
		} else {
			String t1 = mod.getLoadedGraphID() + (mod.isModified()?TITLE_MODIFIED:"");
			String t2 = mod2.getLoadedGraphID() + (mod2.isModified()?TITLE_MODIFIED:"");
			title = TITLE_COMPARING + t1 + TITLE_TO + t2;
		}
		this.setTitle(title);
	}
	
	/**
	 * Writes the current contact map view to a png file and exits CMView (to be used from command line).
	 * @param imageFileName the name of the file to be written
	 */
	public void writeImageAndExit(String imageFileName) {
		System.out.println("Writing image file...");
		if(this.mod == null) {
			System.err.println("Failed to write image. No contact map loaded.");
			System.exit(1);
		} else {
			File imageFile = new File(imageFileName);
			// Create a buffered image in which to draw
			BufferedImage bufferedImage = new BufferedImage(cmPane.getWidth(), cmPane.getHeight(), BufferedImage.TYPE_INT_RGB);

			// Create a graphics contents on the buffered image
			Graphics2D g2d = bufferedImage.createGraphics();

			// Draw the current contact map window to Image
			cmPane.paintComponent(g2d);

			try {
				ImageIO.write(bufferedImage, "png", imageFile);
				System.out.println("Image written to " + imageFile.getAbsolutePath());
			} catch (IOException e) {
				System.err.println("Error while trying to write to PNG file " + imageFile.getPath());
			}
		}
		System.exit(1);
	}
	
	/**
	 * Handles the event that a new overlay in the overlays dropdown lists is chosen.
	 * This method is called by StatusBar as a response to the gui action.
	 * @param secondView whether the second drop down list was changed, otherwise the first
	 * @param selectedIndex which index was selected from the drop down list
	 */
	protected void handleBgOverlayChange(boolean secondView, Object selectedItem) {
		
		clearBackgrounds(secondView);
		
		if (selectedItem == BgOverlayType.COMMON_NBH.getItem()) {
			handleShowNbhSizeMap(secondView);
		} else if (selectedItem == BgOverlayType.DENSITY.getItem()) {
			handleShowDensityMap(secondView);
		} else if (selectedItem == BgOverlayType.DISTANCE.getItem()) {
			handleShowDistanceMap(secondView);
		} else if (selectedItem == BgOverlayType.DELTA_RANK.getItem()) {
			handleShowDeltaRankMap(secondView);
		} else if (selectedItem == BgOverlayType.DIFF_DIST.getItem()) {
			handleShowDiffDistMap(secondView);
		} else if (selectedItem == BgOverlayType.TF_FUNC.getItem()){
			if (tfDialog==null)
				tfDialog = new TransferFunctionDialog(this, this.cmPane);
			else if (!tfDialog.isDisplayable())
				tfDialog = new TransferFunctionDialog(this, this.cmPane);
//			handleShowTFbasedMap(secondView);			
		} else {
			//this.guiState.setResidueScoringFunctionName(secondView, selectedItem.toString());
			//handleShowResidueScoringMap(secondView);
			//this.statusBar.initResidueScoringFunctionGroup(statusBar.getScoringFunctionWithName((String) selectedItem), secondView);

		}
		
	}
	
	private void clearBackgrounds(boolean secondView) {
		mmToggleDiffDistMap.setIcon(icon_deselected);
		if (secondView) {
			if(guiState.getShowBottomNbhSizeMap()) {
				handleShowNbhSizeMap(secondView);
			}
			if (guiState.getShowBottomDeltaRankMap()) {
				handleShowDeltaRankMap(secondView);
			}
			if (guiState.getShowBottomDistanceMap()) {
				handleShowDistanceMap(secondView);
			}
			if (guiState.getShowBottomDensityMap()) {
				handleShowDensityMap(secondView);
			}
			if(guiState.getShowBottomDiffDistMap()) {
				handleShowDiffDistMap(secondView);
			}
			if(guiState.getShowBottomResidueScoringMap()) {
				//handleShowResidueScoringMap(secondView);
			}
		} else {
			if(guiState.getShowNbhSizeMap()) {
				handleShowNbhSizeMap(secondView);
			}
			if (guiState.getShowDeltaRankMap()) {
				handleShowDeltaRankMap(secondView);
			}
			if (guiState.getShowDistanceMap()) {
				handleShowDistanceMap(secondView);
			}
			if (guiState.getShowDensityMap()) {
				handleShowDensityMap(secondView);
			}
			if(guiState.getShowDiffDistMap()) {
				handleShowDiffDistMap(secondView);
			}			
			if(guiState.getShowResidueScoringMap()) {
				//handleShowResidueScoringMap(secondView);
			}
		}
	}
	
	/**
	 * Handles the user action to switch on discretization of a weighted graph
	 * @param weightCutoff the cutoff to apply for discretization
	 */
	public void handleSwitchDiscretizeOn(double weightCutoff) {
		// 1. switch on discretization
		// - make backup copy
		// - apply discretization
		// - notify that contact map has changed
		mod.makeBackupGraph();
		mod.discretizeGraphByWeightCutoff(weightCutoff);
		cmPane.resetSelections();
		cmPane.reloadContacts();	// will update screen buffer and repaint
	}
	
	/**
	 * Handles the user action to switch on discretization of a weighted graph
	 * @param fraction contacts with the length/fraction highest weights will be kept, others discarded
	 */
	public void handleSwitchDiscretizeOn(int fraction) {
		// 1. switch on discretization
		// - make backup copy
		// - apply discretization
		// - notify that contact map has changed
		mod.makeBackupGraph();
		mod.discretizeGraphByOrderedWeights(fraction);
		cmPane.resetSelections();
		cmPane.reloadContacts();	// will update screen buffer and repaint
	}
	
	/** 
	 * Handles the user action to switch off discretization of a weighted graph
	 */
	public void handleSwitchDiscretizeOff() {
		// 4. switch off discretization
		// - restore backup copy
		mod.restoreBackupGraph();
		// - notify that contact map has changed
		cmPane.resetSelections();
		cmPane.reloadContacts();	// will update screen buffer and repaint
	}
	
	/**
	 * Handles the user action to change the discretization weightCutoff while discretization is active
	 * @param weightCutoff the new cutoff to apply for discretization
	 */
	public void handleChangeDiscretization(double weightCutoff) {
		// 2. change discretization
		// - restore backup copy
		// - apply discretization
		mod.restoreBackupGraph();
		mod.discretizeGraphByWeightCutoff(weightCutoff);
		// - notify that contact map has changed
		cmPane.resetSelections();
		cmPane.reloadContacts();	// will update screen buffer and repaint
	}
	
	/**
	 * Handles the user action to switch to discretization by ordered weights while discretization is active
	 * @param fraction contacts with the length/fraction highest weights will be kept, others discarded
	 */	
	public void handleChangeDiscretization(int fraction) {
		// 2. change discretization
		// - restore backup copy
		// - apply discretization
		mod.restoreBackupGraph();
		mod.discretizeGraphByOrderedWeights(fraction);
		// - notify that contact map has changed
		cmPane.resetSelections();
		cmPane.reloadContacts();	// will update screen buffer and repaint
	}

	/**
	 * Handles the user action to apply discretization permanently while discretization by weightCutoff is active
	 * @param weightCutoff
	 */
	public void handleApplyDiscretizationPermanently(double weightCutoff) {
		// 3. apply discretization permanently
		// - restore backup copy
		// - apply discretization
		// - delete backup copy
		// - hide discretization controls
		mod.restoreBackupGraph();
		mod.discretizeGraphByWeightCutoff(weightCutoff);
		mod.deleteBackupGraph();
		mod.setIsGraphWeighted(false);
		statusBar.showMultiModelGroup(false, null);
		// - notify that contact map has changed
		cmPane.resetSelections();
		cmPane.reloadContacts();	// will update screen buffer and repaint
		updateTitle();
	}

	/**
	 * Handles the user action to apply discretization permanently while discretization by ordered weights is active
	 * @param fraction contacts with the length/fraction highest weights will be kept, others discarded
	 */
	public void handleApplyDiscretizationPermanently(int fraction) {
		// 3. apply discretization permanently
		// - restore backup copy
		// - apply discretization
		// - delete backup copy
		// - hide discretization controls
		mod.restoreBackupGraph();
		mod.discretizeGraphByOrderedWeights(fraction);
		mod.deleteBackupGraph();
		mod.setIsGraphWeighted(false);
		statusBar.showMultiModelGroup(false, null);
		// - notify that contact map has changed
		cmPane.resetSelections();
		cmPane.reloadContacts();	// will update screen buffer and repaint
		updateTitle();
	}

	public void handleAddBestDR() {
		mod.addBestDR();
		cmPane.resetSelections();
		cmPane.reloadContacts();
		updateTitle();
	}
	
	public void handleDeleteWorstDR() {
		mod.delWorstDR();
		cmPane.resetSelections();
		cmPane.reloadContacts();
		updateTitle();
	}

	
	/* -------------------- getter methods -------------------- */
	
	/**
	 * Returns the gui state object associated with this View.
	 * @return the gui state object associated with this View
	 */
	protected GUIState getGUIState() {
		return this.guiState;
	}
	
	/**
	 * Returns true if a database connection is expected to be available. This is to avoid
	 * trying to connect when it is clear that the trial will fail.
	 */
	public boolean isDatabaseConnectionAvailable() {
		return this.database_found;
	}

	
	
}

