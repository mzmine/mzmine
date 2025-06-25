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

package io.github.mzmine.modules.dataprocessing.align_lcimage;

import com.google.common.collect.Range;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.modules.dataprocessing.align_join.RowAlignmentScoreCalculator;
import io.github.mzmine.modules.dataprocessing.align_join.RowVsRowScore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LcImageAlignerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(LcImageAlignerTask.class.getName());
  private final Boolean useMobTol;
  private final String flistName;

  private final String description;
  private final double totalRows;

  private final double mobWeight;
  private final double mzWeight;
  private final AtomicDouble progress = new AtomicDouble(0);
  private final AtomicInteger scoredRows = new AtomicInteger(0);
  private final AtomicInteger processedScores = new AtomicInteger(0);
  private final AtomicInteger totalScores = new AtomicInteger(1);
  private final ParameterSet parameters;
  private final MZmineProject project;
  private final FeatureList lcFeatureList;
  private final List<FeatureList> imageLists;
  private final MZTolerance mzTol;
  private final MobilityTolerance mobTol;

  public LcImageAlignerTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull final ParameterSet parameters, @NotNull MZmineProject project) {
    super(storage, moduleCallDate);

    this.parameters = parameters;
    this.project = project;

    final ModularFeatureList[] matchingFeatureLists = parameters.getValue(
        LcImageAlignerParameters.flists).getMatchingFeatureLists();

    // checked in setup dialog
    imageLists = Arrays.stream(matchingFeatureLists)
        .filter(flist -> flist.hasFeatureType(ImageType.class)).map(list -> (FeatureList) list)
        .toList();

    final List<FeatureList> featureLists = Arrays.stream(matchingFeatureLists)
        .filter(flist -> !flist.hasFeatureType(ImageType.class)).map(list -> (FeatureList) list)
        .toList();
    if (featureLists.size() > 1) {
      logger.warning(
          "More than one DI/LC feature list selected. Using " + featureLists.get(0).getName());
    }
    lcFeatureList = featureLists.get(0);

    totalRows = imageLists.stream().mapToInt(FeatureList::getNumberOfRows).sum();
    mzTol = parameters.getValue(LcImageAlignerParameters.mzTolerance);
    useMobTol = parameters.getValue(LcImageAlignerParameters.mobTolerance);
    mobTol = parameters.getParameter(LcImageAlignerParameters.mobTolerance).getEmbeddedParameter()
        .getValue();
    mzWeight = parameters.getValue(LcImageAlignerParameters.mzWeight);
    mobWeight = parameters.getValue(LcImageAlignerParameters.mobilityWeight);
    flistName = parameters.getValue(LcImageAlignerParameters.name)
        .replace("{lc}", lcFeatureList.getName());
    description = "Aligning images from " + imageLists.stream().map(FeatureList::getName)
        .collect(Collectors.joining(", ")) + " on LC feature list " + lcFeatureList.getName();
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return scoredRows.get() / totalRows * 0.8d
           + processedScores.get() / (double) totalScores.get() * 0.2d;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // align images to all possible base list rows
    final ConcurrentLinkedDeque<RowVsRowScore> scores = new ConcurrentLinkedDeque<>();

    final List<FeatureList> sourceLists = new ArrayList<>();
    sourceLists.add(lcFeatureList);
    sourceLists.addAll(imageLists);
    final List<RawDataFile> allDataFiles = FeatureListUtils.getAllDataFiles(sourceLists);
    final ModularFeatureList alignedFlist = new ModularFeatureList(flistName, getMemoryMapStorage(),
        allDataFiles);
    FeatureListUtils.transferRowTypes(alignedFlist, sourceLists, true);
    FeatureListUtils.transferSelectedScans(alignedFlist, sourceLists);

    final List<FeatureListRow> lcRows = this.lcFeatureList.getRows().stream().map(
        row -> (FeatureListRow) new ModularFeatureListRow(alignedFlist, (ModularFeatureListRow) row,
            true)).sorted(FeatureListRowSorter.MZ_ASCENDING).toList();

    logger.finest(() -> "Copied " + lcRows.size() + " LC rows.");

    // score all rows (parallel)
    imageLists.stream().flatMap(FeatureList::stream).parallel().forEach(imageRow -> {
      if (isCanceled()) {
        return;
      }

      final Range<Double> mzRange = mzTol.getToleranceRange(imageRow.getAverageMZ());
      final double maxMzDiff = RangeUtils.rangeLength(mzRange) / 2;
      final Float mobility = imageRow.getAverageMobility();
      final Range<Float> mobRange =
          mobility != null && useMobTol ? mobTol.getToleranceRange(mobility) : Range.all();
      final double maxMobDiff = mobRange.equals(Range.all()) ? Double.POSITIVE_INFINITY
          : RangeUtils.rangeLength(mobRange) / 2;

      final List<FeatureListRow> matchingLcRows = FeatureListUtils.getCandidatesWithinRanges(
          mzRange, Range.all(), mobRange, lcRows, true);
      for (FeatureListRow lcRow : matchingLcRows) {
        RowVsRowScore score = new RowVsRowScore(imageRow, lcRow, mzRange, null, mobRange, null,
            mzWeight, 0, mobWeight, 0);
        scores.add(score);
      }
      scoredRows.getAndIncrement();
    });

    logger.finest(() -> "Created " + scores.size() + " RowVsRowScores.");

    if (isCanceled()) {
      return;
    }

    totalScores.set(scores.size());

    logger.finest("Aligning best images to their LC rows.");
    addFeaturesBasedOnScores(scores, alignedFlist);

    lcRows.forEach(alignedFlist::addRow);

    alignedFlist.getAppliedMethods().addAll(lcFeatureList.getAppliedMethods());
    alignedFlist.getAppliedMethods().addAll(
        new SimpleFeatureListAppliedMethod(LcImageAlignerModule.class, parameters,
            getModuleCallDate()));

    // score alignment by the number of features that fall within the mz, RT, mobility range
    // do not apply all the advanced filters to keep it simple
    MobilityTolerance mobTol = useMobTol ? this.mobTol : null;
    RowAlignmentScoreCalculator calculator = new RowAlignmentScoreCalculator(imageLists, mzTol,
        null, mobTol, mzWeight, 0, mobWeight);
    FeatureListUtils.addAlignmentScores(alignedFlist, calculator, true);

    if (isCanceled()) {
      return;
    }

    project.addFeatureList(alignedFlist);

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Copied from the join aligner, with the difference that we don't use the alignedRowsMap here, to
   * keep track of the candidate (=image) rows that we align on the base feature list. Instead, an
   * image feature may be present more than once in the resulting aligned feature list.
   */
  @NotNull
  private Object2BooleanOpenHashMap<FeatureListRow> addFeaturesBasedOnScores(
      ConcurrentLinkedDeque<RowVsRowScore> scoresList, ModularFeatureList alignedList) {
    // natural order is reversed so best highest score is first element
    final RowVsRowScore[] sortedScores = scoresList.stream().sorted().toArray(RowVsRowScore[]::new);

    // track if row was aligned
    final Object2BooleanOpenHashMap<FeatureListRow> alignedRowsMap = new Object2BooleanOpenHashMap<>(
        sortedScores.length);

    for (RowVsRowScore score : sortedScores) {
      final FeatureListRow alignedRow = score.getAlignedBaseRow(); // lc row = aligned row
      final FeatureListRow imageRow = score.getRowToAdd();
//      if (alignedRowsMap.getOrDefault(row, false)) {
//        logger.finest(() -> "Will align feature " + row.getID() + " multiple times");
//         in join aligner we would stop here
//      }
      for (Feature feature : imageRow.getFeatures()) {
        final RawDataFile dataFile = feature.getRawDataFile();
        if (!alignedRow.hasFeature(
            dataFile)) { // only add the best fit. If there is a fit already, don't align.
          alignedRow.addFeature(dataFile, new ModularFeature(alignedList, feature), false);
          alignedRowsMap.put(imageRow, true);
        }
      }
    }

    return alignedRowsMap;
  }
}
