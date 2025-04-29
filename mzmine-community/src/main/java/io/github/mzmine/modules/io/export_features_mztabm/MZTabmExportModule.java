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

package io.github.mzmine.modules.io.export_features_mztabm;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

import java.io.File;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;

public class MZTabmExportModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Export to mzTab-m file.";

  private static final String MODULE_DESCRIPTION = //
      "This method exports the feature list contents into a mzTab-m file.";

  private static final Logger logger = Logger.getLogger(MZTabmExportModule.class.getName());

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }


  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    @NotNull ModularFeatureList[] featureLists = parameters.getParameter(MZTabmExportParameters.featureLists).getValue().getMatchingFeatureLists();

    String plNamePattern = "{}";
    File fileName = parameters.getValue(MZTabmExportParameters.filename);
    boolean substitute = fileName.getPath().contains(plNamePattern);

    if (!substitute && featureLists.length > 1) {
      MZmineCore.getDesktop().displayErrorMessage("""
          Cannot export multiple feature lists to the same mzTab-m file. Please use "{}" pattern in filename.\
          This will be replaced with the feature list name to generate one file per feature list.
          """);
      logger.warning("Cannot export feature lists.");
      return ExitCode.ERROR;
    }

    for (ModularFeatureList featureList: featureLists) {
//      parameters.getValue(MZTabmExportParameters.filename).getName() + featureList.getName()
      MZTabmExportTask task = new MZTabmExportTask(project, featureList, parameters, moduleCallDate);
      tasks.add(task);
    }
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTEXPORT;
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return MZTabmExportParameters.class;
  }
}
