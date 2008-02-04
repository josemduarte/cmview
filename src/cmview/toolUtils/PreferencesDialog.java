package cmview.toolUtils;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A preferences dialog consist of a button pane (with an "OK", "Apply" and 
 * "Cancel" button) and a tabbed-pane. Preference categories can be added 
 * to the tabbed-pane by means of new panes. Conceivable application of this 
 * strategy could aim to a very general splitting of preferences into "Basis"
 * and rather "Advanced" ones where each of those is represented in a 
 * separated pane. 
 * 
 * @author Lars Petzold
 */
public abstract class PreferencesDialog extends JDialog implements ActionListener,ChangeListener {

    protected JButton     okButton;
    protected JButton     cancelButton;
    protected JButton     applyButton;
    protected JTabbedPane tabbedPane;
    protected int         previousTab            = 0;
    protected boolean     returnedToUnsavedTab = false;

    /**
     * Construct preferences dialog with the parent frame and the window title.
     * @param d      parent dialog
     * @param title  window title
     */
    public PreferencesDialog( Frame f, String title ) {
	super(f,title);
	initDialog();
    }

    /**
     * Construct preferences dialog with the parent dialog and the window title.
     * @param d      parent dialog
     * @param title  window title
     */
    public PreferencesDialog( Dialog d, String title ) {
	super(d,title);
	initDialog();
    }

    /**
     * Initializes dialog by constructing an empty tabbed-panel and a button-
     * pane with OK and CANCEL buttons.
     * */
    protected void initDialog() {
	// create buttons and make them action sensitive
	okButton     = new JButton("OK");
	cancelButton = new JButton("Cancel");
	applyButton  = new JButton("Apply");
	okButton.addActionListener(this);
	cancelButton.addActionListener(this);
	applyButton.addActionListener(this);

	// align buttons in a button-pane
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
	buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	buttonPane.add(okButton);
	buttonPane.add(Box.createHorizontalStrut(10));
	buttonPane.add(applyButton);
	buttonPane.add(Box.createHorizontalStrut(10));
	buttonPane.add(cancelButton);

	// create tabbed-pane together with the first tab
	tabbedPane = new JTabbedPane();
	tabbedPane.addChangeListener(this);

	// put everything together
	Container contentPane = getContentPane();
	contentPane.add(tabbedPane,BorderLayout.PAGE_START);
	contentPane.add(buttonPane,BorderLayout.PAGE_END);
    }    

    /**
     *  Action listener for the buttons.
     *  @see ok()
     *  @see cancel() 
     */
    public void actionPerformed(ActionEvent e) {
	if( e.getSource() == okButton ) {
	    ok();
	} else if( e.getSource() == cancelButton ) {
	    cancel();
	} else if( e.getSource() == applyButton ) {
	    apply(getTabbedPane().getSelectedIndex());
	} else {
	    System.err.println("Error: unrecognised action performed. Implement a proper exception handling to catch this!");
	}
    }

    /**
     * State listener for the tabs.
     * A change event is triggered when the tab has been changed.
     * If there are unsaved changes the user will be asked to clean the 
     * situation. 
     * @param c  the change event
     * */
    public void stateChanged(ChangeEvent c) {
	if( c.getSource() == getTabbedPane() ) {
	    if( hasAnythingChanged() && !isReturnedToUnsavedTab() ) {
		System.out.println("handleUnsavedChanges getriggert von:"+getTabbedPane().getTitleAt(getTabbedPane().getSelectedIndex()));
		handleUnsavedChanges();
	    } 
	    setPreviousTab(getTabbedPane().getSelectedIndex());
	}
    }

    /** 
     * Initializes, shows dialog and positions it relative to the parent 
     * component.
     */
    public void createGUI() {
	// pack all things in the dialog and determine dialog bounds
	pack();
	// position the dialog right in the center of its parent frame
	setLocationRelativeTo(getParent());
	showIt();
    }

    /**
     * Packs window and sets it visible.
     */
    public void showIt() {
	pack();
	setVisible(true);		
    }

    /**
     * Action to be performed when the OK button has been clicked 
     */
    public abstract void ok();

    /**
     * Action to be performed when the CANCEL button has been clicked. 
     */
    public abstract void cancel();

    /**
     * Action to be performed when the APPLY button has been clicked.
     * @param tabIdx  apply modification made in the tabbed-pane having this 
     *                 index
     */
    public abstract boolean apply(int tabIdx);

    /**
     * Resets setting in tabbed-pane with the given index
     * @param i  index of the tabbed-pane to be reset.
     */
    public abstract void reset(int i);

    /**
     * Tests whether anything has changed.
     * This function is called when visible the tabbed-pane has changed.
     * @return true if there are 
     */
    protected abstract boolean hasAnythingChanged();

    /**
     * Gets the OK button.
     * @return the OK button
     */
    public JButton getOkButton() {
	return okButton;
    }

    /**
     * Gets the Cancel button
     * @return the cancel button
     */
    public JButton getCancelButton() {
	return cancelButton;
    }

    /**
     * Gets the Apply button.
     * @return the apply button
     */
    public JButton getApplyButton() {
	return applyButton;
    }

    /**
     * Gets the tabbed-pane which is meant to hold all preference tabs.
     * @return the tabbed-pane
     */
    public JTabbedPane getTabbedPane() {
	return tabbedPane;
    }

    /**
     * Adds a new tabbed-pane to the tabbed-panel of the preference dialog.
     * @param title  title of the new tabbed-pane to be added
     * @param comp   component to be added in the new tabbed pane (the data)
     */
    public void addTab(String title, Component comp) {
	tabbedPane.addTab(title,comp);
    }

    /**
     * Handles unsaved changes.
     * The user will interactively be prompted to clean up the situation by
     * applying or discarding the changes. A third alternative is given by a 
     * Cancel button which brings the focus back to the tabbed-pane with the 
     * unsaved modified settings.
     */
    protected void handleUnsavedChanges() {
	// the dialog's message
	String[] message = new String[2];
	message[0] = "Would you like to save changes before switching the tab?";
	message[1] = "Press the Cancel button to return to the recently modified tab.";
	// the button labels
	Object[] buttonLabels = {"Save","Discard","Cancel"};
	int decision = JOptionPane.showOptionDialog(this,
		message,
		"Unsaved changes!", 
		JOptionPane.YES_NO_CANCEL_OPTION,
		JOptionPane.WARNING_MESSAGE,
		null,
		buttonLabels, 
		buttonLabels[0]);
	System.out.println("decision="+decision);
	// 0 -> Save: apply changes, enter recently chosen tab
	// 1 -> Discard: discard changes, enter recently chosen tab
	// 2 -> Cancel: return to the previous tab
	switch(decision) {
	case 0:
	    if( apply(getPreviousTab()) ) {
		// application of modification made in the previous tabbed-pane
		// have been completed successfully
		// -> we remain in the recently chose tabbed-pane
		isReturnedToUnsavedTab(false);
	    } else {
		// modification could not be applied for some reasons
		// -> return to the previous tabbed-pane and lets see what went 
		//     wrong
		// -> reset returned-to-unsaved-tab to check values again when 
		//     switching to the next tabbed-pane
		isReturnedToUnsavedTab(true);
		getTabbedPane().setSelectedIndex(getPreviousTab());
		isReturnedToUnsavedTab(false);
	    }
	    break;
	case 1:
	    // discard the modification made in the previous tabbed-pane and 
	    // remain the the recently selected one
	    reset(getPreviousTab());
	    isReturnedToUnsavedTab(false);
	    break;
	case 2:
	    // return to the previous pane at continue modifications
	    // -> we have to set the "returned-to-unsaved" state as the 
	    //    function setSelectedIndex() directly triggers a new 
	    //    change event with the "return-to-unsaved" state not 
	    //    catching
	    isReturnedToUnsavedTab(true);
	    getTabbedPane().setSelectedIndex(getPreviousTab());
	    break;
	default:
	    System.err.println("Error: confirmation dialog for catch unsaved-change event has an unrecognised button!");
	}

    }

    /**
     * Sets the index of the future previous pane which is the current pane.
     * Confusion but it makes still sense as the index of the previous pane 
     * should only be considered when the selected tabbed pane has changed. 
     * In that case, the recently selected pane is the future previous pane, 
     * isn't it?
     * @param idx  index of the future previous pane
     * @see #getPreviouseTab()
     */
    protected void setPreviousTab(int idx) {
	previousTab = idx;
    }

    /**
     * Index of the previously selected tabbed-pane.
     * @return the index
     * @see #setPreviousTab(int)
     */
    protected int getPreviousTab() {
	return previousTab;
    }
    
    protected void isReturnedToUnsavedTab(boolean isReturned) {
	returnedToUnsavedTab = isReturned;
    }

    protected boolean isReturnedToUnsavedTab() {
	return returnedToUnsavedTab;
    }
}
