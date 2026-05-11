/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundNameIdentifier;
import io.github.mzmine.util.spectraldb.entry.AnalogCompoundGroup.RowAnnotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Groups a list of analog spectral-library annotations (each tagged with the row that produced it)
 * into compound clusters via union-find over {@link CompoundNameIdentifier} values.
 *
 * <p>Two annotations end up in the same cluster iff <b>any</b> non-null identifier value matches
 * — InChIKey, InChI, SMILES, compound name, formula, CAS, etc. — so library entries that differ
 * only in collision energy, adduct, or name format collapse to one compound. Grouping is
 * transitive: A↔B via InChIKey and B↔C via SMILES yields {A, B, C}.
 */
public final class AnalogCompoundGrouper {

  private AnalogCompoundGrouper() {
  }

  /**
   * Group annotations into compound clusters.
   *
   * @param all every (row, annotation) pair from a feature list's analog matches
   * @return one {@link AnalogCompoundGroup} per cluster
   */
  public static @NotNull List<AnalogCompoundGroup> group(@NotNull final List<RowAnnotation> all) {
    if (all.isEmpty()) {
      return List.of();
    }

    final int n = all.size();
    final UnionFind uf = new UnionFind(n);

    // index every (normalized identifier value) -> annotation indices that carry it; whenever two
    // or more indices share a value, union them. InChIKey is also indexed by its first 14 chars
    // (the stereo-tolerant prefix) so stereo and non-stereo variants merge.
    final Map<IdentifierValue, Integer> firstSeen = new HashMap<>();
    for (int i = 0; i < n; i++) {
      final var annotation = all.get(i).annotation();
      for (final CompoundNameIdentifier id : CompoundNameIdentifier.values()) {
        final String raw = annotation.getNameIdentifier(id);
        final String normalized = normalize(raw);
        if (normalized == null) {
          continue;
        }
        unionByKey(uf, firstSeen, new IdentifierValue(id, normalized), i);
        if (id == CompoundNameIdentifier.INCHI_KEY && normalized.length() >= 14) {
          // stereo-tolerant prefix link; uses a distinct identifier marker so a 14-char prefix
          // collision with another identifier type can't accidentally union unrelated annotations
          unionByKey(uf, firstSeen, new IdentifierValue(id, normalized.substring(0, 14) + "*"), i);
        }
      }
    }

    // bucket annotation indices by union-find root, preserving input order for stability
    final Map<Integer, List<Integer>> buckets = new LinkedHashMap<>();
    for (int i = 0; i < n; i++) {
      buckets.computeIfAbsent(uf.find(i), _ -> new ArrayList<>()).add(i);
    }

    final List<AnalogCompoundGroup> result = new ArrayList<>(buckets.size());
    for (final List<Integer> indices : buckets.values()) {
      final List<RowAnnotation> members = new ArrayList<>(indices.size());
      for (final int idx : indices) {
        members.add(all.get(idx));
      }
      final SpectralDBAnnotation representative = pickRepresentative(members);
      final String compoundKey = bestCompoundKey(representative);
      result.add(new AnalogCompoundGroup(members, representative, compoundKey));
    }
    return result;
  }

  private static void unionByKey(final UnionFind uf, final Map<IdentifierValue, Integer> firstSeen,
      final IdentifierValue key, final int index) {
    final Integer prior = firstSeen.putIfAbsent(key, index);
    if (prior != null) {
      uf.union(prior, index);
    }
  }

  // Most non-null identifier fields first; ties broken by highest similarity score so the cluster's
  // representative is the most fully-described entry (and tends to have a structure for the chart).
  private static SpectralDBAnnotation pickRepresentative(final List<RowAnnotation> members) {
    SpectralDBAnnotation best = members.getFirst().annotation();
    int bestFilled = countFilledIdentifiers(best);
    float bestScore = best.getScore() == null ? 0f : best.getScore();
    for (int i = 1; i < members.size(); i++) {
      final SpectralDBAnnotation candidate = members.get(i).annotation();
      final int filled = countFilledIdentifiers(candidate);
      final float score = candidate.getScore() == null ? 0f : candidate.getScore();
      if (filled > bestFilled || (filled == bestFilled && score > bestScore)) {
        best = candidate;
        bestFilled = filled;
        bestScore = score;
      }
    }
    return best;
  }

  private static int countFilledIdentifiers(final SpectralDBAnnotation a) {
    int count = 0;
    for (final CompoundNameIdentifier id : CompoundNameIdentifier.values()) {
      if (normalize(a.getNameIdentifier(id)) != null) {
        count++;
      }
    }
    return count;
  }

  // Mirrors FeatureAnnotation.getBestNameIdentifier() so the cluster's display key uses the same
  // preference order as the rest of the codebase.
  private static String bestCompoundKey(final SpectralDBAnnotation representative) {
    for (final CompoundNameIdentifier id : CompoundNameIdentifier.values()) {
      final String raw = representative.getNameIdentifier(id);
      final String normalized = normalize(raw);
      if (normalized != null) {
        // preserve original (non-uppercased) value for display so e.g. SMILES casing is kept
        return raw.trim();
      }
    }
    return null;
  }

  private static String normalize(final String raw) {
    if (raw == null) {
      return null;
    }
    final String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    // case-fold for matching; for case-sensitive identifiers like SMILES this risks false unions
    // between SMILES strings that differ only in atom case (e.g. "n" aromatic vs. "N"), but the
    // far more common case is duplicate spellings of compound names and CAS numbers. Acceptable.
    return trimmed.toLowerCase(Locale.ROOT);
  }

  // Tag the identifier source so the same normalized string under different identifiers doesn't
  // accidentally union (e.g. an InChIKey prefix colliding with a compound name fragment).
  private record IdentifierValue(CompoundNameIdentifier source, String value) {

  }

  /**
   * Compact union-find with path compression and union by rank.
   */
  private static final class UnionFind {

    private final int[] parent;
    private final int[] rank;

    UnionFind(final int size) {
      parent = new int[size];
      rank = new int[size];
      for (int i = 0; i < size; i++) {
        parent[i] = i;
      }
    }

    int find(int x) {
      while (parent[x] != x) {
        parent[x] = parent[parent[x]];
        x = parent[x];
      }
      return x;
    }

    void union(final int a, final int b) {
      final int ra = find(a);
      final int rb = find(b);
      if (ra == rb) {
        return;
      }
      if (rank[ra] < rank[rb]) {
        parent[ra] = rb;
      } else if (rank[ra] > rank[rb]) {
        parent[rb] = ra;
      } else {
        parent[rb] = ra;
        rank[ra]++;
      }
    }
  }

}
