package cmview;
import java.awt.*;
import javax.swing.*;
import java.awt.print.*;
import javax.swing.border.Border;

/**
 * Helper class for printing Swing components.
 * Encapsulates the code for printing a Swing component using a print dialog.
 * Usage: Call static printComponent(JComponent) method.
 * Taken from some Java tutorial.
 */
public class PrintUtil implements Printable {
  private Component componentToBePrinted;

  public static void printComponent(Component c) {
    new PrintUtil(c).print();
  }
  
  private PrintUtil(Component componentToBePrinted) {
    this.componentToBePrinted = componentToBePrinted;
  }
  
  private void print() {
    PrinterJob printJob = PrinterJob.getPrinterJob();
    printJob.setPrintable(this);
    if (printJob.printDialog())
      try {
        printJob.print();
      } catch(PrinterException pe) {
        System.out.println("Error printing: " + pe);
      }
  }

  /** Overloaded method to implement printable interface */
  public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
    if (pageIndex > 0) {
      return(NO_SUCH_PAGE);
    } else {
      Graphics2D g2d = (Graphics2D)g;
      g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
      // Set the right size for printing in ContactMapPane
      disableDoubleBuffering(componentToBePrinted);
      if(componentToBePrinted instanceof ContactMapPane) {
    	ContactMapPane p = (ContactMapPane) componentToBePrinted;
        double h = pageFormat.getImageableHeight();
        double w = pageFormat.getImageableWidth();
        Border saveBorder = p.getBorder();
        p.setBorder(null);
    	p.setPrintSize(h,w);
    	p.setPrinting(true);
    	p.paint(g2d);
    	p.setPrinting(false);
    	p.setBorder(saveBorder);
    	p.repaint();
      } else {
    	  componentToBePrinted.paint(g2d);
      }
      enableDoubleBuffering(componentToBePrinted);
      // TODO: Reset the right size for screen output in ContactMapPane
      return(PAGE_EXISTS);
    }
  }

  private static void disableDoubleBuffering(Component c) {
    RepaintManager currentManager = RepaintManager.currentManager(c);
    currentManager.setDoubleBufferingEnabled(false);
  }

  private static void enableDoubleBuffering(Component c) {
    RepaintManager currentManager = RepaintManager.currentManager(c);
    currentManager.setDoubleBufferingEnabled(true);
  }
}