/*
 * Copyright 2006-2010 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.components.HelpButton;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 
 */
class DeconvolutionSetupDialog extends JDialog implements ActionListener {

    private DeconvolutionParameters parameters;
    private ExitCode exitCode = ExitCode.UNKNOWN;

    // Dialog components
    private JButton btnOK, btnCancel, btnHelp, btnSetPeak;
    private JComboBox comboPeaksConstructors;
    private JCheckBox removeOriginalCheckBox;
    private JTextField txtField;

    public DeconvolutionSetupDialog(String title,
            DeconvolutionParameters parameters, String helpID) {

        super(MZmineCore.getDesktop().getMainFrame(),
                "Please select peak resolver", true);

        this.parameters = parameters;

        addComponentsToDialog(helpID);
        setResizable(false);
    }

    public ExitCode getExitCode() {
        return exitCode;
    }

    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnSetPeak) {

            int indexPeakResolver = comboPeaksConstructors.getSelectedIndex();

            PeakResolverSetupDialog dialog = new PeakResolverSetupDialog(
                    parameters, indexPeakResolver);

            dialog.setVisible(true);
        }

        if (src == btnOK) {

            parameters.setTypeNumber(comboPeaksConstructors.getSelectedIndex());
            parameters.setParameterValue(DeconvolutionParameters.suffix, txtField.getText());
            parameters.setParameterValue(DeconvolutionParameters.autoRemove, removeOriginalCheckBox.isSelected());
            exitCode = ExitCode.OK;
            dispose();
        }

        if (src == btnCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
        }

    }

    /**
     * This function add all components for this dialog
     * 
     */
    private void addComponentsToDialog(String helpID) {

        // Elements of suffix
        txtField = new JTextField();
        txtField.setText((String) parameters.getParameterValue(DeconvolutionParameters.suffix));
        txtField.selectAll();
        txtField.setMaximumSize(new Dimension(250, 30));

        // Elements of Peak recognition
        comboPeaksConstructors = new JComboBox(
                DeconvolutionParameters.peakResolverNames);
        comboPeaksConstructors.setSelectedIndex(parameters.getPeakResolverTypeNumber());
        comboPeaksConstructors.addActionListener(this);
        comboPeaksConstructors.setMaximumSize(new Dimension(200, 28));
        btnSetPeak = new JButton("Set parameters");
        btnSetPeak.addActionListener(this);
        
        //Element remove original peak list
        removeOriginalCheckBox = new JCheckBox();
        removeOriginalCheckBox.setSelected((Boolean) parameters.getParameterValue(DeconvolutionParameters.autoRemove));

        // Elements of buttons
        btnOK = new JButton("OK");
        btnOK.addActionListener(this);
        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this);
        btnHelp = new HelpButton(helpID);

        JPanel pnlCombo = new JPanel();
        pnlCombo.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 10.0;
        c.weightx = 10.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        pnlCombo.add(new JLabel("Suffix"), c);
        c.gridwidth = 4;
        c.gridx = 1;
        pnlCombo.add(txtField, c);

        c.gridx = 0;
        c.gridy = 3;
        pnlCombo.add(new JLabel("Peak recognition"), c);
        c.gridwidth = 3;
        c.gridx = 1;
        pnlCombo.add(comboPeaksConstructors, c);
        c.gridwidth = 1;
        c.gridx = 4;
        pnlCombo.add(btnSetPeak, c);

        c.gridx = 0;
        c.gridy = 4;
        pnlCombo.add(new JLabel(DeconvolutionParameters.autoRemove.getName()), c);
        c.gridx = 1;
        pnlCombo.add(removeOriginalCheckBox, c);
        
        c.gridx = 1;
        c.gridy = 5;
        pnlCombo.add(btnOK, c);
        c.gridx = 2;
        pnlCombo.add(btnCancel, c);
        c.gridx = 3;
        pnlCombo.add(btnHelp, c);

        // Panel where everything is collected
        JPanel pnlAll = new JPanel(new BorderLayout());
        pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlAll.add(pnlCombo, BorderLayout.CENTER);
        add(pnlAll);

        pack();
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

}
