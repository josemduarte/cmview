

import java.awt.*;
import java.awt.event.*;
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

 	private boolean dragging;      // This is set to true while the user is drawing.

 	private boolean mouseIn;
 	private boolean selected;
 	private boolean pressed;

	private Model mod;
	private View view;
	private boolean changeWin;

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
	public int [][] admatrix = new int[0][];
	public int[][] selmatrix = new int[0][];
	public int selNum;
	
	// all actions done, i.e. selections,... , refer to the buffer

	

	public PaintController(Model mod, View view){
		this.mod = mod;

		mod.ModelInit();
		this.view=view;
	    addMouseListener(this);
	    addMouseMotionListener(this);

	}
	
	
	  public Dimension getMinimumSize() {
	  return new Dimension(width, height);
	  }
	/** Window Size */
	  public int[] getMyMinimumSize() {
		 dim = mod.getMatrixSize();
		 
		 winwidth = getWidth();
		 winheight = getHeight();
		 
		 if (winwidth>=dim[0] && winheight >=dim[1]){
			 // windowsize has chenged
			 changeWin =true;
		 }
		 else {
			 winwidth = dim[0];
			 winheight = dim[1];
		 }
		 if (changeWin==true){

			 // building a squared rectangle
			 if (winwidth<= winheight){
				 winheight=winwidth;
			 }
			 else {
				 winwidth = winheight;
			 }
			  
		 }
		
		 
		 int[]dimsi= {winwidth, winheight};
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
		    
		  offscreen = createImage(winwidth, winheight);
		  if (offscreen == null) System.out.println("screenimage null");
		  
		  bufferGraphics = offscreen.getGraphics(); 
	      bufferGraphics.clearRect(0,0,(int)(dims[0]*ratio),(int)( dims[1]*ratio));
	      admatrix = mod.getMatrix();            


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
	   selmatrix = admatrix;
	   switch(selval){
	   
	   case 1: squareSelect(evt);
	   //case 2: regionGrow((int)(xs/ratio),(int)(ys/ratio));
	   
       }
	   /*
	    bufferGraphics.setColor(Color.red);
		for(int z = 0; z<= (int)(dim[0]); z++){
			for(int p = 0; p<= (int)(dim[0]); p++){
				if ((selmatrix[z][p]==10)){
	         		bufferGraphics.drawRect((int)(ratio*z),(int)(ratio*p),(int)(ratio*1),(int)(ratio*1));
	         		bufferGraphics.fillRect((int)(ratio*z),(int)(ratio*p),(int)(ratio*1),(int)(ratio*1));

				}
				
			}
			
		}*/
	   
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
         	
         		bufferGraphics.drawRect((int)(ratio*xsi),(int)(ratio*ysj),(int)(ratio*1),(int)(ratio*1));
         		bufferGraphics.fillRect((int)(ratio*xsi),(int)(ratio*ysj),(int)(ratio*1),(int)(ratio*1));
         		//selmatrix[xsi][ysj]=10;
         	  }
           }
       }
       
       //ABSPEICHERN IN M*N AARAY??????? 
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
	   
/*
	   if ((x==0) | (y==0)){
		   System.out.println("x und y sind null");
	   }
	   while((x<=dim[0]) && (y<=dim[0])){
	   if (selmatrix[x][y]==0){
		   System.out.println("matrix ist null");
		   return;
	   }
	   
	   if (selmatrix[x][y]==10){
		   System.out.println("matrix ist 10");
		   return;
	   }
	   else {
		   	   selmatrix[x][y]=10;	
		   	  System.out.println("matrix ist EINS");
			   g.setColor(Color.red);
			   g.drawRect((int)(x*ratio),(int)(y*ratio),(int)(1*ratio),(int)(1*ratio));
			   g.fillRect((int)(x*ratio),(int)(y*ratio),(int)(1*ratio),(int)(1*ratio));

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
	   }*/
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
	   
	   //System.out.println(pos[0] + "\t"+pos[1]);
	   dragging = true;
	   pressed = true;
	   
	   
	   int sel = view.getValue();
	   
	   if (sel ==3){
	   this.commonNeighbours(evt);
	   }

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

	   System.out.println("Show: "+ xs+"\t"+ys);
	   int radius =10;
	   xs = (int)(xs/ratio);
	   ys = (int)(ys/ratio);
	   System.out.println("Show: "+ xs+"\t"+ys);
	   // searching in vertical direction
	   bufferGraphics.setColor(Color.blue);
	   bufferGraphics.drawOval((int)((xs)*ratio)-10, (int)((ys)*ratio)-10, radius,radius);
	   
	   for (int m = 0; m<= dim[0]; m++){
		   System.out.println("Searching for contacts: "+ xs + "\t"+ys);
		   //searching the whole y-axis on x-position

		   if(admatrix[xs][m]==1){
			   int lowtri = m;
			   int xnew = m;
			   System.out.println("Found contact, Searching for matching residue: " + xs + "\t"+ lowtri);
			  
			   if (admatrix[xnew][ys]==1){
				   System.out.println("Found next contact of residue: " + xnew + "\t"+ ys);
				   
				   bufferGraphics.setColor(Color.blue);
				   bufferGraphics.drawOval((int)((xs)*ratio)-10, (int)((lowtri)*ratio)-10, radius,radius);
				   bufferGraphics.setColor(Color.blue);
				   bufferGraphics.drawOval((int)((xnew)*ratio)-10, (int)((ys)*ratio)-10, radius,radius);
				   
				   bufferGraphics.setColor(Color.red);
				   bufferGraphics.drawLine((int)(xs*ratio),(int)( ys*ratio), (int)(xs*ratio), (int)(lowtri*ratio));
				   bufferGraphics.drawLine((int)(xs*ratio),(int)( lowtri*ratio), (int)(xnew*ratio), (int)(ys*ratio));
				   bufferGraphics.drawLine((int)(xnew*ratio),(int)( ys*ratio), (int)(xs*ratio), (int)(ys*ratio));
				   
				   radius = radius + 5;
				   
				   bufferGraphics.setColor(Color.gray);
				   bufferGraphics.drawLine(0,(int)(lowtri*ratio),(int)(lowtri*ratio), (int)(lowtri*ratio));

			   }
			   
		   }
  
	   }
	      g = this.getGraphics();
	      g.drawImage(offscreen,0,0,this);

   }
   
 
} 

