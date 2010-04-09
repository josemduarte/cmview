package cmview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.JPanel;

import owl.core.structure.graphs.RIGNbhood;
import owl.core.structure.graphs.RIGNode;
import owl.core.util.IntPairSet;
import owl.gmbp.CMPdb_nbhString_traces;
import owl.gmbp.CMPdb_sphoxel;

import cmview.datasources.Model;
import edu.uci.ics.jung.graph.util.Pair;

public class ContactPane extends JPanel{
	
/*--------------------------- member variables --------------------------*/
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// underlying data
	private Model mod;
	private ContactMapPane cmPane;
	private ContactView contactView;
	
	private Dimension screenSize;			// current size of this component on screen
	
	// query data
	private char iRes='A', jRes='A', ssType='H';
	private String iResType="Ala", jResType="Ala";
	private int iNum=0, jNum=0;
	private String nbhString, nbhStringL;
	private String jAtom = "CA";
	
	private String host = "talyn";
	private String username = "vehlow";
	private String password = "nieve";
	private String db = "bagler_all5p0";
	
	// Sphoxel-Data
	private CMPdb_sphoxel sphoxel;
	private double [][] ratios;
	private double [][][] bayesRatios;
	private double minRatio = 0;
	private double maxRatio = 0;
	// NBHStraces-Data
	private CMPdb_nbhString_traces nbhsTraces;
	private Vector<float[]> nbhsNodes;
	private int[] numNodesPerLine;
	private int[] maxDistsJRes, minDistsJRes;
	private int numLines;
	
	// variables for representation (drawing methods)
	private int numSteps = 16; // change later via interface
	private final int border = 15;
	private final int yBorderThres = 45;
	private int pixelWidth = 5*36/this.numSteps; // change in dependence of numSteps
	private int pixelHeight = this.pixelWidth; //5*36/this.numSteps;	
	private float voxelsize = (float) (this.numSteps*this.pixelWidth/Math.PI); //pixelWidth;

	private boolean removeOutliers = false;
	private double minAllowedRat = -3;
	private double maxAllowedRat = 1;
	
	private boolean paintCentralResidue = true;
	
	private int xDim=0, yDim=0;
	
	private final char[] aas = new char[]{'A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y'}; // possible Residues
	

	public ContactPane(Model mod, ContactMapPane cmPane, ContactView contactView) throws SQLException{
		this.mod = mod;
		this.cmPane = cmPane;
		this.contactView = contactView;
		
		// Get first shell neighbours of involved residues
		Pair<Integer> currentResPair = this.cmPane.getmousePos();
		this.iNum = currentResPair.getFirst();
		this.jNum = currentResPair.getSecond();
		System.out.println("first:"+this.iNum+"  second:"+this.jNum);
		
		if(this.iNum==this.jNum){
			IntPairSet firstShellNbrs1 = this.cmPane.getfirstShellNbrRels(this.mod, this.iNum);
			
			if( firstShellNbrs1.isEmpty() ) {
				return; // nothing to do!
			}		
			
		} else{
			IntPairSet firstShellNbrs1 = this.cmPane.getfirstShellNbrRels(this.mod, this.iNum);
			IntPairSet firstShellNbrs2 = this.cmPane.getfirstShellNbrRels(this.mod, this.jNum);
			
			if( firstShellNbrs1.isEmpty() || firstShellNbrs2.isEmpty()) {
				return; // nothing to do!
			}
		}
		// use pair to get iRes and jRes, isstype, nbhstring
		RIGNode nodeI = this.mod.getNodeFromSerial(this.iNum); //this.mod.getGraph().getNodeFromSerial(this.iNum);
		RIGNode nodeJ = this.mod.getNodeFromSerial(this.jNum);		
		this.iRes = nodeI.toString().charAt(nodeI.toString().length()-1);
		this.jRes = nodeJ.toString().charAt(nodeJ.toString().length()-1);
		System.out.println("nodeI="+nodeI.toString()+"-->"+iRes+"  nodeI="+nodeJ.toString()+"-->"+jRes);
		
		this.iResType = nodeI.getResidueType();
		this.jResType = nodeJ.getResidueType();
		System.out.println("iresType: "+this.iResType+"  jresType: "+this.jResType);
		System.out.println("i secStrucElement: "+nodeI.getSecStrucElement());
		System.out.println("j secStrucElement: "+nodeJ.getSecStrucElement());
		
		RIGNbhood nbhood = this.mod.getGraph().getNbhood(nodeI);
		this.nbhString = nbhood.getNbString();
		this.nbhStringL = "%";
		for (int i=0; i<this.nbhString.length(); i++){
			this.nbhStringL += this.nbhString.charAt(i);
			this.nbhStringL += "%";
		}
		System.out.println(this.nbhString+"-->"+this.nbhStringL);	
		
		// Definition of sstype and jatom
		
		// compute sphoxeldata
		this.sphoxel = new CMPdb_sphoxel(this.iRes, this.jRes, this.host, this.username, this.password, this.db);
		sphoxel.setDiffSSType(true); // set to true if you want to differentiate ssType
		sphoxel.setSSType('H');
		sphoxel.setNumSteps(this.numSteps); // choose number of steps for resolution
		sphoxel.runBayes();
		System.out.println("BayesRatios computed");
		this.ratios = sphoxel.getRatios();
		this.bayesRatios = sphoxel.getBayesRatios();
		this.minRatio = sphoxel.getMinRatio();
		this.maxRatio = sphoxel.getMaxRatio();
		// compute nbhstringTraces
		this.nbhsTraces = new CMPdb_nbhString_traces(this.nbhStringL, this.jAtom, this.host, this.username, this.password, this.db);
		nbhsTraces.setDiffSSType(true);
		nbhsTraces.setSSType('H');
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
				System.out.print(this.numNodesPerLine[i]+"_"+this.minDistsJRes[i]+"_"+this.maxDistsJRes[i]+"\t");
			}
			System.out.println();
		}		
		
		this.xDim = 2*this.numSteps*this.pixelWidth + 2*this.border;
		this.yDim = this.numSteps*this.pixelHeight + 2*this.border + this.yBorderThres;
//		this.yDim = this.numSteps*this.pixelHeight + 2*this.border + 45;	
		System.out.println("numSteps="+this.numSteps+" pixelS="+this.pixelHeight+" Dim="+this.xDim+"x"+this.yDim);
		System.out.println("voxelsize= "+this.voxelsize);
//		this.xDim = (int) Math.round(this.voxelsize*2*Math.PI) + 2*this.border;
//		this.yDim = (int) Math.round(this.voxelsize*Math.PI) + 2*this.border +this.yBorderThres;	
//		System.out.println("Traces Dim="+this.xDim+"x"+this.yDim);	
	
		this.screenSize = new Dimension(this.xDim,this.yDim);
		this.contactView.setPreferredSize(this.screenSize);
		this.setSize(screenSize);
	}
	
	/*------------------------ writing methods --------------------*/
	public void writeSphoxels(String filename){
		this.sphoxel.writeSphoxelOutput(filename);
	}
	public void writeTraces(String filename){
		this.nbhsTraces.writeNbhsTracesOutput(filename);
	}
	
	/*------------------------ drawing methods --------------------*/
	
	public void drawSphoxels(Graphics2D g2){
		Shape shape = null;
		float xPos, yPos;
		double val;
		Color col; // = new Color(24,116,205,255);
		ColorScale scale = new ColorScale();
		
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
				int alpha = 0;
				if(val<0)
					alpha = (int)Math.round((255*Math.abs(val)/Math.abs(minRatio))+0.5f);
				else
					alpha = (int)Math.round((255*Math.abs(val)/Math.abs(maxRatio))+0.5f);
				if (alpha>255) alpha--;
				if (alpha<0) alpha++;
//				val = -val;
				col = scale.getColor4BlueRedScale(val,alpha);
				
				g2.setColor(col);
				
				g2.draw(shape);
				g2.fill(shape);
			}
		}
	}
	
	public void drawNbhsTraces(Graphics2D g2){
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
		
		g2.setColor(Color.blue);
		
		Font f = new Font("Dialog", Font.PLAIN, 12);
		g2.setFont(f);
		
		for(int i=0; i<this.nbhsNodes.size(); i++){
			node = (float[]) this.nbhsNodes.get(i);
//			System.out.println("0:graphi_id + '\t' + 1:i_num + '\t' + 2:j_num + '\t' + 3:theta + '\t' + 4:phi");
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
					g2.setColor(Color.magenta); break;
				case 1: // 'S'
					g2.setColor(Color.yellow); break;
				case 2: // 'O'
					g2.setColor(Color.cyan); break;
			}		
			g2.draw(circle);
			g2.fill(circle);
			nodeName = Character.toString(this.aas[jResID]) +" "+ String.valueOf(jNum-iNum);
			g2.setColor(Color.black);
			g2.drawString(nodeName, xPos+radius, yPos+radius+this.yBorderThres);	
			
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
//					System.out.print(nodeID +"\t" + lineID +"\t" + this.numNodesPerLine[lineID] +"\t");
					float ratio = (float)(nodeID+1)/(float)this.numNodesPerLine[lineID];
					g2.setColor(scale.getColor4RGBscale(ratio, 1.0f, 1));
					
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
						g2.setColor(col);					
						
						
						if (iNum>jNum && iNum<jNumNB && this.paintCentralResidue){
							// paint central residue
							circle = new Ellipse2D.Float( xPosINum-radius, yPosINum+this.yBorderThres-radius,2*radius, 2*radius);
							
							line = new Line2D.Float(xPos, yPos+this.yBorderThres, xPosINum, yPosINum+this.yBorderThres);		
							g2.draw(line);
							line = new Line2D.Float(xPosINum, yPosINum+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
							g2.draw(line);
						}
						else {
							// -- test for distance
							if (Math.abs(xPosNB-xPos) > this.xDim/2){
								if (xPos<xPosNB){
									line = new Line2D.Float(xPos, yPos+this.yBorderThres, xPosNB-this.xDim, yPosNB+this.yBorderThres);		
									g2.draw(line);
									line = new Line2D.Float(xPos+this.xDim, yPos+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
									g2.draw(line);
								}
								else{
									line = new Line2D.Float(xPos-this.xDim, yPos+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
									g2.draw(line);
									line = new Line2D.Float(xPos, yPos+this.yBorderThres, xPosNB+this.xDim, yPosNB+this.yBorderThres);		
									g2.draw(line);
								}
							}
							else 
							{
								line = new Line2D.Float(xPos, yPos+this.yBorderThres, xPosNB, yPosNB+this.yBorderThres);		
								g2.draw(line);
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

	/**
	 * Main method to draw the component on screen. This method is called each
	 * time the component has to be (re) drawn on screen. It is called
	 * automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	@Override
	protected synchronized void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		
		g2.setBackground(Color.blue);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		drawSphoxels(g2);
		drawNbhsTraces(g2);
	}
}
