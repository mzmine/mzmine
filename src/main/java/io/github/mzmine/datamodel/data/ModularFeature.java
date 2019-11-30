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
import javafx.collections.ObservableMap;

/**
 * Feature with modular DataTypes
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
@SuppressWarnings("rawtypes")
public class ModularFeature implements ModularDataModel {

  private final ObservableMap<Class<? extends DataType>, DataType> map =
      FXCollections.observableMap(new HashMap<>());

  public ModularFeature() {

  }

  public ModularFeature(Feature p) {
    this();
    int[] scans = p.getScanNumbers();
    set(new ScanNumbersType(scans));
    set(new RawFileType(p.getDataFile()));
    set(new DetectionType(p.getFeatureStatus()));
    set(new MZType(p.getMZ()));
    set(new RTType((float) p.getRT()));
    set(new HeightType((float) p.getHeight()));
    set(new AreaType((float) p.getArea()));
    set(new BestScanNumberType(p.getRepresentativeScanNumber()));
    set(new FragmentScanNumbersType(p.getAllMS2FragmentScanNumbers()));
    set(new BestFragmentScanNumberType(p.getMostIntenseFragmentScanNumber()));
    set(new IsotopePatternType(p.getIsotopePattern()));
    set(new ChargeType(p.getCharge()));
    set(new ParentChromatogramIDType(p.getParentChromatogramRowID()));
    // symmetry
    if (p.getFWHM() != null)
      set(new FwhmType(p.getFWHM().floatValue()));
    if (p.getAsymmetryFactor() != null)
      set(new AsymmetryFactorType(p.getAsymmetryFactor().floatValue()));
    if (p.getTailingFactor() != null)
      set(new TailingFactorType(p.getTailingFactor().floatValue()));

    // datapoints of feature
    List<DataPoint> dps = new ArrayList<>();
    for (int i = 0; i < scans.length; i++) {
      dps.add(p.getDataPoint(scans[i]));
    }
    set(new DataPointsType(dps));

    // ranges
    Range<Float> rtRange = Range.closed(p.getRawDataPointsRTRange().lowerEndpoint().floatValue(),
        p.getRawDataPointsRTRange().upperEndpoint().floatValue());
    System.out.println(rtRange.toString() + " rtrange " + p.getRawDataPointsRTRange().toString());
    Range<Float> intensityRange =
        Range.closed(p.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            p.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    set(new RTRangeType(rtRange));
    set(new IntensityRangeType(intensityRange));
    set(new MZRangeType(p.getRawDataPointsMZRange()));
    /*
     * getDataPoint
     * 
     */
  }

  public DataPoint getDataPoint(int scan) {
    int index = getScanNumbers().indexOf(scan);
    if (index < 0)
      return null;
    return getDataPoints().get(index);
  }

  public List<Integer> getScanNumbers() {
    return get(ScanNumbersType.class).map(DataType::getValue).orElse(List.of());
  }

  public List<DataPoint> getDataPoints() {
    return get(DataPointsType.class).map(DataType::getValue).orElse(List.of());
  }

  public RawDataFile getRawDataFile() {
    return get(RawFileType.class).map(DataType::getValue).orElse(null);
  }

  @Override
  public ObservableMap<Class<? extends DataType>, DataType> getMap() {
    return map;
  }

}
