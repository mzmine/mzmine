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

package io.github.mzmine.modules.dataprocessing.align_common;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.align_join.RowVsRowScore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.progress.TotalFinishedItemsProgress;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.mzio.links.MzioMZmineLinks;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Orchestrates the alignment process with: prechecks, alignment, sorting of final list, renumbering
 * of IDs. This abstracts away much of the complex scheduling of the baseRows and aligned Rows.
 */
public class BaseFeatureListAligner {

  private static final Logger logger = Logger.getLogger(BaseFeatureListAligner.class.getName());
  private final Task parentTask;
  private final List<FeatureList> featureLists;
  private final String featureListName;
  private final MemoryMapStorage storage;
  private final FeatureRowAlignScorer rowAligner;
  private final FeatureAlignmentPostProcessor postProcessor;
  private final FeatureCloner featureCloner;
  private final FeatureListRowSorter baseRowSorter;
  private final TotalFinishedItemsProgress progress = new TotalFinishedItemsProgress();
  private int iteration = 1;

  public BaseFeatureListAligner(final Task parentTask, final List<FeatureList> featureLists,
      final String featureListName, final @Nullable MemoryMapStorage storage,
      final FeatureRowAlignScorer rowAligner, final FeatureCloner featureCloner,
      final FeatureListRowSorter baseRowSorter, final @Nullable FeatureAlignmentPostProcessor postProcessor) {

    this.parentTask = parentTask;
    this.featureLists = featureLists;
    this.featureListName = featureListName;
    this.storage = storage;
    this.rowAligner = rowAligner;
    this.featureCloner = featureCloner;
    this.baseRowSorter = baseRowSorter;
    this.postProcessor = postProcessor;
  }

  /**
   * Create new aligned list from all lists. Check for duplicate RawDataFile in all lists, if
   * duplicated then fail with IllegalArgumentException
   *
   * @param featureLists    input lists
   * @param featureListName the new feature list name
   * @param storage         the storage to use
   * @return the new aligned feature list or null if there were 0 RawDataFIles
   */
  @Nullable
  public static ModularFeatureList createEmptyAlignedList(final List<FeatureList> featureLists,
      final String featureListName, final @Nullable MemoryMapStorage storage) {
    // Collect all data files
    final List<RawDataFile> allDataFiles = FeatureListUtils.getAllDataFiles(featureLists);
    if (allDataFiles.isEmpty()) {
      return null;
    }

    // Create a new aligned feature list based on the baseList and renumber IDs
    var alignedFeatureList = new ModularFeatureList(featureListName, storage, allDataFiles);
    FeatureListUtils.transferRowTypes(alignedFeatureList, featureLists, true);
    FeatureListUtils.transferSelectedScans(alignedFeatureList, featureLists);
    FeatureListUtils.copyPeakListAppliedMethods(featureLists.getFirst(), alignedFeatureList);
    return alignedFeatureList;
  }

  @NotNull
  public static Object2BooleanOpenHashMap<FeatureListRow> addFeaturesBasedOnScores(
      Collection<RowVsRowScore> scoresList, final ModularFeatureList alignedFeatureList,
      final FeatureCloner featureCloner, final AtomicLong alignedRows) {
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
            var newFeature = featureCloner.cloneFeature(feature, alignedFeatureList, alignedRow);
            // very important to not trigger row bindings - GC-EI currently has features with different mz
            // this is resolved later by a GCConsensunsPostProcessor
            // row bindings are then updated at last
            alignedRow.addFeature(dataFile, newFeature, false);
            alignedRowsMap.put(row, true);
            alignedRows.getAndIncrement();
          }
        }
      }
    }

    return alignedRowsMap;
  }

  /**
   * Remove all rows that were algined in this step. Modifies the argument list
   *
   * @param allRows        FeatureList<List<Rows>>
   * @param alignedRowsMap marks all aligned rows
   * @return number of aligned and remaining rows
   */
  public static AlignedRemainingRows removeAlignedRows(List<List<FeatureListRow>> allRows,
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
    return new AlignedRemainingRows(alignedCounter.get(), remainingCounter.get());
  }

  public ModularFeatureList alignFeatureLists() {
    // Remember how many rows we need to process. Each row will be processed
    // twice, first for score calculation, second for actual alignment.
    long totalRows = featureLists.stream().mapToLong(FeatureList::getNumberOfRows).sum();
    progress.setTotal(totalRows);

    // open dialog if there may be too much work
    checkTotalWorkloadAndMemory(totalRows);

    var alignedFeatureList = createEmptyAlignedList(featureLists, featureListName, storage);
    if (alignedFeatureList == null) {
      return null; // issue during creation
    }

    final AtomicInteger newRowID = new AtomicInteger(1);

    // list all rows for each feature list
    final List<List<FeatureListRow>> allRows = new ArrayList<>(featureLists.size());

    // sort feature lists by name to make reproducible
    // this is needed if 2 feature lists have the same number of rows, which will lead to different results
    featureLists.stream().sorted(comparing(FeatureList::getName)).forEach(flist -> {
      allRows.add(new ArrayList<>(flist.getRows()));
    });

    // still contains rows from unaligned feature lists
    while (!allRows.isEmpty()) {
      if (parentTask.isCanceled()) {
        return null;
      }
      var finishedAll = nextAlignmentIteration(allRows, alignedFeatureList, newRowID);
      if (finishedAll) {
        break; // end loop
      }
      iteration++;
    }

    // apply special handling - like GC-EI consensus feature finding
    if (postProcessor != null) {
      postProcessor.handlePostAlignment(alignedFeatureList);
    }

    // first update row bindings
    alignedFeatureList.parallelStream().filter(row -> row.getNumberOfFeatures() > 1)
        .forEach(FeatureListRow::applyRowBindings);

    // then sort by RT and reset IDs
    FeatureListUtils.sortByDefaultRT(alignedFeatureList, true);


    // score alignment by the number of features that fall within the mz, RT, mobility range
    // do not apply all the advanced filters to keep it simple
    rowAligner.calculateAlignmentScores(alignedFeatureList, featureLists);

    return alignedFeatureList;
  }

  /**
   * Check the estimated memory requirements for this run
   */
  private void checkTotalWorkloadAndMemory(final long totalRows) {
    // after alignment:  25000 rows x 250 samples = 7 GB
    // before alignment: 250 feature lists: 4,011,743 features to be aligned 9 GB
    // during end of alignment (both aligned and non aligned lists present): 15 GB
    final int rowsPerList = (int) (totalRows / featureLists.size());
    final double imsCorrectionFactor = featureLists.stream()
        .mapToDouble(FeatureListUtils::getImsRamFactor).average().orElse(1d);
    final double gbMemoryPerMillionFeatures =
        3.74 * imsCorrectionFactor; // this is from 15 GB per 4M features
    final double maxMemoryGB = ConfigService.getConfiguration().getMaxMemoryGB();
    final double expectedRamUsage = gbMemoryPerMillionFeatures / 1_000_000 * totalRows;

    logger.info("""
        Alignment started on a total of %d rows across %d samples (mean %d rows). \
        Max memory available: %.1f GB. Expecting to use %.1f GB.""".formatted(totalRows,
        featureLists.size(), rowsPerList, maxMemoryGB, expectedRamUsage));

    // estimate if there might be an issue with this size and memory
    if (expectedRamUsage > maxMemoryGB * 0.85) {
      DialogLoggerUtil.showMessageDialog("Large dataset feature alignment", false,
          FxTextFlows.newTextFlow(FxTexts.text("""
                  mzmine feature alignment started on %d total features across %d samples.
                  This may result in a large aligned feature list and memory constraints.
                  Consider applying higher thresholds during chromatogram builder and feature resolving, /
                  such as increased minimum height, chromatographic threshold, and feature top/edge ratio in the local minimum resolver.
                  When working on large datasets, consult the performance documentation for tuning options:
                  """.formatted(totalRows, featureLists.size())),
              FxTexts.hyperlinkText(MzioMZmineLinks.PERFORMANCE_DOCU.getUrl())));
    }
  }

  private boolean nextAlignmentIteration(final List<List<FeatureListRow>> allRows,
      final ModularFeatureList alignedFeatureList, final AtomicInteger newRowID) {
    // sort remaining unaligned rows by size
    // feature list with the highest number of unaligned rows first
    allRows.sort(comparingInt(value -> ((List<?>) value).size()).reversed());

    // remove next feature list's rows
    // select the next base feature list with max number of rows
    final List<FeatureListRow> nextUnalignedFeatureList = allRows.removeFirst();
    if (nextUnalignedFeatureList.isEmpty()) {
      logger.finer(
          () -> String.format("End of aligner reached. %d feature lists had no unaligned rows left",
              allRows.size()));
      return true;
    }

    // create new rows, used as base for next alignment iteration, and later added to feature list
    List<FeatureListRow> nextBaseRows = new ArrayList<>(nextUnalignedFeatureList.size());
    for (var unalignedRow : nextUnalignedFeatureList) {
      nextBaseRows.add(new ModularFeatureListRow(alignedFeatureList, newRowID.getAndIncrement(),
          (ModularFeatureListRow) unalignedRow, true));
    }
    // either by mz in Join or by RT by GC
    nextBaseRows.sort(baseRowSorter);

    // align all remaining feature lists onto the feature list with max(row number) = nextBaseRows
    if (!allRows.isEmpty()) {
      Collection<RowVsRowScore> scoresList = rowAligner.alignRowsOnBaseRows(parentTask, allRows,
          nextBaseRows);

      // after an iteration, rows of all other featureLists have been given a mapping
      // now we have to find the best match
      // track all aligned rows - only align to highest scoring row
      final var alignedRowsMap = addFeaturesBasedOnScores(scoresList, alignedFeatureList,
          featureCloner, progress.getFinished());

      // keep track of unaligned rows for the next interation.
      AlignedRemainingRows result = removeAlignedRows(allRows, alignedRowsMap);
      result.logStatus(iteration, featureLists.size());
    }

    // add all new base rows
    for (var row : nextBaseRows) {
      alignedFeatureList.addRow(row);
    }
    return false;
  }

  public double getFinishedPercentage() {
    return progress.progress();
  }

}
