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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularDataModelMap;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One aligned row of an {@link OtherFeatureList}. Groups the per-{@link RawDataFile}
 * {@link OtherFeature}s that were aligned across files. Every row carries exactly one
 * {@link TraceKey} (the "m/z equivalent"): it is both the row's identity together with the aligned
 * retention time, and the gate that decides which {@link OtherFeature}s may occupy the row's per-file
 * cells (a cell's feature must have a matching {@link TraceKey}).
 * <p>
 * The row is a {@link ModularDataModel} (map-based, like {@link OtherFeatureImpl}) so that
 * DataType-driven machinery (export, table) can read row-level values (e.g. {@link RTType}) through
 * the generic {@code get(DataType)} API. The row owns its per-file {@link OtherFeature}s: they are
 * copies of the processed features in {@link OtherTimeSeriesData}, so re-processing the raw data
 * does not change the aligned list.
 * <p>
 * Each row belongs to exactly one {@link OtherFeatureList}. Setting or removing a feature registers
 * the feature types in that list and re-applies its row bindings. Not thread-safe.
 */
public class OtherFeatureListRow extends ModularDataModelMap {

  private final Map<DataType, Object> map = new LinkedHashMap<>();
  private final Map<RawDataFile, OtherFeature> features = new LinkedHashMap<>();

  private final @NotNull OtherFeatureList otherFeatureList;
  private final int id;
  private final @NotNull TraceKey traceKey;

  /**
   * Creates an empty row. Use {@link OtherFeatureList#createRow(TraceKey)} to get a fresh ID. The
   * row is not added to the list.
   *
   * @param otherFeatureList the list this row belongs to
   * @param id               the row ID, unique within the list
   * @param traceKey         the trace key of all features of this row
   */
  public OtherFeatureListRow(final @NotNull OtherFeatureList otherFeatureList, final int id,
      final @NotNull TraceKey traceKey) {
    this.otherFeatureList = otherFeatureList;
    this.id = id;
    this.traceKey = traceKey;
  }

  /**
   * @return the list this row belongs to
   */
  @NotNull
  public OtherFeatureList getOtherFeatureList() {
    return otherFeatureList;
  }

  @Override
  public Map<DataType, Object> getMap() {
    return map;
  }

  public int getID() {
    return id;
  }

  @NotNull
  public TraceKey getTraceKey() {
    return traceKey;
  }

  /**
   * Places the given feature in this row's cell for its raw data file and replaces the previous
   * feature of that file. The feature's own {@link TraceKey} must equal this row's key (the
   * alignment gate). The feature types are registered in the {@link OtherFeatureList} and the row
   * bindings are re-applied. The feature is stored as is, pass a copy if it is also used elsewhere,
   * e.g., in {@link OtherTimeSeriesData}.
   *
   * @param feature the feature to set
   * @return the replaced feature of the same raw data file or null if the cell was empty
   * @throws IllegalArgumentException if the feature's {@link TraceKey} does not match this row
   */
  @Nullable
  public OtherFeature setFeature(final @NotNull OtherFeature feature) {
    return setFeature(feature, true);
  }

  /**
   * Same as {@link #setFeature(OtherFeature)}, but the row bindings may be skipped when many
   * features are set at once, e.g., during alignment or project load. Call
   * {@link OtherFeatureList#applyRowBindings(OtherFeatureListRow)} once after the last feature.
   *
   * @param feature           the feature to set
   * @param updateRowBindings true to re-apply the row bindings
   * @return the replaced feature of the same raw data file or null if the cell was empty
   * @throws IllegalArgumentException if the feature's {@link TraceKey} does not match this row
   */
  @Nullable
  public OtherFeature setFeature(final @NotNull OtherFeature feature,
      final boolean updateRowBindings) {
    final TraceKey featureKey = TraceKey.of(feature);
    if (!traceKey.equals(featureKey)) {
      throw new IllegalArgumentException(
          "Cannot add feature with trace key %s to row of trace key %s".formatted(featureKey,
              traceKey));
    }
    final OtherFeature previous = features.put(feature.getRawDataFile(), feature);
    otherFeatureList.addFeatureType(feature.getTypes().toArray(DataType[]::new));
    if (updateRowBindings) {
      otherFeatureList.applyRowBindings(this);
    }
    return previous;
  }

  /**
   * Removes the feature of a raw data file from this row and re-applies the row bindings. The row
   * stays in its list even if it is empty afterwards.
   *
   * @param file the raw data file
   * @return the removed feature or null if the row had no feature for this file
   */
  @Nullable
  public OtherFeature removeFeature(final @NotNull RawDataFile file) {
    final OtherFeature removed = features.remove(file);
    if (removed != null) {
      otherFeatureList.applyRowBindings(this);
    }
    return removed;
  }

  @Nullable
  public OtherFeature getFeature(final @NotNull RawDataFile file) {
    return features.get(file);
  }

  public boolean hasFeature(final @NotNull RawDataFile file) {
    return features.containsKey(file);
  }

  @NotNull
  public Collection<OtherFeature> getFeatures() {
    return features.values();
  }

  @NotNull
  public Collection<RawDataFile> getRawDataFiles() {
    return features.keySet();
  }

  public int getNumberOfFeatures() {
    return features.size();
  }

  public Stream<OtherFeature> streamFeatures() {
    return features.values().stream();
  }

  /**
   * @return the aligned retention time of this row, or null if not set.
   */
  @Nullable
  public Float getRT() {
    return get(RTType.class);
  }

  /**
   * @return the aligned retention time range of this row, or null if not set.
   */
  @Nullable
  public Range<Float> getRtRange() {
    return get(RTRangeType.class);
  }

  @Override
  public String toString() {
    return "OtherFeatureListRow{id=%d, traceKey=%s, features=%d}".formatted(id, traceKey,
        features.size());
  }
}
