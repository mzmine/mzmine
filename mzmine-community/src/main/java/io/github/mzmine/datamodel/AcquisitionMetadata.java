/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.datamodel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Instrument and acquisition declarations read from a raw data file.
 *
 * <p>This is deliberately separate from the editable sample metadata table. Only terms from the
 * bundled PSI-MS allowlist are controlled terms; all other file header values remain local fields.
 */
public record AcquisitionMetadata(@NotNull List<Term> terms,
                                  @NotNull Map<String, String> localFields) {

  private static final Map<String, List<Term>> VOCABULARY = loadVocabulary();
  public static final AcquisitionMetadata EMPTY = new AcquisitionMetadata(List.of(), Map.of());

  public AcquisitionMetadata {
    final List<Term> providedTerms = terms;
    terms = providedTerms.stream().flatMap(term -> resolve(term.accession()).stream())
        .filter(term -> providedTerms.stream().anyMatch(
            original -> original.field() == term.field()))
        .distinct().toList();
    localFields = Map.copyOf(localFields);
  }

  public AcquisitionMetadata(final @NotNull List<Term> terms) {
    this(terms, Map.of());
  }

  public @NotNull AcquisitionMetadata plus(final @NotNull AcquisitionMetadata other) {
    final List<Term> mergedTerms = new ArrayList<>(terms);
    mergedTerms.addAll(other.terms);
    final Map<String, String> mergedFields = new LinkedHashMap<>(localFields);
    mergedFields.putAll(other.localFields);
    return new AcquisitionMetadata(mergedTerms, mergedFields);
  }

  /** Exact canonical-label match only; vendor text that is not in PSI-MS stays local. */
  public static @NotNull List<Term> resolveLabel(final @NotNull Field field,
      final @Nullable String label) {
    if (label == null || label.isBlank()) {
      return List.of();
    }
    return VOCABULARY.values().stream().flatMap(List::stream)
        .filter(term -> term.field() == field && term.label().equalsIgnoreCase(label.strip()))
        .toList();
  }

  /** Returns recognised controlled terms only; arbitrary CV names and values are ignored. */
  public static @NotNull List<Term> resolve(final @Nullable String accession) {
    return accession == null ? List.of() : VOCABULARY.getOrDefault(accession, List.of());
  }

  public enum Field {
    INSTRUMENT_MODEL, ANALYZER, IONIZATION, DETECTOR, ACQUISITION_METHOD
  }

  /** A controlled vocabulary declaration exactly as it occurred in the file. */
  public record Term(@NotNull Field field, @NotNull String accession, @NotNull String label) {
  }

  private static @NotNull Map<String, List<Term>> loadVocabulary() {
    try (final var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
        AcquisitionMetadata.class.getResourceAsStream("/acquisition-cv.tsv")),
        StandardCharsets.UTF_8))) {
      return reader.lines().filter(line -> !line.startsWith("#") && !line.isBlank())
          .map(line -> line.split("\\t", 3))
          .map(parts -> new Term(Field.valueOf(parts[0]), parts[1], parts[2]))
          .collect(Collectors.groupingBy(Term::accession, Collectors.toUnmodifiableList()));
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Cannot read packaged acquisition vocabulary", exception);
    }
  }
}
