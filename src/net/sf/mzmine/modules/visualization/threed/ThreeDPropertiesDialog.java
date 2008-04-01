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

package net.sf.mzmine.modules.visualization.threed;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.SwingConstants;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import visad.ScalarMap;
import visad.util.ColorMapWidget;
import visad.util.GMCWidget;

/**
 * 3D visualizer properties dialog
 */
class ThreeDPropertiesDialog extends JDialog implements ActionListener {

    private static final String title = "3D visualizer properties";

    private GMCWidget gmcWidget;
    private ColorMapWidget colorWidget;

    ThreeDPropertiesDialog(ThreeDDisplay display) {

        super(MZmineCore.getDesktop().getMainFrame(), title, true);

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        try {

            ScalarMap colorMap = (ScalarMap) display.getMapVector().get(4);

            GUIUtils.addLabel(this, "Color mapping", SwingConstants.CENTER);

            colorWidget = new ColorMapWidget(colorMap);
            add(colorWidget);

            GUIUtils.addLabel(this, "Graphics mode control",
                    SwingConstants.CENTER);

            gmcWidget = new GMCWidget(display.getGraphicsModeControl());
            add(gmcWidget);

        } catch (Exception e) {
            e.printStackTrace();
        }

        GUIUtils.addSeparator(this);

        GUIUtils.addButton(this, "OK", null, this);

        pack();
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
        dispose();
    }

}
