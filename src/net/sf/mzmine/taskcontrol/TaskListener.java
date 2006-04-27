/**
 * 
 */
package net.sf.mzmine.taskcontrol;

/**
 *
 */
public interface TaskListener {
    
    public void taskStarted(Task task);

    public void taskFinished(Task task);
    
}
