package cmview.gmbp;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;

//import javax.swing.JFrame;

public class ColorScaleView extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ContactPane cPane;
	
	private int dimX, dimY;
	private int pixelHeight = 25;
	private int pixelWidth = 2*pixelHeight;
	private int border = 25;	
	private int yBorderThres = 20;
	private int steps = 20;
	private int start = -steps; // -steps or 0
	private int chosenColourScale = ContactStatusBar.BLUERED;	

	private double minRatio = 0;
	private double maxRatio = 0;
	private boolean removeOutliers = false; //true;
	private double minAllowedRat = ContactPane.defaultMinAllowedRat;
	private double maxAllowedRat = ContactPane.defaultMaxAllowedRat;
	
	private double[] scale;
	
	private final Color backgroundColor = Color.white;
	
	public ColorScaleView(ContactPane cPane){
		
		this.cPane = cPane;
				
		this.scale = new double[this.steps-this.start+1];
		
		updateDimensions();
		this.pack();
		this.setVisible(true);
		
		// Setting the main layout 
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setLocation(20,20);
		
		final JFrame parent = this;					// need a final to refer to in the thread below
		EventQueue.invokeLater(new Runnable() {		// execute after other events have been processed
			public void run() {
				parent.toFront();					// bring new window to front
			}
		});
	}
	
	private void updateDimensions(){
//		this.dimX = this.steps*this.pixelWidth + 3*this.border;
		this.dimY = (this.steps-this.start+1)*this.pixelHeight + 2*this.border + yBorderThres;
		this.dimX = 1*this.pixelWidth + 2*this.border;
				
		this.setSize(dimX, dimY);
		this.setPreferredSize(new Dimension(dimX, dimY));
	}
	
	private void drawSquares(Graphics2D g2d){
		Shape shape = null;
		float xPos, yPos;
		Color color = Color.white;
		ColorScale scale = new ColorScale();
		double ratio;
		double deltaRNeg, deltaRPos;		
		float ratio1, ratio2;
//		System.out.println("1/steps: "+((float)1.0f/steps));
		
//		g2d.setColor(Color.white);
//		shape = new Rectangle2D.Float(0, 0, this.dimX+border , this.dimY);
//		g2d.draw(shape);
//		g2d.fill(shape);
				
		if (this.removeOutliers){
			deltaRNeg = this.minAllowedRat/this.steps;
			deltaRPos = this.maxAllowedRat/this.steps;
		}
		else{
			deltaRNeg = this.minRatio/this.steps;
			deltaRPos = this.maxRatio/this.steps;
		}
		for(int i=this.start; i<=this.steps; i++){
			xPos = 0*pixelWidth+border;
			yPos = (i-this.start)*pixelHeight +border+yBorderThres;
			
			shape = new Rectangle2D.Float(xPos, yPos, pixelWidth, pixelHeight);
			ratio1 = (float)i/this.steps;
			ratio2 = Math.abs(ratio1);
			// ----- compute alpha and set color
			switch (this.chosenColourScale){
			case ContactStatusBar.BLUERED:
				color = scale.getColor4BlueRedScale(ratio1,ratio2); break;
			case ContactStatusBar.HOTCOLD:
				color = scale.getColor4HotColdScale(ratio1,ratio2); break;
			case ContactStatusBar.RGB:
				color = scale.getColor4RGBscalePolar((float)ratio1,ratio2, -1); break;
			}							
			g2d.setColor(color);	
			g2d.fill(shape);			
			g2d.setColor(Color.black);	
			g2d.draw(shape);
			
			g2d.setColor(Color.black);
			xPos = xPos+pixelWidth+(pixelWidth/5);
			xPos = 0*pixelWidth+(pixelWidth/5)+border;
			yPos = yPos+(2*pixelHeight/3);
			if (i<0)
				ratio = -i*deltaRNeg;
			else 
				ratio = i*deltaRPos;
			this.scale[i-this.start] = ratio;
			ratio = (Math.round(ratio*100));
			ratio = ratio/100;
			String ratioS = String.valueOf(ratio);
			if (ratio>=0)
				ratioS = "+"+ratioS;
			g2d.drawString(ratioS, xPos, yPos);
			
		}
				
	}
	
	private void updateScaleValues(){
		double ratio;
		double deltaRNeg, deltaRPos;
		
		updateParam();
		if (this.removeOutliers){
			deltaRNeg = this.minAllowedRat/this.steps;
			deltaRPos = this.maxAllowedRat/this.steps;
		}
		else{
			deltaRNeg = this.minRatio/this.steps;
			deltaRPos = this.maxRatio/this.steps;
		}
		for(int i=this.start; i<=this.steps; i++){			
			if (i<0)
				ratio = -i*deltaRNeg;
			else 
				ratio = i*deltaRPos;
			this.scale[i-this.start] = ratio;
			ratio = (Math.round(ratio*100));
			ratio = ratio/100;			
		}
	}
	
	public void paint(Graphics g) {	
//		System.out.println("paint");
		Graphics2D g2d = (Graphics2D)g;
		
		g2d.setBackground(Color.white);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		updateParam();
		updateDimensions();
		g2d.setColor(backgroundColor);
		if (isOpaque()) {
			g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
		}
		drawSquares(g2d);
	}
	
	public void updateParam(){
		this.minAllowedRat = this.cPane.getMinAllowedRat();
		this.maxAllowedRat = this.cPane.getMaxAllowedRat();
		this.minRatio = this.cPane.getMinRatio();
		this.maxRatio = this.cPane.getMaxRatio();
		this.removeOutliers = this.cPane.isRemoveOutliers();
	}
	
	public double[] getScaleValues(){
		this.updateScaleValues();
		return this.scale;
	}
	
	public int getChosenColourScale() {
		return chosenColourScale;
	}

	public void setChosenColourScale(int chosenColourScale) {
		this.chosenColourScale = chosenColourScale;
		// show actual colour scale
	}

	public void setMinRatio(double minRatio) {
		this.minRatio = minRatio;
	}

	public double getMinRatio() {
		return minRatio;
	}

	public void setMaxRatio(double maxRatio) {
		this.maxRatio = maxRatio;
	}

	public double getMaxRatio() {
		return maxRatio;
	}

	public void setRemoveOutliers(boolean removeOutliers) {
		this.removeOutliers = removeOutliers;
	}

	public boolean isRemoveOutliers() {
		return removeOutliers;
	}

	public void setMinAllowedRat(double minAllowedRat) {
		this.minAllowedRat = minAllowedRat;
	}

	public double getMinAllowedRat() {
		return minAllowedRat;
	}

	public void setMaxAllowedRat(double maxAllowedRat) {
		this.maxAllowedRat = maxAllowedRat;
	}

	public double getMaxAllowedRat() {
		return maxAllowedRat;
	}

}
