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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.RawFileType;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import io.github.mzmine.datamodel.data.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.data.types.numbers.DataPointsType;
import io.github.mzmine.datamodel.data.types.numbers.HeightType;
import io.github.mzmine.datamodel.data.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.data.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import io.github.mzmine.datamodel.data.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.data.types.numbers.RTType;
import io.github.mzmine.datamodel.data.types.numbers.ScanNumbersType;
import io.github.mzmine.util.DataTypeUtils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Feature with modular DataTypes
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class ModularFeature implements ModularDataModel {

  private final @Nonnull ModularFeatureList flist;
  private final ObservableMap<DataType, Property<?>> map =
      FXCollections.observableMap(new HashMap<>());

  public ModularFeature(@Nonnull ModularFeatureList flist) {
    this.flist = flist;
  }

  /**
   * Creates a ModularFeature on the basis of chromatogram results with the
   * {@link DataTypeUtils#addDefaultChromatographicTypeColumns(ModularFeatureList)} columns
   * 
   * @param flist
   * @param p
   */
  public ModularFeature(@Nonnull ModularFeatureList flist, Feature p) {
    this(flist);
    // ensure that the default columns are available
    DataTypeUtils.addDefaultChromatographicTypeColumns(flist);

    // add values to feature
    int[] scans = p.getScanNumbers();
    set(ScanNumbersType.class, IntStream.of(scans).boxed().collect(Collectors.toList()));
    set(RawFileType.class, (p.getDataFile()));
    set(DetectionType.class, (p.getFeatureStatus()));
    set(MZType.class, (p.getMZ()));
    set(RTType.class, ((float) p.getRT()));
    set(HeightType.class, ((float) p.getHeight()));
    set(AreaType.class, ((float) p.getArea()));
    set(BestScanNumberType.class, (p.getRepresentativeScanNumber()));

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
  }

  public ModularFeatureList getFeatureList() {
    return flist;
  }

  @Override
  public ObservableMap<Class<? extends DataType>, DataType> getTypes() {
    return flist.getFeatureTypes();
  }

  @Override
  public ObservableMap<DataType, Property<?>> getMap() {
    return map;
  }

  public DataPoint getDataPoint(int scan) {
    int index = getScanNumbers().indexOf(scan);
    if (index < 0)
      return null;
    return getDataPoints().get(index);
  }

  public ListProperty<Integer> getScanNumbers() {
    return get(ScanNumbersType.class);
  }

  public ListProperty<DataPoint> getDataPoints() {
    return get(DataPointsType.class);
  }

  public RawDataFile getRawDataFile() {
    ObjectProperty<RawDataFile> raw = get(RawFileType.class);
    return raw.getValue();
  }

  public FloatProperty getRT() {
    return get(RTType.class);
  }

  public DoubleProperty getMZ() {
    return get(MZType.class);
  }

  public FloatProperty getHeight() {
    return get(HeightType.class);
  }

  public FloatProperty getArea() {
    return get(AreaType.class);
  }

}
