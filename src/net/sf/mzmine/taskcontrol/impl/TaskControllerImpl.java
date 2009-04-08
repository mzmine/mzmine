/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.taskcontrol.impl;

import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Task controller implementation
 */
public class TaskControllerImpl implements TaskController, Runnable {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Update the task progress window every 300 ms
	 */
	private final int TASKCONTROLLER_THREAD_SLEEP = 300;

	private Thread taskControllerThread;

	private TaskQueue taskQueue;
	private TaskProgressWindow taskWindow;

	private Vector<WorkerThread> runningThreads;
	private int maxRunningThreads;

	/**
	 * Initialize the task controller
	 */
	public void initModule() {

		taskQueue = new TaskQueue();

		runningThreads = new Vector<WorkerThread>();
		maxRunningThreads = Runtime.getRuntime().availableProcessors();

		// Create a low-priority thread that will manage the queue and start
		// worker threads for tasks
		taskControllerThread = new Thread(this, "Task controller thread");
		taskControllerThread.setPriority(Thread.MIN_PRIORITY);
		taskControllerThread.start();

		// Create the task progress window and add it to desktop
		taskWindow = new TaskProgressWindow();
		MZmineCore.getDesktop().addInternalFrame(taskWindow);
		
		// Initially, hide the task progress window
		taskWindow.setVisible(false);

	}

	TaskQueue getTaskQueue() {
		return taskQueue;
	}

	public void addTask(Task task) {
		addTasks(new Task[] { task }, TaskPriority.NORMAL);
	}

	public void addTask(Task task, TaskPriority priority) {
		addTasks(new Task[] { task }, priority);
	}

	public void addTasks(Task tasks[]) {
		addTasks(tasks, TaskPriority.NORMAL);
	}

	public void addTasks(Task tasks[], TaskPriority priority) {

		assert tasks != null;
		assert tasks.length >= 1;

		for (Task task : tasks) {
			WrappedTask newQueueEntry = new WrappedTask(task, priority);
			taskQueue.addWrappedTask(newQueueEntry);
		}

		// Wake up the task controller thread
		synchronized (this) {
			this.notifyAll();
		}

		// Show the task list component
		taskWindow.setVisible(true);

	}

	/**
	 * Task controller thread main method.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		while (true) {

			// If the queue is empty, we can sleep. When new task is added into
			// the queue, we will be awaken by notify()
			synchronized (this) {
				while (taskQueue.isEmpty()) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						// Ignore
					}
				}
			}

			// Check if all tasks in the queue are finished
			if (taskQueue.allTasksFinished()) {
				taskWindow.setVisible(false);
				taskQueue.clear();
				continue;
			}

			// Remove already finished threads from runningThreads
			Iterator<WorkerThread> threadIterator = runningThreads.iterator();
			while (threadIterator.hasNext()) {
				WorkerThread thread = threadIterator.next();
				if (thread.isFinished())
					threadIterator.remove();
			}

			// Get a snapshot of the queue
			WrappedTask[] queueSnapshot = taskQueue.getQueueSnapshot();

			// Check all tasks in the queue
			for (WrappedTask task : queueSnapshot) {

				// Skip assigned and canceled tasks
				if (task.isAssigned()
						|| (task.getActualTask().getStatus() == TaskStatus.CANCELED))
					continue;

				// Create a new thread if the task is high-priority or if we
				// have less then maximum # of threads running
				if ((task.getPriority() == TaskPriority.HIGH)
						|| (runningThreads.size() < maxRunningThreads)) {
					WorkerThread newThread = new WorkerThread(task);
					runningThreads.add(newThread);
					newThread.start();
				}
			}

			// Tell the queue to refresh the Task progress window
			taskQueue.refresh();

			// Sleep for a while until next update
			try {
				Thread.sleep(TASKCONTROLLER_THREAD_SLEEP);
			} catch (InterruptedException e) {
				// Ignore
			}

		}

	}

	public void setTaskPriority(Task task, TaskPriority priority) {

		// Get a snapshot of current task queue
		WrappedTask currentQueue[] = taskQueue.getQueueSnapshot();

		// Find the requested task
		for (WrappedTask wrappedTask : currentQueue) {

			if (wrappedTask.getActualTask() == task) {
				logger.finest("Setting priority of task \""
						+ task.getTaskDescription() + "\" to " + priority);
				wrappedTask.setPriority(priority);

				// Call refresh to re-sort the queue according to new priority
				// and update the Task progress window
				taskQueue.refresh();
			}
		}
	}

}
