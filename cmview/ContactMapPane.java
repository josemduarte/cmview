package cmview;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.BorderFactory;

import cmview.datasources.Model;
import proteinstructure.*;

/**
 * The panel containing the contact map and associated event handling.
 * 
 * @author:		Henning Stehr
 * Class: 		ContactMapPane (replaces PaintController)
 * Package: 	cm2pymol
 * Date:		22/05/2007, last updated: 22/05/2007
 * 
 * TODO:
 * - use JPanel instead of Canvas
 * - draw crosshair and coordinates on separate Panel so that the contact map can be saved as image
 */

public class ContactMapPane extends JPanel
implements MouseListener, MouseMotionListener {

	// constants
	static final long serialVersionUID = 1l;

	//private int xs, ys;              //  x, y coordinates of start point of common neighbours
	private Point squareSelStart;   //  start point of square selection, TODO must change the name of this, it's used for square selection and for common neighbours 
	private Point squareSelEnd;     //  end point of square selection
	private Point pos;              //  current position of mouse
	
	private int winsize;          // size of the effective (square) size

	private double ratio;		  // scale factor
	//private int trinum;		  // number of the current triangle

	private boolean dragging;     // set to true while the user is dragging

	private boolean mouseIn;
	//private Model mod;          //TODO not sure if we need the full Model object here, commenting it out for now
	private Graph graph;
	private View view;
	private boolean showCommonNeighbours = false;

	private int dim;


	private ContactList allContacts; // contains all contacts in contact map
	private ContactList selContacts; // contains permanent list of contacts selected
	private ContactList tmpContacts; // contains transient list of contacts selected while dragging

	//private int[][] resi = new int[20][];

	/**
	 * Constructor
	 * @param mod
	 * @param view
	 */
	public ContactMapPane(Model mod, View view){
		//this.mod = mod;
		this.view = view;
		addMouseListener(this);
		addMouseMotionListener(this);
		//mod.ModelInit();
		this.setOpaque(true); // make this component opaque
		this.setBorder(BorderFactory.createLineBorder(Color.black));
		
		// TODO we don't need to pass the Model at all but rather just the Graph object, isn't it?
		this.graph = mod.getGraph();
		this.allContacts = graph.getContacts();
		this.selContacts = new ContactList();
		this.dim = graph.fullLength;
		this.pos = new Point();
		
		//this.trinum = 0;
	}

	protected void paintComponent(Graphics g) {
		Graphics2D bufferGraphics = (Graphics2D) g.create();

		// paint background
		if (isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}

		int windowSize = this.getWindowSize();

		ratio = (double)windowSize/dim;		// scale factor, = size of one contact

		setBackground(Color.white);

		// drawing the contact map (initially and on repaint)
		bufferGraphics.setColor(Color.black);
		for (Contact cont:allContacts){ 
			// if there is a contact, draw a rectangle
			int x = cm2screen(cont).x;
			int y = cm2screen(cont).y;
			bufferGraphics.drawRect(x,y,(int)(ratio*1),(int)(ratio*1));
			bufferGraphics.fillRect(x,y,(int)(ratio*1),(int)(ratio*1));
		}

		// drawing selection rectangle if dragging mouse and showing temp selection in red (tmpContacts)
		if (dragging) {
			bufferGraphics.setColor(Color.black);
			int xmin = Math.min(squareSelStart.x,squareSelEnd.x);
			int ymin = Math.min(squareSelStart.y,squareSelEnd.y);
			int xmax = Math.max(squareSelStart.x,squareSelEnd.x);
			int ymax = Math.max(squareSelStart.y,squareSelEnd.y);
			bufferGraphics.drawRect(xmin,ymin,xmax-xmin,ymax-ymin);
			
			bufferGraphics.setColor(Color.red);
			for (Contact cont:tmpContacts){ 
				// if there is a contact, draw a rectangle
				int x = cm2screen(cont).x;
				int y = cm2screen(cont).y;
				bufferGraphics.drawRect(x,y,(int)(ratio*1),(int)(ratio*1));
				bufferGraphics.fillRect(x,y,(int)(ratio*1),(int)(ratio*1));
			}
		}

		// showing permanent selection in red
		bufferGraphics.setColor(Color.red);
		for (Contact cont:selContacts){
			int x = cm2screen(cont).x;
			int y = cm2screen(cont).y;
			bufferGraphics.drawRect(x,y,(int)(ratio*1),(int)(ratio*1));
			bufferGraphics.fillRect(x,y,(int)(ratio*1),(int)(ratio*1));
		}

		// drawing coordinates on lower left corner (following crosshairs)
		drawCoordinates(bufferGraphics);
		
		if(this.showCommonNeighbours) {
			commonNeighbours(bufferGraphics);
			this.showCommonNeighbours = false;
		}

	}


	public Dimension getMinimumSize() {
		return super.getMinimumSize();
	}

	public Dimension getPreferredSize() {
		return new Dimension(800, 800);
	}

	public Dimension getMaximumSize() {
		return super.getMaximumSize();
	}

	/** 
	 * used by paintComponent to get the current window size 
	 */
	private int getWindowSize(){
		winsize = Math.min(getWidth(), getHeight()); // size of drawing square
		return winsize;
	}

	/**
	 * Square selection: from left upper to right lower corner of a rectangle
	 * @param upperLeft
	 * @param lowerRight 
	 */
	public void squareSelect(Contact upperLeft, Contact lowerRight){
		// we reset the tmpContacts list so every new mouse selection starts from a blank list
		tmpContacts = new ContactList();

		int imin = Math.min(upperLeft.i,lowerRight.i);
		int jmin = Math.min(upperLeft.j,lowerRight.j);
		int imax = Math.max(upperLeft.i,lowerRight.i);
		int jmax = Math.max(upperLeft.j,lowerRight.j);
		// we loop over all contacts so time is o(number of contacts) instead of looping over the square (o(n2) being n size of square)
		for (Contact cont:allContacts){
			if (cont.i<=imax && cont.i>=imin && cont.j<=jmax && cont.j>=jmin){
				tmpContacts.add(cont);
			}
		}
	}

	/**
	 * Fill selection
	 * @param cont contact where mouse has been clicked
	 */
	public void fillSelect(Contact cont){
		int i = cont.i;
		int j = cont.j;
		if ((i < 1) || (j < 1) || (i > dim) || (j > dim)) {
			return;
		} else {
			if (!allContacts.contains(cont)){
				return;
			}

			if (selContacts.contains(cont)){
				return;
			}
			else {
				selContacts.add(cont);

				// 1 distance
				fillSelect(new Contact(i-1,j));
				fillSelect(new Contact(i+1,j));
				fillSelect(new Contact(i,j-1));
				fillSelect(new Contact(i,j+1));

				// 2 distance
				fillSelect(new Contact(i-2,j));
				fillSelect(new Contact(i+2,j));
				fillSelect(new Contact(i,j-2));
				fillSelect(new Contact(i,j+2));
				fillSelect(new Contact(i-1,j+1));
				fillSelect(new Contact(i+1,j+1));
				fillSelect(new Contact(i-1,j-1));
				fillSelect(new Contact(i+1,j-1));
			}
		}
	}

	public ContactList getSelContacts(){
		return selContacts;
	}

	/**
	 * Returns a contact given screen coordinates
	 * @param dim
	 * @return
	 */
	private Contact screen2cm(Point point){
		return new Contact((int) Math.ceil(point.y/ratio),(int) Math.ceil(point.x/ratio));
	}

	private int screen2cm(int z){
		return (int) Math.ceil(z/ratio);
	}

	/**
	 * Returns upper left corner of the square representing the contact
	 * @param cont
	 * @return
	 */
	private Point cm2screen(Contact cont){
		return new Point((int) Math.round((cont.j-1)*ratio), (int) Math.round((cont.i-1)*ratio));
	}
	
	private int cm2screen(int k){
		return (int) Math.round((k-1)*ratio);
	}

	/** ############################################### */
	/** ############    MOUSE EVENTS   ################ */
	/** ############################################### */   

	public void mousePressed(MouseEvent evt) {
		// This is called when the user presses the mouse anywhere
		// in the frame

		if (evt.isPopupTrigger()) {
			showPopup(evt);
			return;
		}
		//xs = evt.getX();
		//ys = evt.getY();
		squareSelStart = new Point(evt.getX(),evt.getY());

	}

	public void mouseReleased(MouseEvent evt) {
		// Called whenever the user releases the mouse button.

		if (evt.isPopupTrigger()) {
			showPopup(evt);
			return;
		}
		// from now only if left click (BUTTON1)
		if (evt.getButton()==MouseEvent.BUTTON1) {

			switch (view.getCurrentAction()) {
			case View.SHOW_COMMON_NBH:
				showCommonNeighbours = true;
				this.repaint();
				return;
				
			case View.SQUARE_SEL:
				if(!dragging) {
					// resets selContacts when clicking mouse (not dragging)
					selContacts = new ContactList();
				} else {
					if (evt.isControlDown()){
						selContacts.addAll(tmpContacts);
					} else{
						selContacts = new ContactList();
						selContacts.addAll(tmpContacts);
					}
				}
				dragging = false;
				return;
				
			case View.FILL_SEL:
				// resets selContacts when clicking mouse
				if (!evt.isControlDown()){
					selContacts = new ContactList();
				}
				fillSelect(screen2cm(new Point(evt.getX(),evt.getY())));
				return;
			}

		}
	}

	public void mouseDragged(MouseEvent evt) {
		// Called whenever the user moves the mouse
		// while a mouse button is held down. 

		dragging = true;
		squareSelEnd = new Point(evt.getX(),evt.getY());

		squareSelect(screen2cm(squareSelStart), screen2cm(squareSelEnd));
		
		mouseMoved(evt);
	} 

	public void mouseEntered(MouseEvent evt) { 
		mouseIn=true;
	}
	
	public void mouseExited(MouseEvent evt) {
	}
	
	public void mouseClicked(MouseEvent evt) {
	}
	
	public void mouseMoved(MouseEvent evt) {
		pos = new Point(evt.getX(), evt.getY());
		this.repaint();
	}

	public void showPopup(MouseEvent e) {
		view.popup.show(e.getComponent(), e.getX(), e.getY());
	}

	public void drawCoordinates(Graphics2D bufferGraphics){

		bufferGraphics.setColor(Color.red);

		if ((mouseIn == true) && (pos.x <= winsize) && (pos.y <= winsize)){

			// writing the coordinates at lower left corner
			bufferGraphics.setColor(Color.blue);
			bufferGraphics.drawString("( Y   " + ",  X )", 5, winsize-50);
			bufferGraphics.drawString("(i_num   " + ",  j_num )", 5, winsize-30);
			bufferGraphics.drawString("(" + (int)(1+pos.y/ratio)+"," + (int)(1+pos.x/ratio)+")", 5, winsize-10);

			// drawing the cross-hair
			bufferGraphics.setColor(Color.green);
			bufferGraphics.drawLine(pos.x, 0, pos.x, winsize);
			bufferGraphics.drawLine(0, pos.y, winsize, pos.y);
		}

	}

	public void commonNeighbours(Graphics2D bufferGraphics){
		// getting point where mouse was clicked and common neighbours for it
		Contact cont = screen2cm(squareSelStart); //TODO squareSelStart variable should be renamed
		EdgeNbh comNbh = graph.getEdgeNbh (cont.i,cont.j);

		// drawing corridor
		drawCorridor(cont, bufferGraphics);
		
		// marking the selected point with a cross
		markPointWithCross(cont, bufferGraphics);
		
		// drawing triangles
		for (int k:comNbh.keySet()){ // k is each common neighbour (residue serial)
			if (k>cont.i && k<cont.j) {
				//draw cyan triangles for neighbours within the box 
				drawTriangle(k, cont, bufferGraphics, Color.cyan);
			}
			else { // i.e. k<cont.i || k>cont.j
				//draw red triangles for neighbours out of the box 
				drawTriangle(k, cont, bufferGraphics, Color.red);
			}
		}

	}

	public void markPointWithCross(Contact cont, Graphics2D bufferGraphics){
		Point point = cm2screen(cont);
		int x = point.x;
		int y = point.y;
		bufferGraphics.setColor(Color.blue);
		bufferGraphics.drawLine(x-3, y-3,x+3, y+3 );
		bufferGraphics.drawLine(x-3, y+3,x+3, y-3 );
		bufferGraphics.drawLine(x-2, y-3,x+2, y+3 );
		bufferGraphics.drawLine(x-2, y+3,x+2, y-3 );
	}

	public void drawCorridor(Contact cont, Graphics2D bufferGraphics){
		Point point = cm2screen(cont);
		int x = point.x;
		int y = point.y;
		bufferGraphics.setColor(Color.gray);
		// Horizontal Lines
		bufferGraphics.drawLine(0, y, y, y);
		bufferGraphics.setColor(Color.green);
		bufferGraphics.drawLine(0, x, x, x);
		// vertical Lines
		bufferGraphics.drawLine(y,y,y,cm2screen(dim));
		bufferGraphics.setColor(Color.gray);
		bufferGraphics.drawLine(x,x,x,cm2screen(dim)); 
	}
	
	public void drawTriangle(int k, Contact cont, Graphics2D bufferGraphics,Color color) {
		int i = cont.i;
		int j = cont.j;
		// we put the i,k and j,k contacts in the right side of the contact map (upper side, i.e.j>i)
		Contact ikCont = new Contact(Math.min(i, k), Math.max(i, k));
		Contact jkCont = new Contact(Math.min(j, k), Math.max(j, k));
		
		// we mark the 2 edges i,k and j,k with a cross
		bufferGraphics.setColor(Color.blue);
		this.markPointWithCross(ikCont, bufferGraphics);
		this.markPointWithCross(jkCont, bufferGraphics);

		// transforming to screen coordinates
		Point point = cm2screen(cont);
		Point ikPoint = cm2screen(ikCont);
		Point jkPoint = cm2screen(jkCont);
		
		// drawing triangle
		bufferGraphics.setColor(color);		
		// line between edges i,j and i,k
		bufferGraphics.drawLine(point.x, point.y, ikPoint.x, ikPoint.y);
		// line between edges i,j and j,k
		bufferGraphics.drawLine(point.x, point.y, jkPoint.x, jkPoint.y);
		// line between edges i,k and j,k
		bufferGraphics.drawLine(ikPoint.x, ikPoint.y, jkPoint.x, jkPoint.y);

		// drawing light gray common neighbour corridor markers
		Contact kkCont = new Contact(k,k); // k node point in the diagonal: the start point of the light gray corridor
		Point kkPoint = cm2screen(kkCont);
		Point endPoint = new Point();
		if (k<j) endPoint = cm2screen(new Contact(j,k)); // if k below j, the endpoint is j,k i.e. we draw a vertical line
		if (k>j) endPoint = cm2screen(new Contact(k,i)); // if k above j, the endpoint is k,i i.e. we draw a horizontal line
		bufferGraphics.setColor(Color.lightGray);
		bufferGraphics.drawLine(kkPoint.x, kkPoint.y, endPoint.x, endPoint.y);
	}

	public EdgeNbh getCommonNbh(){
		Contact cont = screen2cm(squareSelStart); //TODO squareSelStart variable should be renamed
		return graph.getEdgeNbh (cont.i,cont.j);
	}
	
} 

