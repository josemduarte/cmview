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
	private Color bgColor = new Color(230,230,230);
	
	/*--------------------------- member variables --------------------------*/
	private Image image;	// image to be drawn in loupe
	private Point mousePos;	// current position of mouse in ContentMapPane
	private int width;
	private int height;
	private int contactSize;
	
	/*----------------------------- constructors ----------------------------*/
	public LoupePanel() {
		super();
	}
	
	/*---------------------------- public methods ---------------------------*/
	/**
	 * Method which is called by ContentMapPane to update this component.
	 * @param image
	 */
	public void updateLoupe(Image image, Point mousePos, int contactSize) {
		this.image = image;
		this.mousePos = mousePos;
		this.contactSize = contactSize;
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
		width = this.getWidth();
		height = this.getHeight();
		
		double scaleFactor = newContactSize / contactSize;
		double xOffs = -1.0 * (mousePos.x * scaleFactor - 0.5 * width);
		double yOffs = -1.0 * (mousePos.y * scaleFactor - 0.5 * height);
		AffineTransform transform = new AffineTransform(scaleFactor, 0.0, 0.0, scaleFactor, xOffs, yOffs);
		
		g2d.setColor(bgColor);
		g2d.fill(new Rectangle2D.Float(0,0,width,height));		
		g2d.drawImage(image,transform,this);
		g2d.draw(new Line2D.Float(0.5f * width, 0.0f, 0.5f * width, height));
		g2d.draw(new Line2D.Float(0.0f, 0.5f * height, width, 0.5f * height));
		
		//g2d.drawString(mousePos.toString(), 10, 10);
		
	}
}
