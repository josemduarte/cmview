package cmview;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.BorderFactory;

import cmview.datasources.Model;

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
	
	public static final int FILL_VALUE = 10;
	
	static final long serialVersionUID = 1l;
	
	public int xs,ys; // xs and ys as startpoints of the square selection
 	public int x, y;  // x and y as endpoints of square selection
 	private int xpos, ypos;
 	//private int winwidth, winheight, winsize; // winsize is the effective (square) size
 	private int winsize; // size of the effective (square) size
 	
	public int i,j, width, height, rw, rh;
	public int rwidth, rheight; 	// distance from xs t x in x-direction (respectively ys and y)
	
	
 	public double ratio;		// scale factor
 	public int value;			// value for selections: 1--> square, 2 --> fill
 	public int trinum = 0;			// number of triangles
    
 	private boolean dragging;      // This is set to true while the user is drawing.

 	public boolean mouseIn;
	private Model mod;
	private View view;
	private boolean showCommonNeighbours = false;
	
	//public Graphics g; 					// buffered Graphic (work panel)
	//public Graphics bufferGraphics; 	// Canvas Graphic
	//public Image offscreen;				// image which is shown as graphic
	
	
	public int dim;
	//private int[] dimsi = new int[2];
	public int[] pos = new int[2];
	public int[] selrec = new int[4]; // points of selected Rectangle
	public int[][] admatrix = new int[0][];
	public int[][] selmatrix = new int[0][];
	public int[][] triangles = new int[20][];
	public int[][] resi = new int[20][];

	// constructor
	public ContactMapPane(Model mod, View view){
		this.mod = mod;
		this.view = view;
	    addMouseListener(this);
	    addMouseMotionListener(this);
	    dim = mod.getMatrixSize();
		//mod.ModelInit();
	    this.setOpaque(true); // make this component opaque
	    this.setBorder(BorderFactory.createLineBorder(Color.black));
	}
	
	protected void paintComponent(Graphics g) {
		Graphics2D bufferGraphics = (Graphics2D) g.create();
        
		// paint background
		if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
		//Insets insets = getInsets(); // get border sizes
		//int startx = insets.left; // ignore for now
		//int starty = insets.top;  // ignore for now
		
		// start drawing
		
//		  dim = mod.getMatrixSize();
//		  int[] dims = this.getMyMinimumSize(); // size of the drawing canvas
//
//	      width = dims[0];   // Width of the Contact Map
//	      height = dims[1];  // Height of the Contact Map.
	 
	      int windowSize = this.getWindowSize();
	      
	      ratio = (double)windowSize/dim;		// scale factor, = size of one contact

		  setBackground(Color.white);
		    
		  //offscreen = createImage(width, height);
		  //if (offscreen == null) System.out.println("screenimage null");
		  
		  // bufferGraphics = offscreen.getGraphics(); 
		  // clearing the buffer
	      //bufferGraphics.clearRect(0,0,(int)(dims[0]*ratio),(int)( dims[1]*ratio)); 		
	      admatrix = mod.getMatrix();            
	      selmatrix = mod.getMatrix();

	     bufferGraphics.setColor(Color.black);
	     // initialising the first contact map
	      for (i = 0; i < dim; i++){
	          for (j = 0; j < dim; j++){
	        	  
	        	  if (admatrix[i][j] > 0){
	        		  // if there is a contact, draw a rectangle
	        		  bufferGraphics.drawRect((int)(ratio*i),(int)(ratio*j),(int)(ratio*1),(int)(ratio*1));
	        		  bufferGraphics.fillRect((int)(ratio*i),(int)(ratio*j),(int)(ratio*1),(int)(ratio*1));
	        	  }
	          }
	      }

	   
	   /** distinction between square-select or fill-select*/ 
	   int selval = view.getValue();
	   
	   switch(selval){
	   
	   case 1: squareSelect(bufferGraphics);
	   case 2: fillSelect((int)(xs/ratio),(int)(ys/ratio));
	   //case 3: commonNeighbours(evt);
	   
       }
	   
	    bufferGraphics.setColor(Color.red);

//	   	for(int z = 0; z<= dim; z++){
//			for(int p = 0; p<= dim; p++){
	   	for(int z = 0; z< dim; z++){
	   		for(int p = 0; p< dim; p++){
	    
				if ((selmatrix[z][p]==10) || (selmatrix[z][p]==5)){
	         		bufferGraphics.drawRect((int)(ratio*z),(int)(ratio*p),(int)(ratio*1),(int)(ratio*1));
	         		bufferGraphics.fillRect((int)(ratio*z),(int)(ratio*p),(int)(ratio*1),(int)(ratio*1));
				}
			}
		}
	   	
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
		  //return new Dimension(width, height);
		  return new Dimension(800, 800);
	  }
	  
	  public Dimension getMaximumSize() {
		  return super.getMaximumSize();
	  }
	  
	  public int getValue(){
		  return value;
	  }
	  
	  /** 
	   * used by paintComponent to get the current window size 
	   * */
	  private int getWindowSize(){
			 winsize = Math.min(getWidth(), getHeight()); // size of drawing square
			 return winsize;
	  }

	 
//   public void update(Graphics g, MouseEvent evt) {
//	   paint(g, evt);
//   }

   
//   /** Allows user interaction --> selections and region growing */
//   public void paint(Graphics g){
//
//		  dim = mod.getMatrixSize();
//		  int[] dims = this.getMyMinimumSize();
//
//	      width = dims[0];   // Width of the Contact Map
//	      height = dims[1];  // Height of the Contact Map.
//	 
//	      ratio = (double)width/dim;		// scale factor
//
//		  setBackground(Color.white);
//		    
//		  //offscreen = createImage(width, height);
//		  //if (offscreen == null) System.out.println("screenimage null");
//		  
//		  // bufferGraphics = offscreen.getGraphics(); 
//		  // clearing the buffer
//	      bufferGraphics.clearRect(0,0,(int)(dims[0]*ratio),(int)( dims[1]*ratio)); 		
//	      admatrix = mod.getMatrix();            
//	      selmatrix = mod.getMatrix();
//
//	     bufferGraphics.setColor(Color.black);
//	     // initialising the first contact map
//	      for (i= 0; i<dim; i++){
//	          for (j= 0; j<dim; j++){
//	        	  
//	        	  if (admatrix[i][j] ==1){
//	        		  // if there is a contact, draw a rectangle
//	        		  bufferGraphics.drawRect((int)(ratio*i),(int)(ratio*j),(int)(ratio*1),(int)(ratio*1));
//	        		  bufferGraphics.fillRect((int)(ratio*i),(int)(ratio*j),(int)(ratio*1),(int)(ratio*1));
//	        	  }
//	          }
//	      }
//
//	   
//	   /** distinction between square-select or fill-select*/ 
//	   int selval = view.getValue();
//	   
//	   switch(selval){
//	   
//	   case 1: squareSelect();
//	   case 2: fillSelect((int)(xs/ratio),(int)(ys/ratio));
//	   //case 3: commonNeighbours(evt);
//	   
//       }
//	   
//	    bufferGraphics.setColor(Color.red);
//
////	   	for(int z = 0; z<= dim; z++){
////			for(int p = 0; p<= dim; p++){
//	   	for(int z = 0; z< dim; z++){
//	   		for(int p = 0; p< dim; p++){
//	    
//				if ((selmatrix[z][p]==10) || (selmatrix[z][p]==5)){
//	         		bufferGraphics.drawRect((int)(ratio*z),(int)(ratio*p),(int)(ratio*1),(int)(ratio*1));
//	         		bufferGraphics.fillRect((int)(ratio*z),(int)(ratio*p),(int)(ratio*1),(int)(ratio*1));
//				}
//			}
//		}
//	   
//       g = this.getGraphics();
//       g.drawImage(offscreen,0,0,this);
//   }
   
   
   /** SQUARE SELECTION: from left upper to right lower corner of a rectangle */
   public void squareSelect(Graphics2D bufferGraphics){
	   
	   bufferGraphics.setColor(Color.black);
	   bufferGraphics.drawRect(xs,ys,rwidth,rheight);
       
       // Marking the selected contacts red
	   bufferGraphics.setColor(Color.red);
	   
       for (int xsi= (int)(xs/(double)ratio); xsi <= (int)((xs +rwidth)/(double)ratio); xsi++){
           for (int ysj= (int)(ys/(double)ratio); ysj<= (int)((ys +rheight)/(double)ratio); ysj++){
         	  
         	  if (admatrix[xsi][ysj] ==1){
         		  // if there is a contact, mark up
         	    selmatrix[xsi][ysj]=5;
         	  }
           }
       }
       
       int[] selrect = {(int)(xs/(double)ratio), (int)(ys/(double)ratio), (int)((rwidth + xs)/(double)ratio), (int)((rheight +ys)/(double)ratio)};
       selrec = selrect;
       
//		int xs = selrec[0];
//		int ys = selrec[1];
//		int rw = selrec[2];
//		int rh = selrec[3];
//		
//		System.out.println("Residues: "+ xs + "\t"+ ys + "\t"+ rw + "\t"+ rh);
		
   }
  
   public void fillSelect(int x, int y){
	   //System.out.println("RegGrow: "+ x + "\t"+ y);


	   if ((x < 0) || (y < 0) || (x >= dim) || (y >= dim)) {
		   return;
	   } else {

		   if (selmatrix[x][y]==0){
			   return;
		   }

		   if (selmatrix[x][y]==FILL_VALUE){
			   return;
		   }
		   else {
			   selmatrix[x][y]=FILL_VALUE;	
			   //System.out.println("matrix ist EINS");

			   // 1 distance
			   fillSelect(x-1,y);
			   fillSelect(x+1,y);
			   fillSelect(x,y-1);
			   fillSelect(x,y+1);

			   // 2 distance
			   fillSelect(x-2,y);
			   fillSelect(x+2,y);
			   fillSelect(x,y-2);
			   fillSelect(x,y+2);
			   fillSelect(x-1,y+1);
			   fillSelect(x+1,y+1);
			   fillSelect(x-1,y-1);
			   fillSelect(x+1,y-1);
		   }
	   }
   }
   
   /** returns the coordinates of the upper left and lower right points of the rectangle */
   public int[] getSelectRect(){
	 return  selrec;
   }
   
   public int[][] getSelectMatrix(){
		 return  selmatrix;
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
	   
       xs = evt.getX();   
	   ys = evt.getY(); 
	
	   dragging = true;
	}
   
  public int[] getPosition(){
	  return pos;
   }
   

   public void mouseReleased(MouseEvent evt) {
       // Called whenever the user releases the mouse button.
	   
	   if (evt.isPopupTrigger()) {
		   showPopup(evt);
		   return;
	   }
	   
       if (dragging == false)
          return;  // Nothing to do because the user isn't drawing.
      
       rwidth = x-xs; 	// difference between start and end point in x-direction
       rheight = y-ys;	// s.o.: respectively y
       
       rw = Math.abs(x-xs); // positive difference
       rh = Math.abs(y-ys);
       
       /** Doing some Selections */
       //paint(g, evt);
    
       dragging = false;
       
	   int sel = view.getValue();
	   
	   if (sel == 3){
		   this.showCommonNeighbours = true;
		   //this.commonNeighbours(evt);
		   this.repaint();
	   }
	  
   }

   public void mouseDragged(MouseEvent evt) {
       // Called whenever the user moves the mouse
       // while a mouse button is held down. 

       if (dragging == false)
    	   	return;
 
       x = evt.getX();   // x-coordinate of mouse.
       y = evt.getY();   // y=coordinate of mouse.

       // show selection box and coordinates while dragging
       
       rwidth = x-xs; 	// difference between start and end point in x-direction
       rheight = y-ys;	// s.o.: respectively y
       
       rw = Math.abs(x-xs); // positive difference
       rh = Math.abs(y-ys);
       
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
	   xpos = evt.getX();
	   ypos = evt.getY();
	   
	   int[] posxy = {xpos, ypos};
	   pos = posxy;
	   //this.update(g, evt);
	   //this.drawCoordinates();
	   this.repaint();
	   }
   
//   /** Method to show the contact map just after application is started */
//   public void showContactMap() {
//	   mouseIn = true;
//	   xpos = 100;
//	   ypos = 100;
//	   int[] posxy = {xpos, ypos};
//	   pos = posxy;
//	   this.update(g, null);
//	   this.drawCoordinates(bufferGraphics);
//   }
   
   public void showPopup(MouseEvent e) {
       view.popup.show(e.getComponent(), e.getX(), e.getY());
   }
   
   public void drawCoordinates(Graphics2D bufferGraphics){

	   bufferGraphics.setColor(Color.red);
	   int[] temp = this.getPosition();
	   
	   if ((mouseIn == true) && (xpos <= winsize) && (ypos <= winsize)){
		   
		  // writing the coordinates at lower left corner
		  bufferGraphics.setColor(Color.blue);
		  bufferGraphics.drawString("( Y   " + ",  X )", 5, winsize-50);
		  bufferGraphics.drawString("(i_num   " + ",  j_num )", 5, winsize-30);
		  bufferGraphics.drawString("(" + (int)(1+temp[1]/ratio)+"," + (int)(1+temp[0]/ratio)+")", 5, winsize-10);
		  
		  // drawing the cross-hair
		  bufferGraphics.setColor(Color.green);
		  bufferGraphics.drawLine(xpos, 0, xpos, winsize);
		  bufferGraphics.drawLine(0, ypos, winsize, ypos);
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

		   if(admatrix[xs][m]==1){

				   if(admatrix[ys][m]==1){
					   	   
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

		   if(admatrix[xs][m]==1){
			   int lowtri = m;
			   int xnew = m;

			   if (admatrix[xnew][ys]==1){
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

		   if(admatrix[m][ys]==1){

				   if (admatrix[m][xs]==1){
				  
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

//	      g = this.getGraphics();
//	      g.drawImage(offscreen,0,0,this);

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

