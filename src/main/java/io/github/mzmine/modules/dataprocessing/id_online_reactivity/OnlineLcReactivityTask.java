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

package io.github.mzmine.modules.dataprocessing.id_online_reactivity;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.AbstractRowsRelationship;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.online_reaction.OnlineLcReactionMatchType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReaction.Type;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.io.CsvReader;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OnlineLcReactivityTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(OnlineLcReactivityTask.class.getName());

  private final File reactionsFile;
  private final MZTolerance mzTol;
  private final FeatureList flist;
  private final boolean onlyGroupedRows;

  private final AtomicLong finishedItems = new AtomicLong(0);
  private String description;

  public OnlineLcReactivityTask(@NotNull ParameterSet parameters, FeatureList flist,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate, parameters, OnlineLcReactivityModule.class);
    this.flist = flist;
    reactionsFile = parameters.getValue(OnlineLcReactivityParameters.filePath);
    mzTol = parameters.getValue(OnlineLcReactivityParameters.mzTol);
    onlyGroupedRows = parameters.getValue(OnlineLcReactivityParameters.onlyGroupedRows);

    description = "Online reactivity task on " + flist.getName();
  }

  @NotNull
  private static List<@Nullable ReactionsMatchingRawFiles> filterReactionsByRowRawFiles(
      final List<ReactionsMatchingRawFiles> reactions, final FeatureListRow rowA) {
    return reactions.stream().map(reaction -> filterRawFilePresentInFeatureRow(reaction, rowA))
        .filter(Objects::nonNull).toList();
  }

  @Nullable
  private static ReactionsMatchingRawFiles filterRawFilePresentInFeatureRow(
      final ReactionsMatchingRawFiles reaction, final FeatureListRow a) {
    // filter
    var filteredRaws = reaction.raws().stream().filter(a::hasFeature).toList();
    return filteredRaws.isEmpty() ? null
        : new ReactionsMatchingRawFiles(reaction.reaction(), filteredRaws);
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public long getFinishedItems() {
    return finishedItems.get();
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {
    List<FeatureListRow> rows = flist.getRows().stream().sorted(FeatureListRowSorter.MZ_ASCENDING)
        .toList();
    if (rows.isEmpty()) {
      logger.info("Empty feature list " + flist.getName());
      setStatus(TaskStatus.FINISHED);
      return;
    }

    R2RMap<RowsRelationship> correlationMap = flist.getMs1CorrelationMap().orElse(null);
    if (onlyGroupedRows && (correlationMap == null || correlationMap.isEmpty())) {
      MZmineCore.getDesktop()
          .displayMessage("Run correlation grouping before running this module " + flist.getName());
      setStatus(TaskStatus.FINISHED);
      return;
    }

    List<RawDataFile> raws = flist.getRawDataFiles();

    // reactions are filter out if they dont match any raw data files
    List<ReactionsMatchingRawFiles> reactions = loadReactions(reactionsFile).stream()
        .map(reaction -> filterByRawFiles(reaction, raws)).filter(Objects::nonNull)
        .collect(Collectors.toCollection(ArrayList::new));

    if (reactions.isEmpty()) {
      if (!isCanceled()) {
        setStatus(TaskStatus.FINISHED);
      }
      return;
    }
    // sort rows by mz and reactions by delta mz
    reactions.sort(Comparator.comparingDouble(reaction -> reaction.reaction().deltaMz()));

    flist.addRowType(DataTypes.get(OnlineLcReactionMatchType.class));

    // products cannot be added to the rows directly because of concurrent modifications
    // collect all and add them later on one thread
    ConcurrentLinkedQueue<OnlineReactionMatch> productMatchesToAdd = new ConcurrentLinkedQueue<>();

    // stream rows in parallel, use map instead of forEach to make sure its executed directly
    int numRows = rows.size();
    long comparedPairs = IntStream.range(0, numRows - 1).parallel().mapToLong(
            i -> processRowAddMatches(i, rows, raws, reactions, correlationMap, productMatchesToAdd))
        .sum();

    if (isCanceled()) {
      return;
    }

    // add all products, rowA is always to row to add to
    Map<FeatureListRow, List<OnlineReactionMatch>> groupedByRow = productMatchesToAdd.stream()
        .collect(Collectors.groupingBy(AbstractRowsRelationship::getRowA));

    groupedByRow.forEach((row, matches) -> row.set(OnlineLcReactionMatchType.class, matches));

    // convert to edges and add them to the feature list
    R2RMap<OnlineReactionMatch> r2rMap = new R2RMap<>();
    for (final FeatureListRow row : rows) {
      int id = row.getID();
      List<OnlineReactionMatch> matches = row.getOnlineReactionMatches();
      if (matches.isEmpty()) {
        continue;
      }
      for (final OnlineReactionMatch match : matches) {
        int id2 = match.getPartnerRowId();
        // only add if ID is smaller than partner id
        if (id2 < id) {
          continue;
        }
        r2rMap.add(match.getRowA(), match.getRowB(), match);
      }
    }
    flist.getRowMaps().addAllRowsRelationships(r2rMap, RowsRelationship.Type.ONLINE_REACTION);
  }

  private int processRowAddMatches(final int indexA, final List<FeatureListRow> rows,
      final List<RawDataFile> raws, final List<ReactionsMatchingRawFiles> reactions,
      final R2RMap<RowsRelationship> correlationMap,
      final ConcurrentLinkedQueue<OnlineReactionMatch> productMatchesToAdd) {
    int numRows = rows.size();

    int matches = 0;
    if (isCanceled()) {
      return matches;
    }

    FeatureListRow rowA = rows.get(indexA);
    double mzA = rowA.getAverageMZ();

    // results in this list and add later
    List<OnlineReactionMatch> matchesToAdd = new ArrayList<>(rowA.getOnlineReactionMatches());

    // filter reactions by filenames that need to contain a specific string
    // only beneficial if more raw data files to filter
    final List<ReactionsMatchingRawFiles> filteredReactions =
        raws.size() > 2 ? filterReactionsByRowRawFiles(reactions, rowA) : reactions;

    // short circuit
    if (filteredReactions.isEmpty()) {
      return matches;
    }

    // reactions and rows are sorted by mz so jump to the first reaction mz
    Range<Double>[] mzRanges = filteredReactions.stream()
        .map(reaction -> mzTol.getToleranceRange(mzA + reaction.reaction().deltaMz()))
        .toArray(Range[]::new);
    int startReaction = 0;

    // binary search first with lowest mz
    int indexB = BinarySearch.binarySearch(mzRanges[0].lowerEndpoint(), DefaultTo.GREATER_EQUALS,
        indexA + 1, rows.size(), index -> rows.get(index).getAverageMZ());

    if (indexB == -1) {
      // out of range already
      return matches;
    }

    for (; indexB < numRows; indexB++) {
      if (isCanceled()) {
        return matches;
      }

      FeatureListRow rowB = rows.get(indexB);

      // filter rows that are correlated
      if (onlyGroupedRows && !correlationMap.contains(rowA, rowB)) {
        continue; // no correlation between rows
      }
      double mzB = rowB.getAverageMZ();
      for (int r = startReaction; r < filteredReactions.size(); r++) {
        ReactionsMatchingRawFiles reaction = filteredReactions.get(r);
        Range<Double> mzRange = mzRanges[r];
        if (mzB < mzRange.lowerEndpoint()) {
          startReaction++;
        } else if (mzB > mzRange.upperEndpoint()) {
          break; // check next row
        } else if (anyRawFileOverlapWithReaction(reaction, rowA, rowB)) {
          // match needs to happen also in the same raw data file
          OnlineReactionMatch match = new OnlineReactionMatch(rowA, rowB, reaction.reaction(),
              Type.Educt);
          matchesToAdd.add(match);
          matches++;

          // add products later on one thread
          productMatchesToAdd.add(
              new OnlineReactionMatch(rowB, rowA, reaction.reaction(), Type.Product));
        }
      }
    }
    rowA.set(OnlineLcReactionMatchType.class, matchesToAdd);
    return matches;
  }

  private boolean anyRawFileOverlapWithReaction(final ReactionsMatchingRawFiles reaction,
      final FeatureListRow a, final FeatureListRow b) {
    return reaction.raws().stream().anyMatch(raw -> a.hasFeature(raw) && b.hasFeature(raw));
  }

  /**
   * @return null if no matching raw data file - otherwise all matching raw data files for this
   * reaction
   */
  @Nullable
  private ReactionsMatchingRawFiles filterByRawFiles(final OnlineReaction reaction,
      final List<RawDataFile> raws) {
    List<RawDataFile> filtered = filterRawDataFilesWithReactionsSubstring(raws, reaction);
    return filtered.isEmpty() ? null : new ReactionsMatchingRawFiles(reaction, filtered);
  }

  /**
   * @return all raw data files that contain the reaction substring
   */
  private List<RawDataFile> filterRawDataFilesWithReactionsSubstring(final List<RawDataFile> raws,
      final OnlineReaction reaction) {
    String substring = reaction.filenameContains().toLowerCase();
    return raws.stream().filter(raw -> raw.getName().toLowerCase().contains(substring)).toList();
  }

  @NotNull
  private List<OnlineReaction> loadReactions(final File reactionsFile) {
    try {
      return CsvReader.readToList(reactionsFile, OnlineReaction.class);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Cannot load file " + reactionsFile.getAbsolutePath(), e);
      setErrorMessage("Cannot load file " + reactionsFile.getAbsolutePath());
      setStatus(TaskStatus.ERROR);
      return List.of();
    }
  }

}
