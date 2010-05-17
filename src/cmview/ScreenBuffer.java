package cmview;

import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.JComponent;

/**
 * A buffered image to be shown on screen.
 */
public class ScreenBuffer {

    /*--------------------------- member variables --------------------------*/

    private JComponent parent;		// to determine the size
    private Image image;			// internal image
    private Graphics2D g2d;			// graphics object of the internal image

    /*----------------------------- constructors ----------------------------*/

    /**
     * Create an empty square shaped screen buffer with the given intial size.
     * On update, the size will be taken from the parent component.
     */
    public ScreenBuffer(JComponent parent) {
	this.parent = parent;
    }

    /*---------------------------- public methods ---------------------------*/

    /**
     * Clears the current buffer (such that it contains no pixels).
     * The size is not changed.
     */
    public void clear() {
		if(g2d!=null){
		    
			g2d.dispose();
			g2d=null;
		    
		    if(image!=null){
			image.flush();
			image=null;
		    }
		    System.gc();
		}
		this.image=parent.createImage(parent.getPreferredSize().width,parent.getPreferredSize().height);
		if(image==null) {
		    System.err.println("Severe Error: Failed to update screen buffer because parent frame is not displayable.");
		    System.exit(1);
		} else {
		    g2d= (Graphics2D) this.image.getGraphics();
		}
    }

    /**
     * Get the buffer as an image. The image can be drawn using Graphics2D.drawImage(),
     * e.g. in the paintComponent method of a JPanel or exported to an image file.
     */
    public Image getImage() {
	return image;
    }

    /**
     * Get the buffer as a graphics object which can be painted on. This way, the
     * ScreenBuffer can be refreshed without calling the refresh() method.
     */
    public Graphics2D getGraphics() {
	return g2d;
    }

//  /**
//  * Draws this ScreenBuffer on top of another screen buffer.
//  */
//  public void drawOnTopOf(ScreenBuffer b) {
//	
//  }

//  /**
//  * Returns whether this buffer is dirty (needs a refresh before being drawn on screen)
//  */
//  public boolean isDirty() {
//  return dirty;
//  }
//	
//  /**
//  * Sets the dirty flag to the given value. Called by the refresh thread to mark that refreshing is on the way.
//  */
//  public void setDirty(boolean d) {
//  dirty = d;
//  }
//	
//  /**
//  * Notifies this screenBuffer that the underlying data has changed and that it needs to refresh itself.
//  */
//  public void scheduleForRefresh() {
//  dirty = true;
//  // do refresh
//  dirty = false;
//  }

//  /*--------------------------- abstract methods --------------------------*/
//	
//  /**
//  * Refreshes this ScreenBuffer (paints to the internal image).
//  */
//  public abstract void refresh();

}
