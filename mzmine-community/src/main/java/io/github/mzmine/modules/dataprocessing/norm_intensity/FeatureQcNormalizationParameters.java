package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.metadata.SampleTypeFilterParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import java.text.DecimalFormat;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/** Settings for the feature specific QC drift correction with QC-RLSC or manual LOESS. */
public class FeatureQcNormalizationParameters extends SimpleParameterSet {

  public static final SampleTypeFilterParameter sampleTypes = new SampleTypeFilterParameter(
      "Reference samples", "Select comparable pooled QC samples used to fit each feature's drift.",
      SampleTypeFilter.of(SampleType.QC));

  public static final ModuleOptionsEnumComboParameter<FeatureQcSmoothingOptions> smoothing = new ModuleOptionsEnumComboParameter<>(
      "Smoothing", """
      QC-RLSC (Dunn et al. 2011) selects the LOESS bandwidth and linear or quadratic degree by \
      leave-one-out cross-validation for each feature and batch.
      Manual LOESS uses a fixed bandwidth, polynomial degree and robustness iterations for all features.""",
      FeatureQcSmoothingOptions.QC_RLSC);

  public static final IntegerParameter minimumQcs = new IntegerParameter("Minimum usable QCs",
      "Minimum positive finite QC observations per feature and batch. mzmine requires at least five.",
      5, 5, Integer.MAX_VALUE);
  public static final DoubleParameter maxFactor = new DoubleParameter("Maximum correction factor", """
      Largest allowed change of a feature's value, up or down. With 10, values may be multiplied by \
      up to 10 or divided by up to 10.
      If any sample of a batch would need a larger correction, the feature gets no normalized values \
      in that batch instead of a capped correction. mzmine safeguard against unreliable fits, not part of QC-RLSC.""",
      new DecimalFormat("0.###"), 10d, 1d, Double.MAX_VALUE);
  public static final DoubleParameter globalMissingQcThreshold = new DoubleParameter(
      "Global missing QC fraction", "Skip a feature globally if its missing QC fraction exceeds this "
      + "value. QC-RLSC allows at most 50% missing QCs per batch.",
      new DecimalFormat("0.###"), .5, 0d, 1d);

  public FeatureQcNormalizationParameters() {
    super(sampleTypes, smoothing, minimumQcs, maxFactor, globalMissingQcThreshold);
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
    final ValueWithParameters<FeatureQcSmoothingOptions> selected = getParameter(smoothing)
        .getValueWithParameters();
    return new FeatureQcSettings(selected.value().createSmoothing(selected.parameters()),
        getValue(minimumQcs), getValue(maxFactor), getValue(globalMissingQcThreshold));
  }
}
