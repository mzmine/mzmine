/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.taskcontrol.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.util.components.LabeledProgressBar;

/**
 * Task queue
 */
class TaskQueue implements TableModel {

	private static final int DEFAULT_CAPACITY = 64;

	private WrappedTask[] queue;
	private int size, capacity;

	private HashSet<TableModelListener> listeners;

	TaskQueue() {

		size = 0;
		capacity = DEFAULT_CAPACITY;
		queue = new WrappedTask[capacity];
		listeners = new HashSet<TableModelListener>();

	}

	synchronized void addWrappedTask(WrappedTask task) {

		if (size == capacity) {
			capacity *= 2;
			WrappedTask[] temp = new WrappedTask[capacity];
			// copy the old queue to the new one
			for (int i = 0; i < queue.length; i++)
				temp[i] = queue[i];
			queue = temp;
		}

		queue[size] = task;
		size++;

		resort();
		fireRowsChanged();

	}

	synchronized void clear() {
		size = 0;
		capacity = DEFAULT_CAPACITY;
		queue = new WrappedTask[capacity];
		fireRowsChanged();
	}

	synchronized void resort() {
		Arrays.sort(queue, 0, size);
	}

	synchronized void refresh() {

		resort();

		fireRowsChanged();
	}

	synchronized WrappedTask getWrappedTask(Task t) {

		for (int i = 0; i < size; i++)
			if (queue[i].getTask() == t)
				return queue[i];

		return null;

	}

	synchronized WrappedTask getWrappedTask(int index) {

		if ((index < 0) || (index >= size))
			return null;
		else
			return queue[index];

	}

	synchronized boolean isEmpty() {
		return size == 0;
	}

	synchronized boolean allTasksFinished() {
		for (int i = 0; i < size; i++) {
			TaskStatus status = queue[i].getTask().getStatus();
			if ((status == TaskStatus.PROCESSING)
					|| (status == TaskStatus.WAITING))
				return false;
		}
		return true;
	}

	synchronized WrappedTask[] getQueueSnapshot() {

		WrappedTask[] snapshot = new WrappedTask[size];
		for (int i = 0; i < size; i++)
			snapshot[i] = queue[i];
		return snapshot;

	}

	private void fireRowsChanged() {

		TableModelEvent te = new TableModelEvent(this);

		Iterator<TableModelListener> listenerIterator = listeners.iterator();

		while (listenerIterator.hasNext()) {

			TableModelListener eventListener = listenerIterator.next();
			eventListener.tableChanged(te);

		}

	}

	/* TableModel implementation */

	private static final String columns[] = { "Item", "Priority", "Status",
			"% done" };

	/**
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount() {
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

			WrappedTask task = queue[row];

			switch (column) {
			case 0:
				return task.getTask().getTaskDescription();
			case 1:
				return task.getPriority();
			case 2:
				return task.getTask().getStatus();
			case 3:
				return new LabeledProgressBar(task.getTask()
						.getFinishedPercentage());
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

	/**
	 * @see javax.swing.table.TableModel#isCellEditable(int, int)
	 */
	public boolean isCellEditable(int row, int col) {
		return false;
	}

	/**
	 * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
	 */
	public void setValueAt(Object val, int row, int col) {
		// do nothing
	}

	/**
	 * @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
	 */
	public void addTableModelListener(TableModelListener listener) {
		listeners.add(listener);
	}

	/**
	 * @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
	 */
	public void removeTableModelListener(TableModelListener listener) {
		listeners.remove(listener);
	}

}
