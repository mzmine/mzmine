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

import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;

public interface OtherTimeSeriesData {

  OtherDataFile getOtherDataFile();

  String getTimeSeriesDomainLabel();

  String getTimeSeriesDomainUnit();

  String getTimeSeriesRangeLabel();

  String getTimeSeriesRangeUnit();

  @NotNull
  List<@NotNull OtherTimeSeries> getTimeSeries();

  default int getNumberOfTimeSeries() {
    return getTimeSeries().size();
  }

  @NotNull
  OtherTimeSeries getTimeSeries(int index);

  /**
   * @return The chromatograms in this data file or null if this file does not contain
   * chromatograms.
   */
  @NotNull
  ChromatogramType getChromatogramType();

  /**
   * @return The processed features for the given series, may be empty. The list is modifiable.
   */
  @NotNull
  ObservableList<OtherFeature> getProcessedFeaturesForSeries(OtherTimeSeries series);

  void addProcessedFeatureForSeries(@NotNull OtherTimeSeries series,
      @NotNull OtherFeature otherFeature);

  boolean removeProcessedFeatureForSeries(@NotNull OtherTimeSeries series,
      @NotNull OtherFeature otherFeature);

  void setProcessedFeaturesForSeries(@NotNull OtherTimeSeries series,
      @NotNull List<OtherFeature> otherFeatures);

  void clearProcessedFeaturesForSeries(OtherTimeSeries series);

  /**
   * A read only copy of the observable map of this data. The map is read only to prevent addition
   * of {@link OtherTimeSeries} that do not belong to this {@link OtherTimeSeriesData}.
   */
  @NotNull
  ObservableMap<OtherTimeSeries, ObservableList<OtherFeature>> getProcessedFeatures();
}
