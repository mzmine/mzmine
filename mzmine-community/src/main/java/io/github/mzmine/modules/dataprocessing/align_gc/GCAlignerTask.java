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

package io.github.mzmine.modules.dataprocessing.align_gc;

import static io.github.mzmine.util.FeatureListRowSorter.DEFAULT_RT;
import static java.util.Comparator.comparingInt;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.align_join.RowVsRowScore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GCAlignerTask extends AbstractFeatureListTask {

  private static final Logger LOGGER = Logger.getLogger(GCAlignerTask.class.getName());
  private final AtomicInteger alignedRows = new AtomicInteger(0);
  private final MZmineProject project;
  private final ParameterSet parameters;
  private final List<FeatureList> featureLists;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final MZmineProcessingStep<SpectralSimilarityFunction> similarityFunction;
  private final String featureListName;
  private ModularFeatureList alignedFeatureList;
  private int totalRows;
  private int iteration = 1;

  public GCAlignerTask(MZmineProject project, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.project = project;
    this.parameters = parameters;
    this.featureLists = Arrays.stream(
        parameters.getParameter(GCAlignerParameters.FEATURE_LISTS).getValue()
            .getMatchingFeatureLists()).map(featureList -> (FeatureList) featureList).toList();
    this.mzTolerance = parameters.getValue(GCAlignerParameters.MZ_TOLERANCE);
    this.rtTolerance = parameters.getValue(GCAlignerParameters.RT_TOLERANCE);
    this.similarityFunction = parameters.getValue(GCAlignerParameters.SIMILARITY_FUNCTION);
    this.featureListName = parameters.getValue(GCAlignerParameters.FEATURE_LIST_NAME);
  }

  @Override
  protected void process() {
    // check for pseudo spectra, else return

    // Compare RT

    //Compare similarity

    //find highest feature -> redo feature finding if necessary

    // Remember how many rows we need to process. Each row will be processed
    // twice, first for score calculation, second for actual alignment.
    for (FeatureList list : featureLists) {
      totalRows += list.getNumberOfRows();
    }

    // Collect all data files
    final List<RawDataFile> allDataFiles = FeatureListUtils.getAllDataFiles(featureLists);
    if (allDataFiles.isEmpty()) {
      return;
    }

    // Create a new aligned feature list based on the baseList and renumber IDs
    alignedFeatureList = new ModularFeatureList(featureListName, getMemoryMapStorage(),
        allDataFiles);
    FeatureListUtils.transferRowTypes(alignedFeatureList, featureLists);
    FeatureListUtils.transferSelectedScans(alignedFeatureList, featureLists);
    final AtomicInteger newRowID = new AtomicInteger(1);

    // list all rows for each feature list
    final List<List<FeatureListRow>> allRows = new ArrayList<>(featureLists.size());

    for (var flist : featureLists) {
      allRows.add(new ArrayList<>(flist.getRows()));
    }

    // still contains rows from unaligned feature lists
    while (!allRows.isEmpty()) {
      // sort remaining unaligned rows by size
      // feature list with the highest number of unaligned rows first
      allRows.sort(comparingInt(value -> ((List<?>) value).size()).reversed());

      // remove next feature list's rows
      // select the next base feature list with max number of rows
      final List<FeatureListRow> nextUnalignedFeatureList = allRows.remove(0);
      if (nextUnalignedFeatureList.isEmpty()) {
        LOGGER.finer(() -> String.format(
            "End of GC aligner reached. %d feature lists had no unaligned rows left",
            allRows.size()));
        break; // end loop
      }

      // create new rows, used as base for next alignment iteration, and later added to feature list
      List<FeatureListRow> nextBaseRows = new ArrayList<>(nextUnalignedFeatureList.size());
      for (var unalignedRow : nextUnalignedFeatureList) {
        nextBaseRows.add(new ModularFeatureListRow(alignedFeatureList, newRowID.getAndIncrement(),
            (ModularFeatureListRow) unalignedRow, true));
      }
      nextBaseRows.sort(DEFAULT_RT);

      // align all remaining feature lists onto the feature list with max(row number) = nextBaseRows
      if (!allRows.isEmpty()) {
        alignRowsOnBaseRows(allRows, nextBaseRows);
      }

      // add all new base rows
      for (var row : nextBaseRows) {
        alignedFeatureList.addRow(row);
      }
      iteration++;
    }

    // sort by RT and reset IDs
    FeatureListUtils.sortByDefaultRT(alignedFeatureList, true);

    // update row bindings
    alignedFeatureList.parallelStream().filter(row -> row.getNumberOfFeatures() > 1)
        .forEach(FeatureListRow::applyRowBindings);

    // score alignment by the number of features that fall within the mz, RT, mobility range
    // do not apply all the advanced filters to keep it simple
    //  RowAlignmentScoreCalculator calculator = new RowAlignmentScoreCalculator(featureLists,
    //      mzTolerance, rtTolerance, mobTol, mzWeight, rtWeight, mobilityWeight);
    //  FeatureListUtils.addAlignmentScores(alignedFeatureList, calculator, false);

    // applied methods
    alignedFeatureList.getAppliedMethods().addAll(featureLists.get(0).getAppliedMethods());
    // Add task description to peakList
    alignedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("GC aligner", GCAlignerModule.class, parameters,
            getModuleCallDate()));
    // Add new aligned feature list to the project {
    project.addFeatureList(alignedFeatureList);

    LOGGER.info("Finished GC aligner");
  }

  /**
   * all unaligned rows are checked against the list of base rows
   *
   * @param unalignedRows FeatureList<Rows>
   * @param baseRowsByRt  list of base rows sorted by rt
   */
  private void alignRowsOnBaseRows(List<List<FeatureListRow>> unalignedRows,
      List<FeatureListRow> baseRowsByRt) {

    // key = a row to be aligned, value = all possible matches in the aligned fl and it's scores
    final ConcurrentLinkedDeque<RowVsRowScore> scoresList = new ConcurrentLinkedDeque<>();

    // stream all rows in all feature lists
    unalignedRows.stream().flatMap(Collection::stream).parallel().forEach(rowToAdd -> {
      if (isCanceled()) {
        return;
      }

      final Range<Float> rtRange = rtTolerance.getToleranceRange(rowToAdd.getAverageRT());
      // find all rows in the aligned rows that might match
      final List<FeatureListRow> candidatesInAligned = FeatureListUtils.getCandidatesWithinRtRange(
          rtRange, baseRowsByRt, true);
      if (candidatesInAligned.isEmpty()) {
        return;
      }

      // calculate score for unaligned row against all candidates
      for (FeatureListRow candidateInAligned : candidatesInAligned) {
        // retention time is already checked for candidates
        SpectralSimilarity similarity = checkSpectralSimilarity(rowToAdd, candidateInAligned);
        if (similarity != null) {
          final RowVsRowScore score = new RowVsRowScore(rowToAdd, candidateInAligned, rtRange, 1,
              similarity.getScore(), 2);
          scoresList.add(score);
        }
      }
    });

    // after an iteration, rows of all other featureLists have been given a mapping
    // now we have to find the best match
    // track all aligned rows - only align to highest scoring row
    final var alignedRowsMap = addFeaturesBasedOnScores(scoresList);

    // keep track of unaligned rows for the next interation.
    removeAlignedRows(unalignedRows, alignedRowsMap);
  }

  private SpectralSimilarity checkSpectralSimilarity(FeatureListRow row, FeatureListRow candidate) {

    DataPoint[] rowDPs = null;
    DataPoint[] candidateDPs = null;
    SpectralSimilarity sim;

    // get data points of mass list of the best
    // fragmentation scans
    if (row.getMostIntenseFragmentScan() != null
        && candidate.getMostIntenseFragmentScan() != null) {
      rowDPs = row.getMostIntenseFragmentScan().getMassList().getDataPoints();
      candidateDPs = candidate.getMostIntenseFragmentScan().getMassList().getDataPoints();
    } else {
      return null;
    }

    // compare mass list data points of selected scans
    if (rowDPs != null && candidateDPs != null) {
      sim = similarityFunction.getModule()
          .getSimilarity(similarityFunction.getParameterSet(), mzTolerance, 0, rowDPs,
              candidateDPs);
      return sim;
    }
    return null;
  }

  @NotNull
  private Object2BooleanOpenHashMap<FeatureListRow> addFeaturesBasedOnScores(
      ConcurrentLinkedDeque<RowVsRowScore> scoresList) {
    // natural order is reversed so best highest score is first element
    final RowVsRowScore[] scores = scoresList.stream().sorted().toArray(RowVsRowScore[]::new);

    // track if row was aligned
    final Object2BooleanOpenHashMap<FeatureListRow> alignedRowsMap = new Object2BooleanOpenHashMap<>(
        scores.length);

    for (RowVsRowScore score : scores) {
      final FeatureListRow alignedRow = score.getAlignedBaseRow();
      final FeatureListRow row = score.getRowToAdd();
      if (!alignedRowsMap.getOrDefault(row, false)) {
        // no row was aligned
        // put all features of the row into the aligned row
        for (Feature feature : row.getFeatures()) {
          final RawDataFile dataFile = feature.getRawDataFile();
          if (!alignedRow.hasFeature(dataFile)) {
            Range<Double> mzTolRange = mzTolerance.getToleranceRange(alignedRow.getAverageMZ());
            if (mzTolRange.contains(feature.getMZ())) {
              alignedRow.addFeature(dataFile, new ModularFeature(alignedFeatureList, feature),
                  false);
            } else {
              IonTimeSeries ionTimeSeries = IonTimeSeriesUtils.extractIonTimeSeries(dataFile,
                  feature.getScanNumbers(), mzTolRange, feature.getRawDataPointsRTRange(),
                  dataFile.getMemoryMapStorage());
              alignedRow.addFeature(dataFile,
                  new ModularFeature(alignedFeatureList, dataFile, ionTimeSeries,
                      FeatureStatus.DETECTED), false);
            }
            alignedRowsMap.put(row, true);
            this.alignedRows.getAndIncrement();
          }
        }
      }
    }
    return alignedRowsMap;
  }

  private void removeAlignedRows(List<List<FeatureListRow>> allRows,
      Object2BooleanOpenHashMap<FeatureListRow> alignedRowsMap) {
    AtomicInteger alignedCounter = new AtomicInteger(0);
    AtomicInteger remainingCounter = new AtomicInteger(0);
    final ListIterator<List<FeatureListRow>> iterator = allRows.listIterator();
    while (iterator.hasNext()) {
      // remove aligned rows
      final List<FeatureListRow> featureList = iterator.next();
      featureList.removeIf(row -> {
        if (alignedRowsMap.getOrDefault(row, false)) {
          alignedCounter.incrementAndGet();
          return true;
        }
        remainingCounter.incrementAndGet();
        return false;
      });
      // remove empty lists
      if (featureList.isEmpty()) {
        iterator.remove();
      }
    }
    LOGGER.finest(() -> String.format("Rows: %d aligned; %d remaining. Iteration %d/%d (max)",
        alignedCounter.get(), remainingCounter.get(), iteration, featureLists.size()));
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return featureLists;
  }

  @Override
  public String getTaskDescription() {
    return "Align GC feature lists";
  }
}
