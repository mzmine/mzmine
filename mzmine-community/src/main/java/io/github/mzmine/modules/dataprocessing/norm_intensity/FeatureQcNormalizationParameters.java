package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.metadata.SampleTypeFilterParameter;
import java.text.DecimalFormat;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/** Settings shared by the selectable feature-specific QC correction methods. */
public class FeatureQcNormalizationParameters extends SimpleParameterSet {

  public static final ComboParameter<FeatureQcMethod> method = new ComboParameter<>(
      "QC correction method", "QC-RLSC selects linear/quadratic fits and bandwidth by leave-one-out "
      + "validation on raw response versus injection order. Common-scale log LOESS uses a fixed "
      + "bandwidth versus acquisition time and a global median-log target.",
      FeatureQcMethod.values(), FeatureQcMethod.DUNN_QC_RLSC);

  public static final IntegerParameter initialQcsToExclude = new IntegerParameter(
      "Initial QC injections to exclude", "Exclude this many initial reference injections per batch "
      + "from fitting and QC diagnostics. Use zero if conditioning QCs were already excluded. "
      + "Dunn's workflows exclude three for GC-MS and eight for UPLC-MS; samples are not deleted.",
      0, 0, Integer.MAX_VALUE);

  public static final SampleTypeFilterParameter sampleTypes = new SampleTypeFilterParameter(
      "Reference samples", "Select comparable pooled QC samples used to fit each feature's drift.",
      SampleTypeFilter.of(SampleType.QC));
  public static final DoubleParameter bandwidth = new DoubleParameter("LOESS bandwidth",
      "Common-scale log LOESS only: fraction of usable QCs in each local linear fit. "
      + "QC-RLSC selects its bandwidth automatically.",
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
      + "value. QC-RLSC allows at most 50% missing QCs per batch; common-scale log LOESS "
      + "requires strictly less than 50%.",
      new DecimalFormat("0.###"), .5, 0d, 1d);

  public FeatureQcNormalizationParameters() {
    super(method, sampleTypes, initialQcsToExclude, bandwidth, robustnessIterations, minimumQcs, maxFactor,
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
    return new FeatureQcSettings(getValue(method), getValue(bandwidth), getValue(robustnessIterations),
        getValue(minimumQcs), getValue(maxFactor), getValue(globalMissingQcThreshold),
        getValue(initialQcsToExclude));
  }
}
