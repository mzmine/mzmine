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

package net.sf.mzmine.visualizers.rawdata.neutralloss;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;

/**
 * Setup dialog for neutral loss visualizer
 */
public class NeutralLossSetupDialog extends JDialog implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    static final int PADDING_SIZE = 5;
    static final int DEFAULT_FRAGMENTS = 5;

    // dialog components
    private JButton btnOK, btnCancel;
    private JFormattedTextField fieldMinRT, fieldMaxRT, fieldMinMZ, fieldMaxMZ,
            numOfFragments;
    private JComboBox comboXaxis;

    private Desktop desktop;
    private TaskController taskController;
    private OpenedRawDataFile dataFile;
    private RawDataFile rawDataFile;

    public NeutralLossSetupDialog(TaskController taskController, Desktop desktop,
            OpenedRawDataFile dataFile) {

        // Make dialog modal
        super(desktop.getMainFrame(), "Neutral loss visualizer parameters", true);

        this.taskController = taskController;
        this.desktop = desktop;
        this.dataFile = dataFile;
        this.rawDataFile = dataFile.getCurrentFile();

        GridBagConstraints constraints = new GridBagConstraints();

        // set default layout constraints
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(PADDING_SIZE, PADDING_SIZE,
                PADDING_SIZE, PADDING_SIZE);

        JComponent comp;
        GridBagLayout layout = new GridBagLayout();

        JPanel components = new JPanel(layout);

        comp = GUIUtils.addLabel(components, "Raw data file");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, dataFile.toString(),
                JLabel.LEFT);
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "X axis");
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comboXaxis = new JComboBox(new String[] { "Parent mass", "Retention time" });
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        components.add(comboXaxis, constraints);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        NumberFormat format = NumberFormat.getNumberInstance();

        comp = GUIUtils.addLabel(components, "Minimum retention time");

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMinRT = new JFormattedTextField(format);
        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMinRT, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "s");
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Maximum retention time");
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMaxRT = new JFormattedTextField(format);
        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMaxRT, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "s");
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Minimum precursor m/z");
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMinMZ = new JFormattedTextField(format);
        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMinMZ, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "m/q (Th)");
        constraints.gridx = 2;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Maximum precursor m/z");
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMaxMZ = new JFormattedTextField(format);
        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMaxMZ, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "m/q (Th)");
        constraints.gridx = 2;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Number of most intense fragments");
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        numOfFragments = new JFormattedTextField(format);
        numOfFragments.setValue(DEFAULT_FRAGMENTS);
        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(numOfFragments, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "");
        constraints.gridx = 2;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addSeparator(components, PADDING_SIZE);
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        JPanel buttonsPanel = new JPanel();
        btnOK = GUIUtils.addButton(buttonsPanel, "OK", null, this);
        btnCancel = GUIUtils.addButton(buttonsPanel, "Cancel", null, this);
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        components.add(buttonsPanel, constraints);

        GUIUtils.addMargin(components, PADDING_SIZE);
        add(components);

        // to activate the selection listener
        comboXaxis.setSelectedIndex(0);
        
        fieldMinRT.setValue(rawDataFile.getDataMinRT(2));
        fieldMaxRT.setValue(rawDataFile.getDataMaxRT(2));
        fieldMinMZ.setValue(rawDataFile.getDataMinMZ(1));
        fieldMaxMZ.setValue(rawDataFile.getDataMaxMZ(1));

        // finalize the dialog
        pack();
        setLocationRelativeTo(desktop.getMainFrame());
        setResizable(false);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnOK) {

            try {

                double rtMin = ((Number) fieldMinRT.getValue()).doubleValue();
                double rtMax = ((Number) fieldMaxRT.getValue()).doubleValue();
                double mzMin = ((Number) fieldMinMZ.getValue()).doubleValue();
                double mzMax = ((Number) fieldMaxMZ.getValue()).doubleValue();

                if ((rtMax <= rtMin) || (mzMax <= mzMin)) {
                    desktop.displayErrorMessage("Invalid bounds");
                    return;
                }

                int fragments = ((Number) numOfFragments.getValue()).intValue();
                if (fragments < 1) {
                    desktop.displayErrorMessage("Invalid number of fragments: "
                            + fragments);
                    return;
                }

                int xAxis = comboXaxis.getSelectedIndex();

                new NeutralLossVisualizerWindow(taskController, desktop, dataFile,
                        xAxis, rtMin, rtMax, mzMin, mzMax, fragments);
                        
                dispose();

            } catch (Exception e) {
                logger.log(Level.FINE, "Error while opening neutral loss visualizer window", e);
                desktop.displayErrorMessage("Invalid input");
            }
        }

        if (src == btnCancel) {
            dispose();
        }

    }
}
