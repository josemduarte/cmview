


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 
 * @author:	Juliane Dinse
 * Class: 	View
 * Package:	cm2pymol
 * Date:	20/02/2007
 *
 */


public class View extends JFrame implements ActionListener{
	
	static final long serialVersionUID = 1l;
	
	JButton send, square, fill, loadPDB, comNei, triangle;
	JTextField tx, ty;
	public JPopupMenu popup;
	JMenuItem sendM, squareM, fillM, loadPDBM, comNeiM, triangleM;
	JMenuItem sendP, squareP, fillP, loadPDBP, comNeiP, triangleP;
	

	//JPanel bpl; // Button Panel
	JLabel bpl;
	JPanel cmp; // Contact Map Panel
	
	private Model mod;
	public PaintController pc;
	private MyTestPyMol tpm;
	private String pyMolServerUrl;
	public int selval;
	public int selINK=0;		 // incrementation numbering
	
	private String pdbCode;
	private String chainCode;
	private String pdbFileName;
	
	public int[] pos = new int[2];
	public String[] text = new String[2];
	public String s1, s2;
	//private MouseEvent evt;
	public String selectionType;
	public boolean sendpy;



	public View(Model mod, String title, String pyMolServerUrl,
			    String pdbCode, String chainCode, String fileName){
		super(title);
		this.mod = mod;
		this.pyMolServerUrl=pyMolServerUrl;
		this.pdbCode = pdbCode;
		this.chainCode = chainCode;
		this.pdbFileName = fileName;
		this.ViewInit();

	}
	
	public void ViewInit(){		
		// setting the layout 
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocation(20,20);

		/** Creating the Panels */
		//bpl = new JPanel(new GridLayout(2,3)); // Button Panel
		bpl = new JLabel("Click right mouse button for context menu");
		bpl.setAlignmentX(SwingConstants.LEFT);
		//bpl.setHorizontalTextPosition(JLabel.LEFT);
		//bpl.setHorizontalAlignment(JLabel.LEFT); 
		cmp= new JPanel(new BorderLayout()); // Contact Map Panel

		/** Creating the Buttons */
		square = new JButton("Square Selection on Map");
		fill = new JButton("Fill Selection on Map");
		loadPDB = new JButton("Load PDB File in PyMol");
		send = new JButton("Send Selection to PyMol");
		comNei = new JButton("Show Common Neighbours on Map");
		triangle = new JButton("Send Common Neighbours to PyMol");

		/** Adding the ActionListener */
		square.addActionListener(this);
		fill.addActionListener(this);
		comNei.addActionListener(this);
		loadPDB.addActionListener(this);
		send.addActionListener(this);
		triangle.addActionListener(this);
		
		/** Adding the Buttons to the Panels */

		bpl.add(square);
		bpl.add(fill);
		bpl.add(comNei);
		bpl.add(loadPDB);
		bpl.add(send);
		bpl.add(triangle);
		
		/* Adding the context menu */
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		popup = new JPopupMenu();
		
		ImageIcon icon1 = new ImageIcon("icons/shape_square.png");
		ImageIcon icon2 = new ImageIcon("icons/paintcan.png");
		ImageIcon icon3 = new ImageIcon("icons/shape_square_go.png");
		ImageIcon icon4 = new ImageIcon("icons/shape_flip_horizontal.png");
		ImageIcon icon5 = new ImageIcon("icons/shape_rotate_clockwise.png");
		ImageIcon icon6 = new ImageIcon("icons/picture_go.png");

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
		
		pc = new PaintController(mod, this);
		cmp.add(pc);
		
		/* create menu bar */
		JMenuBar menuBar;
		JMenu menu, submenu;
		JMenuItem menuItem;
		
		menuBar = new JMenuBar();
		
		// file menu
		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		submenu = new JMenu("Load from");
			submenu.setMnemonic(KeyEvent.VK_L);
			menuItem = new JMenuItem("Graph database");
			submenu.add(menuItem);
			menuItem = new JMenuItem("Pdbase");
			submenu.add(menuItem);
			menuItem = new JMenuItem("MSD");
			submenu.add(menuItem);
			menuItem = new JMenuItem("PDB file");
			submenu.add(menuItem);
			menuItem = new JMenuItem("Contact map file");
			submenu.add(menuItem);			
		menu.add(submenu);
		submenu = new JMenu("Save to");
			submenu.setMnemonic(KeyEvent.VK_S);
			menuItem = new JMenuItem("Graph database");
			submenu.add(menuItem);			
			menuItem = new JMenuItem("Contact map file");
			submenu.add(menuItem);
			menuItem = new JMenuItem("PNG file");
			submenu.add(menuItem);			
		menu.add(submenu);
		menuItem = new JMenuItem("Quit");
		menu.add(menuItem);
		menuBar.add(menu);
		
		// View menu
		menu = new JMenu("View");
		menu.setMnemonic(KeyEvent.VK_V);
		menuItem = new JMenuItem("Reset");
		menu.add(menuItem);
		menuItem = new JMenuItem("Color by contact type");
		menu.add(menuItem);
		menuBar.add(menu);
		
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
		menuItem = new JMenuItem("Help");
		menu.add(menuItem);			
		menuItem = new JMenuItem("About");
		menu.add(menuItem);		
		menuBar.add(menu);
		
		this.setJMenuBar(menuBar);
			
		/* Creating the vertical Boxes */
		Box verlBox = Box.createVerticalBox();
		verlBox.add(cmp, BorderLayout.CENTER);
		verlBox.add(bpl, BorderLayout.SOUTH);
		getContentPane().add(verlBox);
		pack();
		setVisible(true);
		}

	
	  public void actionPerformed (ActionEvent e) {
		  // square button clicked
		  if (e.getSource() == square || e.getSource() == squareM || e.getSource() == squareP) {
			  
				selval = 1;
				selINK = selINK +1;
				selectionType = "Squa";
				
		  }
		  // fill button clicked
		  if (e.getSource() == fill || e.getSource() == fillM || e.getSource() == fillP) {
			  
				selval = 2;
				selINK = selINK +1;
				selectionType = "Fill";
		  }
		  // showing com. Nei. button clicked
		  if (e.getSource() == comNei || e.getSource() == comNeiM || e.getSource() == comNeiP) {
			  
			  	selval = 3;
		  }
		  // loading pdb button clicked
		  if (e.getSource() == loadPDB || e.getSource() == loadPDBM || e.getSource() == loadPDBP) {
		
				// TODO: Move object creation to somewhere else
				tpm = new MyTestPyMol(this.pyMolServerUrl,
						              this.pdbCode, this.chainCode, this.pdbFileName);
				tpm.MyTestPyMolInit();
				
				   }
		  // send selection button clicked
		  if (e.getSource() == send || e.getSource() == sendM || e.getSource() == sendP) {
			  
			  	   sendpy =true;	
				   int selval = this.getValue();
				   switch(selval){		   
				   
				   case 1: tpm.SquareCommands(this.getSelNum(), pc.getSelectMatrix(), pc.getSelectRect());
				   case 2: tpm.FillCommands(this.getSelNum(), pc.getSelectMatrix(), mod.getMatrixSize());
				   }
		  }
		  // send com.Nei. button clicked
		  if(e.getSource()== triangle || e.getSource()== triangleM || e.getSource()== triangleP) {
			  
			  tpm.showTriangles(pc.getTriangleNumber(), pc.getResidues());
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

