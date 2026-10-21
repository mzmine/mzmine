/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.features.FeatureList;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/** Guards transformations that would change IDs captured by the active normalization summary. */
public final class NormalizationRowIdentity {

  private NormalizationRowIdentity() {}

  public static boolean hasActiveRowDependentNormalization(@NotNull final FeatureList featureList) {
    return IntensityNormalizerModule.getNormalizationFunctionsOfLatestCall(featureList).functions()
        .stream().anyMatch(RawFileNormalizationFunction::isRowDependent);
  }

  public static @NotNull Set<Integer> protectedRowIds(@NotNull final FeatureList featureList) {
    final Set<Integer> ids = new LinkedHashSet<>();
    IntensityNormalizerModule.getNormalizationFunctionsOfLatestCall(featureList).functions()
        .forEach(function -> ids.addAll(function.protectedRowIds()));
    return Set.copyOf(ids);
  }

  public static void requireRenumberingAllowed(@NotNull final FeatureList featureList) {
    if (hasActiveRowDependentNormalization(featureList)) {
      throw new IllegalStateException("Cannot reset row IDs while the latest intensity normalization "
          + "contains row-dependent factors. Filter with row-ID resetting disabled.");
    }
  }

  public static void requireIdentityPreservingTransformation(@NotNull final FeatureList featureList,
      @NotNull final String operation) {
    if (hasActiveRowDependentNormalization(featureList)) {
      throw new IllegalStateException("Cannot %s while the latest intensity normalization contains "
          + "row-dependent factors. This operation changes feature-row identity.".formatted(operation));
    }
  }

  /** Rejects a newly allocated ID that is already represented by a frozen row-level outcome. */
  public static void requireNewRowIdAvailable(@NotNull final FeatureList featureList,
      final int rowId) {
    if (isProtectedRowId(featureList, rowId)) {
      throw new IllegalArgumentException("Cannot allocate row ID %d because it is reserved by the "
          + "latest row-dependent intensity normalization.".formatted(rowId));
    }
  }

  public static int nextUnprotectedRowId(@NotNull final FeatureList featureList,
      final int candidate) {
    int next = Math.max(1, candidate);
    // The normal allocation path already provides an unused ID. Avoid scanning every existing row
    // for ordinary append/import operations; only a frozen-ID collision needs collision resolution.
    if (!isProtectedRowId(featureList, next) && !containsRowId(featureList, next)) return next;
    do {
      next++;
    } while (isProtectedRowId(featureList, next) || containsRowId(featureList, next));
    return next;
  }

  private static boolean containsRowId(final FeatureList featureList, final int rowId) {
    return featureList.getRows().stream().anyMatch(row -> row.getID() == rowId);
  }

  private static boolean isProtectedRowId(final FeatureList featureList, final int rowId) {
    return IntensityNormalizerModule.getNormalizationFunctionsOfLatestCall(featureList).functions()
        .stream().anyMatch(function -> function.protectsRowId(rowId));
  }

  public static boolean hasSameLatestNormalization(@NotNull final FeatureList source,
      @NotNull final FeatureList target) {
    return IntensityNormalizerModule.getNormalizationFunctionsOfLatestCall(source).equals(
        IntensityNormalizerModule.getNormalizationFunctionsOfLatestCall(target));
  }
}
