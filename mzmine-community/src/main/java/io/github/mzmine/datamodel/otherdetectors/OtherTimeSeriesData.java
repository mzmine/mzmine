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

import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Coollects traces from a single detector. these traces may be read from the raw data or created
 * from {@link OtherSpectralData} by slicing along the retention time axis.
 */
public interface OtherTimeSeriesData {

  OtherDataFile getOtherDataFile();

  @NotNull
  String getTimeSeriesDomainLabel();

  @NotNull
  String getTimeSeriesDomainUnit();

  @NotNull
  String getTimeSeriesRangeLabel();

  @NotNull
  String getTimeSeriesRangeUnit();

  @NotNull
  List<@NotNull OtherFeature> getRawTraces();

  default int getNumberOfTimeSeries() {
    return getRawTraces().size();
  }

  @NotNull
  OtherFeature getRawTrace(int index);

  /**
   * @return The chromatograms in this data file or null if this file does not contain
   * chromatograms.
   */
  @NotNull
  ChromatogramType getChromatogramType();

  List<OtherFeature> getProcessedFeatures();

  /**
   * @return The processed features for the given series, may be empty. The list is modifiable.
   */
  @NotNull
  List<OtherFeature> getProcessedFeaturesForTrace(OtherFeature rawTrace);

  void replaceProcessedFeaturesForTrace(OtherFeature rawTrace,
      @NotNull List<OtherFeature> newFeatures);

  void addProcessedFeature(@NotNull OtherFeature newFeature);
}
