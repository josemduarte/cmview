


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 
 * @author:	Juliane Dinse
 * Class: 	View
 * Package:	CM2PyMol
 * Date:	20/02/2007
 * 
 * tasks:
 * - preparing visualisation of contact map by providing a panel
 * - implements the ActionListener --> Buttons for different selection options
 * - if button is clicked --> sending signals to other programs
 *
 */


public class View extends JFrame implements ActionListener{
	
	static final long serialVersionUID = 1l;
	
	JButton send, square, fill, loadPDB, comNei, triangle;
	JTextField tx, ty;
	public JPopupMenu popup;
	JMenuItem sendM, squareM, fillM, loadPDBM, comNeiM, triangleM;

	JPanel bpl; // Button Panel
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
		this.ViewInit();
		this.pyMolServerUrl=pyMolServerUrl;
		this.pdbCode = pdbCode;
		this.chainCode = chainCode;
		this.pdbFileName = fileName;
		
		// send structure to pymol - why does this not work here?
		// TODO: Test for success
		//tpm = new MyTestPyMol(start, mod, this, pc, pymol, this.pyMolServerUrl);
		//tpm.MyTestPyMolInit();

	}
	
	public void ViewInit(){		
		// setting the layout 
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocation(20,20);

		/** Creating the Panels */
		bpl = new JPanel(new GridLayout(2,3)); // Button Panel
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
		
		popup.add(squareM);
		popup.add(fillM);
		popup.add(sendM);
		popup.add(comNeiM);
		popup.add(triangleM);
		popup.add(loadPDBM);
		
		pc = new PaintController(mod, this);
		cmp.add(pc);
		
		/** Creating the vertical Boxes */
		Box verlBox = Box.createVerticalBox();
		verlBox.add(cmp, BorderLayout.CENTER);
		verlBox.add(bpl, BorderLayout.SOUTH);
		getContentPane().add(verlBox);
		pack();
		setVisible(true);
		}

	
	  public void actionPerformed (ActionEvent e) {
		  // square button clicked
		  if (e.getSource() == square || e.getSource() == squareM) {
			  
				selval = 1;
				selINK = selINK +1;
				selectionType = "Squa";
				
		  }
		  // fill button clicked
		  if (e.getSource() == fill || e.getSource() == fillM) {
			  
				selval = 2;
				selINK = selINK +1;
				selectionType = "Fill";
		  }
		  // showing com. Nei. button clicked
		  if (e.getSource() == comNei || e.getSource() == comNeiM) {
			  
			  	selval = 3;
		  }
		  // loading pdb button clicked
		  if (e.getSource() == loadPDB || e.getSource() == loadPDBM) {
		
				// TODO: Move object creation to somewhere else
				tpm = new MyTestPyMol(mod, this, pc, this.pyMolServerUrl,
						              this.pdbCode, this.chainCode, this.pdbFileName);
				tpm.MyTestPyMolInit();
				
				   }
		  // send selection button clicked
		  if (e.getSource() == send || e.getSource() == sendM) {
			  
			  	   sendpy =true;	
				   int selval = this.getValue();
				   switch(selval){
				   
				   case 1: tpm.SquareCommands();
				   case 2: tpm.FillCommands();
				   }
		  }
		  // send com.Nei. button clicked
		  if(e.getSource()== triangle || e.getSource()== triangleM) {
			  tpm.showTriangles();
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

