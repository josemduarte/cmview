package cmview;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.util.Hashtable;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import proteinstructure.Contact;
import proteinstructure.ContactList;
import proteinstructure.EdgeNbh;
import proteinstructure.NodeNbh;
import cmview.datasources.Model;

/**
 * The panel containing the contact map and associated event handling.
 */
public class ContactMapPane extends JPanel
implements MouseListener, MouseMotionListener, ComponentListener {

	// constants
	static final long serialVersionUID = 1l;

	private Point mousePressedPos;   // position where mouse where last pressed, used for start of square selection and for common neighbours 
	private Point mouseDraggingPos;  //  current position of mouse dragging, used for end point of square selection
	private Point pos;               //  current position of mouse
	private int currentRulerCoord;	 // the ruler serial that is shown if showRulerSer=true
	private Contact rightClickCont;	 // position in contact map where right mouse button was pressed to open context menu
	private EdgeNbh currCommonNbh;	 // common nbh that the user selected last (used to send it to pymol)
	private int lastMouseButtonPressed; // mouse button which was pressed last (being updated by MousePressed)
	
	private Dimension screenSize;	// current size of this component on screen
	private int winsize;          	// size of the effective square available on screen for drawing the contact map
	private int printsize;			// size of the effective square available for printing the contact map

	private double ratio;		  	// ratio of screen size and contact map size = size of each contact on screen

	private int contactMapSize;
	
	private boolean dragging;     	// set to true while the user is dragging (to display selection rectangle)
	private boolean mouseIn;		// true if the mouse is currently in the contact map window (otherwise supress crosshair)
	private boolean showCommonNeighbours; // while true, common neighbourhoods are drawn on screen
	private boolean showRulerCoord; // while true, current ruler coordinate will be shown instead of usual coordinates
	private boolean printing;		// set to true by PrintUtil to notify paintComponent to render for printer rather than for screen
	
	private Model mod;
	private View view;

	private ContactList allContacts; // contains all contacts in contact map
	private ContactList selContacts; // contains permanent list of currently selected contacts
	private ContactList tmpContacts; // contains transient list of contacts selected while dragging
	private Hashtable<Contact,Color> userContactColors;  // user defined colors for individual contacts
	private double[][] densityMatrix; // matrix of contact density
	
	// drawing colors
	private Color backgroundColor;	  // background color
	private Color contactColor;	  	  // color for contacts
	private Color selectionColor;	  // color for selected contacts
	private Color selRectColor;	  // color of selection rectangle
	private Color crosshairColor;     // color of crosshair
	private Color diagCrosshairColor; // color of "diagonal crosshair"
	private Color coordinatesColor;	  // color of coordinates
	private Color inBoxTriangleColor; // color for common nbh triangles
	private Color outBoxTriangleColor;// color for common nbh triangles
	private Color crossOnContactColor;// color for crosses on common nbh contacts
	private Color corridorColor;	  // color for contact corridor when showing common nbh
	private Color nbhCorridorColor;	  // color for nbh contact corridor when showing common nbh
	
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
		addComponentListener(this);
		this.setOpaque(true); // make this component opaque
		this.setBorder(BorderFactory.createLineBorder(Color.black));
		this.screenSize = new Dimension(Start.INITIAL_SCREEN_SIZE, Start.INITIAL_SCREEN_SIZE);
		
		this.allContacts = mod.getContacts();
		this.selContacts = new ContactList();
		this.tmpContacts = new ContactList();
		this.contactMapSize = mod.getMatrixSize();
		this.pos = new Point();
		this.mousePressedPos = new Point();
		this.mouseDraggingPos = new Point();
		this.currCommonNbh = null; // no common nbh selected
		
		this.dragging = false;
		this.showCommonNeighbours = false;
		this.showRulerCoord = false;
		this.printing = false;
		this.userContactColors = new Hashtable<Contact, Color>();
		this.densityMatrix = null;
		
		// set default colors
		this.contactColor = Color.black;
		this.selectionColor = Color.red;
		this.backgroundColor = Color.white;
		this.selRectColor = Color.black;
		this.crosshairColor = Color.green;
		this.diagCrosshairColor = Color.lightGray;
		this.coordinatesColor = Color.blue;
		this.inBoxTriangleColor = Color.cyan;
		this.outBoxTriangleColor = Color.red;
		this.crossOnContactColor = Color.yellow;
		this.corridorColor = Color.green;
		this.nbhCorridorColor = Color.lightGray;
		
		setBackground(backgroundColor);

	}

	/**
	 * Main method to draw the component on screen. This method is called each time the component has to be
	 * (re) drawn on screen. It is called automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();

		//setBackground(backgroundColor);	// is this necessary?
		
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
			// draw border square
			g2d.setColor(contactColor);
			g2d.drawRect(0, 0, outputSize, outputSize);
		} else {
			outputSize = getWindowSize();
		}
		ratio = (double)outputSize/contactMapSize;		// scale factor, = size of one contact
		int contactSquareSize = (int)(ratio*1); // the size of the square representing a contact
		
		// drawing density matrix
		if(view.getShowDensityMatrix()) {
			// assuming that density matrix has values from [0,1]
			int size = densityMatrix.length;
			for(int i = 0; i < size; i++) {
				for(int j = i; j < size; j++) {
					Color c = colorMapRedBlue(densityMatrix[i][j]);
					if(!c.equals(backgroundColor)) {
						g2d.setColor(c);
						Contact cont = new Contact(i+1,j+1);
						int x = getCellUpperLeft(cont).x;
						int y = getCellUpperLeft(cont).y;
						g2d.drawRect(x,y,contactSquareSize,contactSquareSize);
						g2d.fillRect(x,y,contactSquareSize,contactSquareSize);
					}
				}
			}
		}
		
		if (!view.getHighlightComNbh()){
			// drawing the contact map
			for (Contact cont:allContacts){ 
				// if there is a contact, draw a rectangle
				if(userContactColors.containsKey(cont)) {
					g2d.setColor(userContactColors.get(cont)); 
				} else {
					g2d.setColor(contactColor);
				}
				int x = getCellUpperLeft(cont).x;
				int y = getCellUpperLeft(cont).y;
				g2d.drawRect(x,y,contactSquareSize,contactSquareSize);
				g2d.fillRect(x,y,contactSquareSize,contactSquareSize);
			}
		} else {
			// showing all common neighbours
			for (Contact cont:view.getComNbhSizes().keySet()){
				int size = view.getComNbhSizes().get(cont);
				Point current = getCellUpperLeft(cont);
				int x = current.x;
				int y = current.y;
				if (allContacts.contains(cont)) {
					// coloring pinks when the cell is a contact, 1/size is simply doing the color grading: lower size lighter than higher size
					g2d.setColor(new Color(1.0f/(float) Math.sqrt(size), 0.0f, 1.0f/(float) Math.sqrt(size)));
				} else {
					// coloring greens when the cell is not a contact, 1/size is simply doing the color grading: lower size lighter than higher size
					g2d.setColor(new Color(0.0f, 1.0f/size,0.0f));
				}
				g2d.drawRect(x,y,contactSquareSize,contactSquareSize);
				g2d.fillRect(x,y,contactSquareSize,contactSquareSize);
			}
		}

		// drawing distance matrix
		if (view.getShowDistMatrix()){
			// this actually contains all possible contacts in matrix so is doing a full loop on all cells
			TreeMap<Contact,Double> distMatrix = mod.getDistMatrix();
			for (Contact cont:distMatrix.keySet()){
				Color c = colorMapBluescale(distMatrix.get(cont));
				g2d.setColor(c);
				Point cellUpperLeft = getCellUpperLeft(cont);
				g2d.drawRect(cellUpperLeft.x,cellUpperLeft.y,contactSquareSize,contactSquareSize);
				g2d.fillRect(cellUpperLeft.x,cellUpperLeft.y,contactSquareSize,contactSquareSize);
			}
		}
		

		// drawing selection rectangle if dragging mouse and showing temp selection in red (tmpContacts)
		if (dragging && view.getCurrentAction()==View.SQUARE_SEL) {
			g2d.setColor(selRectColor);
			int xmin = Math.min(mousePressedPos.x,mouseDraggingPos.x);
			int ymin = Math.min(mousePressedPos.y,mouseDraggingPos.y);
			int xmax = Math.max(mousePressedPos.x,mouseDraggingPos.x);
			int ymax = Math.max(mousePressedPos.y,mouseDraggingPos.y);
			g2d.drawRect(xmin,ymin,xmax-xmin,ymax-ymin);
			
			g2d.setColor(selectionColor);
			for (Contact cont:tmpContacts){ 
				// if there is a contact, draw a rectangle
				int x = getCellUpperLeft(cont).x;
				int y = getCellUpperLeft(cont).y;
				g2d.drawRect(x,y,contactSquareSize,contactSquareSize);
				g2d.fillRect(x,y,contactSquareSize,contactSquareSize);
			}
		} 
		// drawing temp selection in red while dragging in range selection mode
		if (dragging && view.getCurrentAction()==View.RANGE_SEL) {
			g2d.setColor(diagCrosshairColor);
			g2d.drawLine(mousePressedPos.x-mousePressedPos.y, 0, outputSize, outputSize-(mousePressedPos.x-mousePressedPos.y));
			g2d.setColor(selectionColor);
			for (Contact cont:tmpContacts){ 
				// if there is a contact, draw a rectangle
				int x = getCellUpperLeft(cont).x;
				int y = getCellUpperLeft(cont).y;
				g2d.drawRect(x,y,contactSquareSize,contactSquareSize);
				g2d.fillRect(x,y,contactSquareSize,contactSquareSize);
			}
		}

		// showing permanent selection in red
		g2d.setColor(selectionColor);
		for (Contact cont:selContacts){
			int x = getCellUpperLeft(cont).x;
			int y = getCellUpperLeft(cont).y;
			g2d.drawRect(x,y,contactSquareSize,contactSquareSize);
			g2d.fillRect(x,y,contactSquareSize,contactSquareSize);
		}	
		
		if (mouseIn && (pos.x <= winsize) && (pos.y <= winsize)){ // second term needed if window is not square shape
			// drawing coordinates on lower left corner (following crosshairs)
			drawCoordinates(g2d);
			// drawing cursor (cross hairs or others)
			drawCursor(g2d);
		} else if(showRulerCoord && currentRulerCoord > 0 && currentRulerCoord <= contactMapSize) {
			drawRulerCoord(g2d);
		}
		if(this.showCommonNeighbours) {
			commonNeighbours(g2d);
			this.showCommonNeighbours = false;
		}

	}

	/** Method called by this component to determine its minimum size */
	public Dimension getMinimumSize() {
		return super.getMinimumSize();
	}

	/** Method called by this component to determine its preferred size */
	public Dimension getPreferredSize() {
		// TODO: This has to be updated when the window is being resized
		return this.screenSize;
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
		userContactColors = new Hashtable<Contact, Color>();
	}
	
	/** Called by ResidueRuler to enable display of ruler coordinates in contactMapPane */
	public void showRulerCoordinate(int resSer) {
		showRulerCoord = true;
		currentRulerCoord = resSer;
		this.repaint();
	}
	
	/** Called by ResidueRuler to switch off showing ruler coordinates */
	public void hideRulerCoordinates() {
		showRulerCoord = false;
		this.repaint();
	}
	
	/** Set the color value in contactColor for the currently selected residues to the given color */
	public void paintCurrentSelection(Color paintingColor) {
		for(Contact cont:selContacts) {
			userContactColors.put(cont, paintingColor);
		}
		this.repaint();
	}
	
	/** Called by view to select all contacts */
	public void selectAllContacts() {
		selContacts = new ContactList();
		selContacts.addAll(allContacts);
		this.repaint();
	}
	
	/** Add to the current selection all contacts along the diagonal that contains cont */
	protected void selectDiagonal(int range) {
		for(Contact c: allContacts) {
			if(c.getRange() == range) {
				selContacts.add(c);
			}
		}
	}
	
	/** Show/hide density matrix */
	protected void toggleDensityMatrix() {
		view.setShowDensityMatrix(!view.getShowDensityMatrix());
		if(view.getShowDensityMatrix()) {
			this.densityMatrix = mod.getDensityMatrix();
		} else {
			this.densityMatrix = null;
		}
		this.repaint();
	}
	
	/**
	 * Update tmpContact with the contacts contained in the rectangle given by the upperLeft and lowerRight.
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
	 * Update tmpContacts with the contacts contained in the range selection (selection by diagonals)
	 *
	 */
	public void rangeSelect(){
		Contact startContact = screen2cm(mousePressedPos);
		Contact endContact = screen2cm(mouseDraggingPos);
		// we reset the tmpContacts list so every new mouse selection starts from a blank list
		tmpContacts = new ContactList();
		int rangeMin = Math.min(startContact.getRange(), endContact.getRange());
		int rangeMax = Math.max(startContact.getRange(), endContact.getRange());
		// we loop over all contacts so time is o(number of contacts) instead of looping over the square (o(n2) being n size of square)
		for (Contact cont:allContacts){
			if (cont.getRange()<=rangeMax && cont.getRange()>=rangeMin){
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

	/** Resets the current selection to the empty set */
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
	 * Returns upper left corner of the square representing the contact
	 */
	private Point getCellUpperLeft(Contact cont){
		return new Point((int) Math.round((cont.j-1)*ratio), (int) Math.round((cont.i-1)*ratio));
	}
			
	/** Return the center of a cell on screen given its coordinates in the contact map */
	private Point getCellCenter(Contact cont){
		Point point = getCellUpperLeft(cont);
		return new Point (point.x+(int)Math.ceil(ratio/2),point.y+(int)Math.ceil(ratio/2));
	}
	
	private Point getCellUpperRight(Contact cont){
		Point point = getCellUpperLeft(cont);
		return new Point (point.x+(int)Math.ceil(ratio),point.y);
	}

	private Point getCellLowerLeft(Contact cont){
		Point point = getCellUpperLeft(cont);
		return new Point (point.x,point.y+(int)Math.ceil(ratio));

	}

	private Point getCellLowerRight(Contact cont){
		Point point = getCellUpperLeft(cont);
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

		lastMouseButtonPressed = evt.getButton();
		mousePressedPos = evt.getPoint();
		if(lastMouseButtonPressed == MouseEvent.BUTTON2) dragging = false;
		
		if (evt.isPopupTrigger()) {
			showPopup(evt);
			return;
		}

	}

	public void mouseReleased(MouseEvent evt) {
		// TODO: Move much of this to MouseClicked and pull up Contact cont = screen2cm...
		// Called whenever the user releases the mouse button.
		if (evt.isPopupTrigger()) {
			showPopup(evt);
			dragging = false;
			return;
		}
		// only if release after left click (BUTTON1)
		if (evt.getButton()==MouseEvent.BUTTON1) {

			switch (view.getCurrentAction()) {
			case View.SHOW_COMMON_NBH:
				Contact c = screen2cm(mousePressedPos); 
				this.currCommonNbh = mod.getEdgeNbh (c.i,c.j);
				dragging = false;
				showCommonNeighbours = true;
				this.repaint();
				return;
				
			case View.SQUARE_SEL:
				if(!dragging) {					
					Contact clicked = screen2cm(mousePressedPos);
					if(allContacts.contains(clicked)) { // if clicked position is a contact
						if(selContacts.contains(clicked)) {
							// if clicked position is a selected contact, deselect it
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
						// else: if clicked position is outside of a contact and ctrl not pressed, reset selContacts
						if(!evt.isControlDown()) selContacts = new ContactList();
					}
					this.repaint();
				} else { // dragging
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
				} else if (cont.j==cont.i){
					selectNodeNbh(cont.i);
				}
				this.repaint();
				return;
			
			case View.RANGE_SEL:
				if (!dragging){
					Contact clicked = screen2cm(mousePressedPos);
					// new behaviour: select current diagonal
					if(!evt.isControlDown()) {
						resetSelContacts();
					}
					selectDiagonal(clicked.getRange());
					this.repaint();
				} else { // dragging
					if (evt.isControlDown()){
						selContacts.addAll(tmpContacts);
					} else{
						selContacts = new ContactList();
						selContacts.addAll(tmpContacts);
					}
				}
				dragging = false;			
				return;

			}
			

		}
	}

	public void mouseDragged(MouseEvent evt) {
		// Called whenever the user moves the mouse
		// while a mouse button is held down. 

		if(lastMouseButtonPressed == MouseEvent.BUTTON1) {
			dragging = true;
			mouseDraggingPos = evt.getPoint();
			switch (view.getCurrentAction()) {
			case View.SQUARE_SEL:
				squareSelect();
				break;
			case View.RANGE_SEL:
				rangeSelect();
				break;
			}	
		}
		
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
	
	/*--------------------------- component events --------------------------*/
	
	public void componentHidden(ComponentEvent evt) {
		// TODO Auto-generated method stub
		
	}

	public void componentMoved(ComponentEvent evt) {
		// TODO Auto-generated method stub
		
	}

	public void componentResized(ComponentEvent evt) {
		// TODO: do everything here that has to be performed only when resized
		int w = getWidth();
		int h = getHeight();
//		int m = Math.min(w,h);
//		w = m;
//		h = m;
//		setSize(w,h);
		screenSize = new Dimension(w,h);
	}

	public void componentShown(ComponentEvent evt) {
		// TODO Auto-generated method stub
		
	}

	/*------------------------ drawing functions ------------------*/
	
	protected void drawCoordinates(Graphics2D g2d){
		Contact currentCell = screen2cm(pos);
		String i_res = mod.getResType(currentCell.i);
		String j_res = mod.getResType(currentCell.j);
		// writing the coordinates at lower left corner
		g2d.setColor(coordinatesColor);
		g2d.drawString("i", 20, winsize-90);
		g2d.drawString("j", 60, winsize-90);
		g2d.drawString(currentCell.i+"", 20, winsize-70);
		g2d.drawString(currentCell.j+"", 60, winsize-70);
		g2d.drawString(i_res==null?"?":i_res, 20, winsize-50);
		g2d.drawString(j_res==null?"?":j_res, 60, winsize-50);
		g2d.drawString(mod.getSecStructure(currentCell.i), 20, winsize-30);
		g2d.drawString(mod.getSecStructure(currentCell.j), 60, winsize-30);

		if(allContacts.contains(currentCell)) {
			g2d.drawLine(48, winsize-55, 55, winsize-55);
		}
		if(view.getCurrentAction()==View.RANGE_SEL){
			g2d.drawString("SeqSep", 100, winsize-70);
			g2d.drawString(Math.abs(currentCell.j-currentCell.i)+"", 100, winsize-50);
		}

		if (view.getShowPdbSers()){
			String i_pdbresser = mod.getPdbResSerial(currentCell.i);
			String j_pdbresser = mod.getPdbResSerial(currentCell.j);
			g2d.drawString(i_pdbresser==null?"?":i_pdbresser, 20, winsize-10);
			g2d.drawString(j_pdbresser==null?"?":j_pdbresser, 60, winsize-10);
		}
	}
	
	// TODO: Merge this with above function?
	private void drawRulerCoord(Graphics2D g2d) {
		String res = mod.getResType(currentRulerCoord);
		g2d.setColor(coordinatesColor);
		g2d.drawString("i", 20, winsize-90);
		g2d.drawString(currentRulerCoord+"", 20, winsize-70);
		g2d.drawString(res==null?"?":res, 20, winsize-50);
		g2d.drawString(mod.getSecStructure(currentRulerCoord), 20, winsize-30);
		if (view.getShowPdbSers()){
			String pdbresser = mod.getPdbResSerial(currentRulerCoord);
			g2d.drawString(pdbresser==null?"?":pdbresser, 20, winsize-10);
		}
	}
	
	protected void drawCursor(Graphics2D g2d){
		// only in case of range selection we draw a diagonal cursor
		if (view.getCurrentAction()==View.RANGE_SEL){
			g2d.setColor(diagCrosshairColor);			
			g2d.drawLine(pos.x-pos.y, 0, getWindowSize(), getWindowSize()-(pos.x-pos.y));
		// all other cases cursor is a cross-hair
		} else {
			// drawing the cross-hair
			g2d.setColor(crosshairColor);
			g2d.drawLine(pos.x, 0, pos.x, winsize);
			g2d.drawLine(0, pos.y, winsize, pos.y);
		}
	}
	
//	/** visualize a contact as an arc */
//	private void drawContactAsArc(Contact cont, Graphics2D g2d) {
//		Point start = getCellCenter(new Contact(cont.i,cont.i));
//		Point end = getCellCenter(new Contact(cont.j,cont.j));
//		
//		// draw half circle
//		double xc = (start.x + end.x) / 2;
//		double yc = (start.y + end.y) / 2;
//		double radius = Math.sqrt(2) * (end.x - start.x);
//		
//		Arc2D arc = new Arc2D.Double();
//		arc.setArcByCenter(xc, yc, radius, -45, 180, Arc2D.OPEN);
//		g2d.draw(arc);
//	}

	private void commonNeighbours(Graphics2D g2d){
		// getting point where mouse was clicked and common neighbours for it
		Contact cont = screen2cm(mousePressedPos); 
		EdgeNbh comNbh = this.currCommonNbh;

		System.out.println("Selecting common neighbours for " + (allContacts.contains(cont)?"contact ":"") + cont);
		System.out.println("Motif: "+comNbh);
		// drawing corridor
		drawCorridor(cont, g2d);
		
		// marking the selected point with a cross
		drawCrossOnContact(cont, g2d, crossOnContactColor);
		System.out.print("Common neighbours: ");
		// drawing triangles
		for (int k:comNbh.keySet()){ // k is each common neighbour (residue serial)
			System.out.print(k+" ");
			if (k>cont.i && k<cont.j) {
				//draw cyan triangles for neighbours within the box 
				drawTriangle(k, cont, g2d, inBoxTriangleColor);
			}
			else { // i.e. k<cont.i || k>cont.j
				//draw red triangles for neighbours out of the box 
				drawTriangle(k, cont, g2d, outBoxTriangleColor);
			}
		}
		System.out.println();
	}

	private void drawCrossOnContact(Contact cont, Graphics2D g2d,Color color){
		g2d.setColor(color);
		if (ratio<6){ // if size of square is too small, then use fixed size 3 in each side of the cross
			Point center = getCellCenter(cont); 
			g2d.drawLine(center.x-3, center.y-3,center.x+3, center.y+3 );
			g2d.drawLine(center.x-3, center.y+3,center.x+3, center.y-3 );
			g2d.drawLine(center.x-2, center.y-3,center.x+2, center.y+3 );
			g2d.drawLine(center.x-2, center.y+3,center.x+2, center.y-3 );	
		} else { // otherwise get upper left, lower left, upper right, lower right to draw a cross spanning the whole contact square
			Point ul = getCellUpperLeft(cont);
			Point ur = getCellUpperRight(cont);
			Point ll = getCellLowerLeft(cont);
			Point lr = getCellLowerRight(cont);
			g2d.drawLine(ul.x,ul.y,lr.x,lr.y);
			g2d.drawLine(ll.x,ll.y, ur.x,ur.y);
			g2d.drawLine(ul.x+1,ul.y,lr.x-1,lr.y);
			g2d.drawLine(ll.x+1,ll.y, ur.x-1,ur.y);
		}
	}

	private void drawCorridor(Contact cont, Graphics2D g2d){
		Point point = getCellCenter(cont);
		int x = point.x;
		int y = point.y;
		g2d.setColor(corridorColor);
		// Horizontal Line
		g2d.drawLine(0, x, x, x);
		// vertical Line
		g2d.drawLine(y,y,y,winsize);
	}
	
	private void drawTriangle(int k, Contact cont, Graphics2D g2d,Color color) {
		int i = cont.i;
		int j = cont.j;
		// we put the i,k and j,k contacts in the right side of the contact map (upper side, i.e.j>i)
		Contact ikCont = new Contact(Math.min(i, k), Math.max(i, k));
		Contact jkCont = new Contact(Math.min(j, k), Math.max(j, k));
		
		// we mark the 2 edges i,k and j,k with a cross
		this.drawCrossOnContact(ikCont, g2d, crossOnContactColor);
		this.drawCrossOnContact(jkCont, g2d, crossOnContactColor);

		// transforming to screen coordinates
		Point point = getCellCenter(cont);
		Point ikPoint = getCellCenter(ikCont);
		Point jkPoint = getCellCenter(jkCont);
		
		// drawing triangle
		g2d.setColor(color);		
		// line between edges i,j and i,k
		g2d.drawLine(point.x, point.y, ikPoint.x, ikPoint.y);
		// line between edges i,j and j,k
		g2d.drawLine(point.x, point.y, jkPoint.x, jkPoint.y);
		// line between edges i,k and j,k
		g2d.drawLine(ikPoint.x, ikPoint.y, jkPoint.x, jkPoint.y);

		// drawing light gray common neighbour corridor markers
		Contact kkCont = new Contact(k,k); // k node point in the diagonal: the start point of the light gray corridor
		Point kkPoint = getCellCenter(kkCont);
		Point endPoint = new Point();
		if (k<(j+i)/2) endPoint = getCellCenter(new Contact(j,k)); // if k below center of segment i->j, the endpoint is j,k i.e. we draw a vertical line
		if (k>(j+i)/2) endPoint = getCellCenter(new Contact(k,i)); // if k above center of segment i->j, the endpoint is k,i i.e. we draw a horizontal line
		g2d.setColor(nbhCorridorColor);
		g2d.drawLine(kkPoint.x, kkPoint.y, endPoint.x, endPoint.y);
	}
	
	/** Given a number between zero and one, returns a color from a gradient. */
	private Color colorMapRedBlue(double val) {
		// TODO: Move this and the following to a class ColorGradient
		if(val == 0) {
			return Color.white;
		}
		// red/blue color map
		double rc = 5/8f;
		double gc = 4/8f;
		double bc = 3/8f;
		double r = Math.max(0,Math.min(1,2.0-4*Math.abs(val-rc)));
		double g = Math.max(0,Math.min(1,1.5-4*Math.abs(val-gc)));
		double b = Math.max(0,Math.min(1,2.0-4*Math.abs(val-bc)));
		return new Color((float) r,(float) g, (float) b);
	}

	/** Given a number between zero and one, returns a color from a gradient. */
//	private Color colorMapMatlab(double val) {
//		// matlab color map
//		double rc = 6/8f;
//		double gc = 4/8f;
//		double bc = 2/8f;
//		double r = Math.max(0,Math.min(1,1.5-4*Math.abs(val-rc)));
//		double g = Math.max(0,Math.min(1,1.5-4*Math.abs(val-gc)));
//		double b = Math.max(0,Math.min(1,1.5-4*Math.abs(val-bc)));
//		return new Color((float) r,(float) g, (float) b);
//	}

	/** Given a number between zero and one, returns a color from a gradient. */
	private Color colorMapBluescale(double val) {
		return new Color((float) (val), (float) (val), (float) Math.min(1,4*val));
	}
	
	/**
	 * To be used whenever the contacts have been changed in the Model object (i.e. the graph object)
	 * Called when deleting edges
	 */
	protected void reloadContacts() {
		this.allContacts = mod.getContacts();
	}
	
	public EdgeNbh getCommonNbh(){
		return currCommonNbh;
	}
	
	/** Returns the contact where the right mouse button was last clicked to open a context menu */
	public Contact getRightClickCont() {
		return this.rightClickCont;
	}
	
	public void showPopup(MouseEvent e) {
		this.rightClickCont = screen2cm(new Point(e.getX(), e.getY()));
		view.popupSendEdge.setText(String.format(View.LABEL_SHOW_PAIR_DIST_3D,rightClickCont.i,rightClickCont.j));
		view.popup.show(e.getComponent(), e.getX(), e.getY());
	}

		
} 

