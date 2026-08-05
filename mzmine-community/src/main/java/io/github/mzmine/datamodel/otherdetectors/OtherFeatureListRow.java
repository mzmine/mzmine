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
 * the generic {@code get(DataType)} API. The per-file {@link OtherFeature}s continue to live in their
 * {@link OtherTimeSeriesData} — this row only references them, it does not own them.
 */
public class OtherFeatureListRow extends ModularDataModelMap {

  private final Map<DataType, Object> map = new LinkedHashMap<>();
  private final Map<RawDataFile, OtherFeature> features = new LinkedHashMap<>();

  private final int id;
  private final @NotNull TraceKey traceKey;

  public OtherFeatureListRow(final int id, final @NotNull TraceKey traceKey) {
    this.id = id;
    this.traceKey = traceKey;
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
   * Places the given feature in this row's cell for its raw data file. The feature's own
   * {@link TraceKey} must equal this row's key (the alignment gate).
   *
   * @throws IllegalArgumentException if the feature's {@link TraceKey} does not match this row
   */
  public void addFeature(final @NotNull OtherFeature feature) {
    final TraceKey featureKey = TraceKey.of(feature);
    if (!traceKey.equals(featureKey)) {
      throw new IllegalArgumentException(
          "Cannot add feature with trace key %s to row of trace key %s".formatted(featureKey,
              traceKey));
    }
    features.put(feature.getRawDataFile(), feature);
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
