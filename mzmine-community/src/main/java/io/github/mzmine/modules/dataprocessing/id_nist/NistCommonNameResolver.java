/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 * SPDX-License-Identifier: MIT
 */
package io.github.mzmine.modules.dataprocessing.id_nist;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Chooses a concise display name without altering the original NIST library annotation. */
public final class NistCommonNameResolver {

  private static final Map<String, String> COMMON_NAMES_BY_CAS = Map.of(
      "54115", "Nicotine",
      "22083745", "Nicotine"
  );

  private NistCommonNameResolver() {
  }

  public static String preferredDisplayName(FeatureListRow row, SpectralDBAnnotation match) {
    final String original = clean(match.getCompoundName());
    final String cas = normalizeCas(match.getCAS());
    final String knownCommonName = COMMON_NAMES_BY_CAS.get(cas);
    if (knownCommonName != null) {
      return knownCommonName;
    }

    // Main and replicate NIST hits for the same compound can use different primary names. Prefer
    // the shortest, least-systematic-looking equivalent name when one is already in the hit list.
    final List<String> equivalentNames = row.getSpectralLibraryMatches().stream()
        .filter(NistMatchUtils::isNistMatch)
        .filter(other -> !cas.isBlank() && cas.equals(normalizeCas(other.getCAS())))
        .map(SpectralDBAnnotation::getCompoundName).filter(Objects::nonNull).map(String::strip)
        .filter(name -> !name.isBlank()).toList();
    return equivalentNames.stream().min(Comparator.comparingInt(NistCommonNameResolver::nameCost))
        .orElse(original);
  }

  static String preferredDisplayName(String cas, String original, List<String> alternatives) {
    final String known = COMMON_NAMES_BY_CAS.get(normalizeCas(cas));
    if (known != null) {
      return known;
    }
    return alternatives.stream().filter(Objects::nonNull).map(String::strip)
        .filter(name -> !name.isBlank()).min(Comparator.comparingInt(NistCommonNameResolver::nameCost))
        .orElse(clean(original));
  }

  private static int nameCost(String name) {
    int punctuationPenalty = 0;
    for (char character : name.toCharArray()) {
      if (character == ',' || character == '(' || character == ')' || character == '['
          || character == ']') {
        punctuationPenalty += 12;
      }
    }
    return name.length() + punctuationPenalty;
  }

  private static String normalizeCas(String cas) {
    return cas == null ? "" : cas.replaceAll("[^0-9]", "");
  }

  private static String clean(String name) {
    return name == null || name.isBlank() ? "Unknown" : name.strip();
  }
}
