/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package io.github.mzmine.modules.dataanalysis.featurecomparison;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.statistics.FeatureListRowAbundances;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** Immutable abundance and status snapshot for one feature-list row. */
public record RowInput(int rowId, @NotNull List<Observation> samples) {

  public RowInput {
    samples = List.copyOf(samples);
  }

  /** Creates a snapshot directly from mzmine feature data using the requested abundance measure. */
  public static @NotNull RowInput of(@NotNull FeatureListRow row,
      @NotNull List<? extends RawDataFile> rawDataFiles, @NotNull AbundanceMeasure measure) {
    final List<Observation> observations = new ArrayList<>(rawDataFiles.size());
    for (final RawDataFile rawDataFile : rawDataFiles) {
      final Feature feature = row.getFeature(rawDataFile);
      observations.add(feature == null ? new Observation(FeatureStatus.UNKNOWN, null)
          : new Observation(feature.getFeatureStatus(), toDouble(getAbundance(feature, measure))));
    }
    return new RowInput(row.getID(), observations);
  }

  /**
   * Creates a snapshot from an abundance vector while preserving its original missing-value mask.
   */
  public static @NotNull RowInput of(@NotNull FeatureListRowAbundances abundances,
      @NotNull List<FeatureStatus> statuses) {
    if (abundances.numberOfSamples() != statuses.size()) {
      throw new IllegalArgumentException("Status and abundance vectors must have the same length");
    }
    final List<Observation> observations = new ArrayList<>(statuses.size());
    for (int index = 0; index < statuses.size(); index++) {
      final Double abundance = abundances.wasMissingValue(index) ? null : abundances.getValue(index);
      observations.add(new Observation(statuses.get(index), abundance));
    }
    return new RowInput(abundances.row().getID(), observations);
  }

  private static Double toDouble(Float value) {
    return value == null ? null : value.doubleValue();
  }

  private static Float getAbundance(@NotNull Feature feature, @NotNull AbundanceMeasure measure) {
    return switch (measure) {
      case Area -> feature.getArea();
      case Height -> feature.getHeight();
      // Normalized measures are modular data values and unavailable on legacy feature objects.
      case NORMALIZED_AREA, NORMALIZED_HEIGHT -> feature instanceof ModularDataModel model
          ? measure.get(model) : null;
    };
  }
}
