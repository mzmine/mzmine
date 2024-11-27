/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_feature_networks;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.mzio.mzmine.datamodel.parameters.ParameterSet;
import java.io.File;
import java.time.Instant;
import java.util.Collection;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This module allows to import edges defined between two {@link FeatureListRow}. The edges are
 * saved to {@link FeatureList#getRowMaps()}
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class ImportFeatureNetworksSimpleModule implements MZmineProcessingModule {

  private static final Logger logger = Logger.getLogger(
      ImportFeatureNetworksSimpleModule.class.getName());

  @Override
  public @NotNull String getName() {
    return "Import molecular networks/feature networks (csv/tsv)";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ImportFeatureNetworksSimpleParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return """
        Import molecular networking or other feature networks into feature lists.
        Can then be visualized with the interactive network visualizer.
        Format: Comma-separated files (csv) or Tab-separated files (tsv) with the following columns:
        ID1,ID2,EdgeType,EdgeAnnotation,EdgeScore
        ID1 and ID2 need to correspond to feature list row IDs in the selected feature lists.
        EdgeType and EdgeAnnotation are strings to define the method used and annotations of the edge
        EdgeScore is a floating point number
        """;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    File[] inputFiles = parameters.getValue(ImportFeatureNetworksSimpleParameters.input);
    var featureList = parameters.getValue(ImportFeatureNetworksSimpleParameters.flist)
        .getMatchingFeatureLists()[0];

    if (inputFiles.length == 0) {
      logger.warning("Please select one or more tsv or csv files for network import");
      return ExitCode.ERROR;
    }

    for (final File inputFile : inputFiles) {
      tasks.add(
          new ImportFeatureNetworksSimpleTask(inputFile, featureList, parameters, moduleCallDate));
    }
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTIMPORT;
  }
}
