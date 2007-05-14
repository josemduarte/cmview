/** 
 * A contact map data model.
 * 
 * @author		Henning Stehr
 * Interface: 	Model
 * Package: 	cm2pymol
 * Date:		14/05/2007, last updated: 14/05/2007
 * 
 */
public interface Model {

	/** Returns the size of the data matrix */
	public abstract int getMatrixSize();

	/** Returns the data matrix */
	public abstract int[][] getMatrix();

	/** Returns the pdb code of the underlying structure */
	public abstract String getPDBCode();

	/** Returns the chain code of the underlying structure */
	public abstract String getChainCode();

	/** Returns the name of the temporary pdb file */
	public abstract String getTempPDBFileName();

	/** Returns true if some residues are unobserved */
	public abstract boolean hasUnobservedResidues();

}