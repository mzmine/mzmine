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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the selected scans that were used to build the chromatograms/features of a
 * {@link FeatureList} and are reused for reintegration (gap filling, smoothing, baseline
 * correction, ...).
 * <p>
 * The scans are organized as {@code ScanSelection -> (RawDataFile -> scans)}. The outer
 * {@link ScanSelection} key allows the same {@link RawDataFile} to carry multiple independent scan
 * lists - for example the positive and negative polarity scans of a polarity switching experiment.
 * Each {@link FeatureListRow} records which {@link ScanSelection} it was derived from (see
 * {@link FeatureListRow#getScanSelection()}), so a module can fetch the matching scan list for any
 * file via {@link #getScans(ScanSelection, RawDataFile)}.
 * <p>
 * We expect a low number of distinct scan selections (usually exactly one). The insertion order of
 * the distinct selections is preserved and exposed as a stable index ({@link #indexOf} /
 * {@link #getSelectionByIndex}) used by the project save/load to reference a selection compactly.
 * <p>
 * All access is synchronized. The maps are small and contention is low (a feature list is typically
 * built by a single task and only read afterwards).
 */
public class FeatureListScans {

  // insertion-ordered so distinct selections get stable indices for save/load
  private final Map<ScanSelection, Map<RawDataFile, List<? extends Scan>>> scansBySelection = new LinkedHashMap<>();

  /**
   * Registers the scans that file contributed for the given scan selection. A {@code null} scans
   * argument removes the entry.
   *
   * @param selection the scan filter that selected these scans
   * @param file      the data file the scans originate from
   * @param scans     the selected scans (Frames for ion mobility data) or null to remove
   */
  public synchronized void setScans(@NotNull final ScanSelection selection,
      @NotNull final RawDataFile file, @Nullable final List<? extends Scan> scans) {
    if (scans == null) {
      final Map<RawDataFile, List<? extends Scan>> inner = scansBySelection.get(selection);
      if (inner != null) {
        inner.remove(file);
        if (inner.isEmpty()) {
          scansBySelection.remove(selection);
        }
      }
      return;
    }
    scansBySelection.computeIfAbsent(selection, _ -> new LinkedHashMap<>()).put(file, scans);
  }

  /**
   * Resolves the scan list for a file given the scan selection a feature/row was derived from. Uses
   * a graceful fallback so that all single-selection feature lists keep working unchanged:
   * <ol>
   *   <li>exact ({@code selection}, {@code file}) match</li>
   *   <li>if the file has exactly one selection, use it</li>
   *   <li>if {@code selection} is given, the single file selection with the same polarity</li>
   *   <li>otherwise null</li>
   * </ol>
   *
   * @param selection the scan selection of the row/feature, may be null if unknown
   * @param file      the data file
   * @return the matching scans or null if it cannot be resolved unambiguously
   */
  public synchronized @Nullable List<? extends Scan> getScans(
      @Nullable final ScanSelection selection, @NotNull final RawDataFile file) {
    if (selection != null) {
      final Map<RawDataFile, List<? extends Scan>> inner = scansBySelection.get(selection);
      if (inner != null && inner.containsKey(file)) {
        return inner.get(file);
      }
    }

    // gather all selections that have scans for this file
    final List<Entry<ScanSelection, List<? extends Scan>>> forFile = new ArrayList<>();
    for (final Entry<ScanSelection, Map<RawDataFile, List<? extends Scan>>> e : scansBySelection.entrySet()) {
      final List<? extends Scan> scans = e.getValue().get(file);
      if (scans != null) {
        forFile.add(Map.entry(e.getKey(), scans));
      }
    }

    if (forFile.isEmpty()) {
      return null;
    }
    if (forFile.size() == 1) {
      // single selection for this file - the common case (no polarity switching)
      return forFile.getFirst().getValue();
    }

    // multiple selections for this file - disambiguate by polarity of the requested selection
    if (selection != null) {
      final PolarityType polarity = selection.getPolarity();
      final List<Entry<ScanSelection, List<? extends Scan>>> samePolarity = forFile.stream()
          .filter(e -> e.getKey().getPolarity() == polarity).toList();
      if (samePolarity.size() == 1) {
        return samePolarity.getFirst().getValue();
      }
    }
    return null;
  }

  /**
   * @param file the data file
   * @return the scans for the file when it has exactly one scan selection, otherwise null. Use this
   * only when no row/feature context (and therefore no {@link ScanSelection}) is available.
   */
  public synchronized @Nullable List<? extends Scan> getScansForFile(
      @NotNull final RawDataFile file) {
    List<? extends Scan> found = null;
    for (final Map<RawDataFile, List<? extends Scan>> inner : scansBySelection.values()) {
      final List<? extends Scan> scans = inner.get(file);
      if (scans != null) {
        if (found != null) {
          // more than one selection for this file -> ambiguous
          return null;
        }
        found = scans;
      }
    }
    return found;
  }

  /**
   * Returns all scans of a file merged across every scan selection, sorted (by scan natural order)
   * and de-duplicated.
   * <p>
   * Use this <b>only</b> for genuinely file-level operations that cannot attribute work to a single
   * scan selection - e.g. a per-file normalization metric or a common RT axis shared by all rows.
   * For polarity switching data this mixes the polarities. Where a row/feature context is
   * available, prefer {@link #getScans(ScanSelection, RawDataFile)} with the row's selection
   * instead.
   *
   * @param file the data file
   * @return the merged, sorted, distinct scans (empty if none)
   */
  public synchronized @NotNull List<? extends Scan> getAllScansForFile(
      @NotNull final RawDataFile file) {
    final TreeSet<Scan> merged = new TreeSet<>();
    for (final Map<RawDataFile, List<? extends Scan>> inner : scansBySelection.values()) {
      final List<? extends Scan> scans = inner.get(file);
      if (scans != null) {
        merged.addAll(scans);
      }
    }
    return new ArrayList<>(merged);
  }

  /**
   * @param file the data file
   * @return true if any scan selection has scans registered for the file
   */
  public synchronized boolean hasScansForFile(@NotNull final RawDataFile file) {
    return scansBySelection.values().stream().anyMatch(inner -> inner.containsKey(file));
  }

  /**
   * @param file the data file
   * @return all scan selections registered for the file (empty if none)
   */
  public synchronized @NotNull List<ScanSelection> getScanSelectionsForFile(
      @NotNull final RawDataFile file) {
    return scansBySelection.entrySet().stream().filter(e -> e.getValue().containsKey(file))
        .map(Entry::getKey).toList();
  }

  /**
   * @return the distinct scan selections in stable (insertion) order
   */
  public synchronized @NotNull List<ScanSelection> getSelections() {
    return new ArrayList<>(scansBySelection.keySet());
  }

  /**
   * @return all raw data files that have scans registered under any selection
   */
  public synchronized @NotNull Set<RawDataFile> getRawDataFiles() {
    final Set<RawDataFile> files = new LinkedHashSet<>();
    for (final Map<RawDataFile, List<? extends Scan>> inner : scansBySelection.values()) {
      files.addAll(inner.keySet());
    }
    return files;
  }

  /**
   * Returns the canonical instance equal to the given selection, registering it if it is new. Used
   * so that all rows referencing the same logical selection share one object and match the map
   * key.
   *
   * @param selection the selection to intern
   * @return the canonical instance
   */
  public synchronized @NotNull ScanSelection intern(@NotNull final ScanSelection selection) {
    for (final ScanSelection existing : scansBySelection.keySet()) {
      if (existing.equals(selection)) {
        return existing;
      }
    }
    // register with an empty file map so the selection has a stable index even before scans are set
    scansBySelection.computeIfAbsent(selection, _ -> new LinkedHashMap<>());
    return selection;
  }

  /**
   * @param selection the selection
   * @return the stable index of the selection, or -1 if not registered
   */
  public synchronized int indexOf(@Nullable final ScanSelection selection) {
    if (selection == null) {
      return -1;
    }
    int i = 0;
    for (final ScanSelection existing : scansBySelection.keySet()) {
      if (existing.equals(selection)) {
        return i;
      }
      i++;
    }
    return -1;
  }

  /**
   * @param index the stable index
   * @return the selection at that index, or null if out of range
   */
  public synchronized @Nullable ScanSelection getSelectionByIndex(final int index) {
    if (index < 0) {
      return null;
    }
    int i = 0;
    for (final ScanSelection existing : scansBySelection.keySet()) {
      if (i == index) {
        return existing;
      }
      i++;
    }
    return null;
  }

  /**
   * @param selection the selection
   * @param file      the data file
   * @return the scans under the exact ({@code selection}, {@code file}) key without any fallback,
   * or null
   */
  public synchronized @Nullable List<? extends Scan> getScansExact(
      @NotNull final ScanSelection selection, @NotNull final RawDataFile file) {
    final Map<RawDataFile, List<? extends Scan>> inner = scansBySelection.get(selection);
    return inner == null ? null : inner.get(file);
  }

  /**
   * Size of the longest registered scan list, used to size reusable data access buffers.
   *
   * @param file if not null, only consider scan lists of this file; otherwise all files
   * @return the largest scan list size or 0 if none
   */
  public synchronized int largestScanCount(@Nullable final RawDataFile file) {
    int max = 0;
    for (final Map<RawDataFile, List<? extends Scan>> inner : scansBySelection.values()) {
      if (file != null) {
        final List<? extends Scan> scans = inner.get(file);
        if (scans != null && scans.size() > max) {
          max = scans.size();
        }
      } else {
        for (final List<? extends Scan> scans : inner.values()) {
          if (scans.size() > max) {
            max = scans.size();
          }
        }
      }
    }
    return max;
  }

  /**
   * Replaces a raw data file in all selections, optionally transforming the scan lists. Used during
   * project load to swap cached files for their real counterparts.
   *
   * @param oldFile the file to replace
   * @param newFile the new file
   * @param mapper  transforms the old scan list into the new one (e.g. cached frames to real
   *                frames)
   */
  public synchronized void replaceRawDataFile(@NotNull final RawDataFile oldFile,
      @NotNull final RawDataFile newFile,
      @NotNull final Function<List<? extends Scan>, List<? extends Scan>> mapper) {
    for (final Map<RawDataFile, List<? extends Scan>> inner : scansBySelection.values()) {
      final List<? extends Scan> scans = inner.remove(oldFile);
      if (scans != null) {
        inner.put(newFile, mapper.apply(scans));
      }
    }
  }

  /**
   * Copies all entries from another instance into this one (interning selections). Used when
   * copying or merging feature lists.
   *
   * @param other the source
   */
  public synchronized void putAll(@NotNull final FeatureListScans other) {
    final List<ScanSelection> selections = other.getSelections();
    for (final ScanSelection selection : selections) {
      final Map<RawDataFile, List<? extends Scan>> innerCopy = other.copyInner(selection);
      final ScanSelection interned = intern(selection);
      for (final Entry<RawDataFile, List<? extends Scan>> e : innerCopy.entrySet()) {
        setScans(interned, e.getKey(), e.getValue());
      }
    }
  }

  private synchronized @NotNull Map<RawDataFile, List<? extends Scan>> copyInner(
      @NotNull final ScanSelection selection) {
    final Map<RawDataFile, List<? extends Scan>> inner = scansBySelection.get(selection);
    return inner == null ? Map.of() : new LinkedHashMap<>(inner);
  }
}
