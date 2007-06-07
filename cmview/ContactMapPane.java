package cmview;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.BorderFactory;

import cmview.datasources.Model;
import proteinstructure.*;

/**
 * The panel containing the contact map and associated event handling.
 * 
 * @author:		Henning Stehr
 * Class: 		ContactMapPane (replaces PaintController)
 * Package: 	cm2pymol
 * Date:		22/05/2007, last updated: 22/05/2007
 * 
 * TODO:
 * - use JPanel instead of Canvas
 * - draw crosshair and coordinates on separate Panel so that the contact map can be saved as image
 */

public class ContactMapPane extends JPanel
implements MouseListener, MouseMotionListener {

	// constants
	static final long serialVersionUID = 1l;

	private int xs, ys;              //  x, y start of the square selection
	private Point squareSelStart;   //  start point of square selection
	private Point squareSelEnd;     //  end point of square selection
	private Point pos;              //  current position of mouse
	
	private int winsize;          // size of the effective (square) size

	private double ratio;		  // scale factor
	private int trinum = 0;		  // number of triangles

	private boolean dragging;     // set to true while the user is dragging

	private boolean mouseIn;
	//private Model mod;          //TODO not sure if we need the full Model object here, commenting it out for now
	private View view;
	private boolean showCommonNeighbours = false;

	private int dim;


	private ContactList allContacts; // contains all contacts in contact map
	private ContactList selContacts; // contains permanent list of contacts selected
	private ContactList tmpContacts; // contains transient list of contacts selected while dragging

	//private int[][] triangles = new int[20][];
	private int[][] resi = new int[20][];

	/**
	 * Constructor
	 * @param mod
	 * @param view
	 */
	public ContactMapPane(Model mod, View view){
		//this.mod = mod;
		this.view = view;
		addMouseListener(this);
		addMouseMotionListener(this);
		this.dim = mod.getMatrixSize();
		//mod.ModelInit();
		this.setOpaque(true); // make this component opaque
		this.setBorder(BorderFactory.createLineBorder(Color.black));

		this.allContacts = mod.getContacts();
		this.selContacts = new ContactList();
		this.pos = new Point();
	}

	protected void paintComponent(Graphics g) {
		Graphics2D bufferGraphics = (Graphics2D) g.create();

		// paint background
		if (isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}

		int windowSize = this.getWindowSize();

		ratio = (double)windowSize/dim;		// scale factor, = size of one contact

		setBackground(Color.white);

		// drawing the contact map (initially and on repaint)
		bufferGraphics.setColor(Color.black);
		for (Contact cont:allContacts){ 
			// if there is a contact, draw a rectangle
			int x = cm2screen(cont).x;
			int y = cm2screen(cont).y;
			bufferGraphics.drawRect(x,y,(int)(ratio*1),(int)(ratio*1));
			bufferGraphics.fillRect(x,y,(int)(ratio*1),(int)(ratio*1));
		}

		// drawing selection rectangle if dragging mouse and showing temp selection in red (tmpContacts)
		if (dragging) {
			bufferGraphics.setColor(Color.black);
			int xmin = Math.min(squareSelStart.x,squareSelEnd.x);
			int ymin = Math.min(squareSelStart.y,squareSelEnd.y);
			int xmax = Math.max(squareSelStart.x,squareSelEnd.x);
			int ymax = Math.max(squareSelStart.y,squareSelEnd.y);
			bufferGraphics.drawRect(xmin,ymin,xmax-xmin,ymax-ymin);
			
			bufferGraphics.setColor(Color.red);
			for (Contact cont:tmpContacts){ 
				// if there is a contact, draw a rectangle
				int x = cm2screen(cont).x;
				int y = cm2screen(cont).y;
				bufferGraphics.drawRect(x,y,(int)(ratio*1),(int)(ratio*1));
				bufferGraphics.fillRect(x,y,(int)(ratio*1),(int)(ratio*1));
			}
		}

		// showing permanent selection in red
		bufferGraphics.setColor(Color.red);
		for (Contact cont:selContacts){
			int x = cm2screen(cont).x;
			int y = cm2screen(cont).y;
			bufferGraphics.drawRect(x,y,(int)(ratio*1),(int)(ratio*1));
			bufferGraphics.fillRect(x,y,(int)(ratio*1),(int)(ratio*1));
		}

		// drawing coordinates on lower left corner (following crosshairs)
		drawCoordinates(bufferGraphics);
		
		if(this.showCommonNeighbours) {
			commonNeighbours(bufferGraphics);
			this.showCommonNeighbours = false;
		}

	}


	public Dimension getMinimumSize() {
		return super.getMinimumSize();
	}

	public Dimension getPreferredSize() {
		return new Dimension(800, 800);
	}

	public Dimension getMaximumSize() {
		return super.getMaximumSize();
	}

	/** 
	 * used by paintComponent to get the current window size 
	 */
	private int getWindowSize(){
		winsize = Math.min(getWidth(), getHeight()); // size of drawing square
		return winsize;
	}

	/**
	 * Square selection: from left upper to right lower corner of a rectangle
	 * @param upperLeft
	 * @param lowerRight 
	 */
	public void squareSelect(Contact upperLeft, Contact lowerRight){
		// we reset the tmpContacts list so every new mouse selection starts from a blank list
		tmpContacts = new ContactList();

		int imin = Math.min(upperLeft.i,lowerRight.i);
		int jmin = Math.min(upperLeft.j,lowerRight.j);
		int imax = Math.max(upperLeft.i,lowerRight.i);
		int jmax = Math.max(upperLeft.j,lowerRight.j);
		// we loop over all contacts so time is o(number of contacts) instead of looping over the square (o(n2) being n size of square)
		for (Contact cont:allContacts){
			if (cont.i<=imax && cont.i>=imin && cont.j<=jmax && cont.j>=jmin){
				tmpContacts.add(cont);
			}
		}
	}

	/**
	 * Fill selection
	 * @param cont contact where mouse has been clicked
	 */
	public void fillSelect(Contact cont){
		int i = cont.i;
		int j = cont.j;
		if ((i < 1) || (j < 1) || (i > dim) || (j > dim)) {
			return;
		} else {
			if (!allContacts.contains(cont)){
				return;
			}

			if (selContacts.contains(cont)){
				return;
			}
			else {
				selContacts.add(cont);

				// 1 distance
				fillSelect(new Contact(i-1,j));
				fillSelect(new Contact(i+1,j));
				fillSelect(new Contact(i,j-1));
				fillSelect(new Contact(i,j+1));

				// 2 distance
				fillSelect(new Contact(i-2,j));
				fillSelect(new Contact(i+2,j));
				fillSelect(new Contact(i,j-2));
				fillSelect(new Contact(i,j+2));
				fillSelect(new Contact(i-1,j+1));
				fillSelect(new Contact(i+1,j+1));
				fillSelect(new Contact(i-1,j-1));
				fillSelect(new Contact(i+1,j-1));
			}
		}
	}

	public ContactList getSelContacts(){
		return selContacts;
	}

	/**
	 * Returns a contact given screen coordinates
	 * @param dim
	 * @return
	 */
	private Contact screen2cm(Point point){
		return new Contact((int) Math.ceil(point.y/ratio),(int) Math.ceil(point.x/ratio));
	}

	/**
	 * Returns upper left corner of the square representing the contact
	 * @param cont
	 * @return
	 */
	private Point cm2screen(Contact cont){
		return new Point((int) Math.round((cont.j-1)*ratio), (int) Math.round((cont.i-1)*ratio));
	}

	/** ############################################### */
	/** ############    MOUSE EVENTS   ################ */
	/** ############################################### */   

	public void mousePressed(MouseEvent evt) {
		// This is called when the user presses the mouse anywhere
		// in the frame

		if (evt.isPopupTrigger()) {
			showPopup(evt);
			return;
		}

		squareSelStart = new Point(evt.getX(),evt.getY());

	}

	public void mouseReleased(MouseEvent evt) {
		// Called whenever the user releases the mouse button.

		if (evt.isPopupTrigger()) {
			showPopup(evt);
			return;
		}
		// from now only if left click (BUTTON1)
		if (evt.getButton()==MouseEvent.BUTTON1) {

			switch (view.getCurrentAction()) {
			case View.SHOW_COMMON_NBH:
				showCommonNeighbours = true;
				//this.commonNeighbours(evt);
				this.repaint();
				return;
				
			case View.SQUARE_SEL:
				if(!dragging) {
					// resets selContacts when clicking mouse (not dragging)
					selContacts = new ContactList();
				} else {
					if (evt.isControlDown()){
						selContacts.addAll(tmpContacts);
					} else{
						selContacts = new ContactList();
						selContacts.addAll(tmpContacts);
					}
				}
				dragging = false;
				return;
				
			case View.FILL_SEL:
				// resets selContacts when clicking mouse
				if (!evt.isControlDown()){
					selContacts = new ContactList();
				}
				fillSelect(screen2cm(new Point(evt.getX(),evt.getY())));
				return;
			}

		}
	}

	public void mouseDragged(MouseEvent evt) {
		// Called whenever the user moves the mouse
		// while a mouse button is held down. 

		dragging = true;
		squareSelEnd = new Point(evt.getX(),evt.getY());

		squareSelect(screen2cm(squareSelStart), screen2cm(squareSelEnd));
		
		mouseMoved(evt);
	} 

	public void mouseEntered(MouseEvent evt) { 
		mouseIn=true;
	}
	
	public void mouseExited(MouseEvent evt) {
	}
	
	public void mouseClicked(MouseEvent evt) {
	}
	
	public void mouseMoved(MouseEvent evt) {
		pos = new Point(evt.getX(), evt.getY());
		this.repaint();
	}

	public void showPopup(MouseEvent e) {
		view.popup.show(e.getComponent(), e.getX(), e.getY());
	}

	public void drawCoordinates(Graphics2D bufferGraphics){

		bufferGraphics.setColor(Color.red);

		if ((mouseIn == true) && (pos.x <= winsize) && (pos.y <= winsize)){

			// writing the coordinates at lower left corner
			bufferGraphics.setColor(Color.blue);
			bufferGraphics.drawString("( Y   " + ",  X )", 5, winsize-50);
			bufferGraphics.drawString("(i_num   " + ",  j_num )", 5, winsize-30);
			bufferGraphics.drawString("(" + (int)(1+pos.y/ratio)+"," + (int)(1+pos.x/ratio)+")", 5, winsize-10);

			// drawing the cross-hair
			bufferGraphics.setColor(Color.green);
			bufferGraphics.drawLine(pos.x, 0, pos.x, winsize);
			bufferGraphics.drawLine(0, pos.y, winsize, pos.y);
		}

	}

	public void commonNeighbours(Graphics2D bufferGraphics){

		trinum=0;
		System.out.println("Show: "+ xs+"\t"+ys);
		xs = (int)(xs/ratio);
		ys = (int)(ys/ratio);
		this.drawCorridor((int)(xs*ratio), (int)(ys*ratio), bufferGraphics);

		// drawing the selected point
		bufferGraphics.setColor(Color.blue);

		this.markingPoints(xs,ys,ratio, bufferGraphics);
		System.out.println("Show: "+ xs+"\t"+ys);

		/** creating common neighbour triangle above the choosen point (red triangles) */
		// searching in vertical direction the y-axis till ys-position
		for (int m = 0; m<= ys; m++){
			Contact contxs = new Contact(xs+1,m+1);
			//if(admatrix[xs][m]==1){
			if (allContacts.contains(contxs)){
				Contact contys = new Contact(ys+1,m+1);
				//if(admatrix[ys][m]==1){
				if (allContacts.contains(contys)){

					bufferGraphics.setColor(Color.blue);
					this.markingPoints(xs,m,ratio, bufferGraphics);
					this.markingPoints(ys,m,ratio, bufferGraphics);

					bufferGraphics.setColor(Color.red);
					this.drawingLine(xs, ys, xs, m, ys, m, ratio, bufferGraphics);

					bufferGraphics.setColor(Color.lightGray);

					bufferGraphics.drawLine((int)(ys*ratio),(int)(m*ratio),(int)(m*ratio), (int)(m*ratio));
					bufferGraphics.drawLine((int)(m*ratio),(int)(m*ratio),(int)(m*ratio), (int)(xs*ratio));

					this.fillResidueMatrix(xs, ys, m, trinum);
					trinum = trinum+1;
				}
			}
		}

		/** creating common neighbour triangle under the choosen point */
		// searching in vertical direction the y-axis from ys-position
		for (int m = ys; m< dim; m++){
			Contact contxs = new Contact(xs+1,m+1);
			//if(admatrix[xs][m]==1){
			if (allContacts.contains(contxs)){  
				int lowtri = m;
				int xnew = m;
				Contact contys = new Contact(ys+1,m+1);
				//if (admatrix[xnew][ys]==1){
				if (allContacts.contains(contys)){
					System.out.println("Found next contact of residue: " + xnew + "\t"+ ys);

					bufferGraphics.setColor(Color.blue);
					this.markingPoints(xs,lowtri,ratio, bufferGraphics);
					this.markingPoints(xnew,ys,ratio, bufferGraphics);

					bufferGraphics.setColor(Color.yellow);
					this.drawingLine(xs, ys, xs, lowtri, xnew, ys, ratio, bufferGraphics);

					bufferGraphics.setColor(Color.lightGray);
					bufferGraphics.drawLine((int)(ys*ratio),(int)(lowtri*ratio),(int)(lowtri*ratio), (int)(lowtri*ratio));
					bufferGraphics.drawLine((int)(lowtri*ratio),(int)(lowtri*ratio),(int)(lowtri*ratio), (int)(xs*ratio));

					this.fillResidueMatrix(xs, ys, lowtri, trinum);
					trinum = trinum+1;

				}
			}
		}

		/** creating common neighbour triangle to the right of the choosen point */
		// searching in horizontal direction the x-axis beginning at xs-position
		for (int m = xs; m< dim; m++){
			Contact contys = new Contact(m+1,ys+1);
			//if(admatrix[m][ys]==1){
			if (allContacts.contains(contys)){
				Contact contxs = new Contact(m+1,xs+1);
				//if (admatrix[m][xs]==1){
				if (allContacts.contains(contxs)){

					bufferGraphics.setColor(Color.blue);
					this.markingPoints(m, ys,ratio, bufferGraphics);
					this.markingPoints(m,xs,ratio, bufferGraphics);

					bufferGraphics.setColor(Color.cyan);
					this.drawingLine(xs, ys, m, ys, m, xs, ratio, bufferGraphics);

					bufferGraphics.setColor(Color.lightGray);
					bufferGraphics.drawLine((int)(m*ratio),(int)(xs*ratio),(int)(m*ratio), (int)(m*ratio));
					bufferGraphics.drawLine((int)(ys*ratio),(int)(m*ratio),(int)(m*ratio), (int)(m*ratio));

					this.fillResidueMatrix(xs, ys, m, trinum);
					trinum = trinum+1;

				}
			}
		}

//		g = this.getGraphics();
//		g.drawImage(offscreen,0,0,this);

	}


	public int getTriangleNumber(){
		return trinum;
	}

	public void markingPoints(int x, int y, double ratio, Graphics2D bufferGraphics){

		bufferGraphics.drawLine((int)(x*ratio)-3, (int)(y*ratio)-3,(int)(x*ratio)+3, (int)(y*ratio)+3 );
		bufferGraphics.drawLine((int)(x*ratio)-3, (int)(y*ratio)+3,(int)(x*ratio)+3, (int)(y*ratio)-3 );
		bufferGraphics.drawLine((int)(x*ratio)-2, (int)(y*ratio)-3,(int)(x*ratio)+2, (int)(y*ratio)+3 );
		bufferGraphics.drawLine((int)(x*ratio)-2, (int)(y*ratio)+3,(int)(x*ratio)+2, (int)(y*ratio)-3 );
	}

	// connecting left upper point(lu), right upper(ru) point and right lower(rl) points via drawline-method 
	public void drawingLine(int xlu, int ylu, int xru, int yru, int xrl, int yrl, double ratio, Graphics2D bufferGraphics){

		bufferGraphics.drawLine((int)(xlu*ratio),(int)( ylu*ratio), (int)(xru*ratio), (int)(yru*ratio));
		bufferGraphics.drawLine((int)(xru*ratio),(int)( yru*ratio), (int)(xrl*ratio), (int)(yrl*ratio));
		bufferGraphics.drawLine((int)(xrl*ratio),(int)( yrl*ratio), (int)(xlu*ratio), (int)(ylu*ratio));

	}

	public void drawCorridor(int x, int y, Graphics2D bufferGraphics){

		bufferGraphics.setColor(Color.gray);
		// Horizontal Lines
		bufferGraphics.drawLine(0, y, y, y);
		bufferGraphics.setColor(Color.green);
		bufferGraphics.drawLine(0, x, x, x);
		// vertical Lines
		bufferGraphics.drawLine(y,y,y,(int)(dim*ratio));
		bufferGraphics.setColor(Color.gray);
		bufferGraphics.drawLine(x,x,x,(int)(dim*ratio)); 
	}

	public int[][] fillResidueMatrix(int res1, int res2, int res3, int trinum){
		resi[trinum] = new int[3];
		resi[trinum][0] = res1;
		resi[trinum][1] = res2;
		resi[trinum][2] = res3;
		return resi;
	}

	public int [][] getResidues(){
		return resi;
	}

} 

