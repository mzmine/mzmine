/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.userinterface.mainwindow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;

import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.userinterface.components.TaskProgressWindow;

public class Statusbar extends JPanel implements MouseListener, Runnable {

    // frequency in milliseconds how often to update free memory label
    public static final int MEMORY_LABEL_UPDATE_FREQUENCY = 1000;

    private JPanel statusTextPanel, memoryPanel;
    private JLabel statusTextLabel, memoryLabel;
    private JProgressBar statusProgBar;
    private MainWindow mainWin;

    private final int statusBarHeight = 25;

    Statusbar() {


        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(new EtchedBorder());

        statusTextPanel = new JPanel();
        statusTextPanel.setLayout(new BoxLayout(statusTextPanel,
                BoxLayout.X_AXIS));
        statusTextPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));

        TaskControllerImpl tc = (TaskControllerImpl) MZmineCore.getTaskController();
        BoundedRangeModel progressModel = tc.getTaskQueue();
        statusProgBar = new JProgressBar(progressModel);
        statusProgBar.setMinimumSize(new Dimension(100, statusBarHeight));
        statusProgBar.setPreferredSize(new Dimension(500, statusBarHeight));
        statusProgBar.setToolTipText("Overall progress of all scheduled tasks");
        statusProgBar.setVisible(false);
        statusProgBar.addMouseListener(this);

        statusTextLabel = new JLabel();
        statusTextLabel.setMinimumSize(new Dimension(100, statusBarHeight));
        statusTextLabel.setPreferredSize(new Dimension(3200, statusBarHeight));

        statusTextPanel.add(Box.createRigidArea(new Dimension(5,
                statusBarHeight)));
        statusTextPanel.add(statusTextLabel);
        statusTextPanel.add(Box.createRigidArea(new Dimension(10,
                statusBarHeight)));
        statusTextPanel.add(statusProgBar);
        statusTextPanel.add(Box.createRigidArea(new Dimension(10,
                statusBarHeight)));

        add(statusTextPanel);

        memoryLabel = new JLabel("");
        memoryLabel.addMouseListener(this);
        memoryPanel = new JPanel();
        memoryPanel.setLayout(new BoxLayout(memoryPanel, BoxLayout.X_AXIS));
        memoryPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        memoryPanel.add(Box.createRigidArea(new Dimension(10, statusBarHeight)));
        memoryPanel.add(memoryLabel);
        memoryPanel.add(Box.createRigidArea(new Dimension(10, statusBarHeight)));

        add(memoryPanel);

        Thread memoryLabelUpdaterThread = new Thread(this,
                "Memory label updater thread");
        memoryLabelUpdaterThread.start();

    }

    /**
     * Set the text displayed in status bar
     * 
     * @param statusText Text for status bar
     * @param textColor Text color
     */
    void setStatusText(String statusText, Color textColor) {
        statusTextLabel.setText(statusText);
        statusTextLabel.setForeground(textColor);
    }

    public void setProgressBarVisible(boolean visible) {
        statusProgBar.setVisible(visible);
    }

    /**
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent event) {

        Object src = event.getSource();

        if (src == statusProgBar) {
            TaskProgressWindow taskProgressWindow = mainWin.getTaskList();
            taskProgressWindow.setVisible(true);
            try {
                taskProgressWindow.setSelected(true);
            } catch (PropertyVetoException e) {
                // do nothing
            }
        }

    }

    /**
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent event) {
        // do nothing

    }

    /**
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent event) {
        // do nothing

    }

    /**
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent event) {
        // do nothing

    }

    /**
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent event) {
        // do nothing

    }

    /**
     * @see java.lang.Runnable#run()
     */
    public synchronized void run() {

        while (true) {

            // get free memory in megabytes
            long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            
            memoryLabel.setText(freeMem + "MB free");
            memoryLabel.setToolTipText("JVM memory: " + freeMem + "MB free, " + totalMem + "MB total");
            
            try {
                wait(MEMORY_LABEL_UPDATE_FREQUENCY);
            } catch (InterruptedException e) {
                // ignore
            }

        }

    }

}