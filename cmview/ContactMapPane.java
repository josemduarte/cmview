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
import proteinstructure.RIGNode;
import proteinstructure.SecStrucElement;
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
	static final boolean BACKGROUND_LOADING = false;// if true, maps will be
													// calculated in a
													// separate thread
	static final boolean BG_PRELOADING = false;		// if true, maps will be
													// preloaded in
													// background

	protected enum ContactSelSet {COMMON, ONLY_FIRST, ONLY_SECOND}; 
	protected static final int FIRST = 0;
	protected static final int SECOND = 1;
	
	/*--------------------------- member variables --------------------------*/

	// underlying data
	private Model mod;
	private Model mod2;						// optional second model for cm
											// comparison
	private Alignment ali; 					// alignment between mod and mod2
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

	// selections 
	// NOTE: in compare mode contacts use alignment indexing starting from 1
	private IntPairSet allContacts; 		// all contacts from the underlying
											// contact map model (all first structure 
											// contacts in compare mode)
	private IntPairSet selContacts; 		// permanent list of currently selected
											// contacts
	private IntPairSet tmpContacts; 		// transient list of contacts selected
											// while dragging
	
	private IntPairSet allSecondContacts; 	// all second structure contacts

	private IntPairSet commonContacts;			// common contacts 
	private IntPairSet uniqueToFirstContacts;	// contacts unique to first structure
	private IntPairSet uniqueToSecondContacts;	// contacts unique to second structure

	private IntPairSet bothStrucContacts;		// i.e. uniqueToFirst+uniqueToSecond+common
	private IntPairSet allButCommonContacts;	// i.e. uniqueToFirst+uniqueToSecond
	
	private TreeSet<Integer> selHorNodes;			// current horizontal residue
													// selection
	private TreeSet<Integer> selVertNodes;			// current vertical residue
													// selection


	// other displayable data
	private Hashtable<Pair<Integer>,Color> userContactColors;  	// user defined colors
																// for individual
																// contacts
	private double[][] densityMatrix; 					 // matrix of contact
														// density
	private HashMap<Pair<Integer>,Integer> comNbhSizes;		// matrix of common
															// neighbourhood sizes
	private HashMap<Pair<Integer>,Double> diffDistMap;		// difference distance map (in comparison mode)

	// buffers for triple buffering
	private ScreenBuffer screenBuffer;		// buffer containing the more or
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
	private Color commonContactsColor; 		// color for common contacts
											// in compare mode
	private Color uniqueToFirstContactsColor; 	// color for contacts unique to 
												// first structure in compare mode
	private Color uniqueToSecondContactsColor;	// color for contacts unique to 
												// second structure in compare mode

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
	public ContactMapPane(Model mod, Alignment ali, View view){
		//this.mod = mod;  // outsourced the setting of the model to function setModel which is invoked further below
		this.mod2 = null;
		this.view = view;
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
		this.setOpaque(true); // make this component opaque
		this.setBorder(BorderFactory.createLineBorder(Color.black));
		this.screenSize = new Dimension(Start.INITIAL_SCREEN_SIZE, Start.INITIAL_SCREEN_SIZE);

		this.mousePos = new Point();
		this.mousePressedPos = new Point();
		this.mouseDraggingPos = new Point();

		// sets the model, initialises all data members
		setModel(mod, ali);
		
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
		this.uniqueToFirstContactsColor = Color.magenta;
		this.uniqueToSecondContactsColor = Color.green;

		setBackground(backgroundColor);
	}

	/**
	 * Sets the model of which the contact map is supposed to be painted.
	 * 
	 * @param mod  the model to be set
	 */
	public void setModel(Model mod, Alignment ali) {
		this.mod = mod;
		this.ali = ali;
		this.allContacts = mapContactSetToAlignment(mod.getLoadedGraphID(),mod.getContacts());
		this.selContacts = new IntPairSet();
		this.tmpContacts = new IntPairSet();
		this.selHorNodes = new TreeSet<Integer>();
		this.selVertNodes = new TreeSet<Integer>();
		this.commonContacts = new IntPairSet();
		this.uniqueToFirstContacts = new IntPairSet();
		this.uniqueToSecondContacts = new IntPairSet();

		this.contactMapSize = ali.getAlignmentLength(); // Note: this used to be mod.getMatrixSize() before we introduced the alignment also for 1 model 
		// initializes outputSize, ratio and contactSquareSize
		setOutputSize(Math.min(screenSize.height, screenSize.width)); 		
		
		this.dragging = false;
		this.currCommonNbh = null; // no common nbh selected
		this.showCommonNbs = false;
		this.showRulerCoord = false;
		this.userContactColors = new Hashtable<Pair<Integer>, Color>();
		this.densityMatrix = null;
		this.comNbhSizes = null;
		this.diffDistMap = null;

	}

	/** 
	 * Add the given new model for contact map comparison. 
	 * @param mod2  the second model
	 * @param ali	the alignment of first and second model
	 */
	public void setSecondModel(Model mod2, Alignment ali) {

		this.mod2 = mod2;
		this.ali = ali;

		// re-mapping new structure through alignment
		this.allContacts = mapContactSetToAlignment(mod.getLoadedGraphID(),mod.getContacts());
		// getting all contacts of the second structure, mapping through alignment
		this.allSecondContacts = mapContactSetToAlignment(mod2.getLoadedGraphID(),mod2.getContacts());

		// now getting the 3 sets: common, uniqueToFirst, uniqueToSecond

		for (Pair<Integer> cont2:allSecondContacts){
			// contacts in second and also in first are common
			if (allContacts.contains(cont2)) {
				commonContacts.add(cont2);
			}
			// contacts in second and not in first are uniqueToSecond
			else if(!allContacts.contains(cont2)){
				uniqueToSecondContacts.add(cont2);
			}
		}

		// contacts in first and not in second are uniqueToFirst
		for (Pair<Integer> cont:allContacts){
			if (!allSecondContacts.contains(cont)){
				uniqueToFirstContacts.add(cont);
			}
		}

		// bothStrucContacts = uniqueToFirst+uniqueToSecond+common
		bothStrucContacts = new IntPairSet();
		bothStrucContacts.addAll(uniqueToFirstContacts);
		bothStrucContacts.addAll(uniqueToSecondContacts);
		bothStrucContacts.addAll(commonContacts);

		// allButCommon = uniqueToFirst+uniqueToSecond
		allButCommonContacts = new IntPairSet();
		allButCommonContacts.addAll(uniqueToFirstContacts);
		allButCommonContacts.addAll(uniqueToSecondContacts);

		// finally resetting things  
		this.contactMapSize = ali.getAlignmentLength();
		// initializes outputSize, ratio and contactSquareSize
		setOutputSize(Math.min(screenSize.height, screenSize.width)); 

		this.selContacts = new IntPairSet();
		this.tmpContacts = new IntPairSet();
		this.selHorNodes = new TreeSet<Integer>();
		this.selVertNodes = new TreeSet<Integer>();

		this.dragging = false;
		this.currCommonNbh = null; // no common nbh selected
		this.showCommonNbs = false;
		this.showRulerCoord = false;
		this.userContactColors = new Hashtable<Pair<Integer>, Color>();
		this.densityMatrix = null;
		this.comNbhSizes = null;
		this.diffDistMap = null;

	}

	/**
	 * Given a set of contacts with sequence indexing returns a new set of 
	 * contacts with alignment indexing
	 * @param contacts
	 * @param tag 
	 */
	private IntPairSet mapContactSetToAlignment(String tag, IntPairSet contacts) {
		IntPairSet aliContacts = new IntPairSet();
		for (Pair<Integer> cont:contacts) {
			aliContacts.add(new Pair<Integer>(mapSeq2Al(tag,cont.getFirst()),mapSeq2Al(tag,cont.getSecond())));
		}
		return aliContacts;
	}
	
	/**
	 * Maps from sequence index to alignment index
	 * Method is protected to be called from ResidueRuler
	 * @param tag
	 * @param alignIdx
	 * @return
	 */
	protected int mapSeq2Al (String tag, int seqIdx) {
		return ali.seq2al(tag, seqIdx);
	}
	
	/**
	 * Maps from alignment index to sequence index.
	 * Method is protected to be called from ResidueRuler
	 * @param tag
	 * @param aliIdx
	 * @return
	 */
	protected int mapAl2Seq (String tag, int aliIdx) {
		return ali.al2seq(tag, aliIdx);
	}
	
	/**
	 * Maps given contact from alignment indices to sequence indices
	 * @param tag
	 * @param cont
	 * @return
	 */
	private Pair<Integer> mapContactAl2Seq (String tag, Pair<Integer> cont) {
		return new Pair<Integer>(mapAl2Seq(tag,cont.getFirst()),mapAl2Seq(tag,cont.getSecond()));
	}
	
	/**
	 * Returns the alignment. Used to get the alignment from ResidueRuler
	 * @return
	 */
	protected Alignment getAlignment() {
		return ali;
	}
	
	/**
	 * Checks if a second model has been assigned.
	 * @return true if so, else false
	 * @see #getSecondModel()
	 */
	public boolean hasSecondModel() {
		return mod2 != null;
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

			drawContacts(g2d,tmpContacts,selContactsColor);
		} 

		// drawing temp selection in red while dragging in range selection mode
		if (dragging && view.getCurrentSelectionMode()==View.DIAG_SEL) {

			g2d.setColor(diagCrosshairColor);
			g2d.drawLine(mousePressedPos.x-mousePressedPos.y, 0, outputSize, outputSize-(mousePressedPos.x-mousePressedPos.y));

			drawContacts(g2d,tmpContacts,selContactsColor);
		}

		// draw permanent selection in red
		drawContacts(g2d,selContacts,selContactsColor);

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
			drawCommonNeighbors(g2d, cont);
			this.showCommonNbs = false;
		}
	}

	private void drawHorizontalNodeSelection(Graphics2D g2d, Interval residues) {
		Point upperLeft = getCellUpperLeft(new Pair<Integer>(residues.beg,1));
		Point lowerRight = getCellLowerRight(new Pair<Integer>(residues.end, contactMapSize));
		g2d.fillRect(upperLeft.x, upperLeft.y, lowerRight.x-upperLeft.x + 1, lowerRight.y - upperLeft.y + 1);	// TODO: Check this
	}

	private void drawVerticalNodeSelection(Graphics2D g2d, Interval residues) {
		Point upperLeft = getCellUpperLeft(new Pair<Integer>(1,residues.beg));
		Point lowerRight = getCellLowerRight(new Pair<Integer>(contactMapSize, residues.end));
		g2d.fillRect(upperLeft.x, upperLeft.y, lowerRight.x-upperLeft.x + 1, lowerRight.y - upperLeft.y + 1);	// TODO: Check this
	}

	/**
	 * @param g2d
	 */
	private void drawDistanceMap(Graphics2D g2d) {
		// this actually contains all cells in matrix so is doing a
		// full loop on all cells
		//TODO indices here refer to sequence, while on screen we have alignment indices. This is fine for single mode, but needs to be changed if we allow distance map in compare mode
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
	 * Draws the contact map (or the 2 contact maps in compare mode)
	 * @param g2d
	 */
	private void drawContactMap(Graphics2D g2d) {
		if (!view.getCompareStatus()) { // single contact map mode
			for (Pair<Integer> cont:allContacts){
				// in single contact map mode we can also have contacts colored by user
				if(userContactColors.containsKey(cont)) {
					g2d.setColor(userContactColors.get(cont)); 
				} else {
					g2d.setColor(contactColor);
				}
				drawContact(g2d, cont);
			}
		} else { // compare mode
			// 1) common=0, first=0, second=1
			if (view.getShowCommon() == false && view.getShowFirst() == false && view.getShowSecond() == true){
				drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor);
			}
			// 2) common=0, first=1, second=0 
			else if (view.getShowCommon() == false && view.getShowFirst() == true && view.getShowSecond() == false){
				drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor);
			}
			// 3) common=0, first=1, second=1
			else if (view.getShowCommon() == false && view.getShowFirst() == true && view.getShowSecond() == true){
				drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor);
				drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor);
			}
			// 4) common=1, first=0, second=0
			else if (view.getShowCommon() == true && view.getShowFirst() == false && view.getShowSecond() == false){
				drawContacts(g2d,commonContacts,commonContactsColor);
			}
			// 5) common=1, first=0, second=1
			else if (view.getShowCommon() == true && view.getShowFirst() == false && view.getShowSecond() == true){
				drawContacts(g2d,commonContacts,commonContactsColor);
				drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor);
			}
			// 6) common=1, first=1, second=0
			else if (view.getShowCommon() == true && view.getShowFirst() == true && view.getShowSecond() == false){
				drawContacts(g2d,commonContacts,commonContactsColor);
				drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor);
			}
			// 7) common=1, first=1, second=1
			else if (view.getShowCommon() == true && view.getShowFirst() == true && view.getShowSecond() == true) {
				drawContacts(g2d,commonContacts,commonContactsColor);
				drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor);
				drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor);
			// 8) common=0, first=0, second=0
			} else { 
				// do nothing
			}
			
		}
	}
	
	/**
	 * Draws contacts for the given contact set in the given color
	 * @param g2d
	 * @param contactSet
	 * @param color
	 */
	private void drawContacts(Graphics2D g2d, IntPairSet contactSet, Color color) {
		g2d.setColor(color);
		for(Pair<Integer> cont:contactSet){
			drawContact(g2d, cont);
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
		// this actually contains all cells in matrix so is doing a
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
		if( !this.hasSecondModel() ) {
			drawCoordinates(g2d,mod,allContacts,20,outputSize-90,false);
		} else {
			drawCoordinates(g2d,mod,allContacts,20,outputSize-90,Start.SHOW_ALIGNMENT_COORDS);
			drawCoordinates(g2d,mod2,allSecondContacts,180,outputSize-90,Start.SHOW_ALIGNMENT_COORDS);
		}
	}

	/**
	 * Draws coordinates for the given model onto the given graphic. Please 
	 * note, that whenever an ordinate of the current mouse position in the 
	 * graphic equals zero the coordinates will not be printed. 
	 * @param g2d  the graphic
	 * @param mod  the model
	 * @param modContacts  all contacts of mod
	 * @param x  offset position in the x dimension of the upper left corner
	 * @param y  offset position in the y dimension of the upper left corner
	 * @param showALiAndSeqPos  toggles between to different position modes: 
	 *  true -> show alignment as well as sequence positions, false -> show 
	 *  sequence position only
	 */
	protected void drawCoordinates(Graphics2D g2d, Model mod, IntPairSet modContacts, int x, int y, boolean showAliAndSeqPos) {
		Pair<Integer> currentCell = screen2cm(mousePos);
		// alignment indices
		int iAliIdx = currentCell.getFirst();
		int jAliIdx = currentCell.getSecond();
		
		// return if any position equals zero
		if( currentCell.getFirst() == 0 || currentCell.getSecond() == 0 ) {
			return;
		}

		String title = mod.getLoadedGraphID()+":";
		String aliTag = mod.getLoadedGraphID();
		
		// converting to sequence indices
		int iSeqIdx = mapAl2Seq(aliTag,iAliIdx);
		int jSeqIdx = mapAl2Seq(aliTag,jAliIdx);
		
		RIGNode iNode = null;
		RIGNode jNode = null;
		String i_res = "";
		String j_res = "";
		
		// handling gaps
		if (iSeqIdx < 0) {
			i_res=AAinfo.getGapCharacterOneLetter()+"";
		} else {
			iNode = mod.getNodeFromSerial(iSeqIdx);
			// handling unobserves
			i_res = iNode==null?"?":iNode.getResidueType();
		}
		if (jSeqIdx < 0) {
			j_res=AAinfo.getGapCharacterOneLetter()+"";
		} else {
			jNode = mod.getNodeFromSerial(jSeqIdx);
			// handling unobserves
			j_res = jNode==null?"?":jNode.getResidueType();
		}
		
		
		int extraX = 0;
		if( showAliAndSeqPos ) {
			extraX = 30;
		}

		g2d.setColor(coordinatesColor);

		int extraTitleY = 0;
		if(hasSecondModel()) {
			extraTitleY = 20;	
			g2d.drawString(title, x, y);
		}
		
		// writing i and j
		g2d.drawString("i", x,           y+extraTitleY);
		g2d.drawString("j", x+extraX+40, y+extraTitleY);

		// writing coordinates and optionally alignment coordinates
		g2d.drawString(iSeqIdx<0?"":iSeqIdx+"", x,           y+extraTitleY+20);
		g2d.drawString(jSeqIdx<0?"":jSeqIdx+"", x+extraX+40, y+extraTitleY+20);
		if( hasSecondModel() && showAliAndSeqPos ) {
			g2d.drawString("(" + iAliIdx + ")", x+extraX,      y+extraTitleY+20);
			g2d.drawString("(" + jAliIdx + ")", x+2*extraX+40, y+extraTitleY+20);
		}		 

		// writing residue types
		g2d.drawString(i_res, x,           y+extraTitleY+40);
		g2d.drawString(j_res, x+extraX+40, y+extraTitleY+40);
		
		// writing secondary structure
		if (mod.hasSecondaryStructure()){
			SecStrucElement iSSElem = iNode==null?null:iNode.getSecStrucElement();
			SecStrucElement jSSElem = jNode==null?null:jNode.getSecStrucElement();
			Character iSSType = iSSElem==null?' ':iSSElem.getType();
			Character jSSType = jSSElem==null?' ':jSSElem.getType();
			switch(iSSType) {
			case 'H': iSSType = '\u03b1'; break;	// alpha
			case 'S': iSSType = '\u03b2'; break;	// beta
			default: iSSType = ' ';
			}
			switch(jSSType) {
			case 'H': jSSType = '\u03b1'; break;
			case 'S': jSSType = '\u03b2'; break;
			default: jSSType = ' ';
			}
			g2d.drawString(Character.toString(iSSType), x,           y+extraTitleY+60);
			g2d.drawString(Character.toString(jSSType), x+extraX+40, y+extraTitleY+60);
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
			String i_pdbresser = mod.getPdbResSerial(iSeqIdx);
			String j_pdbresser = mod.getPdbResSerial(jSeqIdx);
			g2d.drawString(i_pdbresser==null?"?":i_pdbresser, x,           y+extraTitleY+80);
			g2d.drawString(j_pdbresser==null?"?":j_pdbresser, x+extraX+40, y+extraTitleY+80);
		}
	}

	// TODO: a) Merge this with above function? b) missing second model display, if we allow ruler in compare mode, we must fix this
	private void drawRulerCoord(Graphics2D g2d) {
		int seqIdx = mapAl2Seq(mod.getLoadedGraphID(),currentRulerCoord);
		RIGNode node = null;
		String res = "";
		if (seqIdx<0) { // handling gaps
			res = AAinfo.getGapCharacterOneLetter()+"";
		} else {
			node = mod.getNodeFromSerial(seqIdx);
			// handling unobserves
			res = node==null?"?":node.getResidueType();
		}
		g2d.setColor(coordinatesColor);
		g2d.drawString("i", 20, outputSize-90);
		g2d.drawString(seqIdx+"", 20, outputSize-70);

		g2d.drawString(res, 20, outputSize-50);
		if (mod.hasSecondaryStructure()){
			SecStrucElement ssElem = node==null?null:node.getSecStrucElement();
			Character ssType = ssElem==null?' ':ssElem.getType();
			switch(ssType) {
			case 'H': ssType = '\u03b1'; break;	// alpha
			case 'S': ssType = '\u03b2'; break;	// beta
			default: ssType = ' ';
			}			
			g2d.drawString(Character.toString(ssType), 20, outputSize-30);
		}
		if (view.getShowPdbSers()){
			String pdbresser = mod.getPdbResSerial(seqIdx);
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

	private void drawCommonNeighbors(Graphics2D g2d, Pair<Integer> cont){
		RIGCommonNbhood comNbh = this.currCommonNbh; // currCommonNbh is in sequence indexing

		System.out.println("Selecting common neighbours for " + (allContacts.contains(cont)?"contact ":"") + cont);
		System.out.println("Common neighbourhood string: "+comNbh);
		System.out.println("Common neighbours: " + comNbh.getCommaSeparatedResSerials());
		// drawing corridor
		drawCorridor(cont, g2d);

		// marking the selected point with a cross
		drawCrossOnContact(cont, g2d, crossOnContactColor);

		// drawing triangles
		for (int k:comNbh.keySet()){ // k is each common neighbour (residue serial in sequence indexing)
			int kAlIdx = mapSeq2Al(mod.getLoadedGraphID(), k); //TODO if we allow common neighbors in compare mode, then mod here must refer to the correct model
			if (kAlIdx>cont.getFirst() && kAlIdx<cont.getSecond()) {
				// draw cyan triangles for neighbours within the box
				drawTriangle(kAlIdx, cont, g2d, inBoxTriangleColor);
			}
			else { // i.e. k<cont.getFirst() || k>cont.getSecond()
				// draw red triangles for neighbours out of the box
				drawTriangle(kAlIdx, cont, g2d, outBoxTriangleColor);
			}
		}
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
		Pair<Integer> kkCont = new Pair<Integer>(k,k); 	// k node point in the diagonal: the
														// start point of the light gray
														// corridor
		Point kkPoint = getCellCenter(kkCont);
		Point endPoint = new Point();
		if (k<(j+i)/2) endPoint = getCellCenter(new Pair<Integer>(j,k));// if k below
																		// center of
																		// segment i->j,
																		// the endpoint
																		// is j,k i.e.
																		// we draw a
																		// vertical line
		if (k>(j+i)/2) endPoint = getCellCenter(new Pair<Integer>(k,i));// if k above
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
		// Called whenever the user releases the mouse button.
		// TODO: Move much of this to MouseClicked and pull up Contact cont = screen2cm...
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
				this.currCommonNbh = mod.getCommonNbhood (mapAl2Seq(mod.getLoadedGraphID(),c.getFirst()),mapAl2Seq(mod.getLoadedGraphID(),c.getSecond()));
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
					if(getCurrentContactSet().contains(clicked)) {  // if clicked position
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

				fillSelect(getCurrentContactSet(), screen2cm(new Point(evt.getX(),evt.getY())));

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
				squareSelect(getCurrentContactSet());
				break;
			case View.DIAG_SEL:
				rangeSelect(getCurrentContactSet());
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

	}

	public void componentMoved(ComponentEvent evt) {

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

		// draw difference distance map (in comparison mode)
		if(view.getCompareStatus() && view.getShowDiffDistMap()) {
			drawDiffDistMap(g2d);
		}
		
		// draw contact map if necessary (single or comparison)
		if(!view.getShowNbhSizeMap() && !view.getShowDistMap()) {
			drawContactMap(g2d);			
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
				//TODO indices in comNbhSizes matrix refer to sequence, while on screen we have alignment indices. This is fine for single mode, but needs to be changed if we allow com nbh sizes in compare mode				
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
				//TODO indices in density matrix refer to sequence, while on screen we have alignment indices. This is fine for single model, but needs to be changed if we allow density map in compare mode
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
		
		// reloading contacts of 1st structure (or single)
		this.allContacts = mapContactSetToAlignment(mod.getLoadedGraphID(),mod.getContacts());
		
		// reloading contacts of 2nd structure if it's present
		// this is not used at the moment, since delete contacts is not 
		// allowed in compare mode, but it's good to keep it here in case 
		// we allow any modification of the second model in the future
		if (this.hasSecondModel()) {
			this.allSecondContacts = mapContactSetToAlignment(mod2.getLoadedGraphID(),mod2.getContacts());
			// now getting the 3 sets: common, uniqueToFirst, uniqueToSecond
			for (Pair<Integer> cont2:allSecondContacts){
				// contacts in second and also in first are common
				if (allContacts.contains(cont2)) {
					commonContacts.add(cont2);
				}
				// contacts in second and not in first are uniqueToSecond
				else if(!allContacts.contains(cont2)){
					uniqueToSecondContacts.add(cont2);
				}
			}
			// contacts in first and not in second are uniqueToFirst
			for (Pair<Integer> cont:allContacts){
				if (!allSecondContacts.contains(cont)){
					uniqueToFirstContacts.add(cont);
				}
			}
			// bothStrucContacts = uniqueToFirst+uniqueToSecond+common
			bothStrucContacts = new IntPairSet();
			bothStrucContacts.addAll(uniqueToFirstContacts);
			bothStrucContacts.addAll(uniqueToSecondContacts);
			bothStrucContacts.addAll(commonContacts);
			// allButCommon = uniqueToFirst+uniqueToSecond
			allButCommonContacts = new IntPairSet();
			allButCommonContacts.addAll(uniqueToFirstContacts);
			allButCommonContacts.addAll(uniqueToSecondContacts);
		}
		
		// updating maps
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

		selContacts.addAll(getCurrentContactSet());

		this.repaint();
	}

	/** Called by view to select all helix-helix contacts */
	public void selectHelixHelix() {
		selContacts = new IntPairSet();
		for(Pair<Integer> e:allContacts) {
			SecStrucElement ss1 = mod.getSecondaryStructure().getSecStrucElement(mapAl2Seq(mod.getLoadedGraphID(),e.getFirst()));
			SecStrucElement ss2 = mod.getSecondaryStructure().getSecStrucElement(mapAl2Seq(mod.getLoadedGraphID(),e.getSecond()));
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
			SecStrucElement ss1 = mod.getSecondaryStructure().getSecStrucElement(mapAl2Seq(mod.getLoadedGraphID(),e.getFirst()));
			SecStrucElement ss2 = mod.getSecondaryStructure().getSecStrucElement(mapAl2Seq(mod.getLoadedGraphID(),e.getSecond()));
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
			SecStrucElement ss1 = mod.getSecondaryStructure().getSecStrucElement(mapAl2Seq(mod.getLoadedGraphID(),e.getFirst()));
			SecStrucElement ss2 = mod.getSecondaryStructure().getSecStrucElement(mapAl2Seq(mod.getLoadedGraphID(),e.getSecond()));
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
			SecStrucElement ss1 = mod.getSecondaryStructure().getSecStrucElement(mapAl2Seq(mod.getLoadedGraphID(),e.getFirst()));
			SecStrucElement ss2 = mod.getSecondaryStructure().getSecStrucElement(mapAl2Seq(mod.getLoadedGraphID(),e.getSecond()));
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
		//TODO at the moment this doesn't work for compare mode, but there's no reason why it wouldn't. What we'd need to do:
		// 		- take 2 selections one for structure 1 and the other for 2, alternatively force user to use alignment indices
		//		- use getCurrentContactSet instead of allContacts
		if(selStr.length() == 0) return 0;	// nothing to select
		if(!Interval.isValidSelectionString(selStr)) return -1;
		TreeSet<Integer> nodeSet1 = Interval.parseSelectionString(selStr);
		TreeSet<Integer> nodeSet2 = Interval.parseSelectionString(selStr);
		selContacts = new IntPairSet();
		for(Pair<Integer> e:allContacts) { 
			if(nodeSet1.contains(mapAl2Seq(mod.getLoadedGraphID(),e.getFirst())) && nodeSet2.contains(mapAl2Seq(mod.getLoadedGraphID(),e.getSecond()))) {
				selContacts.add(e);
			}
		}
		this.repaint();
		return selContacts.size();
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
	 * Resets selections if sets of contacts are being hidden
	 * Later we might want to put more stuff in this method, that's the reason
	 * for the name (instead of being called resetSelectionsWhenToggling)
	 * To be called when one of the 3 showing contacts button (common, first, 
	 * second) is clicked
	 * @param state true if we switch to show, false if we switch to hide
	 */
	protected void toggleShownContacts(boolean state) {
		if (state == false) { //we are hiding a set of contacts: we reset selection 
			this.resetSelections();
		}
		this.updateScreenBuffer(); // takes care of redrawing contact map
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

	/**
	 * Deletes currently selected contacts from first and from second model if present
	 */
	public void deleteSelectedContacts() {
		for (Pair<Integer> cont:selContacts){
			mod.removeEdge(mapContactAl2Seq(mod.getLoadedGraphID(), cont));
			if (hasSecondModel()) {
				mod2.removeEdge(mapContactAl2Seq(mod2.getLoadedGraphID(), cont));
			}
		}
		resetSelections();
		reloadContacts();	// will update screen buffer and repaint
	}

	/**
	 * Update tmpContact with the contacts contained in the rectangle given by
	 * the upperLeft and lowerRight.
	 */
	private void squareSelect(IntPairSet contacts){
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
	private void rangeSelect(IntPairSet contacts){
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

	/**
	 * Add to the current selection all contacts along the diagonal that
	 * contains cont
	 */
	private void selectDiagonal(int range) {
		for(Pair<Integer> c: getCurrentContactSet()) {
			if(getRange(c) == range) {
				selContacts.add(c);
			}
		}
	}

	/**
	 * Gets the current set of contacts depending on the state of the GUI:
	 * - single/comparison mode
	 * - within comparison mode: 8 states for the combinations of the 3 buttons 
	 *   showCommon, showFirst, showSecond
	 * @return
	 */
	private IntPairSet getCurrentContactSet() {
		// pairwise comparison mode
		if (this.hasSecondModel()){
			// 1) common=0, first=0, second=1
			if (view.getShowCommon() == false && view.getShowFirst() == false && view.getShowSecond() == true){
				return uniqueToSecondContacts;
			}
			// 2) common=0, first=1, second=0
			else if (view.getShowCommon() == false && view.getShowFirst() == true && view.getShowSecond() == false){
				return uniqueToFirstContacts;
			}
			// 3) common=0, first=1, second=1
			else if (view.getShowCommon() == false && view.getShowFirst() == true && view.getShowSecond() == true){
				return allButCommonContacts;
			}
			// 4) common=1, first=0, second=0
			else if (view.getShowCommon() == true && view.getShowFirst() == false && view.getShowSecond() == false){
				return commonContacts;
			}
			// 5) common=1, first=0, second=1
			else if (view.getShowCommon() == true && view.getShowFirst() == false && view.getShowSecond() == true){
				return allSecondContacts;
			}
			// 6) common=1, first=1, second=0
			else if (view.getShowCommon() == true && view.getShowFirst() == true && view.getShowSecond() == false){
				return allContacts;
			}
			// 7) common=1, first=1, second=1
			else if (view.getShowCommon() == true && view.getShowFirst() == true && view.getShowSecond() == true){
				return bothStrucContacts;
			}
			return new IntPairSet(); // in case common=0, first=0, second=0, we return an empty set
		// single contact map mode
		} else {
			return allContacts;
		}
	}
	
	protected void selectNodeNbh(int i) {
		int seqIdx = mapAl2Seq(mod.getLoadedGraphID(),i);
		RIGNbhood nbh = mod.getNbhood(seqIdx);
		System.out.println("Selecting neighbourhood of residue: "+seqIdx);
		System.out.println("Neighbourhood string: "+nbh);
		System.out.println("Neighbours: " + nbh.getCommaSeparatedResSerials());
		for (RIGNode j:nbh.getNeighbors()){
			selContacts.add(new Pair<Integer>(Math.min(seqIdx, j.getResidueSerial()),Math.max(seqIdx, j.getResidueSerial())));
		}
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
	 * Gets the 6 sets of selected contacts for displaying in 3D (e.g. PyMol)
	 * in sequence indexing (so converted from alignment indices which is what 
	 * selContacts uses)
	 * The 6 sets are:
	 * COMMON:
	 *   FIRST -> common contacts with first model's sequence indices (to draw solid yellow lines)
	 *	 SECOND -> common contacts with second model's sequence indices (to draw solid yellow lines)
	 * ONLY_FIRST:
	 *	 FIRST -> contacts only in first model with first model's sequence indices (to draw solid red lines)
	 *	 SECOND -> contacts only in first model with second model's sequence indices, i.e. "absent" contacts (to draw dashed green lines)
	 * ONLY_SECOND:
	 *	 SECOND -> contacts only in second model with second model's sequence indices (to draw solid green lines)
	 *	 FIRST -> contacts only in second model with first model's sequence indices, i.e. "absent" contacts (to draw dashed red lines)
     *
	 * @return mapping from {@link ContactSelSet} type to an array of contacts. 
	 *  The array entries are supposed to be adressed by {@link FIRST} and 
	 *  {@link SECOND} yielding the contacts for the first and second model, 
	 *  respectively. According to the currently selected contacts these 
	 *  contact lists might be empty! 
	 */
	public TreeMap<ContactSelSet,IntPairSet[]> getSetsOfSelectedContactsFor3D(){
		
		TreeMap<ContactSelSet,IntPairSet[]> selMap = new TreeMap<ContactSelSet, IntPairSet[]>();

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


		for (Pair<Integer> cont : selContacts) {
			if (commonContacts.contains(cont)) {
				common[FIRST].add(new Pair<Integer>(mapAl2Seq(mod.getLoadedGraphID(),cont.getFirst()), 
						mapAl2Seq(mod.getLoadedGraphID(), cont.getSecond())));
				common[SECOND].add(new Pair<Integer>(mapAl2Seq(mod2.getLoadedGraphID(),cont.getFirst()),
						mapAl2Seq(mod2.getLoadedGraphID(), cont.getSecond())));
			}

			else if (uniqueToFirstContacts.contains(cont)) {

				firstOnly[FIRST].add(new Pair<Integer>(mapAl2Seq(mod.getLoadedGraphID(),cont.getFirst()),
						mapAl2Seq(mod.getLoadedGraphID(), cont.getSecond())));

				pos1 = mapAl2Seq(mod2.getLoadedGraphID(), cont.getFirst());
				pos2 = mapAl2Seq(mod2.getLoadedGraphID(), cont.getSecond());

				if( pos1 != -1 && pos2 != -1 ) {
					firstOnly[SECOND].add(new Pair<Integer>(pos1,pos2));
				}
			}

			else if (uniqueToSecondContacts.contains(cont)) {
				secondOnly[SECOND].add(new Pair<Integer>(mapAl2Seq(mod2.getLoadedGraphID(),cont.getFirst()),
						mapAl2Seq(mod2.getLoadedGraphID(), cont.getSecond())));

				pos1 = mapAl2Seq(mod.getLoadedGraphID(), cont.getFirst());
				pos2 = mapAl2Seq(mod.getLoadedGraphID(), cont.getSecond());

				if( pos1 != -1 && pos2 != -1 ) {
					secondOnly[FIRST].add(new Pair<Integer>(pos1,pos2));
				}
			}
		}

		selMap.put(ContactSelSet.COMMON,     common);
		selMap.put(ContactSelSet.ONLY_FIRST, firstOnly);
		selMap.put(ContactSelSet.ONLY_SECOND,secondOnly);
			
		return selMap;
	}

	
	/**
	 * Returns a set of end points of the currently selected contacts 
	 * in alignment indexing starting from 1.
	 * @return
	 */
	public TreeSet<Integer> getAlignmentColumnsFromSelectedContacts() { 

		TreeSet<Integer> positions = new TreeSet<Integer>();
		
		getAlignmentColumnsFromContacts(selContacts,positions);
		
		return positions;
	}
	
	/**
	 * Given a set of contacts puts into <code>positions</code> their 
	 * end points in alignment indexing starting from 1
	 * @param contacts  set of contacts
	 * @param positions  contains the alignment columns incident to the 
	 *  contacts in <code>contacts</code>
	 */
	public void getAlignmentColumnsFromContacts(IntPairSet contacts, TreeSet<Integer> positions) {
		for( Pair<Integer> cont : contacts ) {
			positions.add(cont.getFirst());
			positions.add(cont.getSecond());
		}
	}

	/** Returns the set of horizontally selected nodes. */
	public TreeSet<Integer> getSelHorNodes() {
		return selHorNodes;
	}

	/** Returns the set of vertically selected nodes. */
	public TreeSet<Integer> getSelVertNodes() {
		return selVertNodes;
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
		// we want to show sequence indices to the user, that's why we map here TODO at the moment will not work for compare mode because we use mod1 for the al2seq mapping
		view.popupSendEdge.setText(String.format(View.LABEL_SHOW_PAIR_DIST_3D,mapAl2Seq(mod.getLoadedGraphID(),rightClickCont.getFirst()),mapAl2Seq(mod.getLoadedGraphID(),rightClickCont.getSecond())));
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
	}

	/**
	 * Gets the common contacts for the 2 loaded models
	 * @return the set of common contacts corresponding, null if there's no 
	 * second model 
	 */
	public IntPairSet getCommonContacts() {
		if(this.hasSecondModel()) {
			return commonContacts;
		} else {
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

