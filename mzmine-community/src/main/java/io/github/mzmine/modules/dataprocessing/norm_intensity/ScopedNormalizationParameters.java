package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/** Serializable frozen factor payload. Sample IDs are table-order IDs such as s1, s2. */
public final class ScopedNormalizationParameters extends SimpleParameterSet {
  public static final FeatureListsParameter featureLists = new FeatureListsParameter();
  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix appended to the normalized feature-list copy.", "scoped norm");
  public static final StringParameter sourceRevision = new StringParameter("Source revision",
      "Frozen result-snapshot revision reviewed before normalization.", "");
  public static final StringParameter factors = new StringParameter("Reviewed factors",
      "One semicolon-separated sample ID=factor pair per source file, retained in project provenance.", "");
  public static final HiddenParameter<IntensityNormalizationSummary> hiddenNormalizationSummary =
      new HiddenParameter<>(new NormalizationFunctionsParameter());

  public ScopedNormalizationParameters() {
    super(new Parameter[]{featureLists, suffix, sourceRevision, factors, hiddenNormalizationSummary});
  }

  public static @NotNull ScopedNormalizationParameters create(final @NotNull FeatureListsSelection lists,
      final @NotNull String selectedSuffix, final @NotNull String revision,
      final @NotNull Map<String, Double> selectedFactors) {
    final ScopedNormalizationParameters parameters = (ScopedNormalizationParameters) new ScopedNormalizationParameters()
        .cloneParameterSet();
    parameters.setParameter(featureLists, lists);
    parameters.setParameter(suffix, selectedSuffix);
    parameters.setParameter(sourceRevision, revision);
    parameters.setParameter(factors, encodeFactors(selectedFactors));
    return parameters;
  }

  public static @NotNull String encodeFactors(final @NotNull Map<String, Double> input) {
    return input.entrySet().stream().map(entry -> entry.getKey() + "=" + Double.toString(entry.getValue()))
        .collect(java.util.stream.Collectors.joining(";"));
  }

  public static @NotNull Map<String, Double> decodeFactors(final @NotNull String input) {
    final Map<String, Double> output = new LinkedHashMap<>();
    if (input.isBlank()) throw new IllegalArgumentException("Reviewed factors are required.");
    for (final String part : input.split(";", -1)) {
      final String[] pair = part.split("=", -1);
      if (pair.length != 2 || !pair[0].matches("s[1-9][0-9]*"))
        throw new IllegalArgumentException("Invalid reviewed factor entry: " + part);
      final double factor;
      try { factor = Double.parseDouble(pair[1]); }
      catch (final NumberFormatException exception) { throw new IllegalArgumentException("Invalid factor for " + pair[0], exception); }
      if (!Double.isFinite(factor) || factor <= 0d || output.put(pair[0], factor) != null)
        throw new IllegalArgumentException("Reviewed factor must be positive and unique for " + pair[0]);
    }
    return Map.copyOf(output);
  }
}
