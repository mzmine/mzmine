/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
