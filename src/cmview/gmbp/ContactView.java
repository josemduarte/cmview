package cmview.gmbp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
//import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import owl.core.structure.PdbLoadError;
import owl.core.util.actionTools.Getter;
import owl.core.util.actionTools.GetterError;
//import javax.swing.JToolBar;

import cmview.ContactMapPane;
import cmview.LoadAction;
import cmview.LoadDialog;
import cmview.LoadDialogConstructionError;
import cmview.Start;
import cmview.datasources.Model;
import cmview.datasources.ModelConstructionError;
import cmview.datasources.PdbFileModel;

public class ContactView extends JFrame implements ActionListener{ //, KeyListener{
	
	/* default argument
	-f /Users/vehlow/Documents/workspace/7ODC.pdb -c A
	*/
	
	protected static Dimension defaultDim = ContactPane.defaultDim; //new Dimension(1200, 880);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// menu item labels (used in main menu, popup menu and icon bar)
	// File
	private static final String LABEL_FILE_INFO = "Info";
//	private static final String LABEL_FILE_PRINT = "Print...";	
//	private static final String LABEL_FILE_QUIT = "Quit";
	private static final String LABEL_PNG_FILE = "PNG File...";
	private static final String LABEL_SPHOXEL_CSV_FILE = "Sphoxel to CSV File...";
	private static final String LABEL_TRACES_CSV_FILE = "Traces to CSV File...";
	private static final String LABEL_SETTINGS_CSV_FILE = "Settings to CSV File...";
	private static final String LABEL_LSETTINGS_CSV_FILE = "Load Settings";	
	private static final String LABEL_PDB_FILE = "Load 3rd model (PDB File)";
	// Select
	private static final String LABEL_SQUARE_SELECTION_MODE = "Square Selection Mode";
	private static final String LABEL_CLUSTER_SELECTION_MODE = "Cluster Selection Mode";
	private static final String LABEL_PAN_VIEW_MODE = "Panning Mode";
	// Show
	private static final String LABEL_HISTOGRAM_MODE = "Histogram4Selection";
	private static final String LABEL_DEL_SELECTION = "DeleteSelectedRange";
	private static final String LABEL_RES_INFO = "Switch residue labels on/off";
	private static final String LABEL_CENTRAL_RES = "Switch central residue on/off";
	private static final String LABEL_TEMPLATE_TRACE = "Show/Hide template trace for NBHString";
	
	// GUI components in the main frame
	JToolBar toolBar;			// icon tool bar
	JPanel lampdaRul;				// Panel for top (lampda) ruler
	JPanel phiRul;			// Panel for left (phi) ruler	
	JPanel svP; 				// Main panel holding the Contact map pane Sperical Voxel view 
	JPanel tbPane;				// tool bar panel holding toolBar and cmp (necessary if toolbar is floatable)
	ContactStatusBar contStatBar;		// A status bar with metainformation on the right
	JPopupMenu popup; 		 	// right-click context menu
	
	// Toolbar Buttons
	JToggleButton tbSquareSel, tbClusterSel, tbPanMode, tbResInfo, tbCentralRes, tbTempTrace;
	
	// indices of the all main menus in the frame's menu bar
	TreeMap<String, Integer> menu2idx;
	
	HashMap<JPopupMenu,JMenu> popupMenu2Parent;
	
	TreeMap<String,JMenu> smFile;
	TreeMap<String,JMenu> smCompare;
	
	// Menu items
	// M -> "menu bar"
	JMenuItem squareM, clusterM;
	// P -> "popup menu"
	JMenuItem histP, deleteP;
	// mm -> "main menu"
	JMenuItem mmInfo, mmSavePng, mmSaveSCsv, mmSaveTCsv, mmSaveSettings, mmLoadSettings, mmLoadPdb, mmQuit;
	JMenuItem mmSelectAll;
	
	private Dimension screenSize;			// current size of this component on screen
	
	// flags for viewing options
	private boolean showSphoxelFeature=true;
	private boolean showTracesFeature=false;

	// Data and status variables
	private ContactGUIState guiState;
	private Model mod, mod2, mod3;
	public ContactMapPane cmPane;
	public ContactPane cPane;
	public AngleRuler lambdaRuler;
	public AngleRuler phiRuler;
	public ColorScaleView colView;
	public HistogramView histView;

	public boolean fileChooserOpened = false;
	
	private class MyDispatcher implements KeyEventDispatcher {
		private ContactPane cPane;
		private MyDispatcher(ContactPane cPane){
			this.cPane = cPane;
		}
	    public boolean dispatchKeyEvent(KeyEvent e) {
	    	// -- forward key events --
	        if (e.getID() == KeyEvent.KEY_PRESSED) {
	        	this.cPane.keyPressed(e);
	        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
	        	this.cPane.keyReleased(e);
	        } else if (e.getID() == KeyEvent.KEY_TYPED) {
	        	this.cPane.keyTyped(e);
	        }
	        return false;
	    }
	}
	
	/** Create a new View object */
	public ContactView(Model mod, String title, ContactMapPane cmPane) {
		super(title);
		Start.viewInstancesCreated();	
		this.guiState = new ContactGUIState(this);
		this.mod = mod;
		this.mod2 = null;
		this.mod3 = null;
		this.cmPane = cmPane;
		if (Start.isCgapDatabaseConnectionAvailable())
			this.showTracesFeature = true;
		else
			defaultDim = new Dimension(1200, 600);
		this.guiState.setShowNBHStraces(this.showTracesFeature);	
		
		initContactView();
	}
	
	/** Create a new View object */
	public ContactView(Model mod, Model mod2, String title, ContactMapPane cmPane) {
		super(title);
		Start.viewInstancesCreated();		
		this.guiState = new ContactGUIState(this);
		this.mod = mod;
		this.mod2 = mod2;
		this.mod3 = null;
		this.cmPane = cmPane;
		if (Start.isCgapDatabaseConnectionAvailable())
			this.showTracesFeature = true;
		else
			defaultDim = new Dimension(1200, 600);
		this.guiState.setShowNBHStraces(this.showTracesFeature);
		
		initContactView();
	}
	
	private void initContactView(){

//		Dimension dim = new Dimension(150, 250);
//		this.setPreferredSize(dim);
		
//		addKeyListener(this);
//		this.setFocusable(true);
		
		if(mod == null) {
			this.setPreferredSize(new Dimension(Start.INITIAL_SCREEN_SIZE,Start.INITIAL_SCREEN_SIZE));
		}
		else{
			int xDim = defaultDim.width + AngleRuler.STD_RULER_WIDTH; //+ ContactStatusBar.DEFAULT_WIDTH;
			int yDim = defaultDim.height + AngleRuler.STD_RULER_WIDTH;
			this.screenSize = new Dimension(xDim, yDim);
			this.setPreferredSize(this.screenSize);
		}
//		this.guiState = new GUIState(this);
		this.initGUI(); 							// build gui tree and pack
		setVisible(true);							// show GUI	

		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
	    manager.addKeyEventDispatcher(new MyDispatcher(this.cPane));
		
		// show status bar groups
		if(mod != null) {
			contStatBar.showDeltaRadiusPanel(true);
			contStatBar.showResolutionPanel(true);
		}
		
		final JFrame parent = this;					// need a final to refer to in the thread below
		EventQueue.invokeLater(new Runnable() {		// execute after other events have been processed
			public void run() {
				parent.toFront();					// bring new window to front
			}
		});
		
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

	
	/** Initialize and show the main GUI window */
	private void initGUI(){

		// Setting the main layout 
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocation(20,20);
//		this.setSize(150, 250);
		
		// Creating the Panels
		tbPane = new JPanel(new BorderLayout());	// toolbar panel, holding only toolbar and cmp (all the rest)
		svP = new JPanel(new BorderLayout()); 		// panel holding the cmPane and (optionally) the ruler panes
		lampdaRul = new JPanel(new BorderLayout()); 	// panel holding the lampda (top) ruler
		phiRul = new JPanel(new BorderLayout()); 	// panel holding the phi (left) ruler
		
//		// Icons
		ImageIcon icon_square_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "shape_square.png"));
		ImageIcon icon_cluster_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "cog.png"));
		ImageIcon icon_pan_view_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "crossarrows.png"));
		ImageIcon icon_hist_view_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "histogram.png"));
		ImageIcon icon_delete_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "cross.png"));
		ImageIcon icon_toggle_res_info = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "information.png"));
		ImageIcon icon_toggle_central_res = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "arrow_in.png"));
		ImageIcon icon_toggle_template_trace = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "TT.png"));
		
		// Tool bar
		toolBar = new JToolBar();
		toolBar.setVisible(Start.SHOW_ICON_BAR);
		
		Dimension separatorDim = new Dimension(30,toolBar.getHeight());
		tbSquareSel = makeToolBarToggleButton(icon_square_sel_mode, LABEL_SQUARE_SELECTION_MODE, true, true, true);
		tbPanMode = makeToolBarToggleButton(icon_pan_view_mode, LABEL_PAN_VIEW_MODE, true, true, true);
		if (this.showTracesFeature)
			tbClusterSel = makeToolBarToggleButton(icon_cluster_sel_mode, LABEL_CLUSTER_SELECTION_MODE, true, true, true);
		else
			tbClusterSel = makeToolBarToggleButton(icon_cluster_sel_mode, LABEL_CLUSTER_SELECTION_MODE, true, false, false);			
		toolBar.addSeparator(separatorDim);
		if (this.showTracesFeature)
			tbResInfo = makeToolBarToggleButton(icon_toggle_res_info, LABEL_RES_INFO, false, true, true);
		else
			tbResInfo = makeToolBarToggleButton(icon_toggle_res_info, LABEL_RES_INFO, false, false, false);
		tbCentralRes = makeToolBarToggleButton(icon_toggle_central_res, LABEL_CENTRAL_RES, true, true, true);
		tbTempTrace =  makeToolBarToggleButton(icon_toggle_template_trace, LABEL_TEMPLATE_TRACE, true, true, true);
		
		
		// ButtonGroup for selection modes (so upon selecting one, others are deselected automatically)
		ButtonGroup selectionModeButtons = new ButtonGroup();
		selectionModeButtons.add(tbSquareSel);
		selectionModeButtons.add(tbClusterSel);
		selectionModeButtons.add(tbPanMode);
		
		// Popup menu
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		popup = new JPopupMenu();
		histP = makePopupMenuItem(LABEL_HISTOGRAM_MODE, icon_hist_view_mode, popup);
		deleteP = makePopupMenuItem(LABEL_DEL_SELECTION, icon_delete_sel_mode, popup);
		
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
		// Save
		submenu = new JMenu("Save to");
		popupMenu2Parent.put(submenu.getPopupMenu(),submenu);
		mmSavePng = makeMenuItem(LABEL_PNG_FILE, null, submenu);
		mmSaveSCsv = makeMenuItem(LABEL_SPHOXEL_CSV_FILE, null, submenu);
		mmSaveTCsv = makeMenuItem(LABEL_TRACES_CSV_FILE, null, submenu);
		mmSaveSettings = makeMenuItem(LABEL_SETTINGS_CSV_FILE, null, submenu);
		menu.add(submenu);
		smFile.put("Save", submenu);
		// Load 
		mmLoadSettings = makeMenuItem(LABEL_LSETTINGS_CSV_FILE, null, menu);
		mmLoadPdb = makeMenuItem(LABEL_PDB_FILE, null, menu);
//		// Print, Quit
//		mmQuit = makeMenuItem(LABEL_FILE_QUIT, null, menu);
		addToJMenuBar(menu);
		
//		// Select menu
		menu = new JMenu("Select");
		menu.setMnemonic(KeyEvent.VK_S);
		submenu = new JMenu("Selection Mode");
		squareM = makeMenuItem(LABEL_SQUARE_SELECTION_MODE, icon_square_sel_mode, submenu);
		if(this.showTracesFeature)
			clusterM = makeMenuItem(LABEL_CLUSTER_SELECTION_MODE, icon_cluster_sel_mode, submenu);		
		menu.add(submenu);
		addToJMenuBar(menu);
		
		// Creating contact map pane and ruler if model loaded
		if(mod != null) {	
			if(mod2 != null)
				cPane = new ContactPane(this.mod, this.mod2, this.cmPane, this);
			else
				cPane = new ContactPane(this.mod, this.cmPane, this);
			contStatBar = new ContactStatusBar(this);	// pass reference to 'this' for handling gui actions
			cPane.setStatusBar(contStatBar);
			svP.add(cPane, BorderLayout.CENTER);
			
			lambdaRuler = new AngleRuler(this, cPane, AngleRuler.TOP);
			phiRuler = new AngleRuler(this, cPane, AngleRuler.LEFT);
			lampdaRul.add(lambdaRuler);
			lampdaRul.setSize(lambdaRuler.getSize());
			lampdaRul.setPreferredSize(lambdaRuler.getSize());
			phiRul.add(phiRuler);
			phiRul.setSize(phiRuler.getSize());
			phiRul.setPreferredSize(phiRuler.getSize());
			
//			lampdaRul.setBorder(BorderFactory.createLineBorder(Color.black));
//			phiRul.setBorder(BorderFactory.createLineBorder(Color.black));
		}
//		cPane.setVisible(false);
		if(guiState.getShowRulers()) {
			svP.add(lampdaRul, BorderLayout.NORTH);
			svP.add(phiRul, BorderLayout.WEST);
		}
	
		// Add everything to the content pane		
		this.tbPane.add(toolBar, BorderLayout.NORTH);			// tbPane is necessary if toolBar is floatable
		
		// Add everything to the content pane				
		this.tbPane.add(svP,BorderLayout.CENTER);			
		this.tbPane.add(contStatBar,BorderLayout.EAST);
		this.getContentPane().add(tbPane, BorderLayout.CENTER);
		
//		System.out.println("Sizes of panels:");
//		System.out.println("ContactView: "+this.getSize().height+"x"+this.getSize().width);
//		System.out.println("tbPane: "+this.tbPane.getSize().getHeight()+"x"+this.tbPane.getSize().getWidth());
//		System.out.println("svP: "+this.svP.getSize().getHeight()+"x"+this.svP.getWidth());
//		System.out.println("cPane: "+this.cPane.getSize().getHeight()+"x"+this.cPane.getSize().getWidth());
//		System.out.println("Size of lambdaRuler: "+lambdaRuler.getSize().height+"x"+lambdaRuler.getSize().width);
//		System.out.println("Size of lampdaRul: "+lampdaRul.getSize().height+"x"+lampdaRul.getSize().width);
//		System.out.println("Size of phiRuler: "+phiRuler.getRulerSize().height+"x"+phiRuler.getRulerSize().width);
						
		pack();
		this.cPane.requestFocus();
	}
	
	public void updateGUI(){ //(Model mod, String title, ContactMapPane cmPane) {
//		this.mod = mod;
//		this.cmPane = cmPane;
		this.cPane.updateQueryParam(0);
		try {
			this.cPane.recalcSphoxel();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// -------- HandleEvents -------------
	
	private void handleSaveToPng() {
		if(this.mod == null) {
			showNoContactWarning();
		} else {
			this.fileChooserOpened = true;
			String fn = Start.getFileChooser().getCurrentDirectory()+"/"+this.cPane.getDescribingFN()+".png";
			Start.getFileChooser().setSelectedFile(new File(fn));
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();
				if (confirmOverwrite(chosenFile)) {
					// Create a buffered image in which to draw
					BufferedImage bufferedImage = new BufferedImage(cPane.getWidth(), cPane.getHeight(), BufferedImage.TYPE_INT_RGB);

					// Create a graphics contents on the buffered image
					Graphics2D g2d = bufferedImage.createGraphics();
					g2d.setBackground(Color.white);
					g2d.setColor(Color.white);

					// Draw the current contact map window to Image
					cPane.paintComponent(g2d);

					try {
//						String file = chosenFile.getPath();
//						if (file.substring(file.length()-4, file.length()-1)!="png")
//							file+= ".png";
						ImageIO.write(bufferedImage, "png", chosenFile);
						System.out.println("File " + chosenFile.getPath() + " saved.");
					} catch (IOException e) {
						System.err.println("Error while trying to write to PNG file " + chosenFile.getPath());
					}
				}
			}
			this.fileChooserOpened = false;
		}
	}
	
	private void handleSaveSphoxelsToCsv() {
		if(this.mod == null) {
			showNoContactWarning();
		} else {
			this.fileChooserOpened = true;
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();
				System.out.println("File " + chosenFile.getPath());
				System.out.println("chosenFile= "+chosenFile.toString());
				String filename = chosenFile.toString();
				System.out.println("Substring: "+filename.substring(filename.length()-4, filename.length()-1));
				if (filename.substring(filename.length()-4, filename.length()-1) != ".csv")
					filename += ".csv";
				if (confirmOverwrite(chosenFile)) {
					cPane.writeSphoxels(filename);
				}
			}
			this.fileChooserOpened = false;
		}
	}
	
	private void handleSaveTracesToCsv() {
		if(this.mod == null) {
			showNoContactWarning();
		} else {
			this.fileChooserOpened = true;
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();
				System.out.println("File " + chosenFile.getPath());
				System.out.println("chosenFile= "+chosenFile.toString());
				String filename = chosenFile.toString();
				System.out.println("Substring: "+filename.substring(filename.length()-4, filename.length()-1));
				if (filename.substring(filename.length()-4, filename.length()-1) != ".csv")
					filename += ".csv";
				if (confirmOverwrite(chosenFile)) {
					cPane.writeTraces(filename);
;				}
			}
			this.fileChooserOpened = false;
		}
	}
	
	private void handleSaveSettingsToCsv(){
		if(this.mod == null) {
			showNoContactWarning();
		} else {
			this.fileChooserOpened = true;
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();
				System.out.println("File " + chosenFile.getPath());
				System.out.println("chosenFile= "+chosenFile.toString());
				String filename = chosenFile.toString();
				System.out.println("Substring: "+filename.substring(filename.length()-4, filename.length()-1));
				if (filename.substring(filename.length()-4, filename.length()-1) != ".csv")
					filename += ".csv";
				if (confirmOverwrite(chosenFile)) {
					cPane.writeSettings(filename);
;				}
			}
			this.fileChooserOpened = false;
		}		
	}
	
	private void handleLoadSettingsFromCsv() throws NumberFormatException, IOException{
		// open global file-chooser and get the name the alignment file
		this.fileChooserOpened = true;
		JFileChooser fileChooser = Start.getFileChooser();
        FileFilter filter = new FileNameExtensionFilter("CSV file", "csv");
        fileChooser.setFileFilter(filter);		
		int ret = fileChooser.showOpenDialog(this);
		File source = null;
		if(ret == JFileChooser.APPROVE_OPTION) {
			source = fileChooser.getSelectedFile();
		} else {
			return;
		}
		cPane.loadSettings(source.getPath());
		this.fileChooserOpened = false;
	}
	
	private void handleInfo() {
		if(this.mod == null) {
			//System.out.println("No contact map loaded yet.");
			showNoContactWarning();
		} else {			
//			JDialog infoDialog = new ContactMapInfoDialog(this, mod, mod2, ali, cmPane);
//			infoDialog.setLocationRelativeTo(this);
//			infoDialog.setVisible(true);
		}
	}

	private void handleQuit() {
		Start.shutDown(0);
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
	
	/**
	 * Handles the user action to change flag whether to use fixed radius ranges 
	 * @param fixedRadiusRanges --> boolean flag
	 */
	public void handleChangeRadiusRangesFixed(boolean fixed) {
		this.cPane.setRadiusRangesFixed(fixed);
		System.out.println("handleChangeRadiusRangeFixed "+fixed);
	}
	
	/**
	 * Handles the user action to change flag whether to differentiate ssType 
	 * @param diffSSType --> boolean flag
	 */
	public void handleChangeDiffSSType(boolean diff) {
		this.cPane.setDiffSStype(diff);
		if (diff)
			this.cPane.calcSphoxelParam();
		try {
			this.cPane.recalcSphoxel();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Handles the user action to change flag whether to show residue labels or not 
	 * @param show --> boolean flag
	 */
	public void handleChangeShowResInfo(boolean show) {
		this.cPane.setShowResInfo(show);
	}
	
	/**
	 * Handles the user action to change flag whether to show residue labels or not 
	 * @param show --> boolean flag
	 */
	public void handleChangeShowCentralRes(boolean show) {
		this.cPane.setPaintCentralResidue(show);
	}
	
	/**
	 * Handles the user action to change flag whether to show the trace of the template nbhString or not 
	 * @param show --> boolean flag
	 */
	public void handleChangeShowTemplateTrace(boolean show) {
		this.cPane.setShowNBHStemplateTrace(show);
	}
	
	/**
	 * Handles the user action to change flag map projection should be used
	 * @param projType --> int
	 */
	public void handleChangeProjectionType(int type) {
		this.cPane.setMapProjType(type);
		this.cPane.repaint();
		this.lambdaRuler.repaint();
	}
	
	/**
	 * Handles the user action to change flag whether to remove outliers in contact density values 
	 * @param removeOutliers --> boolean flag
	 */
	public void handleChangeRemOutliers(boolean rem) {
		this.cPane.setRemoveOutliers(rem);
//		this.cPane.repaint();
		if (colView!=null || histView!=null)
			this.cPane.calcHistogramms();
		if (colView!=null)
			colView.repaint();
		if (histView!=null)
			histView.repaint();
	}
	
	/**
	 * Handles the user action to change flag whether to remove outliers in contact density values 
	 * @param removeOutliers --> boolean flag
	 */
	public void handleChangeOutlierThresholds(double val, int type) {
		if (type == 0)
			this.cPane.setMinAllowedRat(val);
		else 
			this.cPane.setMaxAllowedRat(val);
//		this.cPane.repaint();
		if (colView!=null || histView!=null)
			this.cPane.calcHistOfSphoxelMap();
		if (colView!=null)
			colView.repaint();
		if (histView!=null)
			histView.repaint();
	}
	
	/**
	 * Handles the user action to change type of colour scaling
	 * @param type --> int
	 */
	public void handleChangeColourScale(int type) {
		this.cPane.setChosenColourScale(type);
		this.cPane.repaint();
		if (colView!=null){
			colView.setChosenColourScale(type);
			colView.repaint();
		}
	}
	
	/**
	 * Handles the user action to change type of colour scaling
	 * @param type --> int
	 */
	public void handleChangeShowColourScale(int type) {		
//		if (colView==null)
			colView = new ColorScaleView(this.cPane);
		colView.setChosenColourScale(type);
		this.cPane.calcHistogramms();
		colView.repaint();	
		
//		histView = new HistogramView(this.cPane);
//		histView.setChosenColourScale(type);
//		histView.repaint();
	}
	
	/**
	 * Handles the user action to change the nbhstring to use for traces
	 * @param type --> string nbhs
	 * @param maxNum--> int num
	 */
	public void handleChangeTracesParam(String nbhs, int num) {	
		boolean recalcOptStrings = false;
		if (this.cPane.getMaxNumTraces()!=num)
			recalcOptStrings = true;
		this.cPane.setNbhString(nbhs);	
		this.cPane.setMaxNumTraces(num);
		try {
			this.cPane.recalcTraces(true);
			if (recalcOptStrings)
				this.cPane.extractSetOfOptStrings();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
////		if (colView==null)
//			colView = new ColorScaleView(this.cPane);
//		colView.setChosenColourScale(type);
//		this.cPane.calcHistogramms();
//		colView.repaint();	
		
//		histView = new HistogramView(this.cPane);
//		histView.setChosenColourScale(type);
//		histView.repaint();
	}
	
	/**
	 * Handles the user action to change the nbhstring to use for traces
	 * @param ssType --> char
	 * @param diffSSType--> boolean
	 */
	public void handleChangeTracesParam(char ssType, boolean diffSSType) {	
		this.cPane.setNbhSSType(ssType);
		this.cPane.setDiffSStypeNBH(diffSSType);
		try {
			this.cPane.recalcTraces(true);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Handles the user action to change the nbhstring to use for traces
	 * @param type --> string nbhs
	 * @param maxNum--> int num
	 */
	public void handleChangeClusterParam(int eps, int num) {	
		this.cPane.setEpsilon(eps);
		this.cPane.setMinNumNBs(num);
		this.cPane.runClusterAnalysis();
	}
	
	
	/**
	 * Handles the user action to show histogram for selection
	 * @param 
	 */
	public void handleShowHistogram() {	
		histView = new HistogramView(this.cPane, "Histograms for occupied angle range "+String.valueOf(cPane.getChosenSelection()+1));		
		this.cPane.calcHistogramms();	
		histView.setChosenColourScale(cPane.getChosenColourScale());
		histView.repaint();
	}
	
	public void handleDeleteSelection(){
		this.cPane.deleteSelectedRange();
	}
	
	/**
	 * Handles the user action to change the radius range 
	 * @param minR and maxR --> range
	 */
	public void handleChangeRadiusRange(float minR, float maxR) {
		this.cPane.setMinR(minR);
		this.cPane.setMaxR(maxR);
		System.out.println("handleChangeRadiusRange "+minR+"-"+maxR);
		try {
			this.cPane.recalcSphoxel();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Handles the user action to change the resolution
	 * @param numSteps the number of Steps to apply for query
	 */
	public void handleChangeResolution(int val) {
//		this.cPane.setNumSteps(val);
		this.cPane.setResol((float)val);
		try {
			this.cPane.recalcSphoxel();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	/* -------------------- Warnings -------------------- */

	/** Shows a window with a warning message that no contact map is loaded yet */
	private void showNoContactWarning() {
		JOptionPane.showMessageDialog(this, "No contact selected yet", "Warning", JOptionPane.INFORMATION_MESSAGE);
	}
	
	/* -------------------- getters and setters methods -------------------- */
		
	/**
	 * Returns the gui state object associated with this View.
	 * @return the gui state object associated with this View
	 */
	protected ContactGUIState getGUIState() {
		return this.guiState;
	}
		
	public boolean isShowSphoxelFeature() {
		return showSphoxelFeature;
	}

	public boolean isShowTracesFeature() {
		return showTracesFeature;
	}
	
	public ColorScaleView getColorScaleView(){
//		if (colView==null){
//			colView = new ColorScaleView(this.cPane);
//			colView.repaint();
//		}
		return colView;
	}
	
	public HistogramView getHistogramView(){
//		if (histView==null){
//			histView = new HistogramView(this.cPane);
//			histView.repaint();
//		}
		return histView;
	}

	public Dimension getScreenSize(){
//		System.out.println("screensize HxW: "+this.cPane.getScreenSize().height+"x"+this.cPane.getScreenSize().width);
//		System.out.println("screensize HxW: "+this.cPane.getSize().height+"x"+this.cPane.getSize().width);
//		return this.cPane.getSize();
//		System.out.println("getScreensize ContactView HxW: "+this.screenSize.height+"x"+this.screenSize.width);
		return this.screenSize;
	}
	
	private void handleLoadFromPdbFile() {

//		if (secondModel == SECOND_MODEL && mod == null){
//			this.showNoContactMapWarning();
//		} else
		{
			try {
				LoadDialog dialog = new LoadDialog(this, "Load from PDB file", new LoadAction(true) {
					public void doit(Object o, String f, String ac, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss, String db, int gid, String seq) {
						doLoadFromPdbFile(f, modelSerial, loadAllModels, cc, ct, dist, minss, maxss);
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
//				LoadDialog actLoadDialog = dialog;
				dialog.showIt();
			} catch (LoadDialogConstructionError e1) {
				System.err.println("Failed to load the load-dialog.");
			}
		}
	}
	
	public void doLoadFromPdbFile(String f, int modelSerial, boolean loadAllModels, String cc, String ct, double dist, int minss, int maxss) {
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
				this.mod3 = mod;
				this.cPane.setThirdModel(this.mod3);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		} catch(ModelConstructionError e) {
			showLoadError(e.getMessage());
		}
	}	
	
	/** Error dialog to be shown if loading a model failed. */
	private void showLoadError(String message) {
		JOptionPane.showMessageDialog(this, "<html>Failed to load contact map:<br>" + message + "</html>", "Load Error", JOptionPane.ERROR_MESSAGE);
	}
	
	/* ------------------Event Handling--------------------------*/

	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
		/* ---------- File Menu ---------- */

		// Save
		if(e.getSource() == mmSavePng) {
			handleSaveToPng();
		}
		if(e.getSource() == mmSaveSCsv) {
			handleSaveSphoxelsToCsv();
		}
		if(e.getSource() == mmSaveTCsv) {
			handleSaveTracesToCsv();
		}
		if(e.getSource() == mmSaveSettings) {
			handleSaveSettingsToCsv();
		}
		
		// Load
		if(e.getSource() == mmLoadSettings) {
			try {
				handleLoadSettingsFromCsv();
			} catch (NumberFormatException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		if(e.getSource() == mmLoadPdb) {
			handleLoadFromPdbFile();
		}
		
		// Info, Print, Quit
		if(e.getSource() == mmInfo) {
			handleInfo();
		}	
		if(e.getSource() == mmQuit) {
			handleQuit();
		}
		
		/* ---------- Action menu ---------- */

		// Selection modes

		// square button clicked
		if (e.getSource() == squareM || e.getSource() == tbSquareSel ) {
			guiState.setSelectionMode(ContactGUIState.SelMode.RECT);
		}
		// cluster button clicked
		if (e.getSource() == clusterM || e.getSource() == tbClusterSel ) {
			guiState.setSelectionMode(ContactGUIState.SelMode.CLUSTER);
		}
		if (e.getSource() == tbPanMode) {
			guiState.setSelectionMode(ContactGUIState.SelMode.PAN);
		}
		
		if (e.getSource() == tbResInfo) {
			handleChangeShowResInfo(tbResInfo.isSelected());
		}
		
		if (e.getSource() == tbCentralRes) {
			handleChangeShowCentralRes(tbCentralRes.isSelected());
		}
		
		if (e.getSource() == tbTempTrace) {
			handleChangeShowTemplateTrace(tbTempTrace.isSelected());
		}
		
		/* ---------- RightClick PopupMenu ------------ */
		// Histogram button clicked
		if (e.getSource() == histP) {
			handleShowHistogram();
		}
		if (e.getSource() == deleteP){
			handleDeleteSelection();
		}
	}

	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		System.out.println("CV keyPressed");
		
	}

	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		System.out.println("CV keyReleased");
		int id = e.getID();
        String keyString;
        if (id == KeyEvent.KEY_TYPED) {
            char c = e.getKeyChar();
            keyString = "key character = '" + c + "'";
        } else {
            int keyCode = e.getKeyCode();
            keyString = "key code = " + keyCode
                    + " ("
                    + KeyEvent.getKeyText(keyCode)
                    + ")";
        }
        System.out.println("keyString= "+keyString);
	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub	
		System.out.println("CV keyTyped");	
	}
	
}
