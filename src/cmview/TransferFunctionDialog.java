package cmview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class TransferFunctionDialog extends JFrame implements ActionListener{
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	ContactMapPane cmPane;
	View view;
	
	TransferFunctionBar tfFct;
	StatusBar stat;
	JPanel tffP;
	
//	private class MyDispatcher implements KeyEventDispatcher {
//		private TransferFunctionBar tfFct;
//		private MyDispatcher(TransferFunctionBar tfFct){
//			this.tfFct = tfFct;
//		}
//	    public boolean dispatchKeyEvent(KeyEvent e) {
//	    	// -- forward key events --
//	        if (e.getID() == KeyEvent.KEY_PRESSED) {
//	        	this.tfFct.keyPressed(e);
//	        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
//	        	this.tfFct.keyReleased(e);
//	        } else if (e.getID() == KeyEvent.KEY_TYPED) {
//	        	this.tfFct.keyTyped(e);
//	        }
//	        return false;
//	    }
//	}
	
	TransferFunctionDialog(View view, ContactMapPane cmPane){
		super("TransferFunction Selection");
		this.view = view;
		this.cmPane = cmPane;
		this.tfFct = new TransferFunctionBar(this, this.view);
		stat = new StatusBar(view);
		
//		setMinimumSize(new Dimension(4*TransferFunctionBar.DEFAULT_WIDTH, TransferFunctionBar.DEFAULT_HEIGHT));
		setPreferredSize(new Dimension(10+4*TransferFunctionBar.DEFAULT_WIDTH, 40+TransferFunctionBar.DEFAULT_HEIGHT));
		initFrame();
		setVisible(true);							// show GUI	

//		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
//	    manager.addKeyEventDispatcher(new MyDispatcher(this.tfFct));
		
		final JFrame parent = this;					// need a final to refer to in the thread below
		EventQueue.invokeLater(new Runnable() {		// execute after other events have been processed
			public void run() {
				parent.toFront();					// bring new window to front
			}
		});
	}

	private void initFrame() {
		// TODO Auto-generated method stub
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocation(20,20);
		
		this.tffP = new JPanel(new BorderLayout()); 		// panel holding the transferFunctionPanel
		this.tffP.add(this.tfFct, BorderLayout.WEST);
//		this.getContentPane().add(tffP, BorderLayout.CENTER); // and get rid of this line
		this.getContentPane().add(this.stat, BorderLayout.CENTER);
		this.getContentPane().add(this.tfFct,BorderLayout.WEST);
//		this.add(this.tffP);
//		this.add(this.stat, BorderLayout.WEST);
		
		pack();
	}
	
	public TransferFunctionBar getTransfFctBar(){
		return this.tfFct;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}
