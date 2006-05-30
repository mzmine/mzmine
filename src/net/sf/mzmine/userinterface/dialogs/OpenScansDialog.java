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

package net.sf.mzmine.userinterface.dialogs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectrumVisualizer;

public class OpenScansDialog extends JDialog implements ActionListener {

    private JTextField firstNumber;
    private JTextField lastNumber;

    private JButton okBtn;
    private JButton cancelBtn;
    
    private RawDataFile file;
    private int scanNumbers[];

    /**
     * Constructor
     */
    public OpenScansDialog(RawDataFile file, int[] scanNumbers, int first, int last) {

        super(MainWindow.getInstance(),
                "Please give first and last scan number", true);

        this.file = file;
        this.scanNumbers = scanNumbers;
        
        JLabel firstLabel = new JLabel("First scan number: ");

        firstNumber = new JTextField(String.valueOf(first));

        JLabel lastLabel = new JLabel("Last scan number: ");

        lastNumber = new JTextField(String.valueOf(last));

        JPanel fields = new JPanel();
        fields.setLayout(new GridLayout(2, 2));
        fields.add(firstLabel);
        fields.add(firstNumber);
        fields.add(lastLabel);
        fields.add(lastNumber);

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

            int first = Integer.parseInt(firstNumber.getText());
            int last = Integer.parseInt(lastNumber.getText());
            
            int firstIndex = -1, lastIndex = -1;
            
            for (int i = 0; i < scanNumbers.length; i++) {
                if (scanNumbers[i] == first) firstIndex = i;
                if (scanNumbers[i] == last) lastIndex = i;
            }
            
            if ((firstIndex < 0) || (lastIndex < 0) || (lastIndex < firstIndex)) {
                MainWindow.getInstance().getStatusBar().setStatusText(
                "Error: incorrect scan numbers.");
                dispose();
                return;
            }
            
            int selectedScans[] = new int[lastIndex - firstIndex + 1];
            for (int i = firstIndex; i <= lastIndex; i++) 
                selectedScans[i - firstIndex] = scanNumbers[i];
                
            // create the spectra visualizer
            new SpectrumVisualizer(file, selectedScans);

        }

        if (src == cancelBtn) {
            MainWindow.getInstance().getStatusBar().setStatusText(
                    "Dialog cancelled.");
        }

        dispose();

    }
    
}