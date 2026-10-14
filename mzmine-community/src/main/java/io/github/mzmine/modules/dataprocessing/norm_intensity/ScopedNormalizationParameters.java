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
  public static final StringParameter rowFactors = new StringParameter("Reviewed row factors",
      "Optional exact feature-row/sample factors for a reviewed feature-specific correction.", "");
  public static final StringParameter unavailableRowIds = new StringParameter("Unavailable row IDs",
      "Rows intentionally left without normalized values by a reviewed feature-specific correction.", "");
  public static final HiddenParameter<IntensityNormalizationSummary> hiddenNormalizationSummary =
      new HiddenParameter<>(new NormalizationFunctionsParameter());

  public ScopedNormalizationParameters() {
    super(new Parameter[]{featureLists, suffix, sourceRevision, factors, rowFactors, unavailableRowIds,
        hiddenNormalizationSummary});
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
    parameters.setParameter(rowFactors, "");
    parameters.setParameter(unavailableRowIds, "");
    parameters.setParameter(hiddenNormalizationSummary, IntensityNormalizationSummary.EMPTY);
    return parameters;
  }

  /** Creates a reviewed copy using an exact factor for each row and source-file pair. */
  public static @NotNull ScopedNormalizationParameters createRowFactors(
      final @NotNull FeatureListsSelection lists, final @NotNull String selectedSuffix,
      final @NotNull String revision, final @NotNull Map<Integer, Map<String, Double>> selectedFactors,
      final @NotNull java.util.Set<Integer> unavailableRows) {
    final ScopedNormalizationParameters parameters = (ScopedNormalizationParameters) new ScopedNormalizationParameters()
        .cloneParameterSet();
    parameters.setParameter(featureLists, lists);
    parameters.setParameter(suffix, selectedSuffix);
    parameters.setParameter(sourceRevision, revision);
    parameters.setParameter(factors, "");
    parameters.setParameter(rowFactors, encodeRowFactors(selectedFactors));
    parameters.setParameter(unavailableRowIds, unavailableRows.stream().sorted()
        .map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
    parameters.setParameter(hiddenNormalizationSummary, IntensityNormalizationSummary.EMPTY);
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

  /** Encodes a compact, locale-independent exact factor provenance payload. */
  public static @NotNull String encodeRowFactors(
      final @NotNull Map<Integer, Map<String, Double>> input) {
    if (input.isEmpty()) return "none";
    return input.entrySet().stream().map(row -> row.getKey() + "@" + encodeFactors(row.getValue()))
        .collect(java.util.stream.Collectors.joining("|"));
  }

  public static @NotNull Map<Integer, Map<String, Double>> decodeRowFactors(
      final @NotNull String input) {
    final Map<Integer, Map<String, Double>> output = new LinkedHashMap<>();
    if (input.isBlank()) throw new IllegalArgumentException("Reviewed row factors are required.");
    if (input.equals("none")) return Map.of();
    for (final String part : input.split("\\|", -1)) {
      final String[] pair = part.split("@", -1);
      if (pair.length != 2) throw new IllegalArgumentException("Invalid reviewed row-factor entry: " + part);
      final int rowId;
      try { rowId = Integer.parseInt(pair[0]); }
      catch (final NumberFormatException exception) { throw new IllegalArgumentException("Invalid row ID: " + pair[0], exception); }
      if (rowId <= 0 || output.put(rowId, decodeFactors(pair[1])) != null)
        throw new IllegalArgumentException("Reviewed row factors need unique positive row IDs.");
    }
    return Map.copyOf(output);
  }

  public static @NotNull java.util.Set<Integer> decodeUnavailableRowIds(
      final @NotNull String input) {
    if (input.isBlank()) return java.util.Set.of();
    final java.util.Set<Integer> output = new java.util.LinkedHashSet<>();
    for (final String part : input.split(",", -1)) {
      final int rowId;
      try { rowId = Integer.parseInt(part); }
      catch (final NumberFormatException exception) { throw new IllegalArgumentException("Invalid unavailable row ID: " + part, exception); }
      if (rowId <= 0 || !output.add(rowId)) throw new IllegalArgumentException("Unavailable row IDs must be positive and unique.");
    }
    return java.util.Set.copyOf(output);
  }
}
