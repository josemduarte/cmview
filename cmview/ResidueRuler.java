package cmview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import cmview.datasources.Model;

public class ResidueRuler extends JPanel implements MouseListener,
		MouseMotionListener {

	private static final long serialVersionUID = 1L;

	protected static final int TOP = 1;
	protected static final int BOTTOM = 2;
	protected static final int LEFT = 3;
	protected static final int RIGHT = 4; 
	
	private ContactMapPane cmPane;
	//private Model mod; //TODO not needed for now, will need it later
 	private View view;
	private int rulerWidth;
	private int rulerLength;
	private int contactMapSize;
	private double ratio;
	private int offSet;
	
	private int location; 

	//private boolean mouseIn;
	//private boolean dragging;
	
	//private Point pos;               // current mouse position
	private Point mousePressedPos;   // position where the mouse has been pressed (in dragging is the start of drag)
	//private Point mouseDraggingPos;  // current position of mouse dragging
	
	public ResidueRuler(ContactMapPane cmPane, Model mod, View view, int location){
		addMouseListener(this);
		addMouseMotionListener(this);
		this.location = location;
		this.cmPane = cmPane;
		//this.mod = mod;
		this.view = view;
		this.contactMapSize = mod.getMatrixSize();
		this.offSet = 0;
		//this.mouseIn = false;
		//this.pos = new Point();
		this.mousePressedPos = new Point();
		//this.mouseDraggingPos = new Point();
		//this.dragging = false;
	}
	
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();

		// paint background
		if (isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		
		rulerLength = cmPane.getWindowSize();
		if (location==TOP || location==BOTTOM){ // horizontal ruler
			rulerWidth = this.getHeight();
			// if we are in top ruler we want an offset for the space that the left ruler ocuppies
			offSet = rulerWidth;
		} else { // vertical ruler
			rulerWidth = this.getWidth();
			offSet = 0;
		}
		
		ratio = (double)rulerLength/contactMapSize;

		setBackground(Color.white);
		
		// painting ticks
		int tickSeparation = 1;
		if (cmPane.getCellSize()<5) {
			tickSeparation = 10;
		}
		for (int i=1;i<=contactMapSize+1;i+=tickSeparation){
			// Warning: contactMapSize+1 is not really a contact but this will draw the last
			Point startPoint = getOuterBorderPoint(i);
			Point endPoint = getInnerBorderPoint(i);
			g2d.setColor(Color.blue);
			g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
		}
		
	}

	/**
	 * Gives outer border point at the beginning of the k cell
	 * @param k
	 * @return
	 */
	private Point getOuterBorderPoint(int k){
		Point point = new Point();
		if (location==TOP) {
			point.x = (int) Math.round((k-1)*ratio) + offSet;
			point.y = 0;
		} else if (location==BOTTOM){
			point.x = (int) Math.round((k-1)*ratio) + offSet;
			point.y = rulerWidth;			
		} else if (location==LEFT){
			point.x = 0;
			point.y = (int) Math.round((k-1)*ratio);			
		} else if (location==RIGHT){
			point.x = rulerWidth;
			point.y = (int) Math.round((k-1)*ratio);			
		}
		return point;
	}

	/**
	 * Gives inner border point at the beginning of the k cell
	 * @param k
	 * @return
	 */
	private Point getInnerBorderPoint(int k){
		Point point = new Point();
		if (location==TOP) {
			point.x = (int) Math.round((k-1)*ratio) + offSet;
			point.y = rulerWidth;
		} else if(location==BOTTOM) {
			point.x = (int) Math.round((k-1)*ratio) + offSet;
			point.y = 0;	
		} else if(location==LEFT) {
			point.x = rulerWidth;
			point.y = (int) Math.round((k-1)*ratio);			
		} else if(location==RIGHT) { // vertical ruler
			point.x = 0;
			point.y = (int) Math.round((k-1)*ratio);
		}

		return point;
	}

//	private Point getCellCenter(int k){
//		Point point = new Point();
//		if (location==TOP || location==BOTTOM) {
//			point.x = (int) Math.round((k-0.5)*ratio) + offSet;
//			point.y = rulerWidth/2;
//		} else {
//			point.x = rulerWidth/2;
//			point.y = (int) Math.round((k-0.5)*ratio);			
//		}
//
//		return point;
//	}
	
	private int screen2cm (Point point){
		if (location==TOP || location==BOTTOM){
			return (int) Math.ceil((point.x-offSet)/ratio);
		} else {
			return (int) Math.ceil(point.y/ratio);
		}
	}

	
	/** ############################################### */
	/** ############    MOUSE EVENTS   ################ */
	/** ############################################### */   
	
	public void mouseClicked(MouseEvent evt) {
	}

	public void mouseEntered(MouseEvent evt) {
		//mouseIn= true;
	}

	public void mouseExited(MouseEvent evt) {
		//mouseIn = false;
		this.repaint();
	}

	public void mousePressed(MouseEvent evt) {
		mousePressedPos = evt.getPoint();
		System.out.println(screen2cm(mousePressedPos));

	}

	public void mouseReleased(MouseEvent evt) {
		// only if release after left click (BUTTON1)
		if (evt.getButton()==MouseEvent.BUTTON1) {
			if (view.getCurrentAction()==View.NODE_NBH_SEL){				
				if (evt.isControlDown()){
					cmPane.selectNodeNbh(screen2cm(mousePressedPos));
				} else{
					cmPane.resetSelContacts();
					cmPane.selectNodeNbh(screen2cm(mousePressedPos));
				}
				cmPane.repaint();
				
			}
			//dragging = false;
		}
		

	}

	public void mouseDragged(MouseEvent evt) {
		//dragging = true;
		//mouseDraggingPos = evt.getPoint();
	}

	public void mouseMoved(MouseEvent evt) {
		//pos = evt.getPoint();
		this.repaint();
	}

}
