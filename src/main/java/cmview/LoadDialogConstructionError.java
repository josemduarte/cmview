package cmview;

/**
 * Exception to be thrown whenever the construction of an instance of class 
 * {@link LoadDialog} failes.
 * 
 * @author Lars Petzold
 *
 */
public class LoadDialogConstructionError extends Exception {

	private static final long serialVersionUID = 1L;

	public LoadDialogConstructionError() {
	}

	public LoadDialogConstructionError(String arg0) {
		super(arg0);
	}

	public LoadDialogConstructionError(Throwable arg0) {
		super(arg0);
	}

	public LoadDialogConstructionError(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
}
