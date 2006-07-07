/*
 * Copyright 2006 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.taskcontrol;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyVetoException;

import javax.swing.JInternalFrame;
import javax.swing.table.TableModel;

import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.dialogs.TaskProgressWindow;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.Logger;

/**
 * 
 */
public class TaskController implements Runnable {

    
    // TODO: always create a worker thread for high priority tasks
    
    
    private static TaskController myInstance;

    private final int TASKCONTROLLER_THREAD_SLEEP = 100;

    private Thread taskControllerThread;

    private WorkerThread[] workerThreads;

    private TaskQueue taskQueue;

    public static TaskController getInstance() {
        return myInstance;
    }

    /**
     * 
     */
    public TaskController(int numberOfThreads) {

        assert myInstance == null;
        myInstance = this;

        taskQueue = new TaskQueue();

        taskControllerThread = new Thread(this, "Task controller thread");
        taskControllerThread.setPriority(Thread.MIN_PRIORITY);
        taskControllerThread.start();

        workerThreads = new WorkerThread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            workerThreads[i] = new WorkerThread(i);
            workerThreads[i].start();
        }

    }

    public Task addTask(Task task) {
        return addTask(task, TaskPriority.NORMAL, null);
    }

    public Task addTask(Task task, TaskPriority priority) {
        return addTask(task, priority, null);
    }

    public Task addTask(Task task, TaskListener listener) {
        return addTask(task, TaskPriority.NORMAL, listener);
    }

    public Task addTask(Task task, TaskPriority priority, TaskListener listener) {

        assert task != null;

        WrappedTask newQueueEntry = new WrappedTask(task, priority, listener);

        Logger.put("Adding task " + task.getTaskDescription()
                + " to the task controller queue");

        taskQueue.addWrappedTask(newQueueEntry);

        synchronized (this) {
            this.notifyAll();
        }

        /*
         * show the task list component
         */
        MainWindow mainWindow = MainWindow.getInstance();
        if (mainWindow != null) {
            TaskProgressWindow tlc = mainWindow.getTaskList();
            JInternalFrame selectedFrame = mainWindow.getDesktop().getSelectedFrame();
           //  KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            // Component currentFocus = focusManager.getFocusOwner();
            tlc.setVisible(true);
            //mainWindow.getDesktop().setSelectedFrame(selectedFrame);
            if (selectedFrame != null) {
                try {
                    selectedFrame.setSelected(true);
                } catch (PropertyVetoException e) {
                    // do nothing
                }
            }
            // currentFocus.requestFocus();
        }

        return task;

    }

    /**
     * Task controller thread main method.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {

        while (true) {

            /* if the queue is empty, we can sleep */
            while (taskQueue.isEmpty()) {
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }

            WrappedTask[] queueSnapshot = taskQueue.getQueueSnapshot();

            // for each task, check if it's assigned
            for (WrappedTask task : queueSnapshot) {

                TaskListener listener = task.getListener();

                if (!task.isAssigned()) {
                    // poll local threads

                    for (WorkerThread worker : workerThreads) {

                        if (worker.getCurrentTask() == null) {
                            if (listener != null)
                                listener.taskStarted(task.getTask());
                            worker.setCurrentTask(task);
                            break;
                        }

                    }

                    // TODO: poll remote nodes

                }

                /* check whether the task is finished */
                TaskStatus status = task.getTask().getStatus();
                if ((status == TaskStatus.FINISHED)
                        || (status == TaskStatus.ERROR)
                        || (status == TaskStatus.CANCELED)) {
                    if (listener != null)
                        listener.taskFinished(task.getTask());
                    taskQueue.removeWrappedTask(task);
                }

            }

            MainWindow mainWindow = MainWindow.getInstance();
            if (taskQueue.isEmpty() && (mainWindow != null)) {
                TaskProgressWindow tlc = mainWindow.getTaskList();
                tlc.setVisible(false);
            } else {
                taskQueue.refresh();
            }

            try {
                Thread.sleep(TASKCONTROLLER_THREAD_SLEEP);
            } catch (InterruptedException e) {
                // do nothing
            }
        }

    }

    public void setTaskPriority(Task task, TaskPriority priority) {
        WrappedTask wt = taskQueue.getWrappedTask(task);
        if (wt != null)
            wt.setPriority(priority);
    }

    public TableModel getTaskTableModel() {
        return taskQueue;
    }

    public Task getTask(int index) {
        WrappedTask wt = taskQueue.getWrappedTask(index);
        if (wt != null)
            return wt.getTask();
        else
            return null;
    }

}
