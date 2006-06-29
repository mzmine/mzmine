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

package net.sf.mzmine.visualizers.rawdata.spectra;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.visualizers.rawdata.threed.ThreeDVisualizer;
import net.sf.mzmine.visualizers.rawdata.tic.TICSetup;

/**
 * 
 */
public class SpectraSetup extends JDialog implements ActionListener {

    private RawDataFile rawDataFile;
    
    // Buttons
    private JButton btnOK;
    private JButton btnCancel;

    // Panels for all above
    private JPanel pnlAll;
    private JPanel pnlLabels;
    private JPanel pnlFields;
    private JPanel pnlButtons;
    
    SpectraSetup(RawDataFile rawDataFile) {
        
        // Make dialog modal
        super(MainWindow.getInstance(), "Spectra visualizer parameters (" + rawDataFile + ")", true);
        
        
        this.rawDataFile = rawDataFile;
        
        // Buttons
        pnlButtons = new JPanel();
        btnOK = new JButton("OK");
        btnOK.addActionListener(this);
        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this);
        pnlButtons.add(btnOK);
        pnlButtons.add(btnCancel);
        
        pnlAll = new JPanel();
       // pnlAll.add(pnlLabels,BorderLayout.CENTER);
        //pnlAll.add(pnlFields,BorderLayout.LINE_END);
        pnlAll.add(pnlButtons,BorderLayout.SOUTH);

        getContentPane().add(pnlAll);
        
        pack();
        
        setLocationRelativeTo(MainWindow.getInstance());
        
        
    }
    
    /**
     * Show a new setup dialog
     * @param rawDataFile
     */
    public static void showSetupDialog(RawDataFile rawDataFile) {

        SpectraSetup setupDialog = new SpectraSetup(rawDataFile);
        setupDialog.setVisible(true);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(java.awt.event.ActionEvent ae) {
        Object src = ae.getSource();
        
        if (src == btnOK) {
            
            int scanNumbers[] = null;
            
            new SpectrumVisualizer(rawDataFile, scanNumbers);
        
        }
        
        dispose();
        
    }
}
