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

package net.sf.mzmine.modules.peakpicking.twostep;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * 
 */
class TwoStepPickerSetupDialog extends JDialog implements ActionListener{

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private TwoStepPickerParameters parameters;
    private ExitCode exitCode = ExitCode.UNKNOWN;

    // Buttons
    protected JButton btnOK, btnCancel, btnSetMass, btnSetPeak;

    // Panels
    private JPanel pnlSuffix, pnlMassDetectors, pnlPeaksConstructors, pnlCombo, pnlButtons;
    
    //Combo Box
    JComboBox comboMassDetectors, comboPeaksConstructors;
    
    // Text Fields
    JTextField txtField;

    // Derived classed may add their components to this panel
    protected JPanel pnlAll;
    
    private RawDataFile dataFile;
    

    public TwoStepPickerSetupDialog(String title, TwoStepPickerParameters parameters, RawDataFile dataFile) { 
    	
        super(MZmineCore.getDesktop().getMainFrame(),
              "Please select mass detector  & peak builder", true);
       
        
        this.parameters = parameters;
        this.dataFile = dataFile;

        pnlSuffix = new JPanel(new BorderLayout());
        pnlSuffix.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));    	
        JLabel lblLabel = new JLabel("Filename suffix ");
        pnlSuffix.add(lblLabel, BorderLayout.WEST);
        txtField = new JTextField();
        txtField.setColumns(10);
        txtField.setText(parameters.getSuffix());
        pnlSuffix.add(txtField, BorderLayout.CENTER);       
        
    	
        // Check if there are any parameters
        String[] massDetectorNames = parameters.massDetectorNames;
        if ((massDetectorNames == null) || (massDetectorNames.length == 0)) {
            dispose();
        }

        String[] peakBuilderNames = parameters.peakBuilderNames;
        if ((peakBuilderNames == null) || (peakBuilderNames.length == 0)) {
            dispose();
        }
     
        // panels for mass detectors & peak builders
        pnlMassDetectors = new JPanel(new GridLayout(1, 3, 5, 0));
        comboMassDetectors = new JComboBox(parameters.massDetectorNames);
        comboMassDetectors.setSelectedIndex(parameters.getMassDetectorTypeNumber());
        comboMassDetectors.addActionListener(this);
       	btnSetMass = new JButton("Set parameters");
       	btnSetMass.addActionListener(this);
        JLabel lblMassDetectors = new JLabel("Mass Detector");
       	pnlMassDetectors.add(lblMassDetectors);
        pnlMassDetectors.add(comboMassDetectors);
        pnlMassDetectors.add(btnSetMass);

        
        pnlPeaksConstructors = new JPanel(new GridLayout(1, 3, 5, 0));
        comboPeaksConstructors = new JComboBox(parameters.peakBuilderNames);
        comboPeaksConstructors.setSelectedIndex(parameters.getPeakBuilderTypeNumber());
        comboPeaksConstructors.addActionListener(this);
        btnSetPeak = new JButton("Set parameters");
        btnSetPeak.addActionListener(this);
        JLabel lblPeakBuilder = new JLabel("Peak Builder");
        lblPeakBuilder.setSize(lblMassDetectors.getSize());
        pnlPeaksConstructors.add(lblPeakBuilder);
        pnlPeaksConstructors.add(comboPeaksConstructors);
        pnlPeaksConstructors.add(btnSetPeak);
        
        
        pnlCombo = new JPanel(new BorderLayout());
        pnlCombo.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));    	
        /*pnlCombo = new JPanel(new BorderLayout());
        Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        pnlCombo.setBorder(BorderFactory.createCompoundBorder(one, two));*/
        pnlCombo.add(pnlMassDetectors, BorderLayout.NORTH);
        pnlCombo.add(pnlPeaksConstructors, BorderLayout.SOUTH);
        
        // Buttons
        pnlButtons = new JPanel();
        btnOK = new JButton("OK");
        btnOK.addActionListener(this);
        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this);
        pnlButtons.add(btnOK);
        pnlButtons.add(btnCancel);

        // Panel where everything is collected
        pnlAll = new JPanel(new BorderLayout());
        pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(pnlAll);

        // Leave the BorderLayout.CENTER area empty, so that derived dialogs can
        // put their own controls in there
        pnlAll.add(pnlSuffix, BorderLayout.NORTH);
        pnlAll.add(pnlCombo, BorderLayout.CENTER);
        pnlAll.add(pnlButtons, BorderLayout.SOUTH);

        pack();
        setTitle(title);
        setResizable(false);
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
    }

    public ExitCode getExitCode() {
        return exitCode;
    }

    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnSetMass) {
        	int ind = comboMassDetectors.getSelectedIndex();
            /*ParameterSetupDialog dialog = new ParameterSetupDialog(
                    "Please set parameter values for " + toString(),
                    parameters.getMassDetectorParameters(ind));
            dialog.setVisible(true);*/

            MassDetectorSetupDialog dialog = new MassDetectorSetupDialog(
                    dataFile, parameters.getMassDetectorParameters(ind), parameters.massDetectorNames[ind]);
            dialog.setVisible(true);
        	
            /*ExitCode exitCodeParameters = dialog.getExitCode();
            if ((exitCodeParameters != ExitCode.OK) && (exitCodeParameters != ExitCode.CANCEL))
            	displayMessage("An error ocurred in setup parameters of " + parameters.massDetectorNames[ind]);*/
            	
        }
        
        if (src == btnSetPeak) {
           	int ind = comboPeaksConstructors.getSelectedIndex();
            ParameterSetupDialog dialog = new ParameterSetupDialog(
                    "Please set parameter values for " + toString(),
                    parameters.getPeakBuilderParameters(ind));
            dialog.setVisible(true);
         	
            
            /*ExitCode exitCodeParameters = dialog.getExitCode();
            if (exitCodeParameters != ExitCode.OK)
              	displayMessage("An error ocurred in setup parameters of " + parameters.peakBuilderNames[ind]);*/
        }        

        if (src == btnOK) {
        	parameters.setTypeNumber(comboMassDetectors.getSelectedIndex(), comboPeaksConstructors.getSelectedIndex());
        	parameters.setSuffix(txtField.getText());
            exitCode = ExitCode.OK;
            dispose();
        }

        if (src == btnCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
        }

    }

    private void displayMessage(String msg) {
        try {
            logger.info(msg);
            JOptionPane.showMessageDialog(this, msg, "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception exce) {
        }
    }


}
