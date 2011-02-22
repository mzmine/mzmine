/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Baseline correction module.
 *
 * @author Chris Pudney, Syngenta Ltd
 * @version $Revision$
 */
public class BaselineCorrection implements ActionListener, MZmineModule {

    // Help ID.
    private static final String HELP_ID = GUIUtils.generateHelpID(BaselineCorrection.class);

    private BaselineCorrectionParameters parameters;

    /**
     * Create the module.
     */
    public BaselineCorrection() {
        parameters = null;
    }

    @Override
    public void initModule() {

        // Create a new instance of our parameters class
        parameters = new BaselineCorrectionParameters();

        // Create a menu item for our module
        MZmineCore.getDesktop().addMenuItem(MZmineMenu.RAWDATAFILTERING,
                "Baseline correction",
                "Compensates for baseline drift in ion chromatograms",
                KeyEvent.VK_B, false, this, null);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        // Obtain a reference to MZmine 2 desktop.
        final Desktop desktop = MZmineCore.getDesktop();

        // Obtain selected raw data files.
        final RawDataFile[] dataFiles = desktop.getSelectedDataFiles();

        if (dataFiles.length == 0) {

            // No data file selected,
            desktop.displayErrorMessage("Please select at least one data file.");

        } else {

            // Show the parameter setup dialog.
            final ParameterSetupDialog dialog = new ParameterSetupDialog(
                    "Please set parameter values for Baseline Correction", parameters, HELP_ID);
            dialog.setVisible(true);

            // If user selected OK.
            if (dialog.getExitCode() == ExitCode.OK) {

                // Create a copy of our parameters.
                final BaselineCorrectionParameters pCopy = (BaselineCorrectionParameters) parameters.clone();

                // Process each data file.
                for (final RawDataFile dataFile : dataFiles) {

                    // Perform task for raw data file.
                    MZmineCore.getTaskController().addTask(new BaselineCorrectionTask(dataFile, pCopy));
                }
            }
        }
    }

    @Override
    public ParameterSet getParameterSet() {
        return parameters;
    }

    @Override
    public void setParameters(final ParameterSet parameterSet) {
        parameters = (BaselineCorrectionParameters) parameterSet;
    }
}
