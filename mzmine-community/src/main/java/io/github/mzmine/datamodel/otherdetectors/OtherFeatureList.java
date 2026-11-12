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
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.util.MemoryMapStorage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A standalone, aligned container of {@link OtherFeature}s (UV/DAD, CAD, …), mirroring the row ↔
 * per-file-feature structure of {@link ModularFeatureList} but without any MS-specific concepts. Each
 * {@link OtherFeatureListRow} groups the per-{@link RawDataFile} {@link OtherFeature}s that share a
 * {@link TraceKey} and were aligned across files. Rows include orphan/single-file rows so a full
 * other-detector report covers peaks that never aligned or correlated to an MS feature.
 * <p>
 * This is produced by the other-detector alignment step and stored as a sub-object of the aligned MS
 * {@link ModularFeatureList} (see {@code ModularFeatureList#getAlignedOtherFeatures()}). It does not
 * share the {@code FeatureList} interface (that interface mixes in MS-specific queries); a thin,
 * DataType-driven exporter reuses the generic export pattern instead.
 */
public class OtherFeatureList {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  private final @Nullable MemoryMapStorage memoryMapStorage;
  private final List<RawDataFile> rawDataFiles;

  /**
   * MS-feature-to-other-detector correlations for this alignment. Owned by the aligned list so that
   * they cannot outlive or reference a different alignment: re-running the alignment produces a new
   * {@link OtherFeatureList} with its own (empty) maps.
   */
  private final MsOtherCorrelationMaps msOtherCorrelationMaps;

  private final ObservableList<OtherFeatureListRow> rows = FXCollections.observableArrayList();
  private final ObservableList<OtherFeatureListRow> rowsUnmodifiableView = FXCollections.unmodifiableObservableList(
      rows);
  // index for fast lookups by row ID (used on hot paths: table cells, export, dashboard)
  private final Map<Integer, OtherFeatureListRow> rowsById = new HashMap<>();

  // DataType schema, split into row-level and per-feature columns (as in ModularFeatureList)
  private final Set<DataType> rowTypes = new LinkedHashSet<>();
  private final Set<DataType> featureTypes = new LinkedHashSet<>();

  // row bindings compute row-level values (e.g. average RT) from the per-file features. Defined once
  // here and applied explicitly via applyRowBindings(row) - a lightweight analogue of the feature
  // list row bindings.
  private final List<OtherRowBinding> rowBindings = new ArrayList<>();

  private final AtomicInteger rowIdCounter = new AtomicInteger(0);

  private @NotNull String name;
  private String dateCreated;

  /**
   * Creates an empty list with empty MS-to-other correlation maps.
   *
   * @param name         the name of this list
   * @param storage      the storage for data of this list, may be null to store in RAM
   * @param rawDataFiles the raw data files (columns) of this list
   */
  public OtherFeatureList(final @NotNull String name, final @Nullable MemoryMapStorage storage,
      final @NotNull List<RawDataFile> rawDataFiles) {
    this(name, storage, rawDataFiles, new MsOtherCorrelationMaps());
  }

  /**
   * Creates an empty list that owns the given correlation maps, e.g., a remapped copy of another
   * list's maps.
   *
   * @param name                   the name of this list
   * @param storage                the storage for data of this list, may be null to store in RAM
   * @param rawDataFiles           the raw data files (columns) of this list
   * @param msOtherCorrelationMaps the correlation maps owned by this list, must not be shared
   */
  public OtherFeatureList(final @NotNull String name, final @Nullable MemoryMapStorage storage,
      final @NotNull List<RawDataFile> rawDataFiles,
      final @NotNull MsOtherCorrelationMaps msOtherCorrelationMaps) {
    this.msOtherCorrelationMaps = msOtherCorrelationMaps;
    this.name = name;
    this.memoryMapStorage = storage;
    // sort data files by name for stable order in export and GUI, matching ModularFeatureList
    final List<RawDataFile> sorted = new ArrayList<>(rawDataFiles);
    sorted.sort(Comparator.comparing(RawDataFile::getName));
    this.rawDataFiles = Collections.unmodifiableList(sorted);
    this.dateCreated = DATE_FORMAT.format(new Date());
    // default row bindings (average RT, RT range); each registers its produced row type
    OtherRowBinding.createDefault().forEach(this::addRowBinding);
  }

  /**
   * @return the MS-feature-to-other-detector correlations for this alignment (keyed by MS row ID).
   */
  @NotNull
  public MsOtherCorrelationMaps getMsOtherCorrelationMaps() {
    return msOtherCorrelationMaps;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public void setName(final @NotNull String name) {
    this.name = name;
  }

  @Nullable
  public MemoryMapStorage getMemoryMapStorage() {
    return memoryMapStorage;
  }

  @NotNull
  public List<RawDataFile> getRawDataFiles() {
    return rawDataFiles;
  }

  public int getNumberOfRawDataFiles() {
    return rawDataFiles.size();
  }

  @NotNull
  public ObservableList<OtherFeatureListRow> getRows() {
    return rowsUnmodifiableView;
  }

  public int getNumberOfRows() {
    return rows.size();
  }

  public Stream<OtherFeatureListRow> stream() {
    return rows.stream();
  }

  /**
   * Adds a row and raises the row ID counter to at least the row's ID, so rows added with existing
   * IDs (project load, copies) never collide with rows created later by
   * {@link #createRow(TraceKey)}.
   *
   * @param row the row to add, its ID must be unique in this list
   */
  public void addRow(final @NotNull OtherFeatureListRow row) {
    final OtherFeatureListRow previous = rowsById.putIfAbsent(row.getID(), row);
    if (previous != null) {
      throw new IllegalArgumentException(
          "Row ID %d already exists in other feature list %s".formatted(row.getID(), name));
    }
    rows.add(row);
    rowIdCounter.accumulateAndGet(row.getID(), Math::max);
  }

  /**
   * Removes a row and all MS-to-other correlations that link to it. The row ID counter is not
   * lowered, so the ID of a removed row is never reused.
   *
   * @param row the row to remove
   * @return true if the row was part of this list
   */
  public boolean removeRow(final @NotNull OtherFeatureListRow row) {
    if (!rowsById.remove(row.getID(), row)) {
      return false;
    }
    rows.remove(row);
    msOtherCorrelationMaps.removeOtherRow(row.getID());
    return true;
  }

  /**
   * @param id the row ID
   * @return the row with this ID or null if there is no such row
   */
  @Nullable
  public OtherFeatureListRow findRowByID(final int id) {
    return rowsById.get(id);
  }

  /**
   * @return the next unused row ID. Row IDs are stable within this list and used to reference rows
   * from the MS-to-other correlation map.
   */
  public int nextRowId() {
    return rowIdCounter.incrementAndGet();
  }

  /**
   * @return the highest row ID handed out or added so far. Persisted with the list.
   */
  public int getRowIdCounter() {
    return rowIdCounter.get();
  }

  /**
   * Restores the row ID counter, e.g., on project load. The counter never drops below the highest
   * row ID already in this list.
   *
   * @param counter the highest row ID handed out so far
   */
  public void setRowIdCounter(final int counter) {
    rowIdCounter.accumulateAndGet(counter, Math::max);
  }

  /**
   * Convenience: create (but do not add) a new row with a fresh ID for the given trace key.
   */
  @NotNull
  public OtherFeatureListRow createRow(final @NotNull TraceKey traceKey) {
    return new OtherFeatureListRow(nextRowId(), traceKey);
  }

  public void addRowType(final @NotNull DataType<?>... types) {
    Collections.addAll(rowTypes, types);
  }

  public void addFeatureType(final @NotNull DataType<?>... types) {
    Collections.addAll(featureTypes, types);
  }

  /**
   * Registers a row binding and its produced row type (kept in the row-type index).
   */
  public void addRowBinding(final @NotNull OtherRowBinding binding) {
    rowBindings.add(binding);
    rowTypes.add(binding.boundType());
  }

  /**
   * Applies all row bindings to the given row, recomputing its row-level values (e.g. average RT)
   * from its per-file features. Call after a row's features have been set/changed.
   */
  public void applyRowBindings(final @NotNull OtherFeatureListRow row) {
    for (final OtherRowBinding binding : rowBindings) {
      binding.apply(row);
    }
  }

  @NotNull
  public Set<DataType> getRowTypes() {
    return Collections.unmodifiableSet(rowTypes);
  }

  @NotNull
  public Set<DataType> getFeatureTypes() {
    return Collections.unmodifiableSet(featureTypes);
  }

  public boolean hasRowType(final @NotNull Class<? extends DataType> type) {
    return rowTypes.stream().anyMatch(type::isInstance);
  }

  public boolean hasFeatureType(final @NotNull Class<? extends DataType> type) {
    return featureTypes.stream().anyMatch(type::isInstance);
  }

  @NotNull
  public String getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(final @NotNull String dateCreated) {
    this.dateCreated = dateCreated;
  }

  public boolean isEmpty() {
    return rows.isEmpty();
  }

  @Override
  public String toString() {
    return name;
  }
}
