package cmview.datasources;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import cmview.Start;

import owl.core.runners.DsspRunner;
import owl.core.sequence.Sequence;
import owl.core.structure.Pdb;
import owl.core.structure.PdbLoadError;
import owl.core.structure.PdbfilePdb;
import owl.core.structure.features.SecondaryStructure;
import owl.core.structure.graphs.RIGEnsemble;
import owl.core.structure.graphs.RIGraph;
import owl.graphAveraging.GraphAverager;
import owl.graphAveraging.GraphAveragerException;

public class CaspServerPredictionsModel extends Model {

	/*--------------------------- member variables --------------------------*/
	int caspTargetNum;	// stores the number of the target for which
						// predictions are being loaded
	ArrayList<SecondaryStructure> ssList;	// secondary structures annotations
											// of server predictions
	
	/*----------------------------- constructors ----------------------------*/
	
	public CaspServerPredictionsModel(File modelDirectory, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, boolean onlyFirstModels, double consensusSSthresh) throws ModelConstructionError {
		if(!modelDirectory.exists() || !modelDirectory.isDirectory()
									|| !modelDirectory.canRead()) {
			throw new ModelConstructionError("Can not access directory " + modelDirectory);
		}
		caspTargetNum = 0;
		//loadFromDirectory(modelDirectory, edgeType,distCutoff, minSeqSep, maxSeqSep, onlyFirstModels, consensusSSthresh);
		loadUsingRIGEnsembl(modelDirectory, edgeType,distCutoff, minSeqSep, maxSeqSep, onlyFirstModels, consensusSSthresh);
	}
	
	public CaspServerPredictionsModel(Model mod) {
		super(mod);
	}

	/*---------------------------- private methods --------------------------*/
	
	/**
	 * Loads the prediction files and updates the members of this model with the weighted contact map
	 * and associated data.
	 */
	private void loadUsingRIGEnsembl(File modelDirectory, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, boolean onlyFirstModels, double consensusSSthresh) throws ModelConstructionError {
		
		// first scan: find maximal sequence, targte number and load dssp secondary structures
		String seq = loadSequenceFromDirectory(modelDirectory, onlyFirstModels, consensusSSthresh);
		if(seq == null) throw new ModelConstructionError("Could not find sequence information in files.");
		
		// create RIGEnsemble
		System.out.println("Loading prediction files...");
		int numLoaded = 0;
		RIGEnsemble ensemble = new RIGEnsemble(edgeType, distCutoff);
		if(onlyFirstModels) ensemble.loadOnlyFirstModels();
		try {
			numLoaded = ensemble.loadFromFileList(modelDirectory, new Sequence("dummyName", seq));
		} catch (IOException e) {
			System.err.println("Could not read from directory: " + e.getMessage());
			throw new ModelConstructionError(e);
		}
		
		// create graphAverager	
		GraphAverager ga = null;
		try {
		ga = new GraphAverager(ensemble);
		} catch (GraphAveragerException e) {
			System.err.println("Could not create graphAverager: " + e.getMessage());
			throw new ModelConstructionError(e);
		}
		
		// assign model fields
		this.edgeType = edgeType;
		this.distCutoff = distCutoff;
		this.maxSeqSep = maxSeqSep;
		this.pdb = null;
		this.graph = ga.getAverageGraph();
		this.setIsGraphWeighted(true);
		
		// assign consensus secondary structure
		if(ssList != null && ssList.size() > 0) {
			SecondaryStructure consensusSS = SecondaryStructure.getConsensusSecondaryStructure(seq, ssList, consensusSSthresh);
			System.out.println("Consensus Secondary Structure:");
			this.setSecondaryStructure(consensusSS);
		} else {
			System.out.println("Consensus secondary structure not assigned");
		}
		
		// assign a loadedGraphId to this model
		String name = String.format("T%04d_ensemble", caspTargetNum);
		this.loadedGraphID = Start.setLoadedGraphID(name, this);
		
		// print results
		System.out.println("Loaded " + numLoaded + (onlyFirstModels?" first":"") + " models");
	}
	
	/**
	 * Scans all prediction files in the given directory to obtain the full sequence. This is assumed
	 * to be the longest sequence found in any of the files.
	 * Side effects: Sets the caspTargetNum and ssList members from information found in the files.
	 * @param modelDirectory the directory where the predictions are to be loaded from
	 * @param onlyFirstModels whether only predictions assigned as model 1 are to be considered
	 * @return the longest sequence found in any of the considered prediction files
	 */
	private String loadSequenceFromDirectory(File modelDirectory, boolean onlyFirstModels, double consensusSSthresh) {
		// load files
		String newSeq = "";
		File[] files = modelDirectory.listFiles();
		int numLoaded = 0;
		int pdbErrors = 0;
		Pdb pdb;
		ssList = new ArrayList<SecondaryStructure>();	// load also secondary structure
		System.out.println("Loading sequence information...");
		System.out.println("Files in directory: " + files.length);
		for(File f:files) {
			if(f.isFile()) {
				//System.out.println(f);
				// load structure
				pdb = new PdbfilePdb(f.getAbsolutePath());
				if(pdb==null) pdbErrors++; else {
					try {
						String[] chains = pdb.getChains();
						Integer[] models = pdb.getModels();
						if(chains==null || models==null || (onlyFirstModels && models[0] != 1)) continue;	// skip if not model 1
						pdb.load(chains[0], models[0]);	// load first chain and first model
						if(caspTargetNum <= 0) caspTargetNum = pdb.getTargetNum();
						
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
							} //else dsspError = true;
						}
						
					} catch (PdbLoadError e) {
						//System.err.println(e.getMessage());					
						pdbErrors++;
						continue;
					}
				}
				numLoaded++;
					
				// extract meta data
				if(pdb.getObsSequence().length() > newSeq.length()) {
					newSeq = pdb.getObsSequence();
				}
			}
		}
		//System.out.println("Structures loaded: " + numLoaded);
		//System.out.println("Pdb errors: " + pdbErrors);
		System.out.println("Sequence length: " + newSeq.length());
		return (newSeq.length() > 0?newSeq:null);
	}
	
	@SuppressWarnings("unused")
	private void loadFromDirectory(File modelDirectory, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep, boolean onlyFirstModels, double consensusSSthresh) throws ModelConstructionError {

		// load files
		File[] files = modelDirectory.listFiles();
		int numLoaded = 0;
		Pdb pdb;
		RIGraph rig;
		LinkedList<SecondaryStructure> ssList = new LinkedList<SecondaryStructure>();
		int caspModNum = 0;
		//int numRes = 0;
		String sequence = null;
		RIGEnsemble rigs = new RIGEnsemble(edgeType, distCutoff);
		boolean dsspError = false;
		int pdbErrors = 0;
		System.out.println("Files in directory: " + files.length);
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
					//numRes = pdb.getObsLength();
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
				System.out.println(pdb.getObsLength() + " " + pdb.getSequence().length());
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
	
	/*-------------------------- implemented methods ------------------------*/
	
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
	
	/*--------------------------------- main --------------------------------*/
	
	/**
	 * Main method for testing and debugging
	 * @param args
	 * @throws ModelConstructionError 
	 */
	public static void main(String[] args) throws ModelConstructionError {
		//String testDir = "/home/stehr/Desktop/T0387";
		String testDir = "/project/StruPPi/CASP9/server_models/T0515";
		File fileDir = new File(testDir);
		Model mod = new CaspServerPredictionsModel(fileDir, "Cb", 8.0, 0, 0, true, 0.5);
		mod.getSecondaryStructure().print();
	}
	
}
