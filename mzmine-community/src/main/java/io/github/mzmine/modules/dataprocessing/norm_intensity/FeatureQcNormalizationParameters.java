package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.metadata.SampleTypeFilterParameter;
import java.text.DecimalFormat;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/** Explicit settings for the common-scale, feature-specific QC method. */
public class FeatureQcNormalizationParameters extends SimpleParameterSet {

  public static final SampleTypeFilterParameter sampleTypes = new SampleTypeFilterParameter(
      "Reference samples", "Select comparable pooled QC samples used to fit each feature's drift.",
      SampleTypeFilter.of(SampleType.QC));
  public static final DoubleParameter bandwidth = new DoubleParameter("LOESS bandwidth",
      "Fraction of usable QC observations in each local linear fit. Must be greater than zero.",
      new DecimalFormat("0.###"), .75, 0d, 1d);
  public static final IntegerParameter robustnessIterations = new IntegerParameter(
      "Robustness iterations", "Number of robust reweighting iterations in the native LOESS solver.",
      2, 0, Integer.MAX_VALUE);
  public static final IntegerParameter minimumQcs = new IntegerParameter("Minimum usable QCs",
      "Minimum positive finite QC observations per feature and batch. mzmine requires at least five.",
      5, 5, Integer.MAX_VALUE);
  public static final DoubleParameter maxFactor = new DoubleParameter("Maximum reciprocal factor",
      "Reject a feature/batch if any factor is outside 1/value through value. Factors are never clipped.",
      new DecimalFormat("0.###"), 10d, 1d, Double.MAX_VALUE);
  public static final DoubleParameter globalMissingQcThreshold = new DoubleParameter(
      "Global missing QC fraction", "Skip a feature globally if its missing QC fraction exceeds this "
      + "value. Each batch independently requires strictly less than 50% missing QCs.",
      new DecimalFormat("0.###"), .5, 0d, 1d);

  public FeatureQcNormalizationParameters() {
    super(sampleTypes, bandwidth, robustnessIterations, minimumQcs, maxFactor,
        globalMissingQcThreshold);
  }

  @Override
  public boolean checkParameterValues(@NotNull final Collection<String> errorMessages,
      final boolean skipRawDataAndFeatureListParameters) {
    if (!super.checkParameterValues(errorMessages, skipRawDataAndFeatureListParameters)) return false;
    try {
      settings();
      return true;
    } catch (final IllegalArgumentException exception) {
      errorMessages.add(exception.getMessage());
      return false;
    }
  }

  public @NotNull FeatureQcSettings settings() {
    return new FeatureQcSettings(getValue(bandwidth), getValue(robustnessIterations),
        getValue(minimumQcs), getValue(maxFactor), getValue(globalMissingQcThreshold));
  }
}
