/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationMaps;
import io.github.mzmine.datamodel.otherdetectors.OtherCorrelationLink;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureListRow;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureToOtherRowPair;
import io.github.mzmine.datamodel.otherdetectors.TraceKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manual edits of the aligned other detector features ({@link OtherFeatureList}) of an MS feature
 * list, e.g., from the correlation dashboard. The list may be created here if no alignment was run,
 * so a fully manual workflow fills it step by step. Manual edits are saved with the project but are
 * not reproducible in batch mode.
 * <p>
 * The aligned list owns copies of the features, so features from
 * {@link io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData} (e.g., manually integrated
 * peaks) are always copied before they are added. As copies are different instances, peaks are
 * compared with {@link #isSamePeak(OtherFeature, OtherFeature)}.
 * <p>
 * Not thread-safe, see {@link OtherFeatureList}. Call from a single thread, e.g., the FX thread.
 */
public final class OtherFeatureManualUtils {

  // assumption: copies and project round trips keep the RT range, a small tolerance covers float
  // rounding
  private static final float SAME_PEAK_RT_TOLERANCE = 1e-4f;

  private OtherFeatureManualUtils() {
  }

  /**
   * @param flist the MS feature list
   * @return the aligned other feature list of the MS feature list. An empty list is created and
   * attached if there is none.
   */
  @NotNull
  public static OtherFeatureList getOrCreateAlignedList(@NotNull final ModularFeatureList flist) {
    final OtherFeatureList existing = flist.getAlignedOtherFeatures();
    if (existing != null) {
      return existing;
    }
    final OtherFeatureList ofl = OtherFeatureListUtils.createEmptyAlignedList(flist,
        flist.getMemoryMapStorage());
    flist.setAlignedOtherFeatures(ofl);
    return ofl;
  }

  /**
   * Checks if two features describe the same peak: same raw data file, same {@link TraceKey} and
   * the same RT range. Copies of a feature (e.g., in the aligned list and in the processed features
   * of the raw file) are different instances but the same peak.
   *
   * @param a a feature
   * @param b another feature
   * @return true if both features are the same peak
   */
  public static boolean isSamePeak(@NotNull final OtherFeature a, @NotNull final OtherFeature b) {
    if (a == b) {
      return true;
    }
    if (!Objects.equals(a.getRawDataFile(), b.getRawDataFile()) || !TraceKey.of(a)
        .equals(TraceKey.of(b))) {
      return false;
    }
    final Range<Float> rangeA = a.getRtRange();
    final Range<Float> rangeB = b.getRtRange();
    if (rangeA == null || rangeB == null) {
      return rangeA == rangeB && Objects.equals(a.getRT(), b.getRT());
    }
    return Math.abs(rangeA.lowerEndpoint() - rangeB.lowerEndpoint()) <= SAME_PEAK_RT_TOLERANCE
        && Math.abs(rangeA.upperEndpoint() - rangeB.upperEndpoint()) <= SAME_PEAK_RT_TOLERANCE;
  }

  /**
   * @param ofl     the aligned other feature list
   * @param feature the peak to look for
   * @return all rows that hold this peak in the feature's raw data file, in row order. A peak may
   * be in more than one row after manual edits.
   */
  @NotNull
  public static List<OtherFeatureListRow> findRowsWithPeak(@NotNull final OtherFeatureList ofl,
      @NotNull final OtherFeature feature) {
    final RawDataFile file = feature.getRawDataFile();
    final TraceKey key = TraceKey.of(feature);
    final List<OtherFeatureListRow> result = new ArrayList<>();
    for (final OtherFeatureListRow row : ofl.getRows()) {
      if (!row.getTraceKey().equals(key)) {
        continue;
      }
      final OtherFeature cell = row.getFeature(file);
      if (cell != null && isSamePeak(cell, feature)) {
        result.add(row);
      }
    }
    return result;
  }

  /**
   * Adds a copy of the feature as a new single-file row.
   *
   * @param ofl     the aligned other feature list
   * @param feature the feature to add, a copy is stored
   * @return the new row and the stored copy
   */
  @NotNull
  public static OtherFeatureToOtherRowPair addAsNewRow(@NotNull final OtherFeatureList ofl,
      @NotNull final OtherFeature feature) {
    final OtherFeatureListRow row = ofl.createRow(TraceKey.of(feature));
    final OtherFeature copy = feature.copy();
    row.setFeature(copy);
    ofl.addRow(row);
    return new OtherFeatureToOtherRowPair(row, copy);
  }

  /**
   * Sets a copy of the feature into the row's cell of its raw data file and replaces the previous
   * feature. Nothing changes if the cell already holds the same peak. A replaced feature only
   * leaves this row, other rows that hold the same peak keep it. Correlations of the row stay, so
   * they now refer to the new feature.
   *
   * @param row     the aligned row, must have the feature's trace key
   * @param feature the feature to set, a copy is stored
   * @return the row and the feature in its cell
   */
  @NotNull
  public static OtherFeatureToOtherRowPair setFeature(@NotNull final OtherFeatureListRow row,
      @NotNull final OtherFeature feature) {
    return setFeatureAndGetReplaced(row, feature).aligned();
  }

  /**
   * Removes the feature of a raw data file from a row and the correlations of all MS rows to this
   * row in this file. A row without features is removed from its list, together with all its
   * correlations.
   *
   * @param row  the aligned row
   * @param file the raw data file
   * @return the removed feature or null if the row had no feature for this file
   */
  @Nullable
  public static OtherFeature removeFeature(@NotNull final OtherFeatureListRow row,
      @NotNull final RawDataFile file) {
    final OtherFeatureList ofl = row.getOtherFeatureList();
    final OtherFeature removed = row.removeFeature(file);
    if (removed == null) {
      return null;
    }
    if (row.getNumberOfFeatures() == 0) {
      ofl.removeRow(row);
      return removed;
    }
    // decision: a correlation in this file cannot point to a missing peak
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    for (final Integer msRowId : List.copyOf(maps.getMsRowsForOtherRow(row.getID()))) {
      maps.removeCorrelation(msRowId, row.getID(), file);
    }
    return removed;
  }

  /**
   * Manually correlates an MS row to an other detector feature in the feature's raw data file. The
   * aligned list is created if needed. The row that takes the feature is, in this order:
   * <ol>
   *   <li>the first row of the same trace that the MS row already correlates to (in any file). Its
   *   cell for this file is replaced if it holds a different peak, e.g., the user picks the peak
   *   at 13.5 min instead of the correlated one at 13.3 min.</li>
   *   <li>{@code selectedRow} if it holds this peak</li>
   *   <li>the first row that holds this peak</li>
   *   <li>a new single-file row</li>
   * </ol>
   * The correlation of the MS row to this row in this file is set to manual.
   *
   * @param flist       the MS feature list
   * @param msRow       the MS row
   * @param feature     the other detector feature, e.g., an aligned or a processed feature. A copy
   *                    is stored if it is not in the aligned list yet.
   * @param selectedRow the aligned row the feature was selected from, null if it is not aligned
   * @return the row and feature that are now correlated
   */
  @NotNull
  public static ManualOtherCorrelationResult correlateManually(
      @NotNull final ModularFeatureList flist, @NotNull final FeatureListRow msRow,
      @NotNull final OtherFeature feature, @Nullable final OtherFeatureListRow selectedRow) {
    final OtherFeatureList ofl = getOrCreateAlignedList(flist);
    final TraceKey key = TraceKey.of(feature);
    final RawDataFile file = feature.getRawDataFile();

    OtherFeatureListRow target = findCorrelatedRowOfTrace(ofl, msRow.getID(), key);
    if (target == null && selectedRow != null && selectedRow.getOtherFeatureList() == ofl) {
      final OtherFeature cell = selectedRow.getFeature(file);
      if (cell != null && isSamePeak(cell, feature)) {
        target = selectedRow;
      }
    }
    if (target == null) {
      target = findRowsWithPeak(ofl, feature).stream().findFirst().orElse(null);
    }

    final ManualOtherCorrelationResult result;
    if (target == null) {
      result = new ManualOtherCorrelationResult(addAsNewRow(ofl, feature), null, true);
    } else {
      result = setFeatureAndGetReplaced(target, feature);
    }
    ofl.getMsOtherCorrelationMaps()
        .addManualCorrelation(msRow.getID(), result.aligned().row().getID(), file);
    return result;
  }

  /**
   * @param ofl     the aligned other feature list
   * @param msRowId the MS row ID
   * @param feature the other detector feature
   * @return true if the MS row is correlated to this peak in the feature's raw data file
   */
  public static boolean isCorrelated(@NotNull final OtherFeatureList ofl, final int msRowId,
      @NotNull final OtherFeature feature) {
    final RawDataFile file = feature.getRawDataFile();
    for (final OtherCorrelationLink link : ofl.getMsOtherCorrelationMaps()
        .getCorrelations(msRowId)) {
      if (!link.perFile().containsKey(file)) {
        continue;
      }
      final OtherFeatureListRow row = ofl.findRowByID(link.otherRowId());
      final OtherFeature cell = row != null ? row.getFeature(file) : null;
      if (cell != null && isSamePeak(cell, feature)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param ofl     the aligned other feature list
   * @param msRowId the MS row ID
   * @param key     the trace key
   * @return the first row of this trace in the preference order of the MS row's correlations, null
   * if the MS row does not correlate to this trace
   */
  @Nullable
  private static OtherFeatureListRow findCorrelatedRowOfTrace(@NotNull final OtherFeatureList ofl,
      final int msRowId, @NotNull final TraceKey key) {
    for (final OtherCorrelationLink link : ofl.getMsOtherCorrelationMaps()
        .getCorrelations(msRowId)) {
      final OtherFeatureListRow row = ofl.findRowByID(link.otherRowId());
      if (row != null && row.getTraceKey().equals(key)) {
        return row;
      }
    }
    return null;
  }

  @NotNull
  private static ManualOtherCorrelationResult setFeatureAndGetReplaced(
      @NotNull final OtherFeatureListRow row, @NotNull final OtherFeature feature) {
    final OtherFeature current = row.getFeature(feature.getRawDataFile());
    if (current != null && isSamePeak(current, feature)) {
      return new ManualOtherCorrelationResult(new OtherFeatureToOtherRowPair(row, current), null,
          false);
    }
    final OtherFeature copy = feature.copy();
    final OtherFeature replaced = row.setFeature(copy);
    return new ManualOtherCorrelationResult(new OtherFeatureToOtherRowPair(row, copy), replaced,
        false);
  }
}
