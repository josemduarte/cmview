package cmview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.sql.SQLException;

import javax.swing.JPanel;

import owl.core.structure.graphs.RIGNbhood;
import owl.core.structure.graphs.RIGNode;
import owl.core.util.IntPairSet;
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
	
	private String host = "talyn";
	private String username = "vehlow";
	private String password = "nieve";
	private String db = "bagler_all5p0";
	
	// Sphoxel-Data
	private double [][] ratios;
	private double [][][] bayesRatios;
	private double minRatio = 0;
	private double maxRatio = 0;
	
	// variables for representation (drawing methods)
	private int numSteps = 4; // change later via interface
	private final int border = 15;
	private final int yBorderThres = 45;
	private int pixelWidth = 5*36/this.numSteps; // change in dependence of numSteps
	private int pixelHeight = 5*36/this.numSteps;	

	private boolean removeOutliers = false;
	private double minAllowedRat = -3;
	private double maxAllowedRat = 1;
	
	private int xDim=0, yDim=0;
	

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
		
		// compute sphoxeldata
		CMPdb_sphoxel sphoxel = new CMPdb_sphoxel(this.iRes, this.jRes, this.host, this.username, this.password, this.db);
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
		
		this.xDim = 2*this.numSteps*this.pixelWidth + 2*this.border;
		this.yDim = this.numSteps*this.pixelHeight + 2*this.border + this.yBorderThres;
//		this.yDim = this.numSteps*this.pixelHeight + 2*this.border + 45;
		
		System.out.println("numSteps="+this.numSteps+" pixelS="+this.pixelHeight+" Dim="+this.xDim+"x"+this.yDim);
	
		this.screenSize = new Dimension(this.xDim,this.yDim);
		this.contactView.setPreferredSize(this.screenSize);
		this.setSize(screenSize);
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
				xPos = j*pixelWidth;
				yPos = i*pixelHeight;
//				shape = new Rectangle2D.Float(xPos+this.border, yPos+this.border+this.yBorderThres, pixelWidth, pixelHeight);
				shape = new Rectangle2D.Float(xPos+this.border, yPos+this.border, pixelWidth, pixelHeight);
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
	}
}
