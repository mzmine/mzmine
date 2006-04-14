/**
 * 
 */
package net.sf.mzmine.taskcontrol;

/**
 * This is a wrapper class that represents a task. Since the task may be
 * transferred to another cluster node (and therefore the reference to local
 * task becomes invalid), this wrapper is necessary to keep the actual reference
 * to the task's location.
 */
class AbstractTaskReference implements Task {

    private Task referencedTask;

    /**
     * 
     */
    AbstractTaskReference(Task referencedTask) {
        this.referencedTask = referencedTask;
    }

    /**
     * @see net.sf.mzmine.newdistributionframework.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return referencedTask.getTaskDescription();
    }

    /**
     * @see net.sf.mzmine.newdistributionframework.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return referencedTask.getFinishedPercentage();
    }

    /**
     * @see net.sf.mzmine.newdistributionframework.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return referencedTask.getStatus();
    }

    /**
     * @see net.sf.mzmine.newdistributionframework.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return referencedTask.getErrorMessage();
    }

    /**
     * @see net.sf.mzmine.newdistributionframework.Task#getResult()
     */
    public Object getResult() {
        return referencedTask.getResult();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        referencedTask.run();
    }

    /**
     * @see net.sf.mzmine.newdistributionframework.Task#getPriority()
     */
    public TaskPriority getPriority() {
        return referencedTask.getPriority();
    }

    /**
     * @return Returns the referencedTask.
     */
    Task getReferencedTask() {
        return referencedTask;
    }

    /**
     * @param referencedTask
     *            The referencedTask to set.
     */
    void setReferencedTask(Task referencedTask) {
        this.referencedTask = referencedTask;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        referencedTask.cancel();
    }

}
