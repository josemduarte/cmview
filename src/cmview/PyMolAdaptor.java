package cmview;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import tools.PymolServerOutputStream;
import java.util.*;

import javax.naming.TimeLimitExceededException; //we are using this for our own purposes here (to mark a timeout)

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
	// colors for triangles, one is chosen randomly from this list
	private static final String[] COLORS = {"blue", "red", "yellow", "magenta", "cyan", "tv_blue", "tv_green", "salmon", "warmpink"};

	private String[] ModelColors = {"lightpink", "palegreen"};
	
	private static final int INITIAL_CMDBUFFER_LENGTH = 10000;
	
	private static final String CMD_END_MARKER = "#end";
	private static final long TIMEOUT = 4000;
	
	/*--------------------------- member variables --------------------------*/
	private String url;
	private PrintWriter Out;
	private boolean connected; 		// indicated whether a connection to pymol server had been established already
	private boolean bufferedMode;
	private File cmdBufferFile;
	private StringWriter cmdBuffer; //TODO use StringBuffer instead?
	private int cmdCounter;
	private int reconnectTries;
	private PrintWriter log;
	private File callbackFile;

	/*----------------------------- constructors ----------------------------*/

	public PyMolAdaptor(String pyMolServerUrl, File cmdBufferFile, PrintWriter log) {
		this.url = pyMolServerUrl;
		this.connected = false;
		this.bufferedMode = true;
		this.cmdBufferFile = cmdBufferFile;
		this.log = log;
		this.cmdBuffer = new StringWriter(INITIAL_CMDBUFFER_LENGTH);
		this.reconnectTries = 0;
		this.cmdCounter = 0;
		this.callbackFile = new File(Start.TEMP_DIR,PYMOL_CALLBACK_FILE);
	}

	/*---------------------------- private methods --------------------------*/

	/**
	 * Gets a proper name for a selection of multiple chains.
	 * @param chainObjNames  collection of chain object identifiers
	 * @param selectionType  any kind of identifier which suggests the type 
	 *  of selection
	 * @param pymolSelSerial  this is supposed to be an incremental serial 
	 *  number which prevent the overwriting of previously made selection 
	 *  which do only differ from that one with respect to this serial
	 * @return selection identifier which respects the following format string: 
	 *  <code>&lt;selectionType&gt;&lt;"chainObjName 1"_"chainObjName 2"_...&gt;&lt;"Sel"&gt;&lt;pymolSelSerial&gt;</code>.
	 */
	private String getMultiChainSelObjectName(Collection<String> chainObjNames, String selectionType, int pymolSelSerial) {

		// construct output string buffer and estimate its capacity with
		// factor 6 -> pdb-code + chain-id + "_"
		// 5        -> estimated length of the pymolSelSerial + "Sel"
		StringBuffer output = new StringBuffer(chainObjNames.size()*6+selectionType.length()+2); 
		output.append(selectionType);
		boolean first = true;

		for( Iterator<String> it = chainObjNames.iterator(); it.hasNext(); ) {
			if( first ) {
				first = false;
				output.append(it.next());
			} else {
				output.append("_"+it.next());
			}
		}

		output.append("Sel"+pymolSelSerial);

		return output.toString();
	}

	/**
	 * Creates an edge between the C-alpha atoms of the given residues in the given chain.  
	 */
	private void setDistance(int i, int j, int pymolSelSerial, String selObjName, String chainObjNameFirst, String chainObjNameSecond){
		String pymolStr = "distance "+selObjName +", " 
		+ chainObjNameFirst  + " and resi " + i + " and name ca, " 
		+ chainObjNameSecond + " and resi " + j + " and name ca"; 
		this.sendCommand(pymolStr);
	}
	
	/**
	 * Create single edge between the CA atoms of the given residues of the given objects.
	 * @param distName
	 * @param firstSelObj  first structure object
	 * @param secondSelObj  second structure object
	 * @param i  index of residue in the first structure object 
	 * @param j  index of residue in the second structure object
	 */
	private void setDistance(String distName,String firstSelObj, String secondSelObj, int i, int j) {
		sendCommand("distance " + distName + ", "
					+ firstSelObj  + " and resi " + i + " and name ca, " 
					+ secondSelObj + " and resi " + j + " and name ca"); 
	}

	private void setDistance(String distName, String firstSelObj, String secondSelObj, 
							TreeSet<Integer> firstRes, TreeSet<Integer> secondRes) {
		
		if(firstRes.first() == 1) {
			System.out.println();
		}
		
		// prepare residue selection string
		String firstResString  = Interval.createSelectionString(firstRes); 
		String secondResString = Interval.createSelectionString(secondRes);
		
		// and send it to PyMol
		sendCommand("distance " + distName + ", " + 
					firstSelObj  + " and ( resi " + firstResString  + " ) and name ca, " + 
					secondSelObj + " and ( resi " + secondResString + " ) and name ca");
	}

	/** 
	 * Create a selection of the given residues in pymol.
	 * @param selObjName
	 * @param chainObjName
	 * @param residues
	 */
	private void createSelectionObject(String selObjName, String chainObjName, ArrayList<Integer> residues) {
		String resString = "";
		int start, last;

		// TODO: use NodeSet instead of ArrayList and replace the following by NodeSet.getIntervals()
		Collections.sort(residues);
		last = residues.get(0);
		start = last;
		for(int i:residues) {
			if(i > last+1) {
				resString += "resi " + (last-start == 0?last:(start + "-" + last)) + " or ";
				start = i;
				last = i;
			} else
				if(i == last) {
					// skip
				} else
					if(i == last+1) {
						last = i;
					}
		}
		resString += "resi " + (last-start == 0?last:(start + "-" + last));
		resString = "(" + resString + ")";
		//System.out.println(resString);

		if (resString.length() + 100 < PymolServerOutputStream.PYMOLCOMMANDLENGTHLIMIT) {
			sendCommand("select "+selObjName+", "+chainObjName+" and "+resString);
			
			// flush the buffer and send commands to PyMol via log-file
			this.flush();
		} else {
			System.err.println("Couldn't create pymol selection. Too many residues.");
		}
	}
	
	private void createSelectionObject(String selObjName, String chainObjName, TreeSet<Integer> residues) {
		String resString = "";
		int start, last;
		last = residues.first();
		start = last;
		for(int i:residues) {
			if(i > last+1) {
				resString += "resi " + (last-start == 0?last:(start + "-" + last)) + " or ";
				start = i;
				last = i;
			} else
				if(i == last) {
					// skip
				} else
					if(i == last+1) {
						last = i;
					}
		}
		resString += "resi " + (last-start == 0?last:(start + "-" + last));
		resString = "(" + resString + ")";
		//System.out.println(resString);

		if (resString.length() + 100 < PymolServerOutputStream.PYMOLCOMMANDLENGTHLIMIT) {
			sendCommand("select " + selObjName + ", " + chainObjName + " and " + resString);
			// flush the buffer and send commands to PyMol via log-file
			this.flush();
		} else {
			System.err.println("Couldn't create pymol selection. Too many residues.");
		}
	}

	/**
	 * Adds a selection of residues of certain chain of a pymol object to 
	 * a previously created selection. 
	 * @param selObjName  the selection to be extended
	 * @param chainObjName  chain object identifier
	 * @param chainCode  chain identifier corresponding to 
	 *  <code>chainObjName</code>
	 * @param residues  set of residues in chain with 
	 *  <code>chainCode</code> which belong to the chain object 
	 *  <code>chainObjName</code>
	 */
	@SuppressWarnings("unused")
	private void add2SelectionObject(String selObjName, String chainObjName, String chainCode, TreeSet<Integer> residues) {
		String resString = "(";
		IntervalSet intervals = Interval.getIntervals(residues);
		for(Interval i : intervals) {
			resString += "resi " + (i.end-i.beg == 0?i.beg:(i.beg + "-" + i.end)) + " or ";
		}
		// we put the last interval twice to encalulate the trailing 'or' 
		// which has been added in the for-loop
		Interval last = intervals.last();
		resString += "resi " + (last.beg == 0?last.beg:(last.beg + "-" + last.end)) + ")";

		if (resString.length() + 100 < PymolServerOutputStream.PYMOLCOMMANDLENGTHLIMIT) {
			sendCommand("select " + selObjName + ", " + 
					selObjName +                /* put previous selection in the selection string */
					" or (" + chainObjName +    /* the chain object */
					" and chain "+chainCode +   /* the chain identifier in the chain object */
					" and " + resString + ")"); /* the interval sequence of residues to be considered */
			
			// flush the buffer and send commands to PyMol via log-file
			this.flush();
		} else {
			System.err.println("Couldn't create pymol selection. Too many residues.");
		}
	}

	/*---------------------------- public methods ---------------------------*/

	/** Send command to pymol and check for errors */
	private void sendCommand(String cmd) {
		if (!Start.isPyMolConnectionAvailable()) {
			return;
		}
		if( bufferedMode ) {
			cmdBuffer.write(cmd + "\n");
		} else {
			Out.println(cmd);			
			if(Out.checkError()) {
				if (reconnectTries>=Start.PYMOL_RECONNECT_TRIES) {
					System.err.println("Couldn't reset connection, PyMol connection is lost!");
					Start.setUsePymol(false);
					return;
				}
				System.err.println("Pymol communication error. The last operation may have failed. Resetting connection.");
				this.Out = new PrintWriter(new PymolServerOutputStream(url),true);
				reconnectTries++;
			}
		}
	}
	
	public void flush() {
		if( bufferedMode ) {
			try {
				cmdCounter++;
				// write data from buffer to file
				FileWriter fWriter = new FileWriter(cmdBufferFile);
				cmdBuffer.flush();
				fWriter.write(cmdBuffer.toString());
				fWriter.write("callback "+callbackFile.getAbsolutePath() + ", " + cmdCounter+"\n");
				fWriter.write(CMD_END_MARKER+cmdCounter);
				fWriter.flush(); //TODO is this needed?
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
				waitForTagInFile(cmdBufferFile, CMD_END_MARKER+cmdCounter, TIMEOUT);
				Out.println("@" + cmdBufferFile.getAbsolutePath());
				if(Out.checkError()) {
					if (reconnectTries>=Start.PYMOL_RECONNECT_TRIES) {
						System.err.println("Couldn't reset connection, PyMol connection is lost!");
						Start.setUsePymol(false);
						return;
					}
					System.err.println("Pymol communication error. The last operation may have failed. Resetting connection.");
					this.Out = new PrintWriter(new PymolServerOutputStream(url),true);
					reconnectTries++;
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
	}

	public void setPyMolServerUrl(String url) {
		this.url = url; 
	}
	
	/**
	 * Try connecting to pymol server. Returns true on success, false otherwise.
	 */
	public boolean tryConnectingToPymol(long timeoutMillis) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis()-start < timeoutMillis) {
			try {
				String cmd;	
				OutputStream test = new PymolServerOutputStream(this.url);
				cmd = "run "+Start.getResourcePath(PYMOLFUNCTIONS_SCRIPT);
				test.write(cmd.getBytes());
				test.flush();
				cmd = "callback "+callbackFile.getAbsolutePath() + ", " + new Date();
				test.write(cmd.getBytes());
				test.flush();
				if(callbackFile.exists()) {
					callbackFile.deleteOnExit();
					this.connected = true;
					this.Out = new PrintWriter(test,true);
					return true;
				} else {
					test.close();
					continue;
				}
			} catch (Exception e) {
				continue;
			}
		}
		return false;
	}

	/**
	 * Sends some inital set-up commands after a connection has been successfully established.
	 */
	public void initialize() {
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
	 * Send command to the pymol server to load a structure with the given name from the given temporary pdb file.
	 */
	public void loadStructure(String fileName, String structureID, boolean secondModel) {

		//System.out.println("START loading structure "+structureID);
		sendCommand("load " + fileName + ", " + structureID);
		sendCommand("hide lines");
		sendCommand("show cartoon");		
		sendCommand("hide sticks");

		if (secondModel){
			// color second model green
			sendCommand("color " + ModelColors[1] + ", " + structureID);
		}else {
			// color main model red
			sendCommand("disable all");
			sendCommand("enable " + structureID);
			sendCommand("color " + ModelColors[0] + ", " + structureID);
		}

		sendCommand("orient");
		
		sendCommand("cmd.refresh()");
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
		
		//System.out.println("DONE loading structure "+structureID);
	}

	/**
	 * Alignes two structures employing PyMol's <code>align</code> command.
	 * PyMol internally makes a sequence alignment of both structures and
	 * tries to find the superposition with minimal RMSD of the structures
	 * given this alignment. Please note, that one major drawback of using
	 * this function is that the superposition relies on a sequence 
	 * alignment to find corresponding residues which might yield quite 
	 * nonsatisfying results for proteins with a rather low sequence 
	 * identity!
	 * 
	 * @param structureID1 the structure id of the first structure
	 * @param structureID2 the structure id of the second structure
	 * 
	 * @see pairFitSuperposition()
	 */
	public void alignStructures(String structureId1,  String structureId2){
		sendCommand("align " + structureId1 + "," + structureId2);
		sendCommand("hide lines");
		sendCommand("hide sticks");
		sendCommand("zoom");
		sendCommand("cmd.refresh()");
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}

	/**
	 * Superimpose two structures identified by their structureIDs, using PyMol's
	 * <code>pair_fit</code> command which does a C-alpha minimum RMSD fit on a
	 * given set of residues.
	 * @param structureID1 the structure id of the first structure
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
		String commandLine = "pair_fit " + structureID1 + "///" + chunkSeq + "/CA";

		chunkSeq.delete(0, chunkSeq.length());
		for( Interval e : chunksSecond ) {
			chunkSeq.append(e.beg + "-" + e.end + "+");
		}
		chunkSeq.deleteCharAt(chunkSeq.length()-1);

		commandLine = commandLine + ", " + structureID2 + "///" + chunkSeq + "/CA";

		sendCommand(commandLine);
		sendCommand("hide lines");
		sendCommand("hide sticks");
		sendCommand("zoom");
		sendCommand("cmd.refresh()");
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}

	/**
	 * Draw triangle objects in PyMol for a given common neighbourhood.
	 * @param chainObjName the structure id of the object in PyMol
	 * @param triangleBaseName the basename of the triagle object in PyMol (a number will be apended)
	 * @param nodeSelName the name of the node selection to be created
	 * @param commonNbh the common neighbourhood object for which triangles will be drawn
	 * @return
	 */
	public Collection<String> showTriangles(String chainObjName, String triangleBaseName, String nodeSelName, RIGCommonNbhood commonNbh){
		int trinum=1;
		TreeSet<Integer> residues = new TreeSet<Integer>();
		int i = commonNbh.getFirstNode().getResidueSerial();
		int j = commonNbh.getSecondNode().getResidueSerial();
		residues.add(i);
		residues.add(j);

		String curTriangleName = "";
		TreeSet<String>  triangleSelNames = new TreeSet<String>();
		
		for (int k:commonNbh.keySet()){

			Random generator = new Random(trinum/2);
			int random = (Math.abs(generator.nextInt(trinum)) * 23) % trinum;

			curTriangleName = triangleBaseName + trinum;
			sendCommand("triangle('"+ curTriangleName + "', "+ i+ ", "+j +", "+k +", '" + COLORS[random] +"', " + 0.7+")");
			trinum++;
			residues.add(k);
			triangleSelNames.add(curTriangleName);
		}
		
		sendCommand("zoom");
		createSelectionObject(nodeSelName, chainObjName, residues );
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
		
		return triangleSelNames;
	}

	/**
	 * Creates an edge selection.
	 * Edges are implemented as distances drawn between the residues of the 
	 * first and the second selection. The given set of pairs of integers 
	 * defines the residues to be connected, whereas <code>getFirst()</code> 
	 * always yields the residue indices in the first selection and 
	 * <code>getSecond()</code> those in the second.
	 * @param firstObjSel  name of the first object selection
	 * @param secondObjSel  name of the second object selection
	 * @param edgeSelName  name of the edge selection to be created
	 * @param nodeSelName  name of the node selection consisting of all 
	 *  residues incident to the contacts 
	 * @param edgeColor  name of the color of the edges
	 * @param selContacts  set of pairs of residues to be connected
	 * @param dash  enable to draw dashed edges
	 */
	public void edgeSelection(String firstObjSel, String secondObjSel, String edgeSelName, String nodeSelName, 
								String edgeColor, IntPairSet selContacts, boolean dash) {

		// if no contacts in selection do nothing
		if (selContacts.size()== 0) {
			return; 
		}

		TreeSet<Integer> firstResidues  = new TreeSet<Integer>();
		TreeSet<Integer> secondResidues = new TreeSet<Integer>();
		
		if( selContacts.size() > 400 ) {
			// send packed selections to PyMol
			TreeMap<Integer, TreeSet<Integer>> adjLists = new TreeMap<Integer, TreeSet<Integer>>();
			int i,j;
			
			for(Pair<Integer> cont:selContacts) {
				i = cont.getFirst();
				j = cont.getSecond();
				if( ! adjLists.containsKey(i) ) {
					adjLists.put(i, new TreeSet<Integer>());
					adjLists.get(i).add(j);
					firstResidues.add(i);
					secondResidues.add(j);
				} else {
					adjLists.get(i).add(j);
					secondResidues.add(j);
				}
			}
			
			// check size
			if( adjLists.size() > 500 ) {
				System.err.println("Selection too big!");
				return;
			}
			
			// send each adjacency list separately
			TreeSet<Integer> dummy = new TreeSet<Integer>();
			for(Integer k : adjLists.keySet()) {
				dummy.add(k);
				this.setDistance(edgeSelName, firstObjSel, secondObjSel, dummy, adjLists.get(k));
				dummy.remove(k);
			}			
		} else {
			// send each contact as a single command to PyMol
			int i,j;
			for (Pair<Integer> cont:selContacts){ 
				i = cont.getFirst();
				j = cont.getSecond();
				// draws an edge between the selected residues
				this.setDistance(edgeSelName,firstObjSel,secondObjSel,i,j);
				firstResidues.add(i);
				secondResidues.add(j);
			}
		}

		// hide distance labels
		sendCommand("hide labels, " + edgeSelName);

		// color distances
		this.sendCommand("color " + edgeColor + "," + edgeSelName);

		if (dash ==true){
			// setting the dashed lines for present and absent distinction
			setDashes(edgeSelName);
		} else { 
			// fixing the side chain problem
			// side chains only occur in case of common contacts
			sendCommand("hide lines,  " + edgeSelName);
			sendCommand("hide sticks, " + edgeSelName);
		}
		
		// create selection of nodes incident to the contacts
		select(nodeSelName, "(" + firstObjSel  + " and resi " + Interval.createSelectionString(firstResidues) + ") or " +
							"(" + secondObjSel + " and resi " + Interval.createSelectionString(secondResidues)+ ")");
		
		sendCommand("disable " + nodeSelName);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}
	
	
	/** Show a single contact or non-contact as distance object in pymol */
	public void sendSingleEdge(String structureID, int pymolSelSerial, Pair<Integer> cont) {

		String selObjName = "Sel"+pymolSelSerial+"_"+structureID+"_Dist";
		setDistance(cont.getFirst(), cont.getSecond(), pymolSelSerial, selObjName, structureID, structureID);
		ArrayList<Integer> residues = new ArrayList<Integer>();
		residues.add(cont.getFirst());
		residues.add(cont.getSecond());
		sendCommand("color orange, " + selObjName);

		createSelectionObject(selObjName+"Nodes", structureID, residues);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}

	/**
	 * Converts the lines of the given selection (e.g. a distance object) 
	 * into dashed lines. 
	 * @param edgeObjName  a selection identifier (please ensure that you 
	 *  create your selection identifiers with function 
	 *  {@link #getChainObjectName(String, String)} or 
	 *  {@link #getMultiChainSelObjectName(Collection, String, int)} only)
	 */
	public void setDashes(String edgeSelName) {
		this.sendCommand("set dash_gap, 0.5, "    + edgeSelName);
		this.sendCommand("set dash_length, 0.5, " + edgeSelName);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}

	/** setting the view in PyMol if new selections were done */
	public void showStructureHideOthers(String structureID1, String structureID2){
		sendCommand("disable all");
		sendCommand("enable " + structureID1);
		sendCommand("enable " + structureID2);
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
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
	public void group(String groupName, String members, String action) {
		
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
		
		// flush the buffer and send commands to PyMol via log-file
		this.flush();
	}
	
	/**
	 * Create a new selection with the given name in PyMol.
	 * @param selName the name of the new selection
	 * @param selection the selection string (following PyMol's selection syntax)
	 */
	private void select(String selName, String selection) {
		
		String command = "select " + selName + "," + selection;
		
		if( command.length() < PymolServerOutputStream.PYMOLCOMMANDLENGTHLIMIT ) {
			sendCommand(command);
		} else {
			System.err.println("Command too long to be sent to PyMol: " + command);
		}
	}
	
	/**
	 * Returns whether a connection of this Adaptor to the server had been already successfully established.
	 * @return true if connection was established, false otherwise
	 */
	public boolean isConnected() {
		return this.connected;
	}

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
}





