package cmview;

import java.awt.Color;

/**
 * The current GUI state of a View instance. This currently includes the current selection mode, compare mode and whether various display options
 * are switched on or off (distance map, pdb residue numbers, etc).
 */
public class GUIState {

	/*------------------------------ constants ------------------------------*/
	
	protected static final SelMode INITIAL_SEL_MODE = SelMode.RECT;
	
	/*--------------------------- type definitions --------------------------*/
	
	// the selection mode
	protected enum SelMode {RECT, FILL, NBH, COMNBH, DIAG, COLOR};
	
	/*--------------------------- member variables --------------------------*/
	
	// current gui state
	private View view;					// the parent view
	private SelMode selectionMode;		// current selection mode, modify using setSelectionMode
	private boolean showPdbSers;		// whether showing pdb serials is switched on
	private boolean showRulers;			// whether showing residue rulers is switched on
	private boolean showIconBar;		// whether showing the icon bar is switched on
	private boolean showNbhSizeMap;		// whether showing the common neighbourhood size map is switched on 
	private boolean showDensityMap;		// whether showing the density map is switched on
	private boolean showDistanceMap;	// whether showing the distance map is switched on
	private boolean compareMode;		// whether we are in pairwise comparison mode (i.e. a second structure has been loaded)
	private Color paintingColor;		// current color for coloring contacts selected by the user
	private boolean showCommon;			// when true, common contacts displayed in compare mode (and selections are only for common)
	private boolean showFirst; 			// when true, contacts unique to first structure displayed in compare mode (and selections are only for first)
	private boolean showSecond; 		// when true, contacts unique to second structure displayed in compare mode (and selections are only for second)
	private boolean showDiffDistMap; 	// whether showing the difference distance map is switched on
	
	/*----------------------------- constructors ----------------------------*/
	
	/**
	 * Initializes the GUI state with default values.
	 */
	GUIState(View view) {
		// TODO: add showAlignmentIndices
		this.view = view;
		this.selectionMode = INITIAL_SEL_MODE;
		this.showPdbSers = false;
		this.showRulers=Start.SHOW_RULERS_ON_STARTUP;
		this.showIconBar=Start.SHOW_ICON_BAR;
		this.showNbhSizeMap = false;
		this.showDensityMap=false;
		this.showDistanceMap=false;
		this.compareMode = false;	
		this.paintingColor = Color.blue;
		this.showCommon= true;
		this.showFirst= true;
		this.showSecond= true;
		this.showDiffDistMap = false;
	}
	
	/*---------------------------- public methods ---------------------------*/

	/*---------------- getters ---------------*/

	/**
	 * @return the selectionMode
	 */
	protected SelMode getSelectionMode() {
		return selectionMode;
	}

	/**
	 * @return the showPdbSers
	 */
	protected boolean getShowPdbSers() {
		return showPdbSers;
	}

	/**
	 * @return the showRulers
	 */
	protected boolean getShowRulers() {
		return showRulers;
	}

	/**
	 * @return the showIconBar
	 */
	protected boolean getShowIconBar() {
		return showIconBar;
	}

	/**
	 * @return the showNbhSizeMap
	 */
	protected boolean getShowNbhSizeMap() {
		return showNbhSizeMap;
	}

	/**
	 * @return the showDensityMap
	 */
	protected boolean getShowDensityMap() {
		return showDensityMap;
	}

	/**
	 * @return the showDistanceMap
	 */
	protected boolean getShowDistanceMap() {
		return showDistanceMap;
	}

	/**
	 * @return the compareMode
	 */
	protected boolean getCompareMode() {
		return compareMode;
	}

	/**
	 * @return the paintingColor
	 */
	protected Color getPaintingColor() {
		return paintingColor;
	}

	/**
	 * @return the showCommon
	 */
	protected boolean getShowCommon() {
		return showCommon;
	}

	/**
	 * @return the showFirst
	 */
	protected boolean getShowFirst() {
		return showFirst;
	}

	/**
	 * @return the showSecond
	 */
	protected boolean getShowSecond() {
		return showSecond;
	}

	/**
	 * @return the showDiffDistMap
	 */
	protected boolean getShowDiffDistMap() {
		return showDiffDistMap;
	}

	/*---------------- setters ---------------*/
	
	/**
	 * Sets the current selection mode. This sets the internal state variable and changes some gui components.
	 * Use getSelectionMode to retrieve the current state.
	 */
	protected void setSelectionMode(SelMode mode) {
		switch(mode) {
		case RECT: view.tbSquareSel.setSelected(true); break;
		case FILL: view.tbFillSel.setSelected(true); break;
		case NBH: view.tbNbhSel.setSelected(true); break;
		case COMNBH: view.tbShowComNbh.setSelected(true); break;
		case DIAG: view.tbDiagSel.setSelected(true); break;
		case COLOR : view.tbSelModeColor.setSelected(true); break;
		default: System.err.println("Error in setSelectionMode. Unknown selection mode " + mode); return;
		}
		this.selectionMode = mode;
	}
	
	/**
	 * @param showPdbSers the showPdbSers to set
	 */
	protected void setShowPdbSers(boolean showPdbSers) {
		this.showPdbSers = showPdbSers;
	}

	/**
	 * @param showRulers the showRulers to set
	 */
	protected void setShowRulers(boolean showRulers) {
		this.showRulers = showRulers;
	}

	/**
	 * @param showIconBar the showIconBar to set
	 */
	protected void setShowIconBar(boolean showIconBar) {
		this.showIconBar = showIconBar;
	}

	/**
	 * @param showNbhSizeMap the showNbhSizeMap to set
	 */
	protected void setShowNbhSizeMap(boolean showNbhSizeMap) {
		this.showNbhSizeMap = showNbhSizeMap;
	}

	/**
	 * @param showDensityMap the showDensityMap to set
	 */
	protected void setShowDensityMap(boolean showDensityMap) {
		this.showDensityMap = showDensityMap;
	}

	/**
	 * @param showDistanceMap the showDistanceMap to set
	 */
	protected void setShowDistanceMap(boolean showDistanceMap) {
		this.showDistanceMap = showDistanceMap;
	}

	/**
	 * @param compareMode the compareMode to set
	 */
	protected void setCompareMode(boolean compareMode) {
		this.compareMode = compareMode;
	}

	/**
	 * @param paintingColor the paintingColor to set
	 */
	protected void setPaintingColor(Color paintingColor) {
		this.paintingColor = paintingColor;
	}

	/**
	 * @param showCommon the showCommon to set
	 */
	protected void setShowCommon(boolean showCommon) {
		this.showCommon = showCommon;
	}

	/**
	 * @param showFirst the showFirst to set
	 */
	protected void setShowFirst(boolean showFirst) {
		this.showFirst = showFirst;
	}

	/**
	 * @param showSecond the showSecond to set
	 */
	protected void setShowSecond(boolean showSecond) {
		this.showSecond = showSecond;
	}

	/**
	 * @param showDiffDistMap the showDiffDistMap to set
	 */
	protected void setShowDiffDistMap(boolean showDiffDistMap) {
		this.showDiffDistMap = showDiffDistMap;
	}
	
}
