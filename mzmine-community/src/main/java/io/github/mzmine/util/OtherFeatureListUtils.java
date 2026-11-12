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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureToOtherRowPair;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationMaps;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureListRow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities to copy, transfer and prune the aligned other detector features
 * ({@link OtherFeatureList}) that are attached to an MS {@link ModularFeatureList}.
 */
public final class OtherFeatureListUtils {

  private OtherFeatureListUtils() {
  }

  /**
   * Creates a copy of a row: same ID and {@link OtherFeatureListRow#getTraceKey()}, copies of the
   * features ({@link OtherFeature#copy()}) and a copy of the row values. Changes to the rows or
   * features of one list therefore never leak into another list.
   *
   * @param row the row to copy
   * @return the new row
   */
  @NotNull
  public static OtherFeatureListRow copyRow(@NotNull final OtherFeatureListRow row) {
    final OtherFeatureListRow copy = new OtherFeatureListRow(row.getID(), row.getTraceKey());
    for (final OtherFeature feature : row.getFeatures()) {
      copy.addFeature(feature.copy());
    }
    copy.getMap().putAll(row.getMap());
    return copy;
  }

  /**
   * Creates a copy of an aligned other feature list for a copied MS feature list. Rows are copied
   * with {@link #copyRow(OtherFeatureListRow)} and keep their IDs. The MS-to-other correlations are
   * copied with remapped MS row IDs, correlations of MS rows without mapping are dropped.
   *
   * @param source           the list to copy
   * @param newStorage       the storage of the target MS feature list
   * @param oldToNewMsRowIds old MS row ID -> new MS row ID
   * @return the new list
   */
  @NotNull
  public static OtherFeatureList createCopy(@NotNull final OtherFeatureList source,
      @Nullable final MemoryMapStorage newStorage,
      @NotNull final Map<Integer, Integer> oldToNewMsRowIds) {
    final MsOtherCorrelationMaps maps = source.getMsOtherCorrelationMaps()
        .createCopyWithRemappedMsRowIds(oldToNewMsRowIds);
    final OtherFeatureList copy = new OtherFeatureList(source.getName(), newStorage,
        source.getRawDataFiles(), maps);
    copy.setDateCreated(source.getDateCreated());
    copy.addRowType(source.getRowTypes().toArray(DataType[]::new));
    copy.addFeatureType(source.getFeatureTypes().toArray(DataType[]::new));
    for (final OtherFeatureListRow row : source.getRows()) {
      copy.addRow(copyRow(row));
    }
    copy.setRowIdCounter(source.getRowIdCounter());
    return copy;
  }

  /**
   * Transfers a copy of the aligned other feature list from {@code source} to {@code target}, with
   * the MS row IDs of the correlations remapped. Does nothing if the source has no alignment. Also
   * works in place ({@code source == target}): the list is then replaced by a remapped copy, which
   * also drops correlations of removed rows.
   *
   * @param source           the MS feature list that holds the aligned other features
   * @param target           the MS feature list to attach the copy to, may be {@code source}
   * @param oldToNewMsRowIds old MS row ID (in source) -> new MS row ID (in target). MS rows without
   *                         mapping were removed.
   */
  public static void transferAlignedOtherFeatures(@NotNull final ModularFeatureList source,
      @NotNull final ModularFeatureList target,
      @NotNull final Map<Integer, Integer> oldToNewMsRowIds) {
    final OtherFeatureList sourceOfl = source.getAlignedOtherFeatures();
    if (sourceOfl == null) {
      return;
    }
    target.setAlignedOtherFeatures(
        createCopy(sourceOfl, target.getMemoryMapStorage(), oldToNewMsRowIds));
  }

  /**
   * Drops the MS-to-other correlations of the given MS rows that were removed from the feature list
   * in place. Use this when the removed rows are known, it only touches their entries.
   *
   * @param flist       the MS feature list the rows were removed from
   * @param removedRows the removed MS rows
   */
  public static void dropCorrelationsOfRemovedRows(@NotNull final ModularFeatureList flist,
      @NotNull final Collection<? extends FeatureListRow> removedRows) {
    final OtherFeatureList ofl = flist.getAlignedOtherFeatures();
    if (ofl == null || ofl.getMsOtherCorrelationMaps().isEmpty()) {
      return;
    }
    for (final FeatureListRow row : removedRows) {
      ofl.getMsOtherCorrelationMaps().clear(row.getID());
    }
  }

  /**
   * Drops the MS-to-other correlations of all MS rows that are not in the feature list anymore.
   * Scans all remaining rows, so use
   * {@link #dropCorrelationsOfRemovedRows(ModularFeatureList, Collection)} when the removed rows
   * are known. Otherwise a later row that reuses a removed row ID would get its correlations.
   * Mirrors {@code NormalizationRowIdentity#dropFactorsOfRemovedRows}.
   *
   * @param flist the MS feature list after rows were removed
   */
  public static void dropCorrelationsOfRemovedRows(@NotNull final ModularFeatureList flist) {
    final OtherFeatureList ofl = flist.getAlignedOtherFeatures();
    if (ofl == null || ofl.getMsOtherCorrelationMaps().isEmpty()) {
      return;
    }
    final Set<Integer> remainingIds = HashSet.newHashSet(flist.getNumberOfRows());
    for (final FeatureListRow row : flist.getRows()) {
      remainingIds.add(row.getID());
    }
    ofl.getMsOtherCorrelationMaps().retainMsRows(remainingIds);
  }

  /**
   * @param ofl  the aligned other feature list
   * @param file the raw data file
   * @return all aligned features of this file together with their rows, sorted by the feature RT.
   * Features without RT are skipped.
   */
  @NotNull
  public static List<OtherFeatureToOtherRowPair> getAlignedFeatures(
      @NotNull final OtherFeatureList ofl, @NotNull final RawDataFile file) {
    final List<OtherFeatureToOtherRowPair> result = new ArrayList<>();
    for (final OtherFeatureListRow row : ofl.getRows()) {
      final OtherFeature feature = row.getFeature(file);
      if (feature != null && feature.getRT() != null) {
        result.add(new OtherFeatureToOtherRowPair(row, feature));
      }
    }
    result.sort(Comparator.comparingDouble(a -> a.feature().getRT()));
    return result;
  }
}
