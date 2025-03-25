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

package io.github.mzmine.util;

import static io.github.mzmine.util.FeatureListRowSorter.DEFAULT_RT;
import static io.github.mzmine.util.FeatureListRowSorter.MZ_ASCENDING;
import static io.github.mzmine.util.RangeUtils.calcCenterScore;
import static io.github.mzmine.util.RangeUtils.isBounded;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.alignment.AlignmentMainType;
import io.github.mzmine.datamodel.features.types.alignment.AlignmentScores;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.gui.framework.fx.features.ParentFeatureListPaneGroup;
import io.github.mzmine.modules.dataprocessing.align_join.RowAlignmentScoreCalculator;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.javafx.WeakAdapter;
import io.github.mzmine.util.math.ScoreAccumulator;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureListUtils {

  /**
   * Bind rows of feature list to a list in a {@link WeakAdapter}
   *
   * @param flist source of changes, used as parent in weak
   * @param rows  target
   */
  public static void bindRows(@NotNull WeakAdapter weak, @NotNull FeatureList flist,
      @NotNull ObservableList<FeatureListRow> rows) {
    weak.addListChangeListener(flist, flist.getRows(), change -> {
      if (weak.isActive()) {
        rows.setAll(change.getList());
      }
    });
    rows.setAll(flist.getRows());
  }

  /**
   * Bind selected rows of {@link FeatureTableFX} to a list in a {@link WeakAdapter}. Use the
   * FeatureList and {@link #bindRows(WeakAdapter, FeatureList, ObservableList)} directly to bind
   * rows of a feature list. Also listen to changes to the active FeatureList in the table. See
   * {@link ParentFeatureListPaneGroup}
   *
   * @param table source of changes, used as parent in weak
   * @param rows  target
   */
  public static void bindSelectedRows(@NotNull WeakAdapter weak, @NotNull FeatureTableFX table,
      @NotNull ObservableList<FeatureListRow> rows) {
    weak.addListChangeListener(table, table.getSelectedTableRows(), change -> {
      if (weak.isActive()) {
        rows.setAll(table.getSelectedRows());
      }
    });
    rows.setAll(table.getSelectedRows());
  }

  /**
   * Copies the FeatureListAppliedMethods from <b>source</b> to <b>target</b>
   *
   * @param source The source feature list.
   * @param target the target feature list.
   */
  public static void copyPeakListAppliedMethods(FeatureList source, FeatureList target) {
    for (FeatureListAppliedMethod proc : source.getAppliedMethods()) {
      target.addDescriptionOfAppliedTask(proc);
    }
  }

  /**
   * All features within all ranges. Use a sorted list to speed up search. Use range.all() instead
   * of null for missign ranges
   *
   * @param mzRange             search range
   * @param rtRange             search range in retention time, provide Range.all() if no RT
   * @param mobilityRange       search range in ion mobility, provide Range.all() if no mobility
   * @param rows                the list of rows to search in
   * @param sortedByMzAscending list is already sorted by ascending mz. This will speed up the
   *                            search
   * @return an unsorted list of candidates within all three ranges if provided
   */
  public static @NotNull List<FeatureListRow> getCandidatesWithinRanges(
      @NotNull Range<Double> mzRange, @NotNull Range<Float> rtRange,
      @NotNull Range<Float> mobilityRange, @NotNull List<? extends FeatureListRow> rows,
      boolean sortedByMzAscending) {

    if (!sortedByMzAscending) {
      rows = rows.stream().sorted(MZ_ASCENDING).toList();
    }

    IndexRange indexRange = BinarySearch.indexRange(mzRange, rows, FeatureListRow::getAverageMZ);
    if (indexRange.isEmpty()) {
      return List.of();
    }

    List<FeatureListRow> candidates = new ArrayList<>();
    for (int i = indexRange.min(); i < indexRange.maxExclusive(); i++) {
      FeatureListRow row = rows.get(i);
      // test only mz to short circuit
      if (mzRange.contains(row.getAverageMZ())) {
        var rowMobility = row.getAverageMobility();
        var rowRT = row.getAverageRT();
        if ((rowMobility == null || mobilityRange.contains(rowMobility)) //
            && (rowRT == null || rtRange.contains(rowRT))) {
          candidates.add(row);
        }
      }
    }
    return candidates;
  }

  /**
   * All features within all ranges. Use a sorted list to speed up search. Use range.all() instead
   * of null for missign ranges
   *
   * @param rtRange search range in retention time, provide Range.all() if no RT
   * @param rows    the list of rows to search in
   * @return an unsorted list of candidates within all three ranges if provided
   */
  public static @NotNull List<FeatureListRow> getCandidatesWithinRtRange(
      @NotNull Range<Float> rtRange, @NotNull List<FeatureListRow> rows,
      boolean sortedByDefaultRt) {

    if (!sortedByDefaultRt) {
      rows = rows.stream().sorted(DEFAULT_RT).toList();
    }
    return BinarySearch.indexRange(rtRange, rows, FeatureListRow::getAverageRT).sublist(rows);
  }


  /**
   * Searches for the given mz value - or the closest available row in the list of rows. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param rowsSortedByMz the sorted list of rows to search in
   * @param mz             search for this mz value
   * @return this index of the given mz value or the closest available mz. -1 if the input list is
   * empty
   */
  public static int binarySearch(List<? extends FeatureListRow> rowsSortedByMz, double mz) {
    return binarySearch(rowsSortedByMz, mz, 0, rowsSortedByMz.size());
  }

  /**
   * Searches for the given mz value - or the closest available row in the list of rows. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param rowsSortedByMz the sorted list of rows to search in
   * @param mz             search for this mz value
   * @param fromIndex      inclusive lower end
   * @param toIndex        exclusive upper end
   * @return this index of the given mz value or the closest available mz. -1 if the input list is
   * empty
   */
  public static int binarySearch(List<? extends FeatureListRow> rowsSortedByMz, double mz,
      int fromIndex, int toIndex) {
    if (toIndex == 0) {
      return -1;
    }
    final int numberOfDataPoints = rowsSortedByMz.size();
    ArrayUtils.rangeCheck(numberOfDataPoints, fromIndex, toIndex);

    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
      int mid = (low + high) >>> 1; // bit shift by 1 for sum of positive integers = / 2
      double midMz = rowsSortedByMz.get(mid).getAverageMZ();

      if (midMz < mz) {
        low = mid + 1;  // Neither mz is NaN, thisVal is smaller
      } else if (midMz > mz) {
        high = mid - 1; // Neither mz is NaN, thisVal is larger
      } else {
        long midBits = Double.doubleToLongBits(midMz);
        long keyBits = Double.doubleToLongBits(mz);
        if (midBits == keyBits) {
          return mid;  // Key found
        } else if (midBits < keyBits) {
          low = mid + 1;  // (-0.0, 0.0) or (!NaN, NaN)
        } else {
          high = mid - 1;  // (0.0, -0.0) or (NaN, !NaN)
        }
      }
    }
    if (low >= numberOfDataPoints) {
      return numberOfDataPoints - 1;
    }
    // might be higher or lower
    final double adjacentMZ = rowsSortedByMz.get(low).getAverageMZ();
    // check for closest distance to mz
    if (adjacentMZ <= mz && low + 1 < numberOfDataPoints) {
      final double higherMZ = rowsSortedByMz.get(low + 1).getAverageMZ();
      return (Math.abs(mz - adjacentMZ) <= Math.abs(higherMZ - mz)) ? low : low + 1;
    } else if (adjacentMZ > mz && low - 1 >= 0) {
      final double lowerMZ = rowsSortedByMz.get(low - 1).getAverageMZ();
      return (Math.abs(mz - adjacentMZ) <= Math.abs(lowerMZ - mz)) ? low : low - 1;
    } else {
      // there was only one data point
      return low;
    }
  }


  /**
   * Get the row with the best alignment score based on the ranges. Use null or Range.all() to
   * deactivate one factor from the score
   *
   * @param rows          search list
   * @param mzRange       score based on this search range
   * @param rtRange       score based on this search range
   * @param mobilityRange score based on this search range
   * @return Optional of the best row with the highest score
   */
  public static <T extends FeatureListRow> Optional<T> getBestRow(@NotNull final List<T> rows,
      @Nullable Range<Double> mzRange, @Nullable Range<Float> rtRange,
      @Nullable Range<Float> mobilityRange, @Nullable Range<Float> ccsRange, double mzWeight,
      double rtWeight, double mobilityWeight, double ccsWeight) {
    return getBestRow(rows, mzRange, rtRange, mobilityRange, ccsRange, mzWeight, rtWeight,
        mobilityWeight, ccsWeight, t -> true);
  }

  /**
   * Get the row with the best alignment score based on the ranges. Use null or Range.all() to
   * deactivate one factor from the score
   *
   * @param rows                search list
   * @param mzRange             score based on this search range
   * @param rtRange             score based on this search range
   * @param mobilityRange       score based on this search range
   * @param additionalRowFilter additional row filters to apply before scoring
   * @return Optional of the best row with the highest score
   */
  public static <T extends FeatureListRow> Optional<T> getBestRow(@NotNull final List<T> rows,
      @Nullable Range<Double> mzRange, @Nullable Range<Float> rtRange,
      @Nullable Range<Float> mobilityRange, @Nullable Range<Float> ccsRange, double mzWeight,
      double rtWeight, double mobilityWeight, double ccsWeight,
      @NotNull Predicate<T> additionalRowFilter) {
    return rows.stream().filter(additionalRowFilter).max(Comparator.comparingDouble(
        r -> getAlignmentScore(r, mzRange, rtRange, mobilityRange, ccsRange, mzWeight, rtWeight,
            mobilityWeight, ccsWeight)));
  }

  /**
   * Calculates alignment scores based on the original lists and the output aligned feature list.
   * Needs original lists to get the number of features that might have matched
   *
   * @param alignedFeatureList the aligned list with average values for mz,RT, mobility
   * @param calculator         the calculator holds tolerances and the orginal feature lists to
   *                           score the alignment
   * @param mergeScores        merge or override scores
   */
  public static void addAlignmentScores(@NotNull FeatureList alignedFeatureList,
      RowAlignmentScoreCalculator calculator, boolean mergeScores) {
    // add the new types to the feature list
    alignedFeatureList.addRowType(DataTypes.get(AlignmentMainType.class));

    SortedList<FeatureListRow> rows = alignedFeatureList.getRows().sorted(MZ_ASCENDING);

    // find the number of rows that match RT,MZ,Mobility in each original feature list
    var changed = rows.stream().parallel().mapToInt(alignedRow -> {
      AlignmentScores score = calculator.calcScore(alignedRow);
      if (mergeScores) {
        AlignmentScores oldScore = alignedRow.get(AlignmentMainType.class);
        score = score.merge(oldScore);
      }
      alignedRow.set(AlignmentMainType.class, score);
      return 1;
    }).sum();
  }

  /**
   * Compare row average values to ranges (during alignment or annotation to other mz, rt, and
   * mobility values based on tolerances -> ranges). General score is SUM((difference
   * row-center(range)) / rangeLength * factor) / sum(factors)
   *
   * @param feature        target feature
   * @param mzRange        allowed range
   * @param rtRange        allowed range
   * @param mobilityRange  allowed range
   * @param mzWeight       weight factor
   * @param rtWeight       weight factor
   * @param mobilityWeight weight factor
   * @return the alignment score between 0-1 with 1 being a perfect match
   */
  public static double getAlignmentScore(Feature feature, @Nullable Range<Double> mzRange,
      @Nullable Range<Float> rtRange, @Nullable Range<Float> mobilityRange,
      @Nullable Range<Float> ccsRange, double mzWeight, double rtWeight, double mobilityWeight,
      double ccsWeight) {
    return getAlignmentScore(feature.getMZ(), feature.getRT(), feature.getMobility(),
        feature.getCCS(), mzRange, rtRange, mobilityRange, ccsRange, mzWeight, rtWeight,
        mobilityWeight, ccsWeight);
  }

  /**
   * Compare row average values to ranges (during alignment or annotation to other mz, rt, and
   * mobility values based on tolerances -> ranges). General score is SUM((difference
   * row-center(range)) / rangeLength * factor) / sum(factors)
   *
   * @param row            target row
   * @param mzRange        allowed range
   * @param rtRange        allowed range
   * @param mobilityRange  allowed range
   * @param mzWeight       weight factor
   * @param rtWeight       weight factor
   * @param mobilityWeight weight factor
   * @return the alignment score between 0-1 with 1 being a perfect match
   */
  public static double getAlignmentScore(FeatureListRow row, @Nullable Range<Double> mzRange,
      @Nullable Range<Float> rtRange, @Nullable Range<Float> mobilityRange,
      @Nullable Range<Float> ccsRange, double mzWeight, double rtWeight, double mobilityWeight,
      double ccsWeight) {
    return getAlignmentScore(row.getAverageMZ(), row.getAverageRT(), row.getAverageMobility(),
        row.getAverageCCS(), mzRange, rtRange, mobilityRange, ccsRange, mzWeight, rtWeight,
        mobilityWeight, ccsWeight);
  }

  /**
   * Compare row average values to ranges (during alignment or annotation to other mz, rt, and
   * mobility values based on tolerances -> ranges). General score is SUM((difference
   * row-center(range)) / rangeLength * factor) / sum(factors)
   *
   * @param row      target row
   * @param rtRange  allowed range
   * @param rtWeight weight factor
   * @return the alignment score between 0-1 with 1 being a perfect match
   */
  public static double getAlignmentScore(FeatureListRow row, @Nullable Range<Float> rtRange,
      double similarity, double rtWeight, double similarityWeight) {
    return getAlignmentScore(row.getAverageRT(), rtRange, similarity, rtWeight, similarityWeight);
  }

  /**
   * Compare row average values to ranges (during alignment or annotation to other mz, rt, and
   * mobility values based on tolerances -> ranges). General score is SUM((difference
   * row-center(range)) / rangeLength * factor) / sum(factors)
   *
   * @param testMz         tested value
   * @param testRt         tested value
   * @param testMobility   tested value
   * @param mzRange        allowed range
   * @param rtRange        allowed range
   * @param mobilityRange  allowed range
   * @param mzWeight       weight factor
   * @param rtWeight       weight factor
   * @param mobilityWeight weight factor
   * @return the alignment score between 0-1 with 1 being a perfect match
   */
  public static double getAlignmentScore(Double testMz, Float testRt, Float testMobility,
      Float testCCS, @Nullable Range<Double> mzRange, @Nullable Range<Float> rtRange,
      @Nullable Range<Float> mobilityRange, @Nullable Range<Float> ccsRange, double mzWeight,
      double rtWeight, double mobilityWeight, double ccsWeight) {

    ScoreAccumulator score = new ScoreAccumulator();

    // values are "matched" if the given value exists in this class and falls within the tolerance.
    // don't score range.all, will distort the scoring.
    checkAndAddCenterScore(score, testMz, mzRange, mzWeight);
    checkAndAddCenterScore(score, testRt, rtRange, rtWeight);
    checkAndAddCenterScore(score, testMobility, mobilityRange, mobilityWeight);
    checkAndAddCenterScore(score, testCCS, ccsRange, ccsWeight);

    return score.getScore();
  }


  /**
   * Test how close testedValue is to the center of the range (perfect score 1), scaled 0-1. Only
   * adds the weighted score if value and range are not null, and if weigth is >0
   *
   * @param score       accumulates the score and weights
   * @param testedValue value to be tested for center of range
   * @param range       center and length of range are used. Unbounded or null ranges will discard
   *                    this score
   * @param weight      weight of the score
   */
  public static void checkAndAddCenterScore(@NotNull final ScoreAccumulator score,
      @Nullable final Double testedValue, final @Nullable Range<Double> range,
      final double weight) {
    if (weight > 0 && isBounded(range) && testedValue != null) {
      // no negative numbers
      score.add(calcCenterScore(testedValue, range), weight);
    }
  }

  /**
   * Test how close testedValue is to the center of the range (perfect score 1), scaled 0-1. Only
   * adds the weighted score if value and range are not null, and if weigth is >0
   *
   * @param score       accumulates the score and weights
   * @param testedValue value to be tested for center of range
   * @param range       center and length of range are used. Unbounded or null ranges will discard
   *                    this score
   * @param weight      weight of the score
   */
  public static void checkAndAddCenterScore(@NotNull final ScoreAccumulator score,
      @Nullable final Float testedValue, final @Nullable Range<Float> range, final double weight) {
    if (weight > 0 && isBounded(range) && testedValue != null) {
      // no negative numbers
      score.add(calcCenterScore(testedValue, range), weight);
    }
  }

  /**
   * Compare row average values to ranges (during alignment or annotation to other mz, rt, and
   * mobility values based on tolerances -> ranges). General score is SUM((difference
   * row-center(range)) / rangeLength * factor) / sum(factors)
   *
   * @param testRt           tested value
   * @param testSimilarity   tested value
   * @param rtRange          allowed range
   * @param rtWeight         weight factor
   * @param similarityWeight weight factor
   * @return the alignment score between 0-1 with 1 being a perfect match
   */
  public static double getAlignmentScore(Float testRt, @Nullable Range<Float> rtRange,
      double testSimilarity, double rtWeight, double similarityWeight) {

    ScoreAccumulator score = new ScoreAccumulator();
    // don't score range.all, will distort the scoring.
    checkAndAddCenterScore(score, testRt, rtRange, rtWeight);
    if (similarityWeight > 0) {
      score.add(testSimilarity, similarityWeight);
    }

    return score.getScore();
  }


  /**
   * Sort feature list by default based on raw data type Sort feature list by mz if imaging data
   * else sort feature list by retention time
   *
   * @param featureList target list
   */
  public static void sortByDefault(FeatureList featureList, boolean renumberIDs) {
    RawDataFile rawDataFile = featureList.getRawDataFiles().get(0);
    if (rawDataFile != null) {
      if (!(rawDataFile instanceof ImagingRawDataFile)) {
        FeatureListUtils.sortByDefaultRT(featureList, renumberIDs);
      } else {
        FeatureListUtils.sortByDefaultMZ(featureList, renumberIDs);
      }
    }
  }

  /**
   * Sort feature list by retention time (default)
   *
   * @param featureList target list
   */
  public static void sortByDefaultRT(FeatureList featureList) {
    // sort rows by rt
    featureList.getRows().sort(FeatureListRowSorter.DEFAULT_RT);
  }

  /**
   * Sort feature list by mz and reset IDs starting with 1
   *
   * @param featureList target list
   * @param renumberIDs renumber rows
   */
  public static void sortByDefaultMZ(FeatureList featureList, boolean renumberIDs) {
    sortByDefaultMZ(featureList);
    if (!renumberIDs) {
      return;
    }
    // reset IDs
    int newRowID = 1;
    for (var row : featureList.getRows()) {
      row.set(IDType.class, newRowID);
      newRowID++;
    }
  }

  /**
   * Sort feature list by mz (default)
   *
   * @param featureList target list
   */
  public static void sortByDefaultMZ(FeatureList featureList) {
    // sort rows by mz
    featureList.getRows().sort(MZ_ASCENDING);
  }

  /**
   * Sort feature list by retention time and reset IDs starting with 1
   *
   * @param featureList target list
   * @param renumberIDs renumber rows
   */
  public static void sortByDefaultRT(FeatureList featureList, boolean renumberIDs) {
    sortByDefaultRT(featureList);
    if (!renumberIDs) {
      return;
    }
    // reset IDs
    int newRowID = 1;
    for (var row : featureList.getRows()) {
      row.set(IDType.class, newRowID);
      newRowID++;
    }
  }

  /**
   * Transfers all row types present in the source feature list to the target feature list.
   */
  public static void transferRowTypes(FeatureList targetFlist, Collection<FeatureList> sourceFlists,
      final boolean transferFeatureTypes) {

    for (FeatureList sourceFlist : sourceFlists) {
      // uses a set so okay to use addAll
      targetFlist.addRowType(sourceFlist.getRowTypes());
    }
    if (transferFeatureTypes) {
      for (FeatureList sourceFlist : sourceFlists) {
        // uses a set so okay to use addAll
        targetFlist.addFeatureType(sourceFlist.getFeatureTypes());
      }
    }
  }

  /**
   * Copies the selected scans from a collection of feature lists to the target feature list.
   */
  public static void transferSelectedScans(FeatureList target, Collection<FeatureList> flists) {
    for (FeatureList flist : flists) {
      for (RawDataFile rawDataFile : flist.getRawDataFiles()) {
        if (target.getSeletedScans(rawDataFile) != null) {
          throw new IllegalStateException(
              "Error, selected scans for file " + rawDataFile + " already set.");
        }
        target.setSelectedScans(rawDataFile, flist.getSeletedScans(rawDataFile));
      }
    }
  }

  /**
   * Loops over all feature lists and collects all raw data files. If a file is present in multiple
   * feature lists, an exception is thrown.
   */
  public static List<RawDataFile> getAllDataFiles(Collection<FeatureList> flists) {
    List<RawDataFile> allDataFiles = new ArrayList<>();
    for (FeatureList featureList : flists) {
      for (RawDataFile dataFile : featureList.getRawDataFiles()) {
        // Each data file can only have one column in aligned feature
        // list
        if (allDataFiles.contains(dataFile)) {
          throw new IllegalArgumentException(
              "File " + dataFile + " is present in multiple feature lists");
        }
        allDataFiles.add(dataFile);
      }
    }
    return allDataFiles;
  }

  /**
   * Maps the row ID to the feature list for quick lookup
   *
   * @return ID to row map
   */
  @NotNull
  public static Int2ObjectMap<FeatureListRow> getRowIdMap(final ModularFeatureList featureList) {
    Int2ObjectMap<FeatureListRow> map = new Int2ObjectArrayMap<>(featureList.getRows().size());
    for (final FeatureListRow row : featureList.getRows()) {
      map.put(row.getID(), row);
    }
    return map;
  }

  /**
   * Does not copy rows
   */
  public static ModularFeatureList createCopy(final FeatureList featureList, final String suffix,
      final MemoryMapStorage storage) {
    return createCopy(featureList, suffix, storage, false);
  }

  public static ModularFeatureList createCopy(final FeatureList featureList, final String suffix,
      final MemoryMapStorage storage, boolean copyRows) {
    return createCopy(featureList, null, suffix, storage, copyRows, featureList.getRawDataFiles(),
        false);
  }

  public static ModularFeatureList createCopy(final FeatureList featureList,
      @Nullable String fullTitle, final @Nullable String suffix, final MemoryMapStorage storage,
      boolean copyRows, List<RawDataFile> dataFiles, boolean renumberIDs) {
    if (StringUtils.isBlank(fullTitle) && StringUtils.isBlank(suffix)) {
      throw new IllegalArgumentException("Either suffix or fullTitle need a value");
    }
    if (fullTitle == null) {
      fullTitle = featureList.getName() + " " + suffix;
    }

    ModularFeatureList newFlist = new ModularFeatureList(fullTitle, storage, dataFiles);

    FeatureListUtils.copyPeakListAppliedMethods(featureList, newFlist);
    FeatureListUtils.transferRowTypes(newFlist, List.of(featureList), true);
    FeatureListUtils.transferSelectedScans(newFlist, List.of(featureList));

    if (copyRows) {
      copyRows(featureList, newFlist, renumberIDs);
    }

    return newFlist;
  }

  public static void copyRows(final FeatureList featureList,
      final ModularFeatureList newFeatureList, final boolean renumberIDs) {
    int id = 1;
    for (final FeatureListRow row : featureList.getRows()) {
      FeatureListRow copy = new ModularFeatureListRow(newFeatureList,
          renumberIDs ? id : row.getID(), (ModularFeatureListRow) row, true);
      newFeatureList.addRow(copy);
      id++;
    }
  }

  /**
   * Get the polarity of the feature list. Only checks a single row
   *
   * @return polarity or {@link PolarityType#UNKNOWN}
   */
  public static @NotNull PolarityType getPolarity(final FeatureList featureList) {
    return getPolarity(featureList, PolarityType.UNKNOWN);
  }

  /**
   * Get the polarity of the feature list. Only checks a single row
   *
   * @return polarity or default value if missing
   */
  public static @NotNull PolarityType getPolarity(final FeatureList featureList,
      @NotNull final PolarityType defaultValue) {
    return featureList.stream().findFirst().map(FeatureListRow::getBestFeature)
        .map(Feature::getRepresentativeScan).map(Scan::getPolarity).orElse(defaultValue);
  }

  public static String rowsToIdString(List<? extends FeatureListRow> rows) {
    return rows.stream().map(FeatureListRow::getID).map(Object::toString)
        .collect(Collectors.joining(","));
  }

  public static List<FeatureListRow> idStringToRows(ModularFeatureList flist, String str) {
    final Set<Integer> ids = Arrays.stream(str.split(",")).map(s -> {
      if (s == null) {
        return null;
      }
      final String stripped = s.strip();
      try {
        return Integer.valueOf(stripped);
      } catch (NumberFormatException e) {
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toSet());
    final List<FeatureListRow> list = flist.stream().filter(row -> ids.contains(row.getID()))
        .sorted(Comparator.comparingInt(FeatureListRow::getID)).toList();

    if (list.size() != ids.size()) {
      throw new RuntimeException(
          "Number of found rows does not match number of ids. Is this the correct feature list? Found: %s, Searched: %s".formatted(
              list.stream().map(FeatureListRow::getID).map(Object::toString)
                  .collect(Collectors.joining(",")),
              ids.stream().map(Object::toString).collect(Collectors.joining(","))));
    }
    return list;
  }

  /**
   * Ims features usually consume more ram than non ims features. IF memory usage can be estimated
   * for regular features, estimate a correction factor for the ram usage if ims files are present.
   */
  public static double getImsRamFactor(FeatureList flist) {
    final int numRaws = flist.getNumberOfRawDataFiles();
    final long numImsFiles = flist.getRawDataFiles().stream()
        .filter(IMSRawDataFile.class::isInstance).count();
    final boolean isIms = flist.getFeatureTypes().contains(DataTypes.get(MobilityType.class));
    // ims features generally consume 10 times the ram of non ims features. not all files may be ims though
    final double imsRamFactor = isIms ? (double) (numImsFiles * 10) / numRaws : 1;
    return imsRamFactor;
  }

}
