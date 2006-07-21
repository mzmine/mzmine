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

package net.sf.mzmine.userinterface.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.util.GUIUtils;

/**
 * 
 */
public class TaskProgressWindow extends JInternalFrame implements
        ActionListener {

    private JTable taskTable;
    private TaskControllerImpl taskController;

    // popup menu
    private JPopupMenu popupMenu;
    private JMenu priorityMenu;
    private JMenuItem cancelTaskMenuItem;
    private JMenuItem highPriorityMenuItem;
    private JMenuItem normalPriorityMenuItem;
    private JMenuItem lowPriorityMenuItem;

    /**
     * 
     */
    public TaskProgressWindow(TaskControllerImpl taskController) {
        super("Tasks in progress...", true, true, true, true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        this.taskController = taskController;
        taskTable = new JTable(taskController.getTaskTableModel());
        taskTable.setCellSelectionEnabled(false);
        taskTable.setColumnSelectionAllowed(false);
        taskTable.setRowSelectionAllowed(true);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane jJobScroll = new JScrollPane(taskTable);
        add(jJobScroll, java.awt.BorderLayout.CENTER);

        // create popup menu items

        popupMenu = new JPopupMenu();

        priorityMenu = new JMenu("Set priority...");
        highPriorityMenuItem = GUIUtils.addMenuItem(priorityMenu, "High", this);
        normalPriorityMenuItem = GUIUtils.addMenuItem(priorityMenu, "Normal",
                this);
        lowPriorityMenuItem = GUIUtils.addMenuItem(priorityMenu, "Low", this);
        popupMenu.add(priorityMenu);

        cancelTaskMenuItem = GUIUtils.addMenuItem(popupMenu, "Cancel task",
                this);

        taskTable.setComponentPopupMenu(popupMenu);

        // set the width for first column (task description)
        taskTable.getColumnModel().getColumn(0).setPreferredWidth(350);

        pack();

        // set position and size
        setBounds(120, 30, 600, 150);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();
        Task selectedTask;

        int selectedRow = taskTable.getSelectedRow();
        selectedTask = taskController.getTask(selectedRow);

        if (selectedTask == null)
            return;

        if (src == cancelTaskMenuItem) {
            selectedTask.cancel();
        }

        if (src == highPriorityMenuItem) {
            taskController.setTaskPriority(selectedTask, TaskPriority.HIGH);
        }

        if (src == normalPriorityMenuItem) {
            taskController.setTaskPriority(selectedTask, TaskPriority.NORMAL);
        }

        if (src == lowPriorityMenuItem) {
            taskController.setTaskPriority(selectedTask, TaskPriority.LOW);
        }

    }

}
