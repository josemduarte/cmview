package cmview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

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
	private static final String LABEL_FILE_QUIT = "Quit";
	private static final String LABEL_PNG_FILE = "PNG File...";
	// Select
//	private static final String LABEL_CLUSTER_SELECTION_MODE = "Cluster Selection Mode";
	
	// GUI components in the main frame
	JToolBar toolBar;			// icon tool bar
	JPanel svP; 				// Main panel holding the Contact map pane Sperical Voxel view 
	JPanel tbPane;				// tool bar panel holding toolBar and cmp (necessary if toolbar is floatable)
	
	// indices of the all main menus in the frame's menu bar
	TreeMap<String, Integer> menu2idx;
	
	HashMap<JPopupMenu,JMenu> popupMenu2Parent;
	
	TreeMap<String,JMenu> smFile;
	TreeMap<String,JMenu> smCompare;
	
	// Menu items
	// M -> "menu bar"
	JMenuItem clusterM;
	// mm -> "main menu"
	JMenuItem mmInfo, mmSavePng, mmQuit;
	JMenuItem mmSelectAll;
	
	// Data and status variables
//	private GUIState guiState;
	private Model mod;
	public ContactMapPane cmPane;
	public ContactPane svPane;
	

	/** Create a new View object */
	public ContactView(Model mod, String title, ContactMapPane cmPane) {
		super(title);
		Start.viewInstancesCreated();
		this.mod = mod;
		this.cmPane = cmPane;
//		Dimension dim = new Dimension(150, 250);
//		this.setPreferredSize(dim);
		
		if(mod == null) {
			this.setPreferredSize(new Dimension(Start.INITIAL_SCREEN_SIZE,Start.INITIAL_SCREEN_SIZE));
		}
//		this.guiState = new GUIState(this);
		this.initGUI(); 							// build gui tree and pack
		setVisible(true);							// show GUI									
		
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
		this.setSize(150, 250);
		
		// Creating the Panels
		tbPane = new JPanel(new BorderLayout());	// toolbar pane, holding only toolbar and cmp (all the rest)
		svP = new JPanel(new BorderLayout()); 		// pane holding the cmPane and (optionally) the ruler panes
		
//		// Icons
//		ImageIcon icon_square_sel_mode = new ImageIcon(this.getClass().getResource(Start.ICON_DIR + "shape_square.png"));
		
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
		menu.add(submenu);
		smFile.put("Save", submenu);
		// Print, Quit
		mmQuit = makeMenuItem(LABEL_FILE_QUIT, null, menu);
		addToJMenuBar(menu);
		
//		// Select menu
//		menu = new JMenu("Select");
//		menu.setMnemonic(KeyEvent.VK_S);
//		submenu = new JMenu("Selection Mode");
//		clusterM = makeMenuItem(this.LABEL_CLUSTER_SELECTION_MODE, icon_square_sel_mode, submenu);
//		menu.add(submenu);
//		menu.addSeparator();
//		mmSelectAll = makeMenuItem("All Contacts", null, menu);		
//		addToJMenuBar(menu);
	
		tbPane.setSize(130, 230);
		// Creating contact map pane if model loaded
		if(mod != null) {			
			try {
				svPane = new ContactPane(this.mod, this.cmPane, this);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			svP.add(svPane);
		}

		// Add everything to the content pane		
//		this.tbPane.add(toolBar, BorderLayout.NORTH);			// tbPane is necessary if toolBar is floatable
		
		this.tbPane.add(svP,BorderLayout.CENTER);				// otherwise can add these to contentPane directly
//		this.tbPane.add(statusBar,BorderLayout.EAST);
		this.getContentPane().add(tbPane, BorderLayout.CENTER); // and get rid of this line
	
		pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

	
}
