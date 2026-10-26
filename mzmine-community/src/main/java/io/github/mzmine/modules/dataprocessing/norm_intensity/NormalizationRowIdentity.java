/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Row-dependent normalization factors are keyed by feature-list row ID. Normalization runs on
 * aligned feature lists, so afterward only copying, filtering, deleting, renumbering and merging
 * change row IDs. Copies, filters and deletions remap the factors once per task, merging is
 * blocked.
 */
public final class NormalizationRowIdentity {

  private NormalizationRowIdentity() {
  }

  public static boolean hasRowDependentNormalization(@NotNull final FeatureList flist) {
    final IntensityNormalizationSummary summary = flist.getSettings().normalization();
    return summary != null && summary.isRowDependent();
  }

  /**
   * Remaps the row factors of a row-dependent normalization after rows were copied, filtered or
   * renumbered. Does nothing if the source has no row-dependent normalization.
   *
   * @param source      the feature list the rows came from, may be the target itself
   * @param target      the feature list with the new rows
   * @param oldToNewIds source row ID -> target row ID; rows missing from the map were removed and
   *                    their factors are dropped
   */
  public static void remapRowFactors(@NotNull final FeatureList source,
      @NotNull final ModularFeatureList target, @NotNull final Map<Integer, Integer> oldToNewIds) {
    if (!hasRowDependentNormalization(source)) {
      return;
    }
    final IntensityNormalizationSummary summary = source.getSettings().normalization();
    target.setSettings(target.getSettings().withNormalization(summary.remapRowIds(oldToNewIds)));
  }

  /**
   * Drops the row factors of rows that were removed from the feature list in place, called by
   * {@link ModularFeatureList#removeRows(java.util.Collection)} and the removeRow methods.
   * Otherwise a later row that reuses a removed row ID would get its factor.
   */
  public static void dropFactorsOfRemovedRows(@NotNull final ModularFeatureList flist) {
    if (!hasRowDependentNormalization(flist)) {
      return;
    }
    // the remaining rows keep their IDs
    final Map<Integer, Integer> remainingIds = HashMap.newHashMap(flist.getNumberOfRows());
    for (final FeatureListRow row : flist.getRows()) {
      remainingIds.put(row.getID(), row.getID());
    }
    remapRowFactors(flist, flist, remainingIds);
  }

  /**
   * Re-applies a row-dependent normalization after features were added to rows, e.g., by gap
   * filling. Row-dependent normalization is skipped for new features that are not in their row yet,
   * see {@link FeatureDataUtils#normalizeAbundances(ModularFeature, NormalizationFunction)}. Row
   * bindings are not applied.
   *
   * @return true if the feature list has a row-dependent normalization and it was applied. Then the
   * row bindings need to be applied afterward.
   */
  public static boolean applyRowDependentIntensityNormalization(@NotNull final FeatureList flist) {
    if (!hasRowDependentNormalization(flist)) {
      return false;
    }
    final IntensityNormalizationSummary normalization = flist.getSettings().normalization();
    FeatureDataUtils.applyIntensityNormalization(flist,
        normalization.toSearchableSummary(flist.getRawDataFiles()));
    return true;
  }

  /**
   * Removes the intensity normalization of a feature list: the normalization functions in its
   * settings and all normalized values. Used by modules that create features with new intensities
   * that do not correspond to the normalized ones, e.g., resolving or smoothing. Normalize again
   * after alignment.
   */
  public static void removeIntensityNormalization(@NotNull final FeatureList flist) {
    flist.setSettings(flist.getSettings().withNormalization(null));
    FeatureDataUtils.clearIntensityNormalizationFromFeature(flist);
  }

  /**
   * Row IDs of different feature lists collide, so their row-dependent factors cannot be combined.
   *
   * @throws IllegalStateException if any feature list has a row-dependent normalization
   */
  public static void requireNoRowDependentNormalization(
      @NotNull final Collection<? extends FeatureList> flists, @NotNull final String operation) {
    for (final FeatureList flist : flists) {
      if (hasRowDependentNormalization(flist)) {
        throw new IllegalStateException(
            "Cannot %s: feature list %s has a row-dependent intensity normalization.".formatted(
                operation, flist.getName()));
      }
    }
  }
}
