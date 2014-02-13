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
import javax.swing.JPanel;
import javax.swing.JProgressBar;


/**
 * Abstract base class providing a GUI for any kind of tools.
 * The resulting dialog provides a button pane (with a "Start", "Preferences" 
 * and "Cancel" button), a progress bar and a user-defined component. It can 
 * be constructed with or without "Preferences" and/or "Cancel" buttons. For 
 * each button there is a abstract method which has to be implemented. It is 
 * invoked whenever the respective button as been pressed. The behavior of the 
 * buttons is well defined and follows these simple rules. Button ...
 * <ul>
 * <li><b>Start</b> has been pressed button which directly leads to a 
 * suspension of the "Preferences" and "Start" buttons (button "Cancel" 
 * remains usable to cancel the tool). Additionally, method 
 * {@link #start()} is invoked. </li>
 * <li><b>Preferences</b> has been pressed which is supposed to open a 
 * preferences dialog if such behavior is implemented by the user. Method 
 * {@link #preferences()} is invoked.</li>
 * <li><b>Cancel</b> has been pressed. The dialog is be disposed 
 * immediately. Method {@link #cancel()} is invoked.</li>
 * </ul>
 */
public abstract class ToolDialog extends JDialog implements ActionListener {

    static final long serialVersionUID = 1l;
    
    JButton startButton;
    JButton cancelButton;
    JButton preferencesButton;
    
    protected JProgressBar progressBar;
    
    // status information of the dialog
    /** dialog is in idle mode, nothing to do so far */
    public static final int IDLE                = 0;
    /** user has pressed the start button */
    public static final int STARTED             = 1;
    /** user has pressed the cancel button in the IDLE state */
    public static final int CANCELED_AT_IDLE    = 2;
    /** user has pressed the cancel button in the STARTED state */
    public static final int CANCELED_AT_STARTED = 3;
    /** tool has returned from the computation */
    public static final int DONE                = 4;
    
    /** current status of the dialog */
    Integer status = IDLE; 
    
    // construction settings
    public static final int CONSTRUCT_EVERYTHING = -1;
    /** construct dialog without the cancel button */
    public static final int CONSTRUCT_WITHOUT_CANCEL = 0;
    /** construct dialog without the preferences button */
    public static final int CONSTRUCT_WITHOUT_PREFERENCES = 1;
    /** construct dialog without the cancel and the preferences button */
    public static final int CONSTRUCT_WITHOUT_CANCEL_AND_PREFERENCES = 2;
    /** construct dialog with the start and the preferences button */
    public static final int CONSTRUCT_WITHOUT_START_AND_PREFERENCES = 4;
    /** construct dialog with any button */
    public static final int CONSTRUCT_WITHOUT_ANY_BUTTONS = 5;
    
    /** construction status */
    int constructionStatus;
    
    public ToolDialog( Frame f, String title, boolean model ) {
	super(f,title,model);
	initDialog(CONSTRUCT_EVERYTHING);
    }
    
    public ToolDialog( Dialog d, String title, boolean model ) {
	super(d,title,model);
	initDialog(CONSTRUCT_EVERYTHING);
    }
    
    public ToolDialog( Frame f, String title, int constructionInfo, boolean modal ) {
	super(f,title,modal);
	initDialog(constructionInfo);
    }

    public ToolDialog( Dialog d, String title, int constructionInfo, boolean modal ) {
	super(d,title,modal);
	initDialog(constructionInfo);
    }
    
    /**
     * Creates default tool dialog with a progress bar at the top and a button
     * pane (START,PREFRENCES and CANCEL) right below.
     * */
    protected void initDialog(int constructionInfo) {
	// create buttons and add action listeners
	startButton       = new JButton("Start");
	preferencesButton = new JButton("Preferences");
	cancelButton      = new JButton("Cancel");
	startButton.addActionListener(this);
	preferencesButton.addActionListener(this);
	cancelButton.addActionListener(this);
	
	// set default button to the start button
	this.getRootPane().setDefaultButton(startButton);
	
	// create progress bar and put i
	progressBar = new JProgressBar(0,100);
	//progressBar.setValue(50);
	progressBar.setBorderPainted(true);
	progressBar.setLayout(new BoxLayout(progressBar, BoxLayout.LINE_AXIS));
	progressBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
	//JPanel progressBarPane = new JPanel();
	//progressBarPane.setLayout(new BoxLayout(progressBarPane, BoxLayout.LINE_AXIS));
	//progressBarPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	
	// align buttons in a button-pane
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
	buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	// add Start button
	if( !(constructionInfo == CONSTRUCT_WITHOUT_START_AND_PREFERENCES
		|| constructionInfo == CONSTRUCT_WITHOUT_ANY_BUTTONS) ) {
	    buttonPane.add(startButton);
	}
	// add Preferences button
	if( !(constructionInfo == CONSTRUCT_WITHOUT_PREFERENCES 
		|| constructionInfo == CONSTRUCT_WITHOUT_CANCEL_AND_PREFERENCES 
		|| constructionInfo == CONSTRUCT_WITHOUT_START_AND_PREFERENCES 
		|| constructionInfo == CONSTRUCT_WITHOUT_ANY_BUTTONS) ) {
	    buttonPane.add(Box.createHorizontalStrut(10));
	    buttonPane.add(preferencesButton);
	}
	// add cancel button
	if( !(constructionInfo == CONSTRUCT_WITHOUT_CANCEL 
		|| constructionInfo == CONSTRUCT_WITHOUT_CANCEL_AND_PREFERENCES
		|| constructionInfo == CONSTRUCT_WITHOUT_ANY_BUTTONS) ) {
	    buttonPane.add(Box.createHorizontalStrut(10));
	    buttonPane.add(cancelButton);
	}
		
	// put everything together arranged in a grid
	//GridLayout grid = new GridLayout(3,1);
	Container contentPane = getContentPane();
	contentPane.setLayout(new BorderLayout());
	contentPane.add(infoComponent(),BorderLayout.NORTH);
	contentPane.add(progressBar,BorderLayout.CENTER);
	contentPane.add(buttonPane,BorderLayout.SOUTH);
	
	constructionStatus = constructionInfo;
    }
        
    /**
     * Action to be performed are always connected to the buttons. Therefore, 
     * "action-to-be-performed" functions for the respective button is called
     * and when it returns the new dialog state is set.
     * @param e an action-event
     * */
    public void actionPerformed(ActionEvent e) {
	if(e.getSource() == startButton ) {
	    start();
	    setStatus(STARTED);
	} else if(e.getSource() == preferencesButton ) {
	    preferences();
	} else if(e.getSource() == cancelButton ) {
	    cancel();
	    if( getStatus() == IDLE ) {
		setStatus(CANCELED_AT_IDLE);
	    } else {
		setStatus(CANCELED_AT_STARTED);
	    }
	}
    }
    
    /**
     * Shows dialog an position it relative to its parent frame.
     */
    public void createGUI() {
	// pack all things in the dialog and determine dialog bounds
	pack();
	// position the dialog right in the center of its parent frame
	setLocationRelativeTo(getParent());
	showIt();
    }
    
    /**
     * Show dialog
     * @see #createGUI()
     */
    public void showIt() {
	pack();
	setVisible(true);
    }
    
    /**
     * Gets the start button, supposed to start the tool. 
     */
    public JButton getStartButton() {
	return startButton;
    }
    
    /**
     * Gets the cancel button, supposed to cancel the tool. 
     */
    public JButton getCanceButton() {
	return cancelButton;
    }
    
    /**
     * Gets the preferences button, supposed to pop up a dialog to perform 
     * preference settings. 
     */
    public JButton getPreferencesButton() {
	return preferencesButton;
    }
    
    /**
     * Gets progress bar. This one can be manipulated with the progress status 
     * of the running tool. 
     */
    public JProgressBar getProgressBar() {
	return progressBar;
    }
    
    /**
     * Sets runtime status of this instance. Do not use this function directly. 
     * The proper value is set according to the recently pressed button and the 
     * previous state of the dialog.
     * @param status  set this value to one of these class constants: 
     * <code>IDLE</code>, <code>START</code>, <code>CANCELED_AT_IDLE</code>, 
     * <code>CANCELED_AT_STARTED</code> and <code>DONE</code>.
     */
    public void setStatus(int status) {
	this.status = status;
    }
    
    /**
     * Gets the runtime status of this instance. The value shall equal to one 
     * of these class constants: <code>IDLE</code>, <code>START</code>, 
     * <code>CANCELED_AT_IDLE</code>, <code>CANCELED_AT_STARTED</code> or 
     * <code>DONE</code>.
     * @return the runtime status
     */
    public int getStatus() {
	return status;
    }
    
    /**
     * Gets the construction status, i.e. a number defining the set of visible 
     * buttons of this object.
     * @return  the contruction status
     */
    public int getConstructionStatus() {
	return constructionStatus;
    }
    
    /**
     * Action to be performed when button START has been pressed.
     */
    public abstract void start();
    
    /**
     * Action to be performed when button CANCEL has been pressed.
     */
    public abstract void cancel();
    
    /**
     * Action to be performed when button PREFERENCES has been pressed.
     * */
    public abstract void preferences();
    
    /**
     * Pane holding user defined information concerning the tool in use.
     * @return any kind of informative component
     */
    protected abstract Component infoComponent();
}
