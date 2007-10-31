package cmview.sadpAdapter;

import javax.swing.JButton;

import cmview.toolUtils.ToolDialog;


public class SADPDialogDoneNotifier extends JButton {
    static final long serialVersionUID = 1l;
    Integer    status = ToolDialog.IDLE;
    SADPRunner runner;
    
    public SADPDialogDoneNotifier(SADPRunner runner) {
	this.runner = runner;
    }
    
    public void notify(Integer status) {
	this.status = status;
	doClick();
    }

    public Integer notification() {
	return status;
    }
        
    public SADPRunner getRunner() {
	return runner;
    }
}
