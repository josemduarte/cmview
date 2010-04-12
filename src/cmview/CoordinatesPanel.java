package cmview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

/**
 * This panel displays the sequence coordinates of the current mouse position if
 * the mouse is over the contact map. It is currently a subcomponent of StatusBar.  
 * 
 * The display of sequent coordinates depends on the mode and some flags which can be set
 * by the setter methods. The modes are:
 * - single contact map : show i and j sequence coordinates of current mouse position
 * - single contact map, diagonal selection mode : show additionally the sequence separation 
 * - compare mode : show i and j coordinates of both contact maps side-by-side (plus titles)
 * - residue ruler mode : if mouse is over residue ruler, indicated by iNum="" or jNum="", show only i or j coordinate
 * Additionally there are some flags to toggle extra information:
 * - hasSecondaryStructure : display secondary structure type of current i and j
 * - writePDBResNum : in addition to canonic sequence coordinates show PDB residue numbers below
 * - showAliAndSeqPos : in addition to sequence coordinates show alingment coordinates (only in compare mode)
 * @author stehr
 *
 */
public class CoordinatesPanel extends JPanel {

	/*------------------------- member variables ------------------------*/
	
	private static final long serialVersionUID = 1L;
	
	// constants / settings
	private Color coordinatesColor = Color.blue;
	private Color coordinatesBgColor = Color.white;
	private int width = 182;
	int leftMargin = 7;			// margin between bg rectangle and edge
	int rightMargin = 12;		// margin between bg rectangle and edge
	int bottomMargin = 0;		// margin between bg rectable and edge
	int textYOffset = 23;		// top margin between rectangle and first text
	int firstColumnX = leftMargin + 13;		// from edge
	int secondColumnX = leftMargin + 55;	// from edge
	int thirdColumnX = leftMargin + 90;		// second contact map or seq sep
	int fourthColumnX = leftMargin + 133;	// leftMargin + 80 + (55-13)
	int hyphenXOffset = 28;
	int hyphenYOffset = 15;
	int hyphenLength = 7;
	int lineHeight = 20;		// offset between lines
	int totalHeight = 3 * lineHeight + bottomMargin + textYOffset;		// height for basic information and background
		
	// different modes for coordinate display (TODO: replace by enum)
	private boolean compareMode = false;			// if true, display in compare mode, otherwise single contact map mode
	private boolean seqSepMode = false;				// if true and in single contact map mode, show sequence separation
	//private boolean residueRulerMode = false;		// now toggled by setting iNum="" or jNum="" 
	// optional display flags
	private boolean showAliAndSeqPos = false;		// additionally show alignment coordinates
	private boolean hasSecondaryStructure = false;	// show secondary structure of i/j
	private boolean writePDBResNum = false;			// additionally show PDB residue numbers
	// data for first contact map
	private boolean isContact = false;				// if true, a hyphen is drawn between the coordinates to indicate contact
	private String iNum = "";						// sequence coordinate
	private String jNum = "";
	private String iAli = "";						// alignment coordinate
	private String jAli = "";
	private String iRes = "";						// residue type
	private String jRes = "";
	private String iSSType = "";					// secondary structure type (alpha/beta)
	private String jSSType = "";
	private String iPdbNum = "";					// PDB residue number (incl. ggf. insertion code)
	private String jPdbNum = "";
	private String title = "";						// name of contact map
	// data for second contact map in compare mode
	private boolean isContact2 = false;				// if true, a hyphen is drawn between the coordinates to indicate contact
	private String iNum2 = "";						// sequence coordinate
	private String jNum2 = "";
	private String iRes2 = "";						// residue type
	private String jRes2 = "";
	private String iSSType2 = "";					// secondary structure type (alpha/beta)
	private String jSSType2 = "";
	private String iPdbNum2 = "";					// PDB residue number (incl. ggf. insertion code)
	private String jPdbNum2 = "";
	private String title2 = "";						// name of contact map

	private String seqSep = "";						// sequence separation (can't this be calculated from iNum and jNum?)

	/*--------------------------   drawing ---------------------------------*/
	
	/** Method called by this component to determine its minimum size */
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(width,totalHeight);
	}

	/** Method called by this component to determine its preferred size */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(width,totalHeight);
	}

	/** Method called by this component to determine its maximum size */
	@Override
	public Dimension getMaximumSize() {
		return new Dimension(width,totalHeight);
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
		drawCoordinates(g2d);
	}
	
	protected void drawCoordinates(Graphics2D g2d) {

//		// old settings (when this was part of StatusBar)
//		int leftMargin = 7;			// margin between bg rectangle and edge
//		int rightMargin = 12;		// margin between bg rectangle and edge
//		int bottomMargin = 5;		// margin between bg rectable and edge
//		int textYOffset = 23;		// top margin between rectangle and first text
//		int firstColumnX = leftMargin + 13;		// from edge
//		int secondColumnX = leftMargin + 55;	// from edge
//		int thirdColumnX = leftMargin + 90;		// second contact map or seq sep
//		int fourthColumnX = leftMargin + 133;	// leftMargin + 80 + (55-13)
//		int hyphenXOffset = 28;
//		int hyphenYOffset = 15;
//		int hyphenLength = 7;
//		int lineHeight = 20;		// offset between lines
//		int baseLineY = getHeight() - totalHeight;	// top of bg rectangle in local coordinates of this component
		totalHeight = 3 * lineHeight + bottomMargin + textYOffset;
		if(writePDBResNum) {
			totalHeight += lineHeight;		// for pdb res numbers
		}	
		if(compareMode) {
			totalHeight += lineHeight;		// for title
		}
		if(compareMode && showAliAndSeqPos ) {
			totalHeight += lineHeight;		// for alignment coordinates
		}	
		
		int baseLineY = 0;	// top of bg rectangle in local coordinates of this component
		
		// draw background rectangle
		g2d.setColor(coordinatesBgColor);
		g2d.fill(new RoundRectangle2D.Float(leftMargin, baseLineY, getWidth()-rightMargin, totalHeight-bottomMargin, 12, 12));		
		g2d.setColor(coordinatesColor);
				
		// first contact map
		
		int x = firstColumnX;			// where first text will be written
		int y = baseLineY+textYOffset;	// where first text will be written
		int hyphenY = 0;				// remember where to draw the hyphen
		
		// coordinates for i
		
		if(compareMode) {
			g2d.drawString(title, x, y);			// name of contact map
			y += 20;
		} 
		
		if(iNum.length() > 0) {
			g2d.drawString(iNum, x,y);					// sequence coordinates
			hyphenY = y;
			y += 20;
			g2d.drawString(iRes, x, y);					// residue type
			y += 20;
			if (hasSecondaryStructure){
				g2d.drawString(iSSType, x, y);			// secondary structure
				y += 20;
			}
			if (writePDBResNum) {
				g2d.drawString(iPdbNum, x, y);			// PDB residue number
				y += 20;
			}
		}
		
		// coordinates for j
		
		if(jNum.length() > 0) {
			
			x = secondColumnX;
			y = baseLineY + textYOffset;
			
			if(compareMode) {
				y += 20;								// skip title
			} 
			g2d.drawString(jNum, x, y);					// sequence coordinates	
			y += 20;
			g2d.drawString(jRes, x, y);					// residue type
			y += 20;
			if (hasSecondaryStructure){
				g2d.drawString(jSSType, x, y);			// secondary structure
				y += 20;
			}
			if (writePDBResNum) {
				g2d.drawString(jPdbNum, x, y);			// PDB residue number
				y += 20;
			}
		}
		
		// draw hyphen if (i,j) is a contact
		if(iNum.length() > 0 && jNum.length() > 0 && isContact) {
			g2d.drawLine(firstColumnX+hyphenXOffset, hyphenY+hyphenYOffset, firstColumnX+hyphenXOffset+hyphenLength, hyphenY+hyphenYOffset);		
		}
		
		// write sequence separation in diagonal select mode
		if(seqSepMode && !compareMode) { 						// we don't show seq separation in compare mode
			g2d.drawString("SeqSep", thirdColumnX, baseLineY+textYOffset);
			g2d.drawString(seqSep, thirdColumnX, baseLineY+textYOffset+lineHeight);		
			
		}

		// second contact map
		
		if(compareMode) {
					
			// coordinates for i

			x = thirdColumnX;			// where first text will be written
			y = baseLineY+textYOffset;	// where first text will be written
			
			g2d.drawString(title2, x, y);			// name of contact map
			y += 20;
			
			if(iNum2.length() > 0) {
				
				g2d.drawString(iNum2, x,y);					// sequence coordinates
				hyphenY = y;
				y += 20;
				g2d.drawString(iRes2, x, y);				// residue type
				y += 20;
				if (hasSecondaryStructure){
					g2d.drawString(iSSType2, x, y);			// secondary structure
					y += 20;
				}
				if (writePDBResNum) {
					g2d.drawString(iPdbNum2, x, y);			// PDB residue number
					y += 20;
				}
			}
			
			// coordinates for j
			
			if(jNum2.length() > 0) {
				
				x = fourthColumnX;
				y = baseLineY + textYOffset;
				
				y += 20;									// skip title
				g2d.drawString(jNum2, x, y);				// sequence coordinates	
				y += 20;
				g2d.drawString(jRes2, x, y);				// residue type
				y += 20;
				if (hasSecondaryStructure){
					g2d.drawString(jSSType2, x, y);			// secondary structure
					y += 20;
				}
				if (writePDBResNum) {
					g2d.drawString(jPdbNum2, x, y);			// PDB residue number
					y += 20;
				}
			}
			
			// draw hyphen if (i,j) is a contact
			if(iNum2.length() > 0 && jNum2.length() > 0 && isContact2) {
				g2d.drawLine(thirdColumnX+hyphenXOffset, hyphenY+hyphenYOffset, thirdColumnX+hyphenXOffset+hyphenLength, hyphenY+hyphenYOffset);		
			}
			
			// optionally draw alignment coordinates
			if(showAliAndSeqPos) {
				g2d.drawString("Alignm Pos:", firstColumnX, y);
				if(iNum.length() > 0) g2d.drawString(iAli, thirdColumnX, y);			// alignment coordinates
				if(jNum.length() > 0) g2d.drawString(jAli, fourthColumnX, y);			// alignment coordinates
			}
		
		}
		
	}

	/*-------------------------- getters and setters -----------------------*/
	
	public void setINum(String string) {
		iNum = string;
		
	}

	public void setJPdbNum(String string) {
		jPdbNum = string;
		
	}

	public void setIPdbNum(String string) {
		iPdbNum = string;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setWritePDBResNum(boolean b) {
		writePDBResNum = false;
		
	}

	public void setSeqSep(String string) {
		seqSep = string;
		
	}

	public void setIsDiagSecMode(boolean b) {
		seqSepMode = b;
		
	}

	public void setIsContact(boolean b) {
		isContact = b;
		
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

	public void setJNum(String string) {
		jNum = string;
		
	}

	public void setCompareMode(boolean b) {
		compareMode = b;
		
	}

	public void setIsContact2(boolean isContact2) {
		this.isContact2 = isContact2;
	}

	public void setINum2(String num2) {
		iNum2 = num2;
	}

	public void setJNum2(String num2) {
		jNum2 = num2;
	}

	public void setIRes2(String res2) {
		iRes2 = res2;
	}

	public void setJRes2(String res2) {
		jRes2 = res2;
	}

	public void setISSType2(String type2) {
		iSSType2 = type2;
	}

	public void setJSSType2(String type2) {
		jSSType2 = type2;
	}

	public void setIPdbNum2(String pdbNum2) {
		iPdbNum2 = pdbNum2;
	}

	public void setJPdbNum2(String pdbNum2) {
		jPdbNum2 = pdbNum2;
	}

	public void setTitle2(String title2) {
		this.title2 = title2;
	}

	
	
}
