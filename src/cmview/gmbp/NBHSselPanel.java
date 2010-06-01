package cmview.gmbp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

public class NBHSselPanel extends JPanel implements MouseListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Color residueSwitchedOnCol = Color.black;
	private static final Color residueSwitchedOffCol = Color.gray;
	private static final Color bgColor = Color.white;
	
	// constants / settings
	private final int textXOffset = 5;		// top margin between rectangle and first text
	private final int textYOffset = 20;		// top margin between rectangle and first text
	private final int letterWidth = 10;
	private final int lineHeight = 20;		// offset between lines
	private final int bottomMargin = 5;		// margin between bg rectable and edge
	private int width = (int)(0.9*ContactStatusBar.DEFAULT_WIDTH);
	private int totalHeight = textYOffset+5;		// height for basic information and background
	private final int numResPerLine = 8;
	
	
	/*--------------------------- member variables --------------------------*/		
	private String nbhString;
	private String actNBHString;
	private char[] nbhsRes;
	private boolean[] nbhsResFlags;
	
	private Point mousePos;
	private int indexClickedRes;
	
	public NBHSselPanel(String nbhs){
		this.nbhString = nbhs;
		this.actNBHString = nbhs;
//		this.nbhString += nbhs;
//		this.nbhString += "TRGB";
		addMouseListener(this);
		
		int numLines = 1;
		while (this.nbhString.length() > numLines*this.numResPerLine)
			numLines++;
		System.out.println(numLines);
		this.totalHeight = (textYOffset+ ((numLines-1) * this.lineHeight) + this.bottomMargin);
		this.setMinimumSize(new Dimension(width,totalHeight));
		
		// initialize arrays to handle toggling
		this.nbhsRes = new char[this.nbhString.length()];
		this.nbhsResFlags = new boolean[this.nbhString.length()];
		for (int i=0; i<this.nbhString.length(); i++){
			this.nbhsRes[i] = this.nbhString.charAt(i);
			this.nbhsResFlags[i] = true;
		}
		
	}
	
	/*----------------Drawing Methods-----------------------------------------*/
	
	/**
	 * Main method to draw the component on screen. This method is called each
	 * time the component has to be (re) drawn on screen. It is called
	 * automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	@Override
	protected synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g.create();
		
		g2d.setColor(bgColor);
		g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
		
		drawNBHS(g2d);
	}
	
	protected void drawNBHS(Graphics2D g2d) {
		
		String resS;	
		int x = textXOffset;	// where first text will be written
		int y = textYOffset;	// where first text will be written
				
		for (int i=0; i<this.nbhsRes.length; i++){
			resS = String.valueOf(this.nbhsRes[i]);
			g2d.setColor(residueSwitchedOnCol);
			if (!this.nbhsResFlags[i])
				g2d.setColor(residueSwitchedOffCol);
			if (i%this.numResPerLine==0 && i>0){
				y += lineHeight;
				x = textXOffset;
			}
			g2d.drawString(resS, x, y);
			x += this.letterWidth;
		}
	}
	
	/*----------------Getters and Setters------------------------------------*/

	public String getNbhString() {
		return nbhString;
	}

	public void setNbhString(String nbhString) {
		this.nbhString = nbhString;
	}

	public String getActNbhString() {
		return actNBHString;
	}
	
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
	

	/*----------------Event Handling-----------------------------------------*/
	
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
//		System.out.println("mouseClicked");
		
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
//		System.out.println("mouseEntered");
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
//		System.out.println("mouseExited");
		
	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
//		System.out.println("mousePressed");
		
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		System.out.println("mouseReleased");
		this.mousePos = e.getPoint();
		System.out.println("mousePos: "+this.mousePos.x+" , "+this.mousePos.y);
		int xI = ((this.mousePos.x - this.textXOffset)/this.letterWidth);
		int yI = (this.mousePos.y - (this.textYOffset-this.lineHeight))/this.lineHeight;
		this.indexClickedRes = xI + (yI * this.numResPerLine);
		System.out.println("Index = "+xI+","+yI+" -> " +this.indexClickedRes);
		// Test if central residue
//		if (this.nbhsRes[this.indexClickedRes] != 'x')
			this.nbhsResFlags[this.indexClickedRes] = !this.nbhsResFlags[this.indexClickedRes];
		String s = "";
		for (int i=0; i<this.nbhString.length(); i++){
			if (this.nbhsResFlags[i])
				s += this.nbhsRes[i];
		}
		this.actNBHString = s;
		System.out.println("Actual NBHString = "+this.actNBHString);
		this.repaint();
	}

}
