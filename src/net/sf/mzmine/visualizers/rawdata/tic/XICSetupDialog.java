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

package net.sf.mzmine.visualizers.rawdata.tic;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.userinterface.mainwindow.MainWindow;

class XICSetupDialog extends JDialog implements ActionListener {

    private JTextField txtXicMZ;
    private JTextField txtXicMZDelta;

    private JButton okBtn;
    private JButton cancelBtn;

    private boolean XICSet = false;

    private TICDataSet[] dataSets;

    /**
     * Constructor
     */
    public XICSetupDialog(TICDataSet[] dataSets) {

        super(MainWindow.getInstance(),
                "Please give centroid and delta MZ values for XIC", true);

        this.dataSets = dataSets;

        // MZ value
        JLabel ricMZLabel = new JLabel("MZ");

        // TODO: remember values in parameter storage
        txtXicMZ = new JTextField("500");

        // MZ delta value
        JLabel ricMZDeltaLabel = new JLabel("MZ delta");

        txtXicMZDelta = new JTextField("1");

        JPanel fields = new JPanel();
        fields.setLayout(new GridLayout(2, 2));
        fields.add(ricMZLabel);
        fields.add(txtXicMZ);
        fields.add(ricMZDeltaLabel);
        fields.add(txtXicMZDelta);

        // Buttons
        JPanel btnPanel = new JPanel();
        okBtn = new JButton("OK");
        cancelBtn = new JButton("Cancel");

        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);
        okBtn.addActionListener(this);
        cancelBtn.addActionListener(this);

        // Add it
        add(fields, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(MainWindow.getInstance());

        pack();
    }

    public void actionPerformed(java.awt.event.ActionEvent ae) {

        Object src = ae.getSource();

        if (src == okBtn) {

            String mzStr = txtXicMZ.getText();
            String mzDeltaStr = txtXicMZDelta.getText();

            try {

                double mz = Double.parseDouble(mzStr);
                if (mz < 0)
                    throw (new NumberFormatException());

                double mzDelta = Double.parseDouble(mzDeltaStr);
                if (mzDelta < 0)
                    throw (new NumberFormatException());

                XICSet = true;
                
                for (TICDataSet dataSet : dataSets) {
                    dataSet.setXICMode(mz - mzDelta, mz + mzDelta);
                }

            } catch (NumberFormatException exe) {
                MainWindow.getInstance().getStatusBar().setStatusText(
                        "Error: incorrect parameter values.");
            }

        }

        if (src == cancelBtn) {
            MainWindow.getInstance().getStatusBar().setStatusText(
                    "Switch to XIC cancelled.");
        }

        dispose();

    }

    public boolean getXICSet() {
        return XICSet;
    }
}