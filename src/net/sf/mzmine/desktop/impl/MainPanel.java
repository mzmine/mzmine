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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Arrays;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;

import ca.guydavis.swing.desktop.CascadingWindowPositioner;
import ca.guydavis.swing.desktop.WindowPositioner;

/**
 * This class is the main window of application
 * 
 */
public class MainPanel extends JPanel {

    private JDesktopPane desktopPane;
    private CascadingWindowPositioner windowPositioner;
    private JSplitPane split;
    private ProjectTree projectTree;
    private StatusBar statusBar;

    /**
     */
    public MainPanel() {

        super(new BorderLayout());

        // Initialize item selector
        projectTree = new ProjectTree();

        JScrollPane projectTreeScroll = new JScrollPane(projectTree);
        projectTreeScroll.setMinimumSize(new Dimension(200, 200));

        // Place objects on main window
        desktopPane = new JDesktopPane();
        desktopPane.setBackground(new Color(65, 105, 170));
        desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        desktopPane.setBorder(new EtchedBorder(EtchedBorder.RAISED));

        windowPositioner = new CascadingWindowPositioner(desktopPane);

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectTreeScroll,
                desktopPane);

        add(split, BorderLayout.CENTER);

        statusBar = new StatusBar();
        add(statusBar, BorderLayout.SOUTH);

    }

    public void addInternalFrame(JInternalFrame frame) {
        desktopPane.add(frame, JLayeredPane.DEFAULT_LAYER);
        frame.setVisible(true);
        Point location = windowPositioner.getPosition(frame,
                Arrays.asList(desktopPane.getAllFrames()));
        frame.setLocation(location.x, location.y);
    }

    public JDesktopPane getDesktopPane() {
        return desktopPane;
    }

    public ProjectTree getProjectTree() {
        return projectTree;
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }

    public WindowPositioner getWindowPositioner() {
        return windowPositioner;
    }

}
