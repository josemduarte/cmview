package cmview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.HashMap;

import javax.swing.JPanel;

public class DeltaRankBar extends JPanel{

	private static final long serialVersionUID = 1L;
	private int height = 160;
	private int leftMargin = 10;
	//private int rightMargin = 170;
	private boolean isActive = false;
	private HashMap<Character, Color> background;
	private String[] vectors;
	private ContactMapPane contactMapPane;
	private String sequence;
	private double[][] probabilities;
	private String xVector = "LAGVEDSKTIRPNFQYHMWC";
	
	public DeltaRankBar() {
		this.setBackground(Color.WHITE);
		background = new HashMap<Character, Color>();
		for (int i = 0; i<20; i++) {
			background.put(xVector.charAt(i),colorMapScaledHeatmap((double)i/20,0.5));
		}
	}
	
	public void setCMPane(ContactMapPane cmp) {
		contactMapPane = cmp;
	}
	
	public void setSequence(String seq) {
		sequence = seq;
	}
	
	/** Method called by this component to determine its minimum size */
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(super.getMinimumSize().width,height);
	}

	/** Method called by this component to determine its preferred size */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(super.getPreferredSize().width,height);
	}

	/** Method called by this component to determine its maximum size */
	@Override
	public Dimension getMaximumSize() {
		return new Dimension(super.getMaximumSize().width,height);
	}

	/**
	 * Main method to draw the component on screen. This method is called each
	 * time the component has to be (re) drawn on screen. It is called
	 * automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	@Override
	protected synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (!isActive) {
			return;
		}
		double ratio = contactMapPane.getRatio();
		int cWidth = (int)ratio;
		int cHeight;
		for (int i = 0; i < vectors.length; i++) {
			int yPos= 0;
			for (int j = 0; j < 20; j++) {
				if (vectors[i].length() == 20) {
					cHeight = (int)Math.round((height*probabilities[i][xVector.indexOf(vectors[i].charAt(j))]));
					g.setColor(background.get(vectors[i].charAt(j)));
				} else {
					cHeight = height/20;
					g.setColor(Color.gray);
				}
				
				g.fillRect((int)(i*ratio)+leftMargin, yPos, cWidth+1, cHeight+1);

				if (vectors[i].length() == 20 && vectors[i].charAt(j) == sequence.charAt(i)) {
					g.setColor(Color.WHITE);
					g.drawRect((int)(i*ratio)+leftMargin, yPos, cWidth-1, cHeight-1);
				}
				yPos += cHeight;
			}
		}
	}
	
	public void setActive(boolean active) {
		isActive = active;
	}
	
	public void setVectors(String[] vec) {
		vectors = vec;
	}
	
	public void setProbabilities(double[][] probabilities) {
		this.probabilities = probabilities;
	}
	
	
	/**
	 * Given a number between zero and one, returns a color from a scaled heatmap-style colormap.
	 * The map is scales such that values around 'middle' are green, higher values are darker
	 * shades of red and lower values are darker shades of blue.
	 * @param val the value for which a color is returned
	 * @param middle the value around which colors are green
	 * @return the Color for the given value
	 */
	private Color colorMapScaledHeatmap(double val, double middle) {
		if(val <= middle) {
			val = val * 0.5/middle;
		} else {
			val = 0.5 + (val-middle) * 0.5 / (1-middle);
		}
		// matlab-style color map
		double bc = 6/8f;
		double gc = 4/8f;
		double rc = 2/8f;
		double r = Math.max(0,Math.min(1,1.5-4*Math.abs(val-rc)));
		double g = Math.max(0,Math.min(1,1.5-4*Math.abs(val-gc)));
		double b = Math.max(0,Math.min(1,1.5-4*Math.abs(val-bc)));
		return new Color((float) r,(float) g, (float) b);
	}
}

