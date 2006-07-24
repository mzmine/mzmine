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

package net.sf.mzmine.batchmode;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.visualizers.rawdata.twod.TwoDVisualizerWindow;

class BatchModeDialog extends JDialog implements ActionListener {

    static final int PADDING_SIZE = 5;
    
    private BatchMode batchModeModule;
    
    
    // dialog components
    private JButton btnOK, btnCancel;
    
    public BatchModeDialog(BatchMode batchModeModule, MZmineCore core, OpenedRawDataFile dataFiles[]) {

        // Make dialog modal
        super(core.getDesktop().getMainFrame(), "Batch mode parameters", true);
        
        this.batchModeModule = batchModeModule;
        
        GridBagConstraints constraints = new GridBagConstraints();
        
        GridBagLayout layout = new GridBagLayout();

        JPanel components = new JPanel(layout);
        
        
        JPanel buttonsPanel = new JPanel();
        btnOK = GUIUtils.addButton(buttonsPanel, "OK", null, this);
        btnCancel = GUIUtils.addButton(buttonsPanel, "Cancel", null, this);
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        components.add(buttonsPanel, constraints);
        
        GUIUtils.addMargin(components, PADDING_SIZE);
        add(components);
        
        // finalize the dialog
        pack();
        setLocationRelativeTo(core.getDesktop().getMainFrame());
        setResizable(false);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();
        
        if (src == btnOK) {
            
            dispose();

        }

        if (src == btnCancel) {
            dispose();
        }
        
    }

}