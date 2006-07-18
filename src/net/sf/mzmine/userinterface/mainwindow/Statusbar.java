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

package net.sf.mzmine.userinterface.mainwindow;

import java.awt.Dimension;
import java.awt.Graphics;
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EtchedBorder;

public class Statusbar extends JPanel {

    private JPanel statusTextPanel;
    private JLabel statusTextLabel;
    private JProgressBar statusProgBar;

    private final int statusBarHeight = 25;

    public Statusbar(MainWindow _mainWin) {

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(new EtchedBorder());

        statusTextPanel = new JPanel();
        statusTextPanel.setLayout(new BoxLayout(statusTextPanel,
                BoxLayout.X_AXIS));
        statusTextPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));

        statusProgBar = new JProgressBar();
        statusProgBar.setMinimumSize(new Dimension(100, statusBarHeight));
        statusProgBar.setPreferredSize(new Dimension(3200, statusBarHeight));
        statusProgBar.setVisible(false);

        statusTextLabel = new JLabel();
        statusTextLabel.setMinimumSize(new Dimension(100, statusBarHeight));
        statusTextLabel.setPreferredSize(new Dimension(3200, statusBarHeight));

        statusTextPanel.add(Box.createRigidArea(new Dimension(5,
                statusBarHeight)));
        statusTextPanel.add(statusTextLabel);
        statusTextPanel.add(statusProgBar);
        statusTextPanel.add(Box.createRigidArea(new Dimension(5,
                statusBarHeight)));

        add(statusTextPanel);

    }

    /**
     * Set the text displayed in status bar
     * 
     * @param t
     *            Text for status bar
     */
    void setStatusText(String statusText) {
        statusTextLabel.setText(statusText);

    }

    /**
     * Sets the text displayed in status bar and progress indicators position
     * 
     * @param t
     *            Text for status bar
     * @param currentSlot
     *            Current position of progress indicator
     * @param fullSlot
     *            Last position of progress indicator
     */
    public void setStatusProgBar(int _currentSlot, int _fullSlot) {
        // if (!statusProgBar.isVisible()) {
        statusProgBar.setVisible(true);
        // }
        statusProgBar.setMaximum(_fullSlot);
        statusProgBar.setValue(_currentSlot);

        // statusTextPanel.update(statusTextPanel.getGraphics());
    }

    public void disableProgBar() {
        statusProgBar.setVisible(false);
    }

}