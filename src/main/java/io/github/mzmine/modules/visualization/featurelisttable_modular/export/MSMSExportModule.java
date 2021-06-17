/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package io.github.mzmine.modules.visualization.featurelisttable_modular.export;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.ExitCode;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import org.jetbrains.annotations.NotNull;

public class MSMSExportModule implements MZmineModule {

  private static final String MODULE_NAME = "MS/MS pattern export";

  public static void exportMSMS(ModularFeatureListRow row) {

    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(MSMSExportModule.class);

    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode != ExitCode.OK) {
      return;
    }

    File outputFile = parameters.getParameter(MSMSExportParameters.outputFile).getValue();

    if (outputFile == null) {
      return;
    }

    // Best peak always exists, because feature list row has at least one peak
    Feature bestFeature = row.getBestFeature();

    // Get the MS/MS scan number
    Scan msmsScanNumber = bestFeature.getMostIntenseFragmentScan();
    if (msmsScanNumber == null) {
      MZmineCore.getDesktop().displayErrorMessage("There is no MS/MS scan for peak " + bestFeature);
      return;
    }

    // MS/MS scan must exist, because msmsScanNumber was > 0
    Scan msmsScan = msmsScanNumber;

    MassList massList = msmsScan.getMassList();
    if (massList == null) {
      MZmineCore.getDesktop().displayErrorMessage(
          "There is no mass list for MS/MS scan #" + msmsScanNumber
              + " (" + bestFeature.getRawDataFile() + ")");
      return;
    }

    try {
      FileWriter fileWriter = new FileWriter(outputFile);
      BufferedWriter writer = new BufferedWriter(fileWriter);

      for (int i = 0; i < massList.getDataPoints().length; i++) {
        DataPoint peak = massList.getDataPoints()[i];
        writer.write(peak.getMZ() + " " + peak.getIntensity());
        writer.newLine();
      }

      writer.close();

    } catch (Exception e) {
      e.printStackTrace();
      MZmineCore.getDesktop().displayErrorMessage(
          "Error writing to file " + outputFile + ": " + ExceptionUtils.exceptionToString(e));
    }
  }

  @Override
  public @NotNull
  String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull
  Class<? extends ParameterSet> getParameterSetClass() {
    return MSMSExportParameters.class;
  }

}
