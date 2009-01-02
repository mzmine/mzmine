/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.threestep;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.threestep.massdetection.MassDetectorSetupDialog;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilderSetupDialog;
import net.sf.mzmine.util.components.HelpButton;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * 
 */
class ThreeStepPickerSetupDialog extends JDialog implements ActionListener {

    private ThreeStepPickerParameters parameters;
    private ExitCode exitCode = ExitCode.UNKNOWN;
    private String title;

    // Dialog components
    private JButton btnOK, btnCancel, btnHelp, btnSetMass, btnSetChromato,
            btnSetPeak;
    private JComboBox comboMassDetectors, comboChromatoBuilder,
            comboPeaksConstructors;
    private JTextField txtField;

    public ThreeStepPickerSetupDialog(String title,
            ThreeStepPickerParameters parameters) {

        super(MZmineCore.getDesktop().getMainFrame(),
                "Please select mass detector  & peak detector", true);

        this.parameters = parameters;
        this.title = title;

        addComponentsToDialog();
        this.setResizable(false);
    }

    public ExitCode getExitCode() {
        return exitCode;
    }

    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnSetMass) {
            int ind = comboMassDetectors.getSelectedIndex();

            MassDetectorSetupDialog dialog = new MassDetectorSetupDialog(
                    parameters, ind);
            dialog.setVisible(true);

        }

        if (src == btnSetChromato) {
            int ind = comboChromatoBuilder.getSelectedIndex();

            ParameterSetupDialog dialog = new ParameterSetupDialog(
                    ThreeStepPickerParameters.chromatogramBuilderNames[ind]
                            + "'s parameter setup dialog ",
                    parameters.getChromatogramBuilderParameters(ind),
                    ThreeStepPickerParameters.chromatogramBuilderHelpFiles[ind]);;

            dialog.setVisible(true);
        }

        if (src == btnSetPeak) {
            int indChromatoBuilder = comboChromatoBuilder.getSelectedIndex();
            int indexPeakBuilder = comboPeaksConstructors.getSelectedIndex();

            PeakBuilderSetupDialog dialog = new PeakBuilderSetupDialog(
                    parameters, indChromatoBuilder, indexPeakBuilder);

            dialog.setVisible(true);
        }

        if (src == btnOK) {
            inform();
            parameters.setTypeNumber(comboMassDetectors.getSelectedIndex(),
                    comboChromatoBuilder.getSelectedIndex(),
                    comboPeaksConstructors.getSelectedIndex());
            parameters.setSuffix(txtField.getText());
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
    private void addComponentsToDialog() {

        // Elements of suffix
        txtField = new JTextField();
        txtField.setText(parameters.getSuffix());
        txtField.selectAll();
        txtField.setMaximumSize(new Dimension(250, 30));

        // Elements of Mass detector
        comboMassDetectors = new JComboBox(
                ThreeStepPickerParameters.massDetectorNames);
        comboMassDetectors.setSelectedIndex(parameters.getMassDetectorTypeNumber());
        comboMassDetectors.addActionListener(this);
        comboMassDetectors.setMaximumSize(new Dimension(200, 30));
        btnSetMass = new JButton("Set parameters");
        btnSetMass.addActionListener(this);

        // Elements of Chromatogram builder
        comboChromatoBuilder = new JComboBox(
                ThreeStepPickerParameters.chromatogramBuilderNames);
        comboChromatoBuilder.setSelectedIndex(parameters.getChromatogramBuilderTypeNumber());
        comboChromatoBuilder.addActionListener(this);
        comboChromatoBuilder.setMaximumSize(new Dimension(200, 30));
        btnSetChromato = new JButton("Set parameters");
        btnSetChromato.addActionListener(this);

        // Elements of Peak recognition
        comboPeaksConstructors = new JComboBox(
                ThreeStepPickerParameters.peakBuilderNames);
        comboPeaksConstructors.setSelectedIndex(parameters.getPeakBuilderTypeNumber());
        comboPeaksConstructors.addActionListener(this);
        comboPeaksConstructors.setMaximumSize(new Dimension(200, 28));
        btnSetPeak = new JButton("Set parameters");
        btnSetPeak.addActionListener(this);

        // Elements of buttons
        btnOK = new JButton("OK");
        btnOK.addActionListener(this);
        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this);
        btnHelp = new HelpButton(
                "net/sf/mzmine/modules/peakpicking/threestep/help/ThreeStepsDetector.html");

        JPanel pnlCombo = new JPanel();
        pnlCombo.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 10.0;
        c.weightx = 10.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        pnlCombo.add(new JLabel("Filename suffix "), c);
        c.gridwidth = 4;
        c.gridx = 1;
        pnlCombo.add(txtField, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        pnlCombo.add(new JLabel("Mass detection"), c);
        c.gridwidth = 3;
        c.gridx = 1;
        pnlCombo.add(comboMassDetectors, c);
        c.gridwidth = 1;
        c.gridx = 4;
        pnlCombo.add(btnSetMass, c);

        c.gridx = 0;
        c.gridy = 2;
        pnlCombo.add(new JLabel("<HTML>Chromatogram<BR>construction</HTML>"), c);
        c.gridwidth = 3;
        c.gridx = 1;
        pnlCombo.add(comboChromatoBuilder, c);
        c.gridwidth = 1;
        c.gridx = 4;
        pnlCombo.add(btnSetChromato, c);

        c.gridx = 0;
        c.gridy = 3;
        pnlCombo.add(new JLabel("Peak recognition"), c);
        c.gridwidth = 3;
        c.gridx = 1;
        pnlCombo.add(comboPeaksConstructors, c);
        c.gridwidth = 1;
        c.gridx = 4;
        pnlCombo.add(btnSetPeak, c);

        c.gridx = 1;
        c.gridy = 4;
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
        setTitle(title);
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

    /**
     * 
     */
    private void inform() {

        Desktop desktop = MZmineCore.getDesktop();
        RawDataFile[] dataFiles = desktop.getSelectedDataFiles();
        int massDetectorNumber = comboMassDetectors.getSelectedIndex();
        String massDetectorName = ThreeStepPickerParameters.massDetectorNames[massDetectorNumber];
        boolean centroid = false;
        boolean notMsLevelOne = false;

        if (dataFiles.length != 0) {
            for (int i = 0; i < dataFiles.length; i++) {

                int msLevels[] = dataFiles[i].getMSLevels();
                Arrays.sort(msLevels);

                if (msLevels[0] != 1) {
                    notMsLevelOne = true;
                    break;
                }

                Scan scan;
                int[] indexArray = dataFiles[i].getScanNumbers(1);
                int increment = indexArray.length / 10;

                // Verify if the current DataFile contains centroided scans
                for (int j = 0; j < indexArray.length; j += increment) {
                    scan = dataFiles[i].getScan(indexArray[j]);
                    if (scan.isCentroided()) {
                        centroid = true;
                        break;
                    }
                }
            }

            if (notMsLevelOne) {
                desktop.displayMessage(" One or more selected files does not contain spectrum of MS level \"1\".\n"
                        + " The actual mass detector only works over spectrum of this level.");
            }

            if ((centroid) && (!massDetectorName.equals("Centroid"))) {
                desktop.displayMessage(" One or more selected files contains centroided data points.\n"
                        + " The actual mass detector could give an unexpected result ");
            }

            if ((!centroid) && (massDetectorName.equals("Centroid"))) {
                desktop.displayMessage(" Neither one of the selected files contains centroided data points.\n"
                        + " The actual mass detector could give an unexpected result ");
            }
        }
    }

}
