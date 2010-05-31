package cmview.gmbp;

import cmview.Start;

public class ContactGUIState {
	
    /*------------------------------ constants ------------------------------*/
	
	protected static final SelMode INITIAL_SEL_MODE = SelMode.RECT;
	
	/*--------------------------- type definitions --------------------------*/
	
	// the selection mode
	protected enum SelMode {RECT, CLUSTER, PAN};
	
	/*--------------------------- member variables --------------------------*/
	
	// current gui state
	private ContactView view;					// the parent view
	private SelMode selectionMode;		// current selection mode, modify using setSelectionMode
	private boolean showRulers;			// whether showing angle rulers is switched on
	private boolean showSphoxelBG;		// whether showing the SphoxelBackground is switched on
	private boolean showNBHStraces;	    // whether showing the NBHStringTraces is switched on
	private boolean showLongitudes;     // whether showing the longitudes is switched on
	private boolean showLatitudes;      // whether showing the latitudes is switched on
	private boolean showLongLatCentre;  // whether showing the longlatcentre is switched on

	/**
	 * Initializes the GUI state with default values.
	 */
	ContactGUIState(ContactView view) {
		this.view = view;
		this.selectionMode = INITIAL_SEL_MODE;
		this.showRulers=Start.SHOW_RULERS;
		this.showSphoxelBG = true;
		this.showNBHStraces = true;
		this.showLongitudes = true;
		this.showLatitudes = true;
		this.showLongLatCentre = true;
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
	 * @return the showRulers
	 */
	protected boolean getShowRulers() {
		return showRulers;
	}
	
	/**
	 * @return the showSphoxelBG
	 */
	protected boolean getShowSphoxelBG() {
		return showSphoxelBG;
	}
	
	/**
	 * @return the showNBHStraces
	 */
	protected boolean getShowNBHStraces() {
		return showNBHStraces;
	}
	
	/**
	 * @return the showLongitudes
	 */
	protected boolean getShowLongitudes() {
		return showLongitudes;
	}

	/**
	 * @return the showLatitudes
	 */
	protected boolean getShowLatitudes() {
		return showLatitudes;
	}

	/**
	 * @return the showLongLatCentre
	 */
	protected boolean getShowLongLatCentre() {
		return showLongLatCentre;
	}
	
	/*---------------- setters ---------------*/
	
	/**
	 * Sets the current selection mode. This sets the internal state variable and changes some gui components.
	 * Use getSelectionMode to retrieve the current state.
	 */
	protected void setSelectionMode(SelMode mode) {
		// switch on toggle buttons
		switch(mode) {
		case RECT: view.tbSquareSel.setSelected(true); break;
//		case CLUSTER: view.tbClusterSel.setSelected(true); break;
		case PAN: view.tbPanMode.setSelected(true); break;
		default: System.err.println("Error in setSelectionMode. Unknown selection mode " + mode); return;
		}
		this.selectionMode = mode;
	}
	
	/**
	 * @param showRulers the showRulers to set
	 */
	protected void setShowRulers(boolean showRulers) {
		this.showRulers = showRulers;
	}
	
	/**
	 * @param showSphoxelBG the showSphoxelBG to set
	 */
	protected void setShowSphoxelBG(boolean showSphoxelBG) {
		this.showSphoxelBG = showSphoxelBG;
	}
	
	/**
	 * @param showNBHStraces the showNBHStraces to set
	 */
	protected void setShowNBHStraces(boolean showNBHStraces) {
		this.showNBHStraces = showNBHStraces;
	}
}
