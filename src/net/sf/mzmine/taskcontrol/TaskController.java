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
import java.util.Comparator;
import java.util.PriorityQueue;

import net.sf.mzmine.util.Logger;


/**
 * 
 */
public class TaskController implements Runnable {

    private final int TASKCONTROLLER_THREAD_SLEEP = 100;

    private Thread taskControllerThread;

    private Thread[] workerThreads;

    private PriorityQueue<AbstractTaskReference> taskQueue;

    /**
     * 
     */
    public TaskController(int numberOfThreads) {

        workerThreads = new Thread[numberOfThreads];
        taskQueue = new PriorityQueue<AbstractTaskReference>(10,
                new TaskPriorityComparator());

        taskControllerThread = new Thread(this, "Task controller thread");
        taskControllerThread.setPriority(Thread.MIN_PRIORITY);
        taskControllerThread.start();

    }

    public Task addTask(Task task) {

        assert task != null;

        AbstractTaskReference newReference = new AbstractTaskReference(task);

        Logger.put("Adding task " + task.getTaskDescription() + " to the task controller queue");
        
        synchronized (taskQueue) {
            taskQueue.add(newReference);
        }

        return newReference;

    }
    
    public InetAddress getTaskNode(DistributableTask task) {
        
        return null;
    }


    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        while (true) {
// TODO: always allocate a thread for high-priority tasks?
            
            /*
             * if the queue is not empty, poll local threads
             */
            synchronized (taskQueue) {
                
                if (!taskQueue.isEmpty()) {

                    for (int i = 0; i < workerThreads.length; i++) {

                        if ((workerThreads[i] == null)
                                || (workerThreads[i].getState() == Thread.State.TERMINATED)) {

                            AbstractTaskReference taskRef = taskQueue.poll();
                            if (taskRef != null) {
                                
                                Logger.put("Creating new thread for task " + taskRef.getTaskDescription());
                                workerThreads[i] = new Thread(taskRef,
                                        "Thread for task: "
                                                + taskRef.getTaskDescription());

                                if (taskRef.getPriority() == Task.TaskPriority.HIGH)
                                    workerThreads[i].setPriority(Thread.MAX_PRIORITY);

                                if (taskRef.getPriority() == Task.TaskPriority.LOW)
                                    workerThreads[i].setPriority(Thread.MIN_PRIORITY);

                                workerThreads[i].start();
                            }

                        }

                    }
                }

                /*
                 * if still not empty, poll the remote nodes, too
                 */
                if (!taskQueue.isEmpty()) {
                    // TODO: find DistributableTasks in the queue and poll remote nodes

                }

                // TODO: update status in GUI - include all worker threads + threads in the queue

            }
            
            try {
                Thread.sleep(TASKCONTROLLER_THREAD_SLEEP);
            } catch (InterruptedException e) {
                // do nothing
            }
        }

    }

    class TaskPriorityComparator implements Comparator<Task> {

        /**
         * @see java.util.Comparator#compare(T, T)
         */
        public int compare(Task arg0, Task arg1) {
            // TODO: think about this
            return arg0.getPriority().compareTo(arg1.getPriority());
        }

    }
}
