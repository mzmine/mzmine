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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.util.components.LabeledProgressBar;

public class StatusBar extends JPanel implements Runnable, MouseListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    // frequency in milliseconds how often to update free memory label
    public static final int MEMORY_LABEL_UPDATE_FREQUENCY = 1000;
    public static final int STATUS_BAR_HEIGHT = 20;
    public static final Font statusBarFont = new Font("SansSerif", Font.PLAIN,
	    12);

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private JPanel statusTextPanel, memoryPanel;
    private JLabel statusTextLabel;
    private LabeledProgressBar memoryLabel;

    public StatusBar() {

	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	setBorder(new EtchedBorder());

	statusTextPanel = new JPanel();
	statusTextPanel.setLayout(new BoxLayout(statusTextPanel,
		BoxLayout.X_AXIS));
	statusTextPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));

	statusTextLabel = new JLabel();
	statusTextLabel.setFont(statusBarFont);
	statusTextLabel.setMinimumSize(new Dimension(100, STATUS_BAR_HEIGHT));
	statusTextLabel
		.setPreferredSize(new Dimension(3200, STATUS_BAR_HEIGHT));

	statusTextPanel.add(Box.createRigidArea(new Dimension(5,
		STATUS_BAR_HEIGHT)));
	statusTextPanel.add(statusTextLabel);

	add(statusTextPanel);

	memoryLabel = new LabeledProgressBar();
	memoryPanel = new JPanel();
	memoryPanel.setLayout(new BoxLayout(memoryPanel, BoxLayout.X_AXIS));
	memoryPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
	memoryPanel.add(Box
		.createRigidArea(new Dimension(10, STATUS_BAR_HEIGHT)));
	memoryPanel.add(memoryLabel);
	memoryPanel.add(Box
		.createRigidArea(new Dimension(10, STATUS_BAR_HEIGHT)));

	memoryLabel.addMouseListener(this);

	add(memoryPanel);

	Thread memoryLabelUpdaterThread = new Thread(this,
		"Memory label updater thread");
	memoryLabelUpdaterThread.start();

    }

    /**
     * Set the text displayed in status bar
     * 
     * @param statusText
     *            Text for status bar
     * @param textColor
     *            Text color
     */
    public void setStatusText(String statusText, Color textColor) {
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
	    double fullMem = ((double) (totalMem - freeMem)) / totalMem;

	    memoryLabel.setValue(fullMem, freeMem + "MB free");
	    memoryLabel.setToolTipText("JVM memory: " + freeMem + "MB, "
		    + totalMem + "MB total");

	    try {
		wait(MEMORY_LABEL_UPDATE_FREQUENCY);
	    } catch (InterruptedException e) {
		// ignore
	    }

	}

    }

    public void mouseClicked(MouseEvent arg0) {
	// Run garbage collector on a new thread, so it does not block the GUI
	new Thread(new Runnable() {
	    @Override
	    public void run() {
		logger.info("Running garbage collector");
		System.gc();
	    }
	}).start();
    }

}