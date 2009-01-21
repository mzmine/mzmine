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

package net.sf.mzmine.modules.dataanalysis.rtmzplots;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.main.mzmineclient.MZmineModule;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;

import org.jfree.data.xy.AbstractXYZDataset;

public class RTMZAnalyzer implements MZmineModule, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    public static final String MeasurementTypeArea = "Area";
    public static final String MeasurementTypeHeight = "Height";

    public static final Object[] MeasurementTypePossibleValues = {
            MeasurementTypeArea, MeasurementTypeHeight };

    public static final Parameter MeasurementType = new SimpleParameter(
            ParameterType.STRING,
            "Peak measurement type",
            "Determines whether peak's area or height is used in computations.",
            MeasurementTypeArea, MeasurementTypePossibleValues);

    private Desktop desktop;

    private SimpleParameterSet parameters;

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#initModule(net.sf.mzmine.main.mzmineclient.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new SimpleParameterSet(new Parameter[] { MeasurementType });

        desktop.addMenuItem(MZmineMenu.DATAANALYSIS,
                "Coefficient of variation (CV) analysis",
                "Plots a variation of each peak among a group of samples",
                KeyEvent.VK_V, false, this, "CV_PLOT");

        desktop.addMenuItem(
                MZmineMenu.DATAANALYSIS,
                "Logratio analysis",
                "Plots a difference of each peak between two groups of samples",
                KeyEvent.VK_L, false, this, "LOGRATIO_PLOT");

    }

    public String toString() {
        return "RT vs m/z analyzer";
    }

    public void setParameters(ParameterSet parameterValues) {
        this.parameters = (SimpleParameterSet) parameterValues;
    }

    public ParameterSet getParameterSet() {
        return parameters;
    }

    public void actionPerformed(ActionEvent event) {

        PeakList[] alignedPeakLists = desktop.getSelectedPeakLists();

        if (alignedPeakLists.length == 0) {
            desktop.displayErrorMessage("Please select at least one aligned peak list.");
        }

        String command = event.getActionCommand();

        for (PeakList pl : alignedPeakLists) {

            if (pl.getRawDataFiles().length < 2) {
                desktop.displayErrorMessage("Alignment " + pl.toString()
                        + " contains less than two peak lists.");
                continue;
            }

            // Show opened raw data file selection and parameter setup dialog
            RTMZSetupDialog setupDialog = null;
            if (command.equals("CV_PLOT"))
                setupDialog = new RTMZSetupDialog(desktop,
                        pl.getRawDataFiles(), parameters,
                        RTMZSetupDialog.SelectionMode.SingleGroup);

            if (command.equals("LOGRATIO_PLOT"))
                setupDialog = new RTMZSetupDialog(desktop,
                        pl.getRawDataFiles(), parameters,
                        RTMZSetupDialog.SelectionMode.TwoGroups);

            setupDialog.setVisible(true);

            if (setupDialog.getExitCode() != ExitCode.OK) {
                logger.info("Analysis cancelled.");
                return;
            }

            // Create dataset & paint scale
            AbstractXYZDataset dataset = null;
            InterpolatingLookupPaintScale paintScale = null;
            if (command.equals("CV_PLOT")) {
                dataset = new CVDataset(pl,
                        setupDialog.getGroupOneSelectedFiles(), parameters);

                paintScale = new InterpolatingLookupPaintScale();
                paintScale.add(0.00, new Color(0, 0, 0));
                paintScale.add(0.15, new Color(102, 255, 102));
                paintScale.add(0.30, new Color(51, 102, 255));
                paintScale.add(0.45, new Color(255, 0, 0));
            }

            if (command.equals("LOGRATIO_PLOT")) {
                dataset = new LogratioDataset(pl,
                        setupDialog.getGroupOneSelectedFiles(),
                        setupDialog.getGroupTwoSelectedFiles(), parameters);

                paintScale = new InterpolatingLookupPaintScale();
                paintScale.add(-1.00, new Color(0, 255, 0));
                paintScale.add(0.00, new Color(0, 0, 0));
                paintScale.add(1.00, new Color(255, 0, 0));
            }

            // Create & show window
            RTMZAnalyzerWindow window = new RTMZAnalyzerWindow(desktop,
                    dataset, pl, parameters, paintScale);
            window.setVisible(true);

        }

    }

}
