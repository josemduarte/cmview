package cmview.gmbp;

import cmview.Start;

public class ContactGUIState {
	
    /*------------------------------ constants ------------------------------*/
	
	protected static final SelMode INITIAL_SEL_MODE = SelMode.RECT;
	
	/*--------------------------- type definitions --------------------------*/
	
	// the selection mode
	protected enum SelMode {RECT, CLUSTER};
	
	/*--------------------------- member variables --------------------------*/
	
	// current gui state
	private ContactView view;					// the parent view
	private SelMode selectionMode;		// current selection mode, modify using setSelectionMode
	private boolean showRulers;			// whether showing angle rulers is switched on

	/**
	 * Initializes the GUI state with default values.
	 */
	ContactGUIState(ContactView view) {
		this.view = view;
		this.selectionMode = INITIAL_SEL_MODE;
		this.showRulers=Start.SHOW_RULERS;
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
	
	/*---------------- setters ---------------*/
	
	/**
	 * Sets the current selection mode. This sets the internal state variable and changes some gui components.
	 * Use getSelectionMode to retrieve the current state.
	 */
	protected void setSelectionMode(SelMode mode) {
		// switch on toggle buttons
//		switch(mode) {
//		case RECT: view.tbSquareSel.setSelected(true); break;
//		case CLUSTER: view.tbClusterSel.setSelected(true); break;
//		default: System.err.println("Error in setSelectionMode. Unknown selection mode " + mode); return;
//		}
		this.selectionMode = mode;
	}
	
	/**
	 * @param showRulers the showRulers to set
	 */
	protected void setShowRulers(boolean showRulers) {
		this.showRulers = showRulers;
	}
}
