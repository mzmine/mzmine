/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.align_join;

import static io.github.mzmine.util.FeatureListRowSorter.MZ_ASCENDING;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.align_common.BaseFeatureListAligner;
import io.github.mzmine.modules.dataprocessing.align_common.FeatureCloner;
import io.github.mzmine.modules.dataprocessing.align_common.FeatureCloner.SimpleFeatureCloner;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JoinAlignerTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(JoinAlignerTask.class.getName());


  /**
   * All feature lists except the base list
   */
  private final List<FeatureList> featureLists;
  private final MZmineProject project;
  private final String featureListName;
  private final ParameterSet parameters;
  private ModularFeatureList alignedFeatureList;
  private final OriginalFeatureListOption handleOriginal;
  private BaseFeatureListAligner listAligner;

  public JoinAlignerTask(MZmineProject project, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.project = project;
    handleOriginal = parameters.getValue(JoinAlignerParameters.handleOriginal);
    featureListName = parameters.getValue(JoinAlignerParameters.peakListName);

    featureLists = Arrays.stream(
            parameters.getValue(JoinAlignerParameters.peakLists).getMatchingFeatureLists())
        .map(flist -> (FeatureList) flist).toList();

    this.parameters = parameters;
  }

  @Override
  public double getFinishedPercentage() {
    if (listAligner == null) {
      return 0;
    }
    return listAligner.getFinishedPercentage();
  }

  @Override
  protected void process() {
    List<String> errors = new ArrayList<>();
    if (!parameters.checkParameterValues(errors)) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(String.join("\n", errors));
      return;
    }

    logger.info(
        () -> "Running parallel join aligner on " + featureLists.size() + " feature lists.");

    listAligner = createAligner(this, getMemoryMapStorage(), parameters, featureLists,
        featureListName);
    alignedFeatureList = listAligner.alignFeatureLists();

    if (alignedFeatureList == null || isCanceled()) {
      return;
    }

    handleOriginal.reflectNewFeatureListToProject(project, alignedFeatureList, featureLists);

    logger.info("Finished join aligner");
  }

  public static BaseFeatureListAligner createAligner(final @Nullable Task parentTask,
      final @Nullable MemoryMapStorage storage, final ParameterSet parameters,
      final List<FeatureList> featureLists, final String featureListName) {
    FeatureCloner featureCloner = new SimpleFeatureCloner();
    // create the row aligner that handles the scoring
    var rowAligner = new JoinRowAlignScorer(parameters);
    return new BaseFeatureListAligner(parentTask, featureLists, featureListName, storage,
        rowAligner, featureCloner, MZ_ASCENDING, null);
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(alignedFeatureList);
  }

  @Override
  public String getTaskDescription() {
    return "Join align feature lists";
  }

}
