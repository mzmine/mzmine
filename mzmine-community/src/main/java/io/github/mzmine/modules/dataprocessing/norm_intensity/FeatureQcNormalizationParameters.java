package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.metadata.SampleTypeFilterParameter;
import java.text.DecimalFormat;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/** Settings for feature-specific QC-RLSC with automatic or fixed smoothing. */
public class FeatureQcNormalizationParameters extends SimpleParameterSet {

  public static final BooleanParameter automaticSmoothing = new BooleanParameter(
      "Automatic smoothing", "Select the LOESS bandwidth and linear or quadratic degree by "
      + "leave-one-out cross-validation for each feature and batch. Disable to use the fixed "
      + "bandwidth and polynomial degree below. Both use the same raw-response QC-RLSC pipeline.", true);

  public static final IntegerParameter initialQcsToExclude = new IntegerParameter(
      "Initial QC injections to exclude", "Exclude this many initial reference injections per batch "
      + "from fitting and QC diagnostics. Use zero if conditioning QCs were already excluded. "
      + "Dunn's workflows exclude three for GC-MS and eight for UPLC-MS; samples are not deleted.",
      0, 0, Integer.MAX_VALUE);

  public static final SampleTypeFilterParameter sampleTypes = new SampleTypeFilterParameter(
      "Reference samples", "Select comparable pooled QC samples used to fit each feature's drift.",
      SampleTypeFilter.of(SampleType.QC));
  public static final DoubleParameter bandwidth = new DoubleParameter("LOESS bandwidth",
      "Fixed smoothing only: fraction of usable QCs in each local fit, rounded up to a whole "
      + "neighborhood. At least polynomial degree + 2 neighbors are needed. Automatic smoothing "
      + "selects the bandwidth instead.",
      new DecimalFormat("0.###"), .75, 0d, 1d);
  public static final IntegerParameter polynomialDegree = new IntegerParameter("Polynomial degree",
      "Fixed smoothing only: 1 for local linear or 2 for local quadratic fitting. "
      + "Automatic smoothing selects the degree instead.", 1, 1, 2);
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
      + "value. QC-RLSC allows at most 50% missing QCs per batch.",
      new DecimalFormat("0.###"), .5, 0d, 1d);

  public FeatureQcNormalizationParameters() {
    super(automaticSmoothing, sampleTypes, initialQcsToExclude, bandwidth, polynomialDegree,
        robustnessIterations, minimumQcs, maxFactor, globalMissingQcThreshold);
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
    return new FeatureQcSettings(getValue(automaticSmoothing), getValue(bandwidth),
        getValue(polynomialDegree), getValue(robustnessIterations),
        getValue(minimumQcs), getValue(maxFactor), getValue(globalMissingQcThreshold),
        getValue(initialQcsToExclude));
  }
}
