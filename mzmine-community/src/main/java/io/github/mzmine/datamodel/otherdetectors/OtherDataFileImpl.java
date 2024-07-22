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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OtherDataFileImpl implements OtherDataFile {

  public static final String DEFAULT_UNIT = "N/A";

  private final RawDataFile rawDataFile;
  private @NotNull String description = "Unknown file";
  private OtherSpectralData spectralData = null;
  private OtherTimeSeriesData timeSeriesData = null;
  private DetectorType detectorType = null;

  public OtherDataFileImpl(RawDataFile rawDataFile) {
    this.rawDataFile = rawDataFile;
  }

  @Override
  public @NotNull RawDataFile getCorrespondingRawDataFile() {
    return rawDataFile;
  }

  @Override
  @Nullable
  public OtherTimeSeriesData getOtherTimeSeries() {
    return timeSeriesData;
  }

  @Override
  @Nullable
  public OtherSpectralData getOtherSpectralData() {
    return spectralData;
  }

  public void setOtherSpectralData(OtherSpectralData spectralData) {
    if (timeSeriesData != null) {
      throw new IllegalStateException(
          "Cannot set spectral data to a file that already has time series data");
    }
    this.spectralData = spectralData;
  }

  public void setOtherTimeSeriesData(OtherTimeSeriesData timeSeriesData) {
    if (spectralData != null) {
      throw new IllegalStateException(
          "Cannot set time series data to a file that already has spectral data");
    }
    this.timeSeriesData = timeSeriesData;
  }

  @Override
  public @NotNull String getDescription() {
    return description;
  }

  public void setDescription(@NotNull String description) {
    this.description = description;
  }

  @Override
  public DetectorType getDetectorType() {
    return detectorType;
  }

  public void setDetectorType(DetectorType detectorType) {
    this.detectorType = detectorType;
  }
}
