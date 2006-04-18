/**
 * 
 */
package net.sf.mzmine.taskcontrol;

import net.sf.mzmine.util.Logger;

/**
 * 
 */
class WorkerThread extends Thread {

    private Task currentTask;

    WorkerThread(int workerNumber) {
        super("Task controller worker thread #" + workerNumber);
    }

    /**
     * @return Returns the currentTask.
     */
    Task getCurrentTask() {
        return currentTask;
    }

    /**
     * @param currentTask
     *            The currentTask to set.
     */
    void setCurrentTask(Task currentTask) {
        assert this.currentTask == null;
        this.currentTask = currentTask;
        synchronized(this) {
            notify();
        }
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        while (true) {

            while (currentTask == null) {
                try {
                    synchronized(this) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    // nothing happens
                }
            }

            switch (currentTask.getPriority()) {
            case HIGH:
                setPriority(MAX_PRIORITY);
                break;
            case LOW:
                setPriority(MIN_PRIORITY);
                break;
            default:
                setPriority(NORM_PRIORITY);
                break;
            }

            try {
                currentTask.run();
            } catch (Exception e) {
                Logger.putFatal("Unhandled exception while processing task "
                        + currentTask.getTaskDescription() + ": " + e);
                e.printStackTrace();
                // TODO: show a message box?
            }

            currentTask = null;

        }

    }

}
