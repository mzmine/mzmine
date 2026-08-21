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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.RawDataFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the MS-feature-to-other-detector correlations for one {@link OtherFeatureList}, keyed by ID
 * only (no embedded {@link OtherFeature}s). This is the single source of truth for the "correlated
 * traces" columns, replacing the former per-feature embedded storage. It is owned by the aligned
 * {@link OtherFeatureList}, so its other-row IDs always belong to that alignment.
 * <p>
 * Correlations are recorded at row level (MS row -> list of correlated other-rows) with per-file
 * truth ({@link OtherCorrelationLink#perFile()}).
 */
public class MsOtherCorrelationMaps {

  /**
   * MS row id -> correlations
   */
  private final Map<Integer, List<OtherCorrelationLink>> byMsRow = new ConcurrentHashMap<>();
  /// reverse index: other-row ID -> set of MS row IDs that correlate to it
  private final Map<Integer, Set<Integer>> otherRowToMsRows = new ConcurrentHashMap<>();

  /**
   * @return the correlated other-rows for the given MS row (empty if none). The returned list is an
   * unmodifiable snapshot.
   */
  @NotNull
  public List<OtherCorrelationLink> getCorrelations(final int msRowId) {
    return byMsRow.getOrDefault(msRowId, List.of());
  }

  /**
   * @return an unmodifiable view of all correlations keyed by MS row ID (for persistence / export).
   */
  @NotNull
  public Map<Integer, List<OtherCorrelationLink>> getAllCorrelations() {
    return Collections.unmodifiableMap(byMsRow);
  }

  /**
   * Replaces the correlations for one MS row. Passing an empty list clears the entry.
   */
  public void setCorrelations(final int msRowId, final @NotNull List<OtherCorrelationLink> links) {
    // drop previous reverse-index entries for this MS row
    removeFromReverseIndex(msRowId);
    if (links.isEmpty()) {
      byMsRow.remove(msRowId);
      return;
    }
    final List<OtherCorrelationLink> copy = List.copyOf(links);
    byMsRow.put(msRowId, copy);
    for (final OtherCorrelationLink link : copy) {
      otherRowToMsRows.computeIfAbsent(link.otherRowId(), k -> new CopyOnWriteArraySet<>())
          .add(msRowId);
    }
  }

  /**
   * @return the MS row IDs that correlate to the given aligned other-row (empty if none).
   */
  @NotNull
  public Set<Integer> getMsRowsForOtherRow(final int otherRowId) {
    return otherRowToMsRows.getOrDefault(otherRowId, Set.of());
  }

  /**
   * Marks the given other-row as the preferred correlation for the MS row by moving it to the front
   * of the row's correlation list (the first entry is the preferred trace). No-op if the other-row is
   * not correlated or is already first.
   */
  public void setPreferredCorrelation(final int msRowId, final int otherRowId) {
    final List<OtherCorrelationLink> current = new ArrayList<>(getCorrelations(msRowId));
    int idx = -1;
    for (int i = 0; i < current.size(); i++) {
      if (current.get(i).otherRowId() == otherRowId) {
        idx = i;
        break;
      }
    }
    if (idx <= 0) {
      return; // not found or already preferred
    }
    current.addFirst(current.remove(idx));
    setCorrelations(msRowId, current);
  }

  /**
   * Adds or updates a manual correlation between an MS row and an aligned other-row for one file
   * (used by the correlation dashboard).
   */
  public void addManualCorrelation(final int msRowId, final int otherRowId,
      final @NotNull RawDataFile file) {
    final List<OtherCorrelationLink> current = new ArrayList<>(getCorrelations(msRowId));
    int idx = -1;
    for (int i = 0; i < current.size(); i++) {
      if (current.get(i).otherRowId() == otherRowId) {
        idx = i;
        break;
      }
    }
    if (idx >= 0) {
      // update the existing link in place so the order (and preferred/first entry) is preserved
      final Map<RawDataFile, PerFileCorrelation> perFile = new LinkedHashMap<>(
          current.get(idx).perFile());
      perFile.put(file, new PerFileCorrelation(MsOtherCorrelationType.MANUAL, null));
      current.set(idx, new OtherCorrelationLink(otherRowId, perFile));
    } else {
      // a newly correlated trace is appended (does not disturb existing preference)
      final Map<RawDataFile, PerFileCorrelation> perFile = new LinkedHashMap<>();
      perFile.put(file, new PerFileCorrelation(MsOtherCorrelationType.MANUAL, null));
      current.add(new OtherCorrelationLink(otherRowId, perFile));
    }
    setCorrelations(msRowId, current);
  }

  /**
   * Removes the correlation between an MS row and an aligned other-row for one file. If the link has
   * no remaining files it is dropped entirely.
   */
  public void removeCorrelation(final int msRowId, final int otherRowId,
      final @NotNull RawDataFile file) {
    final List<OtherCorrelationLink> current = new ArrayList<>(getCorrelations(msRowId));
    final OtherCorrelationLink existing = current.stream()
        .filter(l -> l.otherRowId() == otherRowId).findFirst().orElse(null);
    if (existing == null) {
      return;
    }
    current.remove(existing);
    final Map<RawDataFile, PerFileCorrelation> perFile = new LinkedHashMap<>(existing.perFile());
    perFile.remove(file);
    if (!perFile.isEmpty()) {
      current.add(new OtherCorrelationLink(otherRowId, perFile));
    }
    setCorrelations(msRowId, current);
  }

  /**
   * Removes all correlations for one MS row.
   */
  public void clear(final int msRowId) {
    removeFromReverseIndex(msRowId);
    byMsRow.remove(msRowId);
  }

  /**
   * Removes all correlations (e.g. before recomputing them).
   */
  public void clearAll() {
    byMsRow.clear();
    otherRowToMsRows.clear();
  }

  public boolean isEmpty() {
    return byMsRow.isEmpty();
  }

  private void removeFromReverseIndex(final int msRowId) {
    final List<OtherCorrelationLink> previous = byMsRow.get(msRowId);
    if (previous == null) {
      return;
    }
    for (final OtherCorrelationLink link : previous) {
      final Set<Integer> msRows = otherRowToMsRows.get(link.otherRowId());
      if (msRows != null) {
        msRows.remove(msRowId);
        if (msRows.isEmpty()) {
          otherRowToMsRows.remove(link.otherRowId());
        }
      }
    }
  }
}
