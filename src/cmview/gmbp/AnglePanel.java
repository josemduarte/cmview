package cmview.gmbp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

public class AnglePanel extends JPanel implements MouseListener{
	
/*------------------------- member variables ------------------------*/
	
	private static final long serialVersionUID = 1L;
	
	// constants / settings
	private Color coordinatesColor = Color.blue;
	private Color coordinatesBgColor = Color.white;
	private int width = ContactStatusBar.DEFAULT_WIDTH;

	private final int leftMargin = 5;			// margin between bg rectangle and edge
	private final int firstColumnX = leftMargin + 10;		// from edge
	private final int secondColumnX = leftMargin + 25;	// from edge
	private final int thirdColumnX = leftMargin + 55;		// second contact map or seq sep
	private final int rightMargin = 12;		// margin between bg rectangle and edge
	private final int bottomMargin = 0;		// margin between bg rectable and edge
	private final int textYOffset = 20;		// top margin between rectangle and first text
	private final int lineHeight = 18;		// offset between lines
	private final int minNumLines = 5;      // 7 if nbhs printed otherwise 5
//	private final int letterWidth = 10;
	private int totalHeight = minNumLines * lineHeight + bottomMargin + textYOffset;		// height for basic information and background
	private final int numResPerLine = 8;
	
	private String title1 = "Contact:";
	private String iRes = "";
	private String jRes = "";
	private int    iNum = 0;
	private int    jNum = 0;
	private String iSSType = "";
	private String jSSType = "";
	private String nbhs = "";
	private String title2 = "AngleRange:";
	private String lambdaMin = "";
	private String lambdaMax = "";
	private String thetaMin = "";
	private String thetaMax = "";
	
	public AnglePanel() {		
		this.setMinimumSize(new Dimension(width,totalHeight));
		this.setPreferredSize(new Dimension(width, totalHeight));
		
//		addMouseListener(this);
//		addMouseMotionListener(this);
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
		g2d.drawString(iNum+iRes+"_"+iSSType.toLowerCase()+" - "+jNum+jRes+"_"+jSSType.toLowerCase(), x, y);	// selected contacts within contact map
		
//		// NBHS
//		y += this.lineHeight;
////		g2d.drawString(nbhs, x, y);
//		String resS;					
//		for (int i=0; i<this.nbhs.length(); i++){
//			resS = String.valueOf(nbhs.charAt(i));
//			if (i%this.numResPerLine==0 && i>0){
//				y += lineHeight;
//				x = firstColumnX;
//			}
//			g2d.drawString(resS, x, y);
//			x += this.letterWidth;
//		}
//		x = firstColumnX;
////		y += this.lineHeight;
		
		baseLineY = y;
		y = baseLineY+this.textYOffset;
		g2d.drawString(title2, x, y);			// Angle Range
		y += this.lineHeight;
//		y = baseLineY + textYOffset;
		x = firstColumnX;
		g2d.drawString("p:", x, y);					// Lambda	
		x = secondColumnX;
		g2d.drawString(lambdaMin, x, y);
		x = thirdColumnX;
		g2d.drawString("- "+lambdaMax, x, y);
		y += 20;
		x = firstColumnX;
		g2d.drawString("t:", x, y);					// Phi
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
	
	public void setJNum(int num) {
		this.jNum = num;		
	}
	public void setINum(int num) {
		this.iNum = num;
	}
	
	public void setJSSType(String string) {
		this.jSSType = string;		
	}
	public void setISSType(String string) {
		this.iSSType = string;
	}
	
	public void setNBHS(String string){
		this.nbhs = string;
		
		int numLines = 1;
		while (this.nbhs.length() > numLines*this.numResPerLine)
			numLines++;
//		totalHeight = (minNumLines-1+numLines) * lineHeight + bottomMargin + textYOffset;

		this.setMinimumSize(new Dimension(width,totalHeight));
		this.repaint();
	}
	
	public void setLambdaMin(String string){
		this.lambdaMin = string;
	}
	public void setLambdaMax(String string){
		this.lambdaMax = string;
	}
	public void setPhiMin(String string){
		this.thetaMin = string;
	}
	public void setPhiMax(String string){
		this.thetaMax = string;
	}
	
	public int getTotalHeight(){
		return this.totalHeight;
	}
	
	/*----------------Event Handling-----------------------------------------*/
	

	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		System.out.println("mouseClicked");
		
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		System.out.println("mouseEntered");
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		System.out.println("mouseExited");
		
	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		System.out.println("mousePressed");
		
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		System.out.println("mouseReleased");
		Point mousePos = e.getPoint();
		System.out.println("mousePos: "+mousePos.x+" , "+mousePos.y);
		
	}

}
