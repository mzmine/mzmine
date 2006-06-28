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

package net.sf.mzmine.visualizers.peaklist.table;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

/**
 *
 */
class TableViewToolBar extends JToolBar {

    private JButton zoomToPeakButton, findInAlignmentsButton;

    static final Icon zoomToPeakIcon = new ImageIcon("annotationsicon.png");
    static final Icon findInAlignmentsIcon = new ImageIcon("tableselectionicon.png");

    TableViewToolBar(TableView masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        zoomToPeakButton = new JButton(zoomToPeakIcon);
        zoomToPeakButton.setActionCommand("ZOOM_TO_PEAK");
        zoomToPeakButton.setToolTipText("Zoom visualizers to selected peak");
        zoomToPeakButton.setEnabled(true);
        zoomToPeakButton.addActionListener(masterFrame);

        findInAlignmentsButton = new JButton(findInAlignmentsIcon);
        findInAlignmentsButton.setActionCommand("FIND_IN_ALIGNMENTS");
        findInAlignmentsButton.setToolTipText("Find peak in alignments");
        findInAlignmentsButton.setEnabled(true);
        findInAlignmentsButton.addActionListener(masterFrame);

        add(zoomToPeakButton);
        addSeparator();
        add(findInAlignmentsButton);

    }

}
