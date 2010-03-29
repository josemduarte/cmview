package cmview;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class StatusBar extends JPanel {

	
	private static final long serialVersionUID = 1L;

	private int width = 170;

	private boolean hasSecondModel = true;

	private String jSeq = "";

	private boolean showAliAndSeqPos = false;

	private String iAli = "";

	private String jAli = "";

	private String iRes = "";

	private String jRes = "";

	private boolean hasSecondaryStructure = false;

	private String iSSType = "";

	private String jSSType = "";

	private boolean drawHyphen = false;

	private boolean isDiagSecMode = false;

	private String seqSep = "";

	private boolean writePDBResNum = false;

	private String iResNum = "";

	private String jResNum = "";

	private String iSeq = "";

	private String title = "";
	
	private JLabel deltaRankLable;
	
	public StatusBar(BoxLayout boxLayout) {
		
		
	}
	public void initDeltaRankLable() {
		this.add(Box.createRigidArea(new Dimension(150,200)));
		deltaRankLable = new JLabel();
		deltaRankLable.setBounds(5, 5, 100, 20);
		this.add(deltaRankLable,2);
	}
	
	/** Method called by this component to determine its minimum size */
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(width,160);
	}

	/** Method called by this component to determine its preferred size */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(width,getHeight());
	}

	/** Method called by this component to determine its maximum size */
	@Override
	public Dimension getMaximumSize() {
		return new Dimension(width,super.getMaximumSize().height);
	}
	
	public void setTitle(String string) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Main method to draw the component on screen. This method is called each
	 * time the component has to be (re) drawn on screen. It is called
	 * automatically by Swing or by explicitly calling cmpane.repaint().
	 */
	@Override
	protected synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g.create();
		drawCoordinates(g2d,20,getHeight()-75);
	}
	
	protected void drawCoordinates(Graphics2D g2d, int x, int y) {
			
		int extraX = 0;
		if( showAliAndSeqPos ) {
			extraX = 30;
		}
		
		int extraTitleY = 0;
		if(hasSecondModel) {
			//extraTitleY = 20;	
			g2d.drawString(title, x, y);
		}
		
		// draw background rectangle
		g2d.setColor(Color.BLACK);
		if (iSeq != "") {
			//g2d.drawLine(5, y-12, getWidth()-6, y-12);
			g2d.setColor(Color.WHITE);
			g2d.fill(new RoundRectangle2D.Float(7, y-23, getWidth()-12, 93, 12, 12));
			g2d.setColor(Color.BLUE);
		}
		
			
		// writing coordinates and optionally alignment coordinates
		g2d.drawString(iSeq, x,y+extraTitleY);
		g2d.drawString(jSeq, x+extraX+40, y+extraTitleY);
		if( hasSecondModel && showAliAndSeqPos ) {
			g2d.drawString(iAli, x+extraX,      y+extraTitleY);
			g2d.drawString(jAli, x+2*extraX+40, y+extraTitleY);
		}		 

		// writing residue types
		g2d.drawString(iRes, x,           y+extraTitleY+20);
		g2d.drawString(jRes, x+extraX+40, y+extraTitleY+20);
		
		// writing secondary structure
		if (hasSecondaryStructure){
			g2d.drawString(iSSType, x,           y+extraTitleY+40);
			g2d.drawString(jSSType, x+extraX+40, y+extraTitleY+40);
		}

		// draw hyphen if (i,j) is a contact
		if(drawHyphen) {
			g2d.drawLine(x+28, y+extraTitleY+15, x+extraX+35, y+extraTitleY+15);		
		}

		// write sequence separation in diagonal selection mode
		
		if(isDiagSecMode) {
			if(!hasSecondModel) { // we don't show seq separation in compare mode
				g2d.drawString("SeqSep", x+80, y+extraTitleY);
				g2d.drawString(seqSep, x+extraX+80, y+extraTitleY+20);		
			}
		}

		// write pdb residue numbers (if available)
		if (writePDBResNum){
			g2d.drawString(iResNum, x,           y+extraTitleY+60);
			g2d.drawString(jResNum, x+extraX+40, y+extraTitleY+60);
		}
	}

	public void setISeq(String string) {
		iSeq = string;
		
	}

	public void setJResNum(String string) {
		jResNum = string;
		
	}

	public void setIResNum(String string) {
		iResNum = string;
		
	}

	public void setWritePDBResNum(boolean b) {
		writePDBResNum = b;
		
	}

	public void setSeqSep(String string) {
		seqSep = string;
		
	}

	public void setIsDiagSecMode(boolean b) {
		isDiagSecMode = b;
		
	}

	public void setDrawHyphen(boolean b) {
		drawHyphen = b;
		
	}

	public void setJSSType(String string) {
		jSSType = string;
		
	}

	public void setISSType(String string) {
		iSSType = string;
		
	}

	public void setHasSecondaryStructure(boolean b) {
		hasSecondaryStructure = b;
		
	}

	public void setJRes(String string) {
		jRes = string;
		
	}

	public void setIRes(String string) {
		iRes = string;
	}

	public void setJAli(String string) {
		jAli = string;
		
	}

	public void setIAli(String string) {
		iAli = string;
		
	}

	public void setShowAliAndSeqPos(boolean b) {
		showAliAndSeqPos = b;
		
	}

	public void setJSeq(String string) {
		jSeq = string;
		
	}

	public void setHasSecondModel(boolean b) {
		hasSecondModel = b;
		
	}

	public void setDeltaRank(float f) {
		{
		deltaRankLable.setText("\u0394" + "rank: "+f);
		}
	}
	
}
