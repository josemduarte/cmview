package cmview;

/**
 * An exception thrown and caught internally by LoadDialog.
 */
public class LoadDialogInputError extends Exception {

	private static final long serialVersionUID = 1L;

	public LoadDialogInputError() {
	}

	public LoadDialogInputError(String arg0) {
		super(arg0);
	}

	public LoadDialogInputError(Throwable arg0) {
		super(arg0);
	}

	public LoadDialogInputError(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
