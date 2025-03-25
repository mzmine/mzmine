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

import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.otherdectectors.OtherFeatureDataType;
import io.github.mzmine.datamodel.features.types.otherdectectors.RawTraceType;
import io.github.mzmine.main.ConfigService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

public class OtherFeatureImpl implements OtherFeature {

  public final ObservableMap<DataType, Object> map = FXCollections.observableHashMap();

  public OtherFeatureImpl() {
  }

  public OtherFeatureImpl(OtherTimeSeries series) {
    set(OtherFeatureDataType.class, series);
    FeatureDataUtils.recalculateIntensityTimeSeriesDependingTypes(this);
  }

  @Override
  public ObservableMap<DataType, Object> getMap() {
    return map;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

//    if (getFeatureData() != null) {
//      sb.append(getFeatureData().getName()).append(" ");
//    }
    if (getWavelength() != null) {
      sb.append(getWavelength()).append(" nm ");
    }
    if (getChromatogramType() != null) {
      sb.append(getChromatogramType().getDescription()).append(" ");
    }
    if (getRT() != null && get(RawTraceType.class) != null) {
      sb.append(ConfigService.getGuiFormats().rt(getRT())).append(" min ");
    }

    return sb.toString().trim();
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OtherFeatureImpl that)) {
      return false;
    }

    return getMap().equals(that.getMap());
  }

  @Override
  public int hashCode() {
    return getMap().hashCode();
  }
}
