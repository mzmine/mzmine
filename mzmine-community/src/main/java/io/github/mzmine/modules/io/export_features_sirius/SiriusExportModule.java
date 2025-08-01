/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.export_features_sirius;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class SiriusExportModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Export for SIRIUS";
  private static final String MODULE_DESCRIPTION = "This method exports a MGF file that contains for each feature, (1) the deconvoluted MS1 isotopic pattern, and (2) the MS/MS spectrum (highest precursor ion intensity). This file can be open and processed with Sirius, https://bio.informatik.uni-jena.de/software/sirius/.";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    SiriusExportTask task = new SiriusExportTask(parameters, moduleCallDate);
    tasks.add(task);
    return ExitCode.OK;

  }

  public static void exportSingleFeatureList(FeatureListRow row, @NotNull Instant moduleCallDate) {

    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(SiriusExportModule.class);

    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode != ExitCode.OK) {
      return;
    }

    final SiriusExportTask task = new SiriusExportTask(parameters, moduleCallDate);
    final ModularFeatureList flist = parameters.getValue(SiriusExportParameters.FEATURE_LISTS)
        .getMatchingFeatureLists()[0];
    final File fileForFeatureList = task.getFileForFeatureList(flist);

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileForFeatureList))) {
      task.exportRow(writer, row);
    } catch (Exception e) {
      e.printStackTrace();
      MZmineCore.getDesktop().displayErrorMessage(
          "Error while exporting feature to SIRIUS: " + ExceptionUtils.exceptionToString(e));
    }

  }

  public static void exportSingleRows(FeatureListRow[] rows, @NotNull Instant moduleCallDate) {
    try {
      ParameterSet parameters = MZmineCore.getConfiguration()
          .getModuleParameters(SiriusExportModule.class);

      ExitCode exitCode = parameters.showSetupDialog(true);
      if (exitCode != ExitCode.OK) {
        return;
      }
      // Open file
      final SiriusExportTask task = new SiriusExportTask(parameters, moduleCallDate);
      final ModularFeatureList flist = parameters.getValue(SiriusExportParameters.FEATURE_LISTS)
          .getMatchingFeatureLists()[0];
      final File fileForFeatureList = task.getFileForFeatureList(flist);

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileForFeatureList))) {
        for (FeatureListRow row : rows) {
          task.exportRow(writer, row);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      MZmineCore.getDesktop().displayErrorMessage(
          "Error while exporting feature to SIRIUS: " + ExceptionUtils.exceptionToString(e));
    }
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTEXPORT;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return SiriusExportParameters.class;
  }

}

/*
 * "If you use the SIRIUS export module, cite MZmine and the following article: Duhrkop, M.
 * Fleischauer, M. Ludwig, A. A. Aksenov, A. V. Melnik, M. Meusel, P. C. Dorrestein, J. Rousu, and
 * S. Boecker, Sirius 4: a rapid tool for turning tandem mass spectra into metabolite structure
 * information, Nat methods, 2019. 8:5
 *
 * [Link](http://dx.doi.org/10.1038/s41592-019-0344-8), and
 * [Link](https://jcheminf.springeropen.com/articles/10.1186/s13321-016-0116-8)
 */
