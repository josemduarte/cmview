package cmview.gmbp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import owl.core.structure.features.SecStrucElement;
import owl.core.structure.graphs.RIGNbhood;
import owl.core.structure.graphs.RIGNode;
//import owl.core.util.FloatPairSet;
//import owl.core.util.IntPairSet;
import owl.gmbp.CMPdb_nbhString_traces;
import owl.gmbp.CMPdb_sphoxel;

import cmview.ContactMapPane;
import cmview.datasources.Model;
import edu.uci.ics.jung.graph.util.Pair;

public class ContactPane extends JPanel implements MouseListener, MouseMotionListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected static final Dimension defaultDim = new Dimension(1600, 800);
	protected static final float g2dRatio = 0.5f; // H/W
		
	/*--------------------------- member variables --------------------------*/		
	// underlying data
	private Model mod;
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
	
	// drawing colors (being set in the constructor)
	private Color backgroundColor;	  		// background color
	private Color squareSelColor;	  		// color of selection rectangle
	private Color crosshairColor;     		// color of crosshair	
	private Color selAngleRangeColor;       // color for selected rectangles
	
	// selections 
//	private FloatPairSet phiRanges;			// permanent list of currently selected phi ranges
//	private FloatPairSet thetaRanges;       // permanent list of currently selected theta ranges
//	private IntPairSet selContacts;         // permanent list of currently selected and referred contacts
	private Vector<Pair<Float>> phiRanges;			// permanent list of currently selected phi ranges
	private Vector<Pair<Float>> thetaRanges;       // permanent list of currently selected theta ranges
	private Vector<Pair<Integer>> selContacts;         // permanent list of currently selected and referred contacts
	private Pair<Float> tmpPhiRange;
	private Pair<Float> tmpThetaRange;
	private Pair<Integer> tmpSelContact;
	private Pair<Float> tmpAngleComb;
	
	private RIGNode nodeI, nodeJ;
	
	// query data
	private char iRes='A', jRes='A';
	private char iSSType='H', jSSType='H';
	private boolean diffSStype=false;
	private String iResType="Ala", jResType="Ala";
	private int iNum=0, jNum=0;
	private String nbhString, nbhStringL;
	private String jAtom = "CA";
	
	private String db = "bagler_all5p0";
	
	// Sphoxel-Data
	private CMPdb_sphoxel sphoxel;
	private double [][] ratios;
//	private double [][][] bayesRatios;
	private double minRatio = 0;
	private double maxRatio = 0;
	private float minr=2.0f, maxr=12.8f;
	// NBHStraces-Data
	private CMPdb_nbhString_traces nbhsTraces;
	private Vector<float[]> nbhsNodes;
	private int[] numNodesPerLine;
	private int[] maxDistsJRes, minDistsJRes;
	private int numLines;
	
	// variables for representation (drawing methods)
	private int numSteps = CMPdb_sphoxel.defaultNumSteps; //CMPdb_sphoxel.defaultNumSteps;  // change later via interface
	private float resol = CMPdb_sphoxel.defaultResol; //CMPdb_sphoxel.getDefaultResol();
	private final int border = 0; //15;
	private final int yBorderThres = 45;
	private float pixelWidth = 5*36/this.numSteps; // change in dependence of numSteps
	private float pixelHeight = this.pixelWidth; //5*36/this.numSteps;	
	private float voxelsize = (float) (this.numSteps*this.pixelWidth/Math.PI); //pixelWidth;

	private boolean removeOutliers = false;
	private double minAllowedRat = -3;
	private double maxAllowedRat = 1;
	
	private boolean paintCentralResidue = true;
	
	private int xDim=0;
	private int yDim=0;
	
	private final char[] aas = new char[]{'A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y'}; // possible Residues
//	private final String AAStr = new String(aas); 
	
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
		this.cmPane = cmPane;
		this.contactView = contactView;		
		addMouseListener(this);
		addMouseMotionListener(this);

		this.setOpaque(true); // make this component opaque
		this.setBorder(BorderFactory.createLineBorder(Color.black));
		
		// initialize data
		this.nbhsNodes = new Vector<float[]>();
		this.ratios = new double[0][0];
				
		try {
			calcParam();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		this.xDim = defaultDim.width;
//		this.yDim = defaultDim.height;
		// update pixel dimensions
		this.pixelWidth = (this.xDim-2*this.border)/(2*this.numSteps) ;
		this.pixelHeight =  this.pixelWidth;
		this.voxelsize = (float) (this.numSteps*this.pixelWidth/Math.PI);
	
		this.g2dSize = defaultDim; // new Dimension(this.xDim,this.yDim);
		this.setSize(this.g2dSize);
		this.screenSize = this.contactView.getPreferredSize();
//		System.out.println("ContactPane screensize HxW: "+this.screenSize.height+"x"+this.screenSize.width);
//		this.screenSize = this.contactView.getScreenSize();
//		System.out.println("ContactPane getscreensize HxW: "+this.screenSize.height+"x"+this.screenSize.width);
//		this.contactView.setPreferredSize(this.screenSize);
//		this.contactView.setPreferredSize(new Dimension(this.screenSize.width+AngleRuler.STD_RULER_WIDTH, this.screenSize.height+AngleRuler.STD_RULER_WIDTH));

		this.mousePos = new Point();
		this.mousePressedPos = new Point();
		this.mouseDraggingPos = new Point();
		
		// set default colors
		this.backgroundColor = Color.white;
		this.squareSelColor = Color.black;	
		this.crosshairColor = Color.yellow;
		this.selAngleRangeColor = Color.white;
		
		this.dragging = false;
//		this.selContacts = new IntPairSet();
//		this.phiRanges = new FloatPairSet();
//		this.thetaRanges = new FloatPairSet();
		this.selContacts = new Vector<Pair<Integer>>();
		this.phiRanges = new Vector<Pair<Float>>();
		this.thetaRanges = new Vector<Pair<Float>>();
		
		this.tmpPhiRange = new Pair<Float>(0.0f, 0.0f);
		this.tmpThetaRange = new Pair<Float>(0.0f, 0.0f);
		this.tmpAngleComb = new Pair<Float>(0.0f, 0.0f);
		
//		setOutputSize(screenSize.width, screenSize.height);
//		System.out.println("outputsize= "+this.outputSize);
	}
	
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
		calcTracesParam();		
		
		calcSphoxel();
		calcNbhsTraces();		
	}
	
	private void calcTracesParam(){
		RIGNbhood nbhood = this.mod.getGraph().getNbhood(nodeI);
		this.nbhString = nbhood.getNbString();
		this.nbhStringL = "%";
		for (int i=0; i<this.nbhString.length(); i++){
			this.nbhStringL += this.nbhString.charAt(i);
			this.nbhStringL += "%";
		}
		System.out.println(this.nbhString+"-->"+this.nbhStringL);	
	}
	
	private void updateAngleRange(){
//		RIGEdge edge = this.mod.getGraph().getEdgeFromSerials(this.tmpSelContact.getFirst(), this.tmpSelContact.getSecond());		
//		edge.setPhiPsi(this.tmpPhiRange.getFirst(), this.tmpPhiRange.getSecond(), this.tmpThetaRange.getFirst(), this.tmpThetaRange.getSecond());		
		this.mod.getGmbp().setPhiRanges(this.phiRanges);
		this.mod.getGmbp().setThetaRanges(this.thetaRanges);
		this.mod.getGmbp().setSelContacts(this.selContacts);
	}
	
	private void calcSphoxelParam(){
		// Get first shell neighbours of involved residues
		Pair<Integer> currentResPair = this.cmPane.getmousePos();
		this.iNum = currentResPair.getFirst();
		this.jNum = currentResPair.getSecond();
		System.out.println("first:"+this.iNum+"  second:"+this.jNum);
		
		// use pair to get iRes and jRes, isstype, nbhstring
		this.nodeI = this.mod.getNodeFromSerial(this.iNum); //this.mod.getGraph().getNodeFromSerial(this.iNum);
		nodeJ = this.mod.getNodeFromSerial(this.jNum);		
		this.iRes = this.nodeI.toString().charAt(this.nodeI.toString().length()-1);
		this.jRes = nodeJ.toString().charAt(nodeJ.toString().length()-1);
		System.out.println("this.nodeI="+this.nodeI.toString()+"-->"+iRes+"  this.nodeI="+nodeJ.toString()+"-->"+jRes);
		
		this.iResType = this.nodeI.getResidueType();
		this.jResType = nodeJ.getResidueType();
		System.out.println("iresType: "+this.iResType+"  jresType: "+this.jResType);

		// Definition of sstype and jatom
		SecStrucElement iSSelem = this.nodeI.getSecStrucElement();
//		type = this.nodeI.getSecStrucElement().getType(); 
		if (iSSelem == null){
			System.out.println("No SSelement!");
			this.diffSStype = false;
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
			
			System.out.println("i secStrucElement: "+this.iSSType);
//			System.out.println("j secStrucElement: "+nodeJ.getSecStrucElement().getType());
			this.diffSStype = true;
		}
		SecStrucElement jSSelem = nodeJ.getSecStrucElement();
		if (jSSelem == null){
			System.out.println("No JSSelement!");
//			this.diffSStype = false;
		}
		else{
			if (jSSelem.isHelix())
				this.jSSType = SecStrucElement.HELIX;
			else if (jSSelem.isOther())
				this.jSSType = SecStrucElement.OTHER;
			else if (jSSelem.isStrand())
				this.jSSType = SecStrucElement.STRAND;
			else if (jSSelem.isTurn())
				this.jSSType = SecStrucElement.TURN;
			
			System.out.println("j secStrucElement: "+this.jSSType);
//			System.out.println("j secStrucElement: "+nodeJ.getSecStrucElement().getType());
//			this.diffSStype = true;
		}	
	}
	
	private void calcSphoxel() throws SQLException{
		// compute sphoxeldata
		this.sphoxel = new CMPdb_sphoxel(this.iRes, this.jRes, this.db);
		this.sphoxel.setDiffSSType(this.diffSStype); // set to true if you want to differentiate ssType
		this.sphoxel.setISSType(this.iSSType);
		this.sphoxel.setNumSteps(this.numSteps); // choose number of steps for resolution
		this.sphoxel.setMinr(this.minr);
		this.sphoxel.setMaxr(this.maxr);
//		this.ratios = new double [this.numSteps][2*this.numSteps];
		
		if (this.minr==2.0 && this.maxr==5.6)
			this.sphoxel.setRadiusPrefix(CMPdb_sphoxel.radiusRanges[0]);
		else if (this.minr==5.6 && this.maxr==9.2)
			this.sphoxel.setRadiusPrefix(CMPdb_sphoxel.radiusRanges[1]);
		else if (this.minr==9.2 && this.maxr==12.8)
			this.sphoxel.setRadiusPrefix(CMPdb_sphoxel.radiusRanges[2]);
		else 
			this.sphoxel.setRadiusPrefix("");
		if (this.sphoxel.getRadiusPrefix() == "")
			this.sphoxel.runBayes();
		else
			this.sphoxel.runBayesPreComp();
		
		this.ratios = this.sphoxel.getRatios();
		System.out.println("BayesRatios computed");		
//		this.bayesRatios = sphoxel.getBayesRatios();
		this.minRatio = this.sphoxel.getMinRatio();
		this.maxRatio = this.sphoxel.getMaxRatio();
	}
	
	public void recalcSphoxel() throws SQLException{
		if (this.sphoxel.getNumSteps()!=this.numSteps || this.sphoxel.getMinr()!=this.minr || this.sphoxel.getMaxr()!=this.maxr){

			this.sphoxel.setDiffSSType(this.diffSStype); // set to true if you want to differentiate ssType
			this.sphoxel.setISSType(this.iSSType);
			this.sphoxel.setNumSteps(this.numSteps); // choose number of steps for resolution
			this.sphoxel.setMinr(this.minr);
			this.sphoxel.setMaxr(this.maxr);
//			this.ratios = new double [this.numSteps][2*this.numSteps];
			
			if (this.minr==2.0 && this.maxr==5.6)
				this.sphoxel.setRadiusPrefix(CMPdb_sphoxel.radiusRanges[0]);
			else if (this.minr==5.6 && this.maxr==9.2)
				this.sphoxel.setRadiusPrefix(CMPdb_sphoxel.radiusRanges[1]);
			else if (this.minr==9.2 && this.maxr==12.8)
				this.sphoxel.setRadiusPrefix(CMPdb_sphoxel.radiusRanges[2]);
			else 
				this.sphoxel.setRadiusPrefix("");
			if (this.sphoxel.getRadiusPrefix() == "")
				this.sphoxel.runBayes();
			else
				this.sphoxel.runBayesPreComp();
			
			this.ratios = this.sphoxel.getRatios();
			System.out.println("BayesRatios computed");		
//			this.bayesRatios = sphoxel.getBayesRatios();
			this.minRatio = this.sphoxel.getMinRatio();
			this.maxRatio = this.sphoxel.getMaxRatio();
		}
	}
	
	private void calcNbhsTraces() throws SQLException{
		// compute nbhstringTraces
		this.nbhsTraces = new CMPdb_nbhString_traces(this.nbhStringL, this.jAtom, this.db);
		nbhsTraces.setDiffSSType(this.diffSStype);
		nbhsTraces.setSSType(this.iSSType);
		nbhsTraces.run();
		System.out.println("NbhsTraces extracted");
		nbhsNodes = nbhsTraces.getNBHSnodes();
		
		if (this.nbhsNodes.size()>0){
			System.out.println("this.nbhsNodes.size(): "+this.nbhsNodes.size());
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
				else{
//				if (node[0]!=nbNode[0] || node[1]!=nbNode[1]){ //nodes not of same line
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
			System.out.println(this.numLines +" =? "+numNodesL.size());
			for(int i=0; i<numNodesL.size(); i++){
				this.numNodesPerLine[i] = (int) numNodesL.get(i);
				this.minDistsJRes[i] = (int) minDists.get(i);
				this.maxDistsJRes[i] = (int) maxDists.get(i);
//				System.out.print(this.numNodesPerLine[i]+"_"+this.minDistsJRes[i]+"_"+this.maxDistsJRes[i]+"\t");
			}
//			System.out.println();
		}		
	}
	
	/*------------------------ setters/getters --------------------*/
	public void setMinR(float r){
		this.minr = r;
	}
	public void setMaxR(float r){
		this.maxr = r;
	}
	public void setNumSteps(int num){
		this.numSteps = num;
		this.resol = 180/(float)this.numSteps;
	}
	public void setResol(float res){
		this.numSteps = (int) (180/res);
		this.resol = 180/(float)this.numSteps;
		System.out.println("numSteps="+this.numSteps+"  --> resol="+this.resol);
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
	
	/*------------------------ writing methods --------------------*/
	public void writeSphoxels(String filename){
		this.sphoxel.writeSphoxelOutput(filename);
	}
	public void writeTraces(String filename){
		this.nbhsTraces.writeNbhsTracesOutput(filename);
	}
	
	/*------------------------ drawing methods --------------------*/
	
	public void drawSphoxels(Graphics2D g2d){
		Shape shape = null;
		float xPos, yPos;
		double val;
		Color col; // = new Color(24,116,205,255);
		ColorScale scale = new ColorScale();
		
		g2d.setBackground(backgroundColor);
		shape = new Rectangle2D.Float(0, 0, this.pixelWidth*this.ratios[0].length, this.pixelHeight*this.ratios.length);
		g2d.setColor(backgroundColor);
		g2d.draw(shape);
		g2d.fill(shape);
		
//		System.out.println("drawSphoxels ratios.length= "+ratios.length);
		
		// ----- color representation -----
		for(int i=0;i<ratios.length;i++){
			for(int j=0;j<ratios[i].length;j++){
				xPos = j*pixelWidth +this.border;
				yPos = i*pixelHeight +this.border;
//				shape = new Rectangle2D.Float(xPos+this.border, yPos+this.border+this.yBorderThres, pixelWidth, pixelHeight);
				shape = new Rectangle2D.Float(xPos, yPos, pixelWidth, pixelHeight);
				val = ratios[i][j]; // add some scaling and shifting --> range 0:255
				// ----- remove outliers
				if (this.removeOutliers){
					if (this.minRatio<this.minAllowedRat)
						this.minRatio = this.minAllowedRat;
					if (this.maxRatio>this.maxAllowedRat)
						this.maxRatio = this.maxAllowedRat;
					if (val<this.minAllowedRat)
						val = this.minAllowedRat;
					else if (val>this.maxAllowedRat)
						val = this.maxAllowedRat;
				}
								
				// ----- compute alpha and set color	
				float alpha = (float) (Math.abs(val)/Math.abs(minRatio));
				if (alpha>1.0f) alpha=1.0f;
				if (alpha<0.0f) alpha=0.0f;
//				val = -val;
				col = scale.getColor4BlueRedScale(val,alpha);
				
				g2d.setColor(col);
//				g2d.setColor(Color.black); // Test purpose
				g2d.draw(shape);
//				g2d.setColor(col);
				g2d.fill(shape);
			}
		}
	}
	
	public void drawNbhsTraces(Graphics2D g2d){
		Shape line = null;
		Shape circle = null;
		float xPos, yPos, xPosNB=0, yPosNB=0;
		int gID, iNum, jNum, gIdNB, iNumNB, jNumNB, jResID, jSSType;
		float[] node, nbNode;
		float radius = 3.f;
		int lineID = 0;
		int nodeID = 0;
		String nodeName;
		float xPosINum = this.voxelsize*(float)(Math.PI);
		float yPosINum = this.voxelsize*(float)(Math.PI/2);
		
		ColorScale scale = new ColorScale();
		
		g2d.setColor(Color.blue);
		
		Font f = new Font("Dialog", Font.PLAIN, 12);
		g2d.setFont(f);
		
		if (this.nbhsNodes.size()>0){
			for(int i=0; i<this.nbhsNodes.size(); i++){
				node = (float[]) this.nbhsNodes.get(i);
//				System.out.println("0:graphi_id + '\t' + 1:i_num + '\t' + 2:j_num + '\t' + 3:theta + '\t' + 4:phi");
				gID = (int) node[0];
				iNum = (int) node[1];
				jNum = (int) node[2];
				xPos = (float) ((Math.PI+node[4]) *this.voxelsize) +this.border;
				yPos = node[3]*this.voxelsize +this.border;
				jResID = (int) node[5];
				jSSType = (int) node[6];
				// ---- draw geometric object for each residue
				circle = new Ellipse2D.Float( xPos-radius, yPos-radius+this.yBorderThres,2*radius, 2*radius);
				switch (jSSType){
					case 0: // 'H'
						g2d.setColor(Color.magenta); break;
					case 1: // 'S'
						g2d.setColor(Color.yellow); break;
					case 2: // 'O'
						g2d.setColor(Color.cyan); break;
				}		
				g2d.draw(circle);
				g2d.fill(circle);
				nodeName = Character.toString(this.aas[jResID]) +" "+ String.valueOf(jNum-iNum);
				g2d.setColor(Color.black);
				g2d.drawString(nodeName, xPos+radius, yPos+radius+this.yBorderThres);	
				
				while (this.numNodesPerLine[lineID]==0){
					lineID++;
					nodeID = 0;
				}
				int j=i+1;
				
				if (j<this.nbhsNodes.size()){				
					// --- gradient color edges between connected nodes
					nbNode = (float[]) this.nbhsNodes.get(j);
					xPosNB = (float) ((Math.PI+nbNode[4])*this.voxelsize) +this.border;
					yPosNB = nbNode[3]*this.voxelsize +this.border;
					if (node[0]==nbNode[0] && node[1]==nbNode[1]){
//						System.out.print(nodeID +"\t" + lineID +"\t" + this.numNodesPerLine[lineID] +"\t");
						float ratio = (float)(nodeID+1)/(float)this.numNodesPerLine[lineID];
						g2d.setColor(scale.getColor4RGBscale(ratio, 1.0f, 1));
						
						gIdNB = (int) nbNode[0];
						iNumNB = (int) nbNode[1];
						jNumNB = (int) nbNode[2];
						if (gID==gIdNB && iNum==iNumNB){						
							// -- shortrange scaling: |jNum-iNum|>ShortRangeThreshold --> blue
							int thres1 = 6; // 1-6:short range  6-25:middle range  25-n:long range
							int thres2 = 25;
							Color col = Color.black;
							if (Math.abs(jNum-iNum)<=thres1){
								ratio = +1 * (float)Math.abs(jNum-iNum)/(float)(thres1);
								// scale on range 0.2:0.8
								ratio = 0.2f + (ratio*(0.8f-0.2f));
								col = scale.getColor4GreyValueRange(ratio, 1);
							}
							else if (Math.abs(jNum-iNum)<=thres2){
								if (jNum-iNum < 0)
									ratio = -1 * (float)(Math.abs(jNum-iNum)-thres1)/(float)(thres2-thres1);
								else 
									ratio = +1 * (float)(Math.abs(jNum-iNum)-thres1)/(float)(thres2-thres1);
								col = scale.getColor4HotColdScale(ratio, 1.0f);
							}
							else {
								if (jNum-iNum < 0)
									ratio = -1.0f;
								else 
									ratio = +1.0f;
								col = scale.getColor4HotColdScale(ratio, 1.0f);
							}
							System.out.print(iNum+"_"+jNum+"_"+Math.abs(jNum-iNum)+":"+ratio +"\t");
							g2d.setColor(col);					
							
							
							if (iNum>jNum && iNum<jNumNB && this.paintCentralResidue){
								// paint central residue
								circle = new Ellipse2D.Float( xPosINum-radius, yPosINum+this.yBorderThres-radius,2*radius, 2*radius);
								
								line = new Line2D.Float(xPos, yPos+this.yBorderThres, xPosINum, yPosINum+this.yBorderThres);		
								g2d.draw(line);
								line = new Line2D.Float(xPosINum, yPosINum+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
								g2d.draw(line);
							}
							else {
								// -- test for distance
								if (Math.abs(xPosNB-xPos) > this.xDim/2){
									if (xPos<xPosNB){
										line = new Line2D.Float(xPos, yPos+this.yBorderThres, xPosNB-this.xDim, yPosNB+this.yBorderThres);		
										g2d.draw(line);
										line = new Line2D.Float(xPos+this.xDim, yPos+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
										g2d.draw(line);
									}
									else{
										line = new Line2D.Float(xPos-this.xDim, yPos+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
										g2d.draw(line);
										line = new Line2D.Float(xPos, yPos+this.yBorderThres, xPosNB+this.xDim, yPosNB+this.yBorderThres);		
										g2d.draw(line);
									}
								}
								else 
								{
									line = new Line2D.Float(xPos, yPos+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
									g2d.draw(line);
								}
							}	
						}									
						nodeID++;
					}	
					else {
						lineID++;
					    System.out.println();
						nodeID = 0;
					}

				}
			}	
		}		
		
	}
	
	protected void drawCrosshair(Graphics2D g2d){
		// only in case of range selection we draw a diagonal cursor
		// drawing the cross-hair
		g2d.setColor(crosshairColor);
//		g2d.drawLine(mousePos.x, 0, mousePos.x, outputSize);
//		g2d.drawLine(0, mousePos.y, outputSize, mousePos.y);
//		int bcenterx = mousePos.y;
//		int bcentery = mousePos.x;
		g2d.drawLine(mousePos.x, 0, mousePos.x, g2dSize.height);
		g2d.drawLine(0, mousePos.y, g2dSize.width, mousePos.y);
		
//		int bcenterx = mousePos.x;
//		int bcentery = mousePos.y;
		
//		System.out.println("MousPos= " +mousePos.x+", "+mousePos.y);
		
//		g2d.drawLine(bcenterx-30,bcentery, bcenterx+30,bcentery);
//		g2d.drawLine(bcenterx,bcentery-30, bcenterx,bcentery+30);
//		g2d.drawArc(bcenterx-30, bcentery-30, 60, 60,0,360);
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
//		System.out.println(phiRanges.size()+"=?"+thetaRanges.size()+"=?"+selContacts.size());
		System.out.println("Coordinates Occupied Rectangle:");
		Iterator<Pair<Float>> itrP = this.phiRanges.iterator();
		Iterator<Pair<Float>> itrT = this.thetaRanges.iterator();
//		Iterator<Pair<Integer>> itrC = selContacts.iterator();
		while (itrP.hasNext()){
			g2d.setColor(selAngleRangeColor);
			Pair<Float> phi = (Pair<Float>) itrP.next();
			Pair<Float> theta = (Pair<Float>) itrT.next();
//			Pair<Integer> con = (Pair<Integer>) itrC.next();
			System.out.println("COR: "+phi.getFirst()+","+theta.getFirst()+" to "+phi.getSecond()+","+theta.getSecond());			
			float xS = phi.getFirst() * this.voxelsize;
			float xE = phi.getSecond() * this.voxelsize;
			float yS = theta.getFirst() * this.voxelsize;
			float yE = theta.getSecond() * this.voxelsize;
			System.out.println(xS+","+yS+" to "+xE+","+yE);
			shape = new Rectangle2D.Float(xS, yS, xE-xS, yE-yS);
			g2d.draw(shape);
//			g2d.fill(shape);
		}
//		for(Pair<Float> phi:phiRanges) {}
		
	}
	
	private void drawSelectedAngleRange(){
		this.contStatBar.getAnglePanel().setIRes(String.valueOf(this.iRes));
		this.contStatBar.getAnglePanel().setJRes(String.valueOf(this.jRes));	
		
		float val = (float)(Math.round(this.tmpPhiRange.getFirst()*100))/100;
		this.contStatBar.getAnglePanel().setPhiMin(String.valueOf(val));	
		val = (float)(Math.round(this.tmpAngleComb.getFirst()*100))/100;
		this.contStatBar.getAnglePanel().setPhiMax(String.valueOf(val));	
		val = (float)(Math.round(this.tmpThetaRange.getFirst()*100))/100;
		this.contStatBar.getAnglePanel().setThetaMin(String.valueOf(val));	
		val = (float)(Math.round(this.tmpAngleComb.getSecond()*100))/100;
		this.contStatBar.getAnglePanel().setThetaMax(String.valueOf(val));
		
		this.contStatBar.repaint();		
	}
	
	// end drawing methods
	
	
	/**
	 * Returns the corresponding theta-phi values in the sphoxelView given screen
	 * coordinates
	 */
	private Pair<Float> screen2A(Point point){
		
		Pair<Float> floatP = new Pair<Float>((float)(point.x)/this.voxelsize,(float)(point.y)/this.voxelsize);
//		System.out.println("scree2A_point "+(point.x)+","+(point.y));
//		System.out.println("scree2A_pair "+(float)(point.x)/this.voxelsize+","+(float)(point.y)/this.voxelsize);
		
		return floatP;
	}
	
	/**
	 * Update tmpContact with the contacts contained in the rectangle given by
	 * the upperLeft and lowerRight.
	 */
	private void squareSelect(){
		Pair<Float> upperLeft = screen2A(mousePressedPos);
		Pair<Float> lowerRight = screen2A(mouseDraggingPos);
		// we reset the tmpContacts list so every new mouse selection starts
		// from a blank list
//		tmpContacts = new IntPairSet();

		float pmin = Math.min(upperLeft.getFirst(),lowerRight.getFirst());
		float tmin = Math.min(upperLeft.getSecond(),lowerRight.getSecond());
		float pmax = Math.max(upperLeft.getFirst(),lowerRight.getFirst());
		float tmax = Math.max(upperLeft.getSecond(),lowerRight.getSecond());

		System.out.println("squareSelect "+pmin+"-"+pmax+" , "+tmin+"-"+tmax);
		
		this.tmpPhiRange = new Pair<Float>(pmin, pmax);
		this.tmpThetaRange = new Pair<Float>(tmin, tmax);
//		this.tmpSelContact = new Pair<Integer>(this.AAStr.indexOf(this.iRes),this.AAStr.indexOf(this.jRes));
		this.tmpSelContact = new Pair<Integer>(this.iNum,this.jNum);
		
		this.tmpAngleComb = new Pair<Float>(pmax, tmax);

	}
	
//	/** Called by ResidueRuler to enable display of ruler "crosshair" */	
//	public void showRulerCrosshair() {
//		showRulerCrosshair = true;
//	}
//	/** Called by ResidueRuler to switch off showing ruler coordinates */
//	public void hideRulerCoordinate() {
//		showRulerCoord = false;
//	}

	/**
	 * Main method to draw the component on screen. This method is called each
	 * time the component has to be (re) drawn on screen. It is called
	 * automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	@Override
	protected synchronized void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();
		
		this.screenSize = this.contactView.getScreenSize();
//		System.out.println("ContactPane paintComponent HxW: "+this.screenSize.height+"x"+this.screenSize.width);
//		System.out.println("ContactPane pC HxW: "+this.getSize().height+"x"+this.getSize().width+"  --> ratio="+(float)(this.getSize().height)/(float)(this.getSize().width));
		
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
		
//		Dimension dim = this.contactView.getSize();		
//		this.xDim = (int) dim.getWidth();
//		this.yDim = (int) dim.getHeight();
		
		// update pixel dimensions
		this.pixelWidth = (this.xDim-2*this.border)/(2*this.numSteps) ;
		this.pixelHeight =  this.pixelWidth; //(this.yDim-2*this.border-this.yBorderThres)/(2*this.numSteps);
		this.voxelsize = (float) (this.numSteps*this.pixelWidth/Math.PI);
		
		g2d.setBackground(this.backgroundColor);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		drawSphoxels(g2d);
		drawNbhsTraces(g2d);
		// drawing selection rectangle if dragging mouse and showing temp
		// selection in red (tmpContacts)
		if (dragging && contactView.getGUIState().getSelectionMode()==ContactGUIState.SelMode.RECT) {
			g2d.setColor(squareSelColor);
			int xmin = Math.min(mousePressedPos.x,mouseDraggingPos.x);
			int ymin = Math.min(mousePressedPos.y,mouseDraggingPos.y);
			int xmax = Math.max(mousePressedPos.x,mouseDraggingPos.x);
			int ymax = Math.max(mousePressedPos.y,mouseDraggingPos.y);
			g2d.drawRect(xmin,ymin,xmax-xmin,ymax-ymin);
		} 
		drawCrosshair(g2d);
		drawOccupiedAngleRanges(g2d);
		drawSelectedAngleRange();
	}
	
	/*---------------------------- setters and getters -----------------------------*/
	
	public void setStatusBar(ContactStatusBar statusBar) {
		this.contStatBar = statusBar;		
	}
	public ContactStatusBar getSatusBar(){
		return this.contStatBar;
	}
	
	public Dimension getScreenSize(){
		return this.screenSize;
	}
	
	public Dimension getPanelSize(){
		return this.g2dSize;
	}
	
	/*---------------------------- mouse events -----------------------------*/

	public void mousePressed(MouseEvent evt) {
		// This is called when the user presses the mouse anywhere
		// in the frame
		
		System.out.println("mousePressed");

		lastMouseButtonPressed = evt.getButton();
		mousePressedPos = evt.getPoint();
		if(lastMouseButtonPressed == MouseEvent.BUTTON2) dragging = false;
	}

	public void mouseReleased(MouseEvent evt) {
		System.out.println("mouseReleased");

		// Called whenever the user releases the mouse button.
		// TODO: Move much of this to MouseClicked and pull up Contact cont = screen2A...
		if (evt.isPopupTrigger()) {
			dragging = false;
			return;
		}
		// only if release after left click (BUTTON1)
		if (evt.getButton()==MouseEvent.BUTTON1) {

			switch (contactView.getGUIState().getSelectionMode()) {
			case RECT:
				if (dragging){
//					squareSelect();
					if (this.selContacts.size()>0 && 
							this.selContacts.lastElement().getFirst()==this.tmpSelContact.getFirst() && 
							this.selContacts.lastElement().getSecond()==this.tmpSelContact.getSecond()){
						this.phiRanges.removeElementAt(this.phiRanges.size()-1);
						this.thetaRanges.removeElementAt(this.thetaRanges.size()-1);
						this.selContacts.removeElementAt(this.selContacts.size()-1);						
					}
					this.phiRanges.add(this.tmpPhiRange);
					this.thetaRanges.add(this.tmpThetaRange);
					this.selContacts.add(this.tmpSelContact);
					updateAngleRange();
				}				
				dragging = false;
//				this.repaint();
				return;
				
			case CLUSTER:
//				// resets selContacts when clicking mouse

//				this.repaint();
				return;

			}
			
			this.tmpPhiRange = new Pair<Float>(0.0f, 0.0f);
			this.tmpThetaRange = new Pair<Float>(0.0f, 0.0f);
		}
	}

	public void mouseDragged(MouseEvent evt) {
//		System.out.println("mouseDragged");

		// Called whenever the user moves the mouse
		// while a mouse button is held down.

		if(lastMouseButtonPressed == MouseEvent.BUTTON1) {
			dragging = true;
			mouseDraggingPos = evt.getPoint();
			switch (contactView.getGUIState().getSelectionMode()) {
			case RECT:
				squareSelect();
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
//		System.out.println("mouseExited");

		mouseIn = false;
		this.repaint();
	}

	public void mouseClicked(MouseEvent evt) {
	}

	public void mouseMoved(MouseEvent evt) {
//		System.out.println("mouseMoved");

		mousePos = evt.getPoint();
		
		Pair<Float> pos = screen2A(mousePos);
		this.tmpAngleComb = new Pair<Float>(pos.getFirst(), pos.getSecond());
		this.repaint();
	}
}
