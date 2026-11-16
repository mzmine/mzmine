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
import io.github.mzmine.datamodel.otherdetectors.PerFileCorrelation;
import io.github.mzmine.datamodel.otherdetectors.TraceKey;
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
 * Rule: a peak is held by at most one aligned row per raw data file. Correlating a peak that is
 * already aligned moves it to the row that should hold it, together with its correlations.
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
   * Manually correlates an MS row to an other detector feature in the feature's raw data file. The
   * aligned list is created if needed.
   * <p>
   * The target row is the first row of the feature's trace that the MS row correlates to (in
   * preference order, in any file). If there is none, the row that already holds the peak is the
   * target, else a new single-file row is created.
   * <ul>
   *   <li>The target already holds this peak: the correlation in this file is set to manual if it
   *   is missing, otherwise nothing changes.</li>
   *   <li>Otherwise the peak replaces the target's cell for this file, e.g., the user picks the peak
   *   at 13.5 min instead of the correlated one at 13.3 min. The replaced peak leaves the aligned
   *   list. The correlations of all MS rows to the replaced peak keep their link but become manual
   *   without a score, as the score was calculated for the replaced peak.</li>
   *   <li>If another row held the peak, the peak is moved, e.g., the peaks at 13.3 and 13.5 min
   *   were misaligned in this file. All correlations to the moved peak move with it and keep their
   *   origin and score. The previous row is removed if it has no peaks left, or if it was
   *   correlated to MS rows before and is not anymore (only aligned peaks remain).</li>
   * </ul>
   * Finally, the MS row gets a manual correlation in this file if it has none.
   *
   * @param flist   the MS feature list
   * @param msRow   the MS row
   * @param feature the other detector feature, usually a processed feature of the raw file. A copy
   *                is stored if it is not in the aligned list yet.
   * @return the row and feature that are now correlated and what changed
   */
  @NotNull
  public static ManualOtherCorrelationResult correlateManually(
      @NotNull final ModularFeatureList flist, @NotNull final FeatureListRow msRow,
      @NotNull final OtherFeature feature) {
    final OtherFeatureList ofl = getOrCreateAlignedList(flist);
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    final RawDataFile file = feature.getRawDataFile();
    final int msRowId = msRow.getID();

    final OtherFeatureToOtherRowPair aligned = findAlignedPeak(ofl, feature);
    final OtherFeatureListRow correlatedRow = findCorrelatedRowOfTrace(ofl, msRowId,
        TraceKey.of(feature));
    // decision: the MS row's preferred row of this trace takes the peak, else the row holding it
    final OtherFeatureListRow target =
        correlatedRow != null ? correlatedRow : aligned != null ? aligned.row() : null;

    if (target == null) {
      final OtherFeatureListRow row = ofl.createRow(TraceKey.of(feature));
      final OtherFeature copy = feature.copy();
      row.setFeature(copy);
      ofl.addRow(row);
      maps.addManualCorrelation(msRowId, row.getID(), file);
      return new ManualOtherCorrelationResult(new OtherFeatureToOtherRowPair(row, copy), null, true,
          null, true);
    }

    final OtherFeature cell = target.getFeature(file);
    if (cell != null && isSamePeak(cell, feature)) {
      final boolean correlated = maps.getPerFileCorrelation(msRowId, target.getID(), file) != null;
      if (!correlated) {
        maps.addManualCorrelation(msRowId, target.getID(), file);
      }
      return new ManualOtherCorrelationResult(new OtherFeatureToOtherRowPair(target, cell), null,
          false, null, !correlated);
    }

    final OtherFeatureListRow source = aligned != null ? aligned.row() : null;
    final boolean sourceWasCorrelated =
        source != null && !maps.getMsRowsForOtherRow(source.getID()).isEmpty();
    final OtherFeature placed;
    if (source != null) {
      placed = aligned.feature();
      source.removeFeature(file);
    } else {
      placed = feature.copy();
    }

    final OtherFeature replaced = target.setFeature(placed);
    if (replaced != null) {
      markCorrelationsManual(target, file);
    }
    if (source != null) {
      // overrides the manual correlations of MS rows that were correlated to both peaks
      moveCorrelations(source, target, file);
      final boolean onlyAlignedLeft =
          sourceWasCorrelated && maps.getMsRowsForOtherRow(source.getID()).isEmpty();
      if (source.getNumberOfFeatures() == 0 || onlyAlignedLeft) {
        ofl.removeRow(source);
      }
    }
    if (maps.getPerFileCorrelation(msRowId, target.getID(), file) == null) {
      maps.addManualCorrelation(msRowId, target.getID(), file);
    }
    return new ManualOtherCorrelationResult(new OtherFeatureToOtherRowPair(target, placed),
        replaced, false, source, true);
  }

  /**
   * Finds the aligned row through which the MS row is correlated to a peak, e.g., to clear the
   * correlation of a peak selected from the processed features.
   *
   * @param ofl     the aligned other feature list
   * @param msRowId the MS row ID
   * @param feature the other detector feature
   * @return the first row (in preference order) that holds this peak and is correlated to the MS
   * row in the feature's raw data file, null if there is none
   */
  @Nullable
  public static OtherFeatureListRow findCorrelatedRow(@NotNull final OtherFeatureList ofl,
      final int msRowId, @NotNull final OtherFeature feature) {
    final RawDataFile file = feature.getRawDataFile();
    for (final OtherCorrelationLink link : ofl.getMsOtherCorrelationMaps()
        .getCorrelations(msRowId)) {
      if (!link.perFile().containsKey(file)) {
        continue;
      }
      final OtherFeatureListRow row = ofl.findRowByID(link.otherRowId());
      final OtherFeature cell = row != null ? row.getFeature(file) : null;
      if (cell != null && isSamePeak(cell, feature)) {
        return row;
      }
    }
    return null;
  }

  /**
   * @param ofl     the aligned other feature list
   * @param feature the peak to look for
   * @return the first row (in row order) that holds this peak and the stored copy, null if the peak
   * is not aligned
   */
  @Nullable
  private static OtherFeatureToOtherRowPair findAlignedPeak(@NotNull final OtherFeatureList ofl,
      @NotNull final OtherFeature feature) {
    final RawDataFile file = feature.getRawDataFile();
    final TraceKey key = TraceKey.of(feature);
    for (final OtherFeatureListRow row : ofl.getRows()) {
      if (!row.getTraceKey().equals(key)) {
        continue;
      }
      final OtherFeature cell = row.getFeature(file);
      if (cell != null && isSamePeak(cell, feature)) {
        return new OtherFeatureToOtherRowPair(row, cell);
      }
    }
    return null;
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

  /**
   * Moves the correlations of all MS rows to the source row in one file to the target row. They
   * keep their origin and score. A link to the source row without any remaining correlated file is
   * removed.
   *
   * @param source the row the peak was moved from
   * @param target the row the peak was moved to
   * @param file   the raw data file of the moved peak
   */
  private static void moveCorrelations(@NotNull final OtherFeatureListRow source,
      @NotNull final OtherFeatureListRow target, @NotNull final RawDataFile file) {
    final MsOtherCorrelationMaps maps = source.getOtherFeatureList().getMsOtherCorrelationMaps();
    for (final Integer msRowId : List.copyOf(maps.getMsRowsForOtherRow(source.getID()))) {
      final PerFileCorrelation moved = maps.getPerFileCorrelation(msRowId, source.getID(), file);
      if (moved != null) {
        maps.removeCorrelation(msRowId, source.getID(), file);
        maps.setPerFileCorrelation(msRowId, target.getID(), file, moved);
      }
    }
  }

  /**
   * After the cell of a file was replaced by a different peak, all MS rows correlated to this row
   * in this file now refer to the new peak.
   *
   * @param row  the aligned row with the replaced cell
   * @param file the raw data file of the replaced cell
   */
  private static void markCorrelationsManual(@NotNull final OtherFeatureListRow row,
      @NotNull final RawDataFile file) {
    final MsOtherCorrelationMaps maps = row.getOtherFeatureList().getMsOtherCorrelationMaps();
    // decision: keep the links but drop the score, which was calculated for the replaced peak
    for (final Integer msRowId : List.copyOf(maps.getMsRowsForOtherRow(row.getID()))) {
      if (maps.getPerFileCorrelation(msRowId, row.getID(), file) != null) {
        maps.addManualCorrelation(msRowId, row.getID(), file);
      }
    }
  }
}
