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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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
   * @param rows       The rows to search.
   * @param rtRange    The rt range. if row's retention time is null or -1 this range will not
   *                   filter (usually only for imaging datasets)
   * @param mzRange    The m/z range.
   * @param sortedByMz If the list is sorted by m/z (ascending)
   * @param <T>
   * @return List of matching rows.
   */
  @NotNull
  public static <T extends FeatureListRow> List<T> getRows(@NotNull final List<T> rows,
      @NotNull final Range<Float> rtRange, @NotNull final Range<Double> mzRange,
      boolean sortedByMz) {
    final List<T> validRows = new ArrayList<>();

    if (sortedByMz) {
      final double lower = mzRange.lowerEndpoint();
      final double upper = mzRange.upperEndpoint();

      for (T row : rows) {
        final double mz = row.getAverageMZ();
        if (mz < lower) {
          continue;
        } else if (mz > upper) {
          break;
        }
        // if retention time is not set or -1, then apply no retention time filter
        final Float averageRT = row.getAverageRT();
        if (averageRT == null || averageRT < 0 || rtRange.contains(averageRT)) {
          validRows.add(row);
        }
      }
    } else {
      for (T row : rows) {
        if (mzRange.contains(row.getAverageMZ()) && rtRange.contains(row.getAverageRT())) {
          validRows.add(row);
        }
      }
    }

    return validRows;
  }

  /**
   * @param rows          The rows to search.
   * @param rtRange       The rt range.
   * @param mzRange       The m/z range.
   * @param mobilityRange The mobilityRange.
   * @param sortedByMz    If the list is sorted by m/z (ascending)
   * @param <T>
   * @return List of matching rows.
   */
  @NotNull
  public static <T extends FeatureListRow> List<T> getRows(@NotNull final List<T> rows,
      @NotNull final Range<Float> rtRange, @NotNull final Range<Double> mzRange,
      @NotNull final Range<Float> mobilityRange, boolean sortedByMz) {
    final List<T> validRows = new ArrayList<>();

    if (sortedByMz) {
      final double lower = mzRange.lowerEndpoint();
      final double upper = mzRange.upperEndpoint();

      for (T row : rows) {
        final double mz = row.getAverageMZ();
        if (mz < lower) {
          continue;
        } else if (mz > upper) {
          break;
        }
        if (rtRange.contains(row.getAverageRT()) && (row.getAverageMobility() == null
            || mobilityRange.contains(row.getAverageMobility()))) {
          validRows.add(row);
        }
      }
    } else {
      for (T row : rows) {
        if (mzRange.contains(row.getAverageMZ()) && rtRange.contains(row.getAverageRT())) {
          validRows.add(row);
        }
      }
    }

    return validRows;
  }

  public static <T extends FeatureListRow> T getBestRow(@NotNull final List<T> rows,
      @Nullable Range<Double> mzRange, @Nullable Range<Float> rtRange,
      @Nullable Range<Float> mobilityRange) {
    return rows.stream()
        .max(Comparator.comparingDouble(r -> getScore(r, mzRange, rtRange, mobilityRange)))
        .orElse(null);
  }

  public static double getScore(FeatureListRow row, @Nullable Range<Double> mzRange,
      @Nullable Range<Float> rtRange, @Nullable Range<Float> mobilityRange) {

    // don't score range.all, will distort the scoring.
    mzRange = mzRange == null || mzRange.equals(Range.all()) ? null : mzRange;
    rtRange = rtRange == null || rtRange.equals(Range.all()) ? null : rtRange;
    mobilityRange =
        mobilityRange == null || mobilityRange.equals(Range.all()) ? null : mobilityRange;

    int scorers = 0;

    double score = 0f;
    final Double exactMass = mzRange != null ? RangeUtils.rangeCenter(mzRange) : null;
    // values are "matched" if the given value exists in this class and falls within the tolerance.
    if (mzRange != null && exactMass != null && !(row.getAverageMZ() == null || !mzRange.contains(
        row.getAverageMZ()))) {
      score +=
          1 - ((Math.abs(row.getAverageMZ() - exactMass)) / (RangeUtils.rangeLength(mzRange) / 2));
      scorers++;
    }

    final Float rt = rtRange != null ? RangeUtils.rangeCenter(rtRange) : null;
    if (rtRange != null && rt != null && !(row.getAverageRT() == null || !rtRange.contains(
        row.getAverageRT()))) {
      score += 1 - ((Math.abs(row.getAverageRT() - rt)) / (RangeUtils.rangeLength(rtRange) / 2));
      scorers++;
    }

    final Float mobility = mobilityRange != null ? RangeUtils.rangeCenter(mobilityRange) : null;
    if (mobilityRange != null && mobility != null && !(row.getAverageMobility() == null
        || !mobilityRange.contains(row.getAverageMobility()))) {
      score += 1 - ((Math.abs(row.getAverageMobility() - mobility)) / (
          RangeUtils.rangeLength(mobilityRange) / 2));
      scorers++;
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
