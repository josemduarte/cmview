package cmview;

/**
 * Exception to be thrown when a non-empty contact selection is required.
 * @author Lars Petzold
 *
 */
public class EmptyContactSelectionError extends Exception {

    private static final long serialVersionUID = 1L;

    public EmptyContactSelectionError() {
    }

    public EmptyContactSelectionError(String arg0) {
	super(arg0);
    }

    public EmptyContactSelectionError(Throwable arg0) {
	super(arg0);
    }

    public EmptyContactSelectionError(String arg0, Throwable arg1) {
	super(arg0, arg1);
    }
}
