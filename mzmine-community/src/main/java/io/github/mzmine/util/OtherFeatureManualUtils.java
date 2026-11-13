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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
   * they now refer to the new feature: if a different peak replaced the cell, the correlations of
   * all MS rows to this row in this file become manual without a score, as their score was
   * calculated for the replaced peak.
   *
   * @param row     the aligned row, must have the feature's trace key
   * @param feature the feature to set, a copy is stored
   * @return the row and the feature in its cell
   */
  @NotNull
  public static OtherFeatureToOtherRowPair setFeature(@NotNull final OtherFeatureListRow row,
      @NotNull final OtherFeature feature) {
    final RawDataFile file = feature.getRawDataFile();
    final OtherFeature current = row.getFeature(file);
    if (current != null && isSamePeak(current, feature)) {
      return new OtherFeatureToOtherRowPair(row, current);
    }
    final OtherFeature copy = feature.copy();
    if (row.setFeature(copy) != null) {
      markCorrelationsManual(row, file);
    }
    return new OtherFeatureToOtherRowPair(row, copy);
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
   * aligned list is created if needed.
   * <p>
   * If the MS row already correlates to the feature's trace, the first such row (in preference
   * order, in any file) takes the feature:
   * <ul>
   *   <li>Its cell already holds this peak: the correlation in this file is set to manual if it is
   *   missing, otherwise nothing changes.</li>
   *   <li>The MS row is correlated to this peak through another row of the trace (misaligned, e.g.,
   *   the peaks at 1.3 and 1.4 min were swapped in this file): the peak is moved to the target row
   *   together with all its correlations, which keep their scores. See
   *   {@link #movePeak(OtherFeatureListRow, OtherFeatureListRow, RawDataFile)}.</li>
   *   <li>Otherwise its cell for this file is replaced by a copy, e.g., the user picks the peak at
   *   13.5 min instead of the correlated one at 13.3 min. See
   *   {@link #setFeature(OtherFeatureListRow, OtherFeature)} for other MS rows linked to this
   *   row. The correlation of the MS row in this file is set to manual.</li>
   * </ul>
   * If the MS row does not correlate to this trace, the first row that holds this peak or a new
   * single-file row takes it, and the correlation in this file is set to manual.
   *
   * @param flist   the MS feature list
   * @param msRow   the MS row
   * @param feature the other detector feature, usually a processed feature of the raw file. A copy
   *                is stored if it is not in the aligned list yet.
   * @return the row and feature that are now correlated
   */
  @NotNull
  public static ManualOtherCorrelationResult correlateManually(
      @NotNull final ModularFeatureList flist, @NotNull final FeatureListRow msRow,
      @NotNull final OtherFeature feature) {
    final OtherFeatureList ofl = getOrCreateAlignedList(flist);
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    final RawDataFile file = feature.getRawDataFile();
    final int msRowId = msRow.getID();

    final OtherFeatureListRow target = findCorrelatedRowOfTrace(ofl, msRowId, TraceKey.of(feature));
    if (target == null) {
      final OtherFeatureListRow withPeak = findRowsWithPeak(ofl, feature).stream().findFirst()
          .orElse(null);
      final ManualOtherCorrelationResult result =
          withPeak == null ? new ManualOtherCorrelationResult(addAsNewRow(ofl, feature), null, true,
              null, false, true) : new ManualOtherCorrelationResult(
              new OtherFeatureToOtherRowPair(withPeak, withPeak.getFeature(file)), null, false,
              null, false, true);
      maps.addManualCorrelation(msRowId, result.aligned().row().getID(), file);
      return result;
    }

    final OtherFeature cell = target.getFeature(file);
    if (cell != null && isSamePeak(cell, feature)) {
      final boolean correlated = maps.getPerFileCorrelation(msRowId, target.getID(), file) != null;
      if (!correlated) {
        maps.addManualCorrelation(msRowId, target.getID(), file);
      }
      return new ManualOtherCorrelationResult(new OtherFeatureToOtherRowPair(target, cell), null,
          false, null, false, !correlated);
    }

    // decision: correlated to this peak through another row of the same trace means misaligned
    final OtherFeatureListRow source = findCorrelatedRow(ofl, msRowId, feature);
    if (source != null) {
      return movePeak(source, target, file);
    }

    final OtherFeature copy = feature.copy();
    final OtherFeature replaced = target.setFeature(copy);
    if (replaced != null) {
      markCorrelationsManual(target, file);
    }
    maps.addManualCorrelation(msRowId, target.getID(), file);
    return new ManualOtherCorrelationResult(new OtherFeatureToOtherRowPair(target, copy), replaced,
        false, null, false, true);
  }

  /**
   * Moves a misaligned peak of one raw data file from the source to the target row and replaces the
   * target's peak in this file. All correlations of MS rows to the moved peak move with it and keep
   * their origin and score. MS rows that were correlated to the replaced peak (but not to the moved
   * one) keep the link, but it becomes manual without a score. A link to the source row without any
   * remaining correlated file is removed. The source row is removed if it has no peaks left or no
   * MS row correlates to it anymore (only aligned peaks remain).
   *
   * @param source the row that holds the peak
   * @param target the row that takes the peak, must have the same trace key
   * @param file   the raw data file of the peak
   * @return the result with the target row and the moved peak
   */
  @NotNull
  private static ManualOtherCorrelationResult movePeak(@NotNull final OtherFeatureListRow source,
      @NotNull final OtherFeatureListRow target, @NotNull final RawDataFile file) {
    final OtherFeatureList ofl = target.getOtherFeatureList();
    final MsOtherCorrelationMaps maps = ofl.getMsOtherCorrelationMaps();
    final OtherFeature moved = source.removeFeature(file);
    if (moved == null) {
      throw new IllegalStateException(
          "Source row %d has no peak in file %s".formatted(source.getID(), file.getName()));
    }
    final OtherFeature replaced = target.setFeature(moved);

    final Set<Integer> msRowIds = new LinkedHashSet<>(maps.getMsRowsForOtherRow(source.getID()));
    msRowIds.addAll(maps.getMsRowsForOtherRow(target.getID()));
    for (final Integer msRowId : msRowIds) {
      final PerFileCorrelation movedCorrelation = maps.getPerFileCorrelation(msRowId,
          source.getID(), file);
      if (movedCorrelation != null) {
        // drops the link to the source row if no other file is correlated
        maps.removeCorrelation(msRowId, source.getID(), file);
        maps.setPerFileCorrelation(msRowId, target.getID(), file, movedCorrelation);
      } else if (replaced != null
          && maps.getPerFileCorrelation(msRowId, target.getID(), file) != null) {
        // decision: the score was calculated for the replaced peak
        maps.addManualCorrelation(msRowId, target.getID(), file);
      }
    }

    final boolean removeSource =
        source.getNumberOfFeatures() == 0 || maps.getMsRowsForOtherRow(source.getID()).isEmpty();
    if (removeSource) {
      ofl.removeRow(source);
    }
    return new ManualOtherCorrelationResult(new OtherFeatureToOtherRowPair(target, moved), replaced,
        false, source, removeSource, true);
  }

  /**
   * @param ofl     the aligned other feature list
   * @param msRowId the MS row ID
   * @param feature the other detector feature
   * @return true if the MS row is correlated to this peak in the feature's raw data file
   */
  public static boolean isCorrelated(@NotNull final OtherFeatureList ofl, final int msRowId,
      @NotNull final OtherFeature feature) {
    return findCorrelatedRow(ofl, msRowId, feature) != null;
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
