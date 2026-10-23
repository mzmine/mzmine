/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import java.util.Collection;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Row-dependent normalization factors are keyed by feature-list row ID. Normalization runs on
 * aligned feature lists, so afterward only copying, filtering, renumbering and merging change row
 * IDs. Copies and filters remap the factors once per task, merging is blocked.
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
