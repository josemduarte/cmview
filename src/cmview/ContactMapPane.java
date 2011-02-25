package cmview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;

import owl.core.sequence.alignment.MultipleSequenceAlignment;
import owl.core.structure.features.SecStrucElement;
import owl.core.structure.graphs.RIGCommonNbhood;
import owl.core.structure.graphs.RIGNbhood;
import owl.core.structure.graphs.RIGNode;
import owl.core.structure.scoring.ResidueContactScoringFunction;
import owl.core.util.IntPairSet;
import owl.core.util.Interval;

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
	
	private int hitcounter = 0;
	// underlying data
	private Model mod;
	private Model mod2;						// optional second model for cm, is null for single model mode
											// comparison
	private MultipleSequenceAlignment ali; 					// alignment between mod and mod2
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
											// Neighbors
	private Point mouseDraggingPos;  		// current position of mouse
											// dragging, used for end point of
											// square selection
	private Point mousePos;             	// current position of mouse (being
											// updated by mouseMoved)
	private Pair<Integer> mouseCell; 		// current contact map coordinate of mouse
											// updated by mouseMoved
	private Pair<Integer> lastMouseCell; 	// last known contact map coordinate of mouse	
											// updated by mouseMoved
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
	//private IntPairSet leftClickCont;	 		// position in contact map where
													// left mouse button was pressed
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
	private double[][] deltaRankMatrix;					// delta rank matrix (0-1)
	
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

	private StatusBar statusBar;
	private DeltaRankBar deltaRankBar;
											// threads are currently active

	/*----------------------------- constructors ----------------------------*/

	/**
	 * Create a new ContactMapPane.
	 * 
	 * @param mod
	 * @param view
	 */
	public ContactMapPane(Model mod, MultipleSequenceAlignment ali, View view){
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
		this.squareSelColor = Color.lightGray;	
		this.crosshairColor = Color.lightGray;
		this.diagCrosshairColor = Color.lightGray;
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
		setOpaque(true);
	}

	/**
	 * Sets the model of which the contact map is supposed to be painted.
	 * 
	 * @param mod  the model to be set
	 */
	public void setModel(Model mod, MultipleSequenceAlignment ali) {
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

		selContactsChanged(); // not sure if we need this call here but just in
		tmpContactsChanged(); // case sel- or tmpContact had some value before
				
	}

	/** 
	 * Add the given new model for contact map comparison. 
	 * @param mod2  the second model
	 * @param ali	the alignment of first and second model
	 */
	public void setSecondModel(Model mod2, MultipleSequenceAlignment ali) {

		this.mod2 = mod2;
		this.ali = ali;

		// re-mapping new structure through alignment
		this.allContacts = mapContactSetToAlignment(mod.getLoadedGraphID(),mod.getContacts());
		// getting all contacts of the second structure, mapping through alignment
		this.allSecondContacts = mapContactSetToAlignment(mod2.getLoadedGraphID(),mod2.getContacts());

		// resetting all other sets
		commonContacts = new IntPairSet();
		uniqueToFirstContacts = new IntPairSet();
		uniqueToSecondContacts = new IntPairSet();
		bothStrucContacts = new IntPairSet();
		allButCommonContacts = new IntPairSet();
		
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
		bothStrucContacts.addAll(uniqueToFirstContacts);
		bothStrucContacts.addAll(uniqueToSecondContacts);
		bothStrucContacts.addAll(commonContacts);

		// allButCommon = uniqueToFirst+uniqueToSecond
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
		this.deltaRankMatrix = null;
		
		selContactsChanged(); // not sure if we need this call here but just in
		tmpContactsChanged(); // case sel- or tmpContact had some value before

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
	 * Given a set of contacts with alignment indexing returns a new set of 
	 * contacts with sequence indexing (with possible gap values).
	 * @param contacts
	 * @param tag 
	 */
	private IntPairSet mapContactSetToSequence(String tag, IntPairSet contacts) {
		IntPairSet aliContacts = new IntPairSet();
		for (Pair<Integer> cont:contacts) {
			aliContacts.add(new Pair<Integer>(mapAl2Seq(tag,cont.getFirst()),mapAl2Seq(tag,cont.getSecond())));
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
	Pair<Integer> mapContactAl2Seq (String tag, Pair<Integer> cont) {
		return new Pair<Integer>(mapAl2Seq(tag,cont.getFirst()),mapAl2Seq(tag,cont.getSecond()));
	}
	
	/**
	 * Returns the alignment. Used to get the alignment from ResidueRuler
	 * @return
	 */
	protected MultipleSequenceAlignment getAlignment() {
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
	 * Some additional stuff to draw for the loupe. This is very dirty
	 * for multiple reasons but it does what we want. 
	 * */
	protected synchronized void paintLoupe(Graphics2D g2d) {
		
		// draw temporary selection
		if (dragging && (view.getGUIState().getSelectionMode()==GUIState.SelMode.RECT
				      || view.getGUIState().getSelectionMode()==GUIState.SelMode.DIAG)) {
			drawContacts(g2d,tmpContacts,selContactsColor,true);
			drawContacts(g2d,tmpContacts,selContactsColor,false);
		}
		
		// draw permanent selection in red
		drawContacts(g2d,selContacts,selContactsColor,true);
		drawContacts(g2d,selContacts,selContactsColor,false);
	}
	
	/**
	 * Main method to draw the component on screen. This method is called each
	 * time the component has to be (re) drawn on screen. It is called
	 * automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	@Override
	protected synchronized void paintComponent(Graphics g) {
		//Image bufferImage = this.createImage(getWidth(), getHeight());
		//Graphics2D g2d = (Graphics2D) bufferImage.getGraphics();
		
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
		if (dragging && view.getGUIState().getSelectionMode()==GUIState.SelMode.RECT) {

			g2d.setColor(squareSelColor);
			int xmin = Math.min(mousePressedPos.x,mouseDraggingPos.x);
			int ymin = Math.min(mousePressedPos.y,mouseDraggingPos.y);
			int xmax = Math.max(mousePressedPos.x,mouseDraggingPos.x);
			int ymax = Math.max(mousePressedPos.y,mouseDraggingPos.y);
			g2d.drawRect(xmin,ymin,xmax-xmin,ymax-ymin);

			drawContacts(g2d,tmpContacts,selContactsColor,true);
			drawContacts(g2d,tmpContacts,selContactsColor,false);
		} 

		// drawing temp selection in red while dragging in range selection mode
		if (dragging && view.getGUIState().getSelectionMode()==GUIState.SelMode.DIAG) {

			g2d.setColor(diagCrosshairColor);
			g2d.drawLine(mousePressedPos.x-mousePressedPos.y, 0, outputSize, outputSize-(mousePressedPos.x-mousePressedPos.y));

			drawContacts(g2d,tmpContacts,selContactsColor,true);
			drawContacts(g2d,tmpContacts,selContactsColor,false);
			
		}

		// draw permanent selection in red
		drawContacts(g2d,selContacts,selContactsColor,true);
		drawContacts(g2d,selContacts,selContactsColor,false);
		
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
				drawRulerCoord();	// drawn in status bar
			}
			if(showRulerCrosshair) {
				drawRulerCrosshair(g2d);
			}			
		} else
			if (mouseIn && (mousePos.x <= outputSize) && (mousePos.y <= outputSize)){ // second term needed if window is not square shape
				drawCoordinates();
				drawCrosshair(g2d);
			} 

		// draw common neighbours
		if(this.showCommonNbs) {
			Pair<Integer> cont = screen2cm(mousePressedPos);
			drawCommonNeighbors(g2d, cont);
			this.showCommonNbs = false;
		}
		
		// actually draw bufferImage to screen
		//((Graphics2D) g).drawImage(bufferImage, null, this);
		
		// update loupe (without crosshair)
		if(mouseIn) view.loupePanel.updateLoupe(screenBuffer.getImage(), mousePos, contactSquareSize, this);
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
	private void drawDistanceMap(Graphics2D g2d,boolean secondMap) {
		// this actually contains all cells in matrix so is doing a
		// full loop on all cells
		//TODO indices here refer to sequence, while on screen we have alignment indices. This is fine for single mode, but needs to be changed if we allow distance map in compare mode
		HashMap<Pair<Integer>,Double> distMatrix = mod.getDistMatrix();
		for (Pair<Integer> cont:distMatrix.keySet()){
			Color c = colorMapScaledHeatmap(distMatrix.get(cont), scaledDistCutoff);
			g2d.setColor(c);
			drawContact(g2d, cont,secondMap);
		}
	}
	
	
	/**
	 * @param g2d
	 */
	private void drawNbhSizeMap(Graphics2D g2d, boolean secondMap) {
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
			drawContact(g2d, cont,secondMap);
		}
	}
	
	/**
	 * Draws the contact map (or the 2 contact maps in compare mode)
	 * @param g2d
	 */
	private void drawContactMap(Graphics2D g2d) {
		if (!view.getGUIState().getCompareMode()) { // single contact map mode
			for (Pair<Integer> cont:allContacts){
				// in single contact map mode we can also have contacts colored by user
				if(userContactColors.containsKey(cont)) {
					g2d.setColor(userContactColors.get(cont)); 
				} else {
					g2d.setColor(contactColor);
					if (Start.USE_EXPERIMENTAL_FEATURES && Start.SHOW_WEIGHTED_CONTACTS) {
						double weight = mod.getGraph().getEdgeFromSerials(cont.getFirst(), cont.getSecond()).getWeight();
						float truncWeight = (float) Math.max(0, Math.min(1, weight)); // truncated weight (if weights are off the (0,1) interval)
						if (Start.SHOW_WEIGHTS_IN_COLOR) {
							g2d.setColor(colorMapHeatmap(1.0f - truncWeight));
						} else { // gray shades
							float colorWeight = 1.0f - truncWeight;
							//g2d.setColor(new Color(colorWeight, colorWeight, colorWeight));	// w/o alpha channel (faster?)
							g2d.setColor(new Color(0f,0f,0f, 1.0f-colorWeight));	// use alpha channel
						}

					}
				}

				drawContact(g2d, cont,false,view.getGUIState().getShowBackground());
				drawContact(g2d, cont,true,view.getGUIState().getShowBottomBackground());
			}
		} else { // compare mode

			if(Start.USE_EXPERIMENTAL_FEATURES && Start.SHOW_WEIGHTED_CONTACTS) {

				// 1) common=0, first=0, second=1
				if (view.getGUIState().getShowCommon() == false && view.getGUIState().getShowFirst() == false && view.getGUIState().getShowSecond() == true){
					drawWeightedContacts(g2d,uniqueToSecondContacts,2,uniqueToSecondContactsColor,true);
					drawWeightedContacts(g2d,uniqueToSecondContacts,2,uniqueToSecondContactsColor,false);
				}
				// 2) common=0, first=1, second=0 
				else if (view.getGUIState().getShowCommon() == false && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == false){
					drawWeightedContacts(g2d,uniqueToFirstContacts,1,uniqueToFirstContactsColor,false);
					drawWeightedContacts(g2d,uniqueToFirstContacts,1,uniqueToFirstContactsColor,true);
				}
				// 3) common=0, first=1, second=1
				else if (view.getGUIState().getShowCommon() == false && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == true){
					drawWeightedContacts(g2d,uniqueToFirstContacts,1,uniqueToFirstContactsColor,false);
					drawWeightedContacts(g2d,uniqueToSecondContacts,2,uniqueToSecondContactsColor,false);
					drawWeightedContacts(g2d,uniqueToFirstContacts,1,uniqueToFirstContactsColor,true);
					drawWeightedContacts(g2d,uniqueToSecondContacts,2,uniqueToSecondContactsColor,true);
				}
				// 4) common=1, first=0, second=0
				else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == false && view.getGUIState().getShowSecond() == false){
					drawWeightedContacts(g2d,commonContacts,3,commonContactsColor,false);
					drawWeightedContacts(g2d,commonContacts,3,commonContactsColor,true);
				}
				// 5) common=1, first=0, second=1
				else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == false && view.getGUIState().getShowSecond() == true){
					drawWeightedContacts(g2d,commonContacts,3,commonContactsColor,false);
					drawWeightedContacts(g2d,uniqueToSecondContacts,2,uniqueToSecondContactsColor,false);
					drawWeightedContacts(g2d,commonContacts,3,commonContactsColor,true);
					drawWeightedContacts(g2d,uniqueToSecondContacts,2,uniqueToSecondContactsColor,true);
				}
				// 6) common=1, first=1, second=0
				else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == false){
					drawWeightedContacts(g2d,commonContacts,3,commonContactsColor,false);
					drawWeightedContacts(g2d,uniqueToFirstContacts,1,uniqueToFirstContactsColor,false);
					drawWeightedContacts(g2d,commonContacts,3,commonContactsColor,true);
					drawWeightedContacts(g2d,uniqueToFirstContacts,1,uniqueToFirstContactsColor,true);
				}
				// 7) common=1, first=1, second=1
				else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == true) {
					drawWeightedContacts(g2d,commonContacts,3,commonContactsColor,false);
					drawWeightedContacts(g2d,uniqueToFirstContacts,1,uniqueToFirstContactsColor,false);
					drawWeightedContacts(g2d,uniqueToSecondContacts,2,uniqueToSecondContactsColor,false);
					drawWeightedContacts(g2d,commonContacts,3,commonContactsColor,true);
					drawWeightedContacts(g2d,uniqueToFirstContacts,1,uniqueToFirstContactsColor,true);
					drawWeightedContacts(g2d,uniqueToSecondContacts,2,uniqueToSecondContactsColor,true);
					// 8) common=0, first=0, second=0
				} else { 
					// do nothing
				}
				
			} else { // don't show weighted contacts
				
				// 1) common=0, first=0, second=1
				if (view.getGUIState().getShowCommon() == false && view.getGUIState().getShowFirst() == false && view.getGUIState().getShowSecond() == true){
					drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor,true);
					drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor,false);
				}
				// 2) common=0, first=1, second=0 
				else if (view.getGUIState().getShowCommon() == false && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == false){
					drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor,false);
					drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor,true);
				}
				// 3) common=0, first=1, second=1
				else if (view.getGUIState().getShowCommon() == false && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == true){
					drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor,false);
					drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor,false);
					drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor,true);
					drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor,true);
				}
				// 4) common=1, first=0, second=0
				else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == false && view.getGUIState().getShowSecond() == false){
					drawContacts(g2d,commonContacts,commonContactsColor,false);
					drawContacts(g2d,commonContacts,commonContactsColor,true);
				}
				// 5) common=1, first=0, second=1
				else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == false && view.getGUIState().getShowSecond() == true){
					drawContacts(g2d,commonContacts,commonContactsColor,false);
					drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor,false);
					drawContacts(g2d,commonContacts,commonContactsColor,true);
					drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor,true);
				}
				// 6) common=1, first=1, second=0
				else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == false){
					drawContacts(g2d,commonContacts,commonContactsColor,false);
					drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor,false);
					drawContacts(g2d,commonContacts,commonContactsColor,true);
					drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor,true);
				}
				// 7) common=1, first=1, second=1
				else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == true) {
					drawContacts(g2d,commonContacts,commonContactsColor,false);
					drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor,false);
					drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor,false);
					drawContacts(g2d,commonContacts,commonContactsColor,true);
					drawContacts(g2d,uniqueToFirstContacts,uniqueToFirstContactsColor,true);
					drawContacts(g2d,uniqueToSecondContacts,uniqueToSecondContactsColor,true);
				// 8) common=0, first=0, second=0
				} else { 
					// do nothing
				}
			}
		}
	}
	
	/**
	 * Draws contacts for the given contact set in the given color
	 * @param g2d
	 * @param contactSet
	 * @param color
	 */
	protected void drawContacts(Graphics2D g2d, IntPairSet contactSet, Color color, boolean secondMap) {
		g2d.setColor(color);
		for(Pair<Integer> cont:contactSet){
			drawContact(g2d, cont, secondMap,(view.getGUIState().getShowBackground() && !secondMap) || (view.getGUIState().getShowBottomBackground() && secondMap) );
		}	
	}

	/**
	 * Draws weighted contacts for the given contact set in the given color
	 * @param g2d
	 * @param contactSet
	 * @param color
	 */
	private void drawWeightedContacts(Graphics2D g2d, IntPairSet contactSet, int modNum, Color color, boolean secondMap) {
		for(Pair<Integer> cont:contactSet){
			double weight;
			if(modNum == 1) {
				weight = mod.getGraph().getEdgeFromSerials(mapAl2Seq(mod.getLoadedGraphID(),cont.getFirst()), mapAl2Seq(mod.getLoadedGraphID(),cont.getSecond())).getWeight();
			} else if(modNum == 2) {
				weight = mod2.getGraph().getEdgeFromSerials(mapAl2Seq(mod2.getLoadedGraphID(),cont.getFirst()), mapAl2Seq(mod2.getLoadedGraphID(),cont.getSecond())).getWeight();
			} else { // both
				double weight1 = mod.getGraph().getEdgeFromSerials(mapAl2Seq(mod.getLoadedGraphID(),cont.getFirst()), mapAl2Seq(mod.getLoadedGraphID(),cont.getSecond())).getWeight();
				double weight2 = mod2.getGraph().getEdgeFromSerials(mapAl2Seq(mod2.getLoadedGraphID(),cont.getFirst()), mapAl2Seq(mod2.getLoadedGraphID(),cont.getSecond())).getWeight();
				weight = Math.max(weight1, weight2);
			}
			float truncWeight = (float) Math.max(0, Math.min(1, weight)); // truncated weight (if weights are off the (0,1) interval)
			Color newColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * truncWeight));
			g2d.setColor(newColor);
			drawContact(g2d, cont, secondMap,(view.getGUIState().getShowBackground() && !secondMap) || (view.getGUIState().getShowBottomBackground() && secondMap) );
		}	
	}

	/**
	 * Draws the given contact cont to the given graphics object g2d using the
	 * global contactSquareSize and g2d current painting color.
	 */
	private void drawContact(Graphics2D g2d, Pair<Integer> cont, boolean secondMap) {
		int x,y;
		if (secondMap) {
			x = getCellUpperLeft(cont).y;
			y = getCellUpperLeft(cont).x;
		} else {
			x = getCellUpperLeft(cont).x;
			y = getCellUpperLeft(cont).y;
		}
		g2d.drawRect(x,y,contactSquareSize,contactSquareSize);
		g2d.fillRect(x,y,contactSquareSize,contactSquareSize);

	}

	/**
	 * Draws the given contact cont to the given graphics object g2d using the
	 * global contactSquareSize and g2d current painting color.
	 */
	private void drawContact(Graphics2D g2d, Pair<Integer> cont, boolean secondMap,boolean small) {
		int x,y;
		if (secondMap) {
			x = getCellUpperLeft(cont).y;
			y = getCellUpperLeft(cont).x;
		} else {
			x = getCellUpperLeft(cont).x;
			y = getCellUpperLeft(cont).y;
		}
		if (small) {
			g2d.drawRect(1+x+contactSquareSize/3,1+y+contactSquareSize/3,contactSquareSize/3,contactSquareSize/3);
			g2d.fillRect(1+x+contactSquareSize/3,1+y+contactSquareSize/3,contactSquareSize/3,contactSquareSize/3);
		} else {
			g2d.drawRect(x,y,contactSquareSize,contactSquareSize);
			g2d.fillRect(x,y,contactSquareSize,contactSquareSize);
		}

	}

	
	/**
	 * @param g2d
	 */
	private void drawDensityMap(Graphics2D g2d, boolean secondMap) {
		// assuming that density matrix has values from [0,1]
		int size = densityMatrix.length;
		for(int i = 0; i < size; i++) {
			for(int j = i; j < size; j++) {
				Color c = colorMapRedBlue(densityMatrix[i][j]);
				if(!c.equals(backgroundColor)) {
					g2d.setColor(c);
					Pair<Integer> cont = new Pair<Integer>(i+1,j+1);
					drawContact(g2d, cont,secondMap);
				}
//				double val = (double)Math.round(100*densityMatrix[i][j])/100;
//				System.out.print(val+"\t");
			}
//			System.out.println();
		}
	}

	
	
	/**
	 * @param g2d
	 */
	private void drawDeltaRankMap(Graphics2D g2d, boolean secondMap) {
		// assuming that delta Rank matrix has values from [-38,38], -100 indicates no data 
		Color c;
		int size = deltaRankMatrix.length;
		for(int i = 0; i < size; i++) {
			for(int j = i; j < size; j++) {
				if (deltaRankMatrix[i][j] < -80) {
					c = Color.LIGHT_GRAY;
				} else {
					c = colorMapScaledHeatmap((((double)deltaRankMatrix[i][j])/76)+0.5,0.5);
				}
				
				if(!c.equals(backgroundColor)) {
					g2d.setColor(c);
					Pair<Integer> cont = new Pair<Integer>(i+1,j+1);
					drawContact(g2d, cont, secondMap);
				}
			}
		}
	}
	

	/**
	 * @param g2d
	 */
	private void drawResidueScoringFunctionMap(Graphics2D g2d, boolean bottom) {
		// assuming that delta Rank matrix has values from [-38,38], -100 indicates no data 
		Color c;
		double v;
		int size = this.contactMapSize;
		for(int i = 0; i < size; i++) {
			for(int j = i; j < size; j++) {
				String fn = view.getGUIState().getResidueScoringFunctionName(bottom);
				ResidueContactScoringFunction f = statusBar.getScoringFunctionWithName(fn);
				v = f.getScore(i, j);
				if (v == -1) {
					c= backgroundColor;
				} else {
					c = colorMapScaledHeatmap(v,0.5);
				}
				if(!c.equals(backgroundColor)) {
					g2d.setColor(c);
					Pair<Integer> cont = new Pair<Integer>(i+1,j+1);
					drawContact(g2d, cont, bottom);
				}
			}
		}
	}
	
	
	private void drawDiffDistMap(Graphics2D g2d, boolean secondMap) {
		// this actually contains all cells in matrix so is doing a
		// full loop on all cells
		for (Pair<Integer> cont:diffDistMap.keySet()){
			Color c = colorMapHeatmap(1-diffDistMap.get(cont));
			g2d.setColor(c);
			drawContact(g2d, cont, secondMap);
		}
	}
	
	/**
	 * @param g2d
	 */
	private void drawTFFctMap(Graphics2D g2d, boolean secondMap) {
		if (this.view.tfDialog.isDisplayable()){
			TransferFunctionBar tfBar = this.view.tfDialog.getTransfFctBar();
			String[] inputValTypes = tfBar.getInputValTypes();
			// assuming that density matrix has values from [0,1]
			int size = densityMatrix.length;
			HashMap<Pair<Integer>,Double> distMatrix = mod.getDistMatrix();
			
			// get min and max of delta rank matrix
			double minDR = 0, maxDR = 0;
			if (this.view.isDatabaseConnectionAvailable()){ // tfBar.useDeltaRank()){
				minDR=0; maxDR=0;
				for(int i = 0; i < size; i++) {
					for(int j = i; j < size; j++) {
						if (deltaRankMatrix[i][j] < minDR)
							minDR = deltaRankMatrix[i][j];
						if (deltaRankMatrix[i][j] > maxDR)
							maxDR = deltaRankMatrix[i][j];
					}
				}
			}
			
			for(int i = 0; i < size; i++) {
				for(int j = i; j < size; j++) {
					Pair<Integer> cont = new Pair<Integer>(i+1,j+1);
					// collect all available values and hand them over to TFDialog
					double[] inputVal = new double[inputValTypes.length];
					for(int type=0; type<inputVal.length; type++){
						if (inputValTypes[type] == View.BgOverlayType.COMMON_NBH.label){
							if (this.comNbhSizes.containsKey(cont)){
								int sizeNbh = comNbhSizes.get(cont);
								if (allContacts.contains(cont)) 
									inputVal[type] = 1.0/Math.sqrt((double)sizeNbh);
								else 
									inputVal[type] = 1.0/(double)sizeNbh;
							}
							else
								inputVal[type] = 0;
						}
						if (inputValTypes[type] == View.BgOverlayType.DENSITY.label){
							inputVal[type] = Math.abs(densityMatrix[i][j]);
						}
						if (inputValTypes[type] == View.BgOverlayType.DISTANCE.label){
							// available to use: scaledDistCutoff
							if (distMatrix.containsKey(cont))
								inputVal[type] = distMatrix.get(cont);
							else
								inputVal[type] = 0;
						}
						if (inputValTypes[type] == View.BgOverlayType.DELTA_RANK.label && this.view.isDatabaseConnectionAvailable()){ //tfBar.useDeltaRank()){
							double val = deltaRankMatrix[i][j];
							double dVal = Math.abs(maxDR-minDR); 
							val = (val-minDR)/dVal;
//							inputVal[type] = (((double)deltaRankMatrix[i][j])/76)+0.5;
							inputVal[type] = val;
							if (inputVal[type]<0 || inputVal[type]>1)
								System.out.println("Invalid value: "+inputVal[type]+" for "+inputValTypes[type]);
							if (inputVal[type]<0)
								inputVal[type] = 0;
							if (inputVal[type]>1)
								inputVal[type] = 1;
						}
						if (inputVal[type]<0 || inputVal[type]>1)
							System.out.println("Invalid value: "+inputVal[type]+" for "+inputValTypes[type]);
					}
					// get respective colour
					Color c = tfBar.getColor(inputVal); //colorMapRedBlue(densityMatrix[i][j]);
					if(!c.equals(backgroundColor)) {
						g2d.setColor(c);
						drawContact(g2d, cont,secondMap);
					}
				}
			}			
		}
		
	}

	/**
	 * NOT USED ANYMORE. NOW, COORDINATES ARE SHOWN IN STATUS BAR.
	 * Draws coordinates for all registered models.
	 */
	protected void drawCoordinates(){
		if( this.hasSecondModel() ) {
			drawCoordinates(mod,mod2,allContacts,allSecondContacts,false);
		} else {
			drawCoordinates(mod,null,allContacts,null,false);
		}
	}

	/**
	 * NOT USED ANYMORE. NOW, COORDINATES ARE SHOWN IN STATUS BAR.
	 * Draws coordinates when mouse is over residue ruler.
	 */
	private void drawRulerCoord() {
		if( this.hasSecondModel() ) {
			drawCoordinates(mod,mod2,allContacts,allSecondContacts,true);
		} else {
			drawCoordinates(mod,null,allContacts,null,true);
		}
	}		


	/**
	 * Passes coordinates for the given model to the status bar on the right. Please 
	 * note, that whenever an ordinate of the current mouse position in the 
	 * graphic equals zero the coordinates will not be printed. 
	 * @param mod  the model
	 * @param mod2 the second model or null if not in compare mode
	 * @param modContacts  all contacts of mod
	 * @param modContacts  all contacts of mod2 or null if not in compare mode 
	 * @param rulerMode if in ruler mode, show only coordinate(s) of either i or j
	 */
	protected void drawCoordinates(Model mod, Model mod2, IntPairSet modContacts, IntPairSet mod2Contacts, boolean rulerMode) {
		Pair<Integer> currentCell = screen2cm(mousePos);

		// alignment indices
		int iAliIdx = 0;
		int jAliIdx = 0;
		
		// initialize position to be shown
		if(rulerMode) {
			if (currentRulerMouseLocation==ResidueRuler.TOP || currentRulerMouseLocation==ResidueRuler.BOTTOM) {
				jAliIdx = currentRulerCoord;
				iAliIdx = 1;					// don't care
			} else {
				jAliIdx = 1;					// don't care
				iAliIdx = currentRulerCoord;
			}						
		} else {
			iAliIdx = currentCell.getFirst();
			jAliIdx = currentCell.getSecond();			
		}
		
		// return if mouse position is out of bounds
		//if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > mod.getMatrixSize() || jAliIdx > mod.getMatrixSize() ) {
		if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > ali.getAlignmentLength() || jAliIdx > ali.getAlignmentLength() ) {
			// TODO: reset all fields in StatusBar?
			return;
		}
		
		statusBar.getCoordinatesPanel().setIAli(iAliIdx + "");
		statusBar.getCoordinatesPanel().setJAli(jAliIdx + "");
		if(hasSecondModel() && view.getGUIState().getShowAlignmentCoords()) {
			statusBar.getCoordinatesPanel().setShowAliAndSeqPos(true);
		} else {
			statusBar.getCoordinatesPanel().setShowAliAndSeqPos(false);			
		}
		
		// first contact map
		
		String aliTag = mod.getLoadedGraphID();
		
		// converting to sequence indices
		int iSeqIdx = mapAl2Seq(aliTag,iAliIdx);
		int jSeqIdx = mapAl2Seq(aliTag,jAliIdx);
			
		// coordinates, residue types and optionally alignment coordinates		
		statusBar.getCoordinatesPanel().setINum(iSeqIdx<0?"-":iSeqIdx+"");
		statusBar.getCoordinatesPanel().setJNum(jSeqIdx<0?"-":jSeqIdx+"");

		String i_res = String.valueOf(MultipleSequenceAlignment.GAPCHARACTER);
		if (iSeqIdx>0) { // to skip gaps
			i_res = mod.getResType(iSeqIdx);;
		}
		String j_res = String.valueOf(MultipleSequenceAlignment.GAPCHARACTER);
		if (jSeqIdx>0) { // to skip gaps
			j_res = mod.getResType(jSeqIdx);
		}
		statusBar.getCoordinatesPanel().setIRes(i_res);
		statusBar.getCoordinatesPanel().setJRes(j_res);
			
		// writing secondary structure
		if (mod.hasSecondaryStructure()){
			SecStrucElement iSSElem = mod.getSecondaryStructure().getSecStrucElement(iSeqIdx);
			SecStrucElement jSSElem = mod.getSecondaryStructure().getSecStrucElement(jSeqIdx);
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
			statusBar.getCoordinatesPanel().setISSType(Character.toString(iSSType));
			statusBar.getCoordinatesPanel().setJSSType(Character.toString(jSSType));
			statusBar.getCoordinatesPanel().setHasSecondaryStructure(true);
		} else {
			statusBar.getCoordinatesPanel().setHasSecondaryStructure(false);
		}
		
		// write pdb residue numbers (if enabled & available)
		if (view.getGUIState().getShowPdbSers() && mod.has3DCoordinates()){
			String i_pdbresser = mod.getPdbResSerial(iSeqIdx);
			String j_pdbresser = mod.getPdbResSerial(jSeqIdx);
			statusBar.getCoordinatesPanel().setIPdbNum(i_pdbresser==null?"?":i_pdbresser);
			statusBar.getCoordinatesPanel().setJPdbNum(j_pdbresser==null?"?":j_pdbresser);
			statusBar.getCoordinatesPanel().setWritePDBResNum(true);		
		} else {
			statusBar.getCoordinatesPanel().setWritePDBResNum(false);
		}
		
		// draw hyphen if (i,j) is a contact
		if(!rulerMode) {
			statusBar.getCoordinatesPanel().setIsContact(modContacts.contains(currentCell));
		}
		
		// second contact map
		
		if(hasSecondModel()) {
			statusBar.getCoordinatesPanel().setCompareMode(true);
			statusBar.getCoordinatesPanel().setTitle(mod.getLoadedGraphID()+":");
			statusBar.getCoordinatesPanel().setTitle2(mod2.getLoadedGraphID()+":");
			
			String aliTag2 = mod2.getLoadedGraphID();
			
			// converting to sequence indices
			int iSeqIdx2 = mapAl2Seq(aliTag2,iAliIdx);
			int jSeqIdx2 = mapAl2Seq(aliTag2,jAliIdx);
				
			// coordinates, residue types and optionally alignment coordinates		
			statusBar.getCoordinatesPanel().setINum2(iSeqIdx2<0?"-":iSeqIdx2+"");
			statusBar.getCoordinatesPanel().setJNum2(jSeqIdx2<0?"-":jSeqIdx2+"");
			
			String i_res2 = String.valueOf(MultipleSequenceAlignment.GAPCHARACTER);
			if (iSeqIdx2>0 && iSeqIdx2 < mod2.getMatrixSize()) { // to skip gaps
				i_res2 = mod2.getResType(iSeqIdx2);
			}
			String j_res2 = String.valueOf(MultipleSequenceAlignment.GAPCHARACTER);
			if (jSeqIdx2>0 && iSeqIdx2 < mod2.getMatrixSize()) { // to skip gaps
				j_res2 = mod2.getResType(jSeqIdx2);
			}
			statusBar.getCoordinatesPanel().setIRes2(i_res2);
			statusBar.getCoordinatesPanel().setJRes2(j_res2);
							
			// writing secondary structure
			if (mod2.hasSecondaryStructure()){
				SecStrucElement iSSElem2 = mod2.getSecondaryStructure().getSecStrucElement(iSeqIdx2);
				SecStrucElement jSSElem2 = mod2.getSecondaryStructure().getSecStrucElement(jSeqIdx2);
				Character iSSType2 = iSSElem2==null?' ':iSSElem2.getType();
				Character jSSType2 = jSSElem2==null?' ':jSSElem2.getType();
				switch(iSSType2) {
				case 'H': iSSType2 = '\u03b1'; break;	// alpha
				case 'S': iSSType2 = '\u03b2'; break;	// beta
				default: iSSType2 = ' ';
				}
				switch(jSSType2) {
				case 'H': jSSType2 = '\u03b1'; break;
				case 'S': jSSType2 = '\u03b2'; break;
				default: jSSType2 = ' ';
				}
				statusBar.getCoordinatesPanel().setISSType2(Character.toString(iSSType2));
				statusBar.getCoordinatesPanel().setJSSType2(Character.toString(jSSType2));
				statusBar.getCoordinatesPanel().setHasSecondaryStructure(true);
			} else {
				statusBar.getCoordinatesPanel().setHasSecondaryStructure(false);
			}
			
			// write pdb residue numbers (if enabled & available)
			if (view.getGUIState().getShowPdbSers() && mod2.has3DCoordinates()){
				String i_pdbresser2 = mod2.getPdbResSerial(iSeqIdx2);
				String j_pdbresser2 = mod2.getPdbResSerial(jSeqIdx2);
				statusBar.getCoordinatesPanel().setIPdbNum(i_pdbresser2==null?"?":i_pdbresser2);
				statusBar.getCoordinatesPanel().setJPdbNum(j_pdbresser2==null?"?":j_pdbresser2);
				statusBar.getCoordinatesPanel().setWritePDBResNum(true);		
			} else {
				statusBar.getCoordinatesPanel().setWritePDBResNum(false);
			}
			
			// draw hyphen if (i,j) is a contact
			if(!rulerMode) {
				statusBar.getCoordinatesPanel().setIsContact2(mod2Contacts.contains(currentCell));
			}
		} else {
			// write sequence separation in diagonal selection mode
			if(!rulerMode && view.getGUIState().getSelectionMode()==GUIState.SelMode.DIAG) {
				statusBar.getCoordinatesPanel().setSeqSep(getRange(currentCell)+"");
				statusBar.getCoordinatesPanel().setIsDiagSecMode(true);				
			} else {
				statusBar.getCoordinatesPanel().setIsDiagSecMode(false);
			}
		}
		
		if(rulerMode) {
			if (currentRulerMouseLocation==ResidueRuler.TOP || currentRulerMouseLocation==ResidueRuler.BOTTOM) {
				statusBar.getCoordinatesPanel().setINum("");		// switch off display of other coordinate
				statusBar.getCoordinatesPanel().setINum2("");		// switch off display of other coordinate				
			} else {
				statusBar.getCoordinatesPanel().setJNum("");
				statusBar.getCoordinatesPanel().setJNum2("");
			}						
		}
		
		statusBar.repaint();
	}
	
	public int getISeqIdx(Pair<Integer> currentCell, boolean secondModel){
		
		// alignment indices
		int iAliIdx = 0;
		int jAliIdx = 0;
		
		// initialize position to be shown
		iAliIdx = currentCell.getFirst();
		jAliIdx = currentCell.getSecond();		
				
		// return if mouse position is out of bounds
		//if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > mod.getMatrixSize() || jAliIdx > mod.getMatrixSize() ) {
		if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > ali.getAlignmentLength() || jAliIdx > ali.getAlignmentLength() ) {
			// TODO: reset all fields in StatusBar?
			return -1;
		}
		if (!secondModel){
			// first contact map
			String aliTag = mod.getLoadedGraphID();
			// converting to sequence indices
			int iSeqIdx = mapAl2Seq(aliTag,iAliIdx);
//			int jSeqIdx = mapAl2Seq(aliTag,jAliIdx);
			
			return iSeqIdx;
		}
		else {
			if (this.mod2!=null){
				String aliTag2 = mod2.getLoadedGraphID();		
				// converting to sequence indices
				int iSeqIdx2 = mapAl2Seq(aliTag2,iAliIdx);
//				int jSeqIdx2 = mapAl2Seq(aliTag2,jAliIdx);
				
				return iSeqIdx2;
			}
			else
				return -1; //iSeqIdx;			
		}
		
	}
	
	public int getJSeqIdx(Pair<Integer> currentCell, boolean secondModel){
		
		// alignment indices
		int iAliIdx = 0;
		int jAliIdx = 0;
		
		// initialize position to be shown
		iAliIdx = currentCell.getFirst();
		jAliIdx = currentCell.getSecond();		
				
		// return if mouse position is out of bounds
		//if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > mod.getMatrixSize() || jAliIdx > mod.getMatrixSize() ) {
		if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > ali.getAlignmentLength() || jAliIdx > ali.getAlignmentLength() ) {
			// TODO: reset all fields in StatusBar?
			return -1;
		}
		if (!secondModel){
			// first contact map
			String aliTag = mod.getLoadedGraphID();
			// converting to sequence indices
//			int iSeqIdx = mapAl2Seq(aliTag,iAliIdx);
			int jSeqIdx = mapAl2Seq(aliTag,jAliIdx);
			
			return jSeqIdx;
		}
		else {
			if (this.mod2!=null){
				String aliTag2 = mod2.getLoadedGraphID();		
				// converting to sequence indices
//				int iSeqIdx2 = mapAl2Seq(aliTag2,iAliIdx);
				int jSeqIdx2 = mapAl2Seq(aliTag2,jAliIdx);
				
				return jSeqIdx2;
			}
			else
				return -1; //iSeqIdx;			
		}
		
	}
	
	public int getISeqIdx(boolean secondModel){
		Pair<Integer> currentCell = screen2cm(mousePressedPos);

		// alignment indices
		int iAliIdx = 0;
		int jAliIdx = 0;
		
		// initialize position to be shown
		iAliIdx = currentCell.getFirst();
		jAliIdx = currentCell.getSecond();		
				
		// return if mouse position is out of bounds
		//if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > mod.getMatrixSize() || jAliIdx > mod.getMatrixSize() ) {
		if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > ali.getAlignmentLength() || jAliIdx > ali.getAlignmentLength() ) {
			// TODO: reset all fields in StatusBar?
			return -1;
		}
		if (!secondModel){
			// first contact map
			String aliTag = mod.getLoadedGraphID();
			// converting to sequence indices
			int iSeqIdx = mapAl2Seq(aliTag,iAliIdx);
//			int jSeqIdx = mapAl2Seq(aliTag,jAliIdx);
			
			return iSeqIdx;
		}
		else {
			if (this.mod2!=null){
				String aliTag2 = mod2.getLoadedGraphID();		
				// converting to sequence indices
				int iSeqIdx2 = mapAl2Seq(aliTag2,iAliIdx);
//				int jSeqIdx2 = mapAl2Seq(aliTag2,jAliIdx);
				
				return iSeqIdx2;
			}
			else
				return -1; //iSeqIdx;			
		}
		
	}
	
	public int getJSeqIdx(boolean secondModel){
		Pair<Integer> currentCell = screen2cm(mousePressedPos);

		// alignment indices
		int iAliIdx = 0;
		int jAliIdx = 0;
		
		// initialize position to be shown
		iAliIdx = currentCell.getFirst();
		jAliIdx = currentCell.getSecond();		
				
		// return if mouse position is out of bounds
		//if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > mod.getMatrixSize() || jAliIdx > mod.getMatrixSize() ) {
		if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > ali.getAlignmentLength() || jAliIdx > ali.getAlignmentLength() ) {
			// TODO: reset all fields in StatusBar?
			return -1;
		}
		if (!secondModel){
			// first contact map
			String aliTag = mod.getLoadedGraphID();
			// converting to sequence indices
//			int iSeqIdx = mapAl2Seq(aliTag,iAliIdx);
			int jSeqIdx = mapAl2Seq(aliTag,jAliIdx);
			
			return jSeqIdx;
		}
		else {
			if (this.mod2!=null){
				String aliTag2 = mod2.getLoadedGraphID();		
				// converting to sequence indices
//				int iSeqIdx2 = mapAl2Seq(aliTag2,iAliIdx);
				int jSeqIdx2 = mapAl2Seq(aliTag2,jAliIdx);
				
				return jSeqIdx2;
			}
			else
				return -1; //iSeqIdx;			
		}
		
	}
	
	public int getISeqIdxRC(boolean secondModel){
		Pair<Integer> currentCell = this.rightClickCont;

		// alignment indices
		int iAliIdx = 0;
		int jAliIdx = 0;
		
		// initialize position to be shown
		iAliIdx = currentCell.getFirst();
		jAliIdx = currentCell.getSecond();		
				
		// return if mouse position is out of bounds
		//if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > mod.getMatrixSize() || jAliIdx > mod.getMatrixSize() ) {
		if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > ali.getAlignmentLength() || jAliIdx > ali.getAlignmentLength() ) {
			// TODO: reset all fields in StatusBar?
			return -1;
		}
		if (!secondModel){
			// first contact map
			String aliTag = mod.getLoadedGraphID();
			// converting to sequence indices
			int iSeqIdx = mapAl2Seq(aliTag,iAliIdx);
//			int jSeqIdx = mapAl2Seq(aliTag,jAliIdx);
			
			return iSeqIdx;
		}
		else {
			if (this.mod2!=null){
				String aliTag2 = mod2.getLoadedGraphID();		
				// converting to sequence indices
				int iSeqIdx2 = mapAl2Seq(aliTag2,iAliIdx);
//				int jSeqIdx2 = mapAl2Seq(aliTag2,jAliIdx);
				
				return iSeqIdx2;
			}
			else
				return -1; //iSeqIdx;			
		}
		
	}
	
	public int getJSeqIdxRC(boolean secondModel){
		Pair<Integer> currentCell = this.rightClickCont;

		// alignment indices
		int iAliIdx = 0;
		int jAliIdx = 0;
		
		// initialize position to be shown
		iAliIdx = currentCell.getFirst();
		jAliIdx = currentCell.getSecond();		
				
		// return if mouse position is out of bounds
		//if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > mod.getMatrixSize() || jAliIdx > mod.getMatrixSize() ) {
		if(iAliIdx == 0 || jAliIdx == 0 || iAliIdx > ali.getAlignmentLength() || jAliIdx > ali.getAlignmentLength() ) {
			// TODO: reset all fields in StatusBar?
			return -1;
		}
		if (!secondModel){
			// first contact map
			String aliTag = mod.getLoadedGraphID();
			// converting to sequence indices
//			int iSeqIdx = mapAl2Seq(aliTag,iAliIdx);
			int jSeqIdx = mapAl2Seq(aliTag,jAliIdx);
			
			return jSeqIdx;
		}
		else {
			if (this.mod2!=null){
				String aliTag2 = mod2.getLoadedGraphID();		
				// converting to sequence indices
//				int iSeqIdx2 = mapAl2Seq(aliTag2,iAliIdx);
				int jSeqIdx2 = mapAl2Seq(aliTag2,jAliIdx);
				
				return jSeqIdx2;
			}
			else
				return -1; //iSeqIdx;			
		}
		
	}

	protected void drawCrosshair(Graphics2D g2d){
		// only in case of range selection we draw a diagonal cursor
		if (view.getGUIState().getSelectionMode()==GUIState.SelMode.DIAG){
			g2d.setColor(diagCrosshairColor);			
			g2d.drawLine(mousePos.x-mousePos.y, 0, getOutputSize(), getOutputSize()-(mousePos.x-mousePos.y));
			//g2d.drawLine(0, getOutputSize()-mousePos.y,mousePos.x,getOutputSize());
			
			// all other cases cursor is a cross-hair
		} else {
			// drawing the cross-hair
			g2d.setColor(crosshairColor);
			g2d.drawLine(mousePos.x, 0, mousePos.x, outputSize);
			g2d.drawLine(0, mousePos.y, outputSize, mousePos.y);
			int bcenterx = mousePos.y;
			int bcentery = mousePos.x;
			
			g2d.drawLine(bcenterx-30,bcentery, bcenterx+30,bcentery);
			g2d.drawLine(bcenterx,bcentery-30, bcenterx,bcentery+30);
			g2d.drawArc(bcenterx-30, bcentery-30, 60, 60,0,360);
			
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
		hitcounter++;
		if (evt.isPopupTrigger()) {
			showPopup(evt);
			dragging = false;
			return;
		}
		// only if release after left click (BUTTON1)
		if (evt.getButton()==MouseEvent.BUTTON1) {
			
			if (view.contView!=null && view.contView.isDisplayable()){ // view.contView==null if closed?
				Pair<Integer> c = screen2cm(mousePressedPos); 
				//System.out.println("CMPane MouseReleased first:"+c.getFirst()+"  second:"+c.getSecond());
				view.contView.cPane.commitSettings();
				view.contView.cPane.calcSphoxelParam(c);
				view.contView.cPane.calcTracesParam();
				try {
					view.contView.cPane.recalcSphoxel();
					view.contView.cPane.recalcTraces(true);
					view.contView.cPane.recalcOptNBHStrings();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				view.contView.cPane.updateSettings();
			}

			switch (view.getGUIState().getSelectionMode()) {
			case COMNBH:
				Pair<Integer> c = screen2cm(mousePressedPos); 
				this.currCommonNbh = mod.getCommonNbhood (mapAl2Seq(mod.getLoadedGraphID(),c.getFirst()),mapAl2Seq(mod.getLoadedGraphID(),c.getSecond()));
				
				dragging = false;
				showCommonNbs = true;
				this.repaint();
				break;

			case COLOR:
				dragging=false;		// TODO: can we pull this up?
				Pair<Integer> clickedPos = screen2cm(mousePressedPos); // TODO: this as well?
				if(!isControlDown(evt)) {
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
				break;	

			case RECT:


				if(!dragging) {					
					Pair<Integer> clicked = screen2cm(mousePressedPos);
					if(getCurrentContactSet().contains(clicked)) {  // if clicked position
																	// is a contact
						if(selContacts.contains(clicked)) {
							// if clicked position is a selected contact,
							// deselect it
							if(isControlDown(evt)) {
								selContacts.remove(clicked);
							} else {
								selContacts = new IntPairSet();
								selContacts.add(clicked);
							}
						} else {
							// if clicked position is a contact but not
							// selected, select it
							if(!isControlDown(evt)) {
								selContacts = new IntPairSet();
							}
							selContacts.add(clicked);
						}
					} else {
						// else: if clicked position is outside of a contact and
						// ctrl not pressed, reset selContacts
						if(!isControlDown(evt)) resetSelections();
					}
					this.repaint();
				} else { // dragging
					if (isControlDown(evt)){
						selContacts.addAll(tmpContacts);
					} else{
						selContacts = new IntPairSet();
						selContacts.addAll(tmpContacts);
					}
				}
				dragging = false;
				break;


			case FILL:
				dragging = false;
				// resets selContacts when clicking mouse
				if (!isControlDown(evt)){
					resetSelections();
				}

				fillSelect(getCurrentContactSet(), screen2cm(new Point(evt.getX(),evt.getY())));

				this.repaint();
				break;

			case NBH:

				// Test output
				Pair<Integer> cNB = screen2cm(mousePressedPos);
				int iNum = cNB.getFirst();
				RIGNode nodeI = this.mod.getNodeFromSerial(iNum); //this.mod.getGraph().getNodeFromSerial(this.iNum);
				System.out.println("ResidueNr:"+iNum+" "+nodeI.getResidueType()+" NB:"+this.mod.getGraph().getNbhood(nodeI).getNbString());
				int jNum = cNB.getSecond();
				RIGNode nodeJ = this.mod.getNodeFromSerial(jNum); //this.mod.getGraph().getNodeFromSerial(this.iNum);
				System.out.println("ResidueNr:"+jNum+" "+nodeJ.getResidueType()+" NB:"+this.mod.getGraph().getNbhood(nodeJ).getNbString());
				
				dragging = false;
				// resets selContacts when clicking mouse
				if (!isControlDown(evt)){
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
				break;

			case DIAG:
				if (!dragging){
					Pair<Integer> clicked = screen2cm(mousePressedPos);
					// new behaviour: select current diagonal
					if(!isControlDown(evt)) {
						resetSelections();
					}
					// in undirected case only selectDiagonal if we are above diagonal
					if (!mod.isDirected() && clicked.getFirst()<clicked.getSecond()) { 
						selectDiagonal(getRange(clicked));
					}

					this.repaint();
				} else { // dragging
					if (isControlDown(evt)){
						selContacts.addAll(tmpContacts);
					} else{
						selContacts = new IntPairSet();
						selContacts.addAll(tmpContacts);
					}
				}
				dragging = false;			
				break;
			
			case TOGGLE:

				Pair<Integer> clicked = screen2cm(mousePressedPos);
				view.handleToggleContact(clicked);
				break;

			}
			
			selContactsChanged(); // at least potentially
		}
	}

	public void mouseDragged(MouseEvent evt) {
		// Called whenever the user moves the mouse
		// while a mouse button is held down.

		if(lastMouseButtonPressed == MouseEvent.BUTTON1) {
			dragging = true;
			mouseDraggingPos = evt.getPoint();
			switch (view.getGUIState().getSelectionMode()) {
			case RECT:
				squareSelect(getCurrentContactSet());
				break;
			case DIAG:
				rangeSelect(getCurrentContactSet());
				break;
			}	
		}
		mouseMoved(evt); // TODO is this necessary? I tried getting rid of it
		// but wasn't quite working
	} 

	public void mouseEntered(MouseEvent evt) { 
		mouseIn = true;
		mouseCell = screen2cm(mousePos);
		lastMouseCell = mouseCell;
	}

	public void mouseExited(MouseEvent evt) {
		mouseIn = false;
		this.repaint();
		view.loupePanel.clear();
		if(Start.SHOW_CONTACTS_IN_REALTIME && Start.isPyMolConnectionAvailable()) {
			if(!this.hasSecondModel()) {
				Start.getPyMolAdaptor().clearCurrentContact();
			} else {
				Start.getPyMolAdaptor().clearCurrentContacts();
			}
		}
	}

	public void mouseClicked(MouseEvent evt) {
	}

	public void mouseMoved(MouseEvent evt) {
		mousePos = evt.getPoint();
		mouseCell = screen2cm(mousePos);
		this.repaint();
		
		// update 'real time contact' in PyMol
		if(Start.SHOW_CONTACTS_IN_REALTIME && Start.isPyMolConnectionAvailable()) {
				if(mouseCell != null && lastMouseCell != null
				&& mouseCell.getFirst() > 0 && mouseCell.getSecond() > 0 
				&& (mouseCell.getFirst() != lastMouseCell.getFirst() || mouseCell.getSecond() != lastMouseCell.getSecond())) {
					if(this.hasSecondModel()) {
					
						if (dragging && (view.getGUIState().getSelectionMode()==GUIState.SelMode.RECT || view.getGUIState().getSelectionMode()==GUIState.SelMode.DIAG)) {
							Start.getPyMolAdaptor().showCurrentSelections(mod, mod2, mapContactSetToSequence(mod.getLoadedGraphID(),tmpContacts), mapContactSetToSequence(mod2.getLoadedGraphID(),tmpContacts));
						} else {
							Start.getPyMolAdaptor().showCurrentContacts(mod, mod2, mapContactAl2Seq(mod.getLoadedGraphID(), mouseCell), mapContactAl2Seq(mod2.getLoadedGraphID(), mouseCell));
						}
					
					} else {

						
						// we are doing this here instead because it is triggered less often than tmpContactsChanged:
						if (dragging && (view.getGUIState().getSelectionMode()==GUIState.SelMode.RECT || view.getGUIState().getSelectionMode()==GUIState.SelMode.DIAG)) {
							Start.getPyMolAdaptor().showCurrentSelection(mod, tmpContacts);
						} else {
							Start.getPyMolAdaptor().showCurrentContact(mod, mouseCell);							
						}
					}
					lastMouseCell = mouseCell; 
				}
			
		}
	}

	/*---------------------------- custom event -----------------------------*/
	// some event-handler like methods but have to be called manually
	
	
	/**
	 * this should be called whenever we change the variable tmpContacts
	 */
	private void tmpContactsChanged() {
		// nothing to do, update of real time contacts happens in mouseMoved (for performance)
	}
	
	/**
	 * this should be called whenever we change the variable selContacts
	 */
	private void selContactsChanged() {
		if(Start.SHOW_CONTACTS_IN_REALTIME && Start.isPyMolConnectionAvailable()) {
			if(!this.hasSecondModel()) {
				Start.getPyMolAdaptor().showCurrentSelection(mod, selContacts);
			} else {
				Start.getPyMolAdaptor().showCurrentSelections(mod, mod2, mapContactSetToSequence(mod.getLoadedGraphID(),selContacts), mapContactSetToSequence(mod2.getLoadedGraphID(),selContacts));
			}
		}
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
		this.view.topRul.repaint();
		this.view.leftRul.repaint();
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
		if (view.getGUIState().getShowDistanceMap()){
			drawDistanceMap(g2d, false);
		}

		// density map
		if(view.getGUIState().getShowDensityMap()) {
			drawDensityMap(g2d, false);
		}
		
		// Delta Rank Map
		if(view.getGUIState().getShowDeltaRankMap()) {
			drawDeltaRankMap(g2d, false);
		}
		
		if(view.getGUIState().getShowResidueScoringMap()) {
			drawResidueScoringFunctionMap(g2d,false);
		}
		
		// common nbh sizes or contact map
		if (view.getGUIState().getShowNbhSizeMap()){
			drawNbhSizeMap(g2d, false);
		}

		// draw difference distance map (in comparison mode)
		if(view.getGUIState().getCompareMode() && view.getGUIState().getShowDiffDistMap()) {
			drawDiffDistMap(g2d, false);
		}
		
		// distance map
		if (view.getGUIState().getShowBottomDistanceMap()){
			drawDistanceMap(g2d, true);
		}

		// density map
		if(view.getGUIState().getShowBottomDensityMap()) {
			drawDensityMap(g2d, true);
		}
		
		// Delta Rank Map
		if(view.getGUIState().getShowBottomDeltaRankMap()) {
			drawDeltaRankMap(g2d, true);
		}
		
		if(view.getGUIState().getShowBottomResidueScoringMap()) {
			drawResidueScoringFunctionMap(g2d,true);
		}
		
		// common nbh sizes or contact map
		if (view.getGUIState().getShowBottomNbhSizeMap()){
			drawNbhSizeMap(g2d, true);
		}

		// draw difference distance map (in comparison mode)
		if(view.getGUIState().getCompareMode() && view.getGUIState().getShowDiffDistMap()) {
			drawDiffDistMap(g2d, false);
		}
		
		// draw difference distance map (in comparison mode)
		if(view.getGUIState().getCompareMode() && view.getGUIState().getShowBottomDiffDistMap()) {
			drawDiffDistMap(g2d, true);
		}
		
		// draw transferFunction based map
		if(view.getGUIState().getShowBottomTFFctMap() && view.tfDialog.isDisplayable()){
			drawTFFctMap(g2d, true);
		}
		
		// draw contact map if necessary (single or comparison)
		//if(!view.getGUIState().getShowNbhSizeMap() && !view.getGUIState().getShowDistanceMap()) {
			drawContactMap(g2d);			
		//}
		if (view.getGUIState().getShowBottomDeltaRankMap() || view.getGUIState().getShowDeltaRankMap()) {
			statusBar.setDeltaRank(ContactMapPane.Round((float)mod.getDeltaRankScore(),2));
			statusBar.showDeltaRankGroup(true);
			deltaRankBar.setSequence(mod.getSequence());
			deltaRankBar.setVectors(mod.getDeltaRankVectors());
			deltaRankBar.setProbabilities(mod.getDeltaRankProbabilities());
			deltaRankBar.repaint();
		}
		statusBar.updateScoringFunctions();
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
	 * Update the Delta Rank Map
	 */
	
	public synchronized void updateDeltaRankMap() {
		deltaRankMatrix = mod.getDeltaRankMatrix();
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
			// resetting all other sets
			commonContacts = new IntPairSet();
			uniqueToFirstContacts = new IntPairSet();
			uniqueToSecondContacts = new IntPairSet();
			bothStrucContacts = new IntPairSet();
			allButCommonContacts = new IntPairSet();

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
			bothStrucContacts.addAll(uniqueToFirstContacts);
			bothStrucContacts.addAll(uniqueToSecondContacts);
			bothStrucContacts.addAll(commonContacts);
			// allButCommon = uniqueToFirst+uniqueToSecond
			allButCommonContacts.addAll(uniqueToFirstContacts);
			allButCommonContacts.addAll(uniqueToSecondContacts);
		}
		
		// updating maps
		if(view.getGUIState().getShowNbhSizeMap() || view.getGUIState().getShowBottomNbhSizeMap()) {
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
		if(view.getGUIState().getShowDensityMap() || view.getGUIState().getShowBottomDensityMap()) {
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
		if(view.getGUIState().getShowDeltaRankMap() || view.getGUIState().getShowBottomDeltaRankMap()) {
				getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
				updateDeltaRankMap();
				doResetCursor = true;
			
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
		
		selContactsChanged();
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
		
		selContactsChanged();
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
		
		selContactsChanged();
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
		
		selContactsChanged();
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
		
		selContactsChanged();
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
		
		selContactsChanged();
		
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

	public void toggleDeltaRankMap(boolean state) {
		if(state) {
					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
					updateDeltaRankMap();
					updateScreenBuffer();		// will repaint
					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
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
	
	/**
	 * Show/hide transferFunction-based map
	 */	
	protected void toggleTFFctMap(boolean state) {
		if (state) {
			getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
			TransferFunctionBar tfBar = this.view.tfDialog.getTransfFctBar();
			String[] inputValTypes = tfBar.getInputValTypes();			
			if (inputValTypes[inputValTypes.length-1] == View.BgOverlayType.DELTA_RANK.label)
				updateDeltaRankMap();
			if (mod.getDistMatrix()==null || densityMatrix==null || comNbhSizes==null) {
				if(BACKGROUND_LOADING) {
					if (mod.getDistMatrix()==null)
						updateDistanceMapBg();		// will update screen buffer
					if (densityMatrix==null)
						updateDensityMapBg();		// will update screen buffer
					if (comNbhSizes==null)
						updateNbhSizeMapBg();
				} else {
//					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
					if (mod.getDistMatrix()==null)
						updateDistanceMap();		
					if (densityMatrix==null)
						updateDensityMap();		
					if (comNbhSizes==null)
						updateNbhSizeMap();
					updateScreenBuffer();		// will repaint
//					getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
				}
			} else {
				updateScreenBuffer();
			}
			getTopLevelAncestor().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
		} else {
			updateScreenBuffer();			// will repaint
		}

		if(state) {
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
			if (view.getGUIState().getShowCommon() == false && view.getGUIState().getShowFirst() == false && view.getGUIState().getShowSecond() == true){
				return uniqueToSecondContacts;
			}
			// 2) common=0, first=1, second=0
			else if (view.getGUIState().getShowCommon() == false && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == false){
				return uniqueToFirstContacts;
			}
			// 3) common=0, first=1, second=1
			else if (view.getGUIState().getShowCommon() == false && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == true){
				return allButCommonContacts;
			}
			// 4) common=1, first=0, second=0
			else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == false && view.getGUIState().getShowSecond() == false){
				return commonContacts;
			}
			// 5) common=1, first=0, second=1
			else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == false && view.getGUIState().getShowSecond() == true){
				return allSecondContacts;
			}
			// 6) common=1, first=1, second=0
			else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == false){
				return allContacts;
			}
			// 7) common=1, first=1, second=1
			else if (view.getGUIState().getShowCommon() == true && view.getGUIState().getShowFirst() == true && view.getGUIState().getShowSecond() == true){
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
		selContactsChanged();
	}
	
	public double getRatio() {
		return ratio;
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
		// square representing a contact
	}

	/*---------------------------- public information methods ---------------------------*/
	/*
	 * these methods are called by others to retrieve the state of the current
	 * component
	 */
	
	/**
	 * @param Model mod and int residue = residue number, whose first shell neighbours are to be found out
	 * @return A list of all first shell neighbours. When used iteratively on all the first shell nbrs, this same
	 * method would give all the second shell neighbours; and so on for higher generation neighbours.
	 */
	public IntPairSet getfirstShellNbrs(Model mod, int residue){
		IntPairSet firstShellNbrs = new IntPairSet();
		
		RIGNbhood nbh = mod.getNbhood(residue);
		for(RIGNode j:nbh.getNeighbors()){
			firstShellNbrs.add(new Pair<Integer> (Math.min(j.getResidueSerial(), residue),Math.max(j.getResidueSerial(), residue)));
		}
		
		return firstShellNbrs;
	}
	
	
	/**
	 * @param Model mod, int residue = residue number, whose first shell relationships are to be investigated
	 * @return The pairs of first shell nbrs, which are nbrs of each other. In short, returns 'first-shell-relationships', and NOT 
	 * a list of all neighbours
	 */
	public IntPairSet getfirstShellNbrRels(Model mod, int residue){
		IntPairSet firstShellNbrRels = new IntPairSet();
				
		RIGNbhood nbh = mod.getNbhood(residue);
		for(RIGNode j:nbh.getNeighbors()){
				INNER1:for(RIGNode k:nbh.getNeighbors()){
					if(k.equals(j)){
						continue INNER1;
					}
					if(this.allContacts.contains((new Pair<Integer>(Math.min(j.getResidueSerial(), k.getResidueSerial()),Math.max(j.getResidueSerial(), k.getResidueSerial()))))){
						firstShellNbrRels.add(new Pair<Integer> (Math.min(j.getResidueSerial(), k.getResidueSerial()),Math.max(j.getResidueSerial(), k.getResidueSerial())));
					}		
				}
			}
		
		return firstShellNbrRels;
	} 
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
	
	/**
	 * @return Left clicked pair of residues in the contact map pane.
	 */
	public Pair<Integer> getmousePos() {
		Pair<Integer> currmousePos = screen2cm(this.mousePos);
				
		return currmousePos;
	}
	
	/**
	 * @return Right clicked pair of residues in the contact map pane.
	 */
	public Pair<Integer> getmousePosRightClick() {
//		Pair<Integer> currmousePos = screen2cm(this.mousePos);
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
		if(view.popupSendEdge != null) {
			view.popupSendEdge.setText(String.format(View.LABEL_SHOW_PAIR_DIST_3D,mapAl2Seq(mod.getLoadedGraphID(),rightClickCont.getFirst()),mapAl2Seq(mod.getLoadedGraphID(),rightClickCont.getSecond())));
		}if(view.sphereP != null) {
			view.sphereP.setText(String.format(View.LABEL_SHOW_SPHERES_POPUP_3D,mapAl2Seq(mod.getLoadedGraphID(),rightClickCont.getFirst()),mapAl2Seq(mod.getLoadedGraphID(),rightClickCont.getSecond())));
		}
		view.popup.show(e.getComponent(), e.getX(), e.getY());
	}

	/**
	 * Returns the corresponding contact in the contact map given screen
	 * coordinates
	 */
	private Pair<Integer> screen2cm(Point point){
	
		if (point.y > point.x) { // check if we're in the bottom left CM
			return new Pair<Integer>((int) Math.ceil(point.x/ratio),(int) Math.ceil(point.y/ratio));
		}
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
		if (mod.isDirected()) {
			return Math.abs(cont.getFirst()-cont.getSecond());
		} else {
			return (cont.getSecond()-cont.getFirst());
		}
	}
	
//	/** Returns the size in pixels of a single contact on screen
//	* TODO: Check whether this number is really the number in pixels (and not
//	plus or minus 1) */
//	private int getContactSquareSize() {
//	return contactSquareSize;
//	}
	
	/**
	 * Checks whether ctrl (or meta on mac) was pressed when the given mouse event was created.
	 * This method specifically takes care of Mac Look&Feels where the Apple-key
	 * is supposed to be used for multiple selections and menu shortcuts. In this
	 * case the functions will - contrary to its name - actually check whether
	 * the Apple-key was down.
	 */
	protected boolean isControlDown(MouseEvent evt) {
		int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();		
		if(mask==Event.META_MASK) return evt.isMetaDown();	// we are on a Mac! Yipiie!
		else return evt.isControlDown();
	}

	public void setStatusBar(StatusBar statusBar) {
		this.statusBar = statusBar;
		
	}
	
	public void setDeltaRankBar(DeltaRankBar bar) {
		deltaRankBar = bar;
		deltaRankBar.setCMPane(this);
	}
	
	/**
	 *  Rounds to x significant digits 
	 * @param Rval
	 * @param Rpl
	 * @return
	 */
	
	public static float Round(float Rval, int Rpl) {
		  float p = (float)Math.pow(10,Rpl);
		  Rval = Rval * p;
		  float tmp = Math.round(Rval);
		  return (float)tmp/p;
	}




} 

