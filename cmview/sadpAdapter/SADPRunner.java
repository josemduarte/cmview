package cmview.sadpAdapter;

import cmview.datasources.Model;
import cmview.toolUtils.ToolRunner;
import actionTools.Doer;
import actionTools.Retriever;

import proteinstructure.Alignment;
import proteinstructure.AlignmentConstructionError;
import proteinstructure.EdgeSet;
import proteinstructure.Graph;
import proteinstructure.PairwiseAlignmentConverter;
import proteinstructure.PairwiseAlignmentGraphConverter;

import sadp.ContactMap;
import sadp.ContactMapConstructorError;
import sadp.SADP;

public class SADPRunner extends ToolRunner {

    Model     inMod1;
    Model     inMod2;
    Graph     inG1;
    Graph     inG2;
    Graph     outG1;
    Graph     outG2;
    EdgeSet   matching;
    Alignment ali;
    String    name1;
    String    name2;
    Retriever progressRetriever;
    
    public SADPRunner() {
	
    }
    
    public SADPRunner(Model mod1, Model mod2) {
	inMod1 = mod1;
	inMod2 = mod2;
    	inG1 = inMod1.getGraph();
	inG2 = inMod2.getGraph();
		
	name1 = (inG1.getPdbCode() == null ? "1" : inG1.getPdbCode())+
	(inG1.getChainCode() == null ? "_" : inG1.getChainCode());

	name2 = (inG2.getPdbCode() == null ? "2" : inG2.getPdbCode())+
	(inG2.getChainCode() == null ? "_" : inG2.getChainCode());
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
        
    public void run() {
	ContactMap cm1,cm2;
	
	try {
	    cm1 = new ContactMap(inG1);
	} catch( ContactMapConstructorError e ) {
	    System.err.println("Error: Converting first graph into a contact map failed: "+e.getMessage());
	    return;
	}
	try {
	    cm2 = new ContactMap(inG2);
	} catch( ContactMapConstructorError e ) {
	    System.err.println("Error: Converting first graph into a contact map failed: "+e.getMessage());
	    return;
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
	    return;
	}
	makeAlignedGraphs(ali);
		
	if( getActionWhenDone() != null ) {
	    ((Doer) getActionWhenDone()).doit();	    
	}
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
    public Alignment getAlignment() {
	return ali;
    }

    /**
     * Gets first aligned graph, i.e., the graph corresponding to the first
     * input model resulting from the alignment of the two input models. The 
     * sequence information in that graph is the same as the sequence in the
     * alignment.
     * @see getAlignment()
     * @see getFirstName();
     */
    public Graph getFirstAlignedGraph() {
	return outG1; 
    }

    /**
     * Gets second aligned graph, i.e., the graph corresponding to the second
     * input model resulting from the alignment of the two input model. The 
     * sequence information in that graph is the same as the sequence in the
     * alignment.
     * @see getAlignment()
     * @see getSecondName();
     */
    public Graph getSecondAlignedGraph() {
	return outG2;
    }
    
    public Model getFirstOutputModel() {
	return getXoutputModel(getFirstInputModel(),getFirstAlignedGraph());
    }
    
    public Model getSecondOutputModel() {
	return getXoutputModel(getSecondInputModel(),getSecondAlignedGraph());
    }
    
    private Model getXoutputModel(Model inMod, Graph g) {
	Model outMod = inMod.copy();
	outMod.setGraph(g);
	return outMod;
    }
    
    /**
     * Gets the name (tag) of the sequence of the first input model in the 
     * alignment.
     * @see getAlignment()
     */
    public String getFirstName() {
	return name1;
    }

    /**
     * Gets the name (tag) of the sequence of the second input model in the 
     * alignment.
     * @see getAlignment()
     */
    public String getSecondName() {
	return name2;
    }
    
    /**
     * Makes the alignment based an a set of alignment edges.
     * */
    private Alignment makeAlignment( EdgeSet matching ) throws AlignmentConstructionError {
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

    /**
     * Makes the two aligned output graph based on an alignment. 
     */
    private void makeAlignedGraphs( Alignment ali ) {
	// sequence information for the gapped graphs is deduced from the alignment
	PairwiseAlignmentGraphConverter pagc = new PairwiseAlignmentGraphConverter(ali,name1,name2,inG1,inG2);
	outG1 = pagc.getFirstGraph();
	outG2 = pagc.getSecondGraph();
    }
    

    
    /**
     * @param args
     */
    public static void main(String[] args) {
	// TODO Auto-generated method stub

    }

}
