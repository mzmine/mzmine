/**
 * 
 */
package net.sf.mzmine.taskcontrol;

import java.util.Date;

import net.sf.mzmine.taskcontrol.Task.TaskPriority;

/**
 * 
 */
class WrappedTask implements Comparable {

    private Task task;
    private Date addedTime;
    private TaskListener listener;
    private TaskPriority priority;
    private WorkerThread assignedTo;

    WrappedTask(Task task, TaskPriority priority, TaskListener listener) {
        addedTime = new Date();
        this.task = task;
        this.listener = listener;
        this.priority = priority;
    }

    /**
     * Tasks are sorted by priority order using this comparator method.
     * 
     * @see java.lang.Comparable#compareTo(T)
     */
    public int compareTo(Object arg) {

        WrappedTask t = (WrappedTask) arg;
        int result;

        result = priority.compareTo(t.priority);
        if (result == 0)
            result = addedTime.compareTo(t.addedTime);
        return result;

    }

        
    
    /**
     * @return Returns the listener.
     */
    TaskListener getListener() {
        return listener;
    }

    /**
     * @return Returns the priority.
     */
    TaskPriority getPriority() {
        return priority;
    }

    
    /**
     * @param priority The priority to set.
     */
    void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    
    /**
     * @return Returns the assigned.
     */
    boolean isAssigned() {
        return assignedTo != null;
    }
    
    void assignTo(WorkerThread thread) {
        assignedTo = thread;
    }

    
    /**
     * @return Returns the task.
     */
    Task getTask() {
        return task;
    }
    
    public String toString() {
        return task.getTaskDescription();
    }
    
    

}
