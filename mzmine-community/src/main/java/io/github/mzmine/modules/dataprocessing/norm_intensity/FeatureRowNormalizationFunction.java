/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.XMLUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/** Precomputed factors and unavailable outcomes keyed by source feature-row ID. */
public final class FeatureRowNormalizationFunction implements NormalizationFunction {

  public static final String XML_TYPE = "feature_row";
  private static final String XML_PAYLOAD_ELEMENT = "packedRowFactors";
  private static final String XML_VERSION_ATTR = "version";
  private static final String XML_ENCODING_ATTR = "encoding";
  private static final String XML_FACTOR_COUNT_ATTR = "factorCount";
  private static final String XML_UNAVAILABLE_COUNT_ATTR = "unavailableCount";
  private static final int PAYLOAD_VERSION = 1;

  private final Map<Integer, Double> factors;
  private final Map<Integer, String> unavailableReasons;

  public FeatureRowNormalizationFunction(@NotNull final Map<Integer, Double> factors,
      @NotNull final Map<Integer, String> unavailableReasons) {
    this.factors = Collections.unmodifiableMap(new LinkedHashMap<>(factors));
    this.unavailableReasons = Collections.unmodifiableMap(new LinkedHashMap<>(unavailableReasons));
    validate();
  }

  private void validate() {
    if (!Collections.disjoint(factors.keySet(), unavailableReasons.keySet())) {
      throw new IllegalArgumentException("Feature-row factors and unavailable rows must be disjoint.");
    }
    for (final var entry : factors.entrySet()) {
      if (entry.getKey() == null || entry.getKey() <= 0 || entry.getValue() == null
          || !Double.isFinite(entry.getValue()) || entry.getValue() <= 0d) {
        throw new IllegalArgumentException("Feature-row factors require positive row IDs and factors.");
      }
    }
    for (final var entry : unavailableReasons.entrySet()) {
      if (entry.getKey() == null || entry.getKey() <= 0 || entry.getValue() == null
          || entry.getValue().isBlank()) {
        throw new IllegalArgumentException("Unavailable feature rows require positive IDs and reasons.");
      }
    }
  }

  public @NotNull Map<Integer, Double> factors() { return factors; }
  public @NotNull Map<Integer, String> unavailableReasons() { return unavailableReasons; }

  @Override
  public boolean equals(final Object other) {
    return other instanceof FeatureRowNormalizationFunction function
        && factors.equals(function.factors) && unavailableReasons.equals(function.unavailableReasons);
  }

  @Override
  public int hashCode() {
    return 31 * factors.hashCode() + unavailableReasons.hashCode();
  }

  @Override public @NotNull String getUniqueID() { return XML_TYPE; }
  @Override public boolean isRowDependent() { return true; }
  @Override public @NotNull Set<Integer> protectedRowIds() {
    final Set<Integer> ids = new LinkedHashSet<>(factors.keySet());
    ids.addAll(unavailableReasons.keySet());
    return Collections.unmodifiableSet(ids);
  }

  @Override public boolean protectsRowId(final int rowId) {
    return factors.containsKey(rowId) || unavailableReasons.containsKey(rowId);
  }

  public @NotNull OptionalDouble getFactorForRow(final int rowId) {
    final Double factor = factors.get(rowId);
    return factor == null ? OptionalDouble.empty() : OptionalDouble.of(factor);
  }

  @Override public @NotNull OptionalDouble getNormalizationFactor(@Nullable final FeatureListRow row,
      @NotNull final Double mz, @NotNull final Float rt) {
    return row == null ? OptionalDouble.empty() : getFactorForRow(row.getID());
  }

  @Override public double getNormalizationFactor(@NotNull final Double mz, @NotNull final Float rt) {
    throw new UnsupportedOperationException("Feature-row normalization requires a feature-list row ID.");
  }

  @Override public void saveToXML(final @NotNull Element functionElement) {
    functionElement.setAttribute(XML_FUNCTION_TYPE_ATTR, getUniqueID());
    final Element payload = functionElement.getOwnerDocument().createElement(XML_PAYLOAD_ELEMENT);
    payload.setAttribute(XML_VERSION_ATTR, Integer.toString(PAYLOAD_VERSION));
    payload.setAttribute(XML_ENCODING_ATTR, "base64-binary");
    payload.setAttribute(XML_FACTOR_COUNT_ATTR, Integer.toString(factors.size()));
    payload.setAttribute(XML_UNAVAILABLE_COUNT_ATTR, Integer.toString(unavailableReasons.size()));
    payload.setTextContent(Base64.getEncoder().encodeToString(pack()));
    functionElement.appendChild(payload);
  }

  private byte @NotNull [] pack() {
    try (final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(bytes)) {
      final Map<Integer, Double> orderedFactors = new TreeMap<>(factors);
      final Map<Integer, String> orderedUnavailable = new TreeMap<>(unavailableReasons);
      final Map<String, Integer> reasonIds = new LinkedHashMap<>();
      orderedUnavailable.values().forEach(reason -> reasonIds.putIfAbsent(reason, reasonIds.size()));
      out.writeInt(orderedFactors.size());
      for (final var entry : orderedFactors.entrySet()) { out.writeInt(entry.getKey()); out.writeDouble(entry.getValue()); }
      out.writeInt(reasonIds.size());
      for (final String reason : reasonIds.keySet()) { final byte[] text = reason.getBytes(StandardCharsets.UTF_8); out.writeInt(text.length); out.write(text); }
      out.writeInt(orderedUnavailable.size());
      for (final var entry : orderedUnavailable.entrySet()) { out.writeInt(entry.getKey()); out.writeInt(reasonIds.get(entry.getValue())); }
      out.flush(); return bytes.toByteArray();
    } catch (IOException e) { throw new IllegalStateException("Cannot pack row normalization factors", e); }
  }

  public static @NotNull FeatureRowNormalizationFunction loadFromXML(@NotNull final Element functionElement) {
    final Element payload = XMLUtils.streamChildElementsByTagName(functionElement, XML_PAYLOAD_ELEMENT)
        .reduce((left, right) -> { throw new IllegalArgumentException("Multiple packed row-factor payloads."); })
        .orElseThrow(() -> new IllegalArgumentException("Missing packed row-factor payload."));
    return loadPacked(payload);
  }

  private static @NotNull FeatureRowNormalizationFunction loadPacked(final Element payload) {
    if (Integer.parseInt(XMLUtils.requireAttribute(payload, XML_VERSION_ATTR)) != PAYLOAD_VERSION || !"base64-binary".equals(XMLUtils.requireAttribute(payload, XML_ENCODING_ATTR))) throw new IllegalArgumentException("Unsupported packed row-factor payload.");
    final int expectedFactors = Integer.parseInt(XMLUtils.requireAttribute(payload, XML_FACTOR_COUNT_ATTR));
    final int expectedUnavailable = Integer.parseInt(XMLUtils.requireAttribute(payload, XML_UNAVAILABLE_COUNT_ATTR));
    if (expectedFactors < 0 || expectedUnavailable < 0) throw new IllegalArgumentException("Negative row-factor payload count.");
    try (final DataInputStream in = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(payload.getTextContent().trim())))) {
      final Map<Integer, Double> factors = new LinkedHashMap<>(); final int factorCount = in.readInt();
      if (factorCount != expectedFactors || factorCount < 0) throw new IllegalArgumentException("Invalid packed factor count.");
      for (int i = 0; i < factorCount; i++) if (factors.put(in.readInt(), in.readDouble()) != null) throw new IllegalArgumentException("Duplicate packed factor row ID.");
      final int reasonCount = in.readInt(); if (reasonCount < 0 || reasonCount > expectedUnavailable || reasonCount > in.available() / Integer.BYTES) throw new IllegalArgumentException("Invalid packed reason count.");
      final String[] reasons = new String[reasonCount];
      for (int i = 0; i < reasonCount; i++) { final int length = in.readInt(); if (length < 0 || length > 1_000_000) throw new IllegalArgumentException("Invalid packed reason length."); final byte[] text = in.readNBytes(length); if (text.length != length) throw new IllegalArgumentException("Truncated packed reason."); reasons[i] = new String(text, StandardCharsets.UTF_8); }
      final Map<Integer, String> unavailable = new LinkedHashMap<>(); final int unavailableCount = in.readInt();
      if (unavailableCount != expectedUnavailable || unavailableCount < 0) throw new IllegalArgumentException("Invalid packed unavailable count.");
      for (int i = 0; i < unavailableCount; i++) { final int id = in.readInt(); final int reasonId = in.readInt(); if (reasonId < 0 || reasonId >= reasonCount || unavailable.put(id, reasons[reasonId]) != null) throw new IllegalArgumentException("Invalid packed unavailable row."); }
      if (in.read() != -1) throw new IllegalArgumentException("Unexpected packed row-factor payload data.");
      return new FeatureRowNormalizationFunction(factors, unavailable);
    } catch (IOException | IllegalArgumentException e) { throw new IllegalArgumentException("Invalid packed row-factor payload.", e); }
  }
}
