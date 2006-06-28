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

package net.sf.mzmine.visualizers.rawdata.threed;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.plaf.metal.MetalIconFactory;

/**
 * 
 */
class ThreeDToolBar extends JToolBar {
    
    private JButton propertiesButton, annotationsButton;
    
    static final Icon propertiesIcon = MetalIconFactory.getTreeComputerIcon();
    static final Icon annotationsIcon = new ImageIcon("annotationsicon.png");

    ThreeDToolBar(ThreeDVisualizer masterFrame) {

        super(JToolBar.VERTICAL);

        setFloatable(false);
        setFocusable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        propertiesButton = new JButton(propertiesIcon);
        propertiesButton.setActionCommand("PROPERTIES");
        propertiesButton.setToolTipText("Set properties");
        propertiesButton.addActionListener(masterFrame);
        
        annotationsButton = new JButton(annotationsIcon);
        annotationsButton.setActionCommand("SHOW_ANNOTATIONS");
        annotationsButton.setToolTipText("Toggle displaying of peak values");
        annotationsButton.addActionListener(masterFrame);
        
        add(propertiesButton);
        addSeparator();
        add(annotationsButton);
        

    }

}
