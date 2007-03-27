

import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.lang.Math.*;
import java.awt.event.MouseEvent.*;

import javax.swing.JTextField;
import java.awt.image.BufferedImage;


public class PaintController extends Canvas
                implements MouseListener, MouseMotionListener {

	public int xs,ys; // xs and ys as startpoints of the square selection
 	public int x, y;  // x and y as endpoints of square selection
 	private int xpos, ypos;
 	public int winwidth, winheight;
 	public double ratio;
 	public int value;
 	public int trinum;

 	private boolean dragging;      // This is set to true while the user is drawing.

 	private boolean mouseIn;
 	private boolean selected;
 	public boolean mousemoved;

 	private Start start;
	private Model mod;
	private View view;


	public Graphics g; 	//buffered Graphic
	public Graphics bufferGraphics; 	//Canvas Graphic
	public Image offscreen;
	private BufferedImage buffer;
	
	public int i,j, width, height, rw, rh;
	public int rwidth, rheight; 	// distance from xs t x in x-direction (respectively ys and y)
	
	public int[] dim = new int[2];
	public int[] dimsi = new int[2];
	public int[] pos = new int[2];
	public int[] selrec = new int[4]; // points of selected Rectangle
	public int[][] admatrix = new int[0][];
	public int[][] selmatrix = new int[0][];
	public int[][] triangles = new int[20][];
	public int[][] resi = new int[20][];
	public int selNum;
	


	public PaintController(Start start, Model mod, View view){
		this.start=start;
		this.mod = mod;

		mod.ModelInit();
		this.view=view;
	    addMouseListener(this);
	    addMouseMotionListener(this);

	}
	
	
	  public Dimension getMinimumSize() {
	  //return new Dimension(width, height);
		  return new Dimension(1200, 800);
	  }
	/** Window Size */
	  public int[] getMyMinimumSize() {
		 // Get the size of the default screen
		 Dimension screendim = new Dimension (1200, 800);
		 //System.out.println(screendim.getWidth() + "\t"+ screendim.getHeight());
		 dim = mod.getMatrixSize();
		 
		 winwidth = getWidth();
		 winheight = getHeight();
		 // create new Dimension of application window
		 Dimension appdim = new Dimension(dim[0]*4, dim[1]*4); 
		 
		 if (appdim.getHeight() > screendim.getHeight() ){
			 // Contact Map should always be a square --> height is smaller size to 
			 // create an optimal map
			 double screenratio = (double)(screendim.getHeight()/ appdim.getHeight());

			 appdim.setSize(appdim.getHeight()*screenratio, appdim.getHeight()*screenratio);
			 
		 }
		 
		 if (winwidth>=appdim.getWidth() || winheight >= appdim.getHeight()){

			 appdim = new Dimension(winheight, winheight);
		 }
			  

		 int[]dimsi= {(int)appdim.getHeight(), (int)appdim.getHeight()};
		  return dimsi;
	  }
	  
	  
	  public int[] getWindowSize(){
		  return dimsi;
	  }
	  
	  
	  public Dimension getPreferredSize() {
		  return getMinimumSize();
	  }
	  
	  
	  public int getValue(){
		  return value;
	  }

	 
   public void update(Graphics g, MouseEvent evt) {
	   paint(g, evt);
   }

   
   /** Allows user interaction --> selections and region growing */
   public void paint(Graphics g, MouseEvent evt){

		  dim = mod.getMatrixSize();
		  
		  int[] dims = this.getMyMinimumSize();

		  
	      width = dims[0];   // Width of the Contact Map
	      height = dims[1];  // Height of the Contact Map.
	 
	      ratio = (double)width/dim[0];

		  setBackground(Color.white);
		    
		  offscreen = createImage(width, height);
		  if (offscreen == null) System.out.println("screenimage null");
		  
		  bufferGraphics = offscreen.getGraphics(); 
	      bufferGraphics.clearRect(0,0,(int)(dims[0]*ratio),(int)( dims[1]*ratio));
	      admatrix = mod.getMatrix();            
	      selmatrix = mod.getMatrix();

	     bufferGraphics.setColor(Color.black);

	      for (i= 0; i<dim[0]; i++){
	          for (j= 0; j<dim[1]; j++){
	        	  
	        	  if (admatrix[i][j] ==1){
	        		  // if there is a contact, draw a rectangle
	        		  bufferGraphics.drawRect((int)(ratio*i),(int)(ratio*j),(int)(ratio*1),(int)(ratio*1));
	        		  bufferGraphics.fillRect((int)(ratio*i),(int)(ratio*j),(int)(ratio*1),(int)(ratio*1));
	        	  }
	          }
	      }

	   
	   /** distinction between square-select or fill-select*/ 
	   int selval = view.getValue();
	   
	   switch(selval){
	   
	   case 1: squareSelect(evt);
	   case 2: regionGrow((int)(xs/ratio),(int)(ys/ratio));
	   
       }
	   
	    bufferGraphics.setColor(Color.red);
		for(int z = 0; z<= (int)(dim[0]); z++){
			for(int p = 0; p<= (int)(dim[0]); p++){
				if ((selmatrix[z][p]==10) || (selmatrix[z][p]==5)){
	         		bufferGraphics.drawRect((int)(ratio*z),(int)(ratio*p),(int)(ratio*1),(int)(ratio*1));
	         		bufferGraphics.fillRect((int)(ratio*z),(int)(ratio*p),(int)(ratio*1),(int)(ratio*1));

				}
				
			}
			
		}
	   
       g = this.getGraphics();
       g.drawImage(offscreen,0,0,this);
   }
   
   
   /** SQUARE SELECTION: from left upper to right lower corner of a rectangle */
   public void squareSelect(MouseEvent evt){
	   
	   bufferGraphics.setColor(Color.black);
	   bufferGraphics.drawRect(xs,ys,rwidth,rheight);
       
       // Marking the selected contacts red
	   bufferGraphics.setColor(Color.red);
	   
       for (int xsi= (int)(xs/(double)ratio); xsi <= (int)((xs +rwidth)/(double)ratio); xsi++){
           for (int ysj= (int)(ys/(double)ratio); ysj<= (int)((ys +rheight)/(double)ratio); ysj++){
         	  
         	  if (admatrix[xsi][ysj] ==1){
         		  // if there is a contact, draw a 1-by-1 rectangle
         	    selmatrix[xsi][ysj]=5;
         		//bufferGraphics.drawRect((int)(ratio*xsi),(int)(ratio*ysj),(int)(ratio*1),(int)(ratio*1));
         		//bufferGraphics.fillRect((int)(ratio*xsi),(int)(ratio*ysj),(int)(ratio*1),(int)(ratio*1));

         	  }
           }
       }
       
       int[] selrect = {(int)(xs/(double)ratio), (int)(ys/(double)ratio), (int)((rwidth + xs)/(double)ratio), (int)((rheight +ys)/(double)ratio)};
       selrec = selrect;
       
		int xs = selrec[0];
		int ys = selrec[1];
		int rw = selrec[2];
		int rh = selrec[3];
		
		System.out.println("Residues: "+ xs + "\t"+ ys + "\t"+ rw + "\t"+ rh);
		
		selected = true;
   }


  
   public void regionGrow(int x, int y){
	   System.out.println("RegGrow: "+ x + "\t"+ y);
	   

	   if ((x==0) | (y==0)){
		  // System.out.println("x und y sind null");
	   }
	   while((x<=dim[0]) && (y<=dim[0])){
		   
	   if (selmatrix[x][y]==0){
		   //System.out.println("matrix ist null");
		   return;
	   }
	   
	   if (selmatrix[x][y]==10){
		   //System.out.println("matrix ist 10");
		   return;
	   }
	   else {
		   	   selmatrix[x][y]=10;	
		   	  System.out.println("matrix ist EINS");
		   	  
		   	  /*
			   bufferGraphics.setColor(Color.red);
			   bufferGraphics.drawRect((int)(x*ratio),(int)(y*ratio),(int)(1*ratio),(int)(1*ratio));
			   bufferGraphics.fillRect((int)(x*ratio),(int)(y*ratio),(int)(1*ratio),(int)(1*ratio));
*/
			   // 1 distance
			   regionGrow(x-1,y);
			   regionGrow(x+1,y);
			   regionGrow(x,y-1);
			   regionGrow(x,y+1);
			   
			   // 2 distance
			   regionGrow(x-2,y);
			   regionGrow(x+2,y);
			   regionGrow(x,y-2);
			   regionGrow(x,y+2);
			   regionGrow(x-1,y+1);
			   regionGrow(x+1,y+1);
			   regionGrow(x-1,y-1);
			   regionGrow(x+1,y-1);
		   
		   
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
       xs = evt.getX();   
	   ys = evt.getY(); 
	
	   dragging = true;
	}
   
  public int[] getPosition(){
	  return pos;
   }
   

   public void mouseReleased(MouseEvent evt) {
           // Called whenever the user releases the mouse button.
	   
       if (dragging == false)
          return;  // Nothing to do because the user isn't drawing.
      
       rwidth = x-xs; 	// difference between start and end point in x-direction
       rheight = y-ys;	// s.o.: respectively y
       
       rw = Math.abs(x-xs); // positive difference
       rh = Math.abs(y-ys);
       
       /** Creating the Contact Map for Selections */
       //myPaint(g);
       /** Doing some Selections */
       paint(g, evt);
    
       dragging = false;
       
	   int sel = view.getValue();
	   
	   if (sel ==3){
	   this.commonNeighbours(evt);
	   }
	  
   }

   public void mouseDragged(MouseEvent evt) {
            // Called whenever the user moves the mouse
            // while a mouse button is held down. 

       if (dragging == false)

    	   return;
         // return;  // Nothing to do because the user isn't drawing.
       
       x = evt.getX();   // x-coordinate of mouse.
       y = evt.getY();   // y=coordinate of mouse.

   } 
   
 
   public void mouseEntered(MouseEvent evt) { 
	   mouseIn=true;
	   
   }   
   public void mouseExited(MouseEvent evt) {
   }    											
   public void mouseClicked(MouseEvent evt) {
   }   
   public void mouseMoved(MouseEvent evt) {
	   mousemoved = true;
	   xpos = evt.getX();
	   ypos = evt.getY();
	   
	   int[] posxy = {xpos, ypos};
	   pos = posxy;
	   this.update(g, evt);

	   this.drawCoordinates();
	   }
   
   
   public void drawCoordinates(){

       //bufferGraphics = offscreen.getGraphics(); 
	   bufferGraphics.setColor(Color.red);
	   int[] temp = this.getPosition();
	   
	   if ((mouseIn == true) && (xpos <= winwidth) && (ypos <= winheight)){
	  // writing the coordinates at lower left corner
	  bufferGraphics.setColor(Color.blue);
	  bufferGraphics.drawString("( X   " + ",  Y )", 0, winheight-50);
	  bufferGraphics.drawString("(j_num   " + ",  i_num )", 0, winheight-30);
	  bufferGraphics.drawString("(" + (int)(temp[0]/ratio)+"," + (int)(temp[1]/ratio)+")", 0, winheight-10);
	  // drawing the cross-hair
	  bufferGraphics.setColor(Color.green);
	  bufferGraphics.drawLine(xpos, 0, xpos, winheight);
	  bufferGraphics.drawLine(0, ypos, winwidth, ypos);
	   }
	  g = this.getGraphics();
	  g.drawImage(offscreen,0,0,this);
   }
   
   
   
   public void commonNeighbours(MouseEvent evt){
	
	   trinum=0;
	   System.out.println("Show: "+ xs+"\t"+ys);
	   xs = (int)(xs/ratio);
	   ys = (int)(ys/ratio);
	   this.drawCorridor((int)(xs*ratio), (int)(ys*ratio));
	   
	   // drawing the selected point
	   bufferGraphics.setColor(Color.blue);

	   this.markingPoints(xs,ys,ratio);
	   System.out.println("Show: "+ xs+"\t"+ys);

	   /** creating common neighbour triangle above the choosen point (red triangles) */
	   // searching in vertical direction the y-axis till ys-position
	   for (int m = 0; m<= ys; m++){

		   if(admatrix[xs][m]==1){

				   if(admatrix[ys][m]==1){
					   	   
						   bufferGraphics.setColor(Color.blue);
						   this.markingPoints(xs,m,ratio);
						   this.markingPoints(ys,m,ratio);
					
						   bufferGraphics.setColor(Color.red);
						   this.drawingLine(xs, ys, xs, m, ys, m, ratio);
						
						   bufferGraphics.setColor(Color.lightGray);
		
						   bufferGraphics.drawLine((int)(ys*ratio),(int)(m*ratio),(int)(m*ratio), (int)(m*ratio));
						   bufferGraphics.drawLine((int)(m*ratio),(int)(m*ratio),(int)(m*ratio), (int)(xs*ratio));
						   
						   this.fillTriangleMatrix(xs, ys, xs, m, ys, m, trinum);
						   this.fillResidueMatrix(xs, ys, m, trinum);
						   System.out.println(xs +"\t"+ ys+"\t"+m);
						   trinum = trinum+1;
				   }
		   	}
	   }
	   
	   /** creating common neighbour triangle under the choosen point */
	   // searching in vertical direction the y-axis from ys-position
	   for (int m = ys; m<= dim[0]; m++){

		   if(admatrix[xs][m]==1){
			   int lowtri = m;
			   int xnew = m;
			   System.out.println("Found contact, Searching for matching residue: " + xs + "\t"+ lowtri);
			  
			   if (admatrix[xnew][ys]==1){
				   System.out.println("Found next contact of residue: " + xnew + "\t"+ ys);
				  
				   bufferGraphics.setColor(Color.blue);
				   this.markingPoints(xs,lowtri,ratio);
				   this.markingPoints(xnew,ys,ratio);
				   
				   bufferGraphics.setColor(Color.yellow);
				   this.drawingLine(xs, ys, xs, lowtri, xnew, ys, ratio);
				   
				   bufferGraphics.setColor(Color.lightGray);
				   bufferGraphics.drawLine((int)(ys*ratio),(int)(lowtri*ratio),(int)(lowtri*ratio), (int)(lowtri*ratio));
				   bufferGraphics.drawLine((int)(lowtri*ratio),(int)(lowtri*ratio),(int)(lowtri*ratio), (int)(xs*ratio));
				
				   this.fillTriangleMatrix(xs, ys, xs, lowtri, xnew, ys, trinum);
				   this.fillResidueMatrix(xs, ys, lowtri, trinum);
				   System.out.println(xs +"\t"+ ys+"\t"+lowtri);
				   trinum = trinum+1;

			   }
		   }
	   }
	   
	   /** creating common neighbour triangle to the right of the choosen point */
	   // searching in horizontal direction the x-axis beginning at xs-position
	   for (int m = xs; m<= dim[0]; m++){

		   if(admatrix[m][ys]==1){

				   if (admatrix[m][xs]==1){
				  
				   bufferGraphics.setColor(Color.blue);
				   this.markingPoints(m, ys,ratio);
				   this.markingPoints(m,xs,ratio);
				   
				   bufferGraphics.setColor(Color.cyan);
				   this.drawingLine(xs, ys, m, ys, m, xs, ratio);
				   
				   bufferGraphics.setColor(Color.lightGray);
				   bufferGraphics.drawLine((int)(m*ratio),(int)(xs*ratio),(int)(m*ratio), (int)(m*ratio));
				   bufferGraphics.drawLine((int)(ys*ratio),(int)(m*ratio),(int)(m*ratio), (int)(m*ratio));
				
				   this.fillTriangleMatrix(xs, ys, m, ys, m,xs, trinum);
				   this.fillResidueMatrix(xs, ys, m, trinum);
				   System.out.println(xs +"\t"+ ys+"\t"+m);
				   trinum = trinum+1;

			   }
		   }
	   }

	   	
	  // 	 t2p = new Triangle2PyMol(start, this);
	   	 

	      g = this.getGraphics();
	      g.drawImage(offscreen,0,0,this);

   }
   

   public int[][] getTriangleCoordinates(){
	   return triangles;
   } 
   
   public int getTriangleNumber(){
	   return trinum;
   }
   
   public void markingPoints(int x, int y, double ratio){
	   
	   bufferGraphics.drawLine((int)(x*ratio)-3, (int)(y*ratio)-3,(int)(x*ratio)+3, (int)(y*ratio)+3 );
	   bufferGraphics.drawLine((int)(x*ratio)-3, (int)(y*ratio)+3,(int)(x*ratio)+3, (int)(y*ratio)-3 );
	   bufferGraphics.drawLine((int)(x*ratio)-2, (int)(y*ratio)-3,(int)(x*ratio)+2, (int)(y*ratio)+3 );
	   bufferGraphics.drawLine((int)(x*ratio)-2, (int)(y*ratio)+3,(int)(x*ratio)+2, (int)(y*ratio)-3 );
	    }
   
   // connecting left upper point(lu), right upper(ru) point and right lower(rl) points via drawline-method 
   public void drawingLine(int xlu, int ylu, int xru, int yru, int xrl, int yrl, double ratio){
	   
	   bufferGraphics.drawLine((int)(xlu*ratio),(int)( ylu*ratio), (int)(xru*ratio), (int)(yru*ratio));
	   bufferGraphics.drawLine((int)(xru*ratio),(int)( yru*ratio), (int)(xrl*ratio), (int)(yrl*ratio));
	   bufferGraphics.drawLine((int)(xrl*ratio),(int)( yrl*ratio), (int)(xlu*ratio), (int)(ylu*ratio));
	   
   }
   
   public void drawCorridor(int x, int y){
	   
	  bufferGraphics.setColor(Color.gray);
	  // Horizontal Lines
	  bufferGraphics.drawLine(0, y, y, y);
	  bufferGraphics.setColor(Color.green);
	  bufferGraphics.drawLine(0, x, x, x);
	  // vertical Lines
	  bufferGraphics.drawLine(y,y,y,(int)(dim[0]*ratio));
	  bufferGraphics.setColor(Color.gray);
	  bufferGraphics.drawLine(x,x,x,(int)(dim[0]*ratio)); 
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
   
   public int[][] fillTriangleMatrix(int xlu, int ylu, int xru, int yru, int xrl, int yrl,int trinum){
	   if (triangles == null)System.out.println("triangles is null");

	
	   triangles[trinum] = new int[6];
	   triangles[trinum][0]=xlu;
	   triangles[trinum][1]=ylu;
	   triangles[trinum][2]=xru;
	   triangles[trinum][3]=yru;
	   triangles[trinum][4]=xrl;
	   triangles[trinum][5]=yrl;
	   return triangles;
   }
} 

