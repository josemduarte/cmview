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

	private ContactMapPane cmPane;
	//private Model mod; //TODO not needed for now, will need it later
 	private View view;
	private int rulerWidth;
	private int rulerLength;
	private int contactMapSize;
	private double ratio;
	private int offSet;
	
	private boolean horizontal;
	
	//private boolean mouseIn;
	//private boolean dragging;
	
	//private Point pos;               // current mouse position
	private Point mousePressedPos;   // position where the mouse has been pressed (in dragging is the start of drag)
	//private Point mouseDraggingPos;  // current position of mouse dragging
	
	public ResidueRuler(ContactMapPane cmPane, Model mod, View view){
		addMouseListener(this);
		addMouseMotionListener(this);

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
		Graphics2D bufferGraphics = (Graphics2D) g.create();

		// paint background
		if (isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		
		rulerLength = cmPane.getWindowSize();
		if (getWidth()>getHeight()){ // horizontal ruler
			horizontal=true;
			rulerWidth = this.getHeight();
			// if we are in top ruler we want an offset for the space that the left ruler ocuppies
			offSet = rulerWidth;
		} else { // vertical ruler
			horizontal=false;
			rulerWidth = this.getWidth();
			offSet = 0;
		}
		
		ratio = (double)rulerLength/contactMapSize;

		setBackground(Color.white);
		
		int tickSeparation = 1;
		if (cmPane.getCellSize()<5) {
			tickSeparation = 10;
		}
		for (int i=1;i<=contactMapSize;i+=tickSeparation){
			Point startPoint = getOuterBorderPoint(i);
			Point endPoint = getInnerBorderPoint(i);
			Point nextEndPoint = getInnerBorderPoint(i+1);
			bufferGraphics.setColor(Color.blue);
			if (i%10==0) {
				//bufferGraphics.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);				
				bufferGraphics.fillRect(startPoint.x, startPoint.y, nextEndPoint.x-startPoint.x, nextEndPoint.y-startPoint.y);
			} else {
				bufferGraphics.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
			}
		}
	}

	private Point getOuterBorderPoint(int k){
		Point point = new Point();
		if (horizontal) { // horizontal ruler
			point.x = (int) Math.round((k-1)*ratio) + offSet;
			point.y = 0;
		} else { // vertical ruler
			point.x = 0;
			point.y = (int) Math.round((k-1)*ratio);			
		}
		return point;
	}

	private Point getInnerBorderPoint(int k){
		Point point = new Point();
		if (horizontal) { // horizontal ruler
			point.x = (int) Math.round((k-1)*ratio) + offSet;
			point.y = rulerWidth;
		} else { // vertical ruler
			point.x = rulerWidth;
			point.y = (int) Math.round((k-1)*ratio);			
		}
		return point;
	}
	
	private int screen2cm (Point point){
		if (horizontal){
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
