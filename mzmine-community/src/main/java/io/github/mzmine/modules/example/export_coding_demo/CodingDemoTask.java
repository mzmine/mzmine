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

package io.github.mzmine.modules.example.export_coding_demo;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.io.CsvWriter;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNullElse;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The task will be scheduled by the TaskController. Progress is calculated from the
 * finishedItems/totalItems
 */
class CodingDemoTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(CodingDemoTask.class.getName());

  private final FeatureList featureList;
  private final File outFile;
  private final Range<Double> mzRange;
  private final Range<Double> rtRange;

  /**
   * Constructor is used to extract all parameters
   *
   * @param featureList data source is featureList
   * @param parameters  user parameters
   */
  public CodingDemoTask(MZmineProject project, FeatureList featureList, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureList = featureList;
    // important to track progress with totalItems and finishedItems
    totalItems = featureList.getNumberOfRows();
    // Get parameter values for easier use
    outFile = parameters.getValue(CodingDemoParameters.outFile);
    mzRange = parameters.getEmbeddedParameterValueIfSelectedOrElse(CodingDemoParameters.mzRange,
        Range.all());
    rtRange = parameters.getEmbeddedParameterValueIfSelectedOrElse(CodingDemoParameters.rtRange,
        Range.all());
  }

  @Override
  protected void process() {
    List<RowStats> results = new ArrayList<>();

    // process each row
    var rows = featureList.getRows();
    for (var row : rows) {
      if (!mzRange.contains(row.getAverageMZ()) && !rtRange.contains(
          row.getAverageRT().doubleValue())) {
        continue;
      }

      // get all fragmentation scans of this row - across all samples
      var ms2Scans = row.getAllFragmentScans();

      // Extract statistics and create a new object. Adding new stats is easy
      RowStats stats = new RowStats(row.getID(), row.getAverageMZ(), row.getAverageRT(),
          ms2Scans.size(),
          ms2Scans.stream().mapToInt(MassSpectrum::getNumberOfDataPoints).max().orElse(0),
          requireNonNullElse(row.getPreferredAnnotationName(), "Unknown"),
          row.streamFeatures().filter(feat -> feat.getFeatureStatus() == FeatureStatus.DETECTED)
              .count());

      // add the stats object for this row to a list
      results.add(stats);

      // increment progress
      incrementFinishedItems();
    }

    try {
      // write the list of results to a new CSV file
      // the header are reflected by the record type RowStats (comparable to Python dataclasses)
      CsvWriter.writeToFile(outFile, results, RowStats.class, WriterOptions.REPLACE);
    } catch (IOException e) {
      // ignore for now
    }
  }

  @Override
  public String getTaskDescription() {
    return "Demo task runs on " + featureList;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureList);
  }

  record RowStats(int id, double mz, double rt, int numFragmentScans, int maxFragmentSignals,
                  String annotation, long numSamplesDetected) {

  }

}
