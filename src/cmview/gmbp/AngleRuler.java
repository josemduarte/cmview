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

public class AngleRuler extends JPanel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected static final int TOP = 1;
	protected static final int BOTTOM = 2;
	protected static final int LEFT = 3;
	protected static final int RIGHT = 4; 
	
	protected static final int STD_RULER_WIDTH = 30;
	protected static final int DEGREES_LAMPDA = 360;
	protected static final int DEGREES_PHI = 180;
	protected static final int DEGREE_RESOL = 10;
	
	private static final Color BACKGROUND_COLOR = Color.yellow;		// if opaque, otherwise transparent
	
	private ContactPane cpane;
	private int rulerWidth;
	private int rulerLength;
	private Dimension screenSize;			// current size of this component on screen
	private Dimension rulerSize;
	private int offSetX, offSetY;
	private double deltaOffSetX, deltaOffSetXEnd, deltaOffSetXCenter;
	
	private int location; 
	
	public AngleRuler(ContactView cview, ContactPane cpane, int location){

		this.cpane = cpane;
		this.location = location;
		this.offSetX = 0;
		this.offSetY = 0;		
		this.screenSize = cview.getSize(); // cview.getScreenSize();
		this.rulerSize = new Dimension(0, 0);
		
		this.rulerWidth = STD_RULER_WIDTH;
		
		if (this.location == TOP || this.location == BOTTOM){
			this.offSetX = this.rulerWidth;
			this.offSetY = 0;
			this.rulerLength = this.cpane.getSize().width + (int)this.offSetX;
			this.rulerSize = new Dimension(this.rulerLength, this.rulerWidth);
			this.setSize(new Dimension(this.rulerLength, this.rulerWidth));
		}
		else { // this.location == LEFT || RIGHT
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
		
		/// --- Update Ruler Size ----		
		this.rulerWidth = STD_RULER_WIDTH;
		
		if (this.location == TOP || this.location == BOTTOM){
			if (this.cpane.getMapProjType() == this.cpane.kavrayskiyMapProj){ 
				this.offSetX = this.rulerWidth; 
				this.deltaOffSetX = this.cpane.getDeltaOffSetX() + this.offSetX;
				this.deltaOffSetXEnd = this.cpane.getDeltaOffSetXEnd() + this.offSetX;
				this.deltaOffSetXCenter = this.cpane.getDeltaOffSetXCenter() + this.offSetX;
			}
			else if (this.cpane.getMapProjType() == this.cpane.cylindricalMapProj){
				this.offSetX = this.rulerWidth;
				this.deltaOffSetX = 0 + this.offSetX;
				this.deltaOffSetXEnd = this.rulerLength;
			}
//			else if (this.cpane.getMapProjType() == this.cpane.azimuthalMapProj){
//				// ToDo: adjust scale!
//				this.offSetX = this.rulerWidth;
////				this.deltaOffSetX = this.cpane.getScreenPosFromRad(0, 0).getFirst();
//				this.deltaOffSetX = 0 + this.offSetX;
//				this.deltaOffSetXEnd = this.rulerLength;
//			}
			this.offSetY = 0;
			this.rulerLength = this.cpane.getPanelSize().width + this.offSetX;
			this.rulerSize = new Dimension(this.rulerLength, this.rulerWidth);
			this.setSize(new Dimension(this.rulerLength, this.rulerWidth));
		}
		else { // this.location == LEFT || RIGHT
			this.rulerLength = this.cpane.getPanelSize().height;
			this.offSetX = 0;
			this.offSetY = this.rulerWidth;
			this.rulerSize = new Dimension(this.rulerWidth, this.rulerLength);
			this.setSize(new Dimension(this.rulerWidth,this.rulerLength));
		}
				
		g2d.setBackground(BACKGROUND_COLOR);
		
		drawRulerGrid(g2d);	
	}
	
	private void drawRulerGrid(Graphics2D g2d){
		Shape shape = null;
		float xS, xE, yS, yE;
		float delta = 0;
		int numTicks = 0;		
		xS = 0.0f;
		xE = this.rulerLength;
		yS = 0.0f;
		yE = this.rulerLength;
		Color col = new Color(255, 255, 255, 255);
		g2d.setColor(col);
		if (this.location == TOP || this.location == BOTTOM){
			xS = 0.0f + this.offSetX;
			xE = this.rulerLength + this.offSetX;
			yS = 0.0f + this.offSetY;
			yE = this.rulerWidth + this.offSetY;
			numTicks = DEGREES_LAMPDA/DEGREE_RESOL;				
			shape = new Rectangle2D.Float(xS-this.offSetX, yS, xE-xS, yE-yS);	
			if (this.cpane.getMapProjType() == this.cpane.kavrayskiyMapProj){
				delta = (float) (this.rulerLength-this.offSetX-(2*this.deltaOffSetX)) / (float) numTicks;
				delta = (float) ((this.deltaOffSetXEnd-this.deltaOffSetX)/ (float)(numTicks));
			}
			else if(this.cpane.getMapProjType() == this.cpane.cylindricalMapProj){
				delta = (float) (this.rulerLength-this.offSetX) / (float) numTicks;
			}
			else if(this.cpane.getMapProjType() == this.cpane.azimuthalMapProj){
				// ToDo: adjust
				delta = (float) (this.rulerLength-this.offSetX) / (float) numTicks;
			}
		}
		else { // this.location == LEFT || RIGHT
			xS = 0.0f + this.offSetX;
			xE = this.rulerWidth + this.offSetX;
			numTicks = DEGREES_PHI/DEGREE_RESOL;
			delta = (float) this.rulerLength / (float) numTicks;
			shape = new Rectangle2D.Float(xS, yS, xE-xS, yE-yS);
		}
		
		g2d.draw(shape);
		g2d.fill(shape);

		if (this.cpane.getMapProjType() != this.cpane.azimuthalMapProj)
			drawRulerGrid(g2d, delta, numTicks);
	}
	
	private void drawRulerGrid(Graphics2D g2d, float delta, int numTicks){
		Shape tick = null;
		String degreeS = "0";
		int degree = 0;
		float xS=0, xE, yS=0, yE;
		
		g2d.setColor(Color.black);
		Font f = new Font("Dialog", Font.PLAIN, 10);
		g2d.setFont(f);

		int i=0;
		float xPosOrig = (float)(this.cpane.getOrigCoord().getFirst() * this.cpane.getVoxelsize()); 
		float yPosOrig = (float)(this.cpane.getOrigCoord().getSecond() * this.cpane.getVoxelsize()); 
		this.deltaOffSetXCenter = this.cpane.getDeltaOffSetXCenter() + this.offSetX;
		xPosOrig = (float) this.deltaOffSetXCenter;
		int count = 0;
		i=numTicks/2;
		while (count<=numTicks){
			if (this.location == TOP || this.location == BOTTOM){
				if (count==0){
					xS = xPosOrig;
				}
				else
					xS += delta;
				if (xS>this.deltaOffSetXEnd){
					xS -= this.deltaOffSetXEnd;
					xS += this.deltaOffSetX; 
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
