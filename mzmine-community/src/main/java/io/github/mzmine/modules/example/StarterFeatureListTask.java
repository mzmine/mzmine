/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.example;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The task will be scheduled by the TaskController. Progress is calculated from the
 * finishedItems/totalItems
 */
class StarterFeatureListTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(StarterFeatureListTask.class.getName());

  private final FeatureList featureList;
  private final File outFile;
  private final Range<Double> mzRange;

  /**
   * Constructor is used to extract all parameters
   *
   * @param featureList data source is featureList
   * @param parameters  user parameters
   */
  public StarterFeatureListTask(MZmineProject project, FeatureList featureList,
      ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureList = featureList;
    // important to track progress with totalItems and finishedItems
    totalItems = featureList.getNumberOfRows();
    // Get parameter values for easier use
    outFile = parameters.getValue(StarterFeatureListParameters.outFile);
    mzRange = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        StarterFeatureListParameters.mzRange, Range.all());
  }

  @Override
  protected void process() {
    // process each row
    for (var row : featureList.getRows()) {
      // do something

      // increment progress
      incrementFinishedItems();
    }

    // can also stream items
    int maximumMs2Scans = featureList.getRows().stream().map(row -> row.getAllFragmentScans())
        .mapToInt(List::size).max().orElse(0);

  }

  @Override
  public String getTaskDescription() {
    return "Runs task on " + featureList;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureList);
  }

}
