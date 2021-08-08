/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
