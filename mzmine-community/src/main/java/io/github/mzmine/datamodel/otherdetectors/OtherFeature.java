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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.otherdectectors.ChromatogramTypeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.OtherFeatureDataType;
import io.github.mzmine.datamodel.features.types.otherdectectors.WavelengthType;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface OtherFeature extends ModularDataModel {

  @Nullable
  default OtherTimeSeries getFeatureData() {
    return get(OtherFeatureDataType.class);
  }

  @NotNull
  default OtherDataFile getOtherDataFile() {
    final OtherTimeSeries timeSeries = getFeatureData();
    if (timeSeries == null) {
      throw new IllegalStateException("TimeSeries is null");
    }

    return timeSeries.getOtherDataFile();
  }

  @NotNull
  default RawDataFile getRawDataFile() {
    return getOtherDataFile().getCorrespondingRawDataFile();
  }

  /**
   * @return Creates a new feature with the same properties, except the actual time series data. If
   * this feature is the "raw" trace, this is used as RawTraceType. If this feature is already
   * processed, the original RawTraceType is used.
   * <p></p>
   * Note: Don't forget to recalculate all time series depending types using
   * {@link
   * io.github.mzmine.datamodel.featuredata.FeatureDataUtils#recalculateIntensityTimeSeriesDependingTypes(OtherFeature)}
   * after setting the {@link OtherFeatureDataType}.
   * <p></p>
   * If the new {@link OtherTimeSeries} is already available, use
   * {@link #createSubFeature(OtherTimeSeries)} instead.
   */
  OtherFeature createSubFeature();

  /**
   * @return Creates a new feature with the same properties, except the actual time series data. If
   * this feature is the "raw" trace, this is used as RawTraceType. If this feature is already
   * processed, the original RawTraceType is used.
   * <p></p>
   * Note: This method automatically recalculates all time series depending types using
   * {@link
   * io.github.mzmine.datamodel.featuredata.FeatureDataUtils#recalculateIntensityTimeSeriesDependingTypes(OtherFeature)}
   * after setting the {@link OtherFeatureDataType}.
   */
  default OtherFeature createSubFeature(OtherTimeSeries series) {
    var feature = createSubFeature();
    feature.set(OtherFeatureDataType.class, series);
    FeatureDataUtils.recalculateIntensityTimeSeriesDependingTypes(feature);
    return feature;
  }

  @Nullable
  default Float getRT() {
    return get(RTType.class);
  }

  @Nullable
  default Range<Float> getRtRange() {
    return get(RTRangeType.class);
  }

  @Nullable
  default Double getWavelength() {
    return get(WavelengthType.class);
  }

  @Nullable
  default ChromatogramType getChromatogramType() {
    return get(ChromatogramTypeType.class);
  }


}
