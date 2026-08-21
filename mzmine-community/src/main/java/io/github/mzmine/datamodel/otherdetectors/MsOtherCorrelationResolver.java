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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds resolved {@link CorrelatedOtherFeature} views on demand from the ID-only
 * {@link MsOtherCorrelationMaps} and the attached {@link OtherFeatureList}. Central place used by the
 * derived "correlated traces" columns and by consumers that need the correlated {@link OtherFeature}.
 */
public final class MsOtherCorrelationResolver {

  private MsOtherCorrelationResolver() {
  }

  /**
   * Resolves a single {@link OtherCorrelationLink} to a row-level result: the aligned other-row, the
   * individual per-file {@link CorrelatedOtherFeature}s, and the best per-file score. Shared by the
   * row- and feature-level resolvers.
   *
   * @return the resolved row result, or null if the linked other-row no longer exists
   */
  @Nullable
  public static MsOtherCorrelationRowResult resolveCorrelation(final @NotNull OtherFeatureList ofl,
      final @NotNull OtherCorrelationLink link) {
    final OtherFeatureListRow otherRow = ofl.findRowByID(link.otherRowId());
    if (otherRow == null) {
      return null;
    }
    final List<CorrelatedOtherFeature> perFileResults = new ArrayList<>(link.perFile().size());
    for (final var entry : link.perFile().entrySet()) {
      final OtherFeature feature = otherRow.getFeature(entry.getKey());
      if (feature == null) {
        continue;
      }
      final PerFileCorrelation pfc = entry.getValue();
      perFileResults.add(new CorrelatedOtherFeature(feature, pfc.origin(), pfc.pearson()));
    }
    return new MsOtherCorrelationRowResult(link.otherRowId(), otherRow, bestCorrelation(link),
        perFileResults);
  }

  /**
   * Row-level view: one {@link MsOtherCorrelationRowResult} per correlated aligned other-row, in the
   * stored order (the first entry is the preferred trace). Order is set by the correlation task
   * (highest score first) and can be changed by the user via the feature-table combo. Each row result
   * carries the individual per-file {@link CorrelatedOtherFeature}s and the best per-file score.
   */
  @NotNull
  public static List<MsOtherCorrelationRowResult> resolveRowCorrelations(
      final @NotNull ModularFeatureList flist, final @NotNull FeatureListRow msRow) {
    final OtherFeatureList ofl = flist.getAlignedOtherFeatures();
    if (ofl == null) {
      return List.of();
    }
    final List<OtherCorrelationLink> links = ofl.getMsOtherCorrelationMaps()
        .getCorrelations(msRow.getID());
    final List<MsOtherCorrelationRowResult> out = new ArrayList<>(links.size());
    for (final OtherCorrelationLink link : links) {
      final MsOtherCorrelationRowResult result = resolveCorrelation(ofl, link);
      if (result != null) {
        out.add(result);
      }
    }
    return out;
  }

  /**
   * Feature-level view: the per-file correlation of the preferred correlated other-row (the first
   * entry of the row's correlation list) for this MS row and file.
   * <p>
   * If the preferred trace correlated in this file, the result carries that file's origin
   * ({@link MsOtherCorrelationType#CALCULATED}/{@link MsOtherCorrelationType#MANUAL}) and score. If the
   * trace is only aligned into this file (an aligned peak exists but this file's MS feature did not
   * correlate), the result carries origin {@link MsOtherCorrelationType#ALIGNED} and a null score -
   * this "aligned" status is derived here, never stored. Returns null only if the preferred trace has
   * no aligned peak in this file at all.
   */
  @Nullable
  public static CorrelatedOtherFeature resolvePreferredCorrelation(
      final @NotNull ModularFeatureList flist, final @NotNull FeatureListRow msRow,
      final @NotNull RawDataFile file) {
    final OtherFeatureList ofl = flist.getAlignedOtherFeatures();
    if (ofl == null) {
      return null;
    }
    final List<OtherCorrelationLink> links = ofl.getMsOtherCorrelationMaps()
        .getCorrelations(msRow.getID());
    if (links.isEmpty()) {
      return null;
    }
    final MsOtherCorrelationRowResult preferred = resolveCorrelation(ofl, links.getFirst());
    if (preferred == null) {
      return null;
    }
    // per-file correlated result for this file, if the preferred trace correlated here
    for (final CorrelatedOtherFeature result : preferred.perFileResults()) {
      if (result.otherFeature().getRawDataFile().equals(file)) {
        return result;
      }
    }
    // aligned into this file but not correlated here - derived ALIGNED status, not stored
    final OtherFeature feature = preferred.otherRow().getFeature(file);
    return feature == null ? null
        : new CorrelatedOtherFeature(feature, MsOtherCorrelationType.ALIGNED, null);
  }

  /**
   * @return the best (highest) per-file Pearson score of this link (ignoring nulls), or null if none
   * has a score.
   */
  @Nullable
  private static Float bestCorrelation(final @NotNull OtherCorrelationLink link) {
    Float best = null;
    for (final PerFileCorrelation pfc : link.perFile().values()) {
      if (pfc.pearson() != null && (best == null || pfc.pearson() > best)) {
        best = pfc.pearson();
      }
    }
    return best;
  }

  /**
   * @return the best (highest) per-file Pearson score of a link (0 if none), for ordering by score.
   */
  public static float scoreOf(final @NotNull OtherCorrelationLink link) {
    final Float c = bestCorrelation(link);
    return c == null ? 0f : c;
  }
}
