package cmview;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import tools.PymolServerOutputStream;
import java.util.*;

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

	/*--------------------------- member variables --------------------------*/
	private String url;
	private PrintWriter Out;
	private boolean connected; 		// indicated whether a connection to pymol server had been established already

	/*----------------------------- constructors ----------------------------*/
	
	/**
	 *  Create a new Pymol communication object 
	 */
	public PyMolAdaptor(String pyMolServerUrl){
		this.url=pyMolServerUrl;
		this.connected = false;  // run tryConnectingToPymol() to connect
	}

	/*---------------------------- private methods --------------------------*/

	/**
	 * Construct a pymol object name from a pdb code and chain code.
	 */
	private String getChainObjectName(String pdbCode, String chainCode) {
		return pdbCode + chainCode;
	}
	
	/**
	 * Constructs a name for a selection.
	 * @param chainObjName The name of the chain object
	 * @param selSerial The serial number of the selection
	 * @return The selection name
	 */
	private String getSelObjectName(String chainObjName, String selectionType, int pymolSelSerial) {
		
		return selectionType + chainObjName+ "Sel" + pymolSelSerial;
	}
	
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
	 * Construct a name for a neighbourhood object.
	 */
	private String getNbhObjectName(String chainObjName, int nbhSerial) {
		return chainObjName + "Nbh" + nbhSerial;
	}
	
	/** Send command to pymol and check for errors */
	public void sendCommand(String cmd) {
		Out.println(cmd);
		if(Out.checkError()) {
			System.err.println("Pymol communication error. The last operation may have failed. Resetting connection.");
			this.Out = new PrintWriter(new PymolServerOutputStream(url),true);
		}
	}
	
	/**
	 * Creates an edge between the C-alpha atoms of the given residues in the given chain. 
	 * The selection in pymol will be names pdbcodeChaincode+"Sel"+selNum 
	 */
	private void setDistance(int i, int j, int pymolSelSerial, String selObjName, String chainObjNameFirst, String chainCodeFirst, String chainObjNameSecond, String chainCodeSecond){
		String pymolStr;
		pymolStr = "distance "+selObjName +", " 
			+ chainObjNameFirst  + " and chain " + chainCodeFirst  + " and resi " + i + " and name ca, " 
			+ chainObjNameSecond + " and chain " + chainCodeSecond + " and resi " + j + " and name ca"; 
		this.sendCommand(pymolStr);
	}

	
	/** 
	 * Create a selection of the given residues in pymol.
	 * @param selObjName
	 * @param chainObjName
	 * @param chainCode
	 * @param residues
	 */
	private void createSelectionObject(String selObjName, String chainObjName, String chainCode, ArrayList<Integer> residues) {
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
			sendCommand("select "+selObjName+", "+chainObjName+" and chain "+chainCode+" and "+resString);
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
	private void add2SelectionObject(String selObjName, String chainObjName, String chainCode, NodeSet residues) {
	    String resString = "(";
	    Vector<Interval> intervals = residues.getIntervals();
	    for(Interval i : intervals) {
		resString += "resi " + (i.end-i.beg == 0?i.beg:(i.beg + "-" + i.end)) + " or ";
	    }
	    // we put the last interval twice to encalulate the trailing 'or' 
	    // which has been added in the for-loop
	    Interval last = intervals.lastElement();
	    resString += "resi " + (last.beg == 0?last.beg:(last.beg + "-" + last.end)) + ")";
	    
	    if (resString.length() + 100 < PymolServerOutputStream.PYMOLCOMMANDLENGTHLIMIT) {
		sendCommand("select " + selObjName + ", " + 
			selObjName +                /* put previous selection in the selection string */
			" or (" + chainObjName +    /* the chain object */
			" and chain "+chainCode +   /* the chain identifier in the chain object */
			" and " + resString + ")"); /* the interval sequence of residues to be considered */
	    } else {
		System.err.println("Couldn't create pymol selection. Too many residues.");
	    }
	}

	/*---------------------------- public methods ---------------------------*/

	/**
	 * Try connecting to pymol server. Returns true on success, false otherwise.
	 */
	public boolean tryConnectingToPymol(long timeoutMillis) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis()-start < timeoutMillis) {
			try {
				String cmd;
				File f = new File(Start.getResourcePath(PYMOL_CALLBACK_FILE));
				OutputStream test = new PymolServerOutputStream(this.url);
				cmd = "run "+Start.getResourcePath(PYMOLFUNCTIONS_SCRIPT);
				test.write(cmd.getBytes());
				test.flush();
				cmd = "callback "+Start.getResourcePath(PYMOL_CALLBACK_FILE) + ", " + new Date();
				test.write(cmd.getBytes());
				test.flush();
				if(f.exists()) {
					f.deleteOnExit();
					hooray(test);
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
	
	/** being called when a connection to pymol has been successfully established */ 
	private void hooray(OutputStream s) {
		this.connected = true;
		this.Out = new PrintWriter(s,true);
		sendCommand("set dash_gap, 0");
		sendCommand("set dash_width, 1.5");
	}
	
	/**
	 * Shuts down the external viewer instance and releases all resources of this Adaptor.
	 * @param url The PyMol server url // TODO: can we get rid of this?
	 */
	public void shutdown(String url) {
		Out.println("quit");
		Out.close();
	}
	
	/**
	 * Send command to the pymol server to load a structure with the given name from the given temporary pdb file.
	 */
	public void loadStructure(String fileName, String pdbCode, String chainCode, boolean secondModel) {
		String chainObjName = getChainObjectName(pdbCode, chainCode);
		System.out.println("START loading structure "+chainObjName);
		sendCommand("load " + fileName + ", " + chainObjName);
		sendCommand("hide lines");
		sendCommand("show cartoon");		
		sendCommand("hide sticks");
		
		if (secondModel){
			// color second model green
			sendCommand("color " + ModelColors[1] + ", " + pdbCode+chainCode);
		}else {
			// color main model red
			sendCommand("disable all");
			sendCommand("enable " + chainObjName);
			sendCommand("color " + ModelColors[0] + ", " + pdbCode+chainCode);
		}
		
		sendCommand("cmd.refresh()");
		System.out.println("DONE loading structure "+chainObjName);
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
	 * @param pdbCodeFirst    pdb code of the first pdb structure
	 * @param chainCodeFirst  chain code corresponding to the first 
	 *                         structure
	 * @param pdbCodeSecond   and the second one
	 * @param chainCodeSecond and its chain to be considered
	 * 
	 * @see alignStructureUserDefined()
	 */
	public void alignStructure(String pdbCodeFirst, String chainCodeFirst,  String pdbCodeSecond, String chainCodeSecond){
		sendCommand("align " + pdbCodeSecond + chainCodeSecond + "," + pdbCodeFirst + chainCodeFirst);
		sendCommand("hide lines");
		sendCommand("hide sticks");
		sendCommand("zoom");
		sendCommand("cmd.refresh()");
	}
	
	/**
	 * Alignes two structures employing PyMol's <code>pair_fit</code> 
	 * command.
	 * It is strongly recommended to use this function to compute 
	 * superpositions in PyMol if you are able to provide structure 
	 * based protein alignments.
	 * 
	 * @see aglappe.sadp.SADP
	 */
	public void alignStructureUserDefined(String pdbCodeFirst, String chainCodeFirst, String pdbCodeSecond, String chainCodeSecond, String aliTagFirst, String aliTagSecond, Alignment ali) {
	
	    String          commandLine = "pair_fit " + pdbCodeFirst + chainCodeFirst + "///";
	    TreeSet<String> projectionTags = new TreeSet<String>();
	    EdgeSet         chunks      = null;
	    StringBuffer    chunkSeq    = null;
	    
	    // get chunks of first sequence with respect to the second sequence 
	    // (consider all alignment columns (-> 0 to 
	    // ali.getAlignmentLength(), do not allow for gaps in any sequence 
	    // in the projection)
	    projectionTags.add(aliTagSecond);
	    chunks   = ali.getMatchingBlocks(aliTagFirst,projectionTags,0,ali.getAlignmentLength(),projectionTags.size());

	    if( !chunks.isEmpty() ) {
		// put edge set into the recommended string format
		chunkSeq = new StringBuffer(chunks.size()*2);
		Edge e;
		for( Iterator<Edge> it = chunks.iterator(); it.hasNext(); ) {
		    e = it.next();
		    chunkSeq.append(e.i + "-" + e.j + "+");
		}
		// delete trailing '+' character
		chunkSeq.deleteCharAt(chunkSeq.length()-1);
		
		// append sequence of chunks to the command line along with the
		// atom types to be considered for superpositioning (here: CA)
		commandLine = commandLine + chunkSeq + "/CA";
		
		// get chunks of second sequence with respect to the first 
		// sequence
		projectionTags.clear();
		chunks.clear();
		chunkSeq.delete(0, chunkSeq.length());
		projectionTags.add(aliTagFirst);
		chunks = ali.getMatchingBlocks(aliTagSecond,projectionTags,0,ali.getAlignmentLength(),projectionTags.size());
		
		if( !chunks.isEmpty() ) {
		    for( Iterator<Edge> it = chunks.iterator(); it.hasNext(); ) {
			e = it.next();
			chunkSeq.append(e.i + "-" + e.j + "+");
		    }
		    chunkSeq.deleteCharAt(chunkSeq.length()-1);
		    commandLine = 
			commandLine + ", " + pdbCodeSecond + chainCodeSecond + "///" +
			chunkSeq + "/CA";
		    
		    System.out.println("superpositioning cmd:"+commandLine);
		    
		    sendCommand(commandLine);//	    Start.getPyMolAdaptor().alignStructureUserDefined(cmPane.getFirstModel().getPDBCode(), cmPane.getFirstModel().getChainCode(),
//		    cmPane.getSecondModel().getPDBCode(), cmPane.getSecondModel().getChainCode(),
//		    runner.getFirstName(),runner.getSecondName(),runner.getAlignment());

		    sendCommand("hide lines");
		    sendCommand("hide sticks");
		    sendCommand("zoom");
		    sendCommand("cmd.refresh()");
		    
		    return;    
		}
	    }
	    
	    System.err.println(
		    "Warning: The alignment of "+pdbCodeFirst+chainCodeFirst+" and "+
		    pdbCodeSecond+chainCodeSecond+" lacks of corresponding residues! "+
		    "No superposition of the structures can be displayed!"
		    );	        
	}
	
	public void pairFitSuperposition(String pdbCodeFirst, String chainCodeFirst, String pdbCodeSecond, String chainCodeSecond, EdgeSet chunksFirst, EdgeSet chunksSecond) {
	    // put edge set into the recommended string format
	    StringBuffer chunkSeq = new StringBuffer(chunksFirst.size()*2);
	    for( Edge e : chunksFirst ) {
		chunkSeq.append(e.i + "-" + e.j + "+");
	    }
	    // delete trailing '+' character
	    chunkSeq.deleteCharAt(chunkSeq.length()-1);

	    // append sequence of chunks to the command line along with the
	    // atom types to be considered for superpositioning (here: CA)
	    String commandLine = "pair_fit " + pdbCodeFirst + chainCodeFirst + "///" + chunkSeq + "/CA";
	
	    chunkSeq.delete(0, chunkSeq.length());
	    for( Edge e : chunksSecond ) {
		chunkSeq.append(e.i + "-" + e.j + "+");
	    }
	    chunkSeq.deleteCharAt(chunkSeq.length()-1);
	    
	    commandLine = commandLine + ", " + pdbCodeSecond + chainCodeSecond + "///" + chunkSeq + "/CA";
	    
	    //System.out.println("superpositioning cmd:"+commandLine);

	    sendCommand(commandLine);
	    sendCommand("hide lines");
	    sendCommand("hide sticks");
	    sendCommand("zoom");
	    sendCommand("cmd.refresh()");
	}
	
	/**
	 * Show the given edge neighbourhood as triangles in PyMol
	 */
	public void showTriangles(String pdbCode, String chainCode, EdgeNbh commonNbh, int pymolNbhSerial){
		String chainObjName = getChainObjectName(pdbCode, chainCode);
		String nbhObjName = getNbhObjectName(chainObjName, pymolNbhSerial);
		int trinum=1;
		ArrayList<Integer> residues = new ArrayList<Integer>();
		int i = commonNbh.i_resser;
		int j = commonNbh.j_resser;
		residues.add(i);
		residues.add(j);

		for (int k:commonNbh.keySet()){
						
			Random generator = new Random(trinum/2);
			int random = (Math.abs(generator.nextInt(trinum)) * 23) % trinum;
			
			sendCommand("triangle('"+ nbhObjName +"Tri"+trinum + "', "+ i+ ", "+j +", "+k +", '" + COLORS[random] +"', " + 0.7+")");
			trinum++;
			residues.add(k);	
		}
		sendCommand("zoom");
		createSelectionObject(nbhObjName + "Nodes", chainObjName, chainCode, residues );
	}

	/** Show the contacts in the given contact list as edges in pymol */
	public void edgeSelection(String pdbCode, String chainCode, String selectionType, String modelContactColor, int pymolSelSerial, EdgeSet selContacts, boolean dash, boolean centzoom){
	    	String chainObjName = getChainObjectName(pdbCode,  chainCode);
		
		if (selContacts.size()== 0) return; // if no contacts in selection do nothing
		
		ArrayList<Integer> residues = new ArrayList<Integer>();
		
		// the selection object name contains both chain object names 
		// if they differ, otherwise only the first chain object name 
		String selObjName = getSelObjectName(chainObjName, selectionType, pymolSelSerial);
				
		for (Edge cont:selContacts){ 
			int i = cont.i;
			int j = cont.j;
			//inserts an edge between the selected residues
			this.setDistance(i, j, pymolSelSerial, selObjName, chainObjName, chainCode, chainObjName, chainCode);
			residues.add(i);
			residues.add(j);
		}
		
		// hide distance labels
		sendCommand("hide labels, " + selObjName);
		
		// color distances
		this.sendCommand("color " + modelContactColor + "," + selObjName);

		if (dash ==true){
			// setting the dashed lines for present and absent distinction
			setDashes(pdbCode, chainCode, selectionType, pymolSelSerial);
		}else { // fixing the side chain problem
				// side chains only occur in case of common contacts
			sendCommand("hide lines, "+ selObjName);
			sendCommand("hide sticks, " + selObjName);
		}
		
		if (centzoom ==true){
			// centers and zooms into the selected object
			this.sendCommand("center " + selObjName);
			this.sendCommand("zoom " + selObjName);
		}
		createSelectionObject(selObjName+"Nodes", chainObjName, chainCode, residues);
		sendCommand("deselect " + selObjName+"Nodes");
	}
	
	/** Show a single contact or non-contact as distance object in pymol */
	public void sendSingleEdge(String pdbCode, String chainCode, int pymolSelSerial, Edge cont) {
		String chainObjName = getChainObjectName(pdbCode, chainCode);
		String selObjName = getSelObjectName(chainObjName, chainCode, pymolSelSerial);
		setDistance(cont.i, cont.j, pymolSelSerial, selObjName, chainObjName, chainCode, chainObjName, chainCode);
		ArrayList<Integer> residues = new ArrayList<Integer>();
		residues.add(cont.i);
		residues.add(cont.j);
		sendCommand("color orange, " + selObjName);
		
		createSelectionObject(selObjName+"Nodes", chainObjName, chainCode, residues);
	}
	
	public void sendTwoChainsEdgeSelection(String pdbCodeFirst, String chainCodeFirst, 
						String pdbCodeSecond, String chainCodeSecond,
						String selectionType, 
						String modelContactColor, 
						int pymolSelSerial, 
						EdgeSet residuePairs, 
						boolean dash, boolean centzoom) {
	    
	    Vector<String> chainObjNames = new Vector<String>(2);
	    chainObjNames.add(getChainObjectName(pdbCodeFirst,  chainCodeFirst));
	    chainObjNames.add(getChainObjectName(pdbCodeSecond, chainCodeSecond));
	    
	    if (residuePairs.size()== 0) return; // if no contacts in selection do nothing

	    ArrayList<Integer> residuesFirst = new ArrayList<Integer>();
	    NodeSet residuesSecond = new NodeSet();

	    // the selection object name contains both chain object names 
	    // if they differ, otherwise only the first chain object name 
	    String selObjName = getMultiChainSelObjectName(chainObjNames,selectionType,pymolSelSerial);

	    for (Edge cont:residuePairs){ 
		int i = cont.i;
		int j = cont.j;
		//inserts an edge between the selected residues
		this.setDistance(i, j, pymolSelSerial, selObjName, chainObjNames.get(0), chainCodeFirst, chainObjNames.get(1), chainCodeSecond);
		residuesFirst.add(i);
		residuesSecond.add(new Node(j));
	    }

	    // hide distance labels
	    sendCommand("hide labels, " + selObjName);
	    
	    // color distances
	    this.sendCommand("color " + modelContactColor + "," + selObjName);
	    
	    if (dash ==true){
		// setting the dashed lines for present and absent distinction
		setDashes(selObjName);
	    } else { // fixing the side chain problem
		// side chains only occur in case of common contacts
		sendCommand("hide lines, "+ selObjName);
		sendCommand("hide sticks, " + selObjName);
	    }

	    if (centzoom ==true){
		// centers and zooms into the selected object
		this.sendCommand("center " + selObjName);
		this.sendCommand("zoom " + selObjName);
	    }
	    
	    // TODO: lars@all: what is the reason for doing this selcting delselecting thing?
//	    createSelectionObject(selObjName+"Nodes", chainObjNames.get(0), chainCodeFirst,  residuesFirst);
//	    add2SelectionObject(selObjName+"Nodes",   chainObjNames.get(1), chainCodeSecond, residuesSecond);
//	    sendCommand("deselect " + selObjName+"Nodes");
	}
	
	/** setting the dashes lines for missed/added contacts */
	public void setDashes(String pdbCode, String chainCode, String selectionType, int pymolSelSerial){
		String chainObjName = this.getChainObjectName(pdbCode, chainCode);
		String selObjName = getSelObjectName(chainObjName, selectionType, pymolSelSerial);
		
		this.sendCommand("set dash_gap, 0.5, " + selObjName);
		this.sendCommand("set dash_length, 0.5, " + selObjName);
	}
	
	/**
	 * Converts the lines of the given selection (e.g. a distance object) 
	 * into dashed lines. 
	 * @param selObjName  a selection identifier (please ensure that you 
	 *  create your selection identifiers with function 
	 *  {@link #getChainObjectName(String, String)} or 
	 *  {@link #getMultiChainSelObjectName(Collection, String, int)} only)
	 */
	public void setDashes(String selObjName) {
	   this.sendCommand("set dash_gap, 0.5, "   +selObjName);
	   this.sendCommand("set dash_length, 0.5, "+selObjName);
	}
	
	/** setting the view in PyMol if new selections were done */
	public void setView(String pdbCode1, String chainCode1, String pdbCode2, String chainCode2){
		sendCommand("disable all");
		sendCommand("enable " + pdbCode1 + chainCode1 );
		sendCommand("enable " + pdbCode2 + chainCode2);
	}
	
	public void groupSelections(String pdbCode, String chainCode, int pymolSelSerial, String memberName1, String memberName2){

		sendCommand("cmd.group(name='"+ pdbCode+chainCode+ "Sel"+ pymolSelSerial+ "', members= '" + memberName1 +" " + memberName2 + "'),");
		sendCommand("cmd.group(name='"+ pdbCode+chainCode+ "Sel"+ pymolSelSerial+ "', members= '" + memberName1+"Node', action= 'add'),");
		sendCommand("cmd.group(name='"+ pdbCode+chainCode+ "Sel"+ pymolSelSerial+ "', members= '" + memberName2+"Node', action= 'add'),");
		
	}
	
	
	/**
	 * Returns whether a connection of this Adaptor to the server had been already successfully established.
	 * @return true if connection was established, false otherwise
	 */
	public boolean isConnected() {
		return this.connected;
	}
	
}





