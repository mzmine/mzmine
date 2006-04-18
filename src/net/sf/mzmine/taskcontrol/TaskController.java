/*
 * Copyright 2006 Okinawa Institute of Science and Technology
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

/**
 * 
 */
package net.sf.mzmine.taskcontrol;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

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
        boolean assigned = false;
        InetAddress node;
    }

    private Vector<WrappedTask> taskQueue;

    public static TaskController getInstance() {
        return myInstance;
    }

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
    }

    public Task addTask(Task task) {
        return addTask(task, null);
    }

    public Task addTask(Task task, TaskListener listener) {

        assert task != null;

        WrappedTask newQueueEntry = new WrappedTask();
        newQueueEntry.task = task;
        newQueueEntry.listener = listener;

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

        return task;

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

                WrappedTask[] currentTasks = taskQueue
                        .toArray(new WrappedTask[0]);
                // sort the array, so we can traverse the tasks in priority
                // order
                Arrays.sort(currentTasks, new TaskPriorityComparator());

                // for each task, check if it's assigned
                for (WrappedTask task : currentTasks) {

                    if (!task.assigned) {
                        // poll local threads

                        for (WorkerThread worker : workerThreads) {
                            // Logger.put("polling thread " + worker + " for
                            // task " + task.task.getTaskDescription());
                            if (worker.getCurrentTask() == null) {
                                worker.setCurrentTask(task.task);
                                task.assigned = true;
                                break;
                            }

                        }

                        // TODO: poll remote nodes

                    } else {
                        /* check whether the task is finished */
                        TaskStatus status = task.task.getStatus();
                        if ((status == TaskStatus.FINISHED)
                                || (status == TaskStatus.ERROR)
                                || (status == TaskStatus.CANCELED)) {
                            if (task.listener != null)
                                task.listener.taskFinished(task.task);
                            taskQueue.remove(task);
                        }
                    }

                    MainWindow mainWindow = MainWindow.getInstance();
                    if (mainWindow != null) {
                        TaskProgressWindow tlc = mainWindow.getTaskList();
                        if (tlc.isVisible()) {

                            if (taskQueue.isEmpty())
                                tlc.setVisible(false);
                            else {
                                Task[] componentList = new Task[currentTasks.length];
                                for (int i = 0; i < currentTasks.length; i++)
                                    componentList[i] = currentTasks[i].task;
                                tlc.setCurrentTasks(componentList);
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

    class TaskPriorityComparator implements Comparator<WrappedTask> {

        /**
         * @see java.util.Comparator#compare(T, T)
         */
        public int compare(WrappedTask arg0, WrappedTask arg1) {
            int result;
            result = arg0.task.getPriority().compareTo(arg1.task.getPriority());
            if (result == 0)
                result = arg0.addedTime.compareTo(arg1.addedTime);
            return result;
        }

    }

}
