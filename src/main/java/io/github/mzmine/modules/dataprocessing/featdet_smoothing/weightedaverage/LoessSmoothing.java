package io.github.mzmine.modules.dataprocessing.featdet_smoothing.weightedaverage;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingAlgorithm;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.IonMobilityUtils;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoessSmoothing implements SmoothingAlgorithm {

  private final Integer mobilityWidth;
  private final Integer rtWidth;
  private final boolean smoothRt;
  private final boolean smoothMobility;

  public LoessSmoothing() {
    this.mobilityWidth = null;
    this.rtWidth = null;
    this.smoothRt = true;
    this.smoothMobility = true;
  }

  public LoessSmoothing(ParameterSet parameters) {
    this.mobilityWidth = parameters.getParameter(LoessSmoothingParameters.mobilitySmoothing)
        .getEmbeddedParameter().getValue();
    this.rtWidth = parameters.getParameter(LoessSmoothingParameters.rtSmoothing)
        .getEmbeddedParameter().getValue();

    smoothRt = parameters.getParameter(LoessSmoothingParameters.rtSmoothing).getValue();
    smoothMobility = parameters.getParameter(LoessSmoothingParameters.mobilitySmoothing).getValue();
  }

  @Override
  public @NotNull String getName() {
    return "Loess smoothing";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return LoessSmoothingParameters.class;
  }

  @Override
  public <T extends Scan> @Nullable double[] smoothRt(@NotNull IonTimeSeries<T> series) {
    if (!smoothRt) {
      return null;
    }

    // has to be at least 3 points wide
//    final double rtWidth = Math.max(1d / series.getNumberOfValues() * 3, this.rtWidth);
    final double rtWidth = (1d / series.getNumberOfValues() * this.rtWidth);
    final LoessInterpolator interpolator = new LoessInterpolator(rtWidth, 0);

    double[] intensities = new double[series.getNumberOfValues()];
    double[] rts = new double[series.getNumberOfValues()];
    for (int i = 0; i < rts.length; i++) {
      intensities[i] = series.getIntensity(i);
      rts[i] = series.getRetentionTime(i);
    }

    double[] smoothed = interpolator.smooth(rts, intensities);
    for (int i = 0; i < intensities.length; i++) {
      if (Double.compare(intensities[i], 0d) <= 0) {
        smoothed[i] = 0d;
      }
    }
    return smoothed;
  }

  @Override
  @Nullable
  public <T extends IntensitySeries & MobilitySeries> double[] smoothMobility(
      @NotNull T mobilogram) {
    if (!smoothMobility) {
      return null;
    }

    // has to be at least 3 points wide
//    final double mobilityWidth = Math
//        .max(1d / mobilogram.getNumberOfValues() * 3, this.mobilityWidth);
    final double mobilityWidth = (1d / mobilogram.getNumberOfValues() * this.mobilityWidth);
    final LoessInterpolator interpolator = new LoessInterpolator(mobilityWidth, 0);

    double[] intensities = new double[mobilogram.getNumberOfValues()];
    mobilogram.getIntensityValues(intensities);
    double[] mobilities = IonMobilityUtils.extractMobilities(mobilogram);

    double[] smoothed = interpolator.smooth(mobilities, intensities);
    for (int i = 0; i < intensities.length; i++) {
      if (Double.compare(intensities[i], 0d) <= 0) {
        smoothed[i] = 0d;
      }
    }
    return smoothed;
  }


}
