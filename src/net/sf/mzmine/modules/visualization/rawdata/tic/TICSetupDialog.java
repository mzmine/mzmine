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

package net.sf.mzmine.modules.visualization.rawdata.tic;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.modules.visualization.rawdata.tic.TICVisualizerWindow.PlotType;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;

/**
 * Setup dialog for TIC visualizer
 */
public class TICSetupDialog extends JDialog implements ActionListener {

    static final int PADDING_SIZE = 5;

    static final String[] plotTypes = { "TIC", "Base peak intensity" };

    // dialog components
    private JButton btnOK, btnCancel;
    private JFormattedTextField fieldMinRT, fieldMaxRT, fieldMinMZ, fieldMaxMZ;
    private JComboBox comboPlotType, comboMSlevel;

    private static final NumberFormat format = NumberFormat.getNumberInstance();

    private Desktop desktop;
    private TaskController taskController;
    private OpenedRawDataFile dataFile;
    private RawDataFile rawDataFile;

    public TICSetupDialog(TaskController taskController, Desktop desktop,
            OpenedRawDataFile dataFile) {

        // Make dialog modal
        super(desktop.getMainFrame(), "TIC visualizer parameters", true);

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

        comp = GUIUtils.addLabel(components, "MS level");
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        Integer msLevels[] = CollectionUtils.toIntegerArray(rawDataFile.getMSLevels());

        comboMSlevel = new JComboBox(msLevels);
        comboMSlevel.addActionListener(this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        components.add(comboMSlevel, constraints);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        comp = GUIUtils.addLabel(components, "Plot type");
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comboPlotType = new JComboBox(plotTypes);
        comboPlotType.addActionListener(this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        components.add(comboPlotType, constraints);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        comp = GUIUtils.addLabel(components, "Minimum retention time");

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMinRT = new JFormattedTextField(format);
        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMinRT, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "s");
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Maximum retention time");
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMaxRT = new JFormattedTextField(format);
        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMaxRT, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "s");
        constraints.gridx = 2;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Minimum m/z");
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMinMZ = new JFormattedTextField(format);
        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMinMZ, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "m/q (Th)");
        constraints.gridx = 2;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Maximum m/z");
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldMaxMZ = new JFormattedTextField(format);
        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldMaxMZ, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "m/q (Th)");
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
        comboMSlevel.setSelectedIndex(0);
        
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

        // ms level selection changed
        if (src == comboMSlevel) {

            int msLevel = (Integer) comboMSlevel.getSelectedItem();

            fieldMinRT.setValue(rawDataFile.getDataMinRT(msLevel));
            fieldMaxRT.setValue(rawDataFile.getDataMaxRT(msLevel));
            fieldMinMZ.setValue(rawDataFile.getDataMinMZ(msLevel));
            fieldMaxMZ.setValue(rawDataFile.getDataMaxMZ(msLevel));

        }

        if (src == btnOK) {

            try {

                int msLevel = (Integer) comboMSlevel.getSelectedItem();

                double rtMin = ((Number) fieldMinRT.getValue()).doubleValue();
                double rtMax = ((Number) fieldMaxRT.getValue()).doubleValue();
                double mzMin = ((Number) fieldMinMZ.getValue()).doubleValue();
                double mzMax = ((Number) fieldMaxMZ.getValue()).doubleValue();

                if ((rtMax <= rtMin) || (mzMax <= mzMin)) {
                    desktop.displayErrorMessage("Invalid bounds");
                    return;
                }

                PlotType plotType = PlotType.TIC;

                if (comboPlotType.getSelectedIndex() == 1)
                    plotType = PlotType.BASE_PEAK;

                new TICVisualizerWindow(taskController, desktop, dataFile,
                        plotType, msLevel, rtMin, rtMax, mzMin, mzMax);

                dispose();

            } catch (Exception e) {
                desktop.displayErrorMessage("Invalid input");
            }
        }

        if (src == btnCancel) {
            dispose();
        }

    }
}
