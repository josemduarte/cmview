package cmview.datasources;

public class ModelConstructionError extends Exception {

	private static final long serialVersionUID = 1L;

	public ModelConstructionError() {
	}

	public ModelConstructionError(String arg0) {
		super(arg0);
	}

	public ModelConstructionError(Throwable arg0) {
		super(arg0);
	}

	public ModelConstructionError(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
