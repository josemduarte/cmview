


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import tools.*;

/**
 * 
 * @author:	Juliane Dinse
 * Class: 	View
 * Package:	tools
 * Date:	20/02/2007
 * 
 * View creates the Contact Map and allows user-interaction, i.e. selections and fills.
 * these selections can be send to PyMol via "Send"-Button.
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
	private PyMol mypymol;
	private MyTestPyMol tpm;
	public int selval;
	public int selINK=0;
	
	public int[] pos = new int[2];
	public String[] text = new String[2];
	public String s1, s2;
	private MouseEvent evt;
	public String selectionType;



	public View(Start start, Model mod, String title, PaintController pc, PyMol mypymol){
		super(title);
		this.start= start;
		
		this.mod = mod;
		this.pc=pc;
		
		this.ViewInit();
		this.mypymol=mypymol;

	}
	
	public void ViewInit(){
		
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

		pc = new PaintController(start, mod, this);

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

		  if (e.getSource() == square){
			  System.out.println("geklickt S!");
			  
				selval = 1;
				selINK = selINK +1;
				selectionType = "Square";
				
		  }
		  
		  if (e.getSource() == fill){
			  System.out.println("geklickt F!");
			  
				selval = 2;
				selINK = selINK +1;
				selectionType = "Fill";
		  }
		  
		  if (e.getSource() == comNei){
			  
			  	selval = 3;
		  }
		  
		  if (e.getSource() == loadPDB){
			  
				tpm = new MyTestPyMol(start, mod, this, pc, mypymol);
				tpm.MyTestPyMolInit();
				
				   }
		  
		  if (e.getSource() == send){
			  
		
				   int selval = this.getValue();
				   switch(selval){
				   
				   case 1: tpm.SquareCommands();
				   case 2: tpm.FillCommands();
				   }
		  }
		  
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

