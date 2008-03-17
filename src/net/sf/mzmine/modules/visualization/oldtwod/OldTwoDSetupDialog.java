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

package net.sf.mzmine.modules.visualization.oldtwod;

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

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;

/**
 * Setup dialog for 2D visualizer
 */
public class OldTwoDSetupDialog extends JDialog implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    static final int PADDING_SIZE = 5;
    static final int DEFAULT_RT_RESOLUTION = 1000;
    static final int DEFAULT_MZ_RESOLUTION = 1000;

    // dialog components
    private JButton btnOK, btnCancel;
    private JFormattedTextField fieldMinRT, fieldMaxRT, fieldMinMZ, fieldMaxMZ,
            fieldResolutionRT, fieldResolutionMZ;
    private JComboBox comboMSlevel;

    private Desktop desktop;
    private RawDataFile dataFile;

    public OldTwoDSetupDialog(RawDataFile dataFile) {

        // Make dialog modal
        super(MZmineCore.getDesktop().getMainFrame(),
                "2D visualizer parameters", true);

        this.desktop = MZmineCore.getDesktop();
        this.dataFile = dataFile;

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

        comp = GUIUtils.addLabel(components, dataFile.toString(), JLabel.LEFT);
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

        Integer msLevels[] = CollectionUtils.toIntegerArray(dataFile.getMSLevels());
        comboMSlevel = new JComboBox(msLevels);
        comboMSlevel.addActionListener(this);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        components.add(comboMSlevel, constraints);
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

        comp = GUIUtils.addLabel(components, "Minimum m/z");
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

        comp = GUIUtils.addLabel(components, "Maximum m/z");
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

        comp = GUIUtils.addLabel(components, "Bitmap retention time resolution");
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldResolutionRT = new JFormattedTextField(format);
        fieldResolutionRT.setValue(DEFAULT_RT_RESOLUTION);
        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldResolutionRT, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "data points");
        constraints.gridx = 2;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addLabel(components, "Bitmap m/z resolution");
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        fieldResolutionMZ = new JFormattedTextField(format);
        fieldResolutionMZ.setValue(DEFAULT_MZ_RESOLUTION);
        constraints.weightx = 1;
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        components.add(fieldResolutionMZ, constraints);
        constraints.weightx = 0;

        comp = GUIUtils.addLabel(components, "data points");
        constraints.gridx = 2;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

        comp = GUIUtils.addSeparator(components, PADDING_SIZE);
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        layout.setConstraints(comp, constraints);

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

            /*fieldMinRT.setValue(dataFile.getDataMinRT(msLevel));
            fieldMaxRT.setValue(dataFile.getDataMaxRT(msLevel));
            fieldMinMZ.setValue(dataFile.getDataMinMZ(msLevel));
            fieldMaxMZ.setValue(dataFile.getDataMaxMZ(msLevel));*/

        }

        if (src == btnOK) {

            try {

                int msLevel = (Integer) comboMSlevel.getSelectedItem();

                float rtMin = ((Number) fieldMinRT.getValue()).floatValue();
                float rtMax = ((Number) fieldMaxRT.getValue()).floatValue();
                float mzMin = ((Number) fieldMinMZ.getValue()).floatValue();
                float mzMax = ((Number) fieldMaxMZ.getValue()).floatValue();

                if ((rtMax <= rtMin) || (mzMax <= mzMin)) {
                    desktop.displayErrorMessage("Invalid bounds");
                    return;
                }

                int rtResolution = ((Number) fieldResolutionRT.getValue()).intValue();
                if (rtResolution < 1) {
                    desktop.displayErrorMessage("Invalid retention time resolution: "
                            + rtResolution);
                    return;
                }

                int mzResolution = ((Number) fieldResolutionMZ.getValue()).intValue();
                if (mzResolution < 1) {
                    desktop.displayErrorMessage("Invalid m/z resolution: "
                            + mzResolution);
                    return;
                }

                OldTwoDVisualizerWindow window = new OldTwoDVisualizerWindow(dataFile);
                window.setZoomRange(msLevel, rtMin, rtMax, mzMin, mzMax);

                dispose();

            } catch (Exception e) {
                logger.log(Level.FINE,
                        "Error while opening 2D visualizer window", e);
                desktop.displayErrorMessage("Invalid input");
            }
        }

        if (src == btnCancel) {
            dispose();
        }

    }
}
