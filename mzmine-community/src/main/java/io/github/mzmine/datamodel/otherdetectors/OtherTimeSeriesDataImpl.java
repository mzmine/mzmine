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
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OtherTimeSeriesDataImpl implements OtherTimeSeriesData {

  private final OtherDataFile otherDataFile;
  private final List<OtherFeature> rawTraces = new ArrayList<>();
  private final List<OtherFeature> processedFeatures = new ArrayList<>();

  public @Nullable ChromatogramType chromatogramType = ChromatogramType.UNKNOWN;
  private @Nullable String timeSeriesDomainLabel = "Retention time";
  private @Nullable String timeSeriesDomainUnit = "min";
  private @Nullable String timeSeriesRangeLabel = DEFAULT_UNIT;
  private @Nullable String timeSeriesRangeUnit = DEFAULT_UNIT;

  public OtherTimeSeriesDataImpl(OtherDataFile otherDataFile) {
    this.otherDataFile = otherDataFile;
  }

  @Override
  public @NotNull OtherFeature getRawTrace(int index) {
    return rawTraces.get(index);
  }

  @Override
  public @NotNull List<@NotNull OtherFeature> getRawTraces() {
    return rawTraces;
  }

  public void addRawTrace(@NotNull OtherFeature series) {
    this.rawTraces.add(series);
  }

  @Override
  public OtherDataFile getOtherDataFile() {
    return otherDataFile;
  }

  @Override
  public @Nullable String getTimeSeriesDomainLabel() {
    return timeSeriesDomainLabel;
  }

  public void setTimeSeriesDomainLabel(@Nullable String timeSeriesDomainLabel) {
    this.timeSeriesDomainLabel = timeSeriesDomainLabel;
  }

  @Override
  public @Nullable String getTimeSeriesDomainUnit() {
    return timeSeriesDomainUnit;
  }

  public void setTimeSeriesDomainUnit(@Nullable String timeSeriesDomainUnit) {
    this.timeSeriesDomainUnit = timeSeriesDomainUnit;
  }

  @Override
  public @Nullable String getTimeSeriesRangeLabel() {
    return timeSeriesRangeLabel;
  }

  public void setTimeSeriesRangeLabel(@Nullable String timeSeriesRangeLabel) {
    this.timeSeriesRangeLabel = timeSeriesRangeLabel;
  }

  @Override
  public @Nullable String getTimeSeriesRangeUnit() {
    return timeSeriesRangeUnit;
  }

  public void setTimeSeriesRangeUnit(@Nullable String timeSeriesRangeUnit) {
    this.timeSeriesRangeUnit = timeSeriesRangeUnit;
  }

  @Override
  public @Nullable ChromatogramType getChromatogramType() {
    return chromatogramType;
  }

  @Override
  public List<OtherFeature> getProcessedFeatures() {
    return processedFeatures;
  }

  public void setChromatogramType(@Nullable ChromatogramType chromatogramType) {
    this.chromatogramType = chromatogramType;
  }

  @Override
  @NotNull
  public List<OtherFeature> getProcessedFeaturesForTrace(OtherFeature rawTrace) {
    return processedFeatures.stream().filter(f -> f.get(RawTraceType.class).equals(rawTrace))
        .toList();
  }

  @Override
  public void replaceProcessedFeaturesForTrace(OtherFeature rawTrace,
      @NotNull List<OtherFeature> newFeatures) {
    if (newFeatures.stream().anyMatch(f -> f.get(RawTraceType.class) == null)) {
      throw new IllegalStateException("RawTraceType is null for some new features.");
    }
    if (!newFeatures.stream().allMatch(f -> f.get(RawTraceType.class).equals(rawTrace))) {
      throw new IllegalStateException("Not all features belong to the requiredTrace");
    }

    final List<OtherFeature> currentFeatures = getProcessedFeaturesForTrace(rawTrace);
    processedFeatures.removeAll(currentFeatures);
    processedFeatures.addAll(newFeatures);
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

    processedFeatures.add(newFeature);
  }
}
