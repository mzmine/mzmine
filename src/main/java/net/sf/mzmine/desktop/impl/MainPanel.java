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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import net.sf.mzmine.desktop.impl.projecttree.ProjectTree;

/**
 * This class is the main window of application
 * 
 */
public class MainPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ProjectTree rawDataTree, peakListTree;
    private TaskProgressTable taskTable;

    /**
     */
    public MainPanel() {

	super(new BorderLayout());

	// Initialize item selector
	rawDataTree = new ProjectTree();
	peakListTree = new ProjectTree();

	JScrollPane rawDataTreeScroll = new JScrollPane(rawDataTree);
	JScrollPane peakListTreeScroll = new JScrollPane(peakListTree);

	JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	split.add(rawDataTreeScroll);
	split.add(peakListTreeScroll);
	split.setResizeWeight(0.5);

	split.setMinimumSize(new Dimension(200, 200));

	add(split, BorderLayout.CENTER);

	taskTable = new TaskProgressTable();
	add(taskTable, BorderLayout.SOUTH);

    }

    public ProjectTree getRawDataTree() {
	return rawDataTree;
    }

    public ProjectTree getPeakListTree() {
	return peakListTree;
    }

    public TaskProgressTable getTaskTable() {
	return taskTable;
    }

}
