/**
 * 
 */
package net.sf.mzmine.taskcontrol;

import net.sf.mzmine.userinterface.mainwindow.MainWindow;
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
            } catch (Throwable e) {
                
                // this should never happen!
                
                String errorMessage = "Unhandled exception while processing task "
                    + currentTask.getTaskDescription() + ": " + e + ", cancelling the task.";
                Logger.putFatal(errorMessage);
                
                currentTask.cancel();
                
                if (MainWindow.getInstance() != null)
                    MainWindow.getInstance().displayErrorMessage(errorMessage);
                
            }
            
            /* discard the task, so that garbage collecter can collect it */
            currentTask = null;
            
        }

    }

}
