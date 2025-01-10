package io.github.mzmine.datamodel.featuredata;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.otherdetectors.MrmTransition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MrmUtils {

  /**
   * Extracts the {@link IonTimeSeries} in the specific rt range of the feature.
   */
  public static @Nullable IonTimeSeries<? extends Scan> getChromInRtRange(
      @NotNull FeatureListRow row, @Nullable RawDataFile file, @Nullable MrmTransition t) {
    if (file == null) {
      return null;
    }

    return getChromInRtRange((ModularFeature) row.getFeature(file), t);
  }

  /**
   * Extracts the {@link IonTimeSeries} in the specific rt range of the feature.
   */
  private static @Nullable IonTimeSeries<? extends Scan> getChromInRtRange(
      @Nullable ModularFeature feature, @Nullable MrmTransition t) {
    if (feature == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN || t == null
        || Range.singleton(0f).equals(feature.getRawDataPointsRTRange())) {
      return null;
    }

    final IonTimeSeries<? extends Scan> chrom = t.chromatogram();
    // sub series is pretty optimised, so we don't bother with a specific method
    final IonTimeSeries<? extends Scan> chromRange = chrom.subSeries(null,
        feature.getRawDataPointsRTRange().lowerEndpoint(),
        feature.getRawDataPointsRTRange().upperEndpoint());

    if (chromRange.getNumberOfValues() == 0) {
      return null;
    }
    return chromRange;
  }
}
