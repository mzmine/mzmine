/**
 * 
 */
package net.sf.mzmine.taskcontrol.impl;

import java.util.Arrays;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class TaskQueue extends AbstractTableModel {

    private static final int DEFAULT_CAPACITY = 64;

    private WrappedTask[] queue;
    private int size, capacity;

    TaskQueue() {

        size = 0;
        capacity = DEFAULT_CAPACITY;
        queue = new WrappedTask[capacity];

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
        fireTableDataChanged();

    }

    synchronized void removeWrappedTask(WrappedTask task) {

        for (int i = 0; i < size; i++) {
            if (queue[i] == task) {
                for (int j = i + 1; j < size; j++)
                    queue[j - 1] = queue[j];
                size--;
                break;
            }
        }

        if ((size < capacity / 2) && (size > DEFAULT_CAPACITY)) {
            capacity /= 2;
            WrappedTask[] temp = new WrappedTask[capacity];
            // copy the old queue to the new one
            for (int i = 0; i < size; i++)
                temp[i] = queue[i];
            queue = temp;
        }

        resort();
        fireTableDataChanged();

    }

    synchronized void resort() {
        
        Arrays.sort(queue, 0, size);

    }

    synchronized void refresh() {
        
        resort();
        fireTableRowsUpdated(0, size - 1);
        
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

    synchronized WrappedTask[] getQueueSnapshot() {
        
        WrappedTask[] snapshot = new WrappedTask[size];
        for (int i = 0; i < size; i++)
            snapshot[i] = queue[i];
        return snapshot;
        
    }

    /* TableModel implementation */

    private final String columns[] = { "Item", "Priority", "Status", "% done" };

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
                return String.valueOf(Math.round(task.getTask()
                        .getFinishedPercentage() * 100))
                        + "%";
            }
        }

        return null;

    }

}
