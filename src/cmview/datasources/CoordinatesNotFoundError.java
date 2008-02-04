package cmview.datasources;

/**
 * Exception to be thrown when the 3D coordinates of a model are required but 
 * cannot be supplied. A pointer to the miss-shaped model is provided.
 */
public class CoordinatesNotFoundError extends Exception {

    private static final long serialVersionUID = 1L;

    Model mod;

    public CoordinatesNotFoundError() {
    }

    public CoordinatesNotFoundError(Model mod) {
	this.mod = mod;
    }

    public CoordinatesNotFoundError(String arg0, Model mod) {
	super(arg0);
	this.mod = mod;
    }

    public CoordinatesNotFoundError(Throwable arg0, Model mod) {
	super(arg0);
	this.mod = mod;
    }

    public CoordinatesNotFoundError(String arg0, Throwable arg1, Model mod) {
	super(arg0, arg1);
	this.mod = mod;
    }
    
    public Model getModel() {
	return mod;
    }
    
    public void setModel(Model mod) {
	this.mod = mod;
    }
}
