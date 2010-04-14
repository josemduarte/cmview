package cmview;

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
	
	protected static final int STD_RULER_WIDTH = 20;
	protected static final int DEGREES_PHI = 360;
	protected static final int DEGREES_THETA = 180;
	protected static final int DEGREE_RESOL = 10;
	
	private static final Color BACKGROUND_COLOR = Color.yellow;		// if opaque, otherwise transparent
	
	private ContactView cview;
	private ContactPane cpane;
	private int rulerWidth;
	private int rulerLength;
//	private int contactMapSize;
	private Dimension screenSize;			// current size of this component on screen
	private Dimension rulerSize;
	private Dimension panelSize;
	private double ratioX, ratioY;
	private int offSetX, offSetY;
	
	private int location; 
	
	public AngleRuler(ContactView cview, ContactPane cpane, int location){
		this.cview = cview;
		this.cpane = cpane;
		this.location = location;
		this.offSetX = 0;
		this.offSetY = 0;		
		this.screenSize = cview.getSize(); // cview.getScreenSize();
		this.panelSize = cpane.getSize(); //cpane.getPanelSize();
		
	}
	
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();
		
		System.out.println("CView HxB: "+this.cview.getSize().height+"x"+this.cview.getSize().width);
		System.out.println("CPane HxB: "+this.cpane.getSize().height+"x"+this.cpane.getSize().width);
		
		this.rulerWidth = STD_RULER_WIDTH;
		
		if (this.location == TOP || this.location == BOTTOM){
//			this.rulerLength = this.cview.getSize().width;
//			this.rulerWidth = this.cview.getSize().height;
			this.offSetX = this.rulerWidth;
			this.offSetY = 0;
			this.rulerLength = this.cpane.getSize().width + this.offSetX;
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
		
//		this.ratioX = (double)this.rulerLength/this.screenSize.width;
//		this.ratioY = (double)this.rulerWidth/this.screenSize.height;
		this.ratioX = (double)this.rulerLength/this.panelSize.width;
		this.ratioY = (double)this.rulerWidth/this.panelSize.height;
		
		
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
			Color col = new Color(255, 0, 0, 126);
			g2d.setColor(col);
			numTicks = DEGREES_PHI/DEGREE_RESOL;			
		}
		else { // this.location == LEFT || RIGHT
			xS = 0.0f + this.offSetX;
			xE = this.rulerWidth + this.offSetX;
//			yS = 0.0f + this.offSetY;
//			yE = this.rulerLength + this.offSetY;
			Color col = new Color(0, 255, 0, 126);
			g2d.setColor(col);
			numTicks = DEGREES_THETA/DEGREE_RESOL;
		}
//		System.out.println("numTicks= "+numTicks);
		float delta = this.rulerLength / numTicks;
		System.out.println("Coordinates: "+xS+", "+yS+", "+xE+", "+yE);
		
		shape = new Rectangle2D.Float(xS, yS, xE-xS, yE-yS);
		g2d.draw(shape);
		g2d.fill(shape);
		
		g2d.setColor(Color.black);
//		tick = new Line2D.Float(xS, yS, xE, yE);
//		g2d.draw(tick);
		Font f = new Font("Dialog", Font.PLAIN, 12);
		g2d.setFont(f);
		
		for (int i=0; i<=numTicks; i++){
			if (this.location == TOP || this.location == BOTTOM){
				xS = i*delta;
				xE = xS;
				xS += this.offSetX;
				xE += this.offSetX;
				yS = this.rulerWidth/2;
				yE = this.rulerWidth;
			}
			else { // this.location == LEFT || RIGHT
				yS = i*delta;
				yE = yS;
				xS = this.rulerWidth/2;
				xE = this.rulerWidth;
			}
			tick = new Line2D.Float(xS, yS, xE, yE);			
			g2d.draw(tick);
//			g2d.fill(shape);
			
			degree = i*DEGREE_RESOL;
			degreeS = String.valueOf(degree);
			if (this.location == TOP || this.location == BOTTOM){
				xS = i*delta;
//				xS += this.offSetX;
				yS = this.rulerWidth/2; //0;
			}
			else { // this.location == LEFT || RIGHT
				yS = i*delta;
				xS = this.rulerWidth/2; //0;
			}
			g2d.drawString(degreeS, xS, yS);
			
		}	
	}

}
