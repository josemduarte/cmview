package cmview.gmbp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

public class AngleRuler extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected static final int TOP = 1;
	protected static final int BOTTOM = 2;
	protected static final int LEFT = 3;
	protected static final int RIGHT = 4; 
	
	protected static final int STD_RULER_WIDTH = 30;
	protected static final int DEGREES_PHI = 360;
	protected static final int DEGREES_THETA = 180;
	protected static final int DEGREE_RESOL = 10;
	
	private static final Color BACKGROUND_COLOR = Color.yellow;		// if opaque, otherwise transparent
	
//	private ContactView cview;
	private ContactPane cpane;
	private int rulerWidth;
	private int rulerLength;
//	private int contactMapSize;
	private Dimension screenSize;			// current size of this component on screen
	private Dimension rulerSize;
//	private Dimension panelSize;
//	private double ratioX, ratioY;
	private int offSetX, offSetY;
	private double deltaOffSetX, deltaOffSetXEnd, deltaOffSetXCenter;
	
	private int location; 
	
	public AngleRuler(ContactView cview, ContactPane cpane, int location){
//		this.cview = cview;
		this.cpane = cpane;
		this.location = location;
		this.offSetX = 0;
		this.offSetY = 0;		
		this.screenSize = cview.getSize(); // cview.getScreenSize();
//		this.panelSize = cpane.getSize(); //cpane.getPanelSize();
		this.rulerSize = new Dimension(0, 0);
		
		this.rulerWidth = STD_RULER_WIDTH;
		
		if (this.location == TOP || this.location == BOTTOM){
//			this.rulerLength = this.cview.getSize().width;
//			this.rulerWidth = this.cview.getSize().height;
			this.offSetX = this.rulerWidth;
			this.offSetY = 0;
			this.rulerLength = this.cpane.getSize().width + (int)this.offSetX;
			this.rulerSize = new Dimension(this.rulerLength, this.rulerWidth);
			this.setSize(new Dimension(this.rulerLength, this.rulerWidth));
		}
		else { // this.location == LEFT || RIGHT
//			this.rulerLength = this.cview.getSize().height;
//			this.rulerWidth = this.cview.getSize().width;
			this.rulerLength = this.cpane.getSize().height;
			this.offSetX = 0;
			this.offSetY = this.rulerWidth;
			this.rulerSize = new Dimension(this.rulerWidth, this.rulerLength);
			this.setSize(new Dimension(this.rulerWidth,this.rulerLength));
		}
		
	}
	
	protected synchronized void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();
			
		this.cpane.repaint();
		System.out.println("paintComponentRuler");
		
//		System.out.println("CView HxB: "+this.cview.getSize().height+"x"+this.cview.getSize().width);
//		System.out.println("CPane HxB: "+this.cpane.getSize().height+"x"+this.cpane.getSize().width);
//		System.out.println("CPane g2dSize HxB: "+this.cpane.getPanelSize().height+"x"+this.cpane.getSize().width);		
		
		/// --- Update Ruler Size ----
		
		this.rulerWidth = STD_RULER_WIDTH;
		
		if (this.location == TOP || this.location == BOTTOM){
//			this.rulerLength = this.cview.getSize().width;
//			this.rulerWidth = this.cview.getSize().height;
			if (this.cpane.getMapProjType() == this.cpane.kavrayskiyMapProj){ //(this.mapProjType==kavrayskiyMapProj){
				this.offSetX = this.rulerWidth; // + (int)this.deltaOffSetX;
//				this.deltaOffSetX = this.cpane.getScreenPosFromRad(0, 0).getFirst();
				this.deltaOffSetX = this.cpane.getDeltaOffSetX() + this.offSetX;
				this.deltaOffSetXEnd = this.cpane.getDeltaOffSetXEnd() + this.offSetX;
				this.deltaOffSetXCenter = this.cpane.getDeltaOffSetXCenter() + this.offSetX;
//				this.deltaOffSetX = 409.3648026452222 + this.offSetX;
//				this.deltaOffSetXEnd = 1030.2930269270032 + this.offSetX;
			}
			else if(this.cpane.getMapProjType() == this.cpane.cylindricalMapProj){
				this.offSetX = this.rulerWidth;
//				this.deltaOffSetX = this.cpane.getScreenPosFromRad(0, 0).getFirst();
				this.deltaOffSetX = 0 + this.offSetX;
				this.deltaOffSetXEnd = this.rulerLength;
			}
			this.offSetY = 0;
//			this.rulerLength = this.cpane.getSize().width + this.offSetX;
			this.rulerLength = this.cpane.getPanelSize().width + this.offSetX;
			this.rulerSize = new Dimension(this.rulerLength, this.rulerWidth);
			this.setSize(new Dimension(this.rulerLength, this.rulerWidth));
		}
		else { // this.location == LEFT || RIGHT
//			this.rulerLength = this.cview.getSize().height;
//			this.rulerWidth = this.cview.getSize().width;
//			this.rulerLength = this.cpane.getSize().height;
			this.rulerLength = this.cpane.getPanelSize().height;
			this.offSetX = 0;
			this.offSetY = this.rulerWidth;
			this.rulerSize = new Dimension(this.rulerWidth, this.rulerLength);
			this.setSize(new Dimension(this.rulerWidth,this.rulerLength));
		}
		
//		this.ratioX = (double)this.rulerLength/this.screenSize.width;
//		this.ratioY = (double)this.rulerWidth/this.screenSize.height;
		
//		this.ratioX = (double)this.rulerLength/this.panelSize.width;
//		this.ratioY = (double)this.rulerWidth/this.panelSize.height;
		
		
//		System.out.println("ratioX= "+this.ratioX+"   ratioY= "+this.ratioY);
//		System.out.println("offSetX= "+this.offSetX+"   offSetY= "+this.offSetY);
		
		g2d.setBackground(BACKGROUND_COLOR);

		drawRulerGrid(g2d);	
	}
	
	public void drawRulerGrid(Graphics2D g2d){
		Shape shape = null;
		Shape tick = null;
		String degreeS = "0";
		int degree = 0;
		float xS, xE, yS, yE;
		float delta = 0;
		int numTicks = 0;		
		xS = 0.0f;
		xE = this.rulerLength;
		yS = 0.0f;
		yE = this.rulerLength;
		if (this.location == TOP || this.location == BOTTOM){
			xS = 0.0f + this.offSetX;
			xE = this.rulerLength + this.offSetX;
			yS = 0.0f + this.offSetY;
			yE = this.rulerWidth + this.offSetY;
//			Color col = new Color(255, 0, 0, 255);
			Color col = new Color(255, 255, 255, 255);
			g2d.setColor(col);
			numTicks = DEGREES_PHI/DEGREE_RESOL;				
			shape = new Rectangle2D.Float(xS-this.offSetX, yS, xE-xS, yE-yS);	
			if (this.cpane.getMapProjType() == this.cpane.kavrayskiyMapProj){
				delta = (float) (this.rulerLength-this.offSetX-(2*this.deltaOffSetX)) / (float) numTicks;
				delta = (float) ((this.deltaOffSetXEnd-this.deltaOffSetX)/ (float)(numTicks));
			}
			else if(this.cpane.getMapProjType() == this.cpane.cylindricalMapProj){
				delta = (float) (this.rulerLength-this.offSetX) / (float) numTicks;
			}
		}
		else { // this.location == LEFT || RIGHT
			xS = 0.0f + this.offSetX;
			xE = this.rulerWidth + this.offSetX;
//			yS = 0.0f + this.offSetY;
//			yE = this.rulerLength + this.offSetY;
//			Color col = new Color(0, 255, 0, 255);
			Color col = new Color(255, 255, 255, 255);
			g2d.setColor(col);
			numTicks = DEGREES_THETA/DEGREE_RESOL;
			delta = (float) this.rulerLength / (float) numTicks;
			shape = new Rectangle2D.Float(xS, yS, xE-xS, yE-yS);
		}
//		System.out.println("numTicks= "+numTicks);
//		float delta = (float) this.rulerLength / (float) numTicks;
//		System.out.println(this.location+"  rlength= "+this.rulerLength+"  numTicks= "+numTicks+"  delta= "+delta+"  Coordinates: "+xS+", "+yS+", "+xE+", "+yE);
		
		g2d.draw(shape);
		g2d.fill(shape);
		
		g2d.setColor(Color.black);
//		tick = new Line2D.Float(xS, yS, xE, yE);
//		g2d.draw(tick);
		Font f = new Font("Dialog", Font.PLAIN, 10);
		g2d.setFont(f);

		int i=0;
		float xPosOrig = this.cpane.getOrigCoord().getFirst() * this.cpane.getVoxelsize(); //(this.origCoordinates.getFirst() * this.voxelsize)
		float yPosOrig = this.cpane.getOrigCoord().getSecond() * this.cpane.getVoxelsize(); //(this.origCoordinates.getSecond() * this.voxelsize)
		this.deltaOffSetXCenter = this.cpane.getDeltaOffSetXCenter() + this.offSetX;
		xPosOrig = (float) this.deltaOffSetXCenter;
		int count = 0;
		i=numTicks/2;
		while (count<=numTicks){
			if (this.location == TOP || this.location == BOTTOM){
				if (count==0){
					xS = xPosOrig;
//					xS += (this.offSetX); //+this.deltaOffSetX);
				}
				else
					xS += delta;
				if (xS>this.deltaOffSetXEnd){
//				if (xS>this.rulerLength){
//					xS -= this.rulerLength;
					xS -= this.deltaOffSetXEnd;
					xS += this.deltaOffSetX; //+this.offSetX
				}
				xE = xS;
				yS = this.rulerWidth - (this.rulerWidth/4);
				yE = this.rulerWidth;
			}
			else { // this.location == LEFT || RIGHT
				if (count==0)
					yS = yPosOrig;
				else
					yS += delta;
				if (yS>this.rulerLength)
					yS -= this.rulerLength;
				yE = yS;
				xS = this.rulerWidth - (this.rulerWidth/4);
				xE = this.rulerWidth;
			}
			tick = new Line2D.Float(xS, yS, xE, yE);			
			g2d.draw(tick);
			
			degree = i*DEGREE_RESOL;
			degreeS = String.valueOf(degree);
			float d = 10*((float)degreeS.length()/2);
			if (this.location == TOP || this.location == BOTTOM){
				xS -= d;
				yS = this.rulerWidth - (this.rulerWidth/3); 
				if (this.cpane.getMapProjType()==this.cpane.kavrayskiyMapProj && i%2==0){
					g2d.drawString(degreeS, xS, yS);
				}
				else if(this.cpane.getMapProjType() == this.cpane.cylindricalMapProj)
					g2d.drawString(degreeS, xS, yS);
				xS += d;
			}
			else { // this.location == LEFT || RIGHT
				d = 6;
				yS += d;
				xS = 0;
				g2d.drawString(degreeS, xS, yS);
				yS -= d;
			}
			
			i++;
			if (i>numTicks)
				i -= numTicks;
			count++;
	    }
		
//		for (int j=1; j<=numTicks; j++){
//			if (j==1){
//				float val = (float) (j*(DEGREE_RESOL*Math.PI/180));
//				float transl = 0;
//				if (this.location == TOP || this.location == BOTTOM){
//					transl = this.cpane.translateXCoordRespective2Orig(val);				
//				}
//				else {
//					transl = this.cpane.translateYCoordRespective2Orig(val);
//				}
//				i = (int) (transl/(DEGREE_RESOL*Math.PI/180));
//			}
//			else 
//				i++;	
//			if (i>=numTicks)
//				i -= numTicks;
//			
//			if (this.location == TOP || this.location == BOTTOM){
//				xS = i*delta;
//				xE = xS;
//				xS += this.offSetX;
//				xE += this.offSetX;
//				yS = this.rulerWidth - (this.rulerWidth/4);
//				yE = this.rulerWidth;
//			}
//			else { // this.location == LEFT || RIGHT
//				yS = i*delta;
//				yE = yS;
//				xS = this.rulerWidth - (this.rulerWidth/4);
//				xE = this.rulerWidth;
//			}
//			tick = new Line2D.Float(xS, yS, xE, yE);			
//			g2d.draw(tick);
////			g2d.fill(shape);
//			
////			degree = i*DEGREE_RESOL;
//			degree = j*DEGREE_RESOL;
//			degreeS = String.valueOf(degree);
//			float d = 10*((float)degreeS.length()/2);
//			if (this.location == TOP || this.location == BOTTOM){
//				xS = i*delta - d;
//				xS += this.offSetX;
//				yS = this.rulerWidth - (this.rulerWidth/3); //0;
//			}
//			else { // this.location == LEFT || RIGHT
//				d = 6;
//				yS = i*delta + d;
////				xS = this.rulerWidth/2; 
//				xS = 0;
//			}
//			g2d.drawString(degreeS, xS, yS);
//			
//		}	
	}
	
	/*-------------------------- overridden methods -------------------------*/

	/** Method called by this component to determine its minimum size */
	@Override
	public Dimension getMinimumSize() {
//		return super.getMinimumSize();
		return new Dimension(STD_RULER_WIDTH, 100);
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
	
	public Dimension getRulerSize(){
		return this.rulerSize;
	}

}
