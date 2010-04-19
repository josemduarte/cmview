package cmview.gmbp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.TreeMap;

import javax.imageio.ImageIO;
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
//import javax.swing.JToolBar;

import cmview.ContactMapPane;
import cmview.Start;
import cmview.datasources.Model;

public class ContactView extends JFrame implements ActionListener{
	
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
	// Select
	private static final String LABEL_SQUARE_SELECTION_MODE = "Square Selection Mode";
	private static final String LABEL_CLUSTER_SELECTION_MODE = "Cluster Selection Mode";
	
	// GUI components in the main frame
//	JToolBar toolBar;			// icon tool bar
	JPanel phiRul;				// Panel for top (phi) ruler
	JPanel thetaRul;			// Panel for left (theta) ruler	
	JPanel svP; 				// Main panel holding the Contact map pane Sperical Voxel view 
	JPanel tbPane;				// tool bar panel holding toolBar and cmp (necessary if toolbar is floatable)
	ContactStatusBar contStatBar;		// A status bar with metainformation on the right
	
	// indices of the all main menus in the frame's menu bar
	TreeMap<String, Integer> menu2idx;
	
	HashMap<JPopupMenu,JMenu> popupMenu2Parent;
	
	TreeMap<String,JMenu> smFile;
	TreeMap<String,JMenu> smCompare;
	
	// Menu items
	// M -> "menu bar"
	JMenuItem squareM, clusterM;
	// mm -> "main menu"
	JMenuItem mmInfo, mmSavePng, mmSaveSCsv, mmSaveTCsv, mmQuit;
	JMenuItem mmSelectAll;
	
	private Dimension screenSize;			// current size of this component on screen
	
	// Data and status variables
	private ContactGUIState guiState;
	private Model mod;
	public ContactMapPane cmPane;
	public ContactPane cPane;
	public AngleRuler phiRuler;
	public AngleRuler thetaRuler;
	
	/** Create a new View object */
	public ContactView(Model mod, String title, ContactMapPane cmPane) {
		super(title);
		Start.viewInstancesCreated();		
		this.guiState = new ContactGUIState(this);
		this.mod = mod;
		this.cmPane = cmPane;
//		Dimension dim = new Dimension(150, 250);
//		this.setPreferredSize(dim);
		
		if(mod == null) {
			this.setPreferredSize(new Dimension(Start.INITIAL_SCREEN_SIZE,Start.INITIAL_SCREEN_SIZE));
		}
		else{
			int xDim = ContactPane.defaultDim.width + ContactStatusBar.DEFAULT_WIDTH + AngleRuler.STD_RULER_WIDTH;
			int yDim = ContactPane.defaultDim.height + AngleRuler.STD_RULER_WIDTH;
			this.screenSize = new Dimension(xDim, yDim);
			this.setPreferredSize(this.screenSize);
		}
//		this.guiState = new GUIState(this);
		this.initGUI(); 							// build gui tree and pack
		setVisible(true);							// show GUI			
		
		// show status bar groups
		if(mod != null) {
			contStatBar.showDeltaRadiusGroup(true);
			contStatBar.showResolutionGroup(true);
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
		phiRul = new JPanel(new BorderLayout()); 	// panel holding the phi (top) ruler
		thetaRul = new JPanel(new BorderLayout()); 	// panel holding the theta (left) ruler
		contStatBar = new ContactStatusBar(this);	// pass reference to 'this' for handling gui actions
		
//		// Icons
		ImageIcon icon_square_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "shape_square.png"));
		ImageIcon icon_cluster_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "cog.png"));
		
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
		menu.add(submenu);
		smFile.put("Save", submenu);
//		// Print, Quit
//		mmQuit = makeMenuItem(LABEL_FILE_QUIT, null, menu);
		addToJMenuBar(menu);
		
//		// Select menu
		menu = new JMenu("Select");
		menu.setMnemonic(KeyEvent.VK_S);
		submenu = new JMenu("Selection Mode");
		squareM = makeMenuItem(LABEL_SQUARE_SELECTION_MODE, icon_square_sel_mode, submenu);
		clusterM = makeMenuItem(LABEL_CLUSTER_SELECTION_MODE, icon_cluster_sel_mode, submenu);		
		menu.add(submenu);
		addToJMenuBar(menu);
		
		// Creating contact map pane and ruler if model loaded
		if(mod != null) {	
			cPane = new ContactPane(this.mod, this.cmPane, this);
			cPane.setStatusBar(contStatBar);
			svP.add(cPane, BorderLayout.CENTER);
			
			phiRuler = new AngleRuler(this, cPane, AngleRuler.TOP);
			thetaRuler = new AngleRuler(this, cPane, AngleRuler.LEFT);
			phiRul.add(phiRuler);
			phiRul.setSize(phiRuler.getSize());
			phiRul.setPreferredSize(phiRuler.getSize());
			thetaRul.add(thetaRuler);
			thetaRul.setSize(thetaRuler.getSize());
			thetaRul.setPreferredSize(thetaRuler.getSize());
			
//			phiRul.setBorder(BorderFactory.createLineBorder(Color.black));
//			thetaRul.setBorder(BorderFactory.createLineBorder(Color.black));
		}
//		cPane.setVisible(false);
		if(guiState.getShowRulers()) {
			svP.add(phiRul, BorderLayout.NORTH);
			svP.add(thetaRul, BorderLayout.WEST);
		}
	
		// Add everything to the content pane				
		this.tbPane.add(svP,BorderLayout.CENTER);			
		this.tbPane.add(contStatBar,BorderLayout.EAST);
		this.getContentPane().add(tbPane, BorderLayout.CENTER);
		
		System.out.println("Sizes of panels:");
		System.out.println("tbPane: "+this.tbPane.getSize().getHeight()+"x"+this.tbPane.getSize().getWidth());
		System.out.println("svP: "+this.svP.getSize().getHeight()+"x"+this.svP.getWidth());
		System.out.println("cPane: "+this.cPane.getSize().getHeight()+"x"+this.cPane.getSize().getWidth());
		System.out.println("Size of phiRuler: "+phiRuler.getSize().height+"x"+phiRuler.getSize().width);
		System.out.println("Size of phiRul: "+phiRul.getSize().height+"x"+phiRul.getSize().width);
		System.out.println("Size of thetaRuler: "+thetaRuler.getRulerSize().height+"x"+thetaRuler.getRulerSize().width);
						
		pack();
	}

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
		if (e.getSource() == squareM) {
			guiState.setSelectionMode(ContactGUIState.SelMode.RECT);
		}
		// cluster button clicked
		if (e.getSource() == clusterM) {
			guiState.setSelectionMode(ContactGUIState.SelMode.CLUSTER);
		}
	}
	
	
	private void handleSaveToPng() {
		if(this.mod == null) {
			showNoContactWarning();
		} else {
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();
				if (confirmOverwrite(chosenFile)) {
					// Create a buffered image in which to draw
					BufferedImage bufferedImage = new BufferedImage(cmPane.getWidth(), cmPane.getHeight(), BufferedImage.TYPE_INT_RGB);

					// Create a graphics contents on the buffered image
					Graphics2D g2d = bufferedImage.createGraphics();
					g2d.setBackground(Color.white);
					g2d.setColor(Color.white);

					// Draw the current contact map window to Image
					cPane.paintComponent(g2d);

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
	
	private void handleSaveSphoxelsToCsv() {
		if(this.mod == null) {
			showNoContactWarning();
		} else {
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
		}
	}
	
	private void handleSaveTracesToCsv() {
		if(this.mod == null) {
			showNoContactWarning();
		} else {
			int ret = Start.getFileChooser().showSaveDialog(this);
			if(ret == JFileChooser.APPROVE_OPTION) {
				File chosenFile = Start.getFileChooser().getSelectedFile();
				System.out.println("File " + chosenFile.getPath());
				System.out.println("chosenFile= "+chosenFile.toString());
				if (confirmOverwrite(chosenFile)) {
					cPane.writeTraces(chosenFile.toString());
;				}
			}
		}
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
	 * Handles the user action to change the radius range 
	 * @param minR and maxR --> range
	 */
	public void handleChangeRadiusRange(float minR, float maxR) {
		this.cPane.setMinR(minR);
		this.cPane.setMaxR(maxR);
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

	public Dimension getScreenSize(){
//		System.out.println("screensize HxW: "+this.cPane.getScreenSize().height+"x"+this.cPane.getScreenSize().width);
//		System.out.println("screensize HxW: "+this.cPane.getSize().height+"x"+this.cPane.getSize().width);
//		return this.cPane.getSize();
//		System.out.println("getScreensize ContactView HxW: "+this.screenSize.height+"x"+this.screenSize.width);
		return this.screenSize;
	}
	
}
