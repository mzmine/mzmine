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

package net.sf.mzmine.desktop.impl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.taskcontrol.impl.WrappedTask;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.ComponentCellRenderer;

/**
 * This class represents a window with a table of running tasks
 */
public class TaskProgressTable extends JPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private JTable taskTable;

    private JPopupMenu popupMenu;
    private JMenu priorityMenu;
    private JMenuItem cancelTaskMenuItem, cancelAllMenuItem,
	    highPriorityMenuItem, normalPriorityMenuItem;

    /**
     * Constructor
     */
    public TaskProgressTable() {

	super(new BorderLayout());

	add(new JLabel("Tasks in progress..."), BorderLayout.NORTH);

	TaskControllerImpl taskController = (TaskControllerImpl) MZmineCore
		.getTaskController();

	taskTable = new JTable(taskController.getTaskQueue());
	taskTable.setCellSelectionEnabled(false);
	taskTable.setColumnSelectionAllowed(false);
	taskTable.setRowSelectionAllowed(true);
	taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	taskTable.setDefaultRenderer(JComponent.class,
		new ComponentCellRenderer());
	taskTable.getTableHeader().setReorderingAllowed(false);

	JScrollPane jJobScroll = new JScrollPane(taskTable);
	add(jJobScroll, BorderLayout.CENTER);

	// Create popup menu and items
	popupMenu = new JPopupMenu();

	priorityMenu = new JMenu("Set priority...");
	highPriorityMenuItem = GUIUtils.addMenuItem(priorityMenu, "High", this);
	normalPriorityMenuItem = GUIUtils.addMenuItem(priorityMenu, "Normal",
		this);
	popupMenu.add(priorityMenu);

	cancelTaskMenuItem = GUIUtils.addMenuItem(popupMenu, "Cancel task",
		this);
	cancelAllMenuItem = GUIUtils.addMenuItem(popupMenu, "Cancel all tasks",
		this);

	// Addd popup menu to the task table
	taskTable.setComponentPopupMenu(popupMenu);

	// Set the width for first column (task description)
	taskTable.getColumnModel().getColumn(0).setPreferredWidth(350);

	jJobScroll.setPreferredSize(new Dimension(600, 120));

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

	TaskControllerImpl taskController = (TaskControllerImpl) MZmineCore
		.getTaskController();

	WrappedTask currentQueue[] = taskController.getTaskQueue()
		.getQueueSnapshot();

	Task selectedTask = null;

	int selectedRow = taskTable.getSelectedRow();

	if ((selectedRow < currentQueue.length) && (selectedRow >= 0))
	    selectedTask = currentQueue[selectedRow].getActualTask();

	Object src = event.getSource();

	if (src == cancelTaskMenuItem) {
	    if (selectedTask == null)
		return;
	    TaskStatus status = selectedTask.getStatus();
	    if ((status == TaskStatus.WAITING)
		    || (status == TaskStatus.PROCESSING)) {
		selectedTask.cancel();
	    }
	}

	if (src == cancelAllMenuItem) {
	    for (WrappedTask wrappedTask : currentQueue) {
		Task task = wrappedTask.getActualTask();
		TaskStatus status = task.getStatus();
		if ((status == TaskStatus.WAITING)
			|| (status == TaskStatus.PROCESSING)) {
		    task.cancel();
		}
	    }
	}

	if (src == highPriorityMenuItem) {
	    if (selectedTask == null)
		return;
	    taskController.setTaskPriority(selectedTask, TaskPriority.HIGH);
	}

	if (src == normalPriorityMenuItem) {
	    if (selectedTask == null)
		return;
	    taskController.setTaskPriority(selectedTask, TaskPriority.NORMAL);
	}

    }

}
