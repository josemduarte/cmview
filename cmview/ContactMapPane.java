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
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;

import proteinstructure.AAinfo;
import proteinstructure.Alignment;
import proteinstructure.IntPairSet;
import proteinstructure.RIGCommonNbhood;
import proteinstructure.Interval;
import proteinstructure.RIGNbhood;
import proteinstructure.SecStrucElement;
import proteinstructure.SecondaryStructure;
import cmview.datasources.Model;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * The panel containing the contact map and associated event handling.
 */
public class ContactMapPane extends JPanel
implements MouseListener, MouseMotionListener, ComponentListener {

	/*------------------------------ constants ------------------------------*/

	static final long serialVersionUID = 1l;

	// The following options are for testing only and may not function correctly
	static final boolean BACKGROUND_LOADING = false; // if true, maps will be
	// calculated in a
	// separate thread
	static final boolean BG_PRELOADING = false;		 // if true, maps will be
	// preloaded in
	// background

	enum ContactSelSet {COMMON, ONLY_FIRST, ONLY_SECOND, SINGLE}; 
	static final int FIRST = 0;
	static final int SECOND = 1;
	static final int PRESENT = 0;
	static final int ABSENT = 1;
	
	/*--------------------------- member variables --------------------------*/

	// underlying data
	private Model mod;
	protected Model mod2;					// optional second model for cm (referenced by View)
	// comparison
	private Alignment ali; // optional alignment between mod and mod2
	private String[] aliTags = new String[2]; // holds the tags of the sequences corresponding to mod1 and mod2
	private View view;
	private int contactMapSize;				// size of the contact map stored in
	// the underlying model, set once in
	// constructor
	private double scaledDistCutoff;		// stores the value between zero and
	// one which represents the current
	// distance cutoff in the distance
	// map

	// used for drawing
	private Point mousePressedPos;   		// position where mouse where last
	// pressed, used for start of square
	// selection and for common
	// neighbours
	private Point mouseDraggingPos;  		// current position of mouse
	// dragging, used for end point of
	// square selection
	private Point mousePos;             	// current position of mouse (being
	// updated by mouseMoved)
	private int currentRulerCoord;	 		// the residue number shown if
	// showRulerSer=true
	private int currentRulerMousePos;		// the current position of the mouse
	// in the ruler
	private int currentRulerMouseLocation;	// the location (NORTH, EAST, SOUTH,
	// WEST) of the ruler where the
	// mouse is currently in
	private boolean showRulerCoord; 		// while true, current ruler
	// coordinate are shown instead of
	// usual coordinates
	private boolean showRulerCrosshair;		// while true, ruler "crosshair" is
	// being shown
	private Pair<Integer> rightClickCont;	 		// position in contact map where
	// right mouse button was pressed to
	// open context menu
	private RIGCommonNbhood currCommonNbh;	 		// common nbh that the user selected
	// last (used to show it in 3D)
	private int lastMouseButtonPressed;		// mouse button which was pressed
	// last (being updated by
	// MousePressed)

	private Dimension screenSize;			// current size of this component on
	// screen
	private int outputSize;          		// size of the effective square
	// available for drawing the contact
	// map (on screen or other output
	// device)
	private double ratio;		  			// ratio of screen size and contact
	// map size = size of each contact
	// on screen
	private int contactSquareSize;			// size of a single contact on
	// screen depending on the current
	// ratio
	private boolean dragging;     			// set to true while the user is
	// dragging (to display selection
	// rectangle)
	protected boolean mouseIn;				// true if the mouse is currently in
	// the contact map window (otherwise
	// supress crosshair)
	private boolean showCommonNbs; 			// while true, common neighbourhoods
	// are drawn on screen
	private boolean common;
	private boolean firstS;
	private boolean secondS;

	// selections
	private IntPairSet allContacts; 		// all contacts from the underlying
	// contact map model
	private IntPairSet selContacts; 		// permanent list of currently selected
	// contacts
	private IntPairSet tmpContacts; 		// transient list of contacts selected
	// while dragging
	private IntPairSet allSecondContacts; // all contacts from the second loaded
	// structure

	private IntPairSet commonContacts;		// only common contacts 
	private IntPairSet mainStrucContacts;	// only main structure contacts
	private IntPairSet secStrucContacts;	// only second structure contacts
	private IntPairSet bothStrucContacts;	// IntPairSet used in compared mode to handle different EdgeSets
	private IntPairSet contacts;			// IntPairSet used in compared mode to handle different EdgeSets
//	private IntPairSet greenPContacts;		// only green present contacts
//	private IntPairSet redPContacts;		// only red present contacts
//	private IntPairSet commonPContacts;	// only commen (present) contacts

	private TreeSet<Integer> selHorNodes;			// current horizontal residue
	// selection
	private TreeSet<Integer> selVertNodes;			// current vertical residue
	// selection


	// other displayable data
	private Hashtable<Pair<Integer>,Color> userContactColors;  // user defined colors
	// for individual
	// contacts
	private double[][] densityMatrix; 					 // matrix of contact
	// density
	private HashMap<Pair<Integer>,Integer> comNbhSizes;		 // matrix of common
	// neighbourhood sizes
	private HashMap<Pair<Integer>,Double> diffDistMap;		// difference distance map (in comparison mode)

	// buffers for triple buffering
	private ScreenBuffer screenBuffer;		// buffer containing the more of
	// less static background image

	// drawing colors (being set in the constructor)
	private Color backgroundColor;	  		// background color
	private Color contactColor;	  	  		// color for contacts
	private Color selContactsColor;	  		// color for selected contacts
	private Color squareSelColor;	  		// color of selection rectangle
	private Color crosshairColor;     		// color of crosshair
	private Color diagCrosshairColor; 		// color of diagonal "crosshair"
	private Color coordinatesColor;	  		// color of coordinates
	private Color inBoxTriangleColor; 		// color for common nbh triangles
	private Color outBoxTriangleColor;		// color for common nbh triangles
	private Color crossOnContactColor;		// color for crosses on common nbh
	// contacts
	private Color corridorColor;	  		// color for contact corridor when
	// showing common nbh
	private Color nbhCorridorColor;	  		// color for nbh contact corridor
	// when showing common nbh
	protected Color horizontalNodeSelectionColor;	// color for horizontally
	// selected residues (using
	// xor mode)
	protected Color verticalNodeSelectionColor;		// color for vertically
	// selected residues (using
	// xor mode)
	private Color commonContactsColor; 		// color for contact map
	// comparison where both maps
	// have contacts
	private Color contactsMainStrucColor; 		// color for contact map
	// comparison where the main
	// structure has contacts
	private Color contactsSecStrucColor;		// color for contact map
	// comparison where just the
	// second structure has a
	// contact

	// status variables for concurrency
	private int threadCounter;				// counts how many background
	// threads are currently active

	/*----------------------------- constructors ----------------------------*/

	/**
	 * Create a new ContactMapPane.
	 * 
	 * @param mod
	 * @param view
	 */
	public ContactMapPane(Model mod, View view){
		//this.mod = mod;  // outsourced the setting of the model to function setModel which is invoked further below
		this.mod2 = null;
		this.view = view;
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
		this.setOpaque(true); // make this component opaque
		this.setBorder(BorderFactory.createLineBorder(Color.black));
		this.screenSize = new Dimension(Start.INITIAL_SCREEN_SIZE, Start.INITIAL_SCREEN_SIZE);

		// HINT: initialisation of some data member stuff has been moved to function setModel

		// sets the model, initialises all data members directly 
		// connected to the effective model
		setModel(mod);

		// set default colors
		this.contactColor = Color.black;
		this.selContactsColor = Color.red;
		this.backgroundColor = Color.white;
		this.squareSelColor = Color.black;	
		this.crosshairColor = Color.green;
		this.diagCrosshairColor = Color.lightGray;
		this.coordinatesColor = Color.blue;
		this.inBoxTriangleColor = Color.cyan;
		this.outBoxTriangleColor = Color.red;
		this.crossOnContactColor = Color.yellow;
		this.corridorColor = Color.green;
		this.nbhCorridorColor = Color.lightGray;
		this.horizontalNodeSelectionColor = new Color(200,200,255);
		this.verticalNodeSelectionColor = new Color(255,200,255);
		this.commonContactsColor = Color.black;
		this.contactsMainStrucColor = Color.magenta;
		this.contactsSecStrucColor = Color.green;

		setBackground(backgroundColor);
	}

	/**
	 * Sets the model of which the contact map is supposed to be painted.
	 * 
	 * @param mod  the model to be set
	 */
	public void setModel(Model mod) {
		this.mod = mod;
		this.allContacts = mod.getContacts();
		this.selContacts = new IntPairSet();
		this.tmpContacts = new IntPairSet();
		this.selHorNodes = new TreeSet<Integer>();
		this.selVertNodes = new TreeSet<Integer>();
		this.commonContacts = new IntPairSet();
		this.mainStrucContacts = new IntPairSet();
		this.secStrucContacts = new IntPairSet();
		this.bothStrucContacts = new IntPairSet();
		this.contacts = new IntPairSet();

		this.contactMapSize = mod.getMatrixSize();
		this.mousePos = new Point();
		this.mousePressedPos = new Point();
		this.mouseDraggingPos = new Point();
		this.currCommonNbh = null; // no common nbh selected

		this.dragging = false;
		this.showCommonNbs = false;
		this.showRulerCoord = false;
		this.userContactColors = new Hashtable<Pair<Integer>, Color>();
		this.densityMatrix = null;
		this.comNbhSizes = null;
		this.diffDistMap = null;

		// initializes outputSize, ratio and contactSquareSize
		setOutputSize(Math.min(screenSize.height, screenSize.width)); 
	}

	/** 
	 * Add the given new model for contact map comparison. 
	 * @param mod2  the second model
	 * @return false if the model is not the same size as the existing first
	 * model.
	 */
	public boolean addSecondModel(Model mod2) 
	throws DifferentContactMapSizeError {
		// check for number of residues
		if(mod.getMatrixSize() != mod2.getMatrixSize()) {
			throw new DifferentContactMapSizeError("ContactMapPane may not contain two models of differnt size.");
		} else {
			System.out.println("Second model loaded.");
			view.setComparisonMode(true);
			view.setTitle("Comparing " + mod.getPDBCode()+mod.getChainCode() + " and " + mod2.getPDBCode() + mod2.getChainCode());
			this.mod2 = mod2;
			selContacts = new IntPairSet();
			return true;
		}
	}

	/**
	 * Gets the second model.
	 * @return second model, null if no second model has been assigned
	 * @see #hasSecondModel()
	 * @see #getFirstModel()
	 */
	public Model getSecondModel(){
		return mod2;
	}

	/**
	 * Gets the first model.
	 * @return first model, null if no first model has been assigned (which 
	 *  usually should not be the case)
	 * @see #getSecondModel()
	 */
	public Model getFirstModel(){
		return mod;
	}

	/**
	 * Checks if a second model has been assigned.
	 * @return true if so, else false
	 * @see #getSecondModel()
	 */
	public boolean hasSecondModel() {
		return mod2 != null;
	}

	/**
	 * Sets the alignment of the first and the second model along with the 
	 * sequence identifiers (so called tags).
	 * @param ali  the alignment
	 * @param tagMod1  sequence identifier for the aligned sequence 
	 *  corresponding to the first model 
	 * @param tagMod2  the one for the second model
	 * @see #getAlignment()
	 */
	public void setAlignment(Alignment ali, String tagMod1, String tagMod2) {
		this.ali = ali;
		this.aliTags[FIRST] = tagMod1;
		this.aliTags[SECOND] = tagMod2;
	}

	/**
	 * Gets the alignment
	 * @return the alignment, null if no such alignment has been assigned
	 * @see #setAlignment(Alignment, String, String)
	 */
	public Alignment getAlignment() {
		return ali;
	}

	/*-------------------------- overridden methods -------------------------*/

	/** Method called by this component to determine its minimum size */
	@Override
	public Dimension getMinimumSize() {
		return super.getMinimumSize();
	}

	/** Method called by this component to determine its preferred size */
	@Override
	public Dimension getPreferredSize() {
		// TODO: This has to be updated when the window is being resized
		return this.screenSize;
	}

	/** Method called by this component to determine its maximum size */
	@Override
	public Dimension getMaximumSize() {
		return super.getMaximumSize();
	}

	/*------------------------ drawing methods --------------------*/

	/**
	 * Main method to draw the component on screen. This method is called each
	 * time the component has to be (re) drawn on screen. It is called
	 * automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	@Override
	protected synchronized void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();

		// draw screen buffer
		if(screenBuffer==null) {
			screenBuffer = new ScreenBuffer(this);
			updateScreenBuffer();
		} else {
			g2d.drawImage(screenBuffer.getImage(),0,0,this);
		}

		// drawing selection rectangle if dragging mouse and showing temp
		// selection in red (tmpContacts)
		if (dragging && view.getCurrentSelectionMode()==View.SQUARE_SEL) {

			g2d.setColor(squareSelColor);
			int xmin = Math.min(mousePressedPos.x,mouseDraggingPos.x);
			int ymin = Math.min(mousePressedPos.y,mouseDraggingPos.y);
			int xmax = Math.max(mousePressedPos.x,mouseDraggingPos.x);
			int ymax = Math.max(mousePressedPos.y,mouseDraggingPos.y);
			g2d.drawRect(xmin,ymin,xmax-xmin,ymax-ymin);

			g2d.setColor(selContactsColor);
			for (Pair<Integer> cont:tmpContacts){ 
				drawContact(g2d, cont);
			}
		} 

		// drawing temp selection in red while dragging in range selection mode
		if (dragging && view.getCurrentSelectionMode()==View.DIAG_SEL) {

			g2d.setColor(diagCrosshairColor);
			g2d.drawLine(mousePressedPos.x-mousePressedPos.y, 0, outputSize, outputSize-(mousePressedPos.x-mousePressedPos.y));

			g2d.setColor(selContactsColor);
			for (Pair<Integer> cont:tmpContacts){ 
				drawContact(g2d, cont);
			}
		}

		// draw permanent selection in red
		g2d.setColor(selContactsColor);
		for (Pair<Integer> cont:selContacts){
			drawContact(g2d, cont);
		}

		// draw node selections
		if(selHorNodes.size() > 0 || selVertNodes.size() > 0)
			g2d.setColor(Color.white);
		g2d.setXORMode(horizontalNodeSelectionColor);
		// g2d.setColor(horizontalNodeSelectionColor);
		for(Interval intv:Interval.getIntervals(selHorNodes)) {
			drawHorizontalNodeSelection(g2d, intv);
		}
		g2d.setXORMode(verticalNodeSelectionColor);
		// g2d.setColor(verticalNodeSelectionColor);
		for(Interval intv:Interval.getIntervals(selVertNodes)) {
			drawVerticalNodeSelection(g2d, intv);
		}
		g2d.setPaintMode();

		// draw crosshair and coordinates
		if((showRulerCoord || showRulerCrosshair) && (currentRulerCoord > 0 && currentRulerCoord <= contactMapSize)) {
			if(showRulerCoord) {
				drawRulerCoord(g2d);
			}
			if(showRulerCrosshair) {
				drawRulerCrosshair(g2d);
			}			
		} else
			if (mouseIn && (mousePos.x <= outputSize) && (mousePos.y <= outputSize)){ // second
				// term
				// needed
				// if
				// window
				// is
				// not
				// square
				// shape
				drawCoordinates(g2d);
				drawCrosshair(g2d);
			} 

		// draw common neighbours
		if(this.showCommonNbs) {
			Pair<Integer> cont = screen2cm(mousePressedPos);
			drawCommonNeighbours(g2d, cont);
			this.showCommonNbs = false;
		}
	}

	private void drawHorizontalNodeSelection(Graphics2D g2d, Interval residues) {
		Point upperLeft = getCellUpperLeft(new Pair<Integer>(residues.beg,1));
		Point lowerRight = getCellLowerRight(new Pair<Integer>(residues.end, contactMapSize));
		g2d.fillRect(upperLeft.x, upperLeft.y, lowerRight.x-upperLeft.x + 1, lowerRight.y - upperLeft.y + 1);	// TODO:
		// Check
		// this
	}

	private void drawVerticalNodeSelection(Graphics2D g2d, Interval residues) {
		Point upperLeft = getCellUpperLeft(new Pair<Integer>(1,residues.beg));
		Point lowerRight = getCellLowerRight(new Pair<Integer>(contactMapSize, residues.end));
		g2d.fillRect(upperLeft.x, upperLeft.y, lowerRight.x-upperLeft.x + 1, lowerRight.y - upperLeft.y + 1);	// TODO:
		// Check
		// this
	}

	/**
	 * @param g2d
	 */
	private void drawDistanceMap(Graphics2D g2d) {
		// this actually contains all possible contacts in matrix so is doing a
		// full loop on all cells
		HashMap<Pair<Integer>,Double> distMatrix = mod.getDistMatrix();
		for (Pair<Integer> cont:distMatrix.keySet()){
			Color c = colorMapScaledHeatmap(distMatrix.get(cont), scaledDistCutoff);
			g2d.setColor(c);
			drawContact(g2d, cont);
		}
	}

	/**
	 * @param g2d
	 */
	private void drawNbhSizeMap(Graphics2D g2d) {
		// showing common neighbourhood sizes
		for (Pair<Integer> cont:comNbhSizes.keySet()){
			int size = comNbhSizes.get(cont);
			if (allContacts.contains(cont)) {
				// coloring pinks when the cell is a contact, 1/size is simply
				// doing the color grading: lower size lighter than higher size
				g2d.setColor(new Color(1.0f/(float) Math.sqrt(size), 0.0f, 1.0f/(float) Math.sqrt(size)));
			} else {
				// coloring greens when the cell is not a contact, 1/size is
				// simply doing the color grading: lower size lighter than
				// higher size
				g2d.setColor(new Color(0.0f, 1.0f/size,0.0f));
			}
			drawContact(g2d, cont);
		}
	}

	/**
	 * @param g2d
	 */
	private void drawContactMap(Graphics2D g2d) {

		// drawing the contact map
		for (Pair<Integer> cont:allContacts){ 
			if(userContactColors.containsKey(cont)) {
				g2d.setColor(userContactColors.get(cont)); 
			} else {
				g2d.setColor(contactColor);
			}
			drawContact(g2d, cont);
		}

	}


	/** draws the compared contact map if a second structure is loaded */
	private void drawComparedMap(Graphics2D g2d){

		common = view.getSelCommonContactsInComparedMode();
		firstS = view.getSelFirstStrucInComparedMode();
		secondS =view.getSelSecondStrucInComparedMode();


		// getting all contacts of the second structure
		this.allSecondContacts = mod2.getContacts();
		// running through the second data set
		for (Pair<Integer> cont2:allSecondContacts){
			// looking for contacts which are also in the first set
			if (allContacts.contains(cont2)) {
				//add to commonContacts-IntPairSet
				commonContacts.add(cont2);
			}
			else if(!allContacts.contains(cont2)){
				// add to secStrucContacts-IntPairSet
				secStrucContacts.add(cont2);
			}
		}

		for (Pair<Integer> cont:allContacts){
			if (!allSecondContacts.contains(cont)){
				// add to mainStrucContacts_EdgeSet
				mainStrucContacts.add(cont);
			}
		}


		// nothing is toggled
		if (common == false && firstS == false && secondS == false){

			// paint background
			int bgSizeX = Math.max(outputSize, getWidth());		// fill background
			// even if window is
			// not square
			int bgSizeY = Math.max(outputSize, getHeight());	// fill background
			// even of window is
			// not square
			g2d.setColor(backgroundColor);
			if (isOpaque()) {
				g2d.fillRect(0, 0, bgSizeX, bgSizeY);
			}
		}

		// only second structure mode is toggled
		else if (common == false && firstS == false && secondS == true){

			for(Pair<Integer> secCont:secStrucContacts){;
			g2d.setColor(contactsSecStrucColor);
			drawContact(g2d, secCont);}	
		}


		// only main structure mode is toggled
		else if (common == false && firstS == true && secondS == false){

			for(Pair<Integer> mainCont:mainStrucContacts){;
			g2d.setColor(contactsMainStrucColor);
			drawContact(g2d, mainCont);}
		}


		// main structure and second structure mode is toggled
		else if (common == false && firstS == true && secondS == true){

			for(Pair<Integer> mainCont:mainStrucContacts){;
			g2d.setColor(contactsMainStrucColor);
			drawContact(g2d, mainCont);}

			for(Pair<Integer> secCont:secStrucContacts){;
			g2d.setColor(contactsSecStrucColor);
			drawContact(g2d, secCont);}

		}

		// common structure mode is toggled
		else if (common == true && firstS == false && secondS == false){

			for(Pair<Integer> comCont:commonContacts){;
			g2d.setColor(commonContactsColor);
			drawContact(g2d, comCont);}

		}

		// common structure and second structure mode is toggled
		else if (common == true && firstS == false && secondS == true){

			for(Pair<Integer> comCont:commonContacts){;
			g2d.setColor(commonContactsColor);
			drawContact(g2d, comCont);}

			for(Pair<Integer> secCont:secStrucContacts){;
			g2d.setColor(contactsSecStrucColor);
			drawContact(g2d, secCont);}

		}
		// common structure and first structure mode is toggled
		else if (common == true && firstS == true && secondS == false){

			for(Pair<Integer> comCont:commonContacts){;
			g2d.setColor(commonContactsColor);
			drawContact(g2d, comCont);}

			for(Pair<Integer> mainCont:mainStrucContacts){;
			g2d.setColor(contactsMainStrucColor);
			drawContact(g2d, mainCont);}

		}
		else {

			// all modes are toggled
			for(Pair<Integer> comCont:commonContacts){;
			g2d.setColor(commonContactsColor);
			drawContact(g2d, comCont);}

			for(Pair<Integer> mainCont:mainStrucContacts){;
			g2d.setColor(contactsMainStrucColor);
			drawContact(g2d, mainCont);}

			for(Pair<Integer> secCont:secStrucContacts){;
			g2d.setColor(contactsSecStrucColor);
			drawContact(g2d, secCont);}

		}
	}

	/**
	 * Draws the given contact cont to the given graphics object g2d using the
	 * global contactSquareSize and g2d current painting color.
	 */
	private void drawContact(Graphics2D g2d, Pair<Integer> cont) {
		int x = getCellUpperLeft(cont).x;
		int y = getCellUpperLeft(cont).y;
		g2d.drawRect(x,y,contactSquareSize,contactSquareSize);
		g2d.fillRect(x,y,contactSquareSize,contactSquareSize);
	}

	/**
	 * @param g2d
	 */
	private void drawDensityMap(Graphics2D g2d) {
		// assuming that density matrix has values from [0,1]
		int size = densityMatrix.length;
		for(int i = 0; i < size; i++) {
			for(int j = i; j < size; j++) {
				Color c = colorMapRedBlue(densityMatrix[i][j]);
				if(!c.equals(backgroundColor)) {
					g2d.setColor(c);
					Pair<Integer> cont = new Pair<Integer>(i+1,j+1);
					drawContact(g2d, cont);
				}
			}
		}
	}

	private void drawDiffDistMap(Graphics2D g2d) {
		// this actually contains all possible contacts in matrix so is doing a
		// full loop on all cells
		for (Pair<Integer> cont:diffDistMap.keySet()){
			Color c = colorMapHeatmap(1-diffDistMap.get(cont));
			g2d.setColor(c);
			drawContact(g2d, cont);
		}
	}

	/**
	 * Draws coordinates for all registered models.
	 * @param g2d  the graphic onto which the coordinate are supposed to be printed
	 */
	protected void drawCoordinates(Graphics2D g2d){
		if( mod2 == null ) {
			drawCoordinates(g2d,mod,allContacts,20,outputSize-90,null,null,false);
		} else {
			drawCoordinates(g2d,mod,allContacts,20,outputSize-90,aliTags[FIRST],aliTags[FIRST]+":",true);
			drawCoordinates(g2d,mod2,allSecondContacts,180,outputSize-90,aliTags[SECOND],aliTags[SECOND]+":",true);
		}
	}

	/**
	 * Draws coordinates for the given model onto the given graphic. Please 
	 * note, that whenever an ordinate of the current mouse position in the 
	 * graphic equals zero the coordinates will not be printed. 
	 * @param g2d  the graphic
	 * @param mod  the model
	 * @param modContacts  all contacts off mod
	 * @param x  offset position in the x dimension of the upper left corner
	 * @param y  offset position in the y dimension of the upper left corner
	 * @param aliTag  tag of mod in the alignment
	 * @param title  optional title to be printed
	 * @param showALiAndSeqPos  toggles between to different position modes: 
	 *  true -> show alignment as well as sequence positions, false -> show 
	 *  sequence position only
	 */
	protected void drawCoordinates(Graphics2D g2d, Model mod, IntPairSet modContacts, int x, int y, String aliTag, String title, boolean showAliAndSeqPos) {
		Pair<Integer> currentCell = screen2cm(mousePos);

		// return if any position equals zero
		if( currentCell.getFirst() == 0 || currentCell.getSecond() == 0 ) {
			return;
		}

		String i_res = mod.getResType(currentCell.getFirst());
		String j_res = mod.getResType(currentCell.getSecond());

		if( i_res == AAinfo.getGapCharacterThreeLetter() ) {
			i_res = AAinfo.getGapCharacterOneLetter()+"";
		}

		if( j_res == AAinfo.getGapCharacterThreeLetter() ) {
			j_res = AAinfo.getGapCharacterOneLetter()+"";
		}

		int extraX = 0;
		if( showAliAndSeqPos ) {
			extraX = 30;
		}

		// writing the coordinates at lower left corner
		g2d.setColor(coordinatesColor);

		int extraTitleY = 0;
		if( title != null ) {
			extraTitleY = 20;	
			g2d.drawString(title, x, y);
		}

		g2d.drawString("i", x,           y+extraTitleY);
		g2d.drawString("j", x+extraX+40, y+extraTitleY);
		g2d.drawString(currentCell.getFirst()+"", x,           y+extraTitleY+20);
		g2d.drawString(currentCell.getSecond()+"", x+extraX+40, y+extraTitleY+20);
		if( showAliAndSeqPos ) {
			// substract 1 from the cell's coordinates to map from alignment 
			// to sequence space
			try {
				g2d.drawString(ali==null ? "(n.a.)" : "("+ali.al2seq(aliTag,currentCell.getFirst()-1)+")", x+extraX,      y+extraTitleY+20);
				g2d.drawString(ali==null ? "(n.a.)" : "("+ali.al2seq(aliTag,currentCell.getSecond()-1)+")", x+2*extraX+40, y+extraTitleY+20);
			} catch( NullPointerException e ) {
				System.err.println(currentCell);
			}
		}

		g2d.drawString(i_res==null?"?":i_res, x,           y+extraTitleY+40);
		g2d.drawString(j_res==null?"?":j_res, x+extraX+40, y+extraTitleY+40);
		if (mod.hasSecondaryStructure()){
			SecondaryStructure ss = mod.getSecondaryStructure();
			g2d.drawString(ss.getSecStrucElement(currentCell.getFirst())==null?"":(new Character(ss.getSecStrucElement(currentCell.getFirst()).getType()).toString()), x,           y+extraTitleY+60);
			g2d.drawString(ss.getSecStrucElement(currentCell.getSecond())==null?"":(new Character(ss.getSecStrucElement(currentCell.getSecond()).getType()).toString()), x+extraX+40, y+extraTitleY+60);
		}

		if(modContacts.contains(currentCell) ) {
			g2d.drawLine(x+28, y+extraTitleY+35, x+extraX+35, y+extraTitleY+35);		
		}

		if(view.getCurrentSelectionMode()==View.DIAG_SEL) {
			if( !showAliAndSeqPos) {
				g2d.drawString("SeqSep", x+80, y+extraTitleY+20);
				g2d.drawString(getRange(currentCell)+"", x+extraX+80, y+extraTitleY+40);		
			}
		}

		if (view.getShowPdbSers()){
			String i_pdbresser = mod.getPdbResSerial(currentCell.getFirst());
			String j_pdbresser = mod.getPdbResSerial(currentCell.getSecond());
			g2d.drawString(i_pdbresser==null?"?":i_pdbresser, x,           y+extraTitleY+80);
			g2d.drawString(j_pdbresser==null?"?":j_pdbresser, x+extraX+40, y+extraTitleY+80);
		}
	}

	// TODO: Merge this with above function?
	private void drawRulerCoord(Graphics2D g2d) {
		String res = mod.getResType(currentRulerCoord);
		g2d.setColor(coordinatesColor);
		g2d.drawString("i", 20, outputSize-90);
		g2d.drawString(currentRulerCoord+"", 20, outputSize-70);
		g2d.drawString(res==null?"?":res, 20, outputSize-50);
		if (mod.hasSecondaryStructure()){
			SecondaryStructure ss = mod.getSecondaryStructure();
			g2d.drawString(ss.getSecStrucElement(currentRulerCoord)==null?"":(new Character(ss.getSecStrucElement(currentRulerCoord).getType()).toString()), 20, outputSize-30);
		}
		if (view.getShowPdbSers()){
			String pdbresser = mod.getPdbResSerial(currentRulerCoord);
			g2d.drawString(pdbresser==null?"?":pdbresser, 20, outputSize-10);
		}
	}

	protected void drawCrosshair(Graphics2D g2d){
		// only in case of range selection we draw a diagonal cursor
		if (view.getCurrentSelectionMode()==View.DIAG_SEL){
			g2d.setColor(diagCrosshairColor);			
			g2d.drawLine(mousePos.x-mousePos.y, 0, getOutputSize(), getOutputSize()-(mousePos.x-mousePos.y));
			// all other cases cursor is a cross-hair
		} else {
			// drawing the cross-hair
			g2d.setColor(crosshairColor);
			g2d.drawLine(mousePos.x, 0, mousePos.x, outputSize);
			g2d.drawLine(0, mousePos.y, outputSize, mousePos.y);
		}
	}

	private void drawRulerCrosshair(Graphics2D g2d) {
		int x1,x2,y1,y2;
		g2d.setColor(crosshairColor);
		if(currentRulerMouseLocation == ResidueRuler.TOP || currentRulerMouseLocation == ResidueRuler.BOTTOM) {
			x1 = currentRulerMousePos;
			x2 = currentRulerMousePos;
			y1 = 0;
			y2 = outputSize;
		} else {
			x1 = 0;
			x2 = outputSize;
			y1 = currentRulerMousePos;
			y2 = currentRulerMousePos;			
		}
		g2d.drawLine(x1, y1, x2, y2);
	}

//	/** visualize a contact as an arc */
//	private void drawContactAsArc(Contact cont, Graphics2D g2d) {
//	Point start = getCellCenter(new Contact(cont.getFirst(),cont.getFirst()));
//	Point end = getCellCenter(new Contact(cont.getSecond(),cont.getSecond()));

//	// draw half circle
//	double xc = (start.x + end.x) / 2;
//	double yc = (start.y + end.y) / 2;
//	double radius = Math.sqrt(2) * (end.x - start.x);

//	Arc2D arc = new Arc2D.Double();
//	arc.setArcByCenter(xc, yc, radius, -45, 180, Arc2D.OPEN);
//	g2d.draw(arc);
//	}

	private void drawCommonNeighbours(Graphics2D g2d, Pair<Integer> cont){
		RIGCommonNbhood comNbh = this.currCommonNbh;

		System.out.println("Selecting common neighbours for " + (allContacts.contains(cont)?"contact ":"") + cont);
		System.out.println("Motif: "+comNbh);
		// drawing corridor
		drawCorridor(cont, g2d);

		// marking the selected point with a cross
		drawCrossOnContact(cont, g2d, crossOnContactColor);
		System.out.print("Common neighbours: ");
		// drawing triangles
		for (int k:comNbh.keySet()){ // k is each common neighbour (residue
			// serial)
			System.out.print(k+" ");
			if (k>cont.getFirst() && k<cont.getSecond()) {
				// draw cyan triangles for neighbours within the box
				drawTriangle(k, cont, g2d, inBoxTriangleColor);
			}
			else { // i.e. k<cont.getFirst() || k>cont.getSecond()
				// draw red triangles for neighbours out of the box
				drawTriangle(k, cont, g2d, outBoxTriangleColor);
			}
		}
		System.out.println();
	}

	private void drawCrossOnContact(Pair<Integer> cont, Graphics2D g2d,Color color){
		g2d.setColor(color);
		if (ratio<6){ // if size of square is too small, then use fixed size 3
			// in each side of the cross
			Point center = getCellCenter(cont); 
			g2d.drawLine(center.x-3, center.y-3,center.x+3, center.y+3 );
			g2d.drawLine(center.x-3, center.y+3,center.x+3, center.y-3 );
			g2d.drawLine(center.x-2, center.y-3,center.x+2, center.y+3 );
			g2d.drawLine(center.x-2, center.y+3,center.x+2, center.y-3 );	
		} else { // otherwise get upper left, lower left, upper right, lower
			// right to draw a cross spanning the whole contact square
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

	private void drawCorridor(Pair<Integer> cont, Graphics2D g2d){
		Point point = getCellCenter(cont);
		int x = point.x;
		int y = point.y;
		g2d.setColor(corridorColor);
		// Horizontal Line
		g2d.drawLine(0, x, x, x);
		// vertical Line
		g2d.drawLine(y,y,y,outputSize);
	}

	private void drawTriangle(int k, Pair<Integer> cont, Graphics2D g2d,Color color) {
		int i = cont.getFirst();
		int j = cont.getSecond();
		// we put the i,k and j,k contacts in the right side of the contact map
		// (upper side, i.e.getSecond()>i)
		Pair<Integer> ikCont = new Pair<Integer>(Math.min(i, k), Math.max(i, k));
		Pair<Integer> jkCont = new Pair<Integer>(Math.min(j, k), Math.max(j, k));

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
		Pair<Integer> kkCont = new Pair<Integer>(k,k); // k node point in the diagonal: the
		// start point of the light gray
		// corridor
		Point kkPoint = getCellCenter(kkCont);
		Point endPoint = new Point();
		if (k<(j+i)/2) endPoint = getCellCenter(new Pair<Integer>(j,k)); // if k below
		// center of
		// segment i->j,
		// the endpoint
		// is j,k i.e.
		// we draw a
		// vertical line
		if (k>(j+i)/2) endPoint = getCellCenter(new Pair<Integer>(k,i)); // if k above
		// center of
		// segment i->j,
		// the endpoint
		// is k,i i.e.
		// we draw a
		// horizontal
		// line
		g2d.setColor(nbhCorridorColor);
		g2d.drawLine(kkPoint.x, kkPoint.y, endPoint.x, endPoint.y);
	}	


	/*---------------------------- mouse events -----------------------------*/

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
		// TODO: Move much of this to MouseClicked and pull up Contact cont =
		// screen2cm...
		// Called whenever the user releases the mouse button.
		if (evt.isPopupTrigger()) {
			showPopup(evt);
			dragging = false;
			return;
		}
		// only if release after left click (BUTTON1)
		if (evt.getButton()==MouseEvent.BUTTON1) {

			switch (view.getCurrentSelectionMode()) {
			case View.SHOW_COMMON_NBH:
				Pair<Integer> c = screen2cm(mousePressedPos); 
				this.currCommonNbh = mod.getCommonNbhood (c.getFirst(),c.getSecond());
				dragging = false;
				showCommonNbs = true;
				this.repaint();
				return;

			case View.SEL_MODE_COLOR:
				dragging=false;		// TODO: can we pull this up?
				Pair<Integer> clickedPos = screen2cm(mousePressedPos); // TODO: this as well?
				if(!evt.isControlDown()) {
					resetSelections();
				}
				Color clickedColor = userContactColors.get(clickedPos);
				if(clickedColor != null) {
					for(Pair<Integer> e:userContactColors.keySet()) {
						if(userContactColors.get(e) == clickedColor) selContacts.add(e); 
					}
				} else {
					if(allContacts.contains(clickedPos)) {
						// select all black contacts
						if(userContactColors.size() == 0) {
							selectAllContacts();
						} else {
							for(Pair<Integer> e:allContacts) {
								Color col = userContactColors.get(e);
								if(col == null) selContacts.add(e);
							}
						}
					}
				}
				this.repaint();
				return;	

			case View.SQUARE_SEL:


				if(!dragging) {					
					Pair<Integer> clicked = screen2cm(mousePressedPos);
					if(allContacts.contains(clicked)) { // if clicked position
						// is a contact
						if(selContacts.contains(clicked)) {
							// if clicked position is a selected contact,
							// deselect it
							if(evt.isControlDown()) {
								selContacts.remove(clicked);
							} else {
								selContacts = new IntPairSet();
								selContacts.add(clicked);
							}
						} else {
							// if clicked position is a contact but not
							// selected, select it
							if(!evt.isControlDown()) {
								selContacts = new IntPairSet();
							}
							selContacts.add(clicked);
						}
					} else {
						// else: if clicked position is outside of a contact and
						// ctrl not pressed, reset selContacts
						if(!evt.isControlDown()) resetSelections();
					}
					this.repaint();
				} else { // dragging
					if (evt.isControlDown()){
						selContacts.addAll(tmpContacts);
					} else{
						selContacts = new IntPairSet();
						selContacts.addAll(tmpContacts);
					}
				}
				dragging = false;
				return;


			case View.FILL_SEL:
				dragging = false;
				// resets selContacts when clicking mouse
				if (!evt.isControlDown()){
					resetSelections();
				}

				if (this.hasSecondModel()){
					if (common == false && firstS == false && secondS == true){
						fillSelect(secStrucContacts, screen2cm(new Point(evt.getX(),evt.getY())));
					}

					else if (common == false && firstS == true && secondS == false){
						fillSelect(mainStrucContacts, screen2cm(new Point(evt.getX(),evt.getY())));
					}

					else if (common == false && firstS == true && secondS == true){
						contacts.addAll(mainStrucContacts);
						contacts.addAll(secStrucContacts);
						fillSelect(contacts, screen2cm(new Point(evt.getX(),evt.getY())));
					}

					else if (common == true && firstS == false && secondS == false){
						fillSelect(commonContacts, screen2cm(new Point(evt.getX(),evt.getY())));
					}

					else if (common == true && firstS == false && secondS == true){
						fillSelect(allSecondContacts, screen2cm(new Point(evt.getX(),evt.getY())));
					}

					else if (common == true && firstS == true && secondS == false){
						fillSelect(allContacts, screen2cm(new Point(evt.getX(),evt.getY())));
					}

					else if (common == true && firstS == true && secondS == true){
						bothStrucContacts.addAll(commonContacts);
						bothStrucContacts.addAll(secStrucContacts);
						bothStrucContacts.addAll(mainStrucContacts);
						fillSelect(bothStrucContacts, screen2cm(new Point(evt.getX(),evt.getY())));
					}
				}
				else {fillSelect(allContacts, screen2cm(new Point(evt.getX(), evt.getY())));
				}


				this.repaint();
				return;

			case View.NODE_NBH_SEL:
				dragging = false;
				// resets selContacts when clicking mouse
				if (!evt.isControlDown()){
					resetSelections();
				}
				// we select the node neighbourhoods of both i and j of the
				// mousePressedPos
				Pair<Integer> cont = screen2cm(mousePressedPos);
				if (cont.getSecond()>cont.getFirst()){ // only if we clicked on the upper side of
					// the matrix
					selectNodeNbh(cont.getFirst());
					selectNodeNbh(cont.getSecond());
				} else if (cont.getSecond()==cont.getFirst()){
					selectNodeNbh(cont.getFirst());
				}
				this.repaint();
				return;

			case View.DIAG_SEL:
				if (!dragging){
					Pair<Integer> clicked = screen2cm(mousePressedPos);
					// new behaviour: select current diagonal
					if(!evt.isControlDown()) {
						resetSelections();
					}

					selectDiagonal(getRange(clicked));


					this.repaint();
				} else { // dragging
					if (evt.isControlDown()){
						selContacts.addAll(tmpContacts);
					} else{
						selContacts = new IntPairSet();
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
			switch (view.getCurrentSelectionMode()) {
			case View.SQUARE_SEL:
				if (this.hasSecondModel()){
					if (common == false && firstS == false && secondS == true){
						squareSelect(secStrucContacts);
					}

					else if (common == false && firstS == true && secondS == false){
						squareSelect(mainStrucContacts);
					}

					else if (common == false && firstS == true && secondS == true){
						contacts.addAll(mainStrucContacts);
						contacts.addAll(secStrucContacts);
						squareSelect(contacts);
					}

					else if (common == true && firstS == false && secondS == false){
						squareSelect(commonContacts);
					}
					else if (common == true && firstS == false && secondS == true){
						squareSelect(allSecondContacts);
					}

					else if (common == true && firstS == true && secondS == false){
						squareSelect(allContacts);
					}

					else if (common == true && firstS == true && secondS == true){
						bothStrucContacts.addAll(commonContacts);
						bothStrucContacts.addAll(secStrucContacts);
						bothStrucContacts.addAll(mainStrucContacts);
						squareSelect(bothStrucContacts);
					}

				} else {
					squareSelect(allContacts);
				}
				break;
			case View.DIAG_SEL:
				if (this.hasSecondModel()){
					if (common == false && firstS == false && secondS == true){
						rangeSelect(secStrucContacts);
					}

					else if (common == false && firstS == true && secondS == false){
						rangeSelect(mainStrucContacts);
					}

					else if (common == false && firstS == true && secondS == true){
						contacts.addAll(mainStrucContacts);
						contacts.addAll(secStrucContacts);
						rangeSelect(contacts);
					}

					else if (common == true && firstS == false && secondS == false){
						rangeSelect(commonContacts);
					}
					else if (common == true && firstS == false && secondS == true){
						rangeSelect(allSecondContacts);
					}

					else if (common == true && firstS == true && secondS == false){
						rangeSelect(allContacts);
					}

					else if (common == true && firstS == true && secondS == true){
						bothStrucContacts.addAll(commonContacts);
						bothStrucContacts.addAll(secStrucContacts);
						bothStrucContacts.addAll(mainStrucContacts);
						rangeSelect(bothStrucContacts);
					}
				}
				else {rangeSelect(allContacts);}
				break;
			}	
		}
		mouseMoved(evt); // TODO is this necessary? I tried getting rid of it
		// but wasn't quite working
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
		mousePos = evt.getPoint();
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
		screenSize = new Dimension(getWidth(),getHeight());
		setOutputSize(Math.min(screenSize.height, screenSize.width));
		updateScreenBuffer();
	}

	public void componentShown(ComponentEvent evt) {		
	}

	/*---------------------------- public trigger methods ---------------------------*/
	/*
	 * these methods are being called by others to control this components
	 * behaviour
	 */

	/**
	 * Repaint the screen buffer because something in the underlying data has
	 * changed.
	 */
	public synchronized void updateScreenBuffer() {

		if(screenBuffer == null) {
			screenBuffer = new ScreenBuffer(this);
		}
		screenBuffer.clear();
		Graphics2D g2d = screenBuffer.getGraphics();

//		if(g2d.getClip() != null) {
//		g2d.fillRect(0,0,g2d.getClip().getBounds().height,g2d.getClip().getBounds().width);
//		}

		// paint background
		int bgSizeX = Math.max(outputSize, getWidth());		// fill background
		// even if window is
		// not square
		int bgSizeY = Math.max(outputSize, getHeight());	// fill background
		// even of window is
		// not square
		g2d.setColor(backgroundColor);
		if (isOpaque()) {
			g2d.fillRect(0, 0, bgSizeX, bgSizeY);
		}

		// distance map
		if (view.getShowDistMap()){
			drawDistanceMap(g2d);
		}

		// density map
		if(view.getShowDensityMap()) {
			drawDensityMap(g2d);
		}

		// common nbh sizes or contact map
		if (view.getShowNbhSizeMap()){
			drawNbhSizeMap(g2d);
		}

		// draw contact map if necessary
		if(!view.getShowNbhSizeMap() && !view.getShowDistMap() && !view.getCompareStatus()) {
			drawContactMap(g2d);			
		}

		// draw comparison contact map
		if(view.getCompareStatus()){

			// draw difference distance map (in comparison mode)
			if(view.getShowDiffDistMap()) {
				drawDiffDistMap(g2d);
			} else {
				drawComparedMap(g2d);
			}
		}
		repaint();
	}

	/**
	 * Triggers the background maps to be updated in a separate thread
	 */
	public void updateNbhSizeMapBg() {
		new Thread() {
			public void run() {
				registerThread(true);
				comNbhSizes = mod.getAllCommonNbhSizes();
				// updateScreenBuffer();
				registerThread(false);
			}
		}.start();
	}

	/**
	 * Triggers the background maps to be updated in a separate thread
	 */
	public void updateDensityMapBg() {
		new Thread() {
			public void run() {
				registerThread(true);
				densityMatrix = mod.getDensityMatrix();
				// updateScreenBuffer();
				registerThread(false);
			}
		}.start();
	}

	/**
	 * Triggers the background maps to be updated in a separate thread
	 */
	public void updateDistanceMapBg() {
		new Thread() {
			public void run() {
				registerThread(true);
				mod.initDistMatrix();
				// updateScreenBuffer();
				registerThread(false);
			}
		}.start();
	}	

	/**
	 * Triggers the background maps to be updated in a separate thread
	 */
	public void updateDiffDistMapBg() {
		new Thread() {
			public void run() {
				registerThread(true);
				diffDistMap = mod.getDiffDistMatrix(ali,mod2);
				// updateScreenBuffer();
				registerThread(false);
			}
		}.start();
	}	

	public void preloadBackgroundMaps() {
		new Thread() {
			public void run() {
				updateDistanceMapBg();
				updateDensityMapBg();
				updateNbhSizeMapBg();
//				System.out.println("Preloading...");
//				view.statusBar.setText("Preloading density map...");
//				densityMatrix = mod.getDensityMatrix();
//				view.statusBar.setText("Preloading nbh size map...");
//				comNbhSizes = mod.getAllEdgeNbhSizes();
//				view.statusBar.setText("Preloading distance map...");
//				mod.initDistMatrix();
//				view.statusBar.setText(" ");
//				System.out.println("Predloading done.");
			}
		}.start();		
	}

	/**
	 * Triggers the nbh size map to be updated
	 */
	public synchronized void updateNbhSizeMap() {
		comNbhSizes = mod.getAllCommonNbhSizes();
	}

	/**
	 * Triggers the density map to be updated
	 */
	public synchronized void updateDensityMap() {
		densityMatrix = mod.getDensityMatrix();
	}

	/**
	 * Triggers the distance map to be updated
	 */
	public synchronized void updateDistanceMap() {
		scaledDistCutoff = mod.initDistMatrix();
		// System.out.println("Scaled distance cutoff: " + scaledDistCutoff);
	}

	/**
	 * Triggers the difference distance map to be updated
	 */
	public synchronized void updateDiffDistMap() {
		diffDistMap = mod.getDiffDistMatrix(ali,mod2);
	}	

	/**
	 * To be called whenever the contacts have been changed in the Model object
	 * (i.e. the graph object). Currently called when deleting edges.
	 */
	public void reloadContacts() {
		boolean doResetCursor = false;
		this.allContacts = mod.getContacts();
		if(view.getShowNbhSizeMap()) {
			if(BACKGROUND_LOADING) {
				updateNbhSizeMapBg();	// will repaint when done
			} else {
				getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
				updateNbhSizeMap();
				doResetCursor = true;
			}
		} else {
			if(BG_PRELOADING) {
				updateNbhSizeMapBg();
			} else {
				markComNbhSizeMapAsDirty();
			}
		}
		if(view.getShowDensityMap()) {
			if(BACKGROUND_LOADING) {
				updateDensityMapBg();	// will repaint when done
			} else {
				getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
				updateDensityMap();
				doResetCursor = true;
			}
		} else {
			if(BG_PRELOADING) {
				updateDensityMapBg();	
			} else {
				markDensityMapAsDirty();
			}
		}
		updateScreenBuffer();		// always repaint to show new contact map
		if(doResetCursor) {
			getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
		}
	}

	/**
	 * Print this ContactMapPane to the given graphics2D object using the given
	 * width and height.
	 */
	public void print(double width, double height, Graphics2D g2d) {
		int printSize = (int) Math.min(width, height);
		int oldSize = getOutputSize();				// save old size
		Border saveBorder = getBorder();			// save border
		setBorder(null);							// set new border
		setOutputSize(printSize);					// set new size
		paintComponent(g2d);						// paint using new values
		g2d.setColor(contactColor);					// TODO: is this ok?
		g2d.drawRect(0, 0, printSize, printSize);	// paint rectangle
		setOutputSize(oldSize);						// restore size
		setBorder(saveBorder);						// restore border
		repaint();									// restore screen
	}

	/** Called by view to reset the user defined contact colors */
	public void resetUserContactColors() {
		userContactColors = new Hashtable<Pair<Integer>, Color>();
		updateScreenBuffer();
	}

	/** Called by ResidueRuler to enable display of ruler coordinates */
	public void showRulerCoordinate() {
		showRulerCoord = true;
	}
	/** Called by ResidueRuler to enable display of ruler "crosshair" */	
	public void showRulerCrosshair() {
		showRulerCrosshair = true;
	}

	public void setRulerCoordinates(int resSer, int mousePos, int location) {
		currentRulerCoord = resSer;
		currentRulerMousePos = mousePos;
		currentRulerMouseLocation = location;
	}

	public int[] getRulerCoordinates(){
		int[] rulerArray = new int[3];
		rulerArray[0]= currentRulerCoord;
		rulerArray[1]= currentRulerMousePos;
		rulerArray[2]= currentRulerMouseLocation;

		return rulerArray;
	}

	/** Called by ResidueRuler to switch off showing ruler coordinates */
	public void hideRulerCoordinate() {
		showRulerCoord = false;
	}

	/** Called by ResidueRuler to switch off showing ruler "crosshair" */	
	public void hideRulerCrosshair() {
		showRulerCrosshair = false;
	}

	/**
	 * Set the color value in contactColor for the currently selected residues
	 * to the given color
	 */
	public void paintCurrentSelection(Color paintingColor) {
		for(Pair<Integer> cont:selContacts) {
			userContactColors.put(cont, paintingColor);
		}
		updateScreenBuffer();
		this.repaint();
	}

	/** Called by view to select all contacts */
	public void selectAllContacts() {
		selContacts = new IntPairSet();
		common = view.getSelCommonContactsInComparedMode();
		firstS = view.getSelFirstStrucInComparedMode();
		secondS = view.getSelSecondStrucInComparedMode();

		if (this.hasSecondModel()){
			if (common == false && firstS == false && secondS == true){
				selContacts.addAll(secStrucContacts);
			}

			else if (common == false && firstS == true && secondS == false){
				selContacts.addAll(mainStrucContacts);
			}

			else if (common == false && firstS == true && secondS == true){
				// removed "selContacts.addAll(contacts)" as contacts is only 
				// being filled in the mouse drag/release mode
				selContacts.addAll(mainStrucContacts);
				selContacts.addAll(secStrucContacts);
				selContacts.addAll(contacts);
			}

			else if (common == true && firstS == false && secondS == false){
				selContacts.addAll(commonContacts);
			}

			else if (common == true && firstS == false && secondS == true){
				selContacts.addAll(allSecondContacts);
			}

			else if (common == true && firstS == true && secondS == false){
				selContacts.addAll(allContacts);
			}

			else if (common == true && firstS == true && secondS == true){
				bothStrucContacts.addAll(commonContacts);
				bothStrucContacts.addAll(secStrucContacts);
				bothStrucContacts.addAll(mainStrucContacts);
				selContacts.addAll(bothStrucContacts);
			}

		} else { 
			selContacts.addAll(allContacts);
		}

		this.repaint();
	}

	/** Called by view to select all helix-helix contacts */
	public void selectHelixHelix() {
		selContacts = new IntPairSet();
		for(Pair<Integer> e:allContacts) {
			SecStrucElement ss1 = mod.getSecondaryStructure().getSecStrucElement(e.getFirst());
			SecStrucElement ss2 = mod.getSecondaryStructure().getSecStrucElement(e.getSecond());
			if(ss1 != null && ss2 != null && ss1 != ss2 && ss1.getType() == SecStrucElement.HELIX && ss2.getType() == SecStrucElement.HELIX) {
				selContacts.add(e);
			}
		}
		this.repaint();
	}

	/** Called by view to select all strand-strand contacts */
	public void selectBetaBeta() {
		selContacts = new IntPairSet();
		for(Pair<Integer> e:allContacts) {
			SecStrucElement ss1 = mod.getSecondaryStructure().getSecStrucElement(e.getFirst());
			SecStrucElement ss2 = mod.getSecondaryStructure().getSecStrucElement(e.getSecond());
			if(ss1 != null && ss2 != null && ss1 != ss2 && ss1.isStrand() && ss2.isStrand()) {
				selContacts.add(e);
			}
		}
		this.repaint();		
	}

	/** Called by view to select all contacts between secondary structure elements */
	public void selectInterSsContacts() {
		selContacts = new IntPairSet();
		for(Pair<Integer> e:allContacts) {
			SecStrucElement ss1 = mod.getSecondaryStructure().getSecStrucElement(e.getFirst());
			SecStrucElement ss2 = mod.getSecondaryStructure().getSecStrucElement(e.getSecond());
			if(ss1 != null && ss2 != null && (ss1 != ss2 && !ss1.inSameSheet(ss2)) && !ss1.isOther() && !ss2.isOther()) {
				selContacts.add(e);
			}
		}
		this.repaint();		
	}

	/** Called by view to select all contacts within secondary structure elements */
	public void selectIntraSsContacts() {
		selContacts = new IntPairSet();
		for(Pair<Integer> e:allContacts) {
			SecStrucElement ss1 = mod.getSecondaryStructure().getSecStrucElement(e.getFirst());
			SecStrucElement ss2 = mod.getSecondaryStructure().getSecStrucElement(e.getSecond());
			if(ss1 != null && ss2 != null && (ss1 == ss2 || ss1.inSameSheet(ss2)) && !ss1.isOther()) {
				selContacts.add(e);
			}
		}
		this.repaint();		
	}

	/**
	 * Select contacts by residue numbers using a selection string. Example selection string: "1-3,7,8-9".
	 * @param selStr selection string
	 * @return number of selected contacts or -1 on error
	 */
	public int selectByResNum(String selStr) {
		if(selStr.length() == 0) return 0;	// nothing to select
		if(!Interval.isValidSelectionString(selStr)) return -1;
		TreeSet<Integer> nodeSet1 = Interval.parseSelectionString(selStr);
		TreeSet<Integer> nodeSet2 = Interval.parseSelectionString(selStr);
		selContacts = new IntPairSet();
		for(Pair<Integer> e:allContacts) {
			if(nodeSet1.contains(e.getFirst()) && nodeSet2.contains(e.getSecond())) {
				selContacts.add(e);
			}
		}
		this.repaint();
		return selContacts.size();
	}

	/**
	 * Add to the current selection all contacts along the diagonal that
	 * contains cont
	 */
	protected void selectDiagonal(int range) {
		for(Pair<Integer> c: allContacts) {
			if(getRange(c) == range) {
				selContacts.add(c);
			}
		}
	}

	/**
	 * Add to the current horizontal residue selection the given interval of
	 * residues
	 */
	protected void selectNodesHorizontally(Interval intv) {
		for(int i=intv.beg; i <= intv.end; i++) {
			selHorNodes.add(i);
		}
		checkNodeIntersectionAndSelect();
	}

	/**
	 * Remove from the current horizontal residue selection the given interval
	 * of residues, assuming that they are contained.
	 */
	protected void deselectNodesHorizontally(Interval intv) {
		for(int i=intv.beg; i <= intv.end; i++) {
			selHorNodes.remove(i);
		}	
		checkNodeIntersectionAndSelect();
	}

	/** Resets the current horizontal residue selection */
	protected void resetHorizontalNodeSelection() {
		selHorNodes = new TreeSet<Integer>();
	}

	/**
	 * Add to the current horizontal residue selection the given interval of
	 * residues
	 */
	protected void selectNodesVertically(Interval intv) {
		for(int i=intv.beg; i <= intv.end; i++) {
			selVertNodes.add(i);
		}
		checkNodeIntersectionAndSelect();
	}

	/**
	 * Remove from the current vertical residue selection the given interval of
	 * residues, assuming that they are contained.
	 */
	protected void deselectNodesVertically(Interval intv) {
		for(int i=intv.beg; i <= intv.end; i++) {
			selVertNodes.remove(i);
		}
		checkNodeIntersectionAndSelect();
	}

	/** Resets the current horizontal residue selection */
	protected void resetVerticalNodeSelection() {
		selVertNodes = new TreeSet<Integer>();
	}

	/**
	 * Show/hide common neighbourhood size map
	 */	
	protected void toggleNbhSizeMap(boolean state) {
		if (state) {
			if(comNbhSizes == null) {
				if(BACKGROUND_LOADING) {
					updateNbhSizeMapBg();
				} else {
					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
					updateNbhSizeMap();
					updateScreenBuffer();		// will repaint
					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
				}
			} else {
				updateScreenBuffer();
			}
		} else {
			updateScreenBuffer();			// will repaint
		}
	}	

	/**
	 * Show/hide density map
	 */
	protected void toggleDensityMap(boolean state) {
		if(state) {
			if(densityMatrix == null) {
				if(BACKGROUND_LOADING) {
					updateDensityMapBg();		// will update screen buffer
					// when done
				} else {
					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
					updateDensityMap();
					updateScreenBuffer();		// will repaint
					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
				}
			} else {
				updateScreenBuffer();
			}
		} else {
			updateScreenBuffer();			// will repaint
		}
	}

	/**
	 * Show/hide distance map TODO: Make this work the same as density map/nbh
	 * size map
	 */	
	protected void toggleDistanceMap(boolean state) {
		if(state) {
			if (mod.getDistMatrix()==null) {
				if(BACKGROUND_LOADING) {
					updateDistanceMapBg();		// will update screen buffer
					// when done
				} else {
					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));			
					updateDistanceMap();
					updateScreenBuffer();		// will repaint
					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
				}
			} else {
				updateScreenBuffer();			// will repaint
			}
		} else {
			updateScreenBuffer();
		}
	}		

	/**
	 * Show/hide difference distance map
	 */	
	protected void toggleDiffDistMap(boolean state) {
		if (state) {
			if(diffDistMap == null) {
				if(BACKGROUND_LOADING) {
					updateDiffDistMapBg();
				} else {
					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
					updateDiffDistMap();
					updateScreenBuffer();		// will repaint
					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
				}
			} else {
				updateScreenBuffer();
			}
		} else {
			updateScreenBuffer();			// will repaint
		}
	}

	private void markComNbhSizeMapAsDirty() {
		comNbhSizes = null;		// mark as dirty
	}

	private void markDensityMapAsDirty() {
		densityMatrix = null;
	}

//	/**
//	* To be called whenever something in the underlying 3D data has changed.
//	*/
//	private void markDistanceMapAsDirty() {

//	}

//	/**
//	* To be called whenever something in the underlying 3D data has changed.
//	*/	
//	private void markDiffDistMapAsDirty() {

//	}

	/** */
	protected void toggleCompareMode(boolean state) {
		if(state) {
			updateScreenBuffer();
		} else {
			// TODO switch it off
			updateScreenBuffer();
		}
	}		

	public void deleteSelectedContacts() {
		for (Pair<Integer> cont:selContacts){
			mod.removeEdge(cont);
		}
		resetSelections();
		reloadContacts();	// will update screen buffer and repaint
	}

	/**
	 * Update tmpContact with the contacts contained in the rectangle given by
	 * the upperLeft and lowerRight.
	 */
	public void squareSelect(IntPairSet contacts){
		Pair<Integer> upperLeft = screen2cm(mousePressedPos);
		Pair<Integer> lowerRight = screen2cm(mouseDraggingPos);
		// we reset the tmpContacts list so every new mouse selection starts
		// from a blank list
		tmpContacts = new IntPairSet();

		int imin = Math.min(upperLeft.getFirst(),lowerRight.getFirst());
		int jmin = Math.min(upperLeft.getSecond(),lowerRight.getSecond());
		int imax = Math.max(upperLeft.getFirst(),lowerRight.getFirst());
		int jmax = Math.max(upperLeft.getSecond(),lowerRight.getSecond());
		// we loop over all contacts so time is o(number of contacts) instead of
		// looping over the square (o(n2) being n size of square)

		for (Pair<Integer> cont:contacts){
			if (cont.getFirst()<=imax && cont.getFirst()>=imin && cont.getSecond()<=jmax && cont.getSecond()>=jmin){
				tmpContacts.add(cont);
			}
		}

	}

	/**
	 * Update tmpContacts with the contacts contained in the range selection
	 * (selection by diagonals)
	 */
	public void rangeSelect(IntPairSet contacts){
		Pair<Integer> startContact = screen2cm(mousePressedPos);
		Pair<Integer> endContact = screen2cm(mouseDraggingPos);
		// we reset the tmpContacts list so every new mouse selection starts
		// from a blank list
		tmpContacts = new IntPairSet();
		int rangeMin = Math.min(getRange(startContact), getRange(endContact));
		int rangeMax = Math.max(getRange(startContact), getRange(endContact));
		// we loop over all contacts so time is o(number of contacts) instead of
		// looping over the square (o(n2) being n size of square)
		for (Pair<Integer> cont:contacts){
			if (getRange(cont)<=rangeMax && getRange(cont)>=rangeMin){
				tmpContacts.add(cont);
			}
		}
	}

	/**
	 * Update selContacts with the result of a fill selection starting from the
	 * given contact.
	 * 
	 * @param cont
	 *            contact where mouse has been clicked TODO: Create a
	 *            tmpContacts first and then copy to selContacts (if we want
	 *            this behaviour)
	 */
	private void fillSelect(IntPairSet contacts, Pair<Integer> cont){
		int i = cont.getFirst();
		int j = cont.getSecond();
		if ((i < 1) || (j < 1) || (i > contactMapSize) || (j > contactMapSize)) {
			return;
		} else {
			if (!contacts.contains(cont)){
				return;
			}

			if (selContacts.contains(cont)){
				return;
			}
			else {
				selContacts.add(cont);

				// 1 distance
				fillSelect(contacts, new Pair<Integer>(i-1,j));
				fillSelect(contacts, new Pair<Integer>(i+1,j));
				fillSelect(contacts, new Pair<Integer>(i,j-1));
				fillSelect(contacts, new Pair<Integer>(i,j+1));

				// 2 distance
				fillSelect(contacts, new Pair<Integer>(i-2,j));
				fillSelect(contacts, new Pair<Integer>(i+2,j));
				fillSelect(contacts, new Pair<Integer>(i,j-2));
				fillSelect(contacts, new Pair<Integer>(i,j+2));
				fillSelect(contacts, new Pair<Integer>(i-1,j+1));
				fillSelect(contacts, new Pair<Integer>(i+1,j+1));
				fillSelect(contacts, new Pair<Integer>(i-1,j-1));
				fillSelect(contacts, new Pair<Integer>(i+1,j-1));
			}
		}
	}

	protected void selectNodeNbh(int i) {
		RIGNbhood nbh = mod.getNbhood(i);
		System.out.println("Selecting node neighbourhood of node: "+i);
		System.out.println("Motif: "+nbh);
		System.out.print("Neighbours: ");
		for (int j:nbh.keySet()){
			System.out.print(j+" ");
			selContacts.add(new Pair<Integer>(Math.min(i, j),Math.max(i, j)));
		}
		System.out.println();

	}

	/** Resets the current contact- and residue selections to the empty set */
	protected void resetSelections() {
		resetContactSelection();
		resetHorizontalNodeSelection();
		resetVerticalNodeSelection();
	}

	/** Resets the current contact selection */
	protected void resetContactSelection() {
		this.selContacts = new IntPairSet();
	}

	/**
	 * Sets the output size and updates the ratio and contact square size. This
	 * will affect all drawing operations. Used by print() method to change the
	 * output size to the size of the paper and back.
	 */
	protected void setOutputSize(int size) {
		outputSize = size;
		ratio = (double) outputSize/contactMapSize;		// scale factor, = size
		// of one contact
		contactSquareSize = (int) (ratio*1); 			// the size of the
		// square representing a
		// contact
	}

	/*---------------------------- public information methods ---------------------------*/
	/*
	 * these methods are called by others to retrieve the state of the current
	 * component
	 */

	/** Returns the currently selected common neighbourhood (to show it in 3D) */
	public RIGCommonNbhood getCommonNbh(){
		return currCommonNbh;
	}

	/**
	 * Returns the contact where the right mouse button was last clicked to open
	 * a context menu (to show it in 3D)
	 */
	public Pair<Integer> getRightClickCont() {
		return this.rightClickCont;
	}

	/** Called by residueRuler to get the current output size for drawing */
	protected int getOutputSize(){
		return outputSize;
	}

	/** Return the selContacts variable */
	public IntPairSet getSelContacts(){
		return selContacts;
	}

	/**
	 * Gets the whole set of selected contacts.
	 * @param seqIdxMode  true if we want sequence indices, false if we want alignment indices
	 * @return mapping from {@link ContactSelSet} type to an array of contacts. 
	 *  The array entries are supposed to be adressed by {@link FIRST} and 
	 *  {@link SECOND} yielding the contacts for the first and second model, 
	 *  respectively. According to the currently selected contacts these 
	 *  contact lists might be empty! In case the contact map panel contains 
	 *  only one model combining {@link ContactSelSet.SINGLE} as the key to the 
	 *  map and {@link FIRST} as the key to the array yiels the desired set of 
	 *  contacts. 
	 */
	public TreeMap<ContactSelSet,IntPairSet[]> getSelectedContacts(boolean seqIdxMode){
		TreeMap<ContactSelSet,IntPairSet[]> selMap = new TreeMap<ContactSelSet, IntPairSet[]>();
		if (this.hasSecondModel()){
			
			// common, firstS and secondS refers to 
			// View.getSel<...>ContactsInComparedMode() repesenting the states 
			// of the buttons in the GUI
			
			IntPairSet[] common = new IntPairSet[2];
			common[FIRST]       = new IntPairSet();
			common[SECOND]      = new IntPairSet();
			IntPairSet[] firstOnly = new IntPairSet[2];
			firstOnly[FIRST]       = new IntPairSet();
			firstOnly[SECOND]      = new IntPairSet();
			IntPairSet[] secondOnly = new IntPairSet[2];
			secondOnly[FIRST]       = new IntPairSet();
			secondOnly[SECOND]      = new IntPairSet();

			int pos1,pos2;
			
			if (seqIdxMode) {
				for (Pair<Integer> cont : selContacts) {
					if (commonContacts.contains(cont)) {
						common[FIRST].add(new Pair<Integer>(ali.al2seq(aliTags[FIRST],cont.getFirst()-1), 
															ali.al2seq(aliTags[FIRST], cont.getSecond()-1)));
						common[SECOND].add(new Pair<Integer>(ali.al2seq(aliTags[SECOND],cont.getFirst()-1),
															ali.al2seq(aliTags[SECOND], cont.getSecond()-1)));
					}

					else if (mainStrucContacts.contains(cont)) {
						
						firstOnly[FIRST].add(new Pair<Integer>(ali.al2seq(aliTags[FIRST],cont.getFirst()-1),
																ali.al2seq(aliTags[FIRST], cont.getSecond()-1)));
						
						pos1 = ali.al2seq(aliTags[SECOND], cont.getFirst()-1);
						pos2 = ali.al2seq(aliTags[SECOND], cont.getSecond()-1);
						
						if( pos1 != -1 && pos2 != -1 ) {
							firstOnly[SECOND].add(new Pair<Integer>(pos1,pos2));
						}
					}

					else if (secStrucContacts.contains(cont)) {
						secondOnly[SECOND].add(new Pair<Integer>(ali.al2seq(aliTags[SECOND],cont.getFirst()-1),
																ali.al2seq(aliTags[SECOND], cont.getSecond()-1)));

						pos1 = ali.al2seq(aliTags[FIRST], cont.getFirst()-1);
						pos2 = ali.al2seq(aliTags[FIRST], cont.getSecond()-1);
								
						if( pos1 != -1 && pos2 != -1 ) {
							secondOnly[FIRST].add(new Pair<Integer>(pos1,pos2));
						}
					}
				}
			} else {
				for (Pair<Integer> cont : selContacts) {
					if (commonContacts.contains(cont)) {
						common[FIRST].add(cont);
						common[SECOND].add(cont);
					}

					else if (mainStrucContacts.contains(cont)) {
						
						firstOnly[FIRST].add(cont);
						
						pos1 = ali.al2seq(aliTags[SECOND], cont.getFirst());
						pos2 = ali.al2seq(aliTags[SECOND], cont.getSecond());
						
						if( pos1 != -1 && pos2 != -1 ) {
							firstOnly[SECOND].add(cont);
						}
					}

					else if (secStrucContacts.contains(cont)) {
						secondOnly[SECOND].add(cont);

						pos1 = ali.al2seq(aliTags[FIRST], cont.getFirst());
						pos2 = ali.al2seq(aliTags[FIRST], cont.getSecond());
								
						if( pos1 != -1 && pos2 != -1 ) {
							secondOnly[FIRST].add(cont);
						}
					}
				}
			}		
			
			selMap.put(ContactSelSet.COMMON,     common);
			selMap.put(ContactSelSet.ONLY_FIRST, firstOnly);
			selMap.put(ContactSelSet.ONLY_SECOND,secondOnly);

		} else { 
			IntPairSet[] selSet = new IntPairSet[2];
			selSet[FIRST] = this.getSelContacts();
			 
			selMap.put(ContactSelSet.SINGLE, selSet);
		}
			
		return selMap;
	}

	
	public TreeSet<Integer> getAlignmentColumnsFromSelectedContacts() { 

		TreeSet<Integer> positions = new TreeSet<Integer>();
		TreeMap<ContactSelSet,IntPairSet[]> selMap = getSelectedContacts(false);
		
		// get positions considering each set of contact selections
		getAlignmentColumnsFromContacts(selMap.get(ContactSelSet.COMMON)[FIRST],positions);
		getAlignmentColumnsFromContacts(selMap.get(ContactSelSet.COMMON)[SECOND],positions);
		getAlignmentColumnsFromContacts(selMap.get(ContactSelSet.ONLY_FIRST)[FIRST],positions);
		getAlignmentColumnsFromContacts(selMap.get(ContactSelSet.ONLY_FIRST)[SECOND],positions);
		getAlignmentColumnsFromContacts(selMap.get(ContactSelSet.ONLY_SECOND)[SECOND],positions);
		getAlignmentColumnsFromContacts(selMap.get(ContactSelSet.ONLY_SECOND)[FIRST],positions);
		
		return positions;
	}
	
	/** Returns the set of horizontally selected nodes. */
	public TreeSet<Integer> getSelHorNodes() {
		return selHorNodes;
	}

	/** Returns the set of vertically selected nodes. */
	public TreeSet<Integer> getSelVertNodes() {
		return selVertNodes;
	}

	/**
	 * Gets the alignment columns for the given set of contacts.
	 * Requires the contacts to be anchored in the indexing space of the aligned 
	 * models!!!
	 * @param contacts  set of contacts
	 * @param positions  contains the alignment columns incident to the 
	 *  contacts in <code>contacts</code> afterwards
	 */
	public void getAlignmentColumnsFromContacts(IntPairSet contacts, TreeSet<Integer> positions) {
		for( Pair<Integer> cont : contacts ) {
			// substract -1 to get the alignment column
			positions.add(cont.getFirst()  - 1);
			positions.add(cont.getSecond() - 1);
		}
	}
	
	/**
	 * Gets the alignment columns for the given set of contacts
	 * @param contacts  set of contacts
	 * @param positions  contains the alignment columns incident to the 
	 *  contacts in <code>contacts</code> afterwards
	 * @param tag  name of the model in the alignment the contacts 
	 *  belong to
	 */
	@SuppressWarnings("unused")
	public void getAlignmentColumnsFromContacts(IntPairSet contacts, TreeSet<Integer> positions, String tag) {
		for( Pair<Integer> cont : contacts ) {
			// substract -1 to get the alignment column
			positions.add(ali.seq2al(tag, cont.getFirst())  - 1);
			positions.add(ali.seq2al(tag, cont.getSecond()) - 1);
		}
	}
	
	/*---------------------------- private methods --------------------------*/
	
	/**
	 * Increases or decreases the thread counter and displays some user
	 * information while threads are running
	 */
	private synchronized void registerThread(boolean increase) {
		if(increase) {
			if(threadCounter == 0) {
				// first thread registered
				getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
			}
			threadCounter++;
		} else {
			threadCounter--;
			if(threadCounter == 0) {
				// no more threads running
				updateScreenBuffer();
				getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
			}
		}
		System.out.println(threadCounter + " threads are running");
		assert(threadCounter >= 0);
	}

	/**
	 * Checks whether nodes have been selected both horizontally and vertically
	 * and in that case selects the intersecting contacts.
	 */
	private void checkNodeIntersectionAndSelect() {
		if(selHorNodes.size() > 0 && selVertNodes.size() > 0) {
			resetContactSelection();
			Pair<Integer> c;
			for(int i:selHorNodes) {	// TODO: Can this be optimized?
				for(int j:selVertNodes) {
					c = new Pair<Integer>(i,j);
					if(allContacts.contains(c)) selContacts.add(c);
				}
			}
		}
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

	/**
	 * Given a number between zero and one, returns a color from a scaled heatmap-style colormap.
	 * The map is scales such that values around 'middle' are green, higher values are darker
	 * shades of red and lower values are darker shades of blue.
	 * @param val the value for which a color is returned
	 * @param middle the value around which colors are green
	 * @return the Color for the given value
	 */
	private Color colorMapScaledHeatmap(double val, double middle) {
		if(val <= middle) {
			val = val * 0.5/middle;
		} else {
			val = 0.5 + (val-middle) * 0.5 / (1-middle);
		}
		// matlab-style color map
		double bc = 6/8f;
		double gc = 4/8f;
		double rc = 2/8f;
		double r = Math.max(0,Math.min(1,1.5-4*Math.abs(val-rc)));
		double g = Math.max(0,Math.min(1,1.5-4*Math.abs(val-gc)));
		double b = Math.max(0,Math.min(1,1.5-4*Math.abs(val-bc)));
		return new Color((float) r,(float) g, (float) b);
	}

	/**
	 * Given a number between zero and one, returns a color from a heatmap-style colormap.
	 * Values around 0.5 are green, lower values are darker shades of blue, higher values
	 * are darker shades of red.
	 * @param val the value for which a color is returned
	 * @return the Color for the given value
	 */
	private Color colorMapHeatmap(double val) {
		// matlab-style color map
		double bc = 6/8f;
		double gc = 4/8f;
		double rc = 2/8f;
		double r = Math.max(0,Math.min(1,1.5-4*Math.abs(val-rc)));
		double g = Math.max(0,Math.min(1,1.5-4*Math.abs(val-gc)));
		double b = Math.max(0,Math.min(1,1.5-4*Math.abs(val-bc)));
		return new Color((float) r,(float) g, (float) b);
	}	

//	/** Given a number between zero and one, returns a color from a gradient. */
//	private Color colorMapBluescale(double val) {
//	return new Color((float) (val), (float) (val), (float) Math.min(1,4*val));
//	}

	private void showPopup(MouseEvent e) {
		this.rightClickCont = screen2cm(new Point(e.getX(), e.getY()));
		view.popupSendEdge.setText(String.format(View.LABEL_SHOW_PAIR_DIST_3D,rightClickCont.getFirst(),rightClickCont.getSecond()));
		view.popup.show(e.getComponent(), e.getX(), e.getY());
	}

	/**
	 * Returns the corresponding contact in the contact map given screen
	 * coordinates
	 */
	private Pair<Integer> screen2cm(Point point){
		return new Pair<Integer>((int) Math.ceil(point.y/ratio),(int) Math.ceil(point.x/ratio));
	}

	/**
	 * Returns upper left corner of the square representing the contact
	 */
	private Point getCellUpperLeft(Pair<Integer> cont){
		return new Point((int) Math.round((cont.getSecond()-1)*ratio), (int) Math.round((cont.getFirst()-1)*ratio));
	}

	/**
	 * Return the center of a cell on screen given its coordinates in the
	 * contact map
	 */
	private Point getCellCenter(Pair<Integer> cont){
		Point point = getCellUpperLeft(cont);
		return new Point (point.x+(int)Math.ceil(ratio/2),point.y+(int)Math.ceil(ratio/2));
	}

	/**
	 * Return the upper right corner of a cell on screen given its coordinates
	 * in the contact map
	 */
	private Point getCellUpperRight(Pair<Integer> cont){
		Point point = getCellUpperLeft(cont);
		return new Point (point.x+(int)Math.ceil(ratio),point.y);
	}

	/**
	 * Return the lower left corner of a cell on screen given its coordinates in
	 * the contact map
	 */
	private Point getCellLowerLeft(Pair<Integer> cont){
		Point point = getCellUpperLeft(cont);
		return new Point (point.x,point.y+(int)Math.ceil(ratio));

	}

	/**
	 * Return the lower right corner of a cell on screen given its coordinates
	 * in the contact map
	 */
	private Point getCellLowerRight(Pair<Integer> cont){
		Point point = getCellUpperLeft(cont);
		return new Point (point.x+(int)Math.ceil(ratio),point.y+(int)Math.ceil(ratio));
		// actually these are the green contacts
	}

	/**
	 * Gets the common contacts for the given pair of input models. <p>
	 * NOTE: At the moment, 1 corresponds to the first model and 2 to the 
	 * second model. Any other combination of indices yields null as return 
	 * value.
	 * @param idxFirst  index indicating a certain model
	 * @param idxSecond  index indicating another model, supposed to be 
	 *  different from idxFirst
	 * @return the edge set of common contacts corresponding to the given model
	 *  identifiers
	 */
	public IntPairSet getCommonContacts(int idxFirst, int idxSecond) {
		if(idxFirst == 1 && idxSecond == 2 || idxFirst == 2 && idxSecond == 1) {
			return commonContacts;
		} else {
			return null;
		}
	}

	/**
	 * Gets the contacts of the model corresponding to the given index.
	 * @param whichOne  index indicating a certain model in the map
	 * @return  edge set of common contact, null if whichOne is not a valid 
	 *  model index
	 */
	public IntPairSet getModelContacts( int whichOne) {
		switch (whichOne) {
		case 1:
			return mainStrucContacts;
		case 2:
			return secStrucContacts;
		default:
			return null;	
		}
	}

	private int getRange(Pair<Integer> cont) {
		return Math.abs(cont.getFirst()-cont.getSecond());
	}
	
//	/** Returns the size in pixels of a single contact on screen
//	* TODO: Check whether this number is really the number in pixels (and not
//	plus or minus 1) */
//	private int getContactSquareSize() {
//	return contactSquareSize;
//	}

} 

