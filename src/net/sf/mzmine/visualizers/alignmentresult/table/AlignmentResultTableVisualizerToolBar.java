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

package net.sf.mzmine.visualizers.alignmentresult.table;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

/**
 *
 */
class AlignmentResultTableVisualizerToolBar extends JToolBar {

    private JButton zoomToPeakButton;
    private JButton changeFormatButton;

    static final Icon zoomToPeakIcon = new ImageIcon("icons/annotationsicon.png");
    static final Icon changeFormatIcon = new ImageIcon("icons/tableselectionicon.png");

    AlignmentResultTableVisualizerToolBar(AlignmentResultTableVisualizerWindow masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        zoomToPeakButton = new JButton(zoomToPeakIcon);
        zoomToPeakButton.setActionCommand("ZOOM_TO_PEAK");
        zoomToPeakButton.setToolTipText("Zoom visualizers to selected peak");
        zoomToPeakButton.setEnabled(true);
        zoomToPeakButton.addActionListener(masterFrame);

        changeFormatButton = new JButton(changeFormatIcon);
        changeFormatButton.setActionCommand("CHANGE_FORMAT");
        changeFormatButton.setToolTipText("Change table column format");
        changeFormatButton.setEnabled(true);
        changeFormatButton.addActionListener(masterFrame);

        add(zoomToPeakButton);
        addSeparator();
        add(changeFormatButton);

    }

}
