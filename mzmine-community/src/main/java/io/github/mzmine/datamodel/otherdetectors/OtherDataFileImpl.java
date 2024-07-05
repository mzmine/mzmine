/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OtherDataFileImpl implements OtherDataFile {

  public static final String DEFAULT_UNIT = "N/A";

  private final RawDataFile rawDataFile;
  private final List<IntensityTimeSeries> timeSeries = new ArrayList<>();
  private final List<OtherSpectrum> spectra = new ArrayList<>();

  private @NotNull String description = "Unknown file";
  private @Nullable String spectraDomainLabel = DEFAULT_UNIT;
  private @Nullable String spectraDomainUnit = DEFAULT_UNIT;
  private @Nullable String spectraRangeLabel = DEFAULT_UNIT;
  private @Nullable String spectraRangeUnit = DEFAULT_UNIT;

  private @Nullable String timeSeriesDomainLabel = "Retention time";
  private @Nullable String timeSeriesDomainUnit = "min";
  private @Nullable String timeSeriesRangeLabel = DEFAULT_UNIT;
  private @Nullable String timeSeriesRangeUnit = DEFAULT_UNIT;

  public OtherDataFileImpl(RawDataFile rawDataFile) {
    this.rawDataFile = rawDataFile;
  }

  @Override
  public @NotNull RawDataFile getCorrespondingRawDataFile() {
    return rawDataFile;
  }

  @Override
  public @NotNull List<@NotNull OtherSpectrum> getSpectra() {
    return spectra;
  }

  @Override
  public @NotNull IntensityTimeSeries getTimeSeries(int index) {
    return timeSeries.get(index);
  }

  @Override
  public RawDataFile getRawDataFile() {
    return rawDataFile;
  }

  @Override
  public @NotNull List<IntensityTimeSeries> getTimeSeries() {
    return timeSeries;
  }

  public void addSpectrum(@NotNull OtherSpectrum spectrum) {
    spectra.add(spectrum);
  }

  public void addTimeSeries(@NotNull IntensityTimeSeries series) {
    this.timeSeries.add(series);
  }

  @Override
  public @Nullable String getSpectraDomainLabel() {
    return spectraDomainLabel;
  }

  public void setSpectraDomainLabel(@Nullable String spectraDomainLabel) {
    this.spectraDomainLabel = spectraDomainLabel;
  }

  @Override
  public @Nullable String getSpectraDomainUnit() {
    return spectraDomainUnit;
  }

  public void setSpectraDomainUnit(@Nullable String spectraDomainUnit) {
    this.spectraDomainUnit = spectraDomainUnit;
  }

  @Override
  public @Nullable String getSpectraRangeLabel() {
    return spectraRangeLabel;
  }

  public void setSpectraRangeLabel(@Nullable String spectraRangeLabel) {
    this.spectraRangeLabel = spectraRangeLabel;
  }

  @Override
  public @Nullable String getSpectraRangeUnit() {
    return spectraRangeUnit;
  }

  public void setSpectraRangeUnit(@Nullable String spectraRangeUnit) {
    this.spectraRangeUnit = spectraRangeUnit;
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
  public @NotNull String getDescription() {
    return description;
  }

  public void setDescription(@NotNull String description) {
    this.description = description;
  }
}
