

import java.awt.*;
import java.awt.event.*;
import java.lang.Math.*;
import java.awt.event.MouseEvent.*;

import javax.swing.JTextField;


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


	private Model mod;
	private View view;
	private boolean changeWin;

	public Graphics g;

	public int i,j, width, height, rw, rh;
	public int rwidth, rheight; 	// distance from xs t x in x-direction (respectively ys and y)
	
	public int[] dim = new int[2];
	public int[] dimsi = new int[2];
	public int[] pos = new int[2];
	public int[] selrec = new int[4]; // points of selected Rectangle
	public int [][] admatrix = new int[0][];
	public int[][] selmatrix = new int[0][];
	public String[] multiSel = new String[1];
	public int selNum;

	

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
   
   public void myUpdate(Graphics g) {
	   myPaint(g);
   }
   
   
   /** Creating contact map and mapping the contacts */ 
   public void myPaint(Graphics g) {
	  dim = mod.getMatrixSize();
	  int[] dims = this.getMyMinimumSize();
	  
      width = dims[0];   // Width of the Contact Map
      height = dims[1];  // Height of the Contact Map.
  
      ratio = (double)width/dim[0];
      
      g= getGraphics();
      admatrix = mod.getMatrix();            
    
    
      g.setColor(Color.white);
      g.fillRect(0, 0, dims[0],dims[1]);

     g.setColor(Color.black);

      for (i= 0; i<dim[0]; i++){
          for (j= 0; j<dim[1]; j++){
        	  
        	  if (admatrix[i][j] ==1){
        		  // if there is a contact, draw a rectangle
 
        		  g.drawRect((int)(ratio*i),(int)(ratio*j),(int)(ratio*1),(int)(ratio*1));
        		  g.fillRect((int)(ratio*i),(int)(ratio*j),(int)(ratio*1),(int)(ratio*1));
        	  }
          }
      }
   }
   
   /** Allows user interaction --> selections and region growing */
   public void paint(Graphics g, MouseEvent evt){
	  // Statement fuer controldown -- mehrfachselektion geht aber noch nicht  !!!
	   
	   /** distinction between square-select or fill-select*/ 
	   int selval = view.getValue();
	   selNum= view.getSelNum();
	  // System.out.println(selval);
	   switch(selval){
	   
	   case 1: squareSelect(evt, selNum);
	   case 2: regionGrow(xs,ys);
	   }

       }
   
   
   /** SQUARE SELECTION: from left upper to right lower corner of a rectangle */
   public void squareSelect(MouseEvent evt, int selNum){
	   
	  
       g.drawRect(xs,ys,rwidth,rheight);
       
       // Marking the selected contacts red
	   g.setColor(Color.red);
	   
       for (int xsi= (int)(xs/(double)ratio); xsi <= (int)((xs +rwidth)/(double)ratio); xsi++){
           for (int ysj= (int)(ys/(double)ratio); ysj<= (int)((ys +rheight)/(double)ratio); ysj++){
         	  
         	  if (admatrix[xsi][ysj] ==1){
         		  // if there is a contact, draw a 1-by-1 rectangle
  
         		  g.drawRect((int)(ratio*xsi),(int)(ratio*ysj),(int)(ratio*1),(int)(ratio*1));
         		  g.fillRect((int)(ratio*xsi),(int)(ratio*ysj),(int)(ratio*1),(int)(ratio*1));

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
		
		//String sel = ""+xs+","+ys+","+rw+","+rh;
		//System.out.println(sel);
		//multiSel[selNum]= sel;
		//System.out.println("Sring: "+multiSel[selNum]);
		
		selected = true;
	
   }


  
   public void regionGrow(int x, int y){
	   x = x;
	   y= y;
	  
	try{
	   
	   if (selmatrix[x][y]==0){
		   return;
	   }
	   
	   if (selmatrix[x][y]==10){
		   return;
	   }
	   else {
			  //System.out.println("Hallo");
			   g.setColor(Color.red);
			   g.drawRect(x,y,1,1);
			   g.fillRect(x,y,1,1);
			   selmatrix[x][y]= 10;
			//   beVisited=true;
		
			   int xv = x;
			   int yv= y;
			   // 1 distance
			   regionGrow(xv-1,yv);
			   regionGrow(xv+1,yv);
			   regionGrow(xv,yv-1);
			   regionGrow(xv,yv+1);
			   
			   // 2 distance
			   regionGrow(xv-2,yv);
			   regionGrow(xv+2,yv);
			   regionGrow(xv,yv-2);
			   regionGrow(xv,yv+2);
			   regionGrow(xv-1,yv+1);
			   regionGrow(xv+1,yv+1);
			   regionGrow(xv-1,yv-1);
			   regionGrow(xv+1,yv-1);
		   
		   
	   }
	}catch (Exception e){
		System.out.println(e);
	}
	   
   }
   
   /** returns the coordinates of the upper left and lower right points of the rectangle */
   public int[] getSelectRect(){
	 return  selrec;
   }
   
   public int[][] getSelectMatrix(){
		 return  selmatrix;
	   }
   
   public String[] getMultiSelection(){
		 return  multiSel;
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

	}
   
  public int[] getPosition(){
	  return pos;
   }
   

   public void mouseReleased(MouseEvent evt) {
           // Called whenever the user releases the mouse button.
	   g = getGraphics();
       if (dragging == false)
          return;  // Nothing to do because the user isn't drawing.
      
       rwidth = x-xs; 	// difference between start and end point in x-direction
       rheight = y-ys;	// s.o.: respectively y
       
       rw = Math.abs(x-xs); // positive difference
       rh = Math.abs(y-ys);
       
       /** Creating the Contact Map for Selections */
       myPaint(g);
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
	   this.myUpdate(g);
	   this.drawCoordinates();
	   /*
	   if (selected==true){
		   int[][] mat = mod.getMatrix();
		   int[] srect = this.getSelectRect();
			int xs = (int)(srect[0]*ratio);
			int ys = (int)(srect[1]*ratio);
			int rw = (int)(srect[2]*ratio);
			int rh = (int)(srect[3]*ratio);
			
			g=getGraphics();
		    g.setColor(Color.red);

		      for (int m= xs; m<rw; m++){
		          for (int n= ys; n<rh; n++){
		        	  
		        	  if (admatrix[m][n] ==1){
		        		  g.drawRect(m,n,(int)(1*ratio), (int)(1*ratio));
		        		  g.fillRect(m,n,(int)(1*ratio), (int)(1*ratio));
		        	  }
			
		          }}
			
	   }*/

   } 
   
   
   public void drawCoordinates(){

	   g= getGraphics();
	   g.setColor(Color.red);
	   int[] temp = this.getPosition();
	   
	   if ((mouseIn == true) && (xpos <= winwidth) && (ypos <= winheight)){
	  // writing the coordinates at lower left corner
	  g.setColor(Color.blue);
	  g.drawString("(" + (int)(temp[0]/ratio)+"," + (int)(temp[1]/ratio)+")", 0, winheight-10);
	  // drawing the cross-hair
	  g.setColor(Color.green);
	  g.drawLine(xpos, 0, xpos, winheight);
	  g.drawLine(0, ypos, winwidth, ypos);
	   }
   }
   
 
} 

