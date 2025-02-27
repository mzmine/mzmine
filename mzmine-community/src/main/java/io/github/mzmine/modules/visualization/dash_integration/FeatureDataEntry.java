package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IntensityTimeSeriesToXYProvider;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param file           The raw data file
 * @param feature        the integrated feature as shown in the feature table. Null if the feature
 *                       was not detected.
 * @param chromatogram   the chromatogram within 2x the rt range of the row
 * @param additionalData additional chromatograms, e.g. from mrm traces. May be empty
 */
public record FeatureDataEntry(@NotNull RawDataFile file, @Nullable IntensityTimeSeries feature,
                               @NotNull IntensityTimeSeries chromatogram,
                               @NotNull List<IntensityTimeSeriesToXYProvider> additionalData) {

}
