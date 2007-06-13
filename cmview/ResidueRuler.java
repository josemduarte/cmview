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

	private static final int TICK_SEPARATION = 1;
	
	private ContactMapPane cmPane;
	//private Model mod; //TODO not needed for now, will need it later
 	//private View view; //TODO not needed for now, will need it later
	private int rulerWidth;
	private int rulerLength;
	private int contactMapSize;
	private double ratio;
	private int offSet;
	
	private boolean horizontal;
	
	public ResidueRuler(ContactMapPane cmPane, Model mod, View view){
		this.cmPane = cmPane;
		//this.mod = mod;
		//this.view = view;
		this.contactMapSize = mod.getMatrixSize();
		this.offSet=0;
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
		
		for (int i=1;i<=contactMapSize;i+=TICK_SEPARATION){
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

	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseDragged(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseMoved(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

}
