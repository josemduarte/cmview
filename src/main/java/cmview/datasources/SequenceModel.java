package cmview.datasources;

import java.io.File;
import java.io.IOException;

import owl.core.sequence.Sequence;
import owl.core.structure.AminoAcid;
import owl.core.structure.graphs.RIGraph;
import owl.core.util.FileFormatException;
import cmview.Start;

public class SequenceModel extends Model {

	public SequenceModel(String sequence) throws ModelConstructionError {
		loadFromSequence(sequence);
	}
	
	public SequenceModel(File seqFile) throws ModelConstructionError {
		// load sequence from file
		Sequence seq = new Sequence();
		try {
			seq.readFromFastaFile(seqFile);
			loadFromSequence(seq.getSeq());
		} catch (IOException e) {
			System.err.println("Error while reading from fasta file " + seqFile);
			throw new ModelConstructionError(e);
		} catch (FileFormatException e) {
			System.err.println("Errors in fasta file format of " + seqFile);
			throw new ModelConstructionError(e);
		}
	}
	
	/**
	 * Does the actual loading of this model from sequence
	 * @param sequence the new sequence for this model
	 * @throws ModelConstructionError
	 */
	private void loadFromSequence(String sequence) throws ModelConstructionError {
		// check sequence and strip whitespace
		StringBuffer rawSeq = new StringBuffer(sequence.length());
		for(char c:sequence.toCharArray()) {
			if(!Character.isWhitespace(c)) {
				if(!AminoAcid.isStandardAA(c)) { 
					System.err.println("Invalid character '" + c + "' in sequence");
					throw new ModelConstructionError("Invalid character '" + c + "' in sequence");
				} else {
					rawSeq.append(Character.toUpperCase(c));
				}
			}
		}
		if(rawSeq.length() == 0) {
			System.err.println("No valid characters found in sequence");
			throw new ModelConstructionError("No valid characters found in sequence");
		}
		
		// create new graph from sequence
		this.graph = new RIGraph(rawSeq.toString());
		this.graph.setContactType(Start.DEFAULT_CONTACT_TYPE);
		this.graph.setCutoff(Start.DEFAULT_DISTANCE_CUTOFF);
		this.isGraphWeighted = false;
		this.secondaryStructure = null;
		
		// add diagonal
		for(int i=1; i < this.graph.getFullLength(); i++) {
			this.graph.addEdgeIJ(i, i+1);
		}
		
		// assign a loadedGraphId to this model
		String name = DEFAULT_LOADEDGRAPHID;
		this.loadedGraphID = Start.setLoadedGraphID(name, this);
	}
	
	public SequenceModel(Model mod) {
	    super(mod);
	}
	
	@Override
	public Model copy() {
	    return new SequenceModel(this);
	}
	
	/**
	 * The loading of the graph is implemented in the constructor not in this 
	 * function. This function essentially does not do anything!
	 * @param pdbChainCode pdb chain code of the chain to be loaded (ignored!)
	 * @throws ModelConstructionError
	 */
	@Override
	public void load(String pdbChainCode, int modelSerial) throws ModelConstructionError {
		return;
	}

}
