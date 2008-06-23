package cmview;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.TreeSet;
import java.util.Set;
import java.util.TreeMap;

import javax.naming.TimeLimitExceededException; //we are using this for our own purposes here (to mark a timeout)

import cmview.datasources.Model;

import edu.uci.ics.jung.graph.util.Pair;

import proteinstructure.*;

/**
 * Encapsulates the code for communication with a PyMol server.   	 
 * TODO: Should be designed such that the visualization frontend can be easily changed (e.g. to JMol). 
 */	
public class PyMolAdaptor {

	/*------------------------------ constants ------------------------------*/
	public static final String 		PYMOLFUNCTIONS_SCRIPT = "cmview.py";	 	// extending pymol with custom functions, previously called graph.py
	public static final String		PYMOL_CALLBACK_FILE = 	"cmview.callback"; 	// file being written by pymol to send messages to this application
	private static final int 		INITIAL_CMDBUFFER_LENGTH = 10000;	
	private static final String 	CMD_END_MARKER = "#end";
	private static final long 		TIMEOUT = 60000;
	private static final File 		CMD_BUFFER_FILE = new File(Start.TEMP_DIR,"CMView_pymol.cmd");
	private static final String		PYMOL_INTERNAL_LOGFILE= "cmview_internal_pymol.log"; // log that pymol itself writes (with -s on startup)
	
	// COLORS
	// colors for triangles, the one with index triangleColorCounter%COLORS.length is chosen
	private static final String[] 	COLORS = {"blue", "red", "yellow", "magenta", "cyan", "tv_blue", "tv_green", "salmon", "warmpink"};
	// colors for structures
	private static final String[] 	ModelColors = {"lightpink", "palegreen"};
	// color for edges in single mode
	private static final String 	SINGLEMODE_EDGE_COLOR = "magenta";
	// colors for edges in pairwise mode
	private static final String 	FIRST_STRUCTURE_EDGE_COLOR  = "magenta";
	private static final String 	SECOND_STRUCTURE_EDGE_COLOR = "green";
	private static final String 	COMMON_EDGE_COLOR = "yellow";
	// color for matching residues from alignment 
	private static final String 	MATCHING_RESIDUES_COLOR = "blue";
	// color for single edge between 2 residues (contact or not) as distance object
	private static final String 	SINGLE_EDGE_COLOR = "orange";		
	
	// the default atom for distance objects, it must be an atom of the backbone, i.e. only possible values one could use would be CA, C, N, O
	private static final String 	DEFAULT_ATOM = "CA";
	
	/*--------------------------- member variables --------------------------*/

	private PrintWriter Out;
	private boolean connected; 		// indicates whether a connection to pymol is established
	private StringWriter cmdBuffer; //TODO use StringBuffer instead?
	private int cmdCounter;			// auto-increment the command number
	private int selCounter;			// auto-increment the selection number
	private PrintWriter log;
	private File callbackFile;
	private int triangleColorCounter;   // auto-increment counter for the COLORS array (triangleColorCounter%COLORS.length)
	// Sphere settings for PyMol:
	//private int SPHERE_SCALE = 2; // sphere scale
	
	/*----------------------------- constructors ----------------------------*/

	/**
	 * Constructs a new PyMolAdaptor with given pymolInput PrintWriter
	 * @param pymolInput
	 */
	public PyMolAdaptor(PrintWriter log) {
		this.Out = null;
		this.connected = false;
		this.log = log;
		this.cmdBuffer = new StringWriter(INITIAL_CMDBUFFER_LENGTH);
		this.cmdCounter = 0;
		this.selCounter = 0;
		this.triangleColorCounter = 0;
		this.callbackFile = new File(Start.TEMP_DIR,PYMOL_CALLBACK_FILE);
	}

	/*---------------------------- private methods --------------------------*/
	
	/**
	 * Draws a single edge between the CA atoms of the given residues in the given objects.
	 * @param distObjName the name of the distance object to be created or to add to
	 * @param mod1  first structure
	 * @param mod2  second structure
	 * @param cont  the pair of residue serials for which we want to draw an edge 
	 */
	private void drawSingleEdge(String distObjName, Model mod1, Model mod2, Pair<Integer> cont) {
		String atom1 = getAtom(mod1, cont.getFirst());
		String atom2 = getAtom(mod2, cont.getSecond());
		
		if (atom1==null || atom2==null) { //see getAtom(Model,int)
			return;
		}
		
		sendCommand("distance " + distObjName + ", "
					+ mod1.getLoadedGraphID() + " and resi " + cont.getFirst()  + " and name "+atom1+", " 
					+ mod2.getLoadedGraphID() + " and resi " + cont.getSecond() + " and name "+atom2); 
	}
	
	/**
	 * Gets the atom from which edges will be drawn
	 * @param mod
	 * @param resser
	 * @return the atom name or null if no atom corresponds to this contact 
	 * definition and residue type 
	 */
	private String getAtom(Model mod, int resser) {
		Set<String> atomSet = AAinfo.getAtomsForCTAndRes(mod.getContactType(),mod.getResType(resser));
		if (atomSet.size()==0) { 
			// This shouldn't happen: only will happen if a contact is assigned wrongly e.g. a Cg contact for a 
			// GLY residue (and this happens for example in an average graph)
			// It will also happen when drawing absent contacts, e.g. first CM is Cb and second Cg; 
			// if we are drawing a contact present in first between ALA-ASP, then ALA has no Cg atoms and we would be 
			// in this case: atomSet.size()==0
			// Thus we return null so then we can catch this case in drawSingleEdge not to draw anything 
			return null;
		}
		else if (atomSet.size()==1) { // cases Ca, Cb, C, Cg, SC for ALA, SC_CAGLY for GLY and ALA
			return atomSet.iterator().next(); // we return the one and only member
		} 
		else { // remaining cases ALL, BB, SC, SC_CAGLY
			if (mod.getContactType().startsWith("SC")) { // case SC or SC_CAGLY
				return "CB";
			} else { // i.e. for ALL, BB and any others
				return DEFAULT_ATOM;
			}
		}
	}
	
	/** 
	 * Create a selection from a set of residues.
	 * @param selObjName the name of the selection to be created
	 * @param structureId the structureId for which residues will be selected
	 * @param residues the set of residues in the given structure
	 */
	private void createSelectionObject(String selObjName, String structureId, TreeSet<Integer> residues) {	
		String cmdStr = "select " + selObjName + ", (" + structureId + " and resi " +  Interval.createSelectionString(residues) + ")";
		sendCommand(cmdStr);
	}
	
	/** 
	 * Create a selection from two sets of residues in two structures.
	 * @param selObjName the name of the selection to be created
	 * @param structureId1 the first structureId for which residues will be selected
	 * @param structureId2 the second structureId for which residues will be selected
	 * @param residues1 the set of residues in structure1 to be selected
	 * @param residues1 the set of residues in structure2 to be selected
	 */
	private void createSelectionObject(String selObjName, String structureId1, TreeSet<Integer> residues1, String structureId2, TreeSet<Integer> residues2) {	
		String cmdStr = "select " + selObjName + ", (" + structureId1 + " and resi " + Interval.createSelectionString(residues1) + ")"
											 + " or (" + structureId2 + " and resi " + Interval.createSelectionString(residues2) + ")";
		sendCommand(cmdStr);
	}
	
	/*
	 * The following method has become useless, since the sphere are being drawn as CGOs. It's still kept for reference.
	 *   
	 /** 
	 * Create a selection from a set of residues and alpha carbons within them.
	 * @param selObjName the name of the selection to be created
	 * @param structureId the structureId for which residues will be selected
	 * @param residues the set of residues in the given structure
	 * TODO: instead of only alpha-Cs, can this be extended to any atom within the residues?
	 */
	/*private void createSelectionObjectWithContactAtoms(String selObjName, Model mod1, TreeSet<Integer> residues1, Model mod2, TreeSet<Integer> residues2) {
		
		String atom1 = getAtom(mod1, residues1.first());
		String atom2 = getAtom(mod2, residues2.first());
		
		if (atom1==null || atom2==null) { //see getAtom(Model,int)
			return;
		}
		
		String cmdStr = "select " + selObjName + ", (" + mod1.getLoadedGraphID() + " and resi " + Interval.createSelectionString(residues1) + " and name " 
			+ atom1 + ")" + " or (" + mod2.getLoadedGraphID() + " and resi " + Interval.createSelectionString(residues2) + " and name " + atom2 + ")";
		sendCommand(cmdStr);
	}*/
	
	/**
	 * Reads the given file until a line with tag is found before given timeOut 
	 * is reached. To be called from {@link #flush()} so that it is guaranteed that 
	 * command buffer file is fully written before loading from it or that PyMol is 
	 * finished executing commands before continuing with others.
	 * @param file
	 * @param tag the tag we want to find in the file
	 * @param timeOut the timeout in milliseconds
	 * @throws IOException if file can't be read
	 * @throws TimeLimitExceededException when timeOut is reached before finding 
	 * the tag in file
	 * @see {@link #flush()}
	 */
	private void waitForTagInFile(File file, String tag, long timeOut) throws IOException, TimeLimitExceededException {
		long startTime = System.currentTimeMillis();

		while (System.currentTimeMillis()<startTime+timeOut) {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line=br.readLine())!=null) {
				if (line.equals(tag)) {
					br.close();
					return;
				}
			}
			br.close();
		}
		throw new TimeLimitExceededException("Timeout reached while waiting for tag "+tag+" in file "+file.getAbsolutePath());
	}

	/** 
	 * Writes command to the command buffer that will be sent to PyMol on 
	 * next call of {@link #flush()} 
	 * @param cmd the PyMol command
	 */
	private void sendCommand(String cmd) {
		if (!this.connected) {
			return;
		}
		
		cmdBuffer.write(cmd + "\n");
	}
	
	/**
	 * Flushes the command buffer so that it is sent to PyMol.
	 * In command buffer mode, this has to be called so that sendCommands are actually executed.
	 */
	private void flush() {
		if(!this.connected) {
			return;
		}
		
		try {
			cmdCounter++;
			// write data from buffer to file
			FileWriter fWriter = new FileWriter(CMD_BUFFER_FILE);
			cmdBuffer.flush();
			fWriter.write(cmdBuffer.toString());
			fWriter.write("callback "+callbackFile.getAbsolutePath() + ", " + cmdCounter+"\n");
			fWriter.write(CMD_END_MARKER+cmdCounter);
			fWriter.close();

		} catch (IOException e1) {
			System.err.println("Cannot write to command buffer file: "+e1.getMessage());
			return;
		} finally {
			log.println(cmdBuffer.toString());
			log.flush();
			cmdBuffer = new StringWriter(INITIAL_CMDBUFFER_LENGTH);
		}

		try {
			waitForTagInFile(CMD_BUFFER_FILE, CMD_END_MARKER+cmdCounter, TIMEOUT);	
			Out.println("@" + CMD_BUFFER_FILE.getAbsolutePath());
			if (Out.checkError()) {			
				System.err.println("Couldn't send command to PyMol. Connection is lost!");
				this.connected = false;
				return;
			}
			waitForTagInFile(callbackFile, Integer.toString(cmdCounter), TIMEOUT);

		} catch (IOException e) {
			System.err.println("Error while reading from command buffer or callback file: "+e.getMessage());
			return;
		} catch (TimeLimitExceededException e1) {
			System.err.println(e1.getMessage());
			return;
		}
	}

	/**
	 * Increments the selection counter and returns it.
	 * This is used to make selection names unique.
	 */
	private int getNextSelNum() {
		return ++selCounter;
	}
	
	/**
	 * Draws a set of edges in the 3D viewer and creates corresponding selections.
	 * Edges are implemented as distances drawn between the residues of the 
	 * first and the second selection. The given set of pairs of integers 
	 * defines the residues to be connected, whereas <code>getFirst()</code> 
	 * always yields the residue indices in the first selection and 
	 * <code>getSecond()</code> those in the second.
	 * @param mod1 the first structure
	 * @param mod2 the second structure
	 * @param edgeSelName  name of the edge selection to be created
	 * @param nodeSelName  name of the node selection consisting of all 
	 *  residues incident to the contacts 
	 * @param edgeColor  name of the color of the edges
	 * @param selContacts  set of pairs of residues to be connected
	 * @param dash true for dashed edges, false for solid edges
	 */
	private void drawEdges(Model mod1, Model mod2, String edgeSelName, String nodeSelName, 
								String edgeColor, IntPairSet selContacts, boolean dash) {

		// if no contacts in selection do nothing
		if (selContacts.size()== 0) {
			return; 
		}

		TreeSet<Integer> firstResidues  = new TreeSet<Integer>();
		TreeSet<Integer> secondResidues = new TreeSet<Integer>();
		
		// send each contact as a single command to PyMol
		for (Pair<Integer> cont:selContacts){ 
			// draws an edge between the selected residues
			this.drawSingleEdge(edgeSelName, mod1, mod2, cont);
			firstResidues.add(cont.getFirst());
			secondResidues.add(cont.getSecond());
		}


		// hide distance labels
		sendCommand("hide labels, " + edgeSelName);

		// color distances
		this.sendCommand("color " + edgeColor + "," + edgeSelName);

		if (dash ==true){
			// setting the dashed lines for present and absent distinction
			sendCommand("set dash_gap, 0.5, "    + edgeSelName);
			sendCommand("set dash_length, 0.5, " + edgeSelName);
		} else { 
			// fixing the side chain problem
			// side chains only occur in case of common contacts
			sendCommand("hide lines,  " + edgeSelName);
		}
		
		// create selection of nodes incident to the contacts
		createSelectionObject(nodeSelName, mod1.getLoadedGraphID(), firstResidues, mod2.getLoadedGraphID(), secondResidues);
		
		sendCommand("disable " + nodeSelName);
		
	}
	
	/**
	 * Draws a set of spheres in the 3D viewer and creates corresponding selections.
	 * The sphere centers are the atoms in contact, sphere radii are equal to the distance
	 * cut-off in use. 
	 * @param mod1
	 * @param mod2
	 * @param nodeCentralSelName
	 * @param residuePair
	 */
	private void drawSpheres(Model mod1, Model mod2, String nodeCentralSelName, Pair<Integer> residuePair) {
		
		/*String SPHERE_COLOR = "magenta"; // sphere color
		double SPHERE_TRANSPARENCY = 0.5; // sphere transparency
		*/
		
		if (residuePair.isEmpty()){
			return;
		}
		
		TreeSet<Integer> firstResidues  = new TreeSet<Integer>();
		TreeSet<Integer> secondResidues = new TreeSet<Integer>();
		
		firstResidues.add(residuePair.getFirst());
		secondResidues.add(residuePair.getSecond());
		
		String atom1 = getAtom(mod1, firstResidues.first());
		String atom2 = getAtom(mod2, secondResidues.first());
		
		/*// create selection of nodes incident to the contacts
		createSelectionObjectWithContactAtoms(nodeCentralSelName, mod1, firstResidues, mod2, secondResidues);
		sendCommand("disable " + nodeCentralSelName);
	
		sendCommand("set sphere_color, " + SPHERE_COLOR + "\n"
				+ "set sphere_transparency=" + SPHERE_TRANSPARENCY);
		sendCommand("alter " + nodeCentralSelName + " , vdw=" + mod1.getDistanceCutoff() + "\n"
				+ "rebuild");
		sendCommand("show spheres, " + nodeCentralSelName);
		*/
		
		String argumentforSphere1 = mod1.getLoadedGraphID() + " and resi " + Interval.createSelectionString(firstResidues) + " and name " + atom1 ;
		String argumentforSphere2 = mod1.getLoadedGraphID() + " and resi " + Interval.createSelectionString(secondResidues) + " and name " + atom2 ;
		sendCommand("sphere('" + nodeCentralSelName + "res1" + "', '" + argumentforSphere1 + "', " + mod1.getDistanceCutoff() + ", 'magenta', " + 0.5 + ")");
		sendCommand("sphere('" + nodeCentralSelName + "res2" + "', '" + argumentforSphere2 + "', " + mod1.getDistanceCutoff() + ", 'magenta', " + 0.5 + ")");
	}
	/**
	 * Same as above, only arguments are different
	 * @param mod1
	 * @param mod2
	 * @param nodeCentralSelName
	 * @param residuePair
	 */
	private void drawSpheres(Model mod1, Model mod2, String nodeCentralSelName, IntPairSet residuePair) {
		
		/*String SPHERE_COLOR = "magenta"; // sphere color
		double SPHERE_TRANSPARENCY = 0.5; // sphere transparency*/

		// if no contacts in selection do nothing
		if (residuePair.size()== 0) {
			return; 
		}
		
		if (residuePair.isEmpty()){
			return;
		}

		TreeSet<Integer> firstResidues  = new TreeSet<Integer>();
		TreeSet<Integer> secondResidues = new TreeSet<Integer>();
		
		// send each contact as a single command to PyMol -> this has been directly copied from
		// the method drawEdges, since it works. In case of spheres, there is actually a single
		// contact being passed. No harm done in doing this, since the following loop will run only once.
		int i,j;
		
		for (Pair<Integer> cont:residuePair){ 
			i = cont.getFirst();
			j = cont.getSecond();
						
			//Instead of writing a separate method for drawing spheres (which, at the moment seems
			// unnecessary), sphere drawing code is directly written here.
			firstResidues.add(i);
			secondResidues.add(j);
			
			String atom1 = getAtom(mod1, firstResidues.first());
			String atom2 = getAtom(mod2, secondResidues.first());
			String argumentforSphere1 = mod1.getLoadedGraphID() + " and resi " + Interval.createSelectionString(firstResidues) + " and name " + atom1 ;
			String argumentforSphere2 = mod1.getLoadedGraphID() + " and resi " + Interval.createSelectionString(secondResidues) + " and name " + atom2 ;
			sendCommand("sphere('" + nodeCentralSelName + "res1" + "', '" + argumentforSphere1 + "', " + mod1.getDistanceCutoff() + ", 'magenta', " + 0.5 + ")");
			sendCommand("sphere('" + nodeCentralSelName + "res2" + "', '" + argumentforSphere2 + "', " + mod1.getDistanceCutoff() + ", 'magenta', " + 0.5 + ")");
		}
		
		/*// create selection of nodes incident to the contacts
		createSelectionObjectWithContactAtoms(nodeCentralSelName, mod1, firstResidues, mod2, secondResidues);
		sendCommand("disable " + nodeCentralSelName);
	
		sendCommand("set sphere_color, " + SPHERE_COLOR + "\n"
				+ "set sphere_transparency=" + SPHERE_TRANSPARENCY);
		sendCommand("alter " + nodeCentralSelName + " , vdw=" + mod1.getDistanceCutoff() + "\n"
				+ "rebuild");
		sendCommand("show spheres, " + nodeCentralSelName);*/	
	}
	/**
	 * Creates or updates a group object.
  	 * Note, that whenever an argument is null all subsequent arguments are 
  	 * implicitely disregarded!
	 * @param groupName  the name of the group
	 * @param members  string of whitespace-separated list of objects to be 
	 *  grouped together
	 * @param action  grouping action (PyMol v1 supports: add, remove, open, 
	 *  close, toggle, auto, ungroup, empty, purge, excise). See PyMol docu 
	 *  for further details!
	 */
	private void group(String groupName, String members, String action) {
		
		// nothing to do if groupName is null!
		if( groupName == null ) {
			return;
		}
		
		// send command to PyMol
		if( members == null ) {
			sendCommand("group " + groupName);
		} else if ( action == null ) {
			sendCommand("group " + groupName + ", " + members);
		} else {
			sendCommand("group " + groupName + ", " + members + ", " + action);
		}
		
	}
	
	/*---------------------------- public methods ---------------------------*/

	/**
	 * Returns whether a connection of this Adaptor to the server had been already successfully established
	 * (and was not subsequently lost).
	 * @return true if a connection is established, false otherwise
	 */
	public boolean isConnected() {
		return this.connected;
	}

	/**
	 * Runs external pymol executable if possible.
	 * @throws IOException if execution of PyMol fails
	 */
	public void startup() throws IOException {
	
			System.out.println("Starting PyMol...");
			File f = new File(Start.PYMOL_EXECUTABLE);
			if(!f.exists()) {
				System.err.println(Start.PYMOL_EXECUTABLE + " does not exist.");
				// try to start pymol anyways because on Mac f.exists() returns false even though the file is there
			}
			File pymolInternalLogFile = new File(Start.TEMP_DIR,PYMOL_INTERNAL_LOGFILE);
			pymolInternalLogFile.deleteOnExit();
			Process pymolProcess = Runtime.getRuntime().exec(f.getCanonicalPath() + " " + Start.PYMOL_PARAMETERS + " -s " + pymolInternalLogFile.getAbsolutePath());			
	
			// we send the stdout/stderr stream to new threads to avoid hanging of pymol
			new StreamGobbler("pymol_stdout", pymolProcess.getInputStream()).start();
			new StreamGobbler("pymol_stderr", pymolProcess.getErrorStream()).start();
			
			this.Out = new PrintWriter(pymolProcess.getOutputStream());
			this.connected = true;
	}
	
	/**
	 * Sends some inital set-up commands after a connection has been 
	 * successfully established.
	 */
	public void initialize() {
		sendCommand("run "+Start.getResourcePath(PYMOLFUNCTIONS_SCRIPT));
		sendCommand("set dash_gap, 0");
		sendCommand("set dash_width, 1.5");
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();		
	}
	
	/**
	 * Shuts down the external viewer instance and releases all resources of this Adaptor.
	 */
	public void shutdown() {
		Out.println("quit");
		Out.close();
	}
	
	/**
	 * Send command to the pymol server to load a structure with the given name
	 * from the given temporary pdb file.
	 * @param fileName
	 * @param structureID
	 * @param secondModel true if the structure we are loading is the second 
	 * model (pairwise mode)
	 */
	public void loadStructure(String fileName, String structureID, boolean secondModel) {

		sendCommand("load " + fileName + ", " + structureID);
		sendCommand("hide lines");
		sendCommand("show cartoon");		

		if (secondModel){
			// color second model green
			sendCommand("color " + ModelColors[1] + ", " + structureID);
		}else {
			// color main model red
			sendCommand("disable all");
			sendCommand("enable " + structureID);
			sendCommand("color " + ModelColors[0] + ", " + structureID);
			sendCommand("orient "+structureID);
		}
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
		
	}

	/**
	 * Aligns two structures employing PyMol's <code>align</code> command.
	 * PyMol internally makes a sequence alignment of both structures and
	 * tries to find the superposition with minimal RMSD of the structures
	 * given this alignment. Please note, that one major drawback of using
	 * this function is that the superposition relies on a sequence 
	 * alignment to find corresponding residues which might yield quite 
	 * nonsatisfying results for proteins with a rather low sequence 
	 * identity!
	 * 
	 * @param structureId1 the structure id of the first structure
	 * @param structureId2 the structure id of the second structure
	 * 
	 * @see pairFitSuperposition()
	 */
	public void alignStructures(String structureId1,  String structureId2){
		sendCommand("align " + structureId2 + "," + structureId1);
		sendCommand("zoom " + structureId1+ " or " + structureId2);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}

	/**
	 * Superimpose two structures identified by their structureIDs, using PyMol's
	 * <code>pair_fit</code> command which does a C-alpha minimum RMSD fit on a
	 * given set of residues.
	 * @param structureID1 the structure id of the first (reference) structure
	 * @param structureID2 the structure id of the second structure
	 * @param chunksFirst an interval set of residues in the first structure
	 * @param chunksSecond an interval set of residues in the second structure
	 */
	public void pairFitSuperposition(String structureID1, String structureID2, IntervalSet chunksFirst, IntervalSet chunksSecond) {
		// put edge set into the recommended string format
		StringBuffer chunkSeq = new StringBuffer(chunksFirst.size()*2);
		for( Interval e : chunksFirst ) {
			chunkSeq.append(e.beg + "-" + e.end + "+");
		}
		// delete trailing '+' character
		chunkSeq.deleteCharAt(chunkSeq.length()-1);

		// append sequence of chunks to the command line along with the
		// atom types to be considered for superpositioning (here: CA)
		String commandLine = structureID1 + "///" + chunkSeq + "/CA";

		chunkSeq.delete(0, chunkSeq.length());
		for( Interval e : chunksSecond ) {
			chunkSeq.append(e.beg + "-" + e.end + "+");
		}
		chunkSeq.deleteCharAt(chunkSeq.length()-1);

		commandLine = "pair_fit " + structureID2 + "///" + chunkSeq + "/CA" + ", " + commandLine ;

		sendCommand(commandLine);
		sendCommand("zoom " + structureID1+ " or " + structureID2);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}

	/**
	 * Draw triangle objects in PyMol for a given common neighbourhood.
	 * @param structureId the structure id of the object in PyMol
	 * @param commonNbh the common neighbourhood object for which triangles will be drawn
	 */
	public void showTriangles(Model mod, RIGCommonNbhood commonNbh){
		
		String structureId = mod.getLoadedGraphID(); 
		
		String topLevelGroup    = "Sel" + getNextSelNum();

		String triangleBaseName = topLevelGroup + "_" + structureId + "_NbhTri";
		String nodeSel      = triangleBaseName + "_Nodes";
		
		int trinum=1;
		TreeSet<Integer> residues = new TreeSet<Integer>();
		int i = commonNbh.getFirstNode().getResidueSerial();
		int j = commonNbh.getSecondNode().getResidueSerial();
		residues.add(i);
		residues.add(j);
		
		String contact_type = mod.getContactType();
		String curTriangleName = "";
		TreeSet<String>  triangleSelNames = new TreeSet<String>();

		for (int k:commonNbh.keySet()){
			int triangleColorIndex = triangleColorCounter%COLORS.length;
			curTriangleName = triangleBaseName + trinum;
			sendCommand("triangle('"+ curTriangleName + "', "+ i+ ", "+j +", "+k +", '" + contact_type + "', '" + COLORS[triangleColorIndex] +"', " + 0.7+")");
			trinum++;
			residues.add(k);
			triangleSelNames.add(curTriangleName);
			triangleColorCounter++;
		}
		
		sendCommand("zoom");
		
		createSelectionObject(nodeSel, structureId, residues);
		
		String groupMembers = "";
		for( String s : triangleSelNames ) {
			groupMembers += s + " ";
		}
		groupMembers += nodeSel;		
		sendCommand("show sticks, " + nodeSel);
		sendCommand("spectrum count, rainbow_cycle, " + nodeSel);
		group(topLevelGroup, groupMembers, null);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();	
	}

	/**
	 * Shows matching residues from the 2 structures as blue edges
	 * @param mod1
	 * @param mod2
	 * @param selContacts set of pairs: first member of the pair corresponds to 
	 * residue in structure1, second member to residue in structure2
	 */
	public void showMatchingResidues(Model mod1, Model mod2, IntPairSet selContacts) {
		// prepare selection names
		String topLevelGroup = "Sel" + getNextSelNum();
		String edgeSel       = topLevelGroup + "_" + mod1.getLoadedGraphID() + "_" + mod2.getLoadedGraphID() + "_AliEdges";
		String nodeSel       = edgeSel + "_Nodes";	
		
		drawEdges(mod1, mod2, edgeSel, nodeSel, MATCHING_RESIDUES_COLOR, selContacts, false);
		
		// group selection in topLevelGroup
		group(topLevelGroup, edgeSel + " " + nodeSel, null);

		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}
	
	/**
	 * Shows edges from a selection in single contact map mode
	 * @param mod
	 * @param selContacts
	 */
	public void showEdgesSingleMode(Model mod, IntPairSet selContacts) {
		String topLevelGroup  = "Sel" + getNextSelNum();
		String edgeSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Cont";
		String nodeSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Nodes";
		
		drawEdges(mod, mod, edgeSel, nodeSel, SINGLEMODE_EDGE_COLOR, selContacts, false);
		
		// group selection in topLevelGroup
		group(topLevelGroup,  edgeSel + " " + nodeSel, null);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}

	/**
	 * Shows spheres which are the end-points of contacts
	 * @param model
	 * @param residuePair
	 */
	public void showSpheres(Model mod, Pair<Integer> residuePair) {
		String topLevelGroup  = "Sel" + getNextSelNum();
		//String edgeSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Cont";
		//String nodeSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Nodes";
		String nodeCentralSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Nodes_central";
		
		sendCommand("hide spheres");
		
		drawSpheres(mod, mod, nodeCentralSel, residuePair);
		// group selection in topLevelGroup
		group(topLevelGroup,  /*edgeSel + " " + nodeSel + " " +*/ nodeCentralSel + "res1 " + nodeCentralSel + "res2", null);
				
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}
	/**
	 * Same as above, the arguments are different
	 * @param mod
	 * @param residuePair
	 */
	public void showSpheres(Model mod, IntPairSet residuePair) {
		String topLevelGroup  = "Sel" + getNextSelNum();
		//String edgeSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Cont";
		//String nodeSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Nodes";
		String nodeCentralSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Nodes_central";
		
		//sendCommand("hide spheres");
		
		drawSpheres(mod, mod, nodeCentralSel, residuePair);
		// group selection in topLevelGroup
		group(topLevelGroup,  /*edgeSel + " " + nodeSel + " " + */nodeCentralSel + "res1 " + nodeCentralSel + "res2", null);
				
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}

	/**
	 * @param Model mod. List of all the second shell residues.
	 * @return void. Ends up drawing sticks for all the second shell residues. 
	 */
	public void showSecShell(Model mod, IntPairSet secondshell){
		String topLevelGroup  = "Sel" + getNextSelNum();
		String edgeSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Cont_SecondShell";
		String nodeSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Nodes_SecondShell";
		
		drawEdges(mod, mod, edgeSel, nodeSel, SINGLEMODE_EDGE_COLOR, secondshell, false);
		
		// group selection in topLevelGroup
		group(topLevelGroup,  edgeSel + " " + nodeSel, null);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();		
	}
	/**
	 * @param Model mod and List of all first shell nbrs.
	 * @return void. Ends up depicting all those first shell nbrs of a residue, which are nbrs of each other
	 */
	public void showShellRels(Model mod, IntPairSet firstshellrels){
		String topLevelGroup  = "Sel" + getNextSelNum();
		String edgeSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Cont_firstShell_Rels";
		String nodeSel        = topLevelGroup + "_" + mod.getLoadedGraphID() + "_Nodes_firstShell_Rels";
		
		drawEdges(mod, mod, edgeSel, nodeSel, SINGLEMODE_EDGE_COLOR, firstshellrels, false);
		
		// group selection in topLevelGroup
		group(topLevelGroup,  edgeSel + " " + nodeSel, null);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}
	/**
	 * Shows edges from selection in pairwise comparison mode
	 * @param mod1
	 * @param mod2
	 * @param selMap a Map of ContactSelSet to an array of IntPairSet of size 2:
	 * ContactSelSet.COMMON:
	 *	 FIRST -> common contacts in first model 
	 *   SECOND -> "" second ""                  
	 * ContactSelSet.ONLY_FIRST:
	 *   FIRST -> contacts only pres. in first model 
	 *   SECOND -> -> draw dashed green lines
	 * ContactSelSet.ONLY_SECOND:
	 *   SECOND -> contacts only pres. in sec. model 
	 *   FIRST -> -> draw dashed red lines
	 */
	public void showEdgesPairwiseMode(Model mod1, Model mod2, TreeMap<ContactMapPane.ContactSelSet, IntPairSet[]> selMap)	{
		// COMMON:
		//   FIRST -> common contacts in first model -> draw solid yellow lines
		//   SECOND -> "" second ""                  -> draw solid yellow lines
		// ONLY_FIRST:
		//   FIRST -> contacts only pres. in first model -> draw solid red lines
		//   SECOND -> -> draw dashed green lines
		// ONLY_SECOND:
		//   SECOND -> contacts only pres. in sec. model -> draw solid green lines
		//   FIRST -> -> draw dashed red lines

		// groups and edge selection names, this naming convention yields 
		// the following grouping tree in PyMol:
		// topLevelGroup
		//   |--firstModGroup
		//   |    |--presFirstEdgeSel
		//   |    |--presFirstNodeSel
		//   |    |--absFirstEdgeSel
		//   |     `-absFirstNodeSel
		//    `-secondModGroup
		//        |--...
		//        ...
		
		String structureID1 = mod1.getLoadedGraphID();
		String structureID2 = mod2.getLoadedGraphID();
		
		String topLevelGroup     = "Sel" + getNextSelNum();
		String firstModGroup     = topLevelGroup + "_" + structureID1;			
		String secondModGroup    = topLevelGroup + "_" + structureID2;
		String presFirstEdgeSel  = firstModGroup + "_PresCont";
		String presFirstNodeSel  = firstModGroup + "_PresCont_Nodes";
		String absFirstEdgeSel   = firstModGroup + "_AbsCont";
		String absFirstNodeSel   = firstModGroup + "_AbsCont_Nodes";
		String commonFirstEdgeSel = firstModGroup + "_CommonCont";
		String commonFirstNodeSel = firstModGroup + "_CommonCont_Nodes";

		String presSecondEdgeSel = secondModGroup + "_PresCont";
		String presSecondNodeSel = secondModGroup + "_PresCont_Nodes";
		String absSecondEdgeSel  = secondModGroup + "_AbsCont";
		String absSecondNodeSel  = secondModGroup + "_AbsCont_Nodes";
		String commonSecondEdgeSel = secondModGroup + "_CommonCont";
		String commonSecondNodeSel = secondModGroup + "_CommonCont_Nodes";

		// send common contacts in the first and second model. It suffices 
		// to check size only for one set as both are supposed to be of 
		// same size.			
		if( selMap.get(ContactMapPane.ContactSelSet.COMMON)[ContactMapPane.FIRST].size()  != 0 ) {				
			// send common contacts to the object corresponding to the first model
			drawEdges(mod1, mod1, commonFirstEdgeSel, commonFirstNodeSel, 
					COMMON_EDGE_COLOR, 
					selMap.get(ContactMapPane.ContactSelSet.COMMON)[ContactMapPane.FIRST], 
					false);

			// send common contacts to the object corresponding to the second model
			drawEdges(mod2, mod2, commonSecondEdgeSel, commonSecondNodeSel,
					COMMON_EDGE_COLOR, 
					selMap.get(ContactMapPane.ContactSelSet.COMMON)[ContactMapPane.SECOND], 
					false);

			// group first and second structure selection
			group(firstModGroup,  commonFirstEdgeSel,  null);
			group(firstModGroup,  commonFirstNodeSel,  null);
			group(secondModGroup, commonSecondEdgeSel, null);
			group(secondModGroup, commonSecondNodeSel, null);

			// and group everything in the topLevelGroup representing the 
			// whole selection
			group(topLevelGroup, firstModGroup,  null);
			group(topLevelGroup, secondModGroup, null);
			
			this.flush();
		}

		// send contacts present in the first and absent in the second model
		if( selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.FIRST].size() != 0 ) {

			// draw true contacts being present in the first model between 
			// the residues of the first model
			drawEdges(mod1, mod1, presFirstEdgeSel, presFirstNodeSel,
					FIRST_STRUCTURE_EDGE_COLOR,
					selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.FIRST], 
					false);

			// group selection of present contact in the first structure
			group(firstModGroup, presFirstEdgeSel, null);
			group(firstModGroup, presFirstNodeSel, null);
			group(topLevelGroup, firstModGroup,    null);

			this.flush();
			
			if( selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.SECOND].size() != 0 ) {
				// draw "contact" being absent in the second model but 
				// NOT in the first one between the residues of the second 
				// model
				drawEdges(mod2, mod2, absSecondEdgeSel, absSecondNodeSel,
						SECOND_STRUCTURE_EDGE_COLOR,
						selMap.get(ContactMapPane.ContactSelSet.ONLY_FIRST)[ContactMapPane.SECOND], 
						true);

				// group selection of absent contact in the second 
				// structure
				group(secondModGroup, absSecondEdgeSel, null);
				group(secondModGroup, absSecondNodeSel, null);
				group(topLevelGroup,  secondModGroup,   null);
			} 
			this.flush();
		}

		// send contacts present in the first and absent in the second model
		if( selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.SECOND].size() != 0 ) {

			// draw true contacts being present in the second model between 
			// the residues of the between model
			drawEdges(mod2, mod2, presSecondEdgeSel, presSecondNodeSel,
					SECOND_STRUCTURE_EDGE_COLOR,
					selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.SECOND],
					false);

			// group selection of present contact
			group(secondModGroup, presSecondEdgeSel, null);
			group(secondModGroup, presSecondNodeSel, null);
			group(topLevelGroup,  secondModGroup,    null);
			
			this.flush();
			
			if( selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.FIRST].size() != 0 ) {
				// draw true contact being present in the second model but 
				// NOT in the first one between the residues of the first 
				// model
				drawEdges(mod1, mod1, absFirstEdgeSel, absFirstNodeSel, 
						FIRST_STRUCTURE_EDGE_COLOR,
						selMap.get(ContactMapPane.ContactSelSet.ONLY_SECOND)[ContactMapPane.FIRST], 
						true);

				// group selection of absent contact
				group(firstModGroup, absFirstEdgeSel, null);
				group(firstModGroup, absFirstNodeSel, null);
				group(topLevelGroup, firstModGroup,   null);
			} 
			this.flush();
		}
		
		// call to flush() is not missing here!: for this case we flush 5 times separately after each block
	}
		
	/** 
	 * Show a single contact or non-contact as distance object in pymol
	 * @param mod
	 * @param cont the pair of residues
	 */
	public void showSingleDistance(Model mod, Pair<Integer> cont) {
		
		int pymolSelSerial = getNextSelNum();
		
		String structureID = mod.getLoadedGraphID();
		
		String topLevelGroup = "Sel" + pymolSelSerial; 
		String edgeSel = topLevelGroup+"_"+structureID+"_Dist";
		String nodeSel = topLevelGroup+"_"+structureID+"_Nodes";
		
		// create edge selection
		drawSingleEdge(edgeSel, mod, mod, cont);
		sendCommand("color "+SINGLE_EDGE_COLOR+", " + edgeSel);		
		
		// create node selection
		TreeSet<Integer> residues = new TreeSet<Integer>();
		residues.add(cont.getFirst());
		residues.add(cont.getSecond());		
		createSelectionObject(nodeSel, structureID, residues);
	
		group(topLevelGroup,  edgeSel + " " + nodeSel, null);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}

	/**
	 * Sets the view in PyMol when new selections are done:
	 * hides all objects (previous selections) and show just the 2 structures
	 * @param structureID1
	 * @param structureID2
	 */
	public void showStructureHideOthers(String structureID1, String structureID2){
		sendCommand("disable all");
		sendCommand("enable " + structureID1);
		sendCommand("enable " + structureID2);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}
		
}





