/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

import java.util.Hashtable;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.components.LabeledProgressBar;

/**
 * This class stores all tasks (as WrappedTasks) in the queue of task controller
 * and also provides data for TaskProgressWindow (as TableModel).
 */
public class TaskQueue extends AbstractTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final int DEFAULT_CAPACITY = 64;

    /**
     * This array stores the actual tasks
     */
    private WrappedTask[] queue;

    /**
     * Current size of the queue
     */
    private int size;

    private Hashtable<Integer, LabeledProgressBar> progressBars;

    TaskQueue() {
	size = 0;
	queue = new WrappedTask[DEFAULT_CAPACITY];
	progressBars = new Hashtable<Integer, LabeledProgressBar>();
    }

    public synchronized int getNumOfWaitingTasks() {
	int numOfWaitingTasks = 0;
	for (int i = 0; i < size; i++) {
	    TaskStatus status = queue[i].getActualTask().getStatus();
	    if ((status == TaskStatus.PROCESSING)
		    || (status == TaskStatus.WAITING))
		numOfWaitingTasks++;
	}
	return numOfWaitingTasks;
    }

    synchronized void addWrappedTask(WrappedTask task) {

	logger.finest("Adding task \"" + task
		+ "\" to the task controller queue");

	// If the queue is full, make a bigger queue
	if (size == queue.length) {
	    WrappedTask[] newQueue = new WrappedTask[queue.length * 2];
	    System.arraycopy(queue, 0, newQueue, 0, size);
	    queue = newQueue;
	}

	queue[size] = task;
	size++;

	// Call fireTableDataChanged because we have a new row and order of rows
	// may have changed
	SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		fireTableDataChanged();
	    }
	});

    }

    synchronized void clear() {
	size = 0;
	queue = new WrappedTask[DEFAULT_CAPACITY];
	SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		fireTableDataChanged();
	    }
	});
    }

    /**
     * Refresh the queue (reorder the tasks according to priority) and send a
     * signal to Tasks in progress window to redraw updated data, such as task
     * status and finished percentages.
     */
    synchronized void refresh() {

	// We must not call fireTableDataChanged, because that would clear the
	// selection in the task window
	SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		fireTableRowsUpdated(0, size - 1);

	    }
	});

    }

    synchronized boolean isEmpty() {
	return size == 0;
    }

    synchronized boolean allTasksFinished() {
	for (int i = 0; i < size; i++) {
	    TaskStatus status = queue[i].getActualTask().getStatus();
	    if ((status == TaskStatus.PROCESSING)
		    || (status == TaskStatus.WAITING))
		return false;
	}
	return true;
    }

    public synchronized WrappedTask[] getQueueSnapshot() {
	WrappedTask[] snapshot = new WrappedTask[size];
	System.arraycopy(queue, 0, snapshot, 0, size);
	return snapshot;
    }

    /* TableModel implementation */

    private static final String columns[] = { "Item", "Priority", "Status",
	    "% done" };

    /**
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public synchronized int getRowCount() {
	return size;
    }

    /**
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() {
	return columns.length;
    }

    public String getColumnName(int column) {
	return columns[column];
    }

    /**
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public synchronized Object getValueAt(int row, int column) {

	if (row < size) {

	    WrappedTask wrappedTask = queue[row];
	    Task actualTask = wrappedTask.getActualTask();

	    switch (column) {
	    case 0:
		return actualTask.getTaskDescription();
	    case 1:
		return wrappedTask.getPriority();
	    case 2:
		return actualTask.getStatus();
	    case 3:
		double finishedPercentage = actualTask.getFinishedPercentage();
		LabeledProgressBar progressBar = progressBars.get(row);
		if (progressBar == null) {
		    progressBar = new LabeledProgressBar(finishedPercentage);
		    progressBars.put(row, progressBar);
		} else {
		    progressBar.setValue(finishedPercentage);
		}
		return progressBar;
	    }
	}

	return null;

    }

    /**
     * @see javax.swing.table.TableModel#getColumnClass(int)
     */
    public Class<?> getColumnClass(int column) {
	switch (column) {
	case 0:
	    return String.class;
	case 1:
	    return TaskPriority.class;
	case 2:
	    return TaskStatus.class;
	case 3:
	    return LabeledProgressBar.class;
	}
	return null;

    }

}
