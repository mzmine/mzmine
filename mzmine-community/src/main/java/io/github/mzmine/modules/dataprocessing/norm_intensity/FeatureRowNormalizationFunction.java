/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.XMLUtils;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Frozen factors keyed by the stable ID of a feature-list row. Missing IDs are unavailable; they
 * never fall back to a coordinate-derived or identity factor.
 */
public record FeatureRowNormalizationFunction(@NotNull Map<Integer, Double> factors,
                                              @NotNull Set<Integer> unavailableRowIds)
    implements NormalizationFunction {

  public static final String XML_TYPE = "feature_row";
  private static final String XML_FACTOR_ELEMENT = "rowFactor";
  private static final String XML_UNAVAILABLE_ELEMENT = "unavailableRow";
  private static final String XML_ROW_ID_ATTR = "rowId";
  private static final String XML_FACTOR_ATTR = "factor";

  public FeatureRowNormalizationFunction {
    factors = Collections.unmodifiableMap(new LinkedHashMap<>(factors));
    unavailableRowIds = Collections.unmodifiableSet(new LinkedHashSet<>(unavailableRowIds));
    if (!java.util.Collections.disjoint(factors.keySet(), unavailableRowIds)) {
      throw new IllegalArgumentException("Feature-row factors and unavailable rows must be disjoint.");
    }
    for (final Map.Entry<Integer, Double> entry : factors.entrySet()) {
      if (entry.getKey() <= 0 || entry.getValue() == null || !Double.isFinite(entry.getValue())
          || entry.getValue() <= 0d) {
        throw new IllegalArgumentException("Feature-row factors require positive row IDs and factors.");
      }
    }
    if (unavailableRowIds.stream().anyMatch(id -> id <= 0)) {
      throw new IllegalArgumentException("Unavailable feature rows require positive row IDs.");
    }
  }

  @Override
  public @NotNull String getUniqueID() {
    return XML_TYPE;
  }

  /** Returns no factor for unavailable or later-added rows. */
  public @NotNull OptionalDouble getFactorForRow(final int rowId) {
    final Double factor = factors.get(rowId);
    return factor == null ? OptionalDouble.empty() : OptionalDouble.of(factor);
  }

  @Override
  public @NotNull OptionalDouble getNormalizationFactor(@Nullable final FeatureListRow row,
      @NotNull final Double mz, @NotNull final Float rt) {
    return row == null ? OptionalDouble.empty() : getFactorForRow(row.getID());
  }

  @Override
  public double getNormalizationFactor(@NotNull final Double mz, @NotNull final Float rt) {
    throw new UnsupportedOperationException(
        "Feature-row normalization requires a feature-list row ID; m/z and RT cannot identify it.");
  }

  @Override
  public void saveToXML(final @NotNull Element functionElement) {
    functionElement.setAttribute(XML_FUNCTION_TYPE_ATTR, getUniqueID());
    for (final Map.Entry<Integer, Double> entry : factors.entrySet()) {
      final Element factor = functionElement.getOwnerDocument().createElement(XML_FACTOR_ELEMENT);
      factor.setAttribute(XML_ROW_ID_ATTR, Integer.toString(entry.getKey()));
      factor.setAttribute(XML_FACTOR_ATTR, Double.toString(entry.getValue()));
      functionElement.appendChild(factor);
    }
    for (final int rowId : unavailableRowIds) {
      final Element unavailable = functionElement.getOwnerDocument().createElement(
          XML_UNAVAILABLE_ELEMENT);
      unavailable.setAttribute(XML_ROW_ID_ATTR, Integer.toString(rowId));
      functionElement.appendChild(unavailable);
    }
  }

  public static @NotNull FeatureRowNormalizationFunction loadFromXML(
      final @NotNull Element functionElement) {
    final Map<Integer, Double> factors = new LinkedHashMap<>();
    for (final Element factor : XMLUtils.streamChildElementsByTagName(functionElement,
        XML_FACTOR_ELEMENT).toList()) {
      final int rowId = Integer.parseInt(XMLUtils.requireAttribute(factor, XML_ROW_ID_ATTR));
      final double value = Double.parseDouble(XMLUtils.requireAttribute(factor, XML_FACTOR_ATTR));
      if (factors.put(rowId, value) != null) {
        throw new IllegalArgumentException("Feature-row factor XML contains duplicate row IDs.");
      }
    }
    final Set<Integer> unavailable = new LinkedHashSet<>();
    for (final Element row : XMLUtils.streamChildElementsByTagName(functionElement,
        XML_UNAVAILABLE_ELEMENT).toList()) {
      if (!unavailable.add(Integer.parseInt(XMLUtils.requireAttribute(row, XML_ROW_ID_ATTR)))) {
        throw new IllegalArgumentException("Feature-row normalization XML contains duplicate unavailable rows.");
      }
    }
    return new FeatureRowNormalizationFunction(factors, unavailable);
  }
}
