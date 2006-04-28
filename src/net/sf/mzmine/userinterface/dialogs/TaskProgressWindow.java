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

package net.sf.mzmine.userinterface.dialogs;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * 
 */
public class TaskProgressWindow extends JInternalFrame {

    private Task[] currentTasks;

    private JTable taskTable;
    private TaskModel taskModel;

    private class TaskModel extends AbstractTableModel {

        private final int NUM_COLUMNS = 4;

        public final String colDescription = "Item";

        public final String colNodeName = "Computer";

        public final String colJobStatus = "Status";

        public final String colJobRate = "% done";

        /**
         * @see javax.swing.table.TableModel#getRowCount()
         */
        public int getRowCount() {
            if (currentTasks == null)
                return 0;
            return currentTasks.length;
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
                return colNodeName;
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
            // the currentTasks array may be changed during the process
            try {
                Task task = currentTasks[row];
                switch (column) {
                case 0:
                    return task.getTaskDescription();
                case 1:
                    return "";
                case 2:
                    return task.getStatus();
                case 3:
                    return String.valueOf(Math.round(task
                            .getFinishedPercentage() * 100))
                            + "%";
                }
            } catch (Exception e) {
            }
            return "";
        }

    }

    public void setCurrentTasks(Task[] newTasks) {
        currentTasks = newTasks;
        taskModel.fireTableDataChanged();
        
    }

    
    
    /**
     * 
     */
    public TaskProgressWindow() {
        super("Tasks in progress...", true, true, true, true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        taskModel = new TaskModel();
        taskTable = new JTable(taskModel);
        taskTable.setColumnSelectionAllowed(false);
        taskTable.setRowSelectionAllowed(false);
        taskTable.setCellSelectionEnabled(false);
        JScrollPane jJobScroll = new JScrollPane(taskTable);
        add(jJobScroll, java.awt.BorderLayout.CENTER);
        // setSize(200, 200);
        taskTable.getColumnModel().getColumn(0).setPreferredWidth(350);
        pack();
        setBounds(120, 460, 600, 150);
        // setLocationRelativeTo(w.getDesktop());
        // setSize(630, 200);    
        JDesktopPane mainWinDesktop = MainWindow.getInstance().getDesktop(); 
        System.out.println(mainWinDesktop.getWidth() + " " + getWidth());
        // setLocation(mainWinDesktop.getWidth()/2-getWidth()/2, mainWinDesktop.getHeight()/2-getHeight()/2 );

        // mainWinDesktop.add(this);
        
    }

}
