package cmview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

/**
 * The component which draws the loupe. This component can be added to a separate JDialog or to the main View.
 * This component should be notified whenever the mouse moves inside the contact map so it can be redrawn.
 * @author stehr
 */
public class LoupePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	/*------------------------------ constants ------------------------------*/
	private int initialSize = Start.LOUPE_WINDOW_SIZE;
	private int newContactSize = Start.LOUPE_CONTACT_SIZE;
	private Color bgColor = new Color(240,240,240);
	
	/*--------------------------- member variables --------------------------*/
	private Image image;	// image to be drawn in loupe
	private Point mousePos;	// current position of mouse in ContentMapPane
	private int width;
	private int height;
	private int contactSize;
	private ContactMapPane parent;
	private boolean clear;
	
	/*----------------------------- constructors ----------------------------*/
	public LoupePanel() {
		super();
		clear = true;
	}
	
	/*---------------------------- public methods ---------------------------*/
	/**
	 * Method which is called by ContentMapPane to update this component.
	 * @param image
	 */
	public void updateLoupe(Image image, Point mousePos, int contactSize, ContactMapPane parent) {
		this.image = image;
		this.mousePos = mousePos;
		this.contactSize = contactSize;
		this.parent = parent;
		
		this.clear = false;
		this.repaint();
	}
	
	/**
	 * Clears the loupe window (fill with bg color).
	 */
	public void clear() {
		this.clear = true;
		this.repaint();
	}
	
	/*-------------------------- implemented methods ------------------------*/
	public Dimension getMinimumSize() {
		return new Dimension(initialSize, initialSize);
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(initialSize, initialSize);
	}
		
	protected synchronized void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();

		// draw background
		g2d.setColor(bgColor);
		g2d.fill(new Rectangle2D.Float(0,0,width,height));
		
		if(!clear) {
			
			// draw contacts
			Image image2=parent.createImage(parent.getPreferredSize().width,parent.getPreferredSize().height);
			Graphics2D g2d2 = (Graphics2D) image2.getGraphics();
			
			width = this.getWidth();
			height = this.getHeight();
			
			double scaleFactor = newContactSize / contactSize;
			double xOffs = -1.0 * (mousePos.x * scaleFactor - 0.5 * width);
			double yOffs = -1.0 * (mousePos.y * scaleFactor - 0.5 * height);
			AffineTransform transform = new AffineTransform(scaleFactor, 0.0, 0.0, scaleFactor, xOffs, yOffs);
			
			g2d2.drawImage(image,null,this);
			parent.paintLoupe(g2d2);
			g2d.drawImage(image2,transform,this);	
		}
		
		// draw crosshair
		g2d.draw(new Line2D.Float(0.5f * width, 0.0f, 0.5f * width, height));
		g2d.draw(new Line2D.Float(0.0f, 0.5f * height, width, 0.5f * height));
		
		if(clear && this.getWidth() > 190) {
			g2d.setColor(Color.gray);
			g2d.drawString("Move mouse over contact map", this.getWidth()/2-85, this.getHeight()/2-10);
			g2d.drawString("to see magnified view", this.getWidth()/2-75, this.getHeight()/2+10);			
		}
		
	}
}
