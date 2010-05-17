package cmview.gmbp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

public class AnglePanel extends JPanel {
	
/*------------------------- member variables ------------------------*/
	
	private static final long serialVersionUID = 1L;
	
	// constants / settings
	private Color coordinatesColor = Color.blue;
	private Color coordinatesBgColor = Color.white;
	private int width = ContactStatusBar.DEFAULT_WIDTH;

	int leftMargin = 5;			// margin between bg rectangle and edge
	int firstColumnX = leftMargin + 10;		// from edge
	int secondColumnX = leftMargin + 25;	// from edge
	int thirdColumnX = leftMargin + 55;		// second contact map or seq sep
	int rightMargin = 12;		// margin between bg rectangle and edge
	int bottomMargin = 0;		// margin between bg rectable and edge
	int textYOffset = 23;		// top margin between rectangle and first text
	int lineHeight = 20;		// offset between lines
	int totalHeight = 6 * lineHeight + bottomMargin + textYOffset;		// height for basic information and background
	
	private String title1 = "Contact:";
	private String iRes = "";
	private String jRes = "";
	private String iSSType = "";
	private String jSSType = "";
	private String nbhs = "";
	private String title2 = "AngleRange:";
	private String phiMin = "";
	private String phiMax = "";
	private String thetaMin = "";
	private String thetaMax = "";
	
	public AnglePanel() {
		this.setMinimumSize(new Dimension(width,totalHeight));
	}
	
/*--------------------------   drawing ---------------------------------*/
	
	/** Method called by this component to determine its minimum size */
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(width,totalHeight);
	}

	/** Method called by this component to determine its preferred size */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(width,totalHeight);
	}

	/** Method called by this component to determine its maximum size */
	@Override
	public Dimension getMaximumSize() {
		return new Dimension(width,totalHeight);
	}
	
	/**
	 * Main method to draw the component on screen. This method is called each
	 * time the component has to be (re) drawn on screen. It is called
	 * automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	@Override
	protected synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g.create();
		drawAngleInfo(g2d);
	}
	
	protected void drawAngleInfo(Graphics2D g2d) {
		
		int baseLineY = 0;	// top of bg rectangle in local coordinates of this component
		// draw background rectangle
		g2d.setColor(coordinatesBgColor);
		g2d.fill(new RoundRectangle2D.Float(leftMargin, baseLineY, getWidth()-rightMargin, totalHeight-bottomMargin, 12, 12));		
		g2d.setColor(coordinatesColor);
		
		int x = firstColumnX;			// where first text will be written
		int y = baseLineY+textYOffset;	// where first text will be written
		
		g2d.drawString(title1, x, y);			
		y += this.lineHeight;
		g2d.drawString(iRes+"_"+iSSType.toLowerCase()+" - "+jRes+"_"+jSSType.toLowerCase(), x, y);	// selected contacts within contact map
		y += this.lineHeight;
		g2d.drawString(nbhs, x, y);
		
		baseLineY = y;
		y = baseLineY+this.textYOffset;
		g2d.drawString(title2, x, y);			// Angle Range
		y += this.lineHeight;
//		y = baseLineY + textYOffset;
		x = firstColumnX;
		g2d.drawString("p:", x,y);					// Phi	
		x = secondColumnX;
		g2d.drawString(phiMin, x, y);
		x = thirdColumnX;
		g2d.drawString("- "+phiMax, x, y);
		y += 20;
		x = firstColumnX;
		g2d.drawString("t:", x, y);					// Theta
		x = secondColumnX;
		g2d.drawString(thetaMin, x, y);
		x = thirdColumnX;
		g2d.drawString("- "+thetaMax, x, y);
		y += 20;
		
	}
	
	
	/*-------------------------- getters and setters -----------------------*/
	public void setJRes(String string) {
		this.jRes = string;		
	}
	public void setIRes(String string) {
		this.iRes = string;
	}
	
	public void setJSSType(String string) {
		this.jSSType = string;		
	}
	public void setISSType(String string) {
		this.iSSType = string;
	}
	
	public void setNBHS(String string){
		this.nbhs = string;
	}
	
	public void setPhiMin(String string){
		this.phiMin = string;
	}
	public void setPhiMax(String string){
		this.phiMax = string;
	}
	public void setThetaMin(String string){
		this.thetaMin = string;
	}
	public void setThetaMax(String string){
		this.thetaMax = string;
	}
	

}
