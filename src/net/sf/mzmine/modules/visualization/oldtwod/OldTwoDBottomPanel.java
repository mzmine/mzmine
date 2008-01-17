/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.oldtwod;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.sf.mzmine.util.GUIUtils;

/**
 * Spectra visualizer's bottom panel
 */
class OldTwoDBottomPanel extends JPanel {

    public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

    private JComboBox peakListSelector;

    OldTwoDBottomPanel(OldTwoDVisualizerWindow masterFrame) {

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        setBackground(Color.white);

        add(Box.createHorizontalStrut(10));

        add(Box.createHorizontalGlue());

        GUIUtils.addLabel(this, "Peak list: ", SwingConstants.RIGHT);

        peakListSelector = new JComboBox();
        peakListSelector.setEnabled(false);
        peakListSelector.setBackground(Color.white);
        peakListSelector.setFont(smallFont);
        peakListSelector.addActionListener(masterFrame);
        peakListSelector.setActionCommand("PEAKLIST_CHANGE");
        add(peakListSelector);

        add(Box.createHorizontalGlue());

        add(Box.createHorizontalStrut(10));

    }


    JComboBox getPeakListSelector() {
        return peakListSelector;
    }
}
