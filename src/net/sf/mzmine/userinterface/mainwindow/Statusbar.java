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
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

public class Statusbar extends JPanel implements Runnable, MouseListener {

    // frequency in milliseconds how often to update free memory label
    public static final int MEMORY_LABEL_UPDATE_FREQUENCY = 1000;
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private JPanel statusTextPanel, memoryPanel;
    private JLabel statusTextLabel, memoryLabel;

    private final int statusBarHeight = 25;

    Statusbar() {

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(new EtchedBorder());

        statusTextPanel = new JPanel();
        statusTextPanel.setLayout(new BoxLayout(statusTextPanel,
                BoxLayout.X_AXIS));
        statusTextPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));

        statusTextLabel = new JLabel();
        statusTextLabel.setMinimumSize(new Dimension(100, statusBarHeight));
        statusTextLabel.setPreferredSize(new Dimension(3200, statusBarHeight));

        statusTextPanel.add(Box.createRigidArea(new Dimension(5,
                statusBarHeight)));
        statusTextPanel.add(statusTextLabel);

        add(statusTextPanel);

        memoryLabel = new JLabel("");
        memoryPanel = new JPanel();
        memoryPanel.setLayout(new BoxLayout(memoryPanel, BoxLayout.X_AXIS));
        memoryPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        memoryPanel.add(Box.createRigidArea(new Dimension(10, statusBarHeight)));
        memoryPanel.add(memoryLabel);
        memoryPanel.add(Box.createRigidArea(new Dimension(10, statusBarHeight)));

        memoryLabel.addMouseListener(this);
        
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
            memoryLabel.setToolTipText("JVM memory: " + freeMem + "MB free, "
                    + totalMem + "MB total");

            try {
                wait(MEMORY_LABEL_UPDATE_FREQUENCY);
            } catch (InterruptedException e) {
                // ignore
            }

        }

    }

    public void mouseClicked(MouseEvent arg0) {
        logger.info("Running garbage collector");
        System.gc();
        
    }

}