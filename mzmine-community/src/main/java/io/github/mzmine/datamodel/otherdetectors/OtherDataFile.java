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

/**
 * Groups data of one detector type. Must be associated with an MS {@link RawDataFile}. One
 * {@link RawDataFile} can contain multiple {@link OtherDataFile}s if multiple other detectors were
 * used.
 */
public interface OtherDataFile {

  @NotNull
  RawDataFile getCorrespondingRawDataFile();

  default boolean hasTimeSeries() {
    return getNumberOfTimeSeries() != 0;
  }

  default boolean hasSpectra() {
    return getNumberOfSpectra() != 0;
  }

  default int getNumberOfSpectra() {
    return getOtherSpectralData() != null ? getOtherSpectralData().getSpectra().size() : 0;
  }

  default int getNumberOfTimeSeries() {
    return getOtherTimeSeries() != null ? getOtherTimeSeries().getNumberOfTimeSeries() : 0;
  }

  @Nullable
  OtherTimeSeriesData getOtherTimeSeries();

  @Nullable
  OtherSpectralData getOtherSpectralData();

  @NotNull
  String getDescription();

  DetectorType getDetectorType();
}
