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

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.dialogs.TaskProgressWindow;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.Logger;

/**
 * 
 */
public class TaskController implements Runnable {

    private static TaskController myInstance;

    private final int TASKCONTROLLER_THREAD_SLEEP = 100;

    private Thread taskControllerThread;

    private WorkerThread[] workerThreads;

    class WrappedTask {

        Task task;
        Date addedTime = new Date();
        TaskListener listener;
        TaskPriority priority;
        boolean assigned = false;
        InetAddress node;
    }

    private Vector<WrappedTask> taskQueue;
    private WrappedTask[] currentTasks; // taskQueue represented as sorted array

    public static TaskController getInstance() {
        return myInstance;
    }

    private TaskModel taskModel;

    /**
     * 
     */
    public TaskController(int numberOfThreads) {

        assert myInstance == null;
        myInstance = this;

        taskQueue = new Vector<WrappedTask>();

        taskControllerThread = new Thread(this, "Task controller thread");
        taskControllerThread.setPriority(Thread.MIN_PRIORITY);
        taskControllerThread.start();

        workerThreads = new WorkerThread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            workerThreads[i] = new WorkerThread(i);
            workerThreads[i].start();
        }

        taskModel = new TaskModel();

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

        WrappedTask newQueueEntry = new WrappedTask();
        newQueueEntry.task = task;
        newQueueEntry.listener = listener;
        newQueueEntry.priority = priority;

        Logger.put("Adding task " + task.getTaskDescription()
                + " to the task controller queue");

        synchronized (taskQueue) {
            taskQueue.add(newQueueEntry);
            taskQueue.notifyAll();
        }

        /*
         * show the task list component
         */
        MainWindow mainWindow = MainWindow.getInstance();
        if (mainWindow != null) {
            TaskProgressWindow tlc = mainWindow.getTaskList();
            tlc.setVisible(true);
        }

        taskModel.fireTableDataChanged();

        return task;

    }

    public Task getTask(int index) {
        synchronized (taskQueue) {
            if ((index < 0) || (index > currentTasks.length))
                return null;
            return currentTasks[index].task;
        }
    }

    /**
     * Task controller thread main method.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {

        while (true) {

            synchronized (taskQueue) {

                /* if the queue is empty, we can sleep */
                while (taskQueue.isEmpty()) {
                    try {
                        taskQueue.wait();
                    } catch (InterruptedException e) {
                    }
                }

                currentTasks = taskQueue.toArray(new WrappedTask[0]);
                // sort the array, so we can traverse the tasks in priority
                // order
                Arrays.sort(currentTasks, new TaskPriorityComparator());

                // for each task, check if it's assigned
                for (int i = 0; i < currentTasks.length; i++) {

                    if (!currentTasks[i].assigned) {
                        // poll local threads

                        for (WorkerThread worker : workerThreads) {
                            // Logger.put("polling thread " + worker + " for
                            // task " + task.task.getTaskDescription());
                            if (worker.getCurrentTask() == null) {
                                if (currentTasks[i].listener != null)
                                    currentTasks[i].listener
                                            .taskStarted(currentTasks[i].task);
                                worker.setCurrentTask(currentTasks[i].task);
                                currentTasks[i].assigned = true;
                                break;
                            }

                        }

                        // TODO: poll remote nodes

                    }

                    /* check whether the task is finished */
                    TaskStatus status = currentTasks[i].task.getStatus();
                    if ((status == TaskStatus.FINISHED)
                            || (status == TaskStatus.ERROR)
                            || (status == TaskStatus.CANCELED)) {
                        if (currentTasks[i].listener != null)
                            currentTasks[i].listener
                                    .taskFinished(currentTasks[i].task);
                        taskQueue.remove(currentTasks[i]);
                        taskModel.fireTableRowsDeleted(i, i);
                    }

                    MainWindow mainWindow = MainWindow.getInstance();
                    if (mainWindow != null) {
                        TaskProgressWindow tlc = mainWindow.getTaskList();
                        if (tlc.isVisible()) {

                            if (taskQueue.isEmpty())
                                tlc.setVisible(false);
                            else {
                                taskModel.fireTableRowsUpdated(0,
                                        currentTasks.length);
                            }

                        }

                    }

                }

            }

            try {
                Thread.sleep(TASKCONTROLLER_THREAD_SLEEP);
            } catch (InterruptedException e) {
                // do nothing
            }
        }

    }
    
    public void setTaskPriority(Task task, TaskPriority priority) {
        synchronized (taskQueue) {
            Iterator<WrappedTask> i = taskQueue.iterator();
            while (i.hasNext()) {
                WrappedTask wt = i.next();
                if (wt.task == task) { 
                    wt.priority = priority;
                    return;
                }
            }
            
            
        }
    }

    public TaskModel getTableModel() {
        return taskModel;
    }

    public class TaskModel extends AbstractTableModel {

        private final int NUM_COLUMNS = 4;

        public final String colDescription = "Item";
        public final String colPriorityName = "Priority";
        public final String colJobStatus = "Status";
        public final String colJobRate = "% done";

        /**
         * @see javax.swing.table.TableModel#getRowCount()
         */
        public int getRowCount() {
            return taskQueue.size();
        }

        /**
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        public int getColumnCount() {
            return NUM_COLUMNS;
        }

        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return colDescription;
            case 1:
                return colPriorityName;
            case 2:
                return colJobStatus;
            case 3:
                return colJobRate;
            }
            return "";
        }

        /**
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        public Object getValueAt(int row, int column) {
            synchronized (taskQueue) {

                try {
                    WrappedTask task = currentTasks[row];
                    switch (column) {
                    case 0:
                        return task.task.getTaskDescription();
                    case 1:
                        return task.priority;
                    case 2:
                        return task.task.getStatus();
                    case 3:
                        return String.valueOf(Math.round(task.task
                                .getFinishedPercentage() * 100))
                                + "%";
                    }
                } catch (Exception e) {
                }
            }
            return "";
        }

        public void fireTableDataChanged() {

            synchronized (taskQueue) {

                currentTasks = taskQueue.toArray(new WrappedTask[0]);
                // sort the array, so we can traverse the tasks in priority
                // order
                Arrays.sort(currentTasks, new TaskPriorityComparator());

            }

            super.fireTableDataChanged();
        }

    }

    class TaskPriorityComparator implements Comparator<WrappedTask> {

        /**
         * @see java.util.Comparator#compare(T, T)
         */
        public int compare(WrappedTask arg0, WrappedTask arg1) {
            int result;
            result = arg0.priority.compareTo(arg1.priority);
            if (result == 0)
                result = arg0.addedTime.compareTo(arg1.addedTime);
            return result;
        }

    }

}
