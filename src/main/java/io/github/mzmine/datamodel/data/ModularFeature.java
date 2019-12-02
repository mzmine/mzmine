/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.IsotopePatternType;
import io.github.mzmine.datamodel.data.types.RawFileType;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import io.github.mzmine.datamodel.data.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.data.types.numbers.BestFragmentScanNumberType;
import io.github.mzmine.datamodel.data.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.data.types.numbers.ChargeType;
import io.github.mzmine.datamodel.data.types.numbers.DataPointsType;
import io.github.mzmine.datamodel.data.types.numbers.FragmentScanNumbersType;
import io.github.mzmine.datamodel.data.types.numbers.FwhmType;
import io.github.mzmine.datamodel.data.types.numbers.HeightType;
import io.github.mzmine.datamodel.data.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.data.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import io.github.mzmine.datamodel.data.types.numbers.ParentChromatogramIDType;
import io.github.mzmine.datamodel.data.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.data.types.numbers.RTType;
import io.github.mzmine.datamodel.data.types.numbers.ScanNumbersType;
import io.github.mzmine.datamodel.data.types.numbers.TailingFactorType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 * Feature with modular DataTypes
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class ModularFeature implements ModularDataModel {

  private final ObservableList<DataType> types = FXCollections.observableArrayList();
  private final ObservableMap<DataType, Object> map = FXCollections.observableMap(new HashMap<>());

  public ModularFeature() {}

  public ModularFeature(Feature p) {
    this();
    int[] scans = p.getScanNumbers();
    set(ScanNumbersType.class, (scans));
    set(RawFileType.class, (p.getDataFile()));
    set(DetectionType.class, (p.getFeatureStatus()));
    set(MZType.class, (p.getMZ()));
    set(RTType.class, ((float) p.getRT()));
    set(HeightType.class, ((float) p.getHeight()));
    set(AreaType.class, ((float) p.getArea()));
    set(BestScanNumberType.class, (p.getRepresentativeScanNumber()));
    set(FragmentScanNumbersType.class, (p.getAllMS2FragmentScanNumbers()));
    set(BestFragmentScanNumberType.class, (p.getMostIntenseFragmentScanNumber()));
    set(IsotopePatternType.class, (p.getIsotopePattern()));
    set(ChargeType.class, (p.getCharge()));
    set(ParentChromatogramIDType.class, (p.getParentChromatogramRowID()));
    // symmetry
    if (p.getFWHM() != null)
      set(FwhmType.class, (p.getFWHM().floatValue()));
    if (p.getAsymmetryFactor() != null)
      set(AsymmetryFactorType.class, (p.getAsymmetryFactor().floatValue()));
    if (p.getTailingFactor() != null)
      set(TailingFactorType.class, (p.getTailingFactor().floatValue()));

    // datapoints of feature
    List<DataPoint> dps = new ArrayList<>();
    for (int i = 0; i < scans.length; i++) {
      dps.add(p.getDataPoint(scans[i]));
    }
    set(DataPointsType.class, dps);

    // ranges
    Range<Float> rtRange = Range.closed(p.getRawDataPointsRTRange().lowerEndpoint().floatValue(),
        p.getRawDataPointsRTRange().upperEndpoint().floatValue());
    Range<Float> intensityRange =
        Range.closed(p.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            p.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    set(RTRangeType.class, rtRange);
    set(IntensityRangeType.class, intensityRange);
    set(MZRangeType.class, p.getRawDataPointsMZRange());
    /*
     * getDataPoint
     * 
     */
  }


  @Override
  public ObservableList<DataType> getTypes() {
    return types;
  }

  @Override
  public ObservableMap<DataType, Object> getMap() {
    return map;
  }

  public DataPoint getDataPoint(int scan) {
    int index = getScanNumbers().indexOf(scan);
    if (index < 0)
      return null;
    return getDataPoints().get(index);
  }

  public List<Integer> getScanNumbers() {
    return get(ScanNumbersType.class).orElse(List.of());
  }

  public List<DataPoint> getDataPoints() {
    return get(DataPointsType.class).orElse(List.of());
  }

  public RawDataFile getRawDataFile() {
    return get(RawFileType.class).orElse(null);
  }

  public float getRT() {
    return get(RTType.class).orElse(-1f);
  }

  public double getMZ() {
    return get(MZType.class).orElse(-1d);
  }

  public float getHeight() {
    return get(HeightType.class).orElse(0f);
  }

  public float getArea() {
    return get(AreaType.class).orElse(0f);
  }

}
