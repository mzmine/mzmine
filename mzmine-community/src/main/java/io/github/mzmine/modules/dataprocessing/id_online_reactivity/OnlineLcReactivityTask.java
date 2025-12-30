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

package io.github.mzmine.modules.dataprocessing.id_online_reactivity;

import static java.util.stream.Collectors.toCollection;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.online_reaction.OnlineLcReactionMatchType;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReaction.Type;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
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

  private final String description;
  private final List<IonType> eductAdducts;
  private final List<IonType> productAdducts;
  private final MetadataColumn<?> sampleIdCol;
  private final MetadataColumn<?> unreactedControlsCol;

  public OnlineLcReactivityTask(@NotNull ParameterSet parameters, FeatureList flist,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate, parameters, OnlineLcReactivityModule.class);
    this.flist = flist;
    reactionsFile = parameters.getValue(OnlineLcReactivityParameters.reactionsFile);
    mzTol = parameters.getValue(OnlineLcReactivityParameters.mzTol);
    onlyGroupedRows = parameters.getValue(OnlineLcReactivityParameters.onlyGroupedRows);
    eductAdducts = parameters.getValue(OnlineLcReactivityParameters.eductAdducts);
    productAdducts = parameters.getValue(OnlineLcReactivityParameters.productAdducts);

    MetadataTable metadata = MZmineCore.getProjectMetadata();
    sampleIdCol = parameters.getOptionalValue(OnlineLcReactivityParameters.uniqueSampleId)
        .map(metadata::getColumnByName).orElse(null);
    unreactedControlsCol = parameters.getOptionalValue(
        OnlineLcReactivityParameters.unreactedControls).map(metadata::getColumnByName).orElse(null);

    description = "Online reactivity task on " + flist.getName();
  }

  @NotNull
  private static List<@Nullable ReactionMatchingRawFiles> filterReactionsByRowRawFiles(
      final List<ReactionMatchingRawFiles> reactions, final FeatureListRow rowA) {
    return reactions.stream().map(reaction -> filterRawFilePresentInFeatureRow(reaction, rowA))
        .filter(Objects::nonNull).toList();
  }

  @Nullable
  private static ReactionMatchingRawFiles filterRawFilePresentInFeatureRow(
      final ReactionMatchingRawFiles reaction, final FeatureListRow a) {
    // filter
    var filteredRaws = reaction.raws().stream().filter(a::hasFeature).toList();
    return filteredRaws.isEmpty() ? null
        : new ReactionMatchingRawFiles(reaction.reaction(), filteredRaws);
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  /**
   * Create a list that contains the original reactions and adduct cross reactions
   *
   * @return new list
   */
  public static List<ReactionMatchingRawFiles> createAdductReactions(
      final List<ReactionMatchingRawFiles> reactions, final List<IonType> eductAdducts,
      final List<IonType> productAdducts) {
    return reactions.stream().<ReactionMatchingRawFiles>mapMulti((reaction, consumer) -> {
      List<OnlineReaction> adductReactions = reaction.reaction()
          .createCrossAdductReactions(eductAdducts, productAdducts);

      // add reaction and then all cross reactions
      consumer.accept(reaction);
      for (final OnlineReaction adductReaction : adductReactions) {
        consumer.accept(new ReactionMatchingRawFiles(adductReaction, reaction.raws()));
      }
    }).collect(toCollection(ArrayList::new));
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
    List<ReactionMatchingRawFiles> reactions = loadReactionsFilteredByRawFiles(raws);

    if (reactions.isEmpty()) {
      if (!isCanceled()) {
        setStatus(TaskStatus.FINISHED);
      }
      return;
    }

    // TODO add a map that links raw data files to their unreacted controls
    // user input by metadata sheet or filename pattern
    // unreacted_control_mix32_pos_suid.mzml is the control of, including,
    // mix32_pos_REACTION.mzml
    // if products are found in control - then remove annotation

    // may create even more reactions if adducts are set by the user
    // needs at least one adduct for both educt and product to create new reactions like
    // reaction, [M+H]+ → [M+Na]+
    if (!(eductAdducts.isEmpty() || productAdducts.isEmpty())) {
      reactions = createAdductReactions(reactions, eductAdducts, productAdducts);
    }

    // sort rows by mz and reactions by delta mz
    reactions.sort(Comparator.comparingDouble(reaction -> reaction.reaction().deltaMz()));

    flist.addRowType(DataTypes.get(OnlineLcReactionMatchType.class));

    // products cannot be added to the rows directly because of concurrent modifications
    // collect all and add them later on one thread
    ConcurrentLinkedQueue<OnlineReactionMatch> productMatchesToAdd = new ConcurrentLinkedQueue<>();

    // stream rows in parallel, use map instead of forEach to make sure its executed directly
    int numRows = rows.size();
    final var finalReactions = reactions;
    long comparedPairs = IntStream.range(0, numRows - 1).parallel().mapToLong(
        i -> processRowAddMatches(i, rows, raws, finalReactions, correlationMap,
            productMatchesToAdd)).sum();

    if (isCanceled()) {
      return;
    }

    // add all products, rowA is always to row to add to
    Map<FeatureListRow, List<OnlineReactionMatch>> groupedByRow = productMatchesToAdd.stream()
        .collect(Collectors.groupingBy(OnlineReactionMatch::getProductRow));

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

  /**
   * Load reactions and filter them out if there is no raw data file matching
   *
   * @return modifiable list
   */
  @NotNull
  private List<ReactionMatchingRawFiles> loadReactionsFilteredByRawFiles(
      final List<RawDataFile> raws) {
    return loadReactions(reactionsFile).stream().map(reaction -> filterByRawFiles(reaction, raws))
        .filter(Objects::nonNull).collect(toCollection(ArrayList::new));
  }

  private int processRowAddMatches(final int indexA, final List<FeatureListRow> rows,
      final List<RawDataFile> raws, final List<ReactionMatchingRawFiles> reactions,
      final R2RMap<RowsRelationship> correlationMap,
      final ConcurrentLinkedQueue<OnlineReactionMatch> productMatchesToAdd) {
    int numRows = rows.size();

    int matches = 0;
    if (isCanceled()) {
      return matches;
    }

    final FeatureListRow rowA = rows.get(indexA);
    final double mzA = rowA.getAverageMZ();

    // results in this list and add later
    List<OnlineReactionMatch> matchesToAdd = new ArrayList<>(rowA.getOnlineReactionMatches());

    // filter reactions by filenames that need to contain a specific string
    // only beneficial if more raw data files to filter
    final List<ReactionMatchingRawFiles> filteredReactions =
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

    for (; indexB < numRows && startReaction < filteredReactions.size(); indexB++) {
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
        ReactionMatchingRawFiles reaction = filteredReactions.get(r);
        Range<Double> mzRange = mzRanges[r];
        if (mzB < mzRange.lowerEndpoint()) {
          break; // mz is less than lowest reaction --> check next row
        } else if (mzB > mzRange.upperEndpoint()) {
          startReaction++;
        } else if (anyRawFileOverlapWithReaction(reaction, rowA, rowB)) {
          // match needs to happen also in the same raw data file
          // a is educt b is product
          matchesToAdd.add(new OnlineReactionMatch(rowA, rowB, reaction.reaction(), Type.Educt));
          matches++;

          // add products later on one thread
          // can use the same object
          productMatchesToAdd.add(
              new OnlineReactionMatch(rowA, rowB, reaction.reaction(), Type.Product));
        }
      }
    }
    rowA.set(OnlineLcReactionMatchType.class, matchesToAdd);
    return matches;
  }

  private boolean anyRawFileOverlapWithReaction(final ReactionMatchingRawFiles reaction,
      final FeatureListRow a, final FeatureListRow b) {
    return reaction.raws().stream().anyMatch(raw -> a.hasFeature(raw) && b.hasFeature(raw));
  }

  /**
   * @return null if no matching raw data file - otherwise all matching raw data files for this
   * reaction
   */
  @Nullable
  private ReactionMatchingRawFiles filterByRawFiles(final OnlineReaction reaction,
      final List<RawDataFile> raws) {
    List<RawDataFile> filtered = filterRawDataFilesWithReactionsSubstring(raws, reaction);
    return filtered.isEmpty() ? null : new ReactionMatchingRawFiles(reaction, filtered);
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
      error("Cannot load file " + reactionsFile.getAbsolutePath(), e);
      return List.of();
    }
  }

}
