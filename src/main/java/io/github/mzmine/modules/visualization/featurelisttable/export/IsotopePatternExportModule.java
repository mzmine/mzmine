/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.featurelisttable.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.ExitCode;

public class IsotopePatternExportModule implements MZmineModule {

    private static final String MODULE_NAME = "Isotope pattern export";

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    public static void exportIsotopePattern(PeakListRow row) {

        ParameterSet parameters = MZmineCore.getConfiguration()
                .getModuleParameters(IsotopePatternExportModule.class);

        ExitCode exitCode = parameters
                .showSetupDialog(MZmineCore.getDesktop().getMainWindow(), true);
        if (exitCode != ExitCode.OK)
            return;

        File outputFile = parameters
                .getParameter(IsotopePatternExportParameters.outputFile)
                .getValue();
        if (outputFile == null)
            return;

        IsotopePattern pattern = row.getBestIsotopePattern();

        DataPoint isotopes[];

        if (pattern != null) {
            isotopes = pattern.getDataPoints();
        } else {
            isotopes = new DataPoint[1];
            Feature bestPeak = row.getBestPeak();
            isotopes[0] = new SimpleDataPoint(bestPeak.getMZ(),
                    bestPeak.getHeight());
        }

        try {
            FileWriter fileWriter = new FileWriter(outputFile);
            BufferedWriter writer = new BufferedWriter(fileWriter);

            for (DataPoint isotope : isotopes) {
                writer.write(isotope.getMZ() + " " + isotope.getIntensity());
                writer.newLine();
            }

            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
            MZmineCore.getDesktop().displayErrorMessage(
                    MZmineCore.getDesktop().getMainWindow(),
                    "Error writing to file " + outputFile + ": "
                            + ExceptionUtils.exceptionToString(e));
        }

    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return IsotopePatternExportParameters.class;
    }

}
