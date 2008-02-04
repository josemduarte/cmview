package cmview;

/**
 * Exception to be thrown whenever two contact maps (or even more) violate the 
 * constraint to have the same size.
 * 
 * @author Lars Petzold
 */
public class DifferentContactMapSizeError extends Exception {

    private static final long serialVersionUID = 1L;

    public DifferentContactMapSizeError() {
    }

    public DifferentContactMapSizeError(String arg0) {
	super(arg0);
    }

    public DifferentContactMapSizeError(Throwable arg0) {
	super(arg0);
    }

    public DifferentContactMapSizeError(String arg0, Throwable arg1) {
	super(arg0, arg1);
    }
}