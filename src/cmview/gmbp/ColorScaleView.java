package cmview.gmbp;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
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
	
	private int baseLineY = 0;
	private int baseLineX = 0;
	// variables for drawing text 
	private final int leftMargin = 5;			// margin between bg rectangle and edge
	private final int firstColumnX = leftMargin + 0;
//	private final int secondColumnX = leftMargin + 50;	
//	private final int thirdColumnX = leftMargin + 90;	
//	private final int fourthColumnX = leftMargin + 130;
	private final int lineHeight = 20;		// offset between lines
	private final int textYOffset = 23;		// top margin between rectangle and first text
	private Font stdFont;
	
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
		this.dimX = 1*this.pixelWidth + 2*this.border + 200;
				
		this.setSize(dimX, dimY);
		this.setPreferredSize(new Dimension(dimX, dimY));
	}
	
	private void drawLegend(Graphics2D g2d){
		String s ="Legend traces";
		int x = baseLineX + firstColumnX;			// where first text will be written
		int y = baseLineY + textYOffset;	// where first text will be written	
		g2d.drawString(s, x, y);
		y += lineHeight;
		g2d.setColor(ContactPane.helixSSTColor);
		g2d.fill(new Rectangle2D.Float(x, y-pixelHeight/2, pixelWidth/2, pixelHeight/2));
		g2d.setColor(Color.black);	g2d.drawString("helix", x + pixelWidth, y);
		y += lineHeight;
		g2d.setColor(ContactPane.sheetSSTColor);
		g2d.fill(new Rectangle2D.Float(x, y-pixelHeight/2, pixelWidth/2, pixelHeight/2));
		g2d.setColor(Color.black);	g2d.drawString("sheet", x + pixelWidth, y);
		y += lineHeight;
		g2d.setColor(ContactPane.otherSSTColor);
		g2d.fill(new Rectangle2D.Float(x, y-pixelHeight/2, pixelWidth/2, pixelHeight/2));
		g2d.setColor(Color.black);	g2d.drawString("others", x + pixelWidth, y);
		y += lineHeight;
		y += lineHeight;
		
		g2d.drawString("Label for Node-symbol:", x, y);
		y += lineHeight;
		g2d.drawString("ResType followed by", x+10, y);
		y += lineHeight;
		g2d.drawString("jNum-iNum", x+10, y);
		y += lineHeight;
		
		GeneralPath rhombus = null;
		Shape circle = null;
		float radius = 6.f;
		// create rhombus for residue types contained in nbhstring
		rhombus = this.cPane.rhombusShape(x+2*radius, y-radius, 2*radius, 2*radius);
		g2d.setColor(Color.black);
		g2d.draw(rhombus);
		g2d.fill(rhombus);
		Font f = new Font("Dialog", Font.PLAIN, 14);
		g2d.setFont(f);
		g2d.drawString("Res Û NBHS-Template", x + pixelWidth/2, y);
		y += lineHeight;
		
		radius = 3.f;
		// create ellipse for residue
		circle = new Ellipse2D.Double( x+3*radius, y-3*radius,2*radius, 2*radius);
		g2d.setColor(new Color(70,70,70));
		g2d.draw(circle);
		g2d.fill(circle);
		f = new Font("Dialog", Font.PLAIN, 12);
		g2d.setFont(f);
		g2d.drawString("Res notÛ NBHS-Template", x + pixelWidth/2, y);
		y += lineHeight;
		
		baseLineY = y;
		g2d.setFont(this.stdFont);
		g2d.setColor(Color.black);
	}
	
	private void drawTraceColouring(Graphics2D g2d){
		Color col;
		ColorScale scale = new ColorScale();
		Shape shape = null;
		// -- shortrange scaling: |jNum-iNum|>ShortRangeThreshold --> blue
		int thres1 = 9; // 1-9:short range  9-25:middle range  25-n/9-n:long range
		int thres2 = 25;
		float ratio;
		int x = baseLineX + firstColumnX;			// where first text will be written
		int y = baseLineY + textYOffset;	// where first text will be written	
		int dx = 5, dy = 12;
		
		g2d.drawString("Colouring of Traces:", x, y);
		y += lineHeight;
		x += 10;
		g2d.drawString("jNum-iNum:", x, y);
		y += lineHeight;
		Font f = new Font("Dialog", Font.PLAIN, 13);
		g2d.setFont(f);
		for (int i=-26; i<=26; i++){
			if (Math.abs(i)<=thres1){
				ratio = +1 * (float)Math.abs(i)/(float)(thres1);
				// scale on range 0.2:0.8
				ratio = 0.2f + (ratio*(0.8f-0.2f));
				col = scale.getColor4GreyValueRange(ratio, 1);
			}
			else if (Math.abs(i)<=thres2){
				if (i < 0)
					ratio = -1 * (float)(Math.abs(i)-thres1)/(float)(thres2-thres1);
				else 
					ratio = +1 * (float)(Math.abs(i)-thres1)/(float)(thres2-thres1);
				col = scale.getColor4HotColdScale(ratio, 1.0f);
			}
			else {
				if (i < 0)
					ratio = -1.0f;
				else 
					ratio = +1.0f;
				col = scale.getColor4HotColdScale(ratio, 1.0f);
			}
			g2d.setColor(col);	
			shape = new Rectangle2D.Float(x, y-dy, dx, dy);
			g2d.draw(shape);
			g2d.fill(shape);
			if (i==-26)
				g2d.drawString("<-25", (x+2*dx), y);
			else if (i==26)
				g2d.drawString(">25", (x+2*dx), y);
			else if (i%2 == 0)
				g2d.drawString(String.valueOf(i), (x+2*dx), y);
			y+=dy;
		}
		baseLineY = y;
		g2d.setFont(this.stdFont);
		g2d.setColor(Color.black);
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
		
		stdFont = g2d.getFont();
		g2d.setBackground(Color.white);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		updateParam();
		updateDimensions();
		g2d.setColor(backgroundColor);
		if (isOpaque()) {
			g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
		}
		drawSquares(g2d);
		this.baseLineY = border + yBorderThres; 
		this.baseLineX = 1*this.pixelWidth + 2*this.border;
		drawLegend(g2d);
		drawTraceColouring(g2d);
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
