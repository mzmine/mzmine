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

package io.github.mzmine.modules.dataprocessing.align_append_rows;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MergeAlignerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MergeAlignerTask.class.getName());

  private final MZmineProject project;
  private ModularFeatureList[] featureLists;
  private ModularFeatureList alignedFeatureList;

  // Processed rows counter
  private int processedRows, totalRows;

  private String featureListName;
  private ParameterSet parameters;

  public MergeAlignerTask(MZmineProject project, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.parameters = parameters;

    featureLists = parameters.getParameter(
        MergeAlignerParameters.featureLists).getValue()
        .getMatchingFeatureLists();

    featureListName = parameters.getParameter(
        MergeAlignerParameters.peakListName).getValue();
  }

  @Override
  public String getTaskDescription() {
    return "List merger, " + featureListName + " (" + featureLists.length + " feature lists)";
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0d;
    }
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public void run() {
    if (featureLists.length < 2) {
      setErrorMessage("Cannot perform feature list merging on " + featureLists.length
                      + " feature lists (need at least 2)");
      setStatus(TaskStatus.ERROR);
      return;
    }

    setStatus(TaskStatus.PROCESSING);
    logger.info("Running feature list merger on " + featureLists.length);

    // Remember how many rows we need to process. Each row will be processed
    // twice, first for score calculation, second for actual alignment.
    for (FeatureList list : featureLists) {
      totalRows += list.getNumberOfRows();
    }

    // Collect all data files
    List<RawDataFile> allDataFiles = Stream.of(featureLists)
        .flatMap(flist -> flist.getRawDataFiles().stream()).distinct().collect(Collectors.toList());

    // create copy of first feature list as base and renumber IDs
    alignedFeatureList = featureLists[0]
        .createCopy(featureListName, getMemoryMapStorage(), allDataFiles, true);

    // next row will have this id
    int newRowID = alignedFeatureList.getNumberOfRows() + 1;

    // Iterate source feature lists
    // Next feature list
    for (int i = 1; i < featureLists.length; i++) {
      ModularFeatureList featureList = featureLists[i];

      // copy all types
      featureList.getRowTypes().values().forEach(type -> alignedFeatureList.addRowType(type));
      featureList.getFeatureTypes().values()
          .forEach(type -> alignedFeatureList.addFeatureType(type));

      // selected scans during chromatogram creation needs to be the same list for the same data file
      for (RawDataFile file : featureList.getRawDataFiles()) {
        List<? extends Scan> seletedScans = alignedFeatureList.getSeletedScans(file);
        List<? extends Scan> seletedScansNew = featureList.getSeletedScans(file);
        if (seletedScans != null && (seletedScansNew == null
                                     || seletedScans.size() != seletedScansNew.size())) {
          setErrorMessage(
              "Cannot merge feature lists from the same RawDataFile that were created with different selected scans (e.g., during chromatogram building). Try to harmonize the scan filters in the previous steps or split the raw data file in two data files, e.g., for positive and negative mode data.");
          setStatus(TaskStatus.ERROR);
          return;
        } else {
          alignedFeatureList.setSelectedScans(file, seletedScansNew);
        }
      }

      // Calculate scores for all possible alignments of this row
      for (FeatureListRow row : featureList.getRows()) {
        if (isCanceled()) {
          return;
        }
        if (row instanceof ModularFeatureListRow mrow) {
          ModularFeatureListRow targetRow = new ModularFeatureListRow(alignedFeatureList, newRowID,
              mrow, true);
          newRowID++;
          alignedFeatureList.addRow(targetRow);
          processedRows++;
        } else {
          setErrorMessage("Not supported for non  modular feature list rows");
          setStatus(TaskStatus.ERROR);
          return;
        }
      }
    }

    // Add new aligned feature list to the project
    project.addFeatureList(alignedFeatureList);

    // Add task description to peakList
    alignedFeatureList
        .addDescriptionOfAppliedTask(
            new SimpleFeatureListAppliedMethod("Feature list merger", MergeAlignerModule.class,
                parameters, getModuleCallDate()));

    logger.info("Finished feature list merger");

    setStatus(TaskStatus.FINISHED);
  }

}
