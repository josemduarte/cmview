package cmview.toolUtils;

import java.util.concurrent.Callable;

import actionTools.Action;

/**
 * Basic class for any tool that should run in a separate thread.
 *  
 * TODO: Maybe it is more reasonable to implement the Callable interface instead of the Runnable interface as 
 * this support direct results retrieval.
 * 
 * @author Lars Petzold
 *
 */
public abstract class ToolRunner<V> implements Callable<V>{
    
    boolean done = false;
    Action  actionWhenDone;
    
    /**
     * Empty constructor.
     */
    public ToolRunner() {
	
    }
    
    /**
     * Creates a new tool runner along with the an action-when-done instance.
     * @param toBePerformedWhenDone
     */
    public ToolRunner(Action toBePerformedWhenDone) {
	actionWhenDone = toBePerformedWhenDone;
    }
    
    /**
     * Gets the action object.
     * @return
     */
    public Action getActionWhenDone() {
	return actionWhenDone;
    }
    
    /**
     * Action to be performed when the runner finishes his work.
     * @param toBePerformedWhenDone
     */
    public void setActionWhenDone(Action toBePerformedWhenDone) {
	actionWhenDone = toBePerformedWhenDone;
    }
    
   
}
