/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import static io.github.mzmine.datamodel.otherdetectors.OtherDataFileImpl.DEFAULT_UNIT;

import io.github.mzmine.datamodel.features.types.otherdectectors.RawTraceType;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OtherTimeSeriesDataImpl implements OtherTimeSeriesData {

  private static final Logger logger = Logger.getLogger(OtherTimeSeriesDataImpl.class.getName());

  private final CloseableReentrantReadWriteLock writeLock = new CloseableReentrantReadWriteLock();

  private final OtherDataFile otherDataFile;
  private final List<OtherFeature> rawTraces = new ArrayList<>();
  private final List<OtherFeature> processedFeatures = new ArrayList<>();

  public @NotNull ChromatogramType chromatogramType = ChromatogramType.UNKNOWN;
  private @NotNull String timeSeriesDomainLabel = "Retention time";
  private @NotNull String timeSeriesDomainUnit = "min";
  private @NotNull String timeSeriesRangeLabel = DEFAULT_UNIT;
  private @NotNull String timeSeriesRangeUnit = DEFAULT_UNIT;

  public OtherTimeSeriesDataImpl(OtherDataFile otherDataFile) {
    this.otherDataFile = otherDataFile;
  }

  @Override
  public @NotNull OtherFeature getRawTrace(int index) {
    try (var _ = writeLock.lockWrite()) {
      return rawTraces.get(index);
    }
  }

  @Override
  public @NotNull List<@NotNull OtherFeature> getRawTraces() {
    return List.copyOf(rawTraces);
  }

  public void addRawTrace(@NotNull OtherFeature series) {
    try (var _ = writeLock.lockWrite()) {
      this.rawTraces.add(series);
    }
  }

  @Override
  public OtherDataFile getOtherDataFile() {
    return otherDataFile;
  }

  @Override
  public @NotNull String getTimeSeriesDomainLabel() {
    return timeSeriesDomainLabel;
  }

  public void setTimeSeriesDomainLabel(@Nullable String timeSeriesDomainLabel) {
    this.timeSeriesDomainLabel = Objects.requireNonNullElse(timeSeriesDomainLabel, DEFAULT_UNIT);
  }

  @Override
  public @NotNull String getTimeSeriesDomainUnit() {
    return timeSeriesRangeLabel;
  }

  public void setTimeSeriesDomainUnit(@Nullable String timeSeriesDomainUnit) {
    this.timeSeriesDomainUnit = Objects.requireNonNullElse(timeSeriesDomainUnit, DEFAULT_UNIT);
  }

  @Override
  public @NotNull String getTimeSeriesRangeLabel() {
    return timeSeriesRangeLabel;
  }

  public void setTimeSeriesRangeLabel(@Nullable String timeSeriesRangeLabel) {
    if (!DEFAULT_UNIT.equals(this.timeSeriesRangeLabel) && timeSeriesRangeLabel != null
        && !this.timeSeriesRangeLabel.equals(timeSeriesRangeLabel)) {
      logger.severe(() -> (
          "Range axis labels of time series in file %s for chromatogram type %s do not have the "
              + "same label (old: %s, new: %s)").formatted(getOtherDataFile().getDescription(),
          getChromatogramType(), this.timeSeriesRangeLabel, timeSeriesRangeLabel));
    }
    this.timeSeriesRangeLabel = Objects.requireNonNullElse(timeSeriesRangeLabel, this.timeSeriesRangeLabel);
  }

  @Override
  public @NotNull String getTimeSeriesRangeUnit() {
    return timeSeriesRangeUnit;
  }

  public void setTimeSeriesRangeUnit(@Nullable String timeSeriesRangeUnit) {
    this.timeSeriesRangeUnit = Objects.requireNonNullElse(timeSeriesRangeUnit, DEFAULT_UNIT);
  }

  @Override
  public @NotNull ChromatogramType getChromatogramType() {
    return chromatogramType;
  }

  public void setChromatogramType(@Nullable ChromatogramType chromatogramType) {
    this.chromatogramType = chromatogramType;
  }

  @Override
  public List<OtherFeature> getProcessedFeatures() {
    return processedFeatures;
  }

  @Override
  @NotNull
  public List<OtherFeature> getProcessedFeaturesForTrace(OtherFeature rawTrace) {
    try (var _ = writeLock.lockRead()) {
      return processedFeatures.stream()
          .filter(f -> Objects.equals(f.get(RawTraceType.class), rawTrace)).toList();
    }
  }

  @Override
  public void replaceProcessedFeaturesForTrace(OtherFeature rawTrace,
      @NotNull List<OtherFeature> newFeatures) {
    if (newFeatures.stream().anyMatch(f -> f.get(RawTraceType.class) == null)) {
      throw new IllegalStateException("RawTraceType is null for some new features.");
    }
    if (!newFeatures.stream().allMatch(f -> Objects.equals(f.get(RawTraceType.class), rawTrace))) {
      throw new IllegalStateException("Not all features belong to the required trace");
    }

    try (var _ = writeLock.lockWrite()) {
      final List<OtherFeature> currentFeatures = getProcessedFeaturesForTrace(rawTrace);
      processedFeatures.removeAll(currentFeatures);
      processedFeatures.addAll(newFeatures);
    }
  }

  @Override
  public void addProcessedFeature(@NotNull OtherFeature newFeature) {
    final OtherFeature rawTrace = newFeature.get(RawTraceType.class);
    if (rawTrace == null) {
      throw new IllegalStateException("The new feature does not have an associated raw trace.");
    }

    if (!rawTraces.stream().anyMatch(f -> f.equals(rawTrace))) {
      throw new IllegalStateException(
          "The newly added feature does not belong to this time series data.");
    }

    try (var _ = writeLock.lockWrite()) {
      processedFeatures.add(newFeature);
    }
  }
}
