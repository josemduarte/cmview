package cmview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

import owl.core.structure.graphs.RIGEdge;
import cmview.datasources.Model;

/**
 * A panel showing a histogram of contact weights
 * @author stehr
 *
 */
public class HistogramPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	// constants/settings
	int numBins = 5;		// number of bins to partition the interval [0,1]
	int[] histogram;		// raw counts of values in bins
	int numValues;			// number of all values (to normalize histogram)
	double step = 1.0 / numBins; // width of bins in value space [0,1]
	
	int width = 150;		// histogram width in pixels
	int height = 80;		// histogram height in pixels
	int binPadding = 4;		// space between two bins
	double binWidth = (width - (numBins) * binPadding) / numBins;	// width of bar on screen
	
	public HistogramPanel() {
		initHistogram();
	}
	
	/**
	 * Initializes an empty histogram
	 */
	private void initHistogram() {
		histogram = new int[numBins];
		numValues = 0;
	}
	
	/**
	 * Calculates the histogram for the graph in the given model. The results are stored in the class
	 * and being drawn when this component is visible. 
	 * @param mod the model containing the graph for which the histogram will be calculated
	 */
	public void calculateHistogram(Model mod) {
		initHistogram();	// delete old values
		for(RIGEdge c:mod.getGraph().getEdges()) {
			double w = c.getWeight();
			//if(w == 0) {histogram[0]++; numValues++;}
			if(w == 1) {histogram[numBins-1]++; numValues++;}
			else for(int i=0; i < numBins; i++) {
				double left = step * i;
				double right = step * (i+1);
				if(w > left && w <= right) {
					histogram[i]++;
					numValues++;
					break;
				}
			}
		}
//		System.out.println("Histogram:");
//		for(int i = 0; i < numBins; i++) {
//			System.out.printf("%d : %d\n", i+1, histogram[i]);
//		}
	}
	
	/** Method called by this component to determine its minimum size */
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(width,height);
	}

	/** Method called by this component to determine its preferred size */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(width,height);
	}

	/** Method called by this component to determine its maximum size */
	@Override
	public Dimension getMaximumSize() {
		return new Dimension(width,height);
	}
	
	/**
	 * Main method to draw the component on screen. This method is called each
	 * time the component has to be (re) drawn on screen. It is called
	 * automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	@Override
	protected synchronized void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();
		drawHistogram(g2d);
	}
	
	/**
	 * draws the current histogram on screen
	 * @param g2d
	 */
	protected void drawHistogram(Graphics2D g2d) {
		g2d.setColor(new Color(0.7f,0.7f,0.7f));
		g2d.drawRect(0, 0, width-1, height-1);
		g2d.setColor(new Color(0.5f,0.7f,0.9f));
		for(int i = 0; i < numBins; i++) {
			double binHeight = numValues==0?0.0:(1.0 * height * histogram[i] / numValues);
			double x = i*(binWidth+binPadding) + binPadding / 2;
			double y = height-binHeight+1;
			g2d.fill(new Rectangle2D.Double(x, y, binWidth, binHeight-2));
		}
	}
}
