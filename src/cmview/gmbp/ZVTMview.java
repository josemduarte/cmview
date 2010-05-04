package cmview.gmbp;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;

import javax.swing.JFrame;

import net.claribole.zvtm.engine.Java2DPainter;

import com.xerox.VTM.engine.View;
import com.xerox.VTM.engine.JPanelView;

import cmview.Start;

public class ZVTMview extends JFrame implements Java2DPainter {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private View view;
	private JPanelView pview;

	public ZVTMview(){
		
		this.setPreferredSize(new Dimension(Start.INITIAL_SCREEN_SIZE,Start.INITIAL_SCREEN_SIZE));
		
//		Camera cam = vs.addCamera();
//		Vector cameras = new Vector(); 
//		cameras.add(cam);
//		
//		view.setJava2DPainter(new Java2DPainter(), Java2DPainter.FOREGROUND);
		
		final JFrame parent = this;					// need a final to refer to in the thread below
		EventQueue.invokeLater(new Runnable() {		// execute after other events have been processed
			public void run() {
				parent.toFront();					// bring new window to front
			}
		});
	}

	@Override
	public void paint(Graphics2D g2d, int viewWidth, int viewHeight) {
		// TODO Auto-generated method stub
		g2d.drawString("aString", 10, viewHeight-20);
	}

}
