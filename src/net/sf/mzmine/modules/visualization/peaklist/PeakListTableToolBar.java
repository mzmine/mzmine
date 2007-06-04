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

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

/**
 *
 */
class PeakListTableToolBar extends JToolBar {
 
    private JButton showSpectrumButton;
    private JButton showXICButton;
    private JButton showAlignmentRowsButton;

    static final Icon zoomToPeakIcon = new ImageIcon("icons/annotationsicon.png");
    static final Icon changeFormatIcon = new ImageIcon("icons/tableselectionicon.png");
    static final Icon printIcon = new ImageIcon("icons/printicon.png");
    static final Icon showSpectrumIcon = new ImageIcon("icons/spectrumicon.png");
    static final Icon showXICIcon = new ImageIcon("icons/xicicon.png");
    static final Icon showAlignmentRowsIcon = new ImageIcon("icons/tableselectionicon.png");

    PeakListTableToolBar(PeakListTableWindow masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        GUIUtils.addButton(this, null, zoomToPeakIcon, masterFrame,
                "ZOOM_TO_PEAK", "Zoom visualizers to selected peak");
        
        addSeparator();
        
        GUIUtils.addButton(this, null, changeFormatIcon, masterFrame,
                "CHANGE_FORMAT", "Change table column format");
        
        addSeparator();
        
        GUIUtils.addButton(this, null, printIcon, masterFrame,
                "PRINT", "Print");
        
        showSpectrumButton = new JButton(showSpectrumIcon);
        showSpectrumButton.setActionCommand("SHOW_SPECTRUM_FOR_PEAK");
        showSpectrumButton.setToolTipText("Show spectrum for selected peak");
        showSpectrumButton.setEnabled(true);
        showSpectrumButton.addActionListener(masterFrame);

        showXICButton = new JButton(showXICIcon);
        showXICButton.setActionCommand("SHOW_XIC_FOR_PEAK");
        showXICButton.setToolTipText("Show XIC for selected peak");
        showXICButton.setEnabled(true);
        showXICButton.addActionListener(masterFrame);

        showAlignmentRowsButton = new JButton(showAlignmentRowsIcon);
        showAlignmentRowsButton.setActionCommand("SHOW_ALIGNMENTS_FOR_PEAK");
        showAlignmentRowsButton.setToolTipText("Show peak in alignment result table(s)");
        showAlignmentRowsButton.setEnabled(true);
        showAlignmentRowsButton.addActionListener(masterFrame);
        
        add(showSpectrumButton);
        addSeparator();
        add(showXICButton);
        addSeparator();
        add(showAlignmentRowsButton);

    }

}
