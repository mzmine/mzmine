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


import io.github.mzmine.datamodel.IsotopePattern;
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

public class IsotopePatternExportModule implements MZmineModule {

  private static final String MODULE_NAME = "Isotope pattern export";

  public static void exportIsotopePattern(ModularFeatureListRow row) {

    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(IsotopePatternExportModule.class);

    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode != ExitCode.OK) {
      return;
    }

    File outputFile = parameters.getParameter(IsotopePatternExportParameters.outputFile).getValue();
    if (outputFile == null) {
      return;
    }

    IsotopePattern pattern = row.getBestIsotopePattern();

    if (pattern == null) {
      MZmineCore.getDesktop().displayMessage("Feature does not possess an isotope pattern.");
      return;
    }

    try {
      FileWriter fileWriter = new FileWriter(outputFile);
      BufferedWriter writer = new BufferedWriter(fileWriter);

      for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
        writer.write(pattern.getMzValue(i) + " " + pattern.getIntensityValue(i));
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
    return IsotopePatternExportParameters.class;
  }

}
