package cmview.gmbp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JFrame;

public class HistogramView extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final Color selColor = Color.green; // color for selected range
	protected static final Color wholeSelColor = Color.black;	// color for whole range

	public ContactPane cPane;
	// variables for drawing methods
	private int dimX, dimY;
	private int pixelHeight = 25;
	private int pixelWidth = 2*pixelHeight;
	private int border = 25;	
	private int yBorderThres = 20;
	private int steps = 20;
	private int start = -steps; // -steps or 0
	private int chosenColourScale = ContactStatusBar.BLUERED;	
	private int maxHistLineLength = 200;
	// variables for drawing text 
	private final int leftMargin = 5;			// margin between bg rectangle and edge
	private final int firstColumnX = leftMargin + 0;
	private final int secondColumnX = leftMargin + 50;	
	private final int thirdColumnX = leftMargin + 90;	
	private final int fourthColumnX = leftMargin + 130;
//	private final int rightMargin = 12;		// margin between bg rectangle and edge
//	private final int bottomMargin = 0;		// margin between bg rectable and edge
	private final int textYOffset = 23;		// top margin between rectangle and first text
	private final int lineHeight = 20;		// offset between lines
	private int baseLineY = 0;
	private int baseLineX = 0;

	// variables for scale
	private double minRatio = 0;
	private double maxRatio = 0;
	private boolean removeOutliers = false; //true;
	private double minAllowedRat = ContactPane.defaultMinAllowedRat;
	private double maxAllowedRat = ContactPane.defaultMaxAllowedRat;
	// variables for histograms
	private double[] scale;
	private int [] histWholeAngleRange; // = new int[scale.length];
	private Vector<int[]> hist4SelAngleRange; // = new Vector<int[]>();
	private int [][] histTWholeAngleRange; // = new int[scale.length];
	private Vector<int[][]> histT4SelAngleRange; // = new Vector<int[]>();
	private Vector<double[]> minMaxAverT4SelAngleRange;
	private boolean showHist = false;
//	private int histType = ContactPane.sphoxelHist;	
	private int chosenSelection = 0;
	private String[] ptSelRange;
	
	private final Color backgroundColor = Color.white;
	
	public HistogramView(ContactPane cPane, String title){
		super(title);
		this.cPane = cPane;
				
		this.scale = new double[this.steps-this.start+1];
		this.hist4SelAngleRange = null;
		this.histWholeAngleRange = null;
		
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
		this.dimY = (this.steps-this.start+1)*this.pixelHeight + 2*this.border + yBorderThres + 2*lineHeight;
		this.dimX = 1*this.pixelWidth + 2*this.border;
		if (showHist)
			this.dimX += (2*this.maxHistLineLength + 2*this.border);
		
		this.setSize(dimX, dimY);
		this.setPreferredSize(new Dimension(dimX, dimY));
	}
	
	private void drawHist(Graphics2D g2d){
//		if (this.histType==ContactPane.sphoxelHist)
			this.baseLineY = 0; this.baseLineX = 0;
			drawHistSphoxel(g2d);
//		else if (this.histType==ContactPane.tracesHist)
			this.baseLineY = 0; this.baseLineX = 1*pixelWidth + this.maxHistLineLength + (2*border);
			drawHistTraces(g2d);	
			drawKeys(g2d);
	}
	
	private void drawKeys(Graphics2D g2d){
		String s ="Keys histogram sphoxel density";
		int x = baseLineX + firstColumnX;			// where first text will be written
		int y = baseLineY;	// where first text will be written		
		g2d.setColor(Color.black);
		g2d.drawString(s, x, y);
		y += lineHeight;	
		g2d.setColor(selColor);
		g2d.fill(new Rectangle2D.Float(x, y-pixelHeight/2, pixelWidth/2, pixelHeight/2));
		g2d.setColor(Color.black);	g2d.drawString("selected range", x + pixelWidth, y);
		y += lineHeight;	
		g2d.setColor(wholeSelColor);
		g2d.fill(new Rectangle2D.Float(x, y-pixelHeight/2, pixelWidth/2, pixelHeight/2));
		g2d.setColor(Color.black);	g2d.drawString("whole sphoxel", x + pixelWidth, y);
		y += lineHeight;			
		s ="Keys histogram traces";
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
		g2d.setColor(ContactPane.anySSTColor);
		g2d.fill(new Rectangle2D.Float(x, y-pixelHeight/2, pixelWidth/2, pixelHeight/2));
		g2d.setColor(Color.black);	g2d.drawString("any (sum)", x + pixelWidth, y);
		y += lineHeight;
		baseLineY = y;
		
	}
	
	private void drawSquares(Graphics2D g2d){
		Shape shape = null;
		float xPos, yPos;
		Color color = Color.white;
		ColorScale scale = new ColorScale();
		double ratio;
		double deltaRNeg, deltaRPos;		
		float ratio1, ratio2;
				
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
			yPos = (i-this.start)*pixelHeight + baseLineY; // +border +yBorderThres;
			
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
	
	private void drawHistTraces(Graphics2D g2d){
		int maxHistVal = 0;
		int maxHistValSS = 0;
		int maxSelHistVal = 0;
		double xS, xE, yS, yE; 
//		double xPos, yPos;
		double dY = 5;
		Shape line; //, rect;
		double deltaRes = (15*pixelHeight)/(5*dY*histT4SelAngleRange.size()); // = pixelHeight
		if (deltaRes<pixelHeight)
			deltaRes=pixelHeight;
		
//		this.maxHistLineLength = this.maxHistLineLength/2;

		// calculate max #counts
		for (int i=0; i<this.histTWholeAngleRange[0].length; i++){
			if (this.histTWholeAngleRange[this.cPane.sstypes.length-1][i]>maxHistVal)
				maxHistVal = this.histTWholeAngleRange[this.cPane.sstypes.length-1][i];
			for (int j=0; j<this.histTWholeAngleRange.length-1; j++){
				if (this.histTWholeAngleRange[j][i]>maxHistValSS)
					maxHistValSS = this.histTWholeAngleRange[j][i];
			}
		}
		Iterator<int[][]> itrHist = this.histT4SelAngleRange.iterator();
		while (itrHist.hasNext()){
			int[][] hist = itrHist.next();
			for (int i=0; i<hist[0].length; i++){
				for (int j=0; j<hist.length; j++){
					if (hist[j][i]>maxSelHistVal)
						maxSelHistVal = hist[j][i];
				}
			}
		}
		// draw title
		String title = "Histogram for traces";		
		int x = baseLineX + firstColumnX;			// where first text will be written
		int y = baseLineY + textYOffset + yBorderThres;	// where first text will be written		
		g2d.setColor(Color.black);
		g2d.drawString(title, x, y);			
		y += this.lineHeight; y += this.lineHeight;
		baseLineY = y;
		// draw surrounding box
		xS = 1*pixelWidth + this.maxHistLineLength + (2*border);
		xE = xS+(maxHistLineLength+20);
		yS = 0*deltaRes +baseLineY; //+border+yBorderThres;
		yE = yS+(20*deltaRes);
		g2d.setColor(Color.gray);
		line = new Line2D.Double(xS, yS, xE, yS);
		g2d.draw(line);
		line = new Line2D.Double(xS, yS, xS, yE);
		g2d.draw(line);
		line = new Line2D.Double(xE, yS, xE, yE);
		g2d.draw(line);		
		// draw 100% marker
//		xPos = 1*pixelWidth + (2*this.maxHistLineLength) + (2*border) + 20;
//		yPos = border+yBorderThres-5;
		line = new Line2D.Double(xE, yS, xE, yS-5);
		g2d.setColor(Color.gray);
		g2d.draw(line);
		g2d.drawString(String.valueOf(maxSelHistVal), (float)xE-5, (float)yS-5); //maxHistVal  or  "100%"
		
		drawHistLinesTraces(g2d, deltaRes, maxSelHistVal, dY);
		drawMinMaxAverOfTraces(g2d, (int)xS, (int)yE);
	}
	
	private void drawMinMaxAverOfTraces(Graphics2D g2d, int xStart, int yStart){
		baseLineY = yStart;
		baseLineX = xStart;
		Iterator<double[]> itr;
//		String title = "Min-Max-Average Values for Theta and Lampda";
		String val2S = "";
		
		int x = baseLineX + firstColumnX;			// where first text will be written
		int y = baseLineY + textYOffset;	// where first text will be written
		
//		g2d.drawString(title, x, y);			
//		y += this.lineHeight;
		y += this.lineHeight/2;
		baseLineY = y;
		// --- draw min-max-average strings
		y = baseLineY;
		x = baseLineX + secondColumnX;
		g2d.drawString("Min", x, y);
		y = baseLineY;
		x = baseLineX + thirdColumnX;
		g2d.drawString("Max", x, y);	
		y = baseLineY;
		x = baseLineX + fourthColumnX;
		g2d.drawString("Aver", x, y);			
		y += this.lineHeight;
		baseLineY = y;	
		x = baseLineX + firstColumnX;	
		g2d.drawString("Lampda", x, y);			
		y += this.lineHeight;
		g2d.drawString("Theta", x, y);			
		y += this.lineHeight;		
//		x = baseLineX + secondColumnX;
//		g2d.drawString("minLampda", x, y);			
//		y += this.lineHeight;
//		g2d.drawString("minTheta", x, y);			
//		y += this.lineHeight;
//		g2d.drawString("maxLampda", x, y);			
//		y += this.lineHeight;
//		g2d.drawString("maxTheta", x, y);			
//		y += this.lineHeight;
//		g2d.drawString("averLampda", x, y);			
//		y += this.lineHeight;
//		g2d.drawString("averTheta", x, y);
		
		y = baseLineY;
		itr = this.minMaxAverT4SelAngleRange.iterator();
		int cnt = 0;
		while (itr.hasNext()){
			if (cnt%2==0)
				y = baseLineY;
			if (cnt<2)
				x = baseLineX + secondColumnX;
			else if (cnt<4)
				x = baseLineX + thirdColumnX;
			else 
				x = baseLineX + fourthColumnX;				
//			x = baseLineX + thirdColumnX;	
//			x = baseLineX + secondColumnX;	
			double[] values = itr.next();
//			for (int i=0; i<values.length; i++){
//				double val = (double)(Math.round(values[i]*100))/100;
//				val2S = String.valueOf(val);
//				g2d.drawString(val2S, x, y);
//				x += 35;
//			}
			double val = (double)(Math.round(values[this.chosenSelection]*100))/100;
			val2S = String.valueOf(val);
			g2d.drawString(val2S, x, y);
			
			y += this.lineHeight;
			cnt++;
		}
		
//		g2d.drawString(iRes+"_"+iSSType.toLowerCase()+" - "+jRes+"_"+jSSType.toLowerCase(), x, y);	// selected contacts within contact map
		y += this.lineHeight;
		baseLineY = y;
	}
	
	private void drawHistLinesTraces(Graphics2D g2d, double deltaRes, int maxSelHistVal, double dY){
		double xPos, yPos;
		double xS, xE, yS;
		Shape line;
		double lineLength = 0;
		Iterator<int[][]> itrHist;
		// draw Hist for all residues		
		for (int i=0; i<this.cPane.aas.length; i++){
			xPos = 1*pixelWidth + this.maxHistLineLength + (2*border);
			yPos = i*deltaRes + baseLineY; //+border+yBorderThres;
			// draw residue letter
			yPos = yPos+(2*deltaRes/3);
			String residue = String.valueOf(this.cPane.aas[i]);
			g2d.setColor(Color.black);
			g2d.drawString(residue, (float)xPos+5, (float)yPos);

			yPos = yPos-(2*deltaRes/3);
//			rect = new Rectangle2D.Double(xPos, yPos, deltaRes, deltaRes);
			line = new Line2D.Double(xPos, yPos+deltaRes, xPos+(maxHistLineLength+20), yPos+deltaRes);
			g2d.setColor(Color.gray);
//			g2d.draw(rect);
			g2d.draw(line);
			
			yS = yPos; //-(2*pixelHeight/3);
			itrHist = this.histT4SelAngleRange.iterator();
			int[][] hist = null;
			int count = 0;
			while (itrHist.hasNext() && count<=this.chosenSelection){
				hist = itrHist.next();
				count++;
			}
			for (int j=0; j<this.cPane.sstypes.length; j++){
				lineLength = hist[j][i]*this.maxHistLineLength/maxSelHistVal;
				xS = xPos+20;
				xE = xS + lineLength;
				yS += dY;		
				switch (j){
				case 0: // 'H'
					g2d.setColor(ContactPane.helixSSTColor); break;
				case 1: // 'S'
					g2d.setColor(ContactPane.sheetSSTColor); break;
				case 2: // 'O'
					g2d.setColor(ContactPane.otherSSTColor); break;
				case 3: // 'A'
					g2d.setColor(ContactPane.anySSTColor); break;
				}
				line = new Line2D.Double(xS, yS, xE, yS);
				g2d.draw(line);		
				line = new Line2D.Double(xS, yS+1, xE, yS+1);
				g2d.draw(line);
				line = new Line2D.Double(xS, yS+2, xE, yS+2);
				g2d.draw(line);	
			}
		}
	}
	
	private void drawHistSphoxel(Graphics2D g2d){
		int maxHistVal = 0;
		int maxSelHistVal = 0;
		double lineLength = 0;
		int index = 0;
		double xS, xE, yS; //yE=yS
//		double dY = 10;
		Shape line;
		// calculate max #counts
		for(int i=0; i<this.histWholeAngleRange.length; i++){
			if (this.histWholeAngleRange[i]>maxHistVal)
				maxHistVal = this.histWholeAngleRange[i];
		}
		Iterator<int[]> itrHist = this.hist4SelAngleRange.iterator();
		while(itrHist.hasNext()){
			int[] counts = itrHist.next();
			for(int i=0; i<counts.length; i++){
				if (counts[i]>maxSelHistVal)
					maxSelHistVal = counts[i];
			}
		}
		// draw title
		String title = "Histogram for sphoxel densities";		
		int x = baseLineX + firstColumnX + border;			// where first text will be written
		int y = baseLineY + textYOffset + yBorderThres;	// where first text will be written		
		g2d.setColor(Color.black);
		g2d.drawString(title, x, y);			
		y += this.lineHeight;
//		double p1 = ptSelRange[0], p2 = ptSelRange[1]; 
//		double t1 = ptSelRange[2], t2 = ptSelRange[3];
//		String values = "lampda["+p1+":"+p2+"]"+"  theta["+t1+":"+t2+"]";
		String values = "lampda["+ptSelRange[0]+":"+ptSelRange[1]+"]"+"  theta["+ptSelRange[2]+":"+ptSelRange[3]+"]";
		g2d.drawString(values, x, y);
		y += this.lineHeight;	
		baseLineY = y;
		// draw squares
		drawSquares(g2d);
		// draw histogramm for scale
		for(int i=this.start; i<this.steps; i++){
			index = i-this.start;
			lineLength = this.histWholeAngleRange[index]*this.maxHistLineLength/maxHistVal;
			xS = border+pixelWidth+(5);
			xE = xS+lineLength;
			yS = index*pixelHeight + (pixelHeight/5); // +border
			yS += baseLineY;
			g2d.setColor(wholeSelColor);
			line = new Line2D.Double(xS, yS, xE, yS);
			g2d.draw(line);
			line = new Line2D.Double(xS, yS+1, xE, yS+1);
			g2d.draw(line);
			line = new Line2D.Double(xS, yS+2, xE, yS+2);
			g2d.draw(line);
			
			g2d.setColor(selColor);
			itrHist = this.hist4SelAngleRange.iterator();
			int[] counts = null;
			int cnt = 0;
			while(itrHist.hasNext() && cnt<=this.chosenSelection){
				counts = itrHist.next();
				cnt++;
			}
			lineLength = counts[index]*this.maxHistLineLength/maxSelHistVal; //maxHistVal;
			xE = xS+lineLength;
			yS += (pixelHeight/3);
//				yS += dY;
			line = new Line2D.Double(xS, yS, xE, yS);
			g2d.draw(line);
			line = new Line2D.Double(xS, yS+1, xE, yS+1);
			g2d.draw(line);
			line = new Line2D.Double(xS, yS+2, xE, yS+2);
			g2d.draw(line);
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
//		if (this.showHist)
			drawHist(g2d);
	}
	
	public void updateParam(){
		this.minAllowedRat = this.cPane.getMinAllowedRat();
		this.maxAllowedRat = this.cPane.getMaxAllowedRat();
		this.minRatio = this.cPane.getMinRatio();
		this.maxRatio = this.cPane.getMaxRatio();
		this.removeOutliers = this.cPane.isRemoveOutliers();
		this.showHist = this.cPane.isShowHist();
//		this.histType = this.cPane.getHistType();
		this.histWholeAngleRange = this.cPane.getHistWholeAngleRange();
		this.hist4SelAngleRange = this.cPane.getHist4SelAngleRange();
		this.histTWholeAngleRange = this.cPane.getHistTWholeAngleRange();
		this.histT4SelAngleRange = this.cPane.getHistT4SelAngleRange();
		this.minMaxAverT4SelAngleRange = this.cPane.getMinMaxAverT4SelAngleRange();
		if (this.hist4SelAngleRange == null || this.hist4SelAngleRange.size() == 0){
			this.showHist = false;
		}
		this.chosenSelection = this.cPane.getChosenSelection();
		this.ptSelRange = this.cPane.getChosenPTRange();
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

	public int[] getHistWholeAngleRange() {
		return histWholeAngleRange;
	}

	public void setHistWholeAngleRange(int[] histWholeAngleRange) {
		this.histWholeAngleRange = histWholeAngleRange;
	}

	public Vector<int[]> getHist4SelAngleRange() {
		return hist4SelAngleRange;
	}

	public void setHist4SelAngleRange(Vector<int[]> hist4SelAngleRange) {
		this.hist4SelAngleRange = hist4SelAngleRange;
	}

	public boolean isShowHist() {
		return showHist;
	}

	public void setShowHist(boolean showHist) {
		this.showHist = showHist;
	}

}
