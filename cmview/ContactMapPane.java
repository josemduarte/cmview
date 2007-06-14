package cmview;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import java.util.Hashtable;

import cmview.datasources.Model;
import proteinstructure.*;

/**
 * The panel containing the contact map and associated event handling.
 * 
 * @author:		Henning Stehr
 * Class: 		ContactMapPane (replaces PaintController)
 * Package: 	cmview
 * Date:		22/05/2007, last updated: 12/06/2007
 * 
 */

public class ContactMapPane extends JPanel
implements MouseListener, MouseMotionListener {

	// constants
	static final long serialVersionUID = 1l;

	private Point mousePressedPos;   // position where mouse where last pressed, used for start of square selection and for common neighbours 
	private Point mouseDraggingPos;  //  current position of mouse dragging, used for end point of square selection
	private Point pos;               //  current position of mouse
	
	private int winsize;          	// size of the effective square available on screen for drawing the contact map
	private int printsize;			// size of the effective square available for printing the contact map

	private double ratio;		  	// ratio of screen size and contact map size = size of each contact on screen

	private int contactMapSize;
	
	private boolean dragging;     	// set to true while the user is dragging (to display selection rectangle)
	private boolean mouseIn;		// true if the mouse is currently in the contact map window (otherwise supress crosshair)
	private boolean showCommonNeighbours; // true while common neighbourhoods should be drawn on screen
	private boolean printing;		// set to true by PrintUtil to notify paintComponent to render for printer rather than for screen
	
	private Model mod;
	private View view;

	private ContactList allContacts; // contains all contacts in contact map
	private ContactList selContacts; // contains permanent list of currently selected contacts
	private ContactList tmpContacts; // contains transient list of contacts selected while dragging
	private Hashtable<Contact,Color> contactColor;  // user defined contact colors

	
	/**
	 * Create a new ContactMapPane.
	 * @param mod
	 * @param view
	 */
	public ContactMapPane(Model mod, View view){
		this.mod = mod;
		this.view = view;
		addMouseListener(this);
		addMouseMotionListener(this);
		this.setOpaque(true); // make this component opaque
		this.setBorder(BorderFactory.createLineBorder(Color.black));
		
		this.allContacts = mod.getContacts();
		this.selContacts = new ContactList();
		this.tmpContacts = new ContactList();
		this.contactMapSize = mod.getMatrixSize();
		this.pos = new Point();
		this.mousePressedPos = new Point();
		this.mouseDraggingPos = new Point();
		
		this.dragging = false;
		this.showCommonNeighbours = false;
		this.printing = false;
		this.contactColor = new Hashtable<Contact, Color>();
	}

	/**
	 * Main method to draw the component on screen. This method is called each time the component has to be
	 * (re) drawn on screen. It is called automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	protected void paintComponent(Graphics g) {
		Graphics2D bufferGraphics = (Graphics2D) g.create();

		// paint background
		if (isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}

		// get output size and calculate scale factor
		int outputSize;
		if(printing) {
			// TODO: Instead of messing with the drawing size, draw to image, scale image and print
			outputSize = getPrintSize();
			//System.out.println("Setting size to " + getPrintSize() + " instead of " + getWindowSize());
			bufferGraphics.drawRect(0, 0, outputSize, outputSize);
		} else {
			outputSize = getWindowSize();
		}
		ratio = (double)outputSize/contactMapSize;		// scale factor, = size of one contact
		int contactSquareSize = (int)(ratio*1); // the size of the square representing a contact
		
		setBackground(Color.white);
		
//		// draw gridlines
//		bufferGraphics.setColor(Color.lightGray);
//		for(int i = 10; i < contactMapSize; i+=10) {
//			Point lowerRight = getCellLowerRight(new Contact(i,i));
//			bufferGraphics.drawLine(lowerRight.x, 0, lowerRight.x, outputSize);
//			bufferGraphics.drawLine(0, lowerRight.y, outputSize, lowerRight.y);
//		}			
		
//		// draw tickmarks
//		bufferGraphics.setColor(Color.black);
//		int tickmarkSize = 3;
//		for(int i = 1; i < contactMapSize; i++) {
//			if(i % 10 == 0) tickmarkSize = 6; else tickmarkSize = 3;
//			Point lowerRight = getCellLowerRight(new Contact(i,i));
//			bufferGraphics.drawLine(lowerRight.x, 0, lowerRight.x, tickmarkSize);
//			bufferGraphics.drawLine(lowerRight.x, outputSize-tickmarkSize, lowerRight.x, outputSize);
//			bufferGraphics.drawLine(0, lowerRight.y, tickmarkSize, lowerRight.y);
//			bufferGraphics.drawLine(outputSize-tickmarkSize, lowerRight.y, outputSize, lowerRight.y);
//
//		}	
		
		if (!view.getHighlightComNbh()){
			// drawing the contact map
			for (Contact cont:allContacts){ 
				// if there is a contact, draw a rectangle
				if(contactColor.containsKey(cont)) {
					bufferGraphics.setColor(contactColor.get(cont)); 
				} else {
					bufferGraphics.setColor(Color.black);
				}
				int x = cm2screen(cont).x;
				int y = cm2screen(cont).y;
				bufferGraphics.drawRect(x,y,contactSquareSize,contactSquareSize);
				bufferGraphics.fillRect(x,y,contactSquareSize,contactSquareSize);
			}
		} else {
			// showing all common neighbours
			for (Contact cont:view.getComNbhSizes().keySet()){
				int size = view.getComNbhSizes().get(cont);
				Point current = cm2screen(cont);
				int x = current.x;
				int y = current.y;
				if (allContacts.contains(cont)) {
					// coloring pinks when the cell is a contact, 1/size is simply doing the color grading: lower size lighter than higher size
					bufferGraphics.setColor(new Color(1.0f/(float) Math.sqrt(size), 0.0f, 1.0f/(float) Math.sqrt(size)));
				} else {
					// coloring greens when the cell is not a contact, 1/size is simply doing the color grading: lower size lighter than higher size
					bufferGraphics.setColor(new Color(0.0f, 1.0f/size,0.0f));
				}
				bufferGraphics.drawRect(x,y,contactSquareSize,contactSquareSize);
				bufferGraphics.fillRect(x,y,contactSquareSize,contactSquareSize);
			}
		}


		// drawing selection rectangle if dragging mouse and showing temp selection in red (tmpContacts)
		if (dragging && view.getCurrentAction()==View.SQUARE_SEL) {
			bufferGraphics.setColor(Color.black);
			int xmin = Math.min(mousePressedPos.x,mouseDraggingPos.x);
			int ymin = Math.min(mousePressedPos.y,mouseDraggingPos.y);
			int xmax = Math.max(mousePressedPos.x,mouseDraggingPos.x);
			int ymax = Math.max(mousePressedPos.y,mouseDraggingPos.y);
			bufferGraphics.drawRect(xmin,ymin,xmax-xmin,ymax-ymin);
			
			bufferGraphics.setColor(Color.red);
			for (Contact cont:tmpContacts){ 
				// if there is a contact, draw a rectangle
				int x = cm2screen(cont).x;
				int y = cm2screen(cont).y;
				bufferGraphics.drawRect(x,y,contactSquareSize,contactSquareSize);
				bufferGraphics.fillRect(x,y,contactSquareSize,contactSquareSize);
			}
		}

		// showing permanent selection in red
		bufferGraphics.setColor(Color.red);
		for (Contact cont:selContacts){
			int x = cm2screen(cont).x;
			int y = cm2screen(cont).y;
			bufferGraphics.drawRect(x,y,contactSquareSize,contactSquareSize);
			bufferGraphics.fillRect(x,y,contactSquareSize,contactSquareSize);
		}

		// draw diagonal
		
		// draw contacts as crosses
		
		// drawing coordinates on lower left corner (following crosshairs)
		drawCoordinates(bufferGraphics);
		
		if(this.showCommonNeighbours) {
			commonNeighbours(bufferGraphics);
			this.showCommonNeighbours = false;
		}

	}

	/** Method called by this component to determine its minimum size */
	public Dimension getMinimumSize() {
		return super.getMinimumSize();
	}

	/** Method called by this component to determine its preferred size */
	public Dimension getPreferredSize() {
		return new Dimension(800, 800);
	}

	/** Method called by this component to determine its maximum size */
	public Dimension getMaximumSize() {
		return super.getMaximumSize();
	}

	/** Called by paintComponent to get the current window size for drawing on screen */
	protected int getWindowSize(){
		winsize = Math.min(getWidth(), getHeight()); // size of drawing square
		return winsize;
	}
	
	/** Called by PrintUtil to set the right paper size for paintComponent */
	public void setPrintSize(double height, double width) {
		printsize = (int) Math.min(height, width);
	}
	
	/** Called by paintComponent to get the size of the effective square available for printing */ 
	private int getPrintSize() {
		return printsize;
	}

	/** Called by PrintUtil to tell paintComponent that it should use the printsize rather than winsize for drawing */
	public void setPrinting(boolean flag) {
		this.printing = flag;
	}
	
	/** Called by view to reset the color map */
	public void resetColorMap() {
		contactColor = new Hashtable<Contact, Color>();
	}
	
	/** Set the color value in contactColor for the currently selected residues to the given color */
	public void paintCurrentSelection(Color paintingColor) {
		for(Contact cont:selContacts) {
			contactColor.put(cont, paintingColor);
		}
		this.repaint();
	}
	
	/**
	 * Update tmpContact with the contacts contained in the rectangle given by the upperLeft and lowerRight.
	 * @param upperLeft
	 * @param lowerRight 
	 */
	public void squareSelect(){
		Contact upperLeft = screen2cm(mousePressedPos);
		Contact lowerRight = screen2cm(mouseDraggingPos);
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
	 * Update selContacts with the result of a fill selection starting from the given contact.
	 * @param cont contact where mouse has been clicked
	 * TODO: Create a tmpContacts first and then copy to selContacts (if we want this behaviour)
	 */
	private void fillSelect(Contact cont){
		int i = cont.i;
		int j = cont.j;
		if ((i < 1) || (j < 1) || (i > contactMapSize) || (j > contactMapSize)) {
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
	
	protected void selectNodeNbh(int i) {
		NodeNbh nbh = mod.getNodeNbh(i);
		System.out.println("Selecting node neighbourhood of node: "+i);
		System.out.println("Motif: "+nbh);
		System.out.print("Neighbours: ");
		for (int j:nbh.keySet()){
			System.out.print(j+" ");
			selContacts.add(new Contact(Math.min(i, j),Math.max(i, j)));
		}
		System.out.println();
		
	}

	/** Return the selContacts variable */
	public ContactList getSelContacts(){
		return selContacts;
	}

	protected void resetSelContacts() {
		this.selContacts = new ContactList();
	}
	
	/**
	 * Returns a contact given screen coordinates
	 * @param point
	 * @return
	 */
	private Contact screen2cm(Point point){
		return new Contact((int) Math.ceil(point.y/ratio),(int) Math.ceil(point.x/ratio));
	}

	/**
	 * Returns the residue serial given a 1-dimensional screen coordinate
	 * @param z
	 * @return
	 */
//	private int screen2cm(int z){
//		return (int) Math.ceil(z/ratio);
//	}

	/**
	 * Returns upper left corner of the square representing the contact
	 * @param cont
	 * @return
	 */
	private Point cm2screen(Contact cont){
		return new Point((int) Math.round((cont.j-1)*ratio), (int) Math.round((cont.i-1)*ratio));
	}
	
	/**
	 * Returns 1-dimensional screen coordinate given a contact map residue serial (node number)
	 * @param k
	 * @return
	 */
//	private int cm2screen(int k){
//		return (int) Math.round((k-1)*ratio);
//	}
	
	/** Gets the cell (contact rectangle) center given the upper left corner coordinates (i.e. the output of the cm2screen method
	 * @param point
	 * @return 
	 */
	//TODO is it ceiling or round that we want to use here??
	private Point getCellCenter(Point point){
		return new Point (point.x+(int)Math.ceil(ratio/2),point.y+(int)Math.ceil(ratio/2));
	}
	
	private Point getCellUpperRight(Contact cont){
		Point point = cm2screen(cont);
		return new Point (point.x+(int)Math.ceil(ratio),point.y);
	}

	private Point getCellLowerLeft(Contact cont){
		Point point = cm2screen(cont);
		return new Point (point.x,point.y+(int)Math.ceil(ratio));

	}

	private Point getCellLowerRight(Contact cont){
		Point point = cm2screen(cont);
		return new Point (point.x+(int)Math.ceil(ratio),point.y+(int)Math.ceil(ratio));

	}
	
	/** Returns the size in pixels of a single contact on screen 
	 * TODO: Check whether this number is really the number in pixels (and not plus or minus 1) */
	protected int getCellSize() {
		return (int) Math.round(ratio);
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

		mousePressedPos = evt.getPoint();

	}

	public void mouseReleased(MouseEvent evt) {
		// Called whenever the user releases the mouse button.

		if (evt.isPopupTrigger()) {
			showPopup(evt);
			return;
		}
		// only if release after left click (BUTTON1)
		if (evt.getButton()==MouseEvent.BUTTON1) {

			switch (view.getCurrentAction()) {
			case View.SHOW_COMMON_NBH:
				dragging = false;
				showCommonNeighbours = true;
				this.repaint();
				return;
				
			case View.SQUARE_SEL:
				if(!dragging) {
					// if clicked position is a selected contact, deselect it					
					Contact clicked = screen2cm(new Point(evt.getX(),evt.getY()));
					if(allContacts.contains(clicked)) {
						if(selContacts.contains(clicked)) {
							// if clicked position is a selected contact, deslect it
							if(evt.isControlDown()) {
								selContacts.remove(clicked);
							} else {
								selContacts = new ContactList();
								selContacts.add(clicked);
							}
						} else {
							// if clicked position is a contact but not selected, select it
							if(!evt.isControlDown()) {
								selContacts = new ContactList();
							}
							selContacts.add(clicked);
						}
					} else {
						// else: if clicked position is outside of a contact, reset selContacts
						selContacts = new ContactList();
					}
					this.repaint();
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
				dragging = false;
				// resets selContacts when clicking mouse
				if (!evt.isControlDown()){
					selContacts = new ContactList();
				}
				fillSelect(screen2cm(new Point(evt.getX(),evt.getY())));
				this.repaint();
				return;

			case View.NODE_NBH_SEL:
				dragging = false;
				// resets selContacts when clicking mouse
				if (!evt.isControlDown()){
					selContacts = new ContactList();
				}
				// we select the node neighbourhoods of both i and j of the mousePressedPos
				Contact cont = screen2cm(mousePressedPos);
				if (cont.j>cont.i){ // only if we clicked on the upper side of the matrix
					selectNodeNbh(cont.i);
					selectNodeNbh(cont.j);
				}
				this.repaint();
				return;

			}
			

		}
	}

	public void mouseDragged(MouseEvent evt) {
		// Called whenever the user moves the mouse
		// while a mouse button is held down. 

		dragging = true;
		mouseDraggingPos = evt.getPoint();

		squareSelect();

		mouseMoved(evt); //TODO is this necessary? I tried getting rid of it but wasn't quite working
	} 

	public void mouseEntered(MouseEvent evt) { 
		mouseIn = true;
	}
	
	public void mouseExited(MouseEvent evt) {
		mouseIn = false;
		this.repaint();
	}
	
	public void mouseClicked(MouseEvent evt) {
	}
	
	public void mouseMoved(MouseEvent evt) {
		pos = evt.getPoint();
		this.repaint();
	}

	public void showPopup(MouseEvent e) {
		view.popup.show(e.getComponent(), e.getX(), e.getY());
	}

	protected void drawCoordinates(Graphics2D bufferGraphics){

		if ((mouseIn == true) && (pos.x <= winsize) && (pos.y <= winsize)){
			Contact currentCell = screen2cm(pos);
			String i_res = mod.getResType(currentCell.i);
			String j_res = mod.getResType(currentCell.j);
			// writing the coordinates at lower left corner
			bufferGraphics.setColor(Color.blue);
			bufferGraphics.drawString("i", 20, winsize-70);
			bufferGraphics.drawString("j", 60, winsize-70);
			bufferGraphics.drawString(currentCell.i+"", 20, winsize-50);
			bufferGraphics.drawString(currentCell.j+"", 60, winsize-50);
			bufferGraphics.drawString(i_res==null?"?":i_res, 20, winsize-30);
			bufferGraphics.drawString(j_res==null?"?":j_res, 60, winsize-30);
			if(allContacts.contains(currentCell)) {
				bufferGraphics.drawLine(48, winsize-35, 55, winsize-35);
			}

			if (view.getShowPdbSers()){
				String i_pdbresser = mod.getPdbResSerial(currentCell.i);
				String j_pdbresser = mod.getPdbResSerial(currentCell.j);
				bufferGraphics.drawString(i_pdbresser==null?"?":i_pdbresser, 20, winsize-10);
				bufferGraphics.drawString(j_pdbresser==null?"?":j_pdbresser, 60, winsize-10);
			}
			// drawing the cross-hair
			bufferGraphics.setColor(Color.green);
			bufferGraphics.drawLine(pos.x, 0, pos.x, winsize);
			bufferGraphics.drawLine(0, pos.y, winsize, pos.y);
		}

	}

	private void commonNeighbours(Graphics2D bufferGraphics){
		// getting point where mouse was clicked and common neighbours for it
		Contact cont = screen2cm(mousePressedPos); 
		EdgeNbh comNbh = mod.getEdgeNbh (cont.i,cont.j);

		System.out.println("Selecting common neighbours for contact "+cont);
		System.out.println("Motif: "+comNbh);
		// drawing corridor
		drawCorridor(cont, bufferGraphics);
		
		// marking the selected point with a cross
		drawCrossOnContact(cont, bufferGraphics, Color.yellow);
		System.out.print("Common neighbours: ");
		// drawing triangles
		for (int k:comNbh.keySet()){ // k is each common neighbour (residue serial)
			System.out.print(k+" ");
			if (k>cont.i && k<cont.j) {
				//draw cyan triangles for neighbours within the box 
				drawTriangle(k, cont, bufferGraphics, Color.cyan);
			}
			else { // i.e. k<cont.i || k>cont.j
				//draw red triangles for neighbours out of the box 
				drawTriangle(k, cont, bufferGraphics, Color.red);
			}
		}
		System.out.println();
	}

	private void drawCrossOnContact(Contact cont, Graphics2D bufferGraphics,Color color){
		bufferGraphics.setColor(color);
		if (ratio<6){ // if size of square is too small, then use fixed size 3 in each side of the cross
			Point center = getCellCenter(cm2screen(cont)); 
			bufferGraphics.drawLine(center.x-3, center.y-3,center.x+3, center.y+3 );
			bufferGraphics.drawLine(center.x-3, center.y+3,center.x+3, center.y-3 );
			bufferGraphics.drawLine(center.x-2, center.y-3,center.x+2, center.y+3 );
			bufferGraphics.drawLine(center.x-2, center.y+3,center.x+2, center.y-3 );	
		} else { // otherwise get upper left, lower left, upper right, lower right to draw a cross spanning the whole contact square
			Point ul = cm2screen(cont);
			Point ur = getCellUpperRight(cont);
			Point ll = getCellLowerLeft(cont);
			Point lr = getCellLowerRight(cont);
			bufferGraphics.drawLine(ul.x,ul.y,lr.x,lr.y);
			bufferGraphics.drawLine(ll.x,ll.y, ur.x,ur.y);
			bufferGraphics.drawLine(ul.x+1,ul.y,lr.x-1,lr.y);
			bufferGraphics.drawLine(ll.x+1,ll.y, ur.x-1,ur.y);
		}
	}

	private void drawCorridor(Contact cont, Graphics2D bufferGraphics){
		Point point = getCellCenter(cm2screen(cont));
		int x = point.x;
		int y = point.y;
		bufferGraphics.setColor(Color.green);
		// Horizontal Line
		bufferGraphics.drawLine(0, x, x, x);
		// vertical Line
		bufferGraphics.drawLine(y,y,y,winsize);
	}
	
	private void drawTriangle(int k, Contact cont, Graphics2D bufferGraphics,Color color) {
		int i = cont.i;
		int j = cont.j;
		// we put the i,k and j,k contacts in the right side of the contact map (upper side, i.e.j>i)
		Contact ikCont = new Contact(Math.min(i, k), Math.max(i, k));
		Contact jkCont = new Contact(Math.min(j, k), Math.max(j, k));
		
		// we mark the 2 edges i,k and j,k with a cross
		this.drawCrossOnContact(ikCont, bufferGraphics, Color.yellow);
		this.drawCrossOnContact(jkCont, bufferGraphics, Color.yellow);

		// transforming to screen coordinates
		Point point = getCellCenter(cm2screen(cont));
		Point ikPoint = getCellCenter(cm2screen(ikCont));
		Point jkPoint = getCellCenter(cm2screen(jkCont));
		
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
		Point kkPoint = getCellCenter(cm2screen(kkCont));
		Point endPoint = new Point();
		if (k<(j+i)/2) endPoint = getCellCenter(cm2screen(new Contact(j,k))); // if k below center of segment i->j, the endpoint is j,k i.e. we draw a vertical line
		if (k>(j+i)/2) endPoint = getCellCenter(cm2screen(new Contact(k,i))); // if k above center of segment i->j, the endpoint is k,i i.e. we draw a horizontal line
		bufferGraphics.setColor(Color.lightGray);
		bufferGraphics.drawLine(kkPoint.x, kkPoint.y, endPoint.x, endPoint.y);
	}

	public EdgeNbh getCommonNbh(){
		Contact cont = screen2cm(mousePressedPos); 
		return mod.getEdgeNbh (cont.i,cont.j);
	}
		
} 

