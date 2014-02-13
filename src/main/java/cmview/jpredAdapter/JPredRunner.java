package cmview.jpredAdapter;

import java.io.IOException;
import java.util.concurrent.Callable;

import owl.core.connections.JPredConnection;
import owl.core.connections.JPredProgressRetriever;
import owl.core.connections.JPredStopNotifier;
import owl.core.structure.features.SecondaryStructure;

/**
 * Thread to run JPredConnection asynchronously.
 * TODO: Should this be merged with JPredConnection?
 * @author stehr
 */
public class JPredRunner implements Callable<SecondaryStructure> {

	String seq;
	JPredProgressRetriever progressRetriever;
	JPredStopNotifier stopNotifier;
	SecondaryStructure result;
	
	volatile boolean cancelled = false;	// flag to indicate that the thread should stop
	
	/**
	 * 
	 * @param seq the query sequence for which to retrieve a secondary structure prediction
	 * @param progressRetriever to be notified of the progress
	 * @param stopNotifier
	 * @param toBeCalledWhenDone
	 */
	JPredRunner(String seq, JPredProgressRetriever progressRetriever, JPredStopNotifier stopNotifier) {
		this.seq = seq;
		this.progressRetriever = progressRetriever;
		this.stopNotifier = stopNotifier;
	}
	
	public SecondaryStructure call() {
		JPredConnection conn = new JPredConnection();
		conn.setDebugMode(true);
		conn.setProgressRetriever(progressRetriever);
		conn.setStopNotifier(stopNotifier);
		try {
			conn.submitQuery(seq);
		} catch (IOException e) {
			progressRetriever.setError(e);
		}
		result = conn.getSecondaryStructurePredictionObject();
		return result;
	}
	
}
