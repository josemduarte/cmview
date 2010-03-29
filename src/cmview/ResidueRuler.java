package cmview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Iterator;

import javax.swing.JPanel;

import proteinstructure.SecStrucElement;
import tools.Interval;

import cmview.datasources.Model;

/**
 * Component derived from JPanel, used by View to show residue rulers around a contact map.
 */
public class ResidueRuler extends JPanel implements MouseListener,
		MouseMotionListener {

	private static final long serialVersionUID = 1L;

	protected static final int TOP = 1;
	protected static final int BOTTOM = 2;
	protected static final int LEFT = 3;
	protected static final int RIGHT = 4; 
	
	//private static final Color HELIX_COLOR = Color.blue;
	private static final Color HELIX_COLOR = new Color(0, 102, 255);
	//private static final Color TURN_COLOR = new Color(135,163,176);
	private static final Color TURN_COLOR = new Color(115, 244, 81);
	//private static final Color SHEET_COLOR = new Color(25,162,223);
	private static final Color SHEET_COLOR = new Color(255, 0, 51);
	private static final Color OTHER_COLOR = new Color(0,0,0,0);	// transparent (should match background if opaque)
	private static final Color UNEXPECTED_SS_COLOR = Color.gray;
	private static final Color BACKGROUND_COLOR = Color.white;		// if opaque, otherwise transparent
	
	private ContactMapPane cmPane;
	private Model mod;
 	private View view;
	private int rulerWidth;
	private int rulerLength;
	private int contactMapSize;
	private double ratio;
	private int offSet;
	
	private int location; 
	
	private boolean dragging;		 // true while dragging
	//private Point pos;               // current mouse position
	private Point mousePressedPos;   // position where the mouse has been pressed (in dragging is the start of drag)
	private int lastButtonPressed;	 // button which was last pressed
	private Point mouseDraggingPos;  // current position of mouse dragging
	
	public ResidueRuler(ContactMapPane cmPane, Model mod, View view, int location){
		addMouseListener(this);
		addMouseMotionListener(this);
		this.location = location;
		this.cmPane = cmPane;
		this.mod = mod;
		this.view = view;
		this.contactMapSize = cmPane.getAlignment().getAlignmentLength();
		this.offSet = 0;
		//this.mouseIn = false;
		//this.pos = new Point();
		this.mousePressedPos = new Point();
		//this.mouseDraggingPos = new Point();
		//this.dragging = false;
		this.setOpaque(false);
	}
	
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();

		// paint background
		if (isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		
		rulerLength = cmPane.getOutputSize();
		if (location==TOP || location==BOTTOM){ // horizontal ruler
			rulerWidth = this.getHeight();
			// if we are in top ruler we want an offset for the space that the left ruler ocuppies
			offSet = rulerWidth;
		} else { // vertical ruler
			rulerWidth = this.getWidth();
			offSet = 0;
		}
		
		ratio = (double)rulerLength/contactMapSize;

		setBackground(BACKGROUND_COLOR);

		// painting secondary structure elements
		if (mod.hasSecondaryStructure()){
			Iterator<SecStrucElement> secStruc = mod.getSecondaryStructure().getIterator();
			while(secStruc.hasNext()) {
				SecStrucElement ssElem = secStruc.next();
				if (ssElem.isHelix()){
					g2d.setColor(HELIX_COLOR);
				} else if (ssElem.isTurn()) {
					g2d.setColor(TURN_COLOR);
				} else if (ssElem.isStrand()){
					g2d.setColor(SHEET_COLOR);
				} else if (ssElem.isOther()){
					g2d.setColor(OTHER_COLOR);
				} else {
					g2d.setColor(UNEXPECTED_SS_COLOR);
				}
				Point startPoint = getOuterBorderCentrePoint(cmPane.mapSeq2Al(mod.getLoadedGraphID(),ssElem.getInterval().beg));
				Point endPoint = getInnerBorderCentrePoint(cmPane.mapSeq2Al(mod.getLoadedGraphID(),ssElem.getInterval().end));
				g2d.fillRect(startPoint.x,startPoint.y,endPoint.x-startPoint.x,endPoint.y-startPoint.y);
			}
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
	 * Gives outer border point at the centre of the k cell
	 * @param k
	 * @return
	 */
	private Point getOuterBorderCentrePoint(int k){
		Point point = getOuterBorderPoint(k);
		if (location==TOP) {
			point.x = point.x+(int)Math.ceil(ratio/2);
			point.y = 0;
		} else if (location==BOTTOM){
			point.x = point.x+(int)Math.ceil(ratio/2);
			point.y = rulerWidth;			
		} else if (location==LEFT){
			point.x = 0;
			point.y = point.y+(int)Math.ceil(ratio/2);			
		} else if (location==RIGHT){
			point.x = rulerWidth;
			point.y = point.y+(int)Math.ceil(ratio/2);			
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

	/**
	 * Gives inner border point at the centre of the k cell
	 * @param k
	 * @return
	 */
	private Point getInnerBorderCentrePoint(int k){
		Point point = getInnerBorderPoint(k);
		if (location==TOP) {
			point.x = point.x+(int)Math.ceil(ratio/2);
			point.y = rulerWidth;
		} else if(location==BOTTOM) {
			point.x = point.x+(int)Math.ceil(ratio/2);
			point.y = 0;	
		} else if(location==LEFT) {
			point.x = rulerWidth;
			point.y = point.y+(int)Math.ceil(ratio/2);			
		} else if(location==RIGHT) { // vertical ruler
			point.x = 0;
			point.y = point.y+(int)Math.ceil(ratio/2);
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
		if(dragging) {
			dragging = false;
			cmPane.showRulerCoordinate();
		} else {
			if(evt.getButton()==MouseEvent.BUTTON1) {
				Point pos=evt.getPoint();
				int clickedRes = screen2cm(pos);
				// if we clicked on the corner, then the residue is <=0, which is nonsense so we just return 
				// (if we continue with the <=0 value then it crashes when it trying to map to sequence)
				if (clickedRes<=0) return;
				
				// node nbh selection mode
				if (view.getGUIState().getSelectionMode()==GUIState.SelMode.NBH) {				
					if (cmPane.isControlDown(evt)){
						cmPane.selectNodeNbh(screen2cm(mousePressedPos));
					} else{
						cmPane.resetSelections();
						cmPane.selectNodeNbh(screen2cm(mousePressedPos));
					}
					cmPane.repaint();

				} else {
					SecStrucElement ssElem = mod.getSecondaryStructure().getSecStrucElement(cmPane.mapAl2Seq(mod.getLoadedGraphID(),clickedRes));
					if(ssElem==null) {
						// clicking outside of secondary structure
						if(!cmPane.isControlDown(evt)) { // default behaviour: control-click on whitespace does nothing
							if(location==TOP || location==BOTTOM) {
								cmPane.resetVerticalNodeSelection();
							} else {
								cmPane.resetHorizontalNodeSelection();
							}	
						}
					} else {
						// clicking on secondary structure element
						Interval ssint = ssElem.getInterval();
						Interval ssintAliIdx = new Interval(cmPane.mapSeq2Al(mod.getLoadedGraphID(), ssint.beg),cmPane.mapSeq2Al(mod.getLoadedGraphID(), ssint.end));
						if(cmPane.isControlDown(evt)) {
							// adding to current selection
							System.out.println("Selecting " + ssElem.getId() + " from " + ssint.beg + " to " + ssint.end); // for the user we want to show sequence indices
							if(location==TOP || location==BOTTOM) {
								if(cmPane.getSelVertNodes().contains(clickedRes)) {
									// selected already: deselect
									cmPane.deselectNodesVertically(ssintAliIdx);
								} else {
									// otherwise: select
									cmPane.selectNodesVertically(ssintAliIdx);
								}
							} else {
								if(cmPane.getSelHorNodes().contains(clickedRes)) {
									// selected already: deselect
									cmPane.deselectNodesHorizontally(ssintAliIdx);
								} else {
									// otherwise: select
									cmPane.selectNodesHorizontally(ssintAliIdx);
								}
							}
						} else {
							// new selection
							System.out.println("Selecting " + ssElem.getId() + " from " + ssint.beg + " to " + ssint.end);
							if(location==TOP || location==BOTTOM) {
								cmPane.resetVerticalNodeSelection();
								cmPane.selectNodesVertically(ssintAliIdx);
							} else {
								cmPane.resetHorizontalNodeSelection();
								cmPane.selectNodesHorizontally(ssintAliIdx);
							}	
						}
					}
				}
				cmPane.repaint();
				
			}
		}
		
	}

	public void mouseEntered(MouseEvent evt) {
		//cmPane.showRulerCrosshair();	// doesn't work properly TODO why??
		cmPane.showRulerCoordinate();
		mouseMoved(evt);
	}

	public void mouseExited(MouseEvent evt) {
		//cmPane.hideRulerCrosshair();	// doesn't work properly TODO why??
		cmPane.hideRulerCoordinate();
		cmPane.repaint();
	}

	public void mousePressed(MouseEvent evt) {
		mousePressedPos = evt.getPoint();
		lastButtonPressed = evt.getButton();
	}

	public void mouseReleased(MouseEvent evt) {
	}

	public void mouseDragged(MouseEvent evt) {
		if(lastButtonPressed == MouseEvent.BUTTON1) {
			dragging = true;
			//cmPane.hideRulerCrosshair();	// doesn't work properly
			mouseDraggingPos = evt.getPoint();
			int res1 = screen2cm(mousePressedPos);
			int res2 = screen2cm(mouseDraggingPos);
			int begRes = Math.min(res1, res2);
			int endRes = Math.max(res1, res2);
			if(location == TOP || location == BOTTOM) {
				cmPane.resetVerticalNodeSelection();
				cmPane.selectNodesVertically(new Interval(begRes,endRes));
				cmPane.repaint();
			} else {
				cmPane.resetHorizontalNodeSelection();
				cmPane.selectNodesHorizontally(new Interval(begRes, endRes));
				cmPane.repaint();
			}
		}
		mouseMoved(evt);	// to update coordinates
	}

	public void mouseMoved(MouseEvent evt) {
		Point pos = evt.getPoint();
		int resSer = screen2cm(pos);
		int oneDPos = (location == TOP || location == BOTTOM)?(pos.x-offSet):pos.y;
		cmPane.setRulerCoordinates(resSer, oneDPos, location);
		cmPane.repaint();
	}

}
