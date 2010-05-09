package cmview.datasources;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import cmview.Start;

import owl.core.runners.DsspRunner;
import owl.core.structure.Pdb;
import owl.core.structure.PdbLoadError;
import owl.core.structure.PdbfilePdb;
import owl.core.structure.features.SecondaryStructure;
import owl.core.structure.graphs.RIGEnsemble;
import owl.core.structure.graphs.RIGraph;

public class CaspServerPredictionsModel extends Model {

	public CaspServerPredictionsModel(File modelDirectory, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, boolean onlyFirstModels, double consensusSSthresh) throws ModelConstructionError {
		if(!modelDirectory.exists() || !modelDirectory.isDirectory()
									|| !modelDirectory.canRead()) {
			throw new ModelConstructionError("Can not access directory " + modelDirectory);
		}
		
		loadFromDirectory(modelDirectory, edgeType,distCutoff, minSeqSep, maxSeqSep, onlyFirstModels, consensusSSthresh);
	}
	
	public CaspServerPredictionsModel(Model mod) {
		super(mod);
	}

	private void loadFromDirectory(File modelDirectory, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, boolean onlyFirstModels, double consensusSSthresh) throws ModelConstructionError {

		// load files
		File[] files = modelDirectory.listFiles();
		int numLoaded = 0;
		Pdb pdb;
		RIGraph rig;
		LinkedList<SecondaryStructure> ssList = new LinkedList<SecondaryStructure>();
		int caspModNum = 0;
		int numRes = 0;
		String sequence = null;
		RIGEnsemble rigs = new RIGEnsemble(edgeType, distCutoff);
		boolean dsspError = false;
		int pdbErrors = 0;
		for(File f:files) {
			if(f.isFile() && (!onlyFirstModels || f.getName().indexOf("TS1") >= 0)) {
				System.out.println(f);
				// load structure
				pdb = new PdbfilePdb(f.getAbsolutePath());
				try {
					String[] chains = pdb.getChains();
					Integer[] models = pdb.getModels();
					pdb.load(chains[0], models[0]);	// load first chain and first model
				} catch (PdbLoadError e) {
					//System.err.println(e.getMessage());					
					pdbErrors++;
					continue;
				}
					
				// extract meta data
				if(sequence==null || pdb.getObsSequence().length() > sequence.length()) {
					sequence = pdb.getObsSequence();
					numRes = pdb.getObsLength();
				}
				//if(numRes==0) numRes = pdb.getObsLength();
				if(caspModNum==0) caspModNum = pdb.getCaspModelNum();
				
				// extract secondary structure
				if(Start.isDsspAvailable() && consensusSSthresh > 0) {
					SecondaryStructure ss = null;
					try {
						ss = DsspRunner.runDssp(pdb, Start.DSSP_EXECUTABLE, Start.DSSP_PARAMETERS);
					} catch (IOException e) {
						// we don't want to print 50x that DSSP does not work
					}
					if(ss != null) {
						ssList.add(ss);
					} else dsspError = true;
				}
				
				// create graph
				if(sequence.equals(pdb.getObsSequence())) {
					rig = pdb.getRIGraph(edgeType, distCutoff);
					rigs.addRIG(rig);
					rigs.addFileName(f.getName());
					numLoaded++;
					//System.out.print(".");
				} else {
					System.err.printf("Sequence length (%d) is smaller than expected (%d)\n", pdb.getObsSequence().length(), sequence.length());
				}
			}
		}
		if(numLoaded == 0) {
			throw new ModelConstructionError("Could not load any server models");
		}
		System.out.println();
		System.out.println(pdbErrors + " errors encountered when loading server models");
		
		// assign model fields
		this.edgeType = edgeType;
		this.distCutoff = distCutoff;
		this.maxSeqSep = maxSeqSep;
		this.pdb = null;
		this.graph = rigs.getAverageGraph();
		this.setIsGraphWeighted(true);
		
		// assign consensus secondary structure
		this.setSecondaryStructure(SecondaryStructure.getConsensusSecondaryStructure(sequence, ssList, consensusSSthresh));
		
		// assign a loadedGraphId to this model
		String name = String.format("T%04d ensemble",caspModNum);
		this.loadedGraphID = Start.setLoadedGraphID(name, this);
		
		// print results
		System.out.println("Loaded " + numLoaded + (onlyFirstModels?" first":"") + " models");
		if(dsspError) System.err.println("Some errors occured when trying to assign secondary structure with DSSP");
	}
	
	@Override
	public Model copy() {
	    return new CaspServerPredictionsModel(this);
	}
	
	/**
	 * The loading of the graph is implemented in the constructor not in this 
	 * function. This function essentially does not do anything here but is needed
	 * for PDB object based model implementations.
	 * @param pdbChainCode pdb chain code of the chain to be loaded (ignored!)
	 * @throws ModelConstructionError
	 */
	@Override
	public void load(String pdbChainCode, int modelSerial) throws ModelConstructionError {
		return;
	}
	
}
