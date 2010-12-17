package cmview.gmbp;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.vecmath.Vector3d;

//import owl.core.structure.AminoAcid;
import owl.core.structure.features.SecStrucElement;
import owl.core.structure.graphs.RIGGeometry;
import owl.core.structure.graphs.RIGNbhood;
import owl.core.structure.graphs.RIGNode;

import owl.gmbp.CMPdb_nbhString_traces;
import owl.gmbp.CMPdb_sphoxel;
import owl.gmbp.CSVhandler;
import owl.gmbp.NbhString_ClusterAnalysis;
import owl.gmbp.OptimalSingleEnv;

import cmview.ContactMapPane;
import cmview.ScreenBuffer;
import cmview.Start;
import cmview.datasources.Model;
import edu.uci.ics.jung.graph.util.Pair;

public class ContactPane extends JPanel implements MouseListener, MouseMotionListener, ComponentListener{ //KeyListener
	/**
	 * -f /Users/vehlow/Documents/workspace/7ODC.pdb -c A
	 */
	private static final long serialVersionUID = 1L;
	
	protected static Dimension defaultDim = new Dimension(1200, 880);
	protected static final float g2dRatio = 0.5f; // H/W
	protected static final double defaultMinAllowedRat = -3;
	protected static final double defaultMaxAllowedRat = 1;
	protected final int cylindricalMapProj = 0;
	protected final int kavrayskiyMapProj = 1;
	protected final int azimuthalMapProj = 2;
	protected final int defaultProjType = kavrayskiyMapProj;
	protected static final int sphoxelHist = 0;
	protected static final int tracesHist = 0;
	protected static final int defaultMaxNrTraces = 100;

	protected static final Color helixSSTColor = Color.cyan; // color for helix residues
	protected static final Color sheetSSTColor = Color.magenta;	// color for helix residues
	protected static final Color otherSSTColor = Color.yellow;	// color for helix residues
	protected static final Color anySSTColor = Color.blue;		// color for helix residues
	
	protected static final Color mod1Color = Color.magenta; //.pink;
	protected static final Color mod2Color = Color.green;
	
	protected static final BasicStroke defaultBasicStroke = new BasicStroke(1);
		
	/*--------------------------- member variables --------------------------*/		
	// underlying data
	private Model mod, mod2, mod3;
	private ContactMapPane cmPane;
	private ContactView contactView;
	
	private ContactStatusBar contStatBar;	
	
	// used for drawing
	private Dimension screenSize;			// current size of this component (contactView) on screen
	private Dimension g2dSize;         		// size of the effective Rectangle
											// available for drawing the sphoxel image
//	private double screenRatio;  			// ratio of screen size and contact
//											// map size = size of each contact
											// on screen
//	private int sphoxelSize;
	
	private Point mousePressedPos;   		// position where mouse where last
											// pressed, used for start of square
											// selection and for common
											// Neighbors
	private Point mouseDraggingPos;  		// current position of mouse
											// dragging, used for end point of
											// square selection
	private Point mousePos;             	// current position of mouse (being
											// updated by mouseMoved)
	private int lastMouseButtonPressed;		// mouse button which was pressed
											// last (being updated by
											// MousePressed)
//	private int currentRulerCoord;	 		// the residue number shown if
//											// showRulerSer=true
//	private int currentRulerMousePos;		// the current position of the mouse
//											// in the ruler
	private boolean dragging;     			// set to true while the user is
											// dragging (to display selection
											// rectangle)
	protected boolean mouseIn;				// true if the mouse is currently in
											// the contact map window (otherwise
											// supress crosshair)
//	private boolean showRulerCoord; 		// while true, current ruler
//											// coordinate are shown instead of
//											// usual coordinates
//	private boolean showRulerCrosshair;		// while true, ruler "crosshair" is
//											// being shown
	
	// buffers for triple buffering
	private ScreenBuffer screenBuffer;		// buffer containing the more or
											// less static background image
	
	// drawing colors (being set in the constructor)
	private Color backgroundColor;	  		// background color
	private Color squareSelColor;	  		// color of selection rectangle
	private Color crosshairColor;     		// color of crosshair	
	private Color selAngleRangeColor;       // color for selected rectangles
	private Color actSelAngleRangeColor;	// color for selected rectangle of actual chosen contact i_j
	private Color longitudeColor;			// color for longitudes
	private Color latitudeColor;			// color for latitudes
	private Color arrowColor;
	private BasicStroke selRangeStroke = new BasicStroke(2);
	private BasicStroke longLatStroke = new BasicStroke(2);
	private BasicStroke crosshairStroke = new BasicStroke(2);
	
	// selections 
	private Vector<Pair<Double>> lambdaRanges;			// permanent list of currently selected lambda ranges
	private Vector<Pair<Double>> phiRanges;       // permanent list of currently selected phi ranges
	private Vector<Pair<Integer>> selContacts;         // permanent list of currently selected and referred contacts
	private Pair<Double> tmpLambdaRange;
	private Pair<Double> tmpPhiRange;
	private Pair<Integer> tmpSelContact;
	private Pair<Double> tmpAngleComb;
	private Pair<Double> origCoordinates;    // actual coordinates of origin (equator, zero meridian) in angle range x:[0:2*PI] y:[0:PI]
	private Pair<Double> centerOfProjection; // longitude and latitude of actual centre of projection (depends on origCoordinates)
	private Pair<Double> rightClickAngle;
	
	private RIGNode nodeI, nodeJ;
	
	// query data
	private char iRes='A', jRes='A';
	private char iSSType='H', jSSType='H', nbhSSType='H';
	private boolean diffSStype=false, diffSStypeNBH=false;
	//	private String iResType="Ala", jResType="Ala";
	private int iNum=0, iNum2=0, jNum=0, jNum2=0;
	private String nbhString="", nbhStringL="%";
	private int[] nbSerials, nbSerials2, nbSerials3;
	private String origNBHString; 
//	private String jAtom = "CA";
	private char[] nbhsRes;
	private int maxNumTraces;
	private String atomType = "CA"; // 4traces: plot only atompositions of type atomtype

//	private String dbSphoxel = "bagler_all13p0_alledges";
//	private String dbTraces = "bagler_all5p0_alledges";

	// Sphoxel-Data
	private CMPdb_sphoxel sphoxel;
	private double [][] ratios;
	private double [][][] bayesRatios;
	private double minRatio = 0;
	private double maxRatio = 0;
	private float minr=2.0f, maxr=12.8f;
	private float[] radiusThresholds = new float[] {2.0f, 5.6f, 9.2f, 12.8f};
	private boolean radiusRangesFixed = true;
	private String radiusPrefix = "rSR";
	
	private int [] histWholeAngleRange; // = new int[scale.length];
	private Vector<int[]> hist4SelAngleRange; // = new Vector<int[]>();
	private int [][] histTWholeAngleRange; // = new int[scale.length];
	private Vector<int[][]> histT4SelAngleRange; // = new Vector<int[]>();
	private Vector<double[]> minMaxAverT4SelAngleRange;
	private double[] nodeDistrWithinSel;
	private int[] nodeDistrAroundSel;
	private double[][] nodeDistr4Sel;
	private int[] foundNodes;
	private boolean showHist = true;
	private int histType = sphoxelHist;	
	private int chosenSelection;
	
	// NBHStraces-Data
	private CMPdb_nbhString_traces nbhsTraces;
	private Vector<float[]> nbhsNodes;
	private int[] numNodesPerLine;
	private int[] maxDistsJRes, minDistsJRes;
	private int numLines;
	private OptimalSingleEnv optNBHString;
	private Vector<String[]> optNBHStrings;
	private String[] setOfOptStrings;
	
	// Clustering Data
	private NbhString_ClusterAnalysis clusterAnal;
	private int[] clusterIDs; // contains clusterID for each node of this.nbhsNodes
	private double[][] clusterProp; // [][0]:minL, [][1]:averageL, [][2]:maxL, [][3]:minP, [][4]:averP, [][5]:maxP
									// lambda[-Pi:+Pi] and phi[0:Pi]
	private double[][] clusterDirProp; // [][0]:minIncSlope, [][1]:averageIncSlope, [][2]:maxIncSlope, [][3]:minOutSlope, [][4]:averOutSlope, [][5]:maxOutSlope
	private Vector<Integer>[] clusters; //clusters contains a vector of all IDs for noise (background) and each found cluster
	private int[] nbCluster;    // holds ID of neighbouring cluster (at the other end of edge); if any neighbouring cluster exists, ID=0
	private Vector<double[]> clusterAverDirec;  // holds direction (vector saved as array) for each cluster
	private Vector<Integer>[] mostFrequentResT4Clusters;

	// ---- variables for representation (drawing methods)
	private int numSteps = CMPdb_sphoxel.defaultNumSteps; //CMPdb_sphoxel.defaultNumSteps;  // change later via interface
	private float resol = CMPdb_sphoxel.defaultResol; //CMPdb_sphoxel.getDefaultResol();
	private final int border = 0; //15;
	private final int yBorderThres = 0; //45;
	private float pixelWidth = 5*36/this.numSteps; // change in dependence of numSteps
	private float pixelHeight = this.pixelWidth; //5*36/this.numSteps;	
	private float voxelsize = (float) (this.numSteps*this.pixelWidth/Math.PI); //pixelWidth;
	private double voxelsizeFactor = 1.5;
	private double deltaRad = Math.PI/this.numSteps;
	private final double dL = 0.5;
	private double rSphere;
	private double maxDistPoints;

	private boolean removeOutliers = false; //true;
	private double minAllowedRat = defaultMinAllowedRat;
	private double maxAllowedRat = defaultMaxAllowedRat;
	private int chosenColourScale = ContactStatusBar.BLUERED;
	
	private boolean showResInfo = false;
	private boolean showNBHStemplateTrace = true;
	
	private int epsilon = NbhString_ClusterAnalysis.defaultEpsilon;
	private int minNumNBs = NbhString_ClusterAnalysis.defaultMinNumNBs;
	
	private boolean paintCentralResidue = true;
//	private boolean mapProjection = false;
	private int mapProjType = defaultProjType;
	private double deltaOffSetX, deltaOffSetXEnd, deltaOffSetXCenter; // = this.getScreenPosFromRad(0, 0).getFirst();
	
	private int xDim=0;
	private int yDim=0;
	
	public final char[] aas = new char[]{'A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y'}; // possible Residues
//	private final String AAStr = new String(aas); 
	public final char[] sstypes = new char[]{'H','S','O','A'};
	private final String SSTStr = new String(sstypes);
	
	private Vector<String[]> settings;
	
	/*----------------------------- constructors ----------------------------*/

	/**
	 * Create a new ContactPane.
	 * 
	 * @param mod
	 * @param cmPane
	 * @param view
	 */
	public ContactPane(Model mod, ContactMapPane cmPane, ContactView contactView){
		this.mod = mod;
		this.mod2 = null;
		this.mod3 = null;
		this.cmPane = cmPane;
		this.contactView = contactView;	
		
		if (!this.contactView.isShowTracesFeature())
			defaultDim = new Dimension(1200, 600);
		
		initContactPane();
	}
	
	/**
	 * Create a new ContactPane.
	 * 
	 * @param mod
	 * @param cmPane
	 * @param view
	 */
	public ContactPane(Model mod, Model mod2, ContactMapPane cmPane, ContactView contactView){
		this.mod = mod;
		this.mod2 = mod2;
		this.mod3 = null;
		this.cmPane = cmPane;
		this.contactView = contactView;	
		
		if (!this.contactView.isShowTracesFeature())
			defaultDim = new Dimension(1200, 600);
		
		initContactPane();
	}
	
	private void initContactPane(){
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
//		this.setFocusable(true);
//		addKeyListener(this);

		this.setOpaque(true); // make this component opaque
		this.setBorder(BorderFactory.createLineBorder(Color.black));
		
		// initialize data
		this.nbhsNodes = new Vector<float[]>();
		this.optNBHStrings = new Vector<String[]>();
		this.ratios = new double[0][0];
			
		this.xDim = defaultDim.width;
		this.yDim = defaultDim.height;
		// update pixel dimensions
		this.pixelWidth = (float)(this.xDim-2*this.border)/(float)(2*this.numSteps) ;
		this.pixelHeight =  this.pixelWidth;
		this.voxelsize = (float) ((float) (this.numSteps*this.pixelWidth)/Math.PI);
	
		this.g2dSize = defaultDim; // new Dimension(this.xDim,this.yDim);
		this.setSize(this.g2dSize);
		this.screenSize = this.contactView.getPreferredSize();
//		System.out.println("ContactPane screensize HxW: "+this.screenSize.height+"x"+this.screenSize.width);
//		this.screenSize = this.contactView.getScreenSize();
//		System.out.println("ContactPane getscreensize HxW: "+this.screenSize.height+"x"+this.screenSize.width);
//		this.contactView.setPreferredSize(this.screenSize);
//		this.contactView.setPreferredSize(new Dimension(this.screenSize.width+AngleRuler.STD_RULER_WIDTH, this.screenSize.height+AngleRuler.STD_RULER_WIDTH));

		this.rSphere = g2dSize.getHeight()/2;
		this.maxDistPoints = distPointsOnSphere(Math.PI, Math.PI/2, Math.PI, 0);
		
		this.mousePos = new Point();
		this.mousePressedPos = new Point();
		this.mouseDraggingPos = new Point();
		
		// set default colors
		this.backgroundColor = Color.white;
		this.squareSelColor = Color.gray;	
		this.crosshairColor = Color.green;
		this.selAngleRangeColor = new Color(100, 100, 100); //Color.black;
		this.actSelAngleRangeColor = Color.black;
		this.longitudeColor = Color.black;
		this.latitudeColor = this.longitudeColor;
		this.arrowColor = new Color(127,255,127,150);
		
		this.dragging = false;
		this.selContacts = new Vector<Pair<Integer>>();
		this.lambdaRanges = new Vector<Pair<Double>>();
		this.phiRanges = new Vector<Pair<Double>>();
		this.settings = new Vector<String[]>();
		
		this.tmpLambdaRange = new Pair<Double>(0.0, 0.0);
		this.tmpPhiRange = new Pair<Double>(0.0, 0.0);
		this.tmpAngleComb = new Pair<Double>(0.0, 0.0);
		this.origCoordinates = new Pair<Double>(Math.PI, Math.PI/2);
//		this.centerOfProjection = new Pair<Double>(Math.PI-this.origCoordinates.getFirst(), Math.PI/2-this.origCoordinates.getSecond());
//		this.centerOfProjection = new Pair<Double>(this.origCoordinates.getFirst()-Math.PI, this.origCoordinates.getSecond()-Math.PI/2);
		this.centerOfProjection = this.origCoordinates;
		this.deltaOffSetX = this.getOffSet(0.0, 0.0); //this.getScreenPosFromRad(0, 0).getFirst();
		this.deltaOffSetXEnd = this.getOffSet(2*Math.PI, 0.0); //this.getScreenPosFromRad(2*Math.PI, 0.0).getFirst();
		this.deltaOffSetXCenter = this.getOffSet(Math.PI, 0.0);
		
		// initialise parameters
		this.minAllowedRat = defaultMinAllowedRat;
		this.maxAllowedRat = defaultMaxAllowedRat;
		this.removeOutliers = false;
		this.maxNumTraces = defaultMaxNrTraces;
		this.epsilon = NbhString_ClusterAnalysis.defaultEpsilon;
		this.minNumNBs = NbhString_ClusterAnalysis.defaultMinNumNBs;		
		
		try {
			calcParam();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
//		setOutputSize(screenSize.width, screenSize.height);
//		System.out.println("outputsize= "+this.outputSize);
		
	}
	/**
	 Save so far chosen settings locally.
	 */	
	public void commitSettings(){
		if (this.selContacts.size()>0){
			String[] actSettings = {String.valueOf(this.iNum), String.valueOf(this.jNum), 
					this.nbhString, String.valueOf(this.contStatBar.getChosenStringID()), String.valueOf(this.maxNumTraces), 
					String.valueOf(this.epsilon), String.valueOf(minNumNBs), 
					String.valueOf(this.centerOfProjection.getFirst()), String.valueOf(this.centerOfProjection.getSecond()), 
					String.valueOf(this.removeOutliers), String.valueOf(minAllowedRat), String.valueOf(maxAllowedRat),
					String.valueOf(this.tmpLambdaRange.getFirst()), String.valueOf(this.tmpLambdaRange.getSecond()),
					String.valueOf(this.tmpPhiRange.getFirst()), String.valueOf(this.tmpPhiRange.getSecond())};
			int index = contactElemOfSettings(this.iNum, this.jNum);
			if (index>-1){
				// replace settings
				this.settings.removeElementAt(index);
			}
			// add settings to vector
			this.settings.add(actSettings);
		}
	}
	
	/**
	 Load settings if contact has been chosen before.
	 */	
	public void updateSettings(){
		if (this.selContacts.size()>0){
			int index = contactElemOfSettings(this.iNum, this.jNum);
			int stringID = 0;
			if (index>-1){
				// load settings for contact i_j
				String[] set = this.settings.elementAt(index);
				this.nbhString = set[2];
//				this.nbhStringL = set[3];
				this.nbhStringL = "%";
				for (int i=0; i<this.nbhString.length(); i++){
					this.nbhStringL += this.nbhString.charAt(i);
					this.nbhStringL += "%";
				}
				stringID = Integer.valueOf(set[3]);
				this.maxNumTraces = Integer.valueOf(set[4]);
				this.epsilon = Integer.valueOf(set[5]);
				this.minNumNBs = Integer.valueOf(set[6]);
				this.centerOfProjection = new Pair<Double>( Double.valueOf(set[7]), Double.valueOf(set[8]));
				this.removeOutliers = Boolean.valueOf(set[9]);
				this.minAllowedRat = Double.valueOf(set[10]);
				this.maxAllowedRat = Double.valueOf(set[11]);
//				this.tmpLambdaRange = new Pair<Double>(Double.valueOf(set[12]), Double.valueOf(set[13]));
//				this.tmpPhiRange = new Pair<Double>(Double.valueOf(set[14]), Double.valueOf(set[15]));
//				updateSelections();
				// -- update contact status bar (menu)
				updateContactStatusBar(stringID);
			}
			else {
				setDefaultSettings();
			}	
		}
	}
	
	/**
	 Set default settings (parameters).
	 */	
	private void setDefaultSettings(){
		// set default settings
		int stringID = 0;
		this.centerOfProjection = this.origCoordinates;
		this.removeOutliers = false;
		this.minAllowedRat = defaultMinAllowedRat;
		this.maxAllowedRat = defaultMaxAllowedRat;
		this.nbhString = this.origNBHString;
		this.maxNumTraces = defaultMaxNrTraces;
		this.epsilon = NbhString_ClusterAnalysis.defaultEpsilon;
		this.minNumNBs = NbhString_ClusterAnalysis.defaultMinNumNBs;
		this.tmpLambdaRange = new Pair<Double>(0.0, 0.0);
		this.tmpPhiRange = new Pair<Double>(0.0, 0.0);
		// update contact status bar (menu)
		updateContactStatusBar(stringID);
	}
	
	/**
	 Handover all parameters to menu.
	 @param stringID of actual chosen nbhString
	 */	
	private void updateContactStatusBar(int stringID){
		// update contact status bar (menu)
		if (contStatBar!= null){
			this.contStatBar.getNBHSPanel().setActNbhString(this.nbhString);
			this.contStatBar.setChosenStringID(stringID);
			this.contStatBar.setMaxNumTraces(maxNumTraces);
			this.contStatBar.setEpsilon(this.epsilon);
			this.contStatBar.setMinNumNBs(this.minNumNBs);
			this.contStatBar.setRemoveOutliers(this.removeOutliers);
			this.contStatBar.setMinAllowedRatio(this.minAllowedRat);
			this.contStatBar.setMaxAllowedRatio(this.maxAllowedRat);
		}			
	}
	
	/**
	 Saves actual chosen range for lambda and phi for actual chosen contact.
	 */	
	private void updateSelections(){
		updateSelections(this.iNum, this.jNum);
	}
	
	/**
	 Saves actual chosen range for lambda and phi for a certain contact.
	 @param iNum of central residue
	 @param jNum of contacting residue
	 */	
	private void updateSelections(int iNum, int jNum){
		int index = checkForSelectedRanges(iNum, jNum);
		if (index>-1){
			this.lambdaRanges.removeElementAt(index);
			this.phiRanges.removeElementAt(index);
			this.selContacts.removeElementAt(index);												
		}
		this.lambdaRanges.add(this.tmpLambdaRange);
		this.phiRanges.add(this.tmpPhiRange);
		this.selContacts.add(this.tmpSelContact);
	}

	/**
	 Hands over all defined angle ranges Gmbp class. 
	 */	
	private void updateAngleRange(){
//		RIGEdge edge = this.mod.getGraph().getEdgeFromSerials(this.tmpSelContact.getFirst(), this.tmpSelContact.getSecond());		
//		edge.setPhiPsi(this.tmpLambdaRange.getFirst(), this.tmpLambdaRange.getSecond(), this.tmpPhiRange.getFirst(), this.tmpPhiRange.getSecond());		
		this.mod.getGmbp().setLambdaRanges(this.lambdaRanges);
		this.mod.getGmbp().setPhiRanges(this.phiRanges);
		this.mod.getGmbp().setSelContacts(this.selContacts);
	}
	
	// ________ Update and compute all necessary parameters based on selected contact in CMap ___________
	
	public void updateQueryParam(int type){
		switch (type){
		case 0:
			calcSphoxelParam(); break;
		case 1:
			calcTracesParam(); break;
		}
	}
	
	private void calcParam() throws SQLException{
		calcSphoxelParam();
		this.sphoxel = new CMPdb_sphoxel(this.iRes, this.jRes);
//		this.sphoxel = new CMPdb_sphoxel(this.iRes, this.jRes, this.dbSphoxel);
//		this.sphoxel.setDBaccess(Start.DB_USER, Start.DB_PWD, Start.DB_HOST, this.dbSphoxel)
//		setSphoxelParam(); // performed within calcSphoxel		
		calcSphoxel();

		calcTracesParam();	
		if (this.contactView.isShowTracesFeature()){
//			this.nbhsTraces = new CMPdb_nbhString_traces(this.nbhStringL, this.jAtom, this.db);
//			this.nbhsTraces = new CMPdb_nbhString_traces(this.nbhStringL, this.atomType, this.dbTraces);
			this.nbhsTraces = new CMPdb_nbhString_traces(this.nbhStringL, this.atomType);
			this.nbhsTraces.setDBaccess(Start.DB_USER, Start.DB_PWD, Start.DB_HOST, Start.DB_NAME);
			setTracesParam();
			calcNbhsTraces();	
			if (this.nbhString!=null){
				this.optNBHString = new OptimalSingleEnv(this.nbhString, this.iRes);
//				this.optNBHString = new OptimalSingleEnv(this.nbhString, this.iRes, Start.DB_HOST, Start.DB_USER, Start.DB_PWD, Start.DB_NAME);
				this.optNBHString.setDBaccess(Start.DB_USER, Start.DB_PWD, Start.DB_HOST, Start.DB_NAME);
				calcOptNbhStrings();
			}		
		}
	}
	
	public void calcTracesParam(){
				
		this.nbSerials = new int[0];
		this.nbSerials2 = new int[0];
		this.nbSerials3 = new int[0];
		if(nodeI!=null){
			RIGNbhood nbhood = this.mod.getGraph().getNbhood(nodeI);
			System.out.println("Edge type: "+this.mod.edgeType);
//			this.jAtom = this.mod.edgeType.toUpperCase();
			// Manual change jAtom --> bagler_cb8p0 or always CA
			this.atomType = this.mod.getGraphGeometry().getAtomType(); //"CA";
			
			this.nbhString = nbhood.getNbString();
			this.nbSerials = new int[nbhood.getSize()];
			int cnt=0;
			for (RIGNode node:nbhood.getNeighbors()){
				int resSer = node.getResidueSerial();
				this.nbSerials[cnt] = resSer;
				cnt++;
				System.out.print(resSer+"_"+node.getResidueType()+"\t");
//				System.out.print(resSer+"_"+this.mod.getNodeFromSerial(resSer).getResidueType()+"\t");
			}
			System.out.println();
			
			this.nbhStringL = "%";
			int count = 0;
//			int indexOfX = 0;
			this.nbhsRes = new char[this.nbhString.length()];
			for (int i=0; i<this.nbhString.length(); i++){
				this.nbhStringL += this.nbhString.charAt(i);
				this.nbhStringL += "%";
				this.nbhsRes[count] = this.nbhString.charAt(i);
//				if (this.nbhString.charAt(i) == 'x')
//					indexOfX = count;
				count++;
			}
			System.out.println(this.nbhString+"-->"+this.nbhStringL);	
			this.origNBHString = this.nbhString;
			
			if(this.mod2 != null){
				cnt=0;
				RIGNode nodeI2 = this.mod2.getNodeFromSerial(iNum2);
				if (nodeI2!=null){
					RIGNbhood nbhood2 = this.mod2.getGraph().getNbhood(nodeI2);
					this.nbSerials2 = new int[nbhood2.getSize()];
					for (RIGNode node:nbhood2.getNeighbors()){
						int resSer = node.getResidueSerial();
						this.nbSerials2[cnt] = resSer;
						cnt++;
						System.out.print(resSer+"_"+node.getResidueType()+"\t");
					}
					System.out.println();				
				}
				else{
					System.out.println("Node for residue number "+iNum2+" doesn't exist within model2");	
				}
			}
			
			if(this.mod3 != null){
				cnt=0;
				RIGNode nodeI3 = this.mod3.getNodeFromSerial(this.iNum);
				if (nodeI3!=null){
					RIGNbhood nbhood3 = this.mod3.getGraph().getNbhood(nodeI3);
					this.nbSerials3 = new int[nbhood3.getSize()];
					for (RIGNode node:nbhood3.getNeighbors()){
						int resSer = node.getResidueSerial();
						this.nbSerials3[cnt] = resSer;
						cnt++;
						System.out.print(resSer+"_"+node.getResidueType()+"\t");
					}
					System.out.println();				
				}
				else
					System.out.println("Mod3: SeqIndex can not be resolved based on selected contact!");
			}			
		}
		else{
			System.out.println("Node for residue number "+iNum+" doesn't exist within model1");	
		}
	}
	
	public void calcSphoxelParam(){
		// Get first shell neighbours of involved residues
//		Pair<Integer> currmousePos = screen2cm(this.mousePos);
//		Pair<Integer> currentResPair = this.cmPane.getmousePos();
		Pair<Integer> currentResPair = this.cmPane.getmousePosRightClick();
		currentResPair = this.cmPane.getRightClickCont();
		calcSphoxelParam(currentResPair);
	}
		
	public void calcSphoxelParam(Pair<Integer> currentResPair){
//		if (currentResPair.equals(this.cmPane.getRightClickCont())){
//			this.iNum = this.cmPane.getISeqIdxRC(false);
//			this.jNum = this.cmPane.getJSeqIdxRC(false);
//			this.iNum2 = this.cmPane.getISeqIdxRC(true);
//			this.jNum2 = this.cmPane.getJSeqIdxRC(true);			
//		}
//		else { // left click contact
//			this.iNum = this.cmPane.getISeqIdx(false);
//			this.jNum = this.cmPane.getJSeqIdx(false);
//			this.iNum2 = this.cmPane.getISeqIdx(true);
//			this.jNum2 = this.cmPane.getJSeqIdx(true);			
//		}		
		this.iNum = this.cmPane.getISeqIdx(currentResPair, false);
		this.jNum = this.cmPane.getJSeqIdx(currentResPair, false);
		this.iNum2 = this.cmPane.getISeqIdx(currentResPair, true);
		this.jNum2 = this.cmPane.getJSeqIdx(currentResPair, true);			
//		this.iNum = currentResPair.getFirst();
//		this.jNum = currentResPair.getSecond();
		
		// use pair to get iRes and jRes, isstype, nbhstring
		this.nodeI = this.mod.getNodeFromSerial(this.iNum); //this.mod.getGraph().getNodeFromSerial(this.iNum);
		this.nodeJ = this.mod.getNodeFromSerial(this.jNum);	
		if (this.nodeI!=null && this.nodeJ!=null){
			this.iRes = this.nodeI.toString().charAt(this.nodeI.toString().length()-1);
			this.jRes = nodeJ.toString().charAt(nodeJ.toString().length()-1);
			
//			this.iResType = this.nodeI.getResidueType();
//			this.jResType = this.nodeJ.getResidueType();
//			System.out.println("iresType: "+this.iResType+"  jresType: "+this.jResType);

			// Definition of sstype
			SecStrucElement iSSelem = this.nodeI.getSecStrucElement();
//			type = this.nodeI.getSecStrucElement().getType(); 
			if (iSSelem == null){
				System.out.println("No SSelement!");
				this.diffSStype = false;
				this.iSSType = CMPdb_sphoxel.AnySStype;
			}
			else{
				if (iSSelem.isHelix())
					this.iSSType = SecStrucElement.HELIX;
				else if (iSSelem.isOther())
					this.iSSType = SecStrucElement.OTHER;
				else if (iSSelem.isStrand())
					this.iSSType = SecStrucElement.STRAND;
				else if (iSSelem.isTurn())
					this.iSSType = SecStrucElement.TURN;
				
				this.diffSStype = true;
			}
//			System.out.println("i secStrucElement: "+this.iSSType);
//			SecStrucElement jSSelem = nodeJ.getSecStrucElement();
//			if (jSSelem == null){
//				System.out.println("No JSSelement!");
//				this.jSSType = this.sphoxel.AnySStype;
//			}
//			else{
//				if (jSSelem.isHelix())
//					this.jSSType = SecStrucElement.HELIX;
//				else if (jSSelem.isOther())
//					this.jSSType = SecStrucElement.OTHER;
//				else if (jSSelem.isStrand())
//					this.jSSType = SecStrucElement.STRAND;
//				else if (jSSelem.isTurn())
//					this.jSSType = SecStrucElement.TURN;
//			}
			// Standard jSStype=any --> no differentiation made
			this.jSSType = CMPdb_sphoxel.AnySStype;
//			System.out.println("j secStrucElement: "+this.jSSType);	
			
//			this.iSSType = SecStrucElement.HELIX;
//			this.iRes = 'L';
//			this.jRes = 'A';
			
			System.out.println("Selected contact changed to: "+this.iNum+this.iRes+"_"+this.iSSType+"  "+this.jNum+this.jRes);			
		}
		else
			System.out.println("Selected contact didn't change, because node for residue number "+iNum+" doesn't exist within model1!");		
	}
	
	private void setSphoxelParam(){
		this.sphoxel.setDiffSSType(this.diffSStype); // set to true if you want to differentiate ssType
		this.sphoxel.setISSType(this.iSSType);
		this.sphoxel.setJSSType(this.jSSType);
		this.sphoxel.setIRes(this.iRes);
		this.sphoxel.setJRes(this.jRes);
		this.sphoxel.setNumSteps(this.numSteps); // choose number of steps for resolution
		this.sphoxel.setMinr(this.minr);
		this.sphoxel.setMaxr(this.maxr);
//		this.ratios = new double [this.numSteps][2*this.numSteps];
	}
	
	// ________ Update and compute the Sphoxel and nbhStringTraces data ___________
	
	private void calcSphoxel() throws SQLException{
		this.ratios = new double [this.numSteps][2*this.numSteps];
		
		if (this.radiusRangesFixed){

			if (this.minr==this.radiusThresholds[0] && this.maxr==this.radiusThresholds[1])
				this.radiusPrefix = CMPdb_sphoxel.radiusRanges[0];
			else if (this.minr==this.radiusThresholds[1] && this.maxr==this.radiusThresholds[2])
				this.radiusPrefix = CMPdb_sphoxel.radiusRanges[1];
			else if (this.minr==this.radiusThresholds[2] && this.maxr==this.radiusThresholds[3])
				this.radiusPrefix = CMPdb_sphoxel.radiusRanges[2];
			
//			File dir1 = new File (".");		
			String fn = "";
			String archFN = Start.SPHOXEL_DIR + "SphoxelBGs.zip";
			boolean useUserDefinedPath = false;
			if (Start.SPHOXEL_BG_FILE_PATH != ""){
				File file = new File(Start.SPHOXEL_BG_FILE_PATH + "/SphoxelBGs.zip");
				if (file.exists()){
					archFN = Start.SPHOXEL_BG_FILE_PATH + "/SphoxelBGs.zip";
					useUserDefinedPath = true;
				}					
			}
			
//			archFN = Start.SPHOXEL_DIR + "SphoxelBGs.zip";
			ZipFile zipfile = null;
			try {
				File file;
				if (Start.SPHOXEL_BG_FILE_PATH != ""  && useUserDefinedPath)
					file = new File(archFN);
				else 
//				File file = new File(archFN);
//				if (!file.exists())
				{
//					System.out.println("ZIP: "+this.getClass().getResource(archFN).getFile());
					file = new File(this.getClass().getResource(archFN).getPath());
				}
				zipfile = new ZipFile(file.getPath());
//				System.out.println(file.getPath()+"  loaded");
//				zipfile = new ZipFile(file);
//				System.out.println(file.toString()+"  loaded");		
				
//				try {
//					File file = new File(this.getClass().getResource(archFN).toURI());
//					zipfile = new ZipFile(file);
//				} catch (URISyntaxException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				zipfile = new ZipFile(new File(this.getClass().getResource(archFN).getFile()) );
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			fn = "";
			
			fn = fn+"sphoxelBG_"+this.iRes+"-"+String.valueOf(this.iSSType).toLowerCase()+"_"+this.jRes+"-"
				+String.valueOf(CMPdb_sphoxel.AnySStype).toLowerCase()+"_"+this.radiusPrefix+".csv";
			System.out.println("Filename= "+fn);
//			fn = "/Users/vehlow/Documents/workspace/outputFiles/LogOddsScoresBayes_fromDB-bagler_all13p0_alledges_A-A_SStype-H_radius9.2-12.8_resol90.csv";
			
//			int zipSize = zipfile.size();
//			Enumeration<? extends ZipEntry> entries = zipfile.entries();
//			while (	entries.hasMoreElements() ){
//				ZipEntry entry = entries.nextElement();
//				if (entry.getName() == fn)
//					System.out.println(entry.getName());
//			}
			ZipEntry zipentry = zipfile.getEntry(fn);
			
			CSVhandler csv = new CSVhandler();
			try {
//				this.bayesRatios = csv.readCSVfile3Ddouble(fn);
				this.bayesRatios = csv.readCSVfile3Ddouble(zipfile, zipentry);
				setNumSteps(this.bayesRatios.length);
				this.ratios = new double[this.bayesRatios.length][this.bayesRatios[0].length];
				for (int i=0; i<this.bayesRatios.length; i++){
					for (int j=0; j<this.bayesRatios[i].length; j++){
						this.ratios[i][j] = this.bayesRatios[i][j][0];
					}
				}
				for(int i=0;i<this.ratios.length;i++){ // dim for phi
					for(int j=0;j<this.ratios[i].length;j++){ // dim for lambda					                 
						if (this.ratios[i][j]<this.minRatio)
							this.minRatio = this.ratios[i][j];
						if (this.ratios[i][j]>this.maxRatio)
							this.maxRatio = this.ratios[i][j];
					}  
				}	
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			setSphoxelParam();
							
			this.sphoxel.runBayesPreComp();
			this.ratios = this.sphoxel.getRatios();
			System.out.println("BayesRatios computed");		
//			this.bayesRatios = sphoxel.getBayesRatios();
			this.minRatio = this.sphoxel.getMinRatio();
			this.maxRatio = this.sphoxel.getMaxRatio();
		}
			
	}
	
	public void recalcSphoxel() throws SQLException{
		if (this.sphoxel.getNumSteps()!=this.numSteps || this.sphoxel.getMinr()!=this.minr || this.sphoxel.getMaxr()!=this.maxr 
				|| this.sphoxel.getIRes()!=this.iRes || this.sphoxel.getJRes()!=this.jRes 
				|| this.sphoxel.getISSType()!=this.iSSType || this.sphoxel.getJSSType()!=this.jSSType || this.sphoxel.getDiffSSType()!=this.diffSStype){
//			setSphoxelParam();
			calcSphoxel();
			
			updateScreenBuffer();
		}
	}
	
	private void setTracesParam(){
		
//		this.iSSType = CMPdb_sphoxel.AnySStype;
//	}
//	else{
//		if (iSSelem.isHelix())
//			this.iSSType = SecStrucElement.HELIX;
//		else if (iSSelem.isOther())
//			this.iSSType = SecStrucElement.OTHER;
//		else if (iSSelem.isStrand())
//			this.iSSType = SecStrucElement.STRAND;
//		else if (iSSelem.isTurn())
//			this.iSSType = SecStrucElement.TURN;
//		
//		this.diffSStype = true;
		
		nbhsTraces.setDiffSSType(this.diffSStypeNBH);
		nbhsTraces.setSSType(this.nbhSSType);
		nbhsTraces.setMaxNumLines(this.maxNumTraces);
		nbhsTraces.setNBHS(this.nbhStringL);
	}
	

	private void calcOptNbhStrings() throws SQLException{
		optNBHString.run();
		this.optNBHStrings = optNBHString.getOptNBHStrings();
		extractSetOfOptStrings();
		// test output
//		System.out.println("setOfOptStrings");
//		for (int i=0; i<this.setOfOptStrings.length; i++)
//			System.out.println(this.setOfOptStrings[i]);
		
//		System.out.println("Optimal NBHStrings extracted");		
	}

	public void extractSetOfOptStrings(){
		String[] stringN;
		int count = 0;
		this.setOfOptStrings = new String[10];
		for(int i=0; i<this.optNBHStrings.size(); i++){
			stringN = (String[]) this.optNBHStrings.get(i);	
			int support = Integer.valueOf(stringN[2]);
			if (count<10 && support<=this.maxNumTraces){
				this.setOfOptStrings[count] = stringN[0];
				count++;
			}
			if (count == 10)
				i = this.optNBHStrings.size();
		}
	}
	
	private void calcNbhsTraces() throws SQLException{
		this.clusterIDs = null;
		this.clusterDirProp = null;
		this.clusters = null;
		this.clusterProp = null;
		this.nbCluster = null;
		this.clusterAverDirec = null;
//		System.out.println("INum="+this.iNum);
		nbhsTraces.run();
		System.out.println("NbhsTraces extracted");
		nbhsNodes = nbhsTraces.getNBHSnodes();
		
//		// FAKE
//		CSVhandler csv = new CSVhandler();
//		String sFileName = "/Users/vehlow/Documents/workspace/outputFiles/NBHSnodes_fromDB-bagler_cb8p0_alledges_nbhs-%C%P%x%H%G%_JAtom-CA.csv";
//		this.nbhString = "CPxHG";
//		sFileName = "/Users/vehlow/Documents/workspace/outputFiles/NBHSnodes_example_%A%K%x%G%L%V%.csv";
//		this.nbhString = "AKxGLV";
//		if (sFileName!=null){
//            System.out.println("Chosen path/file:" + sFileName);
//		}
//		else
//            System.out.println("No path chosen!");
//		try {
//			this.nbhsNodes = csv.readCSVfileVector(sFileName);
//		} catch (NumberFormatException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		this.nbhStringL = "%";
//		int count = 0;
//		this.nbhsRes = new char[this.nbhString.length()];
//		for (int i=0; i<this.nbhString.length(); i++){
//			this.nbhStringL += this.nbhString.charAt(i);
//			this.nbhStringL += "%";
//			this.nbhsRes[count] = this.nbhString.charAt(i);
//			count++;
//		}
//		// END FAKE
		
		// compute nbhstringTrace properties	
		if (this.nbhsNodes.size()>0){
//			System.out.println("this.nbhsNodes.size(): "+this.nbhsNodes.size());
			float[] node, nbNode;
			int numNodes = 1, minDist = 1, maxDist = 1;
			Vector<Integer> numNodesL = new Vector<Integer>();	
			Vector<Integer> minDists = new Vector<Integer>();	
			Vector<Integer> maxDists = new Vector<Integer>();	
			this.numLines = 1;
			node = (float[]) this.nbhsNodes.get(0);
			minDist = (int) node[2];
			for(int i=1; i<this.nbhsNodes.size(); i++){
				node = (float[]) this.nbhsNodes.get(i);	
				nbNode = (float[]) this.nbhsNodes.get(i-1);
//				System.out.println(node[0]+"-"+nbNode[0] +"  "+ node[1]+"-"+nbNode[1]);
				if (node[0]==nbNode[0] && node[1]==nbNode[1] && i>0){
					numNodes++;
				}
				else{  //nodes not of same line
					minDists.add(minDist);// add first before update mindist
					minDist = (int) node[2];
					maxDist = (int) nbNode[2];// first update maxdist before adding
					maxDists.add(maxDist);
					numNodesL.add(numNodes);
					this.numLines++;
					numNodes = 1;
				}
			}
			numNodesL.add(numNodes);
			minDists.add(minDist);
			maxDists.add(maxDist);
			this.numNodesPerLine = new int[this.numLines];
			this.minDistsJRes = new int[this.numLines];
			this.maxDistsJRes = new int[this.numLines];
//			System.out.println(this.numLines +" =? "+numNodesL.size());
			for(int i=0; i<numNodesL.size(); i++){
				this.numNodesPerLine[i] = (int) numNodesL.get(i);
				this.minDistsJRes[i] = (int) minDists.get(i);
				this.maxDistsJRes[i] = (int) maxDists.get(i);
//				System.out.print(this.numNodesPerLine[i]+"_"+this.minDistsJRes[i]+"_"+this.maxDistsJRes[i]+"\t");
			}
//			System.out.println();
		}
		
	}
	
	public void recalcOptNBHStrings() throws SQLException{
		this.optNBHString.setFullNBHString(this.origNBHString);
		this.optNBHString.setiRes(this.iRes);
		calcOptNbhStrings();
		
		// Update menu (contact status bar)
		if (contStatBar!= null){
			this.contStatBar.getNBHSPanel().setNbhString(this.origNBHString);
			this.contStatBar.setSetOfOptStrings(this.setOfOptStrings);
			this.contStatBar.setChosenStringID(0);
		}
	}
	
	public void recalcTraces(boolean perform) throws SQLException{
		if (perform){
			setTracesParam();
			calcNbhsTraces();
			
			updateScreenBuffer();			
		}
	}
	
	public void recalcTraces() throws SQLException{
		if (this.nbhsTraces.getDiffSSType()!=this.diffSStype || this.nbhsTraces.getSSType()!=this.iSSType){
			if (this.contactView.isShowTracesFeature())
				recalcTraces(true);
		}
	}
	
	public void calcHistogramms(){
		calcHistOfSphoxelMap();
		calcHistOfNBHSTraces();
		calcHistOfNodeCluster();
		calcHistOfNodeDistrAroundSel();
		calcNodeDistr4Sel();
	}
	
	/**
	 * Computes the min, max and average position of nodes within each cluster.
	 * Computes ditribution of residue types within each cluster.
	 */
	private void calcHistOfNBHSTraces(){
//		ColorScaleView colView = this.contactView.getColorScaleView();
//		double [] scale = colView.getScaleValues();
		this.histTWholeAngleRange = new int[4][aas.length];
		this.histT4SelAngleRange = new Vector<int[][]>();
		this.minMaxAverT4SelAngleRange = new Vector<double[]>();
		int[][][] tempHist = new int[this.lambdaRanges.size()][4][aas.length];
		double[] averPhi = new double[this.phiRanges.size()];
		double[] averLambda = new double[this.lambdaRanges.size()];
		double[] minPhi = new double[this.lambdaRanges.size()];
		double[] maxPhi = new double[this.lambdaRanges.size()];
		double[] minLambda = new double[this.lambdaRanges.size()];
		double[] maxLambda = new double[this.lambdaRanges.size()];
		this.foundNodes = new int[this.lambdaRanges.size()];
		
		float[] node;
		float phiRad, lambdaRad;
		double tMin, tMax, pMin, pMax;
		int jResID, jSSTypeID;
//		char jRes, jSSType;
		
		for(int i=0; i<this.nbhsNodes.size(); i++){			
			node = (float[]) this.nbhsNodes.get(i);
			phiRad = node[3];
			lambdaRad = (float) (node[4] + Math.PI);
			jResID = (int) node[5];
//			jRes = this.aas[jResID];
			jSSTypeID = (int) node[6];
//			jSSType = this.sstypes[jSSTypeID];
			
			if (jSSTypeID>=histTWholeAngleRange.length-1 || jSSTypeID<0)
				jSSTypeID = this.SSTStr.indexOf("O");
			this.histTWholeAngleRange[jSSTypeID][jResID] += 1; 
			this.histTWholeAngleRange[this.sstypes.length-1][jResID] += 1; 
			
			Iterator<Pair<Double>> itrP = this.lambdaRanges.iterator();
			Iterator<Pair<Double>> itrT = this.phiRanges.iterator();
			int index = 0;
			while (itrP.hasNext()){
				Pair<Double> lambda = (Pair<Double>) itrP.next();
				Pair<Double> phi = (Pair<Double>) itrT.next();
				tMin = phi.getFirst();
				tMax = phi.getSecond();
				pMin = lambda.getFirst();
				pMax = lambda.getSecond();
				
				if (phiRad>=tMin && phiRad<=tMax && lambdaRad>=pMin && lambdaRad<=pMax){
					tempHist[index][jSSTypeID][jResID] += 1; 
					tempHist[index][this.sstypes.length-1][jResID] += 1; 
					
					if (phiRad>maxPhi[index])
						maxPhi[index]=phiRad;
					if (lambdaRad>maxLambda[index])
						maxLambda[index]=lambdaRad;
					if (phiRad<minPhi[index] || minPhi[index]==0)
						minPhi[index]=phiRad;
					if (lambdaRad<minLambda[index] || minLambda[index]==0)
						minLambda[index]=lambdaRad;
					averPhi[index] += phiRad;
					averLambda[index] += lambdaRad;
					foundNodes[index] += 1;
				}
				
				index++;
			}
			
		}
		for (int i=0; i<this.lambdaRanges.size(); i++){
			int [][] count4Sel = tempHist[i];
			histT4SelAngleRange.add(count4Sel);
			
			System.out.println("Selection Nr:"+i);
			for (int j=0; j<aas.length; j++)
				System.out.println(aas[j]+":   H:"+count4Sel[0][j]+":   S:"+count4Sel[1][j]
				                         +":   O:"+count4Sel[2][j]+":   A:"+count4Sel[3][j]);
			
			// calculate average values
			averPhi[i] = averPhi[i]/foundNodes[i];
			averLambda[i] = averLambda[i]/foundNodes[i];
		}
		this.minMaxAverT4SelAngleRange.add(minLambda);
		this.minMaxAverT4SelAngleRange.add(minPhi);
		this.minMaxAverT4SelAngleRange.add(maxLambda);
		this.minMaxAverT4SelAngleRange.add(maxPhi);
		this.minMaxAverT4SelAngleRange.add(averLambda);
		this.minMaxAverT4SelAngleRange.add(averPhi);
		tMin =0;
	}
	
	public void calcHistOfSphoxelMap(){
		ColorScaleView colView = this.contactView.getColorScaleView();
		HistogramView histView = this.contactView.getHistogramView();
		double [] scale;
		if (colView != null)
			scale = colView.getScaleValues();
		else 
			scale = histView.getScaleValues();
		this.histWholeAngleRange = new int[scale.length];
		this.hist4SelAngleRange = new Vector<int[]>();
		double phiRad, lambdaRad;
		double tMin, tMax, pMin, pMax;
		double ratio;
		int[][] counts = new int[this.lambdaRanges.size()][scale.length];
		for(int i=0;i<ratios.length;i++){
			for(int j=0;j<ratios[i].length;j++){	
				Iterator<Pair<Double>> itrP = this.lambdaRanges.iterator();
				Iterator<Pair<Double>> itrT = this.phiRanges.iterator();
				phiRad = (i*deltaRad);
				lambdaRad = (j*deltaRad);
				ratio = ratios[i][j];
				for(int k=0; k<scale.length-1; k++){
//					if (scale[k]==0)
//						k++;
					if (ratio>=scale[k] && ratio<scale[k+1]){
						histWholeAngleRange[k] += 1; // = histWholeAngleRange[k]+1;
						k = scale.length;
					}
				}
				int index = 0;
				while (itrP.hasNext()){
					Pair<Double> lambda = (Pair<Double>) itrP.next();
					Pair<Double> phi = (Pair<Double>) itrT.next();
					tMin = phi.getFirst();
					tMax = phi.getSecond();
					pMin = lambda.getFirst();
					pMax = lambda.getSecond();
					for(int k=0; k<scale.length-1; k++){
						if (phiRad>=tMin && phiRad<tMax && lambdaRad>=pMin && lambdaRad<pMax && ratio>=scale[k] && ratio<scale[k+1]){
							counts[index][k] += 1; //= counts[index][k]+1;
							k = scale.length;
						}
					}
					index++;
				}
			}
		}
		// copy values from int array to vector
		for (int i=0; i<this.lambdaRanges.size(); i++){
			int [] count4Sel = counts[i];
			hist4SelAngleRange.add(count4Sel);
		}
		
		ratio = 0;
	}
	
	/**
	 * Computes the min, max and aver values
	 */
	private void calcHistOfNodeCluster(){		

//		this.minMaxAverT4SelAngleRange.add(minLambda);
//		this.minMaxAverT4SelAngleRange.add(minPhi);
//		this.minMaxAverT4SelAngleRange.add(maxLambda);
//		this.minMaxAverT4SelAngleRange.add(maxPhi);
//		this.minMaxAverT4SelAngleRange.add(averLambda);
//		this.minMaxAverT4SelAngleRange.add(averPhi);
//		int clusterID =1;
		this.nodeDistrWithinSel = null;
		if (this.chosenSelection>=0 && this.chosenSelection<this.lambdaRanges.size()){			

			// cluster centre
			double averL = minMaxAverT4SelAngleRange.get(4)[this.chosenSelection]; // this.clusterProp[clusterID-1][1];
		    double averP = minMaxAverT4SelAngleRange.get(5)[this.chosenSelection];
		    double maxRad = 0;
		    if (Math.abs(minMaxAverT4SelAngleRange.get(0)[this.chosenSelection]-averL)>maxRad)
		    	maxRad = Math.abs(minMaxAverT4SelAngleRange.get(0)[this.chosenSelection]-averL);
		    if (Math.abs(minMaxAverT4SelAngleRange.get(2)[this.chosenSelection]-averL)>maxRad)
		    	maxRad = Math.abs(minMaxAverT4SelAngleRange.get(2)[this.chosenSelection]-averL);
		    if (Math.abs(minMaxAverT4SelAngleRange.get(1)[this.chosenSelection]-averP)>maxRad)
		    	maxRad = Math.abs(minMaxAverT4SelAngleRange.get(1)[this.chosenSelection]-averP);
		    if (Math.abs(minMaxAverT4SelAngleRange.get(3)[this.chosenSelection]-averP)>maxRad)
		    	maxRad = Math.abs(minMaxAverT4SelAngleRange.get(3)[this.chosenSelection]-averP);
		    double deltaRad = 0.03;
		    averL-=Math.PI;
		    int numRanges = (int)(maxRad/deltaRad) + 1;
		    nodeDistrWithinSel = new double[numRanges];
		    
			
			double lambda, phi;
//			double thres = getGeodesicDist(averL-maxRad, averP-maxRad, averL, averP);
//		    System.out.println("Node Distribution within selection "+this.chosenSelection+"  maxRange="+String.valueOf(maxRad));
			
			double rad = deltaRad;
			int id = 0;
			int cnt=0;
			double sum = 0;
			while(rad-deltaRad < maxRad){	
				double innerT = euclideanDist(averL-(rad-deltaRad), averP, averL, averP);
				double outerT = euclideanDist(averL-rad, averP, averL, averP);
//				System.out.println(innerT+"<dist<"+outerT);

				
				for(int i=0; i<this.nbhsNodes.size(); i++){			
					float[] node = (float[]) this.nbhsNodes.get(i);
//				for (int nodeID:nodeIDs){
//					float[] node = (float[]) this.nbhsNodes.get(nodeID);
					lambda = node[4]; //+Math.PI;
					phi = node[3];
					
					double dist = euclideanDist(lambda, phi, averL, averP);
					if (dist>=innerT && dist<outerT)
						cnt++;
				}
				
				nodeDistrWithinSel[id] = (double)cnt/(double)this.foundNodes[this.chosenSelection];
				sum += nodeDistrWithinSel[id];
//				System.out.println("i="+id+"  rad<"+rad+"   "+innerT+"<dist<"+outerT+"   cnt="+cnt+"   distr="+nodeDistrWithinSel[id]+ "   sum="+sum);
				id++;
				rad+=deltaRad;
				cnt=0;
			}			
			
		}
		else
			System.out.println("Distribution can't be computed of background (noise) cluster!");
		

//		if (clusterAnal!=null)
//			clusterAnal.analyseNodeDistributionInCluster(index);
	}
	
	private void calcHistOfNodeDistrAroundSel(){		

//		System.out.println("calcHistOfNodeDistrAroundSel:");
		this.nodeDistrAroundSel = null;
		if (this.chosenSelection>=0 && this.chosenSelection<this.lambdaRanges.size()){			

			// cluster centre
			double averL = minMaxAverT4SelAngleRange.get(4)[this.chosenSelection]; // this.clusterProp[clusterID-1][1];
		    double averP = minMaxAverT4SelAngleRange.get(5)[this.chosenSelection];
		    double maxRad = 0;
		    if (Math.abs(minMaxAverT4SelAngleRange.get(0)[this.chosenSelection]-averL)>maxRad)
		    	maxRad = Math.abs(minMaxAverT4SelAngleRange.get(0)[this.chosenSelection]-averL);
		    if (Math.abs(minMaxAverT4SelAngleRange.get(2)[this.chosenSelection]-averL)>maxRad)
		    	maxRad = Math.abs(minMaxAverT4SelAngleRange.get(2)[this.chosenSelection]-averL);
		    if (Math.abs(minMaxAverT4SelAngleRange.get(1)[this.chosenSelection]-averP)>maxRad)
		    	maxRad = Math.abs(minMaxAverT4SelAngleRange.get(1)[this.chosenSelection]-averP);
		    if (Math.abs(minMaxAverT4SelAngleRange.get(3)[this.chosenSelection]-averP)>maxRad)
		    	maxRad = Math.abs(minMaxAverT4SelAngleRange.get(3)[this.chosenSelection]-averP);
		    		    
		    averL-=Math.PI; // to get into range [-pi:pi]
		    int numRanges = 5;
		    this.nodeDistrAroundSel = new int[numRanges];  
			
			double lambda, phi;
			double iRad=0, oRad=maxRad;
			double iSurf = Math.PI*oRad*oRad;
//			double annulusSurf = 0;
//		    System.out.println("Node Distribution within selection "+this.chosenSelection+"  RadiusSel="
//		    		+String.valueOf(maxRad)+" A="+iSurf);
			
		    for(int i=0; i<this.nbhsNodes.size(); i++){			
				float[] node = (float[]) this.nbhsNodes.get(i);
				lambda = node[4]; //+Math.PI;
				phi = node[3];				
				double dist = euclideanDist(lambda, phi, averL, averP);

				iRad=0;
				oRad=maxRad;
			    for (int j=0; j<numRanges; j++){
					if (dist>=iRad && dist<oRad)
						this.nodeDistrAroundSel[j]++;
					
			    	iRad = oRad;
			    	oRad = Math.sqrt( (iSurf/Math.PI) + (iRad*iRad) );
			    }
			}
		    
		    // Test printout
		    iRad=0;
			oRad=maxRad;
		    for (int j=0; j<numRanges; j++){	
//		    	annulusSurf = Math.PI*(Math.pow(oRad, 2) - Math.pow(iRad, 2));
//		    	System.out.println("Annulus: "+iRad+"-"+oRad+" A="+annulusSurf+" cnt="+this.nodeDistrAroundSel[j]);			
		    	iRad = oRad;
		    	oRad = Math.sqrt( (iSurf/Math.PI) + (iRad*iRad) );
		    }
			
		}
		else
			System.out.println("Distribution can't be computed of background (noise) cluster!");
		

//		if (clusterAnal!=null)
//			clusterAnal.analyseNodeDistributionInCluster(index);
	}
	
	/**
	 * 
	 */
	private void calcNodeDistr4Sel(){
//		System.out.println("calc Hist Of Node Distr Within and Around Sel:");
		this.nodeDistr4Sel = null;
		
		if (this.chosenSelection>=0 && this.chosenSelection<this.lambdaRanges.size()){			

			// cluster centre
			double averL = minMaxAverT4SelAngleRange.get(4)[this.chosenSelection]; // this.clusterProp[clusterID-1][1];
		    double averP = minMaxAverT4SelAngleRange.get(5)[this.chosenSelection];
		    double startRad = 0.05;		    		    
		    averL-=Math.PI; // to get into range [-pi:pi]
		    int numRanges = 15;
		    this.nodeDistr4Sel = new double[2][numRanges];  
			
			double lambda, phi;
			double iRad=0, oRad=startRad;
			double iSurf = Math.PI*oRad*oRad;
//			double annulusSurf = 0;
//		    System.out.println("Node Distribution within selection "+this.chosenSelection+"  RadiusSel="
//		    		+String.valueOf(startRad)+" A="+iSurf);
			
		    for(int i=0; i<this.nbhsNodes.size(); i++){			
				float[] node = (float[]) this.nbhsNodes.get(i);
				lambda = node[4]; //+Math.PI;
				phi = node[3];				
				double dist = euclideanDist(lambda, phi, averL, averP);

				iRad=0;
				oRad=startRad;
			    for (int j=0; j<numRanges; j++){
					if (dist>=iRad && dist<oRad)
						this.nodeDistr4Sel[1][j]++;
					
			    	iRad = oRad;
			    	oRad = Math.sqrt( (iSurf/Math.PI) + (iRad*iRad) );
			    }
			}
		    
		    // Test printout
		    iRad=0;
			oRad=startRad;
		    for (int j=0; j<numRanges; j++){	
		    	this.nodeDistr4Sel[0][j] = oRad;
//		    	annulusSurf = Math.PI*(Math.pow(oRad, 2) - Math.pow(iRad, 2));
//		    	System.out.println("Annulus: "+iRad+"-"+oRad+" A="+annulusSurf+" cnt="+(int)this.nodeDistr4Sel[1][j]);			
		    	iRad = oRad;
		    	oRad = Math.sqrt( (iSurf/Math.PI) + (iRad*iRad) );
		    }
			
		}
		else
			System.out.println("Distribution can't be computed of background (noise) cluster!");
		
	}
	
	private double euclideanDist(double l1, double p1, double l2, double p2){
		if (Math.abs(l1-l2)>Math.PI){
			if (l1<0 || l2<0){
				l1 -= Math.PI;
				l2 -= Math.PI;
				if (l1<-Math.PI)
					l1 += (2*Math.PI);
				if (l2<-Math.PI)
					l2 += (2*Math.PI);
			}
			else{
				l1 -= Math.PI;
				l2 -= Math.PI;	
				if (l1<0)
					l1 += (2*Math.PI);	
				if (l2<0)
					l2 += (2*Math.PI);		
			}
		}
		double dist = Math.sqrt(Math.pow(l1-l2, 2) + Math.pow(p1-p2, 2));
		return dist;
	}
	
	public void runClusterAnalysis(){
		
		double alpha = this.epsilon*Math.PI/180;
		double eps = alpha * this.rSphere;
		
		if (this.clusterAnal==null)
			this.clusterAnal = new NbhString_ClusterAnalysis(this.nbhsNodes,eps,this.minNumNBs,this.rSphere);
		else {
			this.clusterAnal.setEpsilon(eps);
			this.clusterAnal.setMinNumNBs(this.minNumNBs);
			this.clusterAnal.setRadiusSphere(this.rSphere);
			this.clusterAnal.setNbhsNodes(this.nbhsNodes);
		}
		
		System.out.println("Start Clusteranalysis for "+this.nbhsNodes.size()+" nodes with epsilon="+eps);
		clusterAnal.runClusterAnalysis();
		this.clusterIDs = clusterAnal.getClusterN();
		// ToDo: evaluate clusters
		clusterAnal.analyseClusters();
		this.clusters = clusterAnal.getClusters();
		this.clusterProp = clusterAnal.getClusterProp();
		
		clusterAnal.analyseEdgeDirection();
		this.clusterDirProp = clusterAnal.getClusterDirProp();
		this.nbCluster = clusterAnal.getNbCluster();
		this.clusterAverDirec = clusterAnal.getClusterAverDirec();
		
		clusterAnal.analyseCNodeTypes();
		this.mostFrequentResT4Clusters = clusterAnal.getMostFrequentResT4Clusters();
		
		this.updateScreenBuffer();
	}
	
	/*------------------------ writing methods --------------------*/
	public void writeSphoxels(String filename){
		this.sphoxel.writeSphoxelOutput(filename);
	}
	public void writeTraces(String filename){
		this.nbhsTraces.writeNbhsTracesOutput(filename);
	}
	public void writeSettings(String filename){
		CSVhandler csv = new CSVhandler();
		csv.generateFile(this.settings, filename);
	}
	
	/*------------------------ loading methods --------------------*/
	public void loadSettings(String filename) throws NumberFormatException, IOException{
		setDefaultSettings();
		CSVhandler csv = new CSVhandler();
		this.settings = csv.readCsvFile(filename);
		
		this.selContacts = new Vector<Pair<Integer>>();
		this.lambdaRanges = new Vector<Pair<Double>>();
		this.phiRanges = new Vector<Pair<Double>>();
		for (int i=0; i<this.settings.size(); i++){
			// load settings for contact i_j
			String[] set = this.settings.elementAt(i);
			int iNum = Integer.valueOf(set[0]);
			int jNum = Integer.valueOf(set[1]);
			if (this.iNum==iNum && this.jNum==jNum){
				this.nbhString = set[2];
//				this.nbhStringL = set[3];
				this.nbhStringL = "%";
				for (int j=0; j<this.nbhString.length(); j++){
					this.nbhStringL += this.nbhString.charAt(j);
					this.nbhStringL += "%";
				}
				int stringID = Integer.valueOf(set[3]);
				this.maxNumTraces = Integer.valueOf(set[4]);
				this.epsilon = Integer.valueOf(set[5]);
				this.minNumNBs = Integer.valueOf(set[6]);
				this.centerOfProjection = new Pair<Double>( Double.valueOf(set[7]), Double.valueOf(set[8]));
				this.removeOutliers = Boolean.valueOf(set[9]);
				this.minAllowedRat = Double.valueOf(set[10]);
				this.maxAllowedRat = Double.valueOf(set[11]);
				
				this.tmpLambdaRange = new Pair<Double>(Double.valueOf(set[12]), Double.valueOf(set[13]));
				this.tmpPhiRange = new Pair<Double>(Double.valueOf(set[14]), Double.valueOf(set[15]));
				this.tmpSelContact = new Pair<Integer>(iNum,jNum);
				
				updateContactStatusBar(stringID);
			}
			this.lambdaRanges.add(new Pair<Double>(Double.valueOf(set[12]), Double.valueOf(set[13])));
			this.phiRanges.add(new Pair<Double>(Double.valueOf(set[14]), Double.valueOf(set[15])));
			this.selContacts.add(new Pair<Integer>(iNum,jNum));
		}
	}
	
	/*------------------------ drawing methods --------------------*/
	
	public GeneralPath rhombusShape(double xC, double yC, double width, double height){
		GeneralPath shape = new GeneralPath();
		shape.moveTo(xC-(width/2), yC);
		shape.lineTo(xC, yC-(height/2));
		shape.lineTo(xC+(width/2), yC);
		shape.lineTo(xC, yC+(height/2));
		shape.lineTo(xC-(width/2), yC);
		return shape;
	}
	
	public GeneralPath triangleShape(double xC, double yC, double width, double height){
		GeneralPath shape = new GeneralPath();
		shape.moveTo(xC, yC-(height/2));
		shape.lineTo(xC+(width/2), yC+(height/2));
		shape.lineTo(xC-(width/2), yC+(height/2));
		shape.lineTo(xC, yC-(height/2));
		return shape;
	}
	
	public GeneralPath triangleShape(double x1, double y1, double x2, double y2, double x3, double y3){
		GeneralPath shape = new GeneralPath();
		shape.moveTo(x1, y1);
		shape.lineTo(x2, y2);
		shape.lineTo(x3, y3);
		shape.lineTo(x1, y1);
		return shape;
	}
	
	public GeneralPath trapezium(Pair<Double> p1, Pair<Double> p2, Pair<Double> p3, Pair<Double> p4){
		GeneralPath shape = new GeneralPath();
		shape.moveTo(p1.getFirst(), p1.getSecond());
		shape.lineTo(p2.getFirst(), p2.getSecond());
		shape.lineTo(p3.getFirst(), p3.getSecond());
		shape.lineTo(p4.getFirst(), p4.getSecond());
		shape.lineTo(p1.getFirst(), p1.getSecond());
		return shape;
	}
	
	public GeneralPath trapezium(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4){
		GeneralPath shape = new GeneralPath();
		shape.moveTo(x1, y1);
		shape.lineTo(x2, y2);
		shape.lineTo(x3, y3);
		shape.lineTo(x4, y4);
		shape.lineTo(x1, y1);
		return shape;
	}
	
	public GeneralPath nGon(double[] x, double[] y){
		GeneralPath shape = new GeneralPath();
		if (x.length>0 && x.length==y.length){
			shape.moveTo(x[0], y[0]);
			for (int i=1; i<x.length; i++){
				shape.lineTo(x[i], y[i]);
			}
			shape.lineTo(x[0], y[0]);
		}
		return shape;
	}
	
	public GeneralPath pathLine(double[] x, double[] y){
		GeneralPath shape = new GeneralPath();
		if (x.length>0 && x.length==y.length){
			shape.moveTo(x[0], y[0]);
			for (int i=1; i<x.length; i++){
				shape.lineTo(x[i], y[i]);
			}
		}
		return shape;
	}
	
	public double getOffSet(double lambdaRad, double phiRad){
		double xPos=0;	
		if (this.mapProjType==kavrayskiyMapProj){
			lambdaRad -= Math.PI;
			phiRad -= (Math.PI/2);
			
			xPos = lambda2Kavrayskiy(lambdaRad, phiRad);		
			xPos = ( (Math.PI+xPos) *this.voxelsize )+this.border;
		}
		else if (this.mapProjType==cylindricalMapProj){
			lambdaRad -= Math.PI;
			xPos = ( (Math.PI+lambdaRad) *this.voxelsize )+this.border;
		}
		return xPos;
	}
	
	private double distPointsOnSphere(double l1, double p1, double l2, double p2){
		double dist = 0;
		
		double x1 = this.rSphere*Math.sin(p1)*Math.cos(l1);
		double y1 = this.rSphere*Math.sin(p1)*Math.sin(l1);
		double z1 = this.rSphere*Math.cos(p1);
		double x2 = this.rSphere*Math.sin(p2)*Math.cos(l2);
		double y2 = this.rSphere*Math.sin(p2)*Math.sin(l2);
		double z2 = this.rSphere*Math.cos(p2);
		
		dist = Math.sqrt(Math.pow(x1-x2,2) + Math.pow(y1-y2,2) + Math.pow(z1-z2,2));
		return dist;
	}
	
	@SuppressWarnings("unused")
	private double greatCircleDistPointsOnSphere(double l1, double p1, double l2, double p2){
		double dist = 0;
		double spherAng = 0;
		spherAng = Math.acos((Math.sin(p1)*Math.sin(p2)) + (Math.cos(p1)*Math.cos(p2)*Math.cos(l1-l2)));
		dist = this.rSphere * spherAng;
		return dist;
	}
	
	private void drawSphoxelMap(Graphics2D g2d){
		double val;
		Color col = Color.white; 
		ColorScale scale = new ColorScale();
				
		// ----- color representation -----
		for(int i=0;i<ratios.length;i++){
			for(int j=0;j<ratios[i].length;j++){						
				val = ratios[i][j]; // add some scaling and shifting --> range 0:255				
				// ----- remove outliers
				double minRatio2Use = this.minRatio;
				double maxRatio2Use = this.maxRatio;
				if (this.removeOutliers){
					if (minRatio2Use<this.minAllowedRat)
						minRatio2Use = this.minAllowedRat;
					if (maxRatio2Use>this.maxAllowedRat)
						maxRatio2Use = this.maxAllowedRat;
					if (val<this.minAllowedRat)
						val = this.minAllowedRat;
					else if (val>this.maxAllowedRat)
						val = this.maxAllowedRat;
				}						
				// ----- compute alpha and set color	
				float alpha = (float) (Math.abs(val)/Math.abs(maxRatio2Use)); // if val>0
				if(val<0){
					alpha = (float) (Math.abs(val)/Math.abs(minRatio2Use));
					val = -val/minRatio2Use;
				}
				else
					val = val/maxRatio2Use;
				switch (this.chosenColourScale){
				case ContactStatusBar.BLUERED:
					col = scale.getColor4BlueRedScale(val,alpha); break;
				case ContactStatusBar.HOTCOLD:
					col = scale.getColor4HotColdScale(val,alpha); break;
				case ContactStatusBar.RGB:
					col = scale.getColor4RGBscalePolar((float)val, alpha, -1); break;
				}	
				
				g2d.setColor(col);	
			
				drawSphoxel(g2d, i, j);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void drawSphoxelCakeMap(Graphics2D g2d){
		this.voxelsizeFactor = 1.0;
		double val;
		Color col = Color.white; // = new Color(24,116,205,255);
		ColorScale scale = new ColorScale();
		Shape shape = null;
		double xPos1, yPos1, xPos2, yPos2, xPos3, yPos3, xPos4, yPos4;
		
		int numSlices = 18; // = 20Grad-Schritte
		double sliceWidth = 2*Math.PI/numSlices;
		double deltaLambda = sliceWidth/2;
		double lCentre = deltaLambda;
		this.centerOfProjection = new Pair<Double>(lCentre, Math.PI/2);
		xPos1 = getScreenPosFromRad(lCentre, Math.PI/2).getFirst();
		xPos2 = getScreenPosFromRad(lCentre+deltaLambda, Math.PI/2).getFirst();
		double dx = 2* Math.abs(xPos2-xPos1); 
		double xOffset = dx/2;
		int cntSlice = 0;
		System.out.println("num="+numSlices+"  width="+sliceWidth+"  deltaL="+deltaLambda+"  xOffset="+xOffset);	
		
		for(int j=0;j<ratios[0].length;j++){
			double lambdaRad = (j*deltaRad); // 0<lambda<2*PI
			if (lambdaRad > (cntSlice+1)*sliceWidth){
				cntSlice++;
				lCentre += sliceWidth;
				this.centerOfProjection = new Pair<Double>(lCentre, Math.PI/2);
				xOffset += dx;
				System.out.println("cnt="+cntSlice+" lCentre="+lCentre+" xOffset="+xOffset+"   lambdaRad="+lambdaRad);
			}
			for(int i=0;i<ratios.length;i++){
				double phiRad = (i*deltaRad); // 0<phi<2*PI
				
				// ----- COMPUTE AND SET COLOUR -------
				val = ratios[i][j]; // add some scaling and shifting --> range 0:255				
				// ----- remove outliers
				double minRatio2Use = this.minRatio;
				double maxRatio2Use = this.maxRatio;
				if (this.removeOutliers){
					if (minRatio2Use<this.minAllowedRat)
						minRatio2Use = this.minAllowedRat;
					if (maxRatio2Use>this.maxAllowedRat)
						maxRatio2Use = this.maxAllowedRat;
					if (val<this.minAllowedRat)
						val = this.minAllowedRat;
					else if (val>this.maxAllowedRat)
						val = this.maxAllowedRat;
				}						
				// ----- compute alpha and set color	
				float alpha = (float) (Math.abs(val)/Math.abs(maxRatio2Use)); // if val>0
				if(val<0){
					alpha = (float) (Math.abs(val)/Math.abs(minRatio2Use));
					val = -val/minRatio2Use;
				}
				else
					val = val/maxRatio2Use;
				switch (this.chosenColourScale){
				case ContactStatusBar.BLUERED:
					col = scale.getColor4BlueRedScale(val,alpha); break;
				case ContactStatusBar.HOTCOLD:
					col = scale.getColor4HotColdScale(val,alpha); break;
				case ContactStatusBar.RGB:
					col = scale.getColor4RGBscalePolar((float)val, alpha, -1); break;
				}					
				g2d.setColor(col);	
				
				// ----- DRAW SPHOXEL -----
				this.voxelsizeFactor = 1.0;
				xPos1 = getScreenPosFromRad(lambdaRad, phiRad).getFirst() - this.rSphere + xOffset;
				xPos2 = getScreenPosFromRad(lambdaRad+deltaRad, phiRad).getFirst() - this.rSphere + xOffset;
				xPos3 = getScreenPosFromRad(lambdaRad+deltaRad, phiRad+deltaRad).getFirst() - this.rSphere + xOffset;
				xPos4 = getScreenPosFromRad(lambdaRad, phiRad+deltaRad).getFirst() - this.rSphere + xOffset;
				this.voxelsizeFactor = 1.5;	
				yPos1 = (phiRad*this.voxelsize) +this.border;	
				yPos2 = (phiRad*this.voxelsize) +this.border;	
				yPos3 = ((phiRad+deltaRad)*this.voxelsize) +this.border;	
				yPos4 = ((phiRad+deltaRad)*this.voxelsize) +this.border;	
				shape = trapezium(xPos1, yPos1, xPos2, yPos2, xPos3, yPos3, xPos4, yPos4);
				g2d.draw(shape);
				g2d.fill(shape);
			}
		}	
		this.voxelsizeFactor = 1.5;

	}
	
	private void drawSphoxel(Graphics2D g2d, int i, int j){
		Shape shape = null;
		double xPos1, yPos1, xPos2, yPos2, xPos3, yPos3, xPos4, yPos4;
		double phiRad = (i*deltaRad); // 0<phi<2*PI
		double lambdaRad = (j*deltaRad); // 0<lambda<2*PI
		// ---- create and draw shape (rectangle or trapezoid)		
		if (this.mapProjType==kavrayskiyMapProj){
			
			xPos1 = getScreenPosFromRad(lambdaRad, phiRad).getFirst();
			yPos1 = getScreenPosFromRad(lambdaRad, phiRad).getSecond();
			xPos2 = getScreenPosFromRad(lambdaRad+deltaRad, phiRad).getFirst();
			yPos2 = getScreenPosFromRad(lambdaRad+deltaRad, phiRad).getSecond();	
			phiRad += deltaRad;
			xPos3 = getScreenPosFromRad(lambdaRad+deltaRad, phiRad).getFirst();
			yPos3 = getScreenPosFromRad(lambdaRad+deltaRad, phiRad).getSecond();
			xPos4 = getScreenPosFromRad(lambdaRad, phiRad).getFirst();
			yPos4 = getScreenPosFromRad(lambdaRad, phiRad).getSecond();	
			phiRad -= deltaRad;
			
			if (xPos1>xPos2){
				double xPosUL=0, xPosLL=0, xPosUR=getSize().getWidth(), xPosLR=getSize().getWidth();
				xPosUL = getScreenPosFromRad(Math.PI-this.origCoordinates.getFirst(), phiRad).getFirst();
				xPosLL = getScreenPosFromRad(Math.PI-this.origCoordinates.getFirst(), phiRad+deltaRad).getFirst();
				xPosUR = getScreenPosFromRad(Math.PI-this.origCoordinates.getFirst()+2*Math.PI, phiRad).getFirst();
				xPosLR = getScreenPosFromRad(Math.PI-this.origCoordinates.getFirst()+2*Math.PI, phiRad+deltaRad).getFirst();
				shape = trapezium(xPos1, yPos1, xPosUR, yPos2, xPosLR, yPos3, xPos4, yPos4);	
				g2d.draw(shape);	
				g2d.fill(shape);
				shape = trapezium(xPosUL, yPos1, xPos2, yPos2, xPos3, yPos3, xPosLL, yPos4);	
				g2d.draw(shape);	
				g2d.fill(shape);
			}
			else 
			{
				shape = trapezium(xPos1, yPos1, xPos2, yPos2, xPos3, yPos3, xPos4, yPos4);	
				g2d.draw(shape);
				g2d.fill(shape);		
			}	
			
		}
		else if (this.mapProjType==cylindricalMapProj){				
			xPos1 = j*pixelWidth;
			yPos1 = i*pixelWidth;
			xPos1 = translateXPixelCoordRespective2Orig(xPos1) +this.border;
			yPos1 = translateYPixelCoordRespective2Orig(yPos1) +this.border;	
			shape = new Rectangle2D.Double(xPos1, yPos1, pixelWidth, pixelHeight);	
			g2d.draw(shape);
			g2d.fill(shape);
			
			if (xPos1+pixelWidth > this.xDim)
				xPos1 -= xDim;
			if (yPos1+pixelHeight > this.yDim)
				yPos1 -= yDim;
			shape = new Rectangle2D.Double(xPos1, yPos1, pixelWidth, pixelHeight);
			g2d.draw(shape);
			g2d.fill(shape);
		}
		else if (this.mapProjType==azimuthalMapProj){	
			xPos1 = getScreenPosFromRad(lambdaRad, phiRad).getFirst();
			yPos1 = getScreenPosFromRad(lambdaRad, phiRad).getSecond();	
			xPos2 = getScreenPosFromRad(lambdaRad+deltaRad, phiRad).getFirst();
			yPos2 = getScreenPosFromRad(lambdaRad+deltaRad, phiRad).getSecond();	
			xPos3 = getScreenPosFromRad(lambdaRad+deltaRad, phiRad+deltaRad).getFirst();
			yPos3 = getScreenPosFromRad(lambdaRad+deltaRad, phiRad+deltaRad).getSecond();	
			xPos4 = getScreenPosFromRad(lambdaRad, phiRad+deltaRad).getFirst();
			yPos4 = getScreenPosFromRad(lambdaRad, phiRad+deltaRad).getSecond();

			if (!isOnFrontView(lambdaRad, phiRad) || !isOnFrontView(lambdaRad+deltaRad, phiRad) || 
					!isOnFrontView(lambdaRad+deltaRad, phiRad+deltaRad) || !isOnFrontView(lambdaRad, phiRad+deltaRad)){
				if (xPos1<2*this.rSphere) //if (isOnFrontView(lambdaRad, phiRad))
					xPos1 = (4*this.rSphere)-xPos1;
				if (xPos2<2*this.rSphere) //if (isOnFrontView(lambdaRad+deltaRad, phiRad))
					xPos2 = (4*this.rSphere)-xPos2;
				if (xPos3<2*this.rSphere) //if (isOnFrontView(lambdaRad+deltaRad, phiRad+deltaRad))
					xPos3 = (4*this.rSphere)-xPos3;
				if (xPos4<2*this.rSphere) //if (isOnFrontView(lambdaRad, phiRad+deltaRad))
					xPos4 = (4*this.rSphere)-xPos4;
				if (!(xPos1>2*this.rSphere && xPos2>2*this.rSphere && xPos3>2*this.rSphere && xPos4>2*this.rSphere))
					System.out.println("!!!!");
			}
//			if (xPos1>2*this.rSphere || xPos2>2*this.rSphere || xPos3>2*this.rSphere || xPos4>2*this.rSphere)
//				System.out.println("!!!!");
//			if (!(xPos1<2*this.rSphere && xPos2<2*this.rSphere && xPos3<2*this.rSphere && xPos4<2*this.rSphere))
//				System.out.println("!!!!");
			shape = trapezium(xPos1, yPos1, xPos2, yPos2, xPos3, yPos3, xPos4, yPos4);
							
//			if (isOnFrontView(lambdaRad, phiRad))
//				shape = trapezium(xPos1, yPos1, xPos2, yPos2, xPos3, yPos3, xPos4, yPos4);
//			else
//				shape = trapezium((4*this.rSphere)-xPos1, yPos1, (4*this.rSphere)-xPos2, yPos2, (4*this.rSphere)-xPos3, yPos3, (4*this.rSphere)-xPos4, yPos4);
			
			g2d.draw(shape);
			g2d.fill(shape);	
		}
	}
	
	@SuppressWarnings("unused")
	private void drawSphoxels(Graphics2D g2d){
		Shape shape = null;
		double xPos;
		double yPos;
		double val;
		Color col = Color.white; // = new Color(24,116,205,255);
		ColorScale scale = new ColorScale();
		
		g2d.setBackground(backgroundColor);
		shape = new Rectangle2D.Float(0, 0, this.pixelWidth*this.ratios[0].length, this.pixelHeight*this.ratios.length);
		g2d.setColor(backgroundColor);
		g2d.draw(shape);
		g2d.fill(shape);
				
		// ----- color representation -----
		for(int i=0;i<ratios.length;i++){
			for(int j=0;j<ratios[i].length;j++){
				xPos = translateXPixelCoordRespective2Orig(j*pixelWidth) +this.border;
				yPos = translateYPixelCoordRespective2Orig(i*pixelHeight) +this.border;			
				shape = new Rectangle2D.Double(xPos, yPos, pixelWidth, pixelHeight);	
				
				val = ratios[i][j]; // add some scaling and shifting --> range 0:255
				
				// ----- remove outliers
				double minRatio2Use = this.minRatio;
				double maxRatio2Use = this.maxRatio;
				if (this.removeOutliers){
					if (minRatio2Use<this.minAllowedRat)
						minRatio2Use = this.minAllowedRat;
					if (maxRatio2Use>this.maxAllowedRat)
						maxRatio2Use = this.maxAllowedRat;
					if (val<this.minAllowedRat)
						val = this.minAllowedRat;
					else if (val>this.maxAllowedRat)
						val = this.maxAllowedRat;
				}
								
				// ----- compute alpha and set color	
				float alpha = (float) (Math.abs(val)/Math.abs(maxRatio2Use)); // if val>0
				if(val<0){
					alpha = (float) (Math.abs(val)/Math.abs(minRatio2Use));
					val = -val/minRatio2Use;
				}
				else
					val = val/maxRatio2Use;
				if (alpha>1.0f) alpha=1.0f;
				if (alpha<0.0f) alpha=0.0f;
				
				switch (this.chosenColourScale){
				case ContactStatusBar.BLUERED:
					col = scale.getColor4BlueRedScale(val,alpha); break;
				case ContactStatusBar.HOTCOLD:
					col = scale.getColor4HotColdScale(val, alpha);
				case ContactStatusBar.RGB:
					col = scale.getColor4RGBscalePolar((float)val, alpha, -1);
				}
				
				g2d.setColor(col);
				g2d.draw(shape);
				g2d.fill(shape);
				if (xPos+pixelWidth > this.xDim)
					xPos -= xDim;
				if (yPos+pixelHeight > this.yDim)
					yPos -= yDim;
				shape = new Rectangle2D.Double(xPos, yPos, pixelWidth, pixelHeight);
				g2d.draw(shape);
				g2d.fill(shape);
			}
		}
	}
	
	public Color getNodeColor4SSType(int jSSTypeID){
		Color col = Color.black;
		switch (jSSTypeID){
		case 0: // 'H'
			col = (helixSSTColor); break;
		case 1: // 'S'
			col = (sheetSSTColor); break;
		case 2: // 'O'
			col = (otherSSTColor); break;
		}
		return col;
	}
	
	public Color getNodeColor4SSType(int jSSTypeID, int alpha){
		Color col = Color.black;
		switch (jSSTypeID){
		case 0: // 'H'
			col = new Color(helixSSTColor.getRed(), helixSSTColor.getGreen(), helixSSTColor.getBlue(), alpha); break;
		case 1: // 'S'
			col = new Color(sheetSSTColor.getRed(), sheetSSTColor.getGreen(), sheetSSTColor.getBlue(), alpha); break;
		case 2: // 'O'
			col = new Color(otherSSTColor.getRed(), otherSSTColor.getGreen(), otherSSTColor.getBlue(), alpha); break;
		}
		return col;
	}
	
	private void drawNBHSNode(Graphics2D g2d, double lambdaRad, double phiRad, boolean specialRes, boolean isJRes, String resType){
		GeneralPath rhombus = null;
		Shape circle = null;
		Font f = new Font("Dialog", Font.PLAIN, 12);
		float radius = 3.f;
		double xPos, yPos;
		xPos = getScreenPosFromRad(lambdaRad, phiRad).getFirst();
		yPos = getScreenPosFromRad(lambdaRad, phiRad).getSecond();
//		if (this.mapProjType==azimuthalMapProj && !isOnFrontView(lambdaRad, phiRad))
//			xPos = (4*this.rSphere)-xPos;
		
		// ---- draw geometric object for each residue
		g2d.setColor(Color.black);
		if (specialRes){
			radius = 6.f;
			// create rhombus for residue types contained in nbhstring
			rhombus = rhombusShape(xPos, yPos+this.yBorderThres, 2*radius, 2*radius);
			g2d.draw(rhombus);
			g2d.fill(rhombus);
			f = new Font("Dialog", Font.PLAIN, 14);
		}
		else {
			radius = 3.f;
			// create ellipse for residue
			circle = new Ellipse2D.Double( xPos-radius, yPos-radius+this.yBorderThres,2*radius, 2*radius);
			g2d.draw(circle);
			g2d.fill(circle);
			f = new Font("Dialog", Font.PLAIN, 12);
		}
		g2d.setFont(f);
		String nodeName = resType; //Character.toString(aas[jResID]) +" "+ String.valueOf(jNum-iNum);		
		
		if (specialRes && isJRes)
				g2d.setColor(Color.blue);
//		if (showResInfo)
			g2d.drawString(nodeName, (float)(xPos+radius), (float)(yPos+radius+this.yBorderThres));
	}
	
	private void drawNBHSNode(Graphics2D g2d, boolean specialRes, boolean isJRes, float[] node, int index){
		int iNum, jNum,jResID, jSSType;
		double phiRad, lambdaRad;
		
		iNum = (int) node[1];
		jNum = (int) node[2];
		phiRad = node[3];
		lambdaRad = node[4];
		jResID = (int) node[5];
		jSSType = (int) node[6];		
		lambdaRad += Math.PI;

		GeneralPath rhombus = null;
		GeneralPath triangle = null;
		Shape circle = null;
		Font f = new Font("Dialog", Font.PLAIN, 12);
		float radius = 3.f;
		String nodeName;
		double xPos, yPos;
		
		xPos = getScreenPosFromRad(lambdaRad, phiRad).getFirst();
		yPos = getScreenPosFromRad(lambdaRad, phiRad).getSecond();
//		if (this.mapProjType==azimuthalMapProj && !isOnFrontView(lambdaRad, phiRad))
//			xPos = (4*this.rSphere)-xPos;
		
		// ---- draw geometric object for each residue
		Color col = getNodeColor4SSType(jSSType);
		if (this.clusterIDs != null && this.clusterIDs[index]>0)
			col = getNodeColor4SSType(jSSType, 255);
		else
			col = getNodeColor4SSType(jSSType, 100);		
		g2d.setColor(col);
		
		if (this.clusterIDs != null && this.clusterIDs[index]>0){
			if (specialRes){
				radius = 7.f;
				f = new Font("Dialog", Font.PLAIN, 14);
			}
			else {
				radius = 4.f;
				f = new Font("Dialog", Font.PLAIN, 12);
			}
			g2d.setColor(Color.white);
			// create triangle for residue
			triangle = triangleShape(xPos, yPos+this.yBorderThres, 2*radius, 2*radius);
			g2d.draw(triangle);
			g2d.fill(triangle);
			radius = radius - 2.f;
			g2d.setColor(col);
			triangle = triangleShape(xPos, yPos+this.yBorderThres, 2*radius, 2*radius);
			g2d.draw(triangle);
			g2d.fill(triangle);
			f = new Font("Dialog", Font.PLAIN, 14);
		}
		else if (specialRes){
			radius = 6.f;
			// create rhombus for residue types contained in nbhstring
			rhombus = rhombusShape(xPos, yPos+this.yBorderThres, 2*radius, 2*radius);
			g2d.draw(rhombus);
			g2d.fill(rhombus);
			f = new Font("Dialog", Font.PLAIN, 14);
		}
		else {
			radius = 3.f;
			// create ellipse for residue
			circle = new Ellipse2D.Double( xPos-radius, yPos-radius+this.yBorderThres,2*radius, 2*radius);
			g2d.draw(circle);
			g2d.fill(circle);
			f = new Font("Dialog", Font.PLAIN, 12);
		}
		g2d.setFont(f);
		nodeName = Character.toString(aas[jResID]) +" "+ String.valueOf(jNum-iNum);		
		
		if (specialRes){
			g2d.setColor(Color.black);
			if (isJRes)
				g2d.setColor(Color.red);
		}
		else
			g2d.setColor(new Color(70,70,70));
		if (showResInfo)
			g2d.drawString(nodeName, (float)(xPos+radius), (float)(yPos+radius+this.yBorderThres));
	}
	
	private void drawNBHSEdges(Graphics2D g2d, float[] node, int nodeID, int lineID, int j){
//		Shape line = null;
		int gID, iNum, jNum, gIdNB, iNumNB, jNumNB; 
		float[] nbNode;
		ColorScale scale = new ColorScale();
		Color col = null;		
		double phiRad, lambdaRad, phiRadNB, lambdaRadNB;
		
		gID = (int) node[0];
		iNum = (int) node[1];
		jNum = (int) node[2];
		phiRad = node[3];
		lambdaRad = node[4];				
		lambdaRad += Math.PI; // cause lambda is expected to have a value of [0:pi]
		
		// --- gradient color edges between connected nodes
		nbNode = (float[]) this.nbhsNodes.get(j);
		gIdNB = (int) nbNode[0];
		iNumNB = (int) nbNode[1];
		jNumNB = (int) nbNode[2];
		phiRadNB = nbNode[3];
		lambdaRadNB = nbNode[4];
		lambdaRadNB += Math.PI; // cause lambda is expected to have a value of [0:pi]
		
		if (gID==gIdNB && iNum==iNumNB){
//			System.out.print(nodeID +"\t" + lineID +"\t" + this.numNodesPerLine[lineID] +"\t");
			float ratio = (float)(nodeID+1)/(float)this.numNodesPerLine[lineID];
			
			// --- use ratio for color scale ---
//			g2d.setColor(scale.getColor4YellowRedScale(ratio, 1.0f));
			g2d.setColor(scale.getColor4RGBscale(ratio, 1.0f, 1));
			
//			// -- equal scaling
//			if (jNum-iNum < 0)
//				ratio = -1 + (float)(jNum-this.minDistsJRes[lineID])/(float)(iNum-this.minDistsJRes[lineID]);
//			else 
//				ratio = (float)(jNum-iNum)/(float)(this.maxDistsJRes[lineID]-iNum);
//		
//			// -- logarithmic scaling
//			if (jNum-iNum < 0)
//				ratio = -1 + (float)Math.log(jNum-this.minDistsJRes[lineID]+1)/(float)Math.log(iNum-this.minDistsJRes[lineID]+1);
////					ratio = -1 + (float)Math.log10(jNum-this.minDistsJRes[lineID]+1)/(float)Math.log10(iNum-this.minDistsJRes[lineID]+1);
//			else 
//				ratio = (float)Math.log(jNum-iNum+1)/(float)Math.log(this.maxDistsJRes[lineID]-iNum+1);
////					ratio = (float)Math.log10(jNum-iNum+1)/(float)Math.log10(this.maxDistsJRes[lineID]-iNum+1);
//			System.out.print(iNum+"_"+jNum+"_"+this.minDistsJRes[lineID]+"_"+this.maxDistsJRes[lineID]+":"+ratio +"\t");
//			g2d.setColor(scale.getColor4RGBscalePolar(ratio, 1.0f, 1));
			
			// -- shortrange scaling: |jNum-iNum|>ShortRangeThreshold --> blue
			int thres1 = 9; // 1-9:short range  9-25:middle range  25-n/9-n:long range
			int thres2 = 25;
			col = Color.black;
			boolean useGeodesic = true; // draw all as geodesic lines
			if (Math.abs(jNum-iNum)<=thres1){
				ratio = +1 * (float)Math.abs(jNum-iNum)/(float)(thres1);
				// scale on range 0.2:0.8
				ratio = 0.2f + (ratio*(0.8f-0.2f));
				col = scale.getColor4GreyValueRange(ratio, 1);
//					useGeodesic = false;
			}
			else if (Math.abs(jNum-iNum)<=thres2){
				if (jNum-iNum < 0)
					ratio = -1 * (float)(Math.abs(jNum-iNum)-thres1)/(float)(thres2-thres1);
				else 
					ratio = +1 * (float)(Math.abs(jNum-iNum)-thres1)/(float)(thres2-thres1);
				col = scale.getColor4HotColdScale(ratio, 1.0f);
//					useGeodesic = false;
			}
			else {
				if (jNum-iNum < 0)
					ratio = -1.0f;
				else 
					ratio = +1.0f;
				col = scale.getColor4HotColdScale(ratio, 1.0f);
//					useGeodesic = false;
			}
//				System.out.print(iNum+"_"+jNum+"_"+Math.abs(jNum-iNum)+":"+ratio +"\t");
			g2d.setColor(col);					
							
			if (iNum>jNum && iNum<jNumNB && this.paintCentralResidue){				
				// --- put central residue to position Math.Pi,Math.Pi/2
//					drawNBHSEdge(g2d, lambdaRad, phiRad, Math.PI, Math.PI/2);
//					drawNBHSEdge(g2d, Math.PI, Math.PI/2, lambdaRadNB, phiRadNB);
				
				// --- put central residue to central screen position (always in centre of view) 
				if (!useGeodesic){
					// --- straight line
					drawNBHSDirectEdge(g2d, lambdaRad, phiRad, this.centerOfProjection.getFirst(), this.centerOfProjection.getSecond());
					drawNBHSDirectEdge(g2d, this.centerOfProjection.getFirst(), this.centerOfProjection.getSecond(), lambdaRadNB, phiRadNB);	
//					drawNBHSEdge(g2d, lambdaRad, phiRad, this.centerOfProjection.getFirst(), this.centerOfProjection.getSecond());
//					drawNBHSEdge(g2d, this.centerOfProjection.getFirst(), this.centerOfProjection.getSecond(), lambdaRadNB, phiRadNB);						
				}
				else {
					// --- draw geodesics
					drawNBHSGeodesicEdge(g2d, lambdaRad, phiRad, this.centerOfProjection.getFirst(), this.centerOfProjection.getSecond());
					drawNBHSGeodesicEdge(g2d, this.centerOfProjection.getFirst(), this.centerOfProjection.getSecond(), lambdaRadNB, phiRadNB);					
				}
			}
			else {
				if (!useGeodesic)
					drawNBHSDirectEdge(g2d, lambdaRad, phiRad, lambdaRadNB, phiRadNB);
//					drawNBHSEdge(g2d, lambdaRad, phiRad, lambdaRadNB, phiRadNB);
				else 
					drawNBHSGeodesicEdge(g2d, lambdaRad, phiRad, lambdaRadNB, phiRadNB);
			}	

			nodeID++;
		}
	}
	
	private void drawNBHSDirectEdge(Graphics2D g2d, double lambdaRad, double phiRad, double lambdaRadNB, double phiRadNB){
		// line equation:   y = ((y2-y1)/(x2-x1))*(x-x1) + y1;
		double l, p, lE=0, pE=0, dL = 0.1;
		if (lambdaRad>lambdaRadNB) { // swap
			l = lambdaRad;
			lambdaRad = lambdaRadNB;
			lambdaRadNB = l;
			p = phiRad;
			phiRad = phiRadNB;
			phiRadNB = p;
		}
		l = lambdaRad;
		p = phiRad;
		if (lambdaRadNB-lambdaRad > Math.PI)
			dL *= -1;
		lE = l+dL;
		if (lE<0)
			lE += (2*Math.PI);
		while ( (dL>0 && lE<lambdaRadNB) || (dL<0 && lE>lambdaRadNB)){
			pE = ((phiRadNB-phiRad)/(lambdaRadNB-lambdaRad))*(lE-lambdaRad) + phiRad;
			drawNBHSEdge(g2d, l, p, lE, pE);
			l = lE; p = pE;
			lE = l+dL;
			if (lE<0)
				lE += (2*Math.PI);
		}
		lE = lambdaRadNB;
		pE = ((phiRadNB-phiRad)/(lambdaRadNB-lambdaRad))*(lE-lambdaRad) + phiRad;
		drawNBHSEdge(g2d, l, p, lE, pE);
	}
	
	/**
	  Computes and draw Geodesic between two different points on sphere.
	  @param g2d
	  @param lambdaRad ellipsoidal longitude (in rad:[0:2Pi]) of first point
	  @param phiRad ellipsoidal latitude (in rad:[0:Pi]) of first point
	  @param lambdaRadNB ellipsoidal longitude (in rad[0:2Pi]) of second point
	  @param phiRadNB ellipsoidal latitude (in rad[0:Pi]) of second point
	  */
	private void drawNBHSGeodesicEdge(Graphics2D g2d, double lambdaRad, double phiRad, double lambdaRadNB, double phiRadNB){
		phiRad -= (Math.PI/2);
		phiRadNB -= (Math.PI/2);
		int factor = 1;
		double dLambda = Math.abs(lambdaRadNB-lambdaRad); 
		boolean crossing0meridian = false;
		if (dLambda>Math.PI){
			crossing0meridian = true;
			// shift geodesic
			lambdaRad += Math.PI;
			lambdaRadNB += Math.PI;
			lambdaRadNB -= 2*Math.PI;

			dLambda = Math.abs(lambdaRadNB-lambdaRad);
		}
		if (lambdaRad>lambdaRadNB) {
			double help = phiRad;
			phiRad = phiRadNB;
			phiRadNB = help;
			help = lambdaRad;
			lambdaRad = lambdaRadNB;
			lambdaRadNB = help;
		}
		if (phiRad>phiRadNB)
			factor *= -1;
	    Ellipsoid ell = Ellipsoid.BESSEL;
	    Geodesic gTest = new Geodesic(ell, phiRad, phiRadNB, lambdaRadNB-lambdaRad); // dLambda); 
	    double stepSize = 0.05; //5.72degree //(phi2rad-phi1rad)/steps;
	    int numSteps = (int) (Math.abs(phiRad-phiRadNB)/stepSize);
	    double lS, pS, lE, pE, dL;
	    double dLS, dLE;
	    double corrFacL = 0;

	    pS = phiRad;
	    dLS = gTest.Lambda(pS);
	    pE = phiRadNB;
	    dLE = gTest.Lambda(pE);
	    
	    pS = phiRad;
	    dL = (gTest.Lambda(pS));
	    lS = getLambdaOfGeod(dL, phiRad, phiRadNB, lambdaRad, lambdaRadNB, dLambda, dLS, dLE);
    	if (lS<lambdaRad)
    		corrFacL = lambdaRad-lS;
    	else if (lS>lambdaRad)
    		corrFacL = -(lS-lambdaRad);
	    lS = lambdaRad;
    	pE = pS + (factor*stepSize);
	    dL = (gTest.Lambda(pE));	    
	    lE = getLambdaOfGeod(dL, phiRad, phiRadNB, lambdaRad, lambdaRadNB, dLambda, dLS, dLE);
	    lE+=corrFacL;

	    for (int i=0; i<numSteps; i++){
	    	if ((factor==1 && pE<phiRadNB) || (factor==-1 && pE>phiRadNB)){
	    		if (crossing0meridian){
	    			double lS2 = lS-Math.PI, lE2 = lE-Math.PI; 
	    			if (lS2<0)
	    				lS2 += 2*Math.PI;
	    			if (lE2<0)
	    				lE2 += 2*Math.PI;
	    			drawNBHSEdge(g2d, lS2, pS+(Math.PI/2), lE2, pE+(Math.PI/2));
	    		}
	    		else
	    			drawNBHSEdge(g2d, lS, pS+(Math.PI/2), lE, pE+(Math.PI/2));
		    	pS = pE;
		    	lS = lE;
			    pE = pS + (factor*stepSize);
			    dL = (gTest.Lambda(pE));
			    lE = getLambdaOfGeod(dL, phiRad, phiRadNB, lambdaRad, lambdaRadNB, dLambda, dLS, dLE);
			    lE+=corrFacL;
	    	}
	    }
	    pE = phiRadNB;
	    dL = (gTest.Lambda(pE));
	    lE = getLambdaOfGeod(dL, phiRad, phiRadNB, lambdaRad, lambdaRadNB, dLambda, dLS, dLE);
	    lE+=corrFacL;
    	lE = lambdaRadNB;
    	if (crossing0meridian){
			double lS2 = lS-Math.PI, lE2 = lE-Math.PI; 
			if (lS2<0)
				lS2 += 2*Math.PI;
			if (lE2<0)
				lE2 += 2*Math.PI;
			drawNBHSEdge(g2d, lS2, pS+(Math.PI/2), lE2, pE+(Math.PI/2));
		}
		else
			drawNBHSEdge(g2d, lS, pS+(Math.PI/2), lE, pE+(Math.PI/2));
	}
	
	private double getLambdaOfGeod(double dL, double p1, double p2, double l1, double l2, double dLambda, double dLS, double dLE){		
		double l=0;
		boolean swap = false;
		int factor = 1;
	    double dLGeod = Math.abs(dLS-dLE);
		if (Math.abs(dLambda-dLGeod) > Math.PI)
			swap = true;
		double scale = 1.0;
		if (!swap && dLGeod>0)
			scale = dLambda/dLGeod;
//		dL = scale*dL;
		if (p1<0 && p2<0){
			if (swap)
				factor *= -1;
			if (p1<p2)
				l = l2 - factor*scale*(Math.abs(dL));
			else //if (p1>p2)
				l = l1 + factor*scale*(Math.abs(dL));
		}
		else if (p1<0 || p2<0){			
			if (swap)
				factor *= -1;
			if (p1<p2)
				l = l1 + factor*scale*((dL)); //l = l1 + factor*scale*(Math.abs(dL)+corrFacL);
			else //if (p1>p2)
				l = l2 - factor*scale*((dL));	//l = l2 - factor*scale*(Math.abs(dL)+corrFacL);			
		}
		else { // if (p1>0 && p2>0)			
			if (swap)
				factor *= -1;
			if (p1<p2)
				l = l1 + factor*scale*(Math.abs(dL));
			else //if (p1>p2)
				l = l2 - factor*scale*(Math.abs(dL));			
		}
		return l;
	}

	
	/* Lambda[0:2Pi], Phi[0:Pi]
	 * */
	private boolean wrapEdge(double lambdaRad, double phiRad, double lambdaRadNB, double phiRadNB){
		boolean wrap = false;
		double xPos, xPosNB=0;
		xPos = getScreenPosFromRad(lambdaRad, phiRad).getFirst();
		xPosNB = getScreenPosFromRad(lambdaRadNB, phiRadNB).getFirst();
		
		// -- test if edge should be wrapped
		if (this.mapProjType==cylindricalMapProj && Math.abs(xPosNB-xPos)>this.xDim/2)
			wrap = true;
		else if (this.mapProjType==kavrayskiyMapProj && Math.abs(xPosNB-xPos)>this.xDim/3)
			wrap= true;
		else if ((this.mapProjType==azimuthalMapProj) && (isOnFrontView(lambdaRad, phiRad) != isOnFrontView(lambdaRadNB, phiRadNB)))
			wrap = true;
		
		return wrap;
	}
	
	/* Lambda[0:2Pi], Phi[0:Pi]
	 * */
	private boolean doWrapEdge(double lambdaRad, double lambdaRadNB){
		boolean wrap = false;
		// -- test if edge should be wrapped
		if (Math.abs(lambdaRad-lambdaRadNB)>Math.PI)
			wrap = true;		
		return wrap;
	}
	
	private void drawNBHSEdge(Graphics2D g2d, double lambdaRad, double phiRad, double lambdaRadNB, double phiRadNB){
		Shape line = null;
		double xPos, yPos, xPosNB=0, yPosNB=0;
		// check for correct range
		if (lambdaRad<0)
			lambdaRad+=(2*Math.PI);
		if (lambdaRadNB<0)
			lambdaRadNB+=(2*Math.PI);
		if (lambdaRad>2*Math.PI)
			lambdaRad-=(2*Math.PI);
		if (lambdaRadNB>2*Math.PI)
			lambdaRadNB-=(2*Math.PI);
		
		xPos = getScreenPosFromRad(lambdaRad, phiRad).getFirst();
		yPos = getScreenPosFromRad(lambdaRad, phiRad).getSecond();
		xPosNB = getScreenPosFromRad(lambdaRadNB, phiRadNB).getFirst();
		yPosNB = getScreenPosFromRad(lambdaRadNB, phiRadNB).getSecond();

		boolean wrap = false;
		// -- test if edge should be wrapped
		if (this.mapProjType==cylindricalMapProj){ 
			if(Math.abs(xPosNB-xPos)>this.xDim/2){
				wrap = true;
//				System.out.println("(this.mapProjType==cylindricalMapProj) && Math.abs(xPosNB-xPos)>this.xDim/2");
				if (xPos<xPosNB){
					line = new Line2D.Double(xPos, yPos+this.yBorderThres, xPosNB-this.xDim, yPosNB+this.yBorderThres);		
					g2d.draw(line);
					line = new Line2D.Double(xPos+this.xDim, yPos+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
					g2d.draw(line);
				}
				else{
					line = new Line2D.Double(xPos-this.xDim, yPos+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
					g2d.draw(line);
					line = new Line2D.Double(xPos, yPos+this.yBorderThres, xPosNB+this.xDim, yPosNB+this.yBorderThres);		
					g2d.draw(line);
				}
			}
		}
		else if (this.mapProjType==kavrayskiyMapProj && Math.abs(xPosNB-xPos)>this.xDim/3){
			wrap = true;
//			System.out.println("(this.mapProjType==kavrayskiyMapProj && Math.abs(xPosNB-xPos)>this.xDim/4)");
//		else if (this.mapProjType==kavrayskiyMapProj){
//			if (Math.abs(lambdaRad-lambdaRadNB)<=Math.PI && ((xPosNB<xPos && lambdaRadNB>lambdaRad)||(xPosNB>xPos && lambdaRadNB<lambdaRad)))
//				wrap = true;
//			if (Math.abs(lambdaRad-lambdaRadNB)>Math.PI && ((xPosNB>xPos && lambdaRadNB>lambdaRad)||(xPosNB<xPos && lambdaRadNB<lambdaRad)))
//				wrap = true;
			if (wrap){
				double phiRadM = phiRad + (phiRadNB-phiRad)/2;
				double borderXPos = getScreenPosFromRad(Math.PI-this.origCoordinates.getFirst(), phiRadM).getFirst();
				double borderYPos = getScreenPosFromRad(lambdaRadNB, phiRadM).getSecond();
				if (xPos<xPosNB){
					line = new Line2D.Double(xPos, yPos+this.yBorderThres, borderXPos, borderYPos+this.yBorderThres);		
					g2d.draw(line);
					borderXPos = getScreenPosFromRad((3*Math.PI)-this.origCoordinates.getFirst(), phiRadM).getFirst();
					line = new Line2D.Double(xPosNB, yPosNB+this.yBorderThres, borderXPos, borderYPos+this.yBorderThres);		
					g2d.draw(line);							
				}
				else {
					line = new Line2D.Double(xPosNB, yPosNB+this.yBorderThres, borderXPos, borderYPos+this.yBorderThres);		
					g2d.draw(line);	
					borderXPos = getScreenPosFromRad((3*Math.PI)-this.origCoordinates.getFirst(), phiRadM).getFirst();	
					line = new Line2D.Double(xPos, yPos+this.yBorderThres, borderXPos, borderYPos+this.yBorderThres);		
					g2d.draw(line);					
				}				
			}
//			if (wrap){
//				System.out.println(String.valueOf(Math.abs(lambdaRad-lambdaRadNB))+" "+lambdaRadNB+"<?>"+lambdaRad+" "+xPosNB+"<?>"+xPos);
//			}
		}
		else if (this.mapProjType==azimuthalMapProj){
			if (isOnFrontView(lambdaRad, phiRad) != isOnFrontView(lambdaRadNB, phiRadNB)){
				wrap = true;
				double borderXFront, borderYFront, borderXBack, borderYBack;
				// compute border positions of front and back view
				Pair<Double> intermP = intermediatePointAzim(lambdaRad, phiRad, lambdaRadNB, phiRadNB);
				borderXFront = getScreenPosFromRad(intermP.getFirst(), intermP.getSecond()).getFirst(); //intermP.getFirst();
				borderYFront = getScreenPosFromRad(intermP.getFirst(), intermP.getSecond()).getSecond(); //intermP.getSecond();
				borderXBack = (4*this.rSphere)- borderXFront;
				borderYBack = borderYFront;
				if (isOnFrontView(lambdaRad, phiRad)){
					line = new Line2D.Double(xPos, yPos+this.yBorderThres, borderXFront, borderYFront+this.yBorderThres);		
					g2d.draw(line);
					line = new Line2D.Double(xPosNB, yPosNB+this.yBorderThres, borderXBack, borderYBack+this.yBorderThres);		
					g2d.draw(line);
				}
				else {
					line = new Line2D.Double(xPos, yPos+this.yBorderThres, borderXBack, borderYBack+this.yBorderThres);		
					g2d.draw(line);
					line = new Line2D.Double(xPosNB, yPosNB+this.yBorderThres, borderXFront, borderYFront+this.yBorderThres);		
					g2d.draw(line);
				}				
			}
//			else 
//			{
//				// standard
//				line = new Line2D.Double(xPos, yPos+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
//				g2d.draw(line);
//			}
		}
		
		if(!wrap) 
		{
			line = new Line2D.Double(xPos, yPos+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
			g2d.draw(line);
		}
	}
	
//	private void drawEdge(Graphics2D g2d, double xPos1, double yPos1, double xPos2, double yPos2){
//		Shape line = null;
//		line = new Line2D.Double(xPos1, yPos1, xPos2, yPos2);		
//		g2d.draw(line);
//	}
	
	/* Computes the intermediate point top wrap line between two points, if points are situated on different views.
	 * The values of for lambda and phi are expected to be within ranges [0:2PI] and [0:PI]
	 * */
	private Pair<Double> intermediatePointAzim(double l1, double p1, double l2, double p2){
		Pair<Double> lp;		
		double l, p, lS=0, lE=0, dL = 0.1;
		if (isOnFrontView(l1, p1)){
			lS = l1; p = p1;
			lE = l2;
		}
		else{
			lS = l2; p = p2;
			lE  = l1;
		}
		if (lS>lE)
			dL = -1*dL;
		l=lS;
		lp = new Pair<Double>(l,p);
		while ( ((l>=lS && l<=lE) || (l>=lE && l<=lS)) && 
				distPointsOnSphere(this.centerOfProjection.getFirst(), this.centerOfProjection.getSecond(), l, p)<this.maxDistPoints ){
			lp = new Pair<Double>(l,p);
			l += dL;
			p = ((p2-p1)/(l2-l1))*(l-l1) + p1;
		}
		return lp;
	}
	
	private void drawNBHSTemplateTrace(Graphics2D g2d){
		
//		System.out.println("Coordinates Template Trace:");
		RIGGeometry graphGeom = this.mod.getGraphGeometry();
		g2d.setStroke(new BasicStroke(3));
		drawNBHSTemplateTrace(g2d, graphGeom, this.mod, this.iNum, this.jNum, nbSerials, Color.black);
		if (this.mod2 != null){
			g2d.setStroke(new BasicStroke(1));
			drawNBHSTemplateTrace(g2d, graphGeom, this.mod, this.iNum, this.jNum, nbSerials, mod1Color);
			
			graphGeom = this.mod2.getGraphGeometry();
			g2d.setStroke(new BasicStroke(3));
			drawNBHSTemplateTrace(g2d, graphGeom, this.mod2, this.iNum2, this.jNum2, nbSerials2, Color.black);
			g2d.setStroke(new BasicStroke(1));
			drawNBHSTemplateTrace(g2d, graphGeom, this.mod2, this.iNum2, this.jNum2, nbSerials2, mod2Color);
		}		
		if (this.mod3 != null){
			graphGeom = this.mod3.getGraphGeometry();
			g2d.setStroke(new BasicStroke(3));
			if (this.mod2 == null){
				drawNBHSTemplateTrace(g2d, graphGeom, this.mod3, this.iNum, this.jNum, nbSerials3, Color.black);
				g2d.setStroke(new BasicStroke(1));
				drawNBHSTemplateTrace(g2d, graphGeom, this.mod3, this.iNum, this.jNum, nbSerials3, Color.lightGray);
			}
			else
				drawNBHSTemplateTrace(g2d, graphGeom, this.mod3, this.iNum, this.jNum, nbSerials3, Color.black);			
		}		
		
		g2d.setStroke(defaultBasicStroke);	
	}
	
	private void drawNBHSTemplateTrace(Graphics2D g2d, RIGGeometry graphGeom, Model curMod, int curINum, int curJNum, int[] nbSer, Color col){
		if (graphGeom!=null){
//			HashMap<String,Vector3d> contactCoord = graphGeom.getRotatedCoordOfContacts();
			HashMap<Pair<Integer>,Vector3d> contactCoord = graphGeom.getRotCoordOfContacts();
			
//			System.out.println("contactCoord.size()"+contactCoord.size());
			if (contactCoord!=null && nbSer!=null){
				for(int i=0; i<nbSer.length; i++){
					int jNum = nbSer[i];
//					RIGNode node = curMod.getNodeFromSerial(jNum);
					String resType = curMod.getNodeFromSerial(jNum).getResidueType();
//					String key = String.valueOf(this.iNum)+"_"+String.valueOf(jNum);
					Pair<Integer> key = new Pair<Integer>(curINum, jNum);
//					System.out.print(this.iNum+"_"+jNum+contactCoord.containsKey(key)+"\t");
					Vector3d coord_sph;
//					if (contactCoord.containsKey(new Integer[]{this.iNum,jNum}))	
						coord_sph = contactCoord.get(key); // (r, phi, lambda)
//					else
//						coord_sph = contactCoord.get(new Integer[]{jNum,this.iNum}); // (r, phi, lambda) 
					double lambda = coord_sph.z;
					double phi = coord_sph.y;
					lambda += Math.PI;
					boolean isJRes = false;
					if (jNum==curJNum)
						isJRes = true;
//					// test output
//					System.out.println("1 , "+iNum+" , "+jNum+" , "+phi+" , "+(lambda-Math.PI)+" , "+resType+"-"
//							+AminoAcid.getByThreeLetterCode(resType).getOneLetterCode());
//					if (i==0)
//						System.out.println(jNum+"_"+resType+": "+coord_sph.toString());
															
					// draw node
					drawNBHSNode(g2d, lambda, phi, true, isJRes, resType);
					if (i+1<nbSer.length){
						int jNumNB = nbSer[i+1];
//						key = String.valueOf(this.iNum)+"_"+String.valueOf(jNumNB);
						key = new Pair<Integer>(curINum, jNumNB);
						Vector3d coord_sph_nb;
//						if (contactCoord.containsKey(new Integer[]{this.iNum,jNumNB}))
							coord_sph_nb = contactCoord.get(key); // (r, phi, lambda)
//						else
//							coord_sph_nb = contactCoord.get(new Integer[]{jNumNB,this.iNum}); // (r, phi, lambda)
//						String resTypeNb = this.mod.getNodeFromSerial(jNumNB).getResidueType();
//						System.out.println(jNumNB+"_"+resTypeNb+": "+coord_sph_nb.toString());
							
						double lambdaNB = coord_sph_nb.z;
						double phiNB = coord_sph_nb.y;
						lambdaNB += Math.PI;
						// draw edge
						g2d.setColor(col);
						if (this.paintCentralResidue && nbSer[i]<curINum && nbSer[i+1]>curINum){
							// include central residue						
							// --- draw geodesics
							drawNBHSGeodesicEdge(g2d, lambda, phi, this.centerOfProjection.getFirst(), this.centerOfProjection.getSecond());
							drawNBHSGeodesicEdge(g2d, this.centerOfProjection.getFirst(), this.centerOfProjection.getSecond(), lambdaNB, phiNB);	
						}
						else {
							// draw edge directly
							drawNBHSGeodesicEdge(g2d, lambda, phi, lambdaNB, phiNB);
						}
					}
				}			
			}		
		}
	}
	
	private void drawNBHSTraces(Graphics2D g2d){
		int gID, iNum, jResID;
		float[] node, nbNode;
		int lineID = 0;
		int nodeID = 0;

		int nbhsIndexC = 0;
		boolean specialRes = false;
		boolean isJRes = false;
		
//		g2d.setStroke(new BasicStroke(4));
				
		for(int i=0; i<this.nbhsNodes.size(); i++){
			
			node = (float[]) this.nbhsNodes.get(i);
			gID = (int) node[0];
			iNum = (int) node[1];
			jResID = (int) node[5];
			
			// ---- check if jResType is element of nbhstring
			specialRes = false;
			isJRes = false;
			if (i>0){
				nbNode = this.nbhsNodes.get(i-1);
				if (nbNode[0]!=gID || nbNode[1]!=iNum)
					nbhsIndexC = 0;				
			}
			// simple differentiation
			if (this.nbhString.contains(String.valueOf(aas[jResID]))){
				specialRes = true;
			}
			// ordering is of importance
			if (nbhsIndexC > 0 && aas[jResID]==this.nbhsRes[nbhsIndexC-1])
				specialRes = true;
			if (this.nbhsRes!=null && aas[jResID] == this.nbhsRes[nbhsIndexC]){
				specialRes = true;
				nbhsIndexC++;
			}	
			
			// --- check if jResType==this.jRes
			if (aas[jResID] == this.jRes)
				isJRes = true;
			
			// ---- draw geometric object for each residue
			drawNBHSNode(g2d, specialRes, isJRes, node, i);				
			
			// jump over empty traces
			while (this.numNodesPerLine[lineID]==0){
				lineID++;
				nodeID = 0;
			}
			int j=i+1;
			
			if (j<this.nbhsNodes.size()){
				nbNode = (float[]) this.nbhsNodes.get(j);
				if (gID==nbNode[0] && iNum==nbNode[1]){
					drawNBHSEdges(g2d, node, nodeID, lineID, j);					
				}
				else {
					lineID++;
					nodeID = 0;
					g2d.setStroke(defaultBasicStroke);
				}
			}	
			else {
				lineID++;
				nodeID = 0;	
			}		
		}
		
	}
	
	/*  Draw an ellipse that is surrounded by the trapezium, given through the points
	 *  xCoord and yCoord. Coordinates should be in clockwise order starting with the upper left corner.
	 *  xCoord and yCoord should both be of length=4.
	 * */
	private void drawVarEllipse(Graphics2D g2d, double[] xCoord, double[] yCoord){
		if (xCoord.length==yCoord.length && xCoord.length==4){
			double x1 = xCoord[0], x2 = xCoord[1], x3 = xCoord[2], x4 = xCoord[3];
			double y1 = yCoord[0], y2 = yCoord[1], y3 = yCoord[2], y4 = yCoord[3];
			// intermediate points
			double x12 = (x1+x2)/2, x23 = (x2+x3)/2, x34 = (x3+x4)/2, x41 = (x4+x1)/2;
			double y12 = (y1+y2)/2, y23 = (y2+y3)/2, y34 = (y3+y4)/2, y41 = (y4+y1)/2;
			
			g2d.draw(new Arc2D.Double(x41, y12, 2*Math.abs(x12-x41), 2*Math.abs(y12-y41), 90, 90, Arc2D.OPEN));
			g2d.draw(new Arc2D.Double(x12-Math.abs(x12-x23), y12, 2*Math.abs(x12-x23), 2*Math.abs(y12-y23), 0, 90, Arc2D.OPEN));
			g2d.draw(new Arc2D.Double(x34-Math.abs(x34-x23), y23-Math.abs(y34-y23), 2*Math.abs(x23-x34), 2*Math.abs(y23-y34), 270, 90, Arc2D.OPEN));
			g2d.draw(new Arc2D.Double(x41, y41-Math.abs(y34-y41), 2*Math.abs(x34-x41), 2*Math.abs(y34-y41), 180, 90, Arc2D.OPEN));
		}
	}
	
	private void drawArrowTriangle(Graphics2D g2d, double lS, double pS, double lE, double pE, double w){
		pS = Math.PI-pS;
		pE = Math.PI-pE;
		GeneralPath triangle;
		double m = -(lE-lS)/(pE-pS); // m = -(lE-lS)/(pS-pE);
		double n = pS - (m*lS); //
		double alpha = Math.atan(m); //Math.atan(Math.abs(m));
		double dL = (w/2)*Math.cos(alpha); //dL=0.1;
		double l1, p1, l2, p2;
		double x1, y1, x2, y2, x3, y3;
		pS = Math.PI-pS;
		pE = Math.PI-pE;
		l1 = lS + dL;
		p1 = Math.PI-(m*l1 + n);
		l2 = lS - dL;
		p2 = Math.PI-(m*l2 + n);
		
		x1 = getScreenPosFromRad(l1, p1).getFirst();
		y1 = getScreenPosFromRad(l1, p1).getSecond();
//		if (this.mapProjType==azimuthalMapProj && !isOnFrontView(l1, p1))		
//			x1 = (4*this.rSphere)-x1;
		x2 = getScreenPosFromRad(l2, p2).getFirst();
		y2 = getScreenPosFromRad(l2, p2).getSecond();
//		if (this.mapProjType==azimuthalMapProj && !isOnFrontView(l2, p2))		
//			x2 = (4*this.rSphere)-x2;
		x3 = getScreenPosFromRad(lE, pE).getFirst();
		y3 = getScreenPosFromRad(lE, pE).getSecond();
//		if (this.mapProjType==azimuthalMapProj && !isOnFrontView(lE, pE))		
//			x3 = (4*this.rSphere)-x3;
		
		boolean paint = true;
		if (this.mapProjType!=azimuthalMapProj && Math.abs(x1-x3)>(this.g2dSize.width/3))
			paint = false;
		if (this.mapProjType==azimuthalMapProj) {
			if (isOnFrontView(lS, pS) != isOnFrontView(lE, pE))
				paint = false;
			if (isOnFrontView(lS, pS)!=isOnFrontView(lS+0.1, pS) || isOnFrontView(lS, pS)!=isOnFrontView(lS-0.1, pS))
				paint = false;
			if (isOnFrontView(lE, pE)!=isOnFrontView(lE+0.1, pE) || isOnFrontView(lE, pE)!=isOnFrontView(lE-0.1, pE))
				paint = false;
		}
		if (paint){
			triangle = triangleShape(x1, y1, x2, y2, x3, y3);
			g2d.draw(triangle);
			g2d.fill(triangle);			
		}
	}
	
	private void drawEdgeClusterDir(Graphics2D g2d){
		double averLS, averLE, averPS, averPE;
		double factor = 0.3;
		for (int i=0; i<this.clusterAverDirec.size(); i++)
//		int i=3;
		{		
			int nbCID = this.nbCluster[i];
			// clusterProp; // [][0]:minL, [][1]:averageL, [][2]:maxL, [][3]:minP, [][4]:averP, [][5]:maxP
			averLS = this.clusterProp[i][1] + Math.PI;
			averPS = this.clusterProp[i][4];
			double[] dir = this.clusterAverDirec.get(i);
			if (nbCID>0 || nbCID<0){ // successor exists 
				nbCID = Math.abs(nbCID);
				averLE = this.clusterProp[nbCID-1][1] + Math.PI;
				averPE = this.clusterProp[nbCID-1][4];
				if (doWrapEdge(averLS, averLE))
//				if (wrapEdge(averLS, averPS, averLE, averPE))
					averLE = averLS -factor*dir[0];
				else
					averLE = averLS + factor*dir[0];				
			}
			else
				averLE = averLS + factor*dir[0];
			averPE = averPS + factor*dir[1];
			g2d.setColor(this.arrowColor);
			drawArrowTriangle(g2d, averLS, averPS, averLE, averPE, 0.15);	
			
			if (this.clusterAverDirec.size() == this.mostFrequentResT4Clusters.length){
				double l = (averLS+averLE)/2;
				double p = (averPS+averPE)/2;
				Vector<Integer> mostFrequentRes = this.mostFrequentResT4Clusters[i];
				String residue = "";
				int nrOfRes = 0;
				for (int resID:mostFrequentRes){
					residue += (aas[resID]+",");
					nrOfRes++;
				}
				residue = residue.substring(0, residue.length()-1);
//				averPE = Math.PI-averPE;
				double x = getScreenPosFromRad(l, p).getFirst();
				double y = getScreenPosFromRad(l, p).getSecond();
				
				g2d.setColor(new Color(255, 255, 255, 150));
			    Shape rect = new Rectangle2D.Double (x, y+this.yBorderThres-20, (double)(nrOfRes*16), (double)(20));
			    g2d.draw(rect);
			    g2d.fill(rect);
//				g2d.drawRect((int)x, (int)y, nrOfRes*5, 10);
				
				Font f = new Font("Dialog", Font.PLAIN, 16);
				g2d.setFont(f);
				g2d.setColor(Color.black);
				
				g2d.drawString(residue, (float)(x), (float)(y+this.yBorderThres));				
			}
			
		}
	}
	
	@SuppressWarnings("unused")
	private void drawEdgeCluster(Graphics2D g2d){
		double averLS, averLE, averPS, averPE;
		double lE, pE;
		// draw arrow for each cluster that has successor-cluster
		for (int i=0; i<this.nbCluster.length; i++){
			int nbCID = this.nbCluster[i];
			if (nbCID>0 || nbCID<0){ // successor exists 
				if (nbCID<0){ // sucessor=central residue
					averLE = this.centerOfProjection.getFirst();
					averPE = this.centerOfProjection.getSecond();					
				}
				else {	
					averLE = this.clusterProp[nbCID-1][1] + Math.PI;
					averPE = this.clusterProp[nbCID-1][4];	
				}
				// clusterProp; // [][0]:minL, [][1]:averageL, [][2]:maxL, [][3]:minP, [][4]:averP, [][5]:maxP
				averLS = this.clusterProp[i][1] + Math.PI;
				averPS = this.clusterProp[i][4];
				if (wrapEdge(averLS, averPS, averLE, averPE)){
					lE = (averLS+averLE)/2 + Math.PI;
					if (lE>2*Math.PI)
						lE -= 2*Math.PI;
					pE = (averPS+averPE)/2;
				}
				else{
					lE = (averLS+averLE)/2;
					pE = (averPS+averPE)/2;
				}				
				g2d.setColor(this.arrowColor);
				drawArrowTriangle(g2d, averLS, averPS, lE, pE, 0.05);
				
				if (nbCID<0){ // sucessor=central residue
					averLS = this.centerOfProjection.getFirst();
					averPS = this.centerOfProjection.getSecond();	
					nbCID = Math.abs(nbCID);
					averLE = this.clusterProp[nbCID-1][1] + Math.PI;
					averPE = this.clusterProp[nbCID-1][4];		
					lE = (averLS+averLE)/2;
					pE = (averPS+averPE)/2;
					drawArrowTriangle(g2d, averLS, averPS, lE, pE, 0.05);
				}
			}
		}
	}
	
	private void drawClusterBoundaries(Graphics2D g2d){
		double minL, maxL, minP, maxP;
		double x1, x2, x3, x4, y1, y2, y3, y4;
		double[] xCoord, yCoord;
		double deltaBorder = 0.05;
		g2d.setColor(selAngleRangeColor);
		g2d.setStroke(selRangeStroke);
		for (int i=0; i<this.clusterProp.length; i++){
			minL = this.clusterProp[i][0] + Math.PI - deltaBorder;
			maxL = this.clusterProp[i][2] + Math.PI + deltaBorder;
			minP = this.clusterProp[i][3] - deltaBorder;
			maxP = this.clusterProp[i][5] + deltaBorder;
			
			x1 = getScreenPosFromRad(minL, minP).getFirst();
			y1 = getScreenPosFromRad(minL, minP).getSecond();
//			if (this.mapProjType==azimuthalMapProj && !isOnFrontView(minL, minP))		
//				x1 = (4*this.rSphere)-x1;
			x2 = getScreenPosFromRad(maxL, minP).getFirst();
			y2 = getScreenPosFromRad(maxL, minP).getSecond();
//			if (this.mapProjType==azimuthalMapProj && !isOnFrontView(maxL, minP))		
//				x2 = (4*this.rSphere)-x2;
			x3 = getScreenPosFromRad(maxL, maxP).getFirst();
			y3 = getScreenPosFromRad(maxL, maxP).getSecond();
//			if (this.mapProjType==azimuthalMapProj && !isOnFrontView(maxL, maxP))		
//				x3 = (4*this.rSphere)-x3;
			x4 = getScreenPosFromRad(minL, maxP).getFirst();
			y4 = getScreenPosFromRad(minL, maxP).getSecond();
//			if (this.mapProjType==azimuthalMapProj && !isOnFrontView(minL, maxP))		
//				x4 = (4*this.rSphere)-x4;
			xCoord = new double[]{x1,x2,x3,x4};
			yCoord = new double[]{y1,y2,y3,y4};
			
			boolean paint = true;
			if (this.mapProjType!=azimuthalMapProj && x1>x2)
				paint = false;
//			if (this.mapProjType==azimuthalMapProj && (isOnFrontView(minL, minP) != isOnFrontView(maxL, maxP)))
//				paint = false;
			if (this.mapProjType==azimuthalMapProj) {
				paint = false;
//				if (isOnFrontView(minL, minP) != isOnFrontView(maxL, maxP))
//					paint = false;
//				if (isOnFrontView(minL, minP)!=isOnFrontView(minL+0.4, minL) || isOnFrontView(minL, minP)!=isOnFrontView(minL-0.4, minP))
//					paint = false;
//				if (isOnFrontView(maxL, maxP)!=isOnFrontView(maxL+0.4, maxP) || isOnFrontView(maxL, maxP)!=isOnFrontView(maxL-0.4, maxP))
//					paint = false;
			}
			if (paint)
				drawVarEllipse(g2d, xCoord, yCoord);
		}
		g2d.setStroke(defaultBasicStroke);
	}
	
	@SuppressWarnings("unused")
	private void drawEdgeCluster1(Graphics2D g2d){
		double clusterCentreX, clusterCentreY;
		double xPosInc, xPosOut, yPosInc, yPosOut;
		double lCentre, lInc, lOut, pCentre, pInc, pOut;
		double averIncSlope, averOutSlope;
		double dX = 0.5;
		double n;
		double thresSlopeVar = 10;
		double arrowWidth = 5;
		float[] node, nbNode;
		
		for (int i=0; i<this.clusterProp.length; i++){
			int cID = i+1;
			lCentre = this.clusterProp[i][1] + Math.PI;
			pCentre = this.clusterProp[i][4];
			
			// take following element (node) within sequence of first node of cluster to determine direction of arrow
			// i.e. if xPosInc east or west of cluster centre
			int cnt = 0;
			int nodeID = this.clusters[cID].get(cnt);
			while (!(nodeID<this.nbhsNodes.size()-1)){
				cnt++;
				nodeID = this.clusters[cID].get(0);
			}
			node = (float[]) this.nbhsNodes.get(nodeID);
			nbNode = (float[]) this.nbhsNodes.get(nodeID+1);
			if (nbNode[4]+Math.PI < node[4]+Math.PI) // east of startNode
				dX *= -1;
			
			clusterCentreX = getScreenPosFromRad(lCentre, pCentre).getFirst();
			clusterCentreY = getScreenPosFromRad(lCentre, pCentre).getSecond();
//			if (this.mapProjType==azimuthalMapProj && !isOnFrontView(clusterCentreX, clusterCentreY))		
//				clusterCentreX = (4*this.rSphere)-clusterCentreX;
			
			averIncSlope = this.clusterDirProp[i][1];
			averOutSlope = this.clusterDirProp[i][4];
			if (Math.abs(this.clusterDirProp[i][2]-this.clusterDirProp[i][0])<thresSlopeVar){
				n = pCentre - (averIncSlope*lCentre);
				lInc = lCentre - dX;
				pInc = (averIncSlope*lInc) + n;
				xPosInc = getScreenPosFromRad(lInc, pInc).getFirst();
				yPosInc = getScreenPosFromRad(lInc, pInc).getSecond();
				g2d.setColor(Color.green);
				drawArrow(g2d, xPosInc,yPosInc,clusterCentreX,clusterCentreY,arrowWidth);
			}
			if (Math.abs(this.clusterDirProp[i][5]-this.clusterDirProp[i][3])<thresSlopeVar){
				n = pCentre - (averOutSlope*lCentre);
				lOut = lCentre + dX;
				pOut = (averOutSlope*lOut) + n;
				xPosOut = getScreenPosFromRad(lOut, pOut).getFirst();
				yPosOut = getScreenPosFromRad(lOut, pOut).getSecond();
				g2d.setColor(Color.yellow);
				drawArrow(g2d, clusterCentreX,clusterCentreY,xPosOut,yPosOut,arrowWidth);

				averOutSlope = this.clusterDirProp[i][3];
				n = pCentre - (averOutSlope*lCentre);
				lOut = lCentre + dX;
				pOut = (averOutSlope*lOut) + n;
				xPosOut = getScreenPosFromRad(lOut, pOut).getFirst();
				yPosOut = getScreenPosFromRad(lOut, pOut).getSecond();
				g2d.setColor(Color.green);
				drawArrow(g2d, clusterCentreX,clusterCentreY,xPosOut,yPosOut,arrowWidth);
				averOutSlope = this.clusterDirProp[i][5];
				n = pCentre - (averOutSlope*lCentre);
				lOut = lCentre + dX;
				pOut = (averOutSlope*lOut) + n;
				xPosOut = getScreenPosFromRad(lOut, pOut).getFirst();
				yPosOut = getScreenPosFromRad(lOut, pOut).getSecond();
				g2d.setColor(Color.green);
				drawArrow(g2d, clusterCentreX,clusterCentreY,xPosOut,yPosOut,arrowWidth);
			}
		}
	}
	
	private void drawArrow(Graphics2D g2d, double xS, double yS, double xE, double yE, double width){
		Shape line, circle;
		double radius = width;
		line = new Line2D.Double(xS,yS,xE,yE);
		g2d.draw(line);
		circle = new Ellipse2D.Double( xE-radius, yE-radius+this.yBorderThres,2*radius, 2*radius);
		g2d.draw(circle);
		g2d.fill(circle);
		
//		xS=xS-2*dL; xE=xE-2*dL;
//		for (int i=0; i<5; i++){
//			line = new Line2D.Double(xS,yS,xE,yE);
//			g2d.draw(line);
//			xS=xS+dL; xE=xE+dL;
//		}
//		xS=xS-3*dL; xE=xE-3*dL;
		
		
	}
	
	private void drawLongitude(Graphics2D g2d, double lambdaRad, double phiRad){
		double xS, xE, yS, yE;
		Shape line;
		xS = getScreenPosFromRad(lambdaRad, phiRad).getFirst();
		xE = getScreenPosFromRad(lambdaRad, phiRad+deltaRad).getFirst();
		yS = getScreenPosFromRad(lambdaRad, phiRad).getSecond();
		yE = getScreenPosFromRad(lambdaRad, phiRad+deltaRad).getSecond();
		if (this.mapProjType==azimuthalMapProj){
			if (isOnFrontView(lambdaRad, phiRad)==isOnFrontView(lambdaRad, phiRad+deltaRad)){
				if (!isOnFrontView(lambdaRad, phiRad) || !isOnFrontView(lambdaRad, phiRad+deltaRad)){
					if (xS<2*this.rSphere) //if (isOnFrontView(lambdaRad, phiRad))
						xS = (4*this.rSphere)-xS;
					if (xE<2*this.rSphere) //if (isOnFrontView(lambdaRad, phiRad+deltaRad))
						xE = (4*this.rSphere)-xE;
				}
//				if (!isOnFrontView(lambdaRad, phiRad)){
//					xS = (4*this.rSphere)-xS;
//					xE = (4*this.rSphere)-xE;
//				}
				line = new Line2D.Double(xS,yS,xE,yE);
				g2d.draw(line);
			}
		}
		else {
			line = new Line2D.Double(xS,yS,xE,yE);
			g2d.draw(line);
		}
	}
	
	private void drawLatitude(Graphics2D g2d, double lambdaRad, double phiRad){
		double xS, xE, yS, yE;
		Shape line;
		xS = getScreenPosFromRad(lambdaRad, phiRad).getFirst();
		xE = getScreenPosFromRad(lambdaRad+deltaRad, phiRad).getFirst();
		yS = getScreenPosFromRad(lambdaRad, phiRad).getSecond();
		yE = getScreenPosFromRad(lambdaRad+deltaRad, phiRad).getSecond();
		if (this.mapProjType==azimuthalMapProj && (isOnFrontView(lambdaRad, phiRad)==isOnFrontView(lambdaRad+deltaRad, phiRad))){
			if (!isOnFrontView(lambdaRad, phiRad) || !isOnFrontView(lambdaRad+deltaRad, phiRad)){
				if (xS<2*this.rSphere) //if (isOnFrontView(lambdaRad, phiRad))
					xS = (4*this.rSphere)-xS;
				if (xE<2*this.rSphere) //if (isOnFrontView(lambdaRad+deltaRad, phiRad))
					xE = (4*this.rSphere)-xE;
			}
//			if (!isOnFrontView(lambdaRad, phiRad)){
//				xS = (4*this.rSphere)-xS;
//				xE = (4*this.rSphere)-xE;
//			}
			line = new Line2D.Double(xS,yS,xE,yE);
			g2d.draw(line);
		}
	}
	
	@SuppressWarnings("unused")
	private void drawThickVerticalLine(Graphics2D g2d, double xS, double yS, double xE, double yE){
		Shape line;
		line = new Line2D.Double(xS,yS,xE,yE);
		g2d.draw(line);
		xS=xS-dL; xE=xE-dL;
		line = new Line2D.Double(xS,yS,xE,yE);
		g2d.draw(line);	
	}
	
	@SuppressWarnings("unused")
	private void drawThickHorizontalLine(Graphics2D g2d, double xS, double yS, double xE, double yE){
		Shape line;
		line = new Line2D.Double(xS,yS,xE,yE);
		g2d.draw(line);
		yS = yS-dL; yE = yE-dL;
		line = new Line2D.Double(xS,yS,xE,yE);
		g2d.draw(line);
	}
	
	private void drawLongitudes(Graphics2D g2d){
		// Laengengrad
		g2d.setColor(this.longitudeColor);
		g2d.setStroke(longLatStroke);
		double xS, xE, yS, yE;
		double phiRad, lambdaRad;
		Shape line;
		
		if (this.mapProjType==kavrayskiyMapProj || this.mapProjType==azimuthalMapProj){			
			for(int i=0;i<ratios.length;i++){
				phiRad = (i*deltaRad);
				lambdaRad = 0.0;
				drawLongitude(g2d, lambdaRad, phiRad);					
				lambdaRad = Math.PI/2;
				drawLongitude(g2d, lambdaRad, phiRad);					
				lambdaRad = Math.PI;
				drawLongitude(g2d, lambdaRad, phiRad);
				lambdaRad = 3*Math.PI/2;
				drawLongitude(g2d, lambdaRad, phiRad);				
				lambdaRad = 2*Math.PI;
				drawLongitude(g2d, lambdaRad, phiRad);
			}
		}
		else if (this.mapProjType==cylindricalMapProj){
			yS = ((0.0) *this.voxelsize) +this.border;
			yE = (Math.PI *this.voxelsize) +this.border;
			
			xS = (translateXCoordRespective2Orig(0.0) *this.voxelsize) +this.border;  xE=xS;
			line = new Line2D.Double(xS,yS,xE,yE);
			g2d.draw(line);
			xS = (translateXCoordRespective2Orig(Math.PI/2) *this.voxelsize) +this.border;  xE=xS;
			line = new Line2D.Double(xS,yS,xE,yE);
			g2d.draw(line);
			xS = (translateXCoordRespective2Orig(Math.PI) *this.voxelsize) +this.border;  xE=xS;
			line = new Line2D.Double(xS,yS,xE,yE);
			g2d.draw(line);
			xS = (translateXCoordRespective2Orig(3*Math.PI/2) *this.voxelsize) +this.border;  xE=xS;
			line = new Line2D.Double(xS,yS,xE,yE);
			g2d.draw(line);
			xS = (translateXCoordRespective2Orig(2*Math.PI) *this.voxelsize) +this.border;  xE=xS;
			line = new Line2D.Double(xS,yS,xE,yE);
			g2d.draw(line);
		}

		g2d.setStroke(defaultBasicStroke);		
	}
	
	private void drawLatitudes(Graphics2D g2d){
		// Breitengrad
		g2d.setColor(this.latitudeColor);
		g2d.setStroke(longLatStroke);
		double xS, xE, yS, yE; //yE = yS;
		Shape line;
		
		if (this.mapProjType==azimuthalMapProj){
			double phiRad, lambdaRad;			
			for(int j=0;j<ratios[0].length;j++){
				lambdaRad = (j*deltaRad);
				
				phiRad = Math.PI/4;
				drawLatitude(g2d, lambdaRad, phiRad);
				phiRad = Math.PI/2;
				drawLatitude(g2d, lambdaRad, phiRad);
				phiRad = 3*Math.PI/4;
				drawLatitude(g2d, lambdaRad, phiRad);
			}
		}
		else if (this.mapProjType==kavrayskiyMapProj || this.mapProjType==cylindricalMapProj){		
			xS = ((0.0f) *this.voxelsize) +this.border;
			xE = (((2*Math.PI)) *this.voxelsize) +this.border;
			yS = (translateYCoordRespective2Orig(Math.PI/2) *this.voxelsize) +this.border; yE = yS;
			line = new Line2D.Double(xS,yS,xE,yE);
			g2d.draw(line);
			yS = (translateYCoordRespective2Orig(Math.PI/4) *this.voxelsize) +this.border; yE = yS;
			line = new Line2D.Double(xS,yS,xE,yE);
			g2d.draw(line);
			yS = (translateYCoordRespective2Orig(3*Math.PI/4) *this.voxelsize) +this.border; yE = yS;
			line = new Line2D.Double(xS,yS,xE,yE);
			g2d.draw(line);	
		}
		g2d.setStroke(defaultBasicStroke);
	}
	
	private void drawLongLatCentre(Graphics2D g2d){
		double bcenterx=0, bcentery=0;
		if (this.mapProjType==kavrayskiyMapProj){
			bcenterx = getScreenPosFromRad(Math.PI, Math.PI/2).getFirst();
			bcentery = getScreenPosFromRad(Math.PI, Math.PI/2).getSecond();
		}
		else if (this.mapProjType==cylindricalMapProj){
			bcenterx = (this.origCoordinates.getFirst() * this.voxelsize) + this.border;
			bcentery = (this.origCoordinates.getSecond() * this.voxelsize) + this.border;
		}
		else if (this.mapProjType==azimuthalMapProj){
			bcenterx = getScreenPosFromRad(Math.PI, Math.PI/2).getFirst();
			bcentery = getScreenPosFromRad(Math.PI, Math.PI/2).getSecond();
//			if (distPointsOnSphere(Math.PI, Math.PI/2, this.centerOfProjection.getFirst(), this.centerOfProjection.getSecond()) > this.maxDistPoints)
//			if (!isOnFrontView(Math.PI, Math.PI/2))	
//				bcenterx = (4*this.rSphere)-bcenterx;
		}
		g2d.setColor(longitudeColor);
		g2d.setStroke(longLatStroke);
		Shape circle = new Ellipse2D.Double(bcenterx-30, bcentery-30, 60, 60);
		g2d.draw(circle);
		g2d.setStroke(defaultBasicStroke);
	}
	
	private void drawCrosshair(Graphics2D g2d){
		// only in case of range selection we draw a diagonal cursor
		// drawing the cross-hair
		g2d.setColor(crosshairColor);
		g2d.setStroke(crosshairStroke);
		g2d.drawLine(mousePos.x, 0, mousePos.x, g2dSize.height);
		g2d.drawLine(0, mousePos.y, g2dSize.width, mousePos.y);	
		g2d.setStroke(defaultBasicStroke);
	}

//	private void drawRulerCrosshair(Graphics2D g2d) {
//		int x1,x2,y1,y2;
//		g2d.setColor(crosshairColor);
//		if(currentRulerMouseLocation == ResidueRuler.TOP || currentRulerMouseLocation == ResidueRuler.BOTTOM) {
//			x1 = currentRulerMousePos;
//			x2 = currentRulerMousePos;
//			y1 = 0;
//			y2 = outputSize;
//		} else {
//			x1 = 0;
//			x2 = outputSize;
//			y1 = currentRulerMousePos;
//			y2 = currentRulerMousePos;			
//		}
//		g2d.drawLine(x1, y1, x2, y2);
//	}
	
	
	private void drawOccupiedAngleRanges(Graphics2D g2d){
		Shape shape;
		
		double xPos1, yPos1, xPos2, yPos2, xPos3, yPos3, xPos4, yPos4;
		double xPosUL=0, xPosLL=0, xPosUR=getSize().getWidth(), xPosLR=getSize().getWidth();
		double yPosUL=0, yPosLL=0, yPosUR=getSize().getWidth(), yPosLR=getSize().getWidth();
		Iterator<Pair<Double>> itrP = this.lambdaRanges.iterator();
		Iterator<Pair<Double>> itrT = this.phiRanges.iterator();
		Iterator<Pair<Integer>> itrC = this.selContacts.iterator(); // holds residue numbers for each contact Pair(iNum, jNum)
		while (itrP.hasNext()){
			Pair<Double> lambda = (Pair<Double>) itrP.next();
			Pair<Double> phi = (Pair<Double>) itrT.next();
			Pair<Integer> contact = itrC.next();
			
			g2d.setColor(selAngleRangeColor);
			// if contact complies currently selected contact
			int iNumber = contact.getFirst();
			int jNumber = contact.getSecond();
			if (iNumber==this.iNum && jNumber==this.jNum)
				g2d.setColor(actSelAngleRangeColor);
			g2d.setStroke(new BasicStroke(2));
			
			double xS = lambda.getFirst();
			double xE = lambda.getSecond();
			double yS = phi.getFirst();
			double yE = phi.getSecond();
			xPos1 = getScreenPosFromRad(xS, yS).getFirst();
			yPos1 = getScreenPosFromRad(xS, yS).getSecond();
			xPos2 = getScreenPosFromRad(xE, yS).getFirst();
			yPos2 = getScreenPosFromRad(xE, yS).getSecond();
			xPos3 = getScreenPosFromRad(xE, yE).getFirst();
			yPos3 = getScreenPosFromRad(xE, yE).getSecond();
			xPos4 = getScreenPosFromRad(xS, yE).getFirst();
			yPos4 = getScreenPosFromRad(xS, yE).getSecond();
			
			if (this.mapProjType==azimuthalMapProj){
				
				if (isOnFrontView(xS, yS) != isOnFrontView(xE, yE)){
					// compute border positions of front and back view
					Pair<Double> intermP = intermediatePointAzim(xS, yS, xE, yS);
					xPosUL = getScreenPosFromRad(intermP.getFirst(), intermP.getSecond()).getFirst(); //intermP.getFirst();
					yPosUL = getScreenPosFromRad(intermP.getFirst(), intermP.getSecond()).getSecond(); //intermP.getSecond();
					xPosUR = (4*this.rSphere)-xPosUL;
					yPosUR = yPosUL;
					intermP = intermediatePointAzim(xS, yE, xE, yE);
					xPosLL = getScreenPosFromRad(intermP.getFirst(), intermP.getSecond()).getFirst(); //intermP.getFirst();
					yPosLL = getScreenPosFromRad(intermP.getFirst(), intermP.getSecond()).getSecond(); //intermP.getSecond();
					xPosLR = (4*this.rSphere)-xPosLL;
					yPosLR = yPosLL;
					
					// test for correct intermediate points
					float radius = 3.f;
					Shape circle;
					circle = new Ellipse2D.Double( xPosUL-radius, yPosUL-radius,2*radius, 2*radius);
					g2d.draw(circle);
					circle = new Ellipse2D.Double( xPosLL-radius, yPosLL-radius,2*radius, 2*radius);
					g2d.draw(circle);
					circle = new Ellipse2D.Double( xPosUR-radius, yPosUR-radius,2*radius, 2*radius);
					g2d.draw(circle);
					circle = new Ellipse2D.Double( xPosLR-radius, yPosLR-radius,2*radius, 2*radius);
					g2d.draw(circle);
					
					double[] x;
					double[] y; 
					if (isOnFrontView(xS, yS)){
						x = new double[]{xPosUL, xPos1, xPos4, xPosLL};
						y = new double[]{yPosUL, yPos1, yPos4, yPosLL}; 
						shape = pathLine(x, y);
						g2d.draw(shape);						
						x = new double[]{xPosUR, xPos2, xPos3, xPosLR};
						y = new double[]{yPosUR, yPos2, yPos3, yPosLR}; 
						shape = pathLine(x, y);
						g2d.draw(shape);
					}
					else {
						x = new double[]{xPosUL, xPos2, xPos3, xPosLL};
						y = new double[]{yPosUL, yPos2, yPos3, yPosLL}; 
						shape = pathLine(x, y);
						g2d.draw(shape);						
						x = new double[]{xPosUR, xPos1, xPos4, xPosLR};
						y = new double[]{yPosUR, yPos1, yPos4, yPosLR}; 
						shape = pathLine(x, y);
						g2d.draw(shape);
					}				
				}
				else {
					shape = trapezium(xPos1, yPos1, xPos2, yPos2, xPos3, yPos3, xPos4, yPos4);			
					g2d.draw(shape);
				}
			}	
			else if (this.mapProjType!=azimuthalMapProj && xPos1>xPos2){
//				System.out.println("Umklappen!!!!");
				xPosUL = getScreenPosFromRad(Math.PI-this.origCoordinates.getFirst(), yS).getFirst();
				xPosLL = getScreenPosFromRad(Math.PI-this.origCoordinates.getFirst(), yE).getFirst();
				xPosUR = getScreenPosFromRad(Math.PI-this.origCoordinates.getFirst()+2*Math.PI, yS).getFirst();
				xPosLR = getScreenPosFromRad(Math.PI-this.origCoordinates.getFirst()+2*Math.PI, yE).getFirst();
				
				// test for correct intermediate points
				float radius = 3.f;
				Shape circle;
				circle = new Ellipse2D.Double( xPosUL-radius, yPos1-radius,2*radius, 2*radius);
				g2d.draw(circle);
				circle = new Ellipse2D.Double( xPosLL-radius, yPos4-radius,2*radius, 2*radius);
				g2d.draw(circle);
				circle = new Ellipse2D.Double( xPosUR-radius, yPos2-radius,2*radius, 2*radius);
				g2d.draw(circle);
				circle = new Ellipse2D.Double( xPosLR-radius, yPos3-radius,2*radius, 2*radius);
				g2d.draw(circle);
				
				double[] x = new double[]{xPosUR+1, xPos1, xPos4, xPosLR+1};
				double[] y = new double[]{yPos2, yPos1, yPos4, yPos3}; 
				shape = pathLine(x, y);
				g2d.draw(shape);
				x = new double[]{xPosUL-1, xPos2, xPos3, xPosLL-1};
				y = new double[]{yPos1, yPos2, yPos3, yPos4}; 
				shape = pathLine(x, y);
				g2d.draw(shape);		
			}
			else{
				shape = trapezium(xPos1, yPos1, xPos2, yPos2, xPos3, yPos3, xPos4, yPos4);			
				g2d.draw(shape);			
			}
			
			g2d.setStroke(defaultBasicStroke);
		}
		
	}
	
	private void drawSelectedAngleRange(Graphics2D g2d){
		if (dragging && contactView.getGUIState().getSelectionMode()==ContactGUIState.SelMode.RECT
				&& this.mapProjType!=azimuthalMapProj) {
			double xS, yS, xE, yE;
			double xPos1, yPos1, xPos2, yPos2, xPos3, yPos3, xPos4, yPos4;
			g2d.setColor(squareSelColor);
			
			Pair<Double> posP = screen2spherCoord(mousePressedPos);
			Pair<Double> posD = screen2spherCoord(mouseDraggingPos);
			xS = posP.getFirst();
			yS = posP.getSecond();
			xE = posD.getFirst();
			yE = posD.getSecond();
			if (this.mapProjType==kavrayskiyMapProj){
				xS = lambdaFromKavrayskiy(xS-Math.PI, yS-(Math.PI/2)) + Math.PI;
				xE = lambdaFromKavrayskiy(xE-Math.PI, yE-(Math.PI/2)) + Math.PI;
			}
			xS -= (this.origCoordinates.getFirst()-Math.PI);
			xE -= (this.origCoordinates.getFirst()-Math.PI);
			
			xPos1 = getScreenPosFromRad(xS, yS).getFirst();
			yPos1 = getScreenPosFromRad(xS, yS).getSecond();
			xPos2 = getScreenPosFromRad(xE, yS).getFirst();
			yPos2 = getScreenPosFromRad(xE, yS).getSecond();
			xPos3 = getScreenPosFromRad(xE, yE).getFirst();
			yPos3 = getScreenPosFromRad(xE, yE).getSecond();
			xPos4 = getScreenPosFromRad(xS, yE).getFirst();
			yPos4 = getScreenPosFromRad(xS, yE).getSecond();	
			
			Shape shape = trapezium(xPos1, yPos1, xPos2, yPos2, xPos3, yPos3, xPos4, yPos4);			
			g2d.draw(shape);
		} 
	}
	
	private void updateAnglePanel(){
		this.contStatBar.getAnglePanel().setIRes(String.valueOf(this.iRes));
		this.contStatBar.getAnglePanel().setJRes(String.valueOf(this.jRes));
		this.contStatBar.getAnglePanel().setINum(this.iNum);
		this.contStatBar.getAnglePanel().setJNum(this.jNum);
		this.contStatBar.getAnglePanel().setISSType(String.valueOf(this.iSSType));
		this.contStatBar.getAnglePanel().setJSSType(String.valueOf(this.jSSType));
		this.contStatBar.getAnglePanel().setNBHS(this.nbhString);
		
		float val = (float)(Math.round(this.tmpLambdaRange.getFirst()*100))/100;
		this.contStatBar.getAnglePanel().setLambdaMin(String.valueOf(val));	
		val = (float)(Math.round(this.tmpAngleComb.getFirst()*100))/100;
		this.contStatBar.getAnglePanel().setLambdaMax(String.valueOf(val));	
		val = (float)(Math.round(this.tmpPhiRange.getFirst()*100))/100;
		this.contStatBar.getAnglePanel().setPhiMin(String.valueOf(val));	
		val = (float)(Math.round(this.tmpAngleComb.getSecond()*100))/100;
		this.contStatBar.getAnglePanel().setPhiMax(String.valueOf(val));
		
		this.contStatBar.repaint();		
	}
	
	// end drawing methods
	
	// --- Methods for coordinate handling (spherical and cartesian coordinates of projection) -----
	
	public boolean angleWithinValidRange(double xPos, double yPos){
		boolean valid = true;
		if (this.mapProjType==kavrayskiyMapProj){
			if (xPos<0 || xPos>2*Math.PI)
				valid = false;
		}
		else if (this.mapProjType==azimuthalMapProj){
			if (xPos<0 || xPos>2*Math.PI || yPos<0 || yPos>Math.PI)
				valid = false;
		}
		return valid;
	}
	
	private int angleWithinCluster(double lambda, double phi){
		int index = -1;
		lambda -= Math.PI; // minL/maxL[-Pi:+Pi]
		if (this.clusterProp != null){
			// clusters at i=1 contains IDs of noise nodes
			for (int i=1; i<this.clusters.length; i++){
				double minL = this.clusterProp[i-1][0];
				double maxL = this.clusterProp[i-1][2];
				double minP = this.clusterProp[i-1][3];
				double maxP = this.clusterProp[i-1][5];
				if (lambda>=minL && lambda<=maxL && phi>=minP && phi<=maxP){
					index = i;
					return index;
				}
			}
		}
		return index;
	}
	
	/*
	 * Pseudocylindrical projection of angle values
	 * Expects values for phi[-Pi/2:+Pi/2] and lambda[-Pi:Pi]
	 * */
	public double lambda2Kavrayskiy(double lambdaRad, double phiRad){
		double lambda = (3*lambdaRad/(2*Math.PI))*Math.sqrt((Math.PI*Math.PI/3)-(phiRad*phiRad));
		return lambda;
	}
	public double lambdaFromKavrayskiy(double lambdaRad, double phiRad){
		double lambda = (2*Math.PI*lambdaRad) / (3*Math.sqrt((Math.PI*Math.PI/3)-(phiRad*phiRad))) ;
		return lambda;
	}
	
	/*
	 * Orthographic projection of angle values from certain viewpoint
	 * Expects values for phi[-Pi/2:+Pi/2] and lambda[-Pi:Pi]
	 * */
	private Pair<Double> orthoProjOfLP(double lambdaRad, double phiRad){
		Pair<Double> pos;
		double xPos=0, yPos=0;	
//		double lambda0 = this.origCoordinates.getFirst(); //Math.PI;
//		double phi0 = this.origCoordinates.getSecond(); //Math.PI/2;
		double lambda0 = this.centerOfProjection.getFirst(); //Math.PI;
		double phi0 = this.centerOfProjection.getSecond(); //Math.PI/2;
		lambda0 -= Math.PI;
		phi0 -= (Math.PI/2);		
		// values of lambdaRad and phiRad should lie between -Pi:+PI and -PI/2:+PI/2
		xPos = Math.cos(phiRad)*Math.sin(lambdaRad-lambda0);
		yPos = (Math.cos(phi0)*Math.sin(phiRad)) - (Math.sin(phi0)*Math.cos(phiRad)*Math.cos(lambdaRad-lambda0));
		pos = new Pair<Double>(xPos, yPos);
		return pos;
	}
	
	/*
	 * Calculates original spherical coordinates from
	 * orthographic projection of angle values from certain viewpoint
	 * Expects values for phi[-Pi/2:+Pi/2] and lambda[-Pi:Pi]
	 * */
	@SuppressWarnings("unused")
	private Pair<Double> lpFromOrthoProj(double x, double y){
		Pair<Double> lp;
		double l=0, p=0;
		double dist = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
		double c = Math.asin(dist);
		double l0 = this.centerOfProjection.getFirst() - Math.PI; 
		double p0 = this.centerOfProjection.getSecond() - (Math.PI/2);
		p = Math.asin( Math.cos(c)*Math.sin(p0) + (y*Math.sin(c)*Math.cos(p0)/dist) );
		l = l0 + Math.atan( x*Math.sin(c) / ( (dist*Math.cos(p0)*Math.cos(c)) - (y*Math.sin(p0)*Math.sin(c)) ) );
		l += Math.PI;
		p += (Math.PI/2);
		lp = new Pair<Double>(l, p);
		return lp;
	}
	
	/*
	 * Calculates original spherical coordinates from
	 * orthographic projection of angle values from certain viewpoint
	 * Expects values for phi[-Pi/2:+Pi/2] and lambda[-Pi:Pi]
	 * */
	private Pair<Double> lpFromOrthoProj(double x, double y, Point point){
		Pair<Double> lp;
		double l=0, p=0;
		double lC = this.centerOfProjection.getFirst();
		double pC = this.centerOfProjection.getSecond();
		double dist = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
		if (point.x>2*this.rSphere){
			dist *= -1;
		}
		double c = Math.asin(dist);
		double l0 = lC - Math.PI; 
		double p0 = pC - (Math.PI/2);
		p = Math.asin( Math.cos(c)*Math.sin(p0) + (y*Math.sin(c)*Math.cos(p0)/dist) );
		l = l0 + Math.atan( x*Math.sin(c) / ( (dist*Math.cos(p0)*Math.cos(c)) - (y*Math.sin(p0)*Math.sin(c)) ) );
		l += Math.PI;
		p += (Math.PI/2);
		
		// determine l and p just for front view
		if (point.x>2*this.rSphere){
			l = 0.0;
			p = 0.0;
		}
		
		lp = new Pair<Double>(l, p);
		return lp;
	}
	
	
	/*
	 * Transforms Radian values to Screen-position values
	 * Expects values for phi[0:Pi] and lambda[0:2Pi]
	 * */
	private Pair<Double> getScreenPosFromRad(double lambdaRad, double phiRad){
		Pair<Double> pos; // = new Pair(0,0);
		double xPos=0, yPos=0;	
		// 0:lambdaRad:2PI 0:phiRad:PI/2 
		
		if (this.mapProjType==kavrayskiyMapProj){
			phiRad = translateYCoordRespective2Orig(phiRad);
			lambdaRad = translateXCoordRespective2Orig(lambdaRad);
			lambdaRad -= Math.PI;
			phiRad -= (Math.PI/2);
			
			yPos = phiRad +(Math.PI/2);
			xPos = lambda2Kavrayskiy(lambdaRad, phiRad);		
			xPos = ( (Math.PI+xPos) *this.voxelsize )+this.border;
			yPos = (yPos*this.voxelsize) +this.border;				
		}
		else if (this.mapProjType==cylindricalMapProj){
			xPos = translateXPixelCoordRespective2Orig(lambdaRad*this.voxelsize )+this.border;
			yPos = translateYPixelCoordRespective2Orig(phiRad*this.voxelsize) +this.border;
		}
		else if (this.mapProjType==azimuthalMapProj){
			// angles negative and positive values
			phiRad -= (Math.PI/2);
			lambdaRad -= Math.PI;	
			xPos = orthoProjOfLP(lambdaRad, phiRad).getFirst();
			yPos = orthoProjOfLP(lambdaRad, phiRad).getSecond();
			xPos = (xPos*this.voxelsize*voxelsizeFactor)+this.border;
			yPos = (yPos*this.voxelsize*voxelsizeFactor)+this.border;	
			xPos += this.rSphere; //R;
			yPos += this.rSphere; //R;
			if (!isOnFrontView(lambdaRad+Math.PI, phiRad+(Math.PI/2)))
				xPos = (4*this.rSphere)-xPos;
		}	
		
		pos = new Pair<Double>(xPos, yPos);
		return pos;
	}
	
	private boolean isOnFrontView(double lambda, double phi){
		double dist = distPointsOnSphere(lambda, phi, this.centerOfProjection.getFirst(), this.centerOfProjection.getSecond());
		if (dist>this.maxDistPoints)
			return false;
		else
			return true;
	}
	
	/**
	 * Returns the corresponding phi-lambda values in the sphoxelView given screen
	 * coordinates
	 */
	private Pair<Double> screen2spherCoord(Point point){
		
		Pair<Double> doubleP;  
		if (this.mapProjType==azimuthalMapProj){
			double x = point.x, y = point.y;
			if (x>(2*this.rSphere))
				x = (4*this.rSphere) - x;
			x -= this.rSphere;
			y -= this.rSphere; //R;
			doubleP = new Pair<Double>((double)(x-this.border)/(this.voxelsize*voxelsizeFactor) , (double)(y-this.border)/(this.voxelsize*voxelsizeFactor)); 
		}
		else 
			doubleP = new Pair<Double>((double)(point.x-this.border)/this.voxelsize , (double)(point.y-this.border)/this.voxelsize); 
		
		return doubleP;
	}
	
	@SuppressWarnings("unused")
	private Pair<Double> translateCoordRespective2Orig(Pair<Double> pair){
		Pair<Double> transl;
		double xPos = translateXCoordRespective2Orig(pair.getFirst());
		double yPos = translateYCoordRespective2Orig(pair.getSecond());
		transl = new Pair<Double>(xPos, yPos);
		return transl;
	}
	
	private double translateXCoordRespective2Orig(double x){
		double dx = (this.origCoordinates.getFirst() - Math.PI);
		double xPos = x + dx;
		if (xPos > 2*Math.PI+0.001)
			xPos -= (2*Math.PI);
		else if (xPos < 0)
			xPos += (2*Math.PI);
		return xPos;
	}
	
	private double translateYCoordRespective2Orig(double y){
		double dy = (this.origCoordinates.getSecond() - (Math.PI/2));
		double yPos = y + dy;
		return yPos;
	}
	
	private double translateXPixelCoordRespective2Orig(double x){
		double dx = (this.origCoordinates.getFirst() - Math.PI) * this.voxelsize;
		double xPos = x + dx;
		xPos = (int)(xPos*1000)/1000;
		if (xPos > 2*Math.PI* this.voxelsize)
			xPos -= (2*Math.PI* this.voxelsize);
		else if (xPos < 0)
			xPos += (2*Math.PI* this.voxelsize);
		return xPos;
	}
	
	private double translateYPixelCoordRespective2Orig(double y){
		double dy = (this.origCoordinates.getSecond() - (Math.PI/2)) * this.voxelsize;
		double yPos = y + dy;
		return yPos;
	}
	
	/**
	 * Deleted selected range from list.
	 */
	public void deleteSelectedRange(){
		
		this.lambdaRanges.removeElementAt(this.chosenSelection);
		this.phiRanges.removeElementAt(this.chosenSelection);
		this.selContacts.removeElementAt(this.chosenSelection);	
	}
	
	@SuppressWarnings("unused")
	private int checkForSelectedRanges(){
		int index = -1;			
		index = checkForSelectedRanges(this.iNum, this.jNum);		
		return index;
	}
	
	private int checkForSelectedRanges(int iNum, int jNum){
		int index = -1;	
		int count = 0;

		if (this.selContacts.size()>0){
			Iterator<Pair<Integer>> itrS = this.selContacts.iterator();
			while (itrS.hasNext()){
				Pair<Integer> sel = itrS.next();
				if (sel.getFirst()==iNum && sel.getSecond()==jNum){
					index = count; 
					break;
				}
				count++;
			}			
		}
	
		return index;
	}
	

	@SuppressWarnings("unused")
	private boolean contactElemOfSelContacts(int iNum, int jNum){
		boolean isElem = false;
		Iterator<Pair<Integer>> itrC = this.selContacts.iterator(); // holds residue numbers for each contact Pair(iNum, jNum)
		while (itrC.hasNext()){
			Pair<Integer> contact = itrC.next();
			int iNumber = contact.getFirst();
			int jNumber = contact.getSecond();
			if (iNumber==iNum && jNumber==jNum)
				isElem = true;
		}
		return isElem;
	}
	
	private int contactElemOfSettings(int iNum, int jNum){
		int index = -1;
		int count = 0;
		Iterator<String[]> itrS = this.settings.iterator(); // holds settings for each contact Pair(iNum, jNum)
		while (itrS.hasNext()){
			String[] set = itrS.next();
			int iNumber = Integer.valueOf(set[0]);
			int jNumber = Integer.valueOf(set[1]);
			if (iNumber==iNum && jNumber==jNum){
				index = count; break;
			}
			count++;
		}
		return index;
	}
	
	/**
	 * Update tmpContact with the contacts contained in the rectangle given by
	 * the upperLeft and lowerRight.
	 */
	private void squareSelect(){
		Pair<Double> upperLeft = screen2spherCoord(mousePressedPos);
		double lUL=0, pUL=0, lLR=0, pLR=0;
		double xPosUL, yPosUL; 
		// we reset the tmpContacts list so every new mouse selection starts from a blank list
		
		xPosUL = upperLeft.getFirst();
		yPosUL = upperLeft.getSecond();
		lUL = xPosUL;
		pUL = yPosUL;
		if (this.mapProjType==kavrayskiyMapProj){
			lUL = lambdaFromKavrayskiy(xPosUL-Math.PI, yPosUL-(Math.PI/2)) + Math.PI;
		}
		else if (this.mapProjType==azimuthalMapProj){
			lUL = lpFromOrthoProj(xPosUL, yPosUL, mousePressedPos).getFirst();
			pUL = lpFromOrthoProj(xPosUL, yPosUL, mousePressedPos).getSecond();			
		}
		if (this.mapProjType!=azimuthalMapProj){
			lUL -= (this.origCoordinates.getFirst()-Math.PI);
		}
		if (lUL<0)
			lUL += (2*Math.PI);
		else if (lUL>2*Math.PI)
			lUL -= (2*Math.PI);		

//		System.out.println("mouseDraggingAng=?tmpAng: "+lLR+"=?"+this.tmpAngleComb.getFirst()+"  "+pLR+"=?"+this.tmpAngleComb.getSecond());
		lLR = tmpAngleComb.getFirst();
		pLR = tmpAngleComb.getSecond();

		double pmin = Math.min(lUL, lLR);
		double tmin = Math.min(pUL, pLR);
		double pmax = Math.max(lUL, lLR);
		double tmax = Math.max(pUL, pLR);

		this.tmpLambdaRange = new Pair<Double>(pmin, pmax);
		this.tmpPhiRange = new Pair<Double>(tmin, tmax);
		this.tmpSelContact = new Pair<Integer>(this.iNum,this.jNum);		
		this.tmpAngleComb = new Pair<Double>(pmax, tmax);
	}
	
//	/** Called by ResidueRuler to enable display of ruler "crosshair" */	
//	public void showRulerCrosshair() {
//		showRulerCrosshair = true;
//	}
//	/** Called by ResidueRuler to switch off showing ruler coordinates */
//	public void hideRulerCoordinate() {
//		showRulerCoord = false;
//	}
	
	private void updateDimensions(){
		int xDimBU = this.xDim;
		int yDimBU = this.yDim;
		this.screenSize = this.contactView.getScreenSize();
		
		if ((float)(this.getSize().height)/(float)(this.getSize().width) > g2dRatio){
			this.xDim = this.getSize().width;
			this.yDim = (int) (this.xDim*g2dRatio);
		}
		else{
			this.yDim = this.getSize().height;
			this.xDim = (int) (this.yDim/g2dRatio);			
		}
		this.g2dSize.setSize(new Dimension(this.xDim, this.yDim));
		this.setSize(this.g2dSize);
		
		this.rSphere = g2dSize.getHeight()/2;
		this.maxDistPoints = distPointsOnSphere(0, 0, 0, Math.PI/2);
		
		// update pixel dimensions
		this.pixelWidth = (float)(this.xDim-2*this.border)/(float)(2*this.numSteps) ;
		this.pixelHeight =  this.pixelWidth; 
		this.voxelsize = (float) ((float)(this.numSteps*this.pixelWidth)/Math.PI);
		
		if (this.xDim!=xDimBU || this.yDim!=yDimBU)
			updateScreenBuffer();
		
	}

	/**
	 * Main method to draw the component on screen. This method is called each
	 * time the component has to be (re) drawn on screen. It is called
	 * automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	@Override
	protected synchronized void paintComponent(Graphics g) {
//		System.out.println("paintComponent");
		
		Graphics2D g2d = (Graphics2D) g.create();		
//		updateDimensions();
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// draw screen buffer
		if(screenBuffer==null) {
			screenBuffer = new ScreenBuffer(this);
			updateScreenBuffer();
		} else {
			
			g2d.drawImage(screenBuffer.getImage(),0,0,this);
		}
		// drawing selection rectangle if dragging mouse and showing temp
		// selection in red (tmpContacts)		
		drawOccupiedAngleRanges(g2d);
		drawSelectedAngleRange(g2d);
		drawCrosshair(g2d);
		updateAnglePanel();
	}
	
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

		// paint background
		int bgSizeX = Math.max(this.g2dSize.width, this.getWidth());		// fill background
															// even if window is
															// not square
		int bgSizeY = Math.max(this.g2dSize.height, this.getHeight());	// fill background
															// even of window is
															// not square
				
		g2d.setColor(backgroundColor);
		if (isOpaque()) {
			g2d.fillRect(0, 0, bgSizeX, bgSizeY);
		}
		
		// sphoxel background
		if (this.contactView.getGUIState().getShowSphoxelBG()){
//			drawSphoxels(g2d);
			drawSphoxelMap(g2d);
			
//			this.mapProjType = azimuthalMapProj;
//			drawSphoxelCakeMap(g2d);
		}
		// neighbourhood-string-traces
		if (this.contactView.getGUIState().getShowNBHStraces())
			drawNBHSTraces(g2d);
		if (this.showNBHStemplateTrace)
			drawNBHSTemplateTrace(g2d);
		if (this.contactView.getGUIState().getShowLongitudes()){
			drawLongitudes(g2d);
		}
		if (this.contactView.getGUIState().getShowLatitudes()){
			drawLatitudes(g2d);
		}
		if (this.contactView.getGUIState().getShowLongLatCentre()){
			drawLongLatCentre(g2d);
		}	
		if (this.contactView.getGUIState().getShowRulers()){
			this.contactView.lambdaRuler.repaint();
			this.contactView.phiRuler.repaint();
		}
		if (this.clusterAverDirec!=null && this.clusterProp!=null) // && this.mapProjType!=azimuthalMapProj)
			drawEdgeClusterDir(g2d);
//		if (this.clusterDirProp!=null)
//			drawEdgeCluster(g2d);
		if (this.clusterProp!=null)
			drawClusterBoundaries(g2d);

		repaint();
	}
	
	/*---------------------------- setters and getters -----------------------------*/
	
	public String getDescribingFN(){
		String fn = mod.getLoadedGraphID()+"_"+this.iNum+this.iRes+"-"+this.jNum+this.jRes+"_"+this.nbhString+"_";
		if (this.diffSStypeNBH)
			fn += this.nbhSSType;
		else
			fn += "Any";
		if (this.mod3 != null)
			fn += "_Answer";
		return fn;
	}
	
	public String[] getSetOfOptStrings() {
		return setOfOptStrings;
	}
	public String getNbhString() {
		return nbhString;
	}
	public String getOrigNBHString() {
		return origNBHString;
	}
	public void setNbhString(String nbhString) {
		this.nbhString = nbhString;
		this.nbhStringL = "%";
		this.nbhsRes = new char[this.nbhString.length()];
		int count = 0;
		this.nbhsRes = new char[this.nbhString.length()];
		for (int i=0; i<this.nbhString.length(); i++){
			this.nbhStringL += this.nbhString.charAt(i);
			this.nbhStringL += "%";
			this.nbhsRes[count] = this.nbhString.charAt(i);
			count++;
		}
		System.out.println("Actual NBHS: "+this.nbhString+"-->"+this.nbhStringL);	
	}

	
	public void setStatusBar(ContactStatusBar statusBar) {
		this.contStatBar = statusBar;	
//		if (contStatBar!= null)
//			this.contStatBar.setSetOfOptStrings(this.setOfOptStrings);	
	}
	public ContactStatusBar getSatusBar(){
		return this.contStatBar;
	}
	
	public Dimension getScreenSize(){
		return this.screenSize;
	}
	
	public Dimension getPreferredSize(){
		return this.g2dSize;
	}
	
	public Dimension getPanelSize(){
		return this.g2dSize;
	}

	public void setRadiusRangesFixed(boolean radiusRangesFixed) {
		this.radiusRangesFixed = radiusRangesFixed;
	}

	public boolean isRadiusRangesFixed() {
		return radiusRangesFixed;
	}
	
	public void setMinR(float r){
		this.minr = r;
	}
	public void setMaxR(float r){
		this.maxr = r;
	}
	public void setNumSteps(int num){
		this.numSteps = num;
		this.resol = 180/(float)this.numSteps;
		this.deltaRad = Math.PI/this.numSteps;
	}
	public void setResol(float res){
		this.numSteps = (int) (180/res);
		this.resol = 180/(float)this.numSteps;
		System.out.println("numSteps="+this.numSteps+"  --> resol="+this.resol);
	}	
	public Pair<Double> getOrigCoord(){
		return this.origCoordinates;
	}
	public float getVoxelsize(){
		return this.voxelsize;
	}

	public boolean isDiffSStype() {
		return diffSStype;
	}
	public void setDiffSStype(boolean diffSStype) {
		this.diffSStype = diffSStype;
		if (!this.diffSStype)
			this.iSSType = CMPdb_sphoxel.AnySStype;
	}

	public boolean isDiffSStypeNBH() {
		return diffSStypeNBH;
	}
	public void setDiffSStypeNBH(boolean diffSStypeNBH) {
		this.diffSStypeNBH = diffSStypeNBH;
	}

	public char getNbhSSType() {
		return nbhSSType;
	}
	public void setNbhSSType(char nbhSSType) {
		this.nbhSSType = nbhSSType;
	}

	public boolean isRemoveOutliers() {
		return removeOutliers;
	}

	public void setRemoveOutliers(boolean removeOutliers) {
		this.removeOutliers = removeOutliers;
		updateScreenBuffer();
	}
	
	public void setShowResInfo(boolean show){
		this.showResInfo = show;
		this.updateScreenBuffer();
	}
	
	public void setPaintCentralResidue(boolean show){
		this.paintCentralResidue = show;
		this.updateScreenBuffer();
	}

	public void setShowNBHStemplateTrace(boolean show) {
		this.showNBHStemplateTrace = show;
		this.updateScreenBuffer();
	}

	public double getMinAllowedRat() {
		return minAllowedRat;
	}

	public void setMinAllowedRat(double minAllowedRat) {
		this.minAllowedRat = minAllowedRat;
		updateScreenBuffer();
	}

	public double getMaxAllowedRat() {
		return maxAllowedRat;
	}

	public void setMaxAllowedRat(double maxAllowedRat) {
		this.maxAllowedRat = maxAllowedRat;
		updateScreenBuffer();
	}


	public int getChosenColourScale() {
		return chosenColourScale;
	}

	public void setChosenColourScale(int chosenColourScale) {
		this.chosenColourScale = chosenColourScale;
		// show actual colour scale
		updateScreenBuffer();
	}

	public int getMapProjType() {
		return mapProjType;
	}

	public void setMapProjType(int mapProjType) {
		this.mapProjType = mapProjType;
		if (this.mapProjType!=azimuthalMapProj){
			this.origCoordinates = new Pair<Double>(this.origCoordinates.getFirst(), Math.PI/2);
			this.centerOfProjection = new Pair<Double>(2*Math.PI-this.origCoordinates.getFirst(), Math.PI-this.origCoordinates.getSecond());
		}
		else{
			this.origCoordinates = new Pair<Double>(this.origCoordinates.getFirst(), Math.PI-this.centerOfProjection.getSecond());
//			this.origCoordinates = this.centerOfProjection;
			this.centerOfProjection = new Pair<Double>(2*Math.PI-this.origCoordinates.getFirst(), Math.PI-this.origCoordinates.getSecond());
		}
		this.updateScreenBuffer();
	}

	public void setDeltaOffSetX(double deltaOffSetX) {
		this.deltaOffSetX = deltaOffSetX;
	}

	public double getDeltaOffSetX() {
		updateDimensions();
		this.deltaOffSetX = getOffSet(0.0, 0.0);
		return deltaOffSetX;
	}
	
	public void setDeltaOffSetXEnd(double deltaOffSetXEnd) {
		this.deltaOffSetXEnd = deltaOffSetXEnd;
	}

	public double getDeltaOffSetXEnd() {
		updateDimensions();
		this.deltaOffSetXEnd = getOffSet(2*Math.PI, 0.0);
		return deltaOffSetXEnd;
	}
	
	public void setDeltaOffSetXCenter(double deltaOffSetXC) {
		this.deltaOffSetXCenter = deltaOffSetXC;
	}

	public double getDeltaOffSetXCenter() {
		updateDimensions();
		// this.origCoordinates = new Pair<Float>((float)(Math.PI), (float)(Math.PI/2));
		this.deltaOffSetXCenter = getOffSet(this.origCoordinates.getFirst(), 0.0);
		return deltaOffSetXCenter;
	}

	public double getMinRatio() {
		return minRatio;
	}

	public void setMinRatio(double minRatio) {
		this.minRatio = minRatio;
	}

	public double getMaxRatio() {
		return maxRatio;
	}

	public void setMaxRatio(double maxRatio) {
		this.maxRatio = maxRatio;
	}	
	
	public int getMaxNumTraces() {
		return maxNumTraces;
	}

	public void setMaxNumTraces(int maxNumTraces) {
		this.maxNumTraces = maxNumTraces;
	}
	
	public void setEpsilon(int epsilon) {
		this.epsilon = epsilon;
	}

	public int getEpsilon() {
		return epsilon;
	}

	public void setMinNumNBs(int minNumNBs) {
		this.minNumNBs = minNumNBs;
	}

	public int getMinNumNBs() {
		return minNumNBs;
	}

	public int[] getHistWholeAngleRange() {
		return histWholeAngleRange;
	}
	public Vector<int[]> getHist4SelAngleRange() {
		return hist4SelAngleRange;
	}
	public void setShowHist(boolean showHist) {
		this.showHist = showHist;
	}
	public boolean isShowHist() {
		return showHist;
	}	
	public void setHistType(int type){
		this.histType = type;
	}
	public int getHistType(){
		return this.histType;
	}
	public Vector<double[]> getMinMaxAverT4SelAngleRange() {
		return minMaxAverT4SelAngleRange;
	}
//	public void setMinMaxAverT4SelAngleRange(
//			Vector<double[]> minMaxAverT4SelAngleRange) {
//		this.minMaxAverT4SelAngleRange = minMaxAverT4SelAngleRange;
//	}
	public double[] getNodeDistrWithinSel(){
		return this.nodeDistrWithinSel;
	}
	public int[] getNodeDistrAroundSel(){
		return this.nodeDistrAroundSel;
	}
	public double[][] getNodeDistr4Sel(){
		return this.nodeDistr4Sel;
	}
	public int[][] getHistTWholeAngleRange() {
		return histTWholeAngleRange;
	}	
	public void setHistTWholeAngleRange(int[][] histTWholeAngleRange) {
		this.histTWholeAngleRange = histTWholeAngleRange;
	}
	public Vector<int[][]> getHistT4SelAngleRange() {
		return histT4SelAngleRange;
	}
	public void setHistT4SelAngleRange(Vector<int[][]> histT4SelAngleRange) {
		this.histT4SelAngleRange = histT4SelAngleRange;
	}

	/* ChosenSelection has value [0:numSelections-1].
	 * If right click outside of any selection, chosen selection = numSelections.
	 *  */
	public int getChosenSelection() {
		return chosenSelection;
	}
	public void setChosenSelection(int chosenSelection) {
		this.chosenSelection = chosenSelection;
	}
	
	public String[] getChosenPTRange(){
//		double[] range;
		String[] rangeS;
		Iterator<Pair<Double>> itrP = this.lambdaRanges.iterator();
		Iterator<Pair<Double>> itrT = this.phiRanges.iterator();
		int count = 0;
		Pair<Double> lambda=null, phi=null;
		while (count<=this.chosenSelection && itrP.hasNext()){
			lambda = itrP.next();
			phi = itrT.next();
			count++;
		}
		double p1 = (double)(Math.round(lambda.getFirst()*100))/100;
		double p2 = (double)(Math.round(lambda.getSecond()*100))/100;
		double t1 = (double)(Math.round(phi.getFirst()*100))/100;
		double t2 = (double)(Math.round(phi.getSecond()*100))/100; //(float)(Math.round(this.tmpAngleComb.getFirst()*100))/100;
		rangeS = new String[]{String.valueOf(p1), String.valueOf(p2), String.valueOf(t1), String.valueOf(t2)};
//		range = new double[]{lambda.getFirst(), lambda.getSecond(), phi.getFirst(), phi.getSecond()};
		return rangeS;
	}

	
//	/**
//	 * Sets the output size and updates the ratio and contact square size. This
//	 * will affect all drawing operations. Used by print() method to change the
//	 * output size to the size of the paper and back.
//	 */
//	protected void setOutputSize(int size) {
//		outputSize = size;
//		ratio = (double) outputSize/numSteps;		// scale factor, = size
////		// of one pixel
////		this.pixelWidth =  (float) ratio; 			// the size of the
////		this.pixelHeight = (float) ratio;
////		// square representing a sphoxel
//	}
	
	private void showPopup(MouseEvent e) {		
		this.rightClickAngle = this.tmpAngleComb;

		// --- determine chosen selection
		Iterator<Pair<Double>> itrP = this.lambdaRanges.iterator();
		Iterator<Pair<Double>> itrT = this.phiRanges.iterator();
		int count = 0;
		this.chosenSelection = this.lambdaRanges.size();
		while (itrP.hasNext()){
			Pair<Double> lambda = itrP.next();
			Pair<Double> phi = itrT.next();
			if (rightClickAngle.getFirst()>=lambda.getFirst() && rightClickAngle.getFirst()<=lambda.getSecond() 
					&& rightClickAngle.getSecond()>=phi.getFirst() && rightClickAngle.getSecond()<=phi.getSecond()){
				this.chosenSelection = count;
				break;
			}
			count++;
		}
		System.out.println("chosen selection= "+this.chosenSelection);
		if (this.chosenSelection != this.lambdaRanges.size())
			this.contactView.popup.show(e.getComponent(), e.getX(), e.getY());
	}
		
	
	/*---------------------------- mouse events -----------------------------*/

	public void mousePressed(MouseEvent evt) {
		// This is called when the user presses the mouse anywhere
		// in the frame
		
//		System.out.println("mousePressed");

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
		// TODO: Move much of this to MouseClicked and pull up Contact cont = screen2spherCoord...
		if (evt.isPopupTrigger()) {
			showPopup(evt);
			dragging = false;
			return;
		}
		// only if release after left click (BUTTON1)
		if (evt.getButton()==MouseEvent.BUTTON1) {

			switch (contactView.getGUIState().getSelectionMode()) {
			case RECT:
//				if (this.mapProjType!=azimuthalMapProj)
				{					
					if (dragging){
	//					squareSelect();
						boolean valid = true; // = angleWithinValidRange(this.tmpSelContact.getFirst(), this.tmpSelContact.getSecond());
						if (!angleWithinValidRange(tmpLambdaRange.getFirst(), tmpPhiRange.getFirst()) 
								|| !angleWithinValidRange(tmpLambdaRange.getSecond(), tmpPhiRange.getSecond()))
							valid = false;
						System.out.println("mouseReleased "+this.tmpLambdaRange.getFirst()+"-"+this.tmpLambdaRange.getSecond()
								+" , "+this.tmpPhiRange.getFirst()+"-"+this.tmpPhiRange.getSecond());
						if (valid){
							updateSelections();
							updateAngleRange();	
							commitSettings();
							System.out.println("mouseReleased pos valid");					
						}
					}				
					dragging = false;
	//				this.repaint();				
				}
				
				return;
				
			case CLUSTER:
				this.tmpSelContact = new Pair<Integer>(this.iNum,this.jNum);
				if (angleWithinValidRange(tmpAngleComb.getFirst(), tmpAngleComb.getSecond())){
					System.out.println("tmpAngleComb: "+this.tmpAngleComb.getFirst()+"-"+this.tmpAngleComb.getSecond());
					System.out.println("tmpAngleComb: "+(this.tmpAngleComb.getFirst()-Math.PI)+"-"+this.tmpAngleComb.getSecond());
					// Test if clicked angleComb lies in any of the defined clusters
					boolean valid = false; //check
					int index = angleWithinCluster(tmpAngleComb.getFirst(), tmpAngleComb.getSecond());
					if (index>0)
						valid = true;
					if (valid){
						// use min and max values of chosen cluster
						double rangeBorder = 0; //0.05;
						double lS = this.clusterProp[index-1][0]+Math.PI-rangeBorder, lE = this.clusterProp[index-1][2]+Math.PI+rangeBorder;
						double pS = this.clusterProp[index-1][3]-rangeBorder, pE = this.clusterProp[index-1][5]+rangeBorder; 
						this.tmpLambdaRange = new Pair<Double>(lS, lE);
						this.tmpPhiRange = new Pair<Double>(pS, pE);						
						System.out.println("clusterBasedSelectedRange "+this.tmpLambdaRange.getFirst()+"-"+this.tmpLambdaRange.getSecond()
								+" , "+this.tmpPhiRange.getFirst()+"-"+this.tmpPhiRange.getSecond());
						updateSelections();
						commitSettings();
						updateAngleRange();	
					}
					if (clusterAnal!=null)
						clusterAnal.analyseNodeDistributionInCluster(index);
					System.out.println("Chosen cluster: "+index);
				}
//				this.repaint();
				return;
				
			case PAN:
				if (this.mapProjType != azimuthalMapProj){
					Pair<Double> pos = screen2spherCoord(mousePos);
					double xPos = pos.getFirst();
					double yPos = Math.PI/2; //pos.getSecond();
					if (this.mapProjType==kavrayskiyMapProj){
						xPos = lambdaFromKavrayskiy(xPos-Math.PI, yPos-(Math.PI/2)) + Math.PI;
					}
					this.origCoordinates = new Pair<Double>(xPos, yPos);
					this.centerOfProjection = new Pair<Double>(2*Math.PI-this.origCoordinates.getFirst(), Math.PI-this.origCoordinates.getSecond());
					
					this.contactView.lambdaRuler.repaint();
					this.contactView.phiRuler.repaint();
					updateScreenBuffer();
//					System.out.println("tmpAngleComb: "+this.tmpAngleComb.getFirst()+","+this.tmpAngleComb.getSecond());
//					System.out.println("OrigCoord: "+this.origCoordinates.getFirst()+","+this.origCoordinates.getSecond());
//					System.out.println("CentreCoord: "+this.centerOfProjection.getFirst()+","+this.centerOfProjection.getSecond());
//					this.repaint();					
				}
				return;
			}
			
			this.tmpLambdaRange = new Pair<Double>(0.0, 0.0);
			this.tmpPhiRange = new Pair<Double>(0.0, 0.0);
		}
	}

	public void mouseDragged(MouseEvent evt) {
//		System.out.println("mouseDragged");

		// Called whenever the user moves the mouse
		// while a mouse button is held down.

		mouseMoved(evt); // TODO is this necessary? I tried getting rid of it
		// but wasn't quite working

		if(lastMouseButtonPressed == MouseEvent.BUTTON1) {
			dragging = true;
			mouseDraggingPos = evt.getPoint();
			switch (contactView.getGUIState().getSelectionMode()) {
			case RECT:
//				if (this.mapProjType!=azimuthalMapProj)
					squareSelect();	
//					System.out.println("mouseReleased "+this.tmpLambdaRange.getFirst()+"-"+this.tmpLambdaRange.getSecond()
//							+" , "+this.tmpPhiRange.getFirst()+"-"+this.tmpPhiRange.getSecond());			
				break;
			case PAN:
				if (this.mapProjType != azimuthalMapProj){
					Pair<Double> pos = screen2spherCoord(mousePos);
					double xPos = pos.getFirst();
					double yPos = Math.PI/2; //pos.getSecond();
					double fac = 2.5*2*Math.PI/360; //(Math.PI / 20);
					if (this.mapProjType==kavrayskiyMapProj){
						xPos = lambdaFromKavrayskiy(xPos-Math.PI, yPos-(Math.PI/2)) + Math.PI;
					}
					xPos = xPos/fac;
					xPos = Math.round(xPos);
					xPos = xPos*fac;
//					xPos = Math.round/(xPos/fac) * fac;
					this.origCoordinates = new Pair<Double>(xPos, yPos);
					this.centerOfProjection = new Pair<Double>(2*Math.PI-this.origCoordinates.getFirst(), Math.PI-this.origCoordinates.getSecond());
					this.contactView.lambdaRuler.repaint();
					this.contactView.phiRuler.repaint();
					updateScreenBuffer();					
				}
				break;
			}
		}
	} 

	public void mouseEntered(MouseEvent evt) { 
		mouseIn = true;
	}

	public void mouseExited(MouseEvent evt) {
//		System.out.println("mouseExited");

		mouseIn = false;
//		this.repaint();
	}

	public void mouseClicked(MouseEvent evt) {
	}
	
	public void mouseMoved(MouseEvent evt) {
//		System.out.println("mouseMoved");
		double xPos, yPos;
		double l, p;
		mousePos = evt.getPoint();
		
		Pair<Double> pos = screen2spherCoord(mousePos);
		xPos = pos.getFirst();
		yPos = pos.getSecond();
		if (this.mapProjType==kavrayskiyMapProj){
			l = lambdaFromKavrayskiy(xPos-Math.PI, yPos-(Math.PI/2)) + Math.PI;
			p = yPos;
		}
		else if (this.mapProjType==azimuthalMapProj){
			l = lpFromOrthoProj(xPos, yPos, mousePos).getFirst();
			p = lpFromOrthoProj(xPos, yPos, mousePos).getSecond();
		}
		else{
			l = xPos;
			p = yPos;
		}
		boolean valid = angleWithinValidRange(l, p);
		if (this.mapProjType!=azimuthalMapProj)
			l -= (this.origCoordinates.getFirst()-Math.PI);
		if (l<0)
			l += (2*Math.PI);
		else if (l>2*Math.PI)
			l -= (2*Math.PI);
		
		if (valid)
			this.tmpAngleComb = new Pair<Double>(l, p);
		else {
			this.tmpAngleComb = new Pair<Double>(0.0, 0.0);
//			System.out.println("valid = false "+l+","+p);
		}
		
		this.repaint();
	}

	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
//		System.out.println("keyPressed");
		
	}

	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
//		System.out.println("keyReleased");
		if (!this.contactView.fileChooserOpened){
			//-- Process arrow "virtual" keys
	    	double first = this.origCoordinates.getFirst();
	    	double second = this.origCoordinates.getSecond();
	    	double fac = 2*(2.5*2*Math.PI/360); //(Math.PI / 20);
	    	fac = fac*2;
	        switch (e.getKeyCode()) {
	            case KeyEvent.VK_LEFT : first-=fac; break;
	            case KeyEvent.VK_RIGHT: first+=fac; break;
	            case KeyEvent.VK_UP   : second-=fac;   break;
	            case KeyEvent.VK_DOWN : second+=fac; break;
	        }
	        if (first!=this.origCoordinates.getFirst() || second!=this.origCoordinates.getSecond()){
	            if (first<0)
	            	first += 2*Math.PI;
	            if (first>2*Math.PI)
	            	first -= 2*Math.PI;
	            if (second<0)
	            	second += Math.PI;
	            if (second>Math.PI)
	            	second -= Math.PI;
	            
	            if (this.mapProjType!=azimuthalMapProj)
	            	second = (float) (Math.PI/2);   // --> panning just horizontally     	
	    		this.origCoordinates = new Pair<Double>(first,second); // this.tmpAngleComb;
	    		this.centerOfProjection = new Pair<Double>(2*Math.PI-this.origCoordinates.getFirst(), Math.PI-this.origCoordinates.getSecond());
	    		
//	    		System.out.println("OrigCoord: "+this.origCoordinates.getFirst()+","+this.origCoordinates.getSecond());
//	    		System.out.println("CentreCoord: "+this.centerOfProjection.getFirst()+","+this.centerOfProjection.getSecond());
	    		
	    		this.contactView.lambdaRuler.repaint();
	    		this.contactView.phiRuler.repaint();	
	    		updateScreenBuffer();        	
	        }			
		}
	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub	
//		System.out.println("keyTyped");	
	}

	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void componentResized(ComponentEvent e) {
		// TODO Auto-generated method stub
		updateDimensions();
	}

	public void componentShown(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void setThirdModel(Model mod3) {
		// TODO Auto-generated method stub
		this.mod3 = mod3;
		calcTracesParam();
		this.repaint();
	}

}
