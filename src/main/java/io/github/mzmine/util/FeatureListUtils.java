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

package io.github.mzmine.util;

import static io.github.mzmine.util.FeatureListRowSorter.MZ_ASCENDING;
import static io.github.mzmine.util.RangeUtils.rangeLength;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.alignment.AlignmentMainType;
import io.github.mzmine.datamodel.features.types.alignment.AlignmentScores;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.modules.dataprocessing.align_join.RowAlignmentScoreCalculator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javafx.collections.transformation.SortedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureListUtils {

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
    // search starting point as the insertion index
    int insertIndex = binarySearch(rows, RangeUtils.rangeCenter(mzRange));
    if (insertIndex < 0) {
      insertIndex = -insertIndex - 1;
    }

    List<FeatureListRow> candidates = new ArrayList<>();
    // right
    for (int i = insertIndex; i < rows.size(); i++) {
      FeatureListRow row = rows.get(i);
      // test only mz to short circuit
      if (mzRange.contains(row.getAverageMZ())) {
        var rowMobility = row.getAverageMobility();
        var rowRT = row.getAverageRT();
        if ((rowMobility == null || mobilityRange.contains(rowMobility)) && (rowRT == null
            || rtRange.contains(rowRT))) {
          candidates.add(row);
        }
      } else {
        break;
      }
    }
    // left
    for (int i = insertIndex - 1; i >= 0; i--) {
      FeatureListRow row = rows.get(i);
      // test only mz to short circuit
      if (mzRange.contains(row.getAverageMZ())) {
        var rowMobility = row.getAverageMobility();
        var rowRT = row.getAverageRT();
        if ((rowMobility == null || mobilityRange.contains(rowMobility)) && (rowRT == null
            || rtRange.contains(rowRT))) {
          candidates.add(row);
        }
      } else {
        break;
      }
    }
    return candidates;
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
    rows.stream().parallel().forEach(alignedRow -> {
      AlignmentScores score = calculator.calcScore(alignedRow);
      if (mergeScores) {
        AlignmentScores oldScore = alignedRow.get(AlignmentMainType.class);
        score = score.merge(oldScore);
      }
      alignedRow.set(AlignmentMainType.class, score);
    });
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

    // don't score range.all, will distort the scoring.
    mzRange = mzRange == null || mzRange.equals(Range.all()) ? null : mzRange;
    rtRange = rtRange == null || rtRange.equals(Range.all()) ? null : rtRange;
    mobilityRange =
        mobilityRange == null || mobilityRange.equals(Range.all()) ? null : mobilityRange;
    ccsRange = ccsRange == null || ccsRange.equals(Range.all()) ? null : ccsRange;

    int scorers = 0;

    double score = 0f;
    // values are "matched" if the given value exists in this class and falls within the tolerance.
    if (mzWeight > 0 && mzRange != null && testMz != null) {
      final double exactMass = RangeUtils.rangeCenter(mzRange);
      double diff = Math.abs(testMz - exactMass);
      double maxAllowedDiff = rangeLength(mzRange) / 2;
      // no negative numbers
      score += Math.max(0, (1 - diff / maxAllowedDiff) * mzWeight);
      scorers += (int) Math.round(mzWeight);
    }

    if (rtWeight > 0 && rtRange != null && testRt != null) {
      final Float rt = RangeUtils.rangeCenter(rtRange);
      float diff = Math.abs(testRt - rt);
      score += Math.max(0, 1 - (diff / (rangeLength(rtRange) / 2)) * rtWeight);
      scorers += (int) Math.round(rtWeight);
    }

    if (mobilityWeight > 0 && mobilityRange != null && testMobility != null) {
      final Float mobility = RangeUtils.rangeCenter(mobilityRange);
      float diff = Math.abs(testMobility - mobility);
      score += Math.max(0, 1 - (diff / (rangeLength(mobilityRange) / 2)) * mobilityWeight);
      scorers += (int) Math.round(mobilityWeight);
    }

    if (ccsWeight > 0 && ccsRange != null && testCCS != null) {
      final Float ccs = RangeUtils.rangeCenter(ccsRange);
      float diff = Math.abs(testCCS - ccs);
      score += Math.max(0, 1 - (diff / (rangeLength(ccsRange) / 2)) * ccsWeight);
      scorers += (int) Math.round(ccsWeight);
    }

    if (scorers == 0) {
      return 0f;
    }

    return score / scorers;
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
  public static void transferRowTypes(FeatureList targetFlist,
      Collection<FeatureList> sourceFlists) {
    for (FeatureList sourceFlist : sourceFlists) {
      for (Class<? extends DataType> value : sourceFlist.getRowTypes().keySet()) {
        if (!targetFlist.hasRowType(value)) {
          targetFlist.addRowType(sourceFlist.getRowTypes().get(value));
        }
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
}
