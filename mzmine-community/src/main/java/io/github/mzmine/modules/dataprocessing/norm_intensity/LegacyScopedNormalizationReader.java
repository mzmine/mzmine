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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Converts the removed scoped-normalization history format into a native frozen summary. */
public final class LegacyScopedNormalizationReader {

  public static final String LEGACY_MODULE_CLASS =
      "io.github.mzmine.modules.dataprocessing.norm_intensity.ScopedNormalizationModule";

  private static final String NAME_SUFFIX = "Name suffix";
  private static final String SOURCE_REVISION = "Source revision";
  private static final String FACTORS = "Reviewed factors";
  private static final String ROW_FACTORS = "Reviewed row factors";
  private static final String UNAVAILABLE_ROW_IDS = "Unavailable row IDs";
  private static final String NORMALIZATION_FUNCTIONS = "Normalization functions";

  private LegacyScopedNormalizationReader() {
  }

  /**
   * Converts legacy applied-method parameters. A request-only record is deliberately rejected:
   * its {@code s1} identifiers have no raw-file binding in applied-method XML.
   */
  public static @NotNull IntensityNormalizerParameters migrate(
      @NotNull final Element legacyParameters) {
    final IntensityNormalizationSummary summary = readCanonicalSummary(legacyParameters);
    if (summary.isEmpty()) {
      validateRequestPayload(legacyParameters);
      throw new IllegalArgumentException(
          "Legacy scoped normalization has no canonical frozen summary with raw-file bindings; "
              + "it cannot be replayed safely.");
    }

    final IntensityNormalizerParameters parameters = (IntensityNormalizerParameters) new IntensityNormalizerParameters()
        .cloneParameterSet();
    final String suffix = textValue(legacyParameters, NAME_SUFFIX);
    if (suffix != null) {
      parameters.setParameter(IntensityNormalizerParameters.suffix, suffix);
    }
    final String sourceRevision = textValue(legacyParameters, SOURCE_REVISION);
    final List<String> messages = new ArrayList<>(summary.messages());
    if (sourceRevision != null && !sourceRevision.isBlank()) {
      messages.add("Migrated legacy scoped normalization; source revision " + sourceRevision);
    } else {
      messages.add("Migrated legacy scoped normalization.");
    }
    parameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary,
        new IntensityNormalizationSummary(summary.functions(), messages));
    parameters.setParameter(IntensityNormalizerParameters.hiddenReviewedFunctions, true);
    return parameters;
  }

  private static @NotNull IntensityNormalizationSummary readCanonicalSummary(
      @NotNull final Element legacyParameters) {
    final Element summaryElement = parameterElement(legacyParameters, NORMALIZATION_FUNCTIONS);
    if (summaryElement == null) {
      return IntensityNormalizationSummary.EMPTY;
    }
    final NormalizationFunctionsParameter summaryParameter = new NormalizationFunctionsParameter();
    summaryParameter.loadValueFromXML(summaryElement);
    return summaryParameter.getValue();
  }

  /** Parses legacy request text solely to reject malformed or unbound records deterministically. */
  private static void validateRequestPayload(@NotNull final Element legacyParameters) {
    final String rowFactors = textValue(legacyParameters, ROW_FACTORS);
    if (rowFactors != null && !rowFactors.isBlank()) {
      final Map<Integer, Map<String, Double>> rows = decodeRowFactors(rowFactors);
      final Set<Integer> unavailable = decodeUnavailableRows(textValue(legacyParameters,
          UNAVAILABLE_ROW_IDS));
      if (!java.util.Collections.disjoint(rows.keySet(), unavailable)) {
        throw new IllegalArgumentException(
            "Legacy scoped normalization has overlapping available and unavailable rows.");
      }
      return;
    }
    final String factors = textValue(legacyParameters, FACTORS);
    if (factors == null || factors.isBlank()) {
      throw new IllegalArgumentException("Legacy scoped normalization has no frozen factors.");
    }
    decodeFactors(factors);
  }

  private static @NotNull Map<String, Double> decodeFactors(@NotNull final String value) {
    final Map<String, Double> factors = new LinkedHashMap<>();
    for (final String entry : value.split(";", -1)) {
      final String[] pair = entry.split("=", -1);
      if (pair.length != 2 || !pair[0].matches("s[1-9][0-9]*")) {
        throw new IllegalArgumentException("Invalid legacy reviewed factor entry: " + entry);
      }
      final double factor;
      try {
        factor = Double.parseDouble(pair[1]);
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException("Invalid legacy factor for " + pair[0], exception);
      }
      if (!Double.isFinite(factor) || factor <= 0d || factors.put(pair[0], factor) != null) {
        throw new IllegalArgumentException("Legacy reviewed factors must be positive and unique.");
      }
    }
    return factors;
  }

  private static @NotNull Map<Integer, Map<String, Double>> decodeRowFactors(
      @NotNull final String value) {
    final Map<Integer, Map<String, Double>> rows = new LinkedHashMap<>();
    if (value.equals("none")) {
      return rows;
    }
    for (final String entry : value.split("\\|", -1)) {
      final String[] pair = entry.split("@", -1);
      if (pair.length != 2) {
        throw new IllegalArgumentException("Invalid legacy row-factor entry: " + entry);
      }
      final int rowId;
      try {
        rowId = Integer.parseInt(pair[0]);
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException("Invalid legacy row ID: " + pair[0], exception);
      }
      if (rowId <= 0 || rows.put(rowId, decodeFactors(pair[1])) != null) {
        throw new IllegalArgumentException("Legacy row factors need unique positive row IDs.");
      }
    }
    return rows;
  }

  private static @NotNull Set<Integer> decodeUnavailableRows(final String value) {
    final Set<Integer> rows = new LinkedHashSet<>();
    if (value == null || value.isBlank()) {
      return rows;
    }
    for (final String entry : value.split(",", -1)) {
      final int rowId;
      try {
        rowId = Integer.parseInt(entry);
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException("Invalid legacy unavailable row ID: " + entry,
            exception);
      }
      if (rowId <= 0 || !rows.add(rowId)) {
        throw new IllegalArgumentException("Legacy unavailable row IDs must be positive and unique.");
      }
    }
    return rows;
  }

  private static String textValue(@NotNull final Element parent, @NotNull final String name) {
    final Element element = parameterElement(parent, name);
    return element == null ? null : element.getTextContent();
  }

  private static Element parameterElement(@NotNull final Element parent, @NotNull final String name) {
    final NodeList children = parent.getChildNodes();
    for (int index = 0; index < children.getLength(); index++) {
      final Node child = children.item(index);
      if (child instanceof Element element && SimpleParameterSet.parameterElement.equals(
          element.getTagName()) && name.equals(element.getAttribute(SimpleParameterSet.nameAttribute))) {
        return element;
      }
    }
    return null;
  }
}
