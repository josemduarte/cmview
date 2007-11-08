package cmview.sadpAdapter;

import javax.swing.JButton;

public class SADPDialogDoneNotifier extends JButton {
    static final long serialVersionUID = 1l;
    Integer    status = SADPDialog.IDLE;
    SADPRunner runner;
    
    public SADPDialogDoneNotifier(SADPRunner runner) {
	this.runner = runner;
    }
    
    /**
     * Set the notification status for this notifier.
     * @param status
     * @see #secureNotify(Integer)
     */
    public void notify(Integer status) {
	this.status = status;
	doClick();
    }
    
    /**
     * Sets the notification status for this notifier in a secure way: If the 
     * current status differs from <code>ToolDialog.IDLE</code> the given 
     * status will not be stored.
     * @param status  the new status
     */
    public void secureNotify(Integer status) {
	if(this.status == SADPDialog.IDLE) {
	    this.status = status;
	    doClick();
	}
    }

    public Integer notification() {
	return status;
    }
        
    public SADPRunner getRunner() {
	return runner;
    }
}
