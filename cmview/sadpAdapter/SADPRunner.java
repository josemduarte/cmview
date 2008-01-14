package cmview.sadpAdapter;

import java.util.TreeSet;

import cmview.Start;
import cmview.datasources.Model;
import cmview.toolUtils.ToolRunner;
import edu.uci.ics.jung.graph.util.Pair;
import actionTools.Doer;
import actionTools.Retriever;
import actionTools.Runner;

import proteinstructure.Alignment;
import proteinstructure.AlignmentConstructionError;
import proteinstructure.RIGraph;
import proteinstructure.PairwiseAlignmentConverter;

import sadp.ContactMap;
import sadp.ContactMapConstructorError;
import sadp.SADP;

public class SADPRunner extends ToolRunner<SADPResult> {

	Model     inMod1;
	Model     inMod2;
	RIGraph     inG1;
	RIGraph     inG2;
	TreeSet<Pair<Integer>>  matching;
	Alignment ali;
	String    name1;
	String    name2;
	Retriever progressRetriever;
	SADPResult result;

	public SADPRunner() {

	}

	public SADPRunner(Model mod1, Model mod2, SADPResult res) {
		inMod1 = mod1;
		inMod2 = mod2;
		inG1 = inMod1.getGraph();
		inG2 = inMod2.getGraph();
		result = res;
		
		name1 = inMod1.getLoadedGraphID();

		name2 = inMod2.getLoadedGraphID();
	}

	public void setFirstInputModel(Model mod1) {
		inMod1 = mod1;
		inG1   = mod1.getGraph();
	}

	public void setSecondInputModel(Model mod2) {
		inMod2 = mod2;
		inG2   = mod2.getGraph();
	}

	public Model getFirstInputModel() {
		return inMod1;
	}

	public Model getSecondInputModel() {
		return inMod2;
	}

	public void setProgressRetriever(Retriever retr) {
		progressRetriever = retr;
	}

	public Retriever getProgressRetriever() {
		return progressRetriever;
	}

	public SADPResult call() {
		ContactMap cm1,cm2;
		
		try {
			cm1 = new ContactMap(inG1);
		} catch( ContactMapConstructorError e ) {
			System.err.println("Error: Converting first graph into a contact map failed: "+e.getMessage());
			return null;
		}
		try {
			cm2 = new ContactMap(inG2);
		} catch( ContactMapConstructorError e ) {
			System.err.println("Error: Converting first graph into a contact map failed: "+e.getMessage());
			return null;
		}

		SADP sadp = new SADP(cm1,cm2);
		sadp.setProgressInfoRetriever(progressRetriever);
		sadp.setFirstSequence(inG1.getSequence());
		sadp.setSecondSequence(inG2.getSequence());
		sadp.run(); // compute alignment
		matching = sadp.getMatching();

		try {
			ali = makeAlignment(matching);
		} catch(AlignmentConstructionError e) {
			System.err.println("Error: Construction of alignment from mapping failed: " + e.getMessage());
			return null;
		}

		// fill results object
		result.setScore(sadp.getScore());
		result.setCompTime(sadp.getTime());
		result.setNumCommonContacts(sadp.getNumberOfCommonContacts());
		result.setAlignment(getAlignment());
		result.setFirstName(getFirstName());
		result.setSecondName(getSecondName());
				
		if( getActionWhenDone() != null ) {
			Start.threadPool.submit(
					new Runner() {
						public void implRun() {
							((Doer) getActionWhenDone()).doit();
						}
					});
		}
		
		return result;
	}

	/**
	 * Gets alignment. Alignment might consist of pseudo-sequences if no
	 * sequence information can be determined from the input models. The 
	 * sequence tags (i.e., their names) have the following format:<br>
	 * <code>tagX = modX.getPDBCode()+modX.getChainCode()</code><br>
	 * If any information for any input-model is missing it is replaced by
	 * pseudonames, e.g., the aligned sequence of the first input model has the
	 * tag "1_" if both PDB-code and chain-ID is missing.
	 * @return a pairwise sequence alignment
	 * */
	private Alignment getAlignment() {
		return ali;
	}

	/**
	 * Gets the name (tag) of the sequence of the first input model in the 
	 * alignment.
	 * @see getAlignment()
	 */
	private String getFirstName() {
		return name1;
	}

	/**
	 * Gets the name (tag) of the sequence of the second input model in the 
	 * alignment.
	 * @see getAlignment()
	 */
	private String getSecondName() {
		return name2;
	}

	/**
	 * Makes the alignment based an a set of alignment edges.
	 * */
	private Alignment makeAlignment( TreeSet<Pair<Integer>> matching ) throws AlignmentConstructionError {
		Alignment ali;
		if( inG1.getSequence() == null ) {
			if( inG2.getSequence() == null ) {
				ali = new PairwiseAlignmentConverter(matching.iterator(),
						inG1.getFullLength(),inG2.getFullLength(),
						name1,name2,0).getAlignment();
			} else {
				ali = new PairwiseAlignmentConverter(matching.iterator(),
						inG1.getFullLength(),inG2.getSequence(),
						name1,name2,0).getAlignment();
			}
		} else if( inG2.getSequence() == null ) {
			ali = new PairwiseAlignmentConverter(matching.iterator(),
					inG1.getSequence(),inG2.getFullLength(),
					name1,name2,0).getAlignment();	    
		} else {
			ali = new PairwiseAlignmentConverter(matching.iterator(),
					inG1.getSequence(),inG2.getSequence(),
					name1,name2,0).getAlignment();	    
		}
		return ali;
	}


}
