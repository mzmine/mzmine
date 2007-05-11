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
 
package net.sf.mzmine.modules.visualization.rawdata.threed;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.sf.mzmine.userinterface.Desktop;
import visad.DisplayImpl;
import visad.ScalarMap;
import visad.util.ColorMapWidget;
import visad.util.GMCWidget;


/**
 *
 */
class ThreeDPropertiesDialog extends JDialog implements ActionListener {

    private static final String TITLE = "3D visualizer properties"; 
    
    private GMCWidget gmcWidget;
    private ColorMapWidget colorWidget;

    public ThreeDPropertiesDialog(Desktop desktop, DisplayImpl display) {
        
        super(desktop.getMainFrame(), TITLE, true);

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        
        try {
            
            ScalarMap colorMap = (ScalarMap) display.getMapVector().get(4);
            colorWidget = new ColorMapWidget(colorMap);
            JLabel colorLabel = new JLabel("Color mapping", SwingConstants.CENTER);
            add(colorLabel);
            add(colorWidget);
         
            JLabel gmcLabel = new JLabel("Graphics mode control", SwingConstants.CENTER);

            gmcWidget = new GMCWidget(display.getGraphicsModeControl());
            add(gmcLabel);
            add(gmcWidget);
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        add(new JSeparator());
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(this);
        add(okButton);
        
        pack();
        setLocationRelativeTo(desktop.getMainFrame());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
        dispose();
    }

}
