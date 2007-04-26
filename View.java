


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import tools.*;

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
	JButton send, square, fill, loadPDB, comNei, triangle;
	JTextField tx, ty;

	JPanel bpl; // Button Panel
	JPanel cmp; // Contact Map Panel
	
	private Start start;
	private Model mod;
	private View view;
	public PaintController pc;
	private PyMol pymol;
	private MyTestPyMol tpm;
	private String pyMolServerUrl;
	public int selval;
	public int selINK=0;		 // incrementation numbering
	
	public int[] pos = new int[2];
	public String[] text = new String[2];
	public String s1, s2;
	//private MouseEvent evt;
	public String selectionType;
	public boolean sendpy;



	public View(Start start, Model mod, String title, PaintController pc, PyMol pymol, String pyMolServerUrl){
		super(title);
		this.start= start;
		this.mod = mod;
		this.pc=pc;
		this.ViewInit();
		this.pymol=pymol;
		this.pyMolServerUrl=pyMolServerUrl;

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
		
		pc = new PaintController(start, mod, this);
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
		  if (e.getSource() == square){
			  
				selval = 1;
				selINK = selINK +1;
				selectionType = "Squa";
				
		  }
		  // fill button clicked
		  if (e.getSource() == fill){
			  
				selval = 2;
				selINK = selINK +1;
				selectionType = "Fill";
		  }
		  // showing com. Nei. button clicked
		  if (e.getSource() == comNei){
			  
			  	selval = 3;
		  }
		  // loading pdb button clicked
		  if (e.getSource() == loadPDB){
			  
				tpm = new MyTestPyMol(start, mod, this, pc, pymol, this.pyMolServerUrl);
				tpm.MyTestPyMolInit();
				
				   }
		  // send selection button clicked
		  if (e.getSource() == send){
			  
			  	   sendpy =true;	
				   int selval = this.getValue();
				   switch(selval){
				   
				   case 1: tpm.SquareCommands();
				   case 2: tpm.FillCommands();
				   }
		  }
		  // send com.Nei. button clicked
		  if(e.getSource()== triangle){
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

