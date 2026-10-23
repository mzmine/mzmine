/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/** Guards row identities captured by directly accessible active normalization state. */
public final class NormalizationRowIdentity {

  private NormalizationRowIdentity() {}

  /** Cheap gate before looking at active row-level normalization state. */
  public static boolean hasNormalizedIntensityColumns(@NotNull final FeatureList featureList) {
    return featureList.hasFeatureType(NormalizedAreaType.class) || featureList.hasFeatureType(
        NormalizedHeightType.class);
  }

  public static boolean hasActiveRowDependentNormalization(@NotNull final FeatureList featureList) {
    return featureList.getPreferences().hasActiveRowDependentIntensityNormalization();
  }

  public static boolean requiresRowIdentityProtection(@NotNull final FeatureList featureList) {
    return hasNormalizedIntensityColumns(featureList) && hasActiveRowDependentNormalization(
        featureList);
  }

  public static @NotNull Set<Integer> protectedRowIds(@NotNull final FeatureList featureList) {
    return featureList.getPreferences().getActiveNormalizationProtectedRowIds();
  }

  public static void requireRenumberingAllowed(@NotNull final FeatureList featureList) {
    if (hasActiveRowDependentNormalization(featureList)) {
      throw new IllegalStateException("Cannot reset row IDs while active intensity normalization "
          + "contains row-dependent factors. Filter with row-ID resetting disabled.");
    }
  }

  public static void requireIdentityPreservingTransformation(@NotNull final FeatureList featureList,
      @NotNull final String operation) {
    if (hasActiveRowDependentNormalization(featureList)) {
      throw new IllegalStateException("Cannot %s while active intensity normalization contains "
          + "row-dependent factors. This operation changes feature-row identity.".formatted(operation));
    }
  }

  /** Rejects a newly allocated ID that is already represented by a frozen row-level outcome. */
  public static void requireNewRowIdAvailable(@NotNull final FeatureList featureList,
      final int rowId) {
    if (requiresRowIdentityProtection(featureList) && protectedRowIds(featureList).contains(rowId)) {
      throw new IllegalArgumentException("Cannot allocate row ID %d because it is reserved by the "
          + "active row-dependent intensity normalization.".formatted(rowId));
    }
  }

  public static boolean hasSameActiveNormalization(@NotNull final FeatureList source,
      @NotNull final FeatureList target) {
    if (source.getPreferences().hasDefinedActiveIntensityNormalization()
        != target.getPreferences().hasDefinedActiveIntensityNormalization()) {
      return false;
    }
    for (final var rawDataFile : target.getRawDataFiles()) {
      if (!Objects.equals(source.getPreferences().getActiveNormalizationFunction(rawDataFile),
          target.getPreferences().getActiveNormalizationFunction(rawDataFile))) {
        return false;
      }
    }
    return true;
  }
}
