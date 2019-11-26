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

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakInformation;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data.types.AreaType;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.HeightType;
import io.github.mzmine.datamodel.data.types.MZType;
import io.github.mzmine.datamodel.data.types.RTType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Map of all feature related data.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class ModularFeatureListRow implements PeakListRow {

  @SuppressWarnings({"rawtypes"})
  private final ObservableMap<Class<? extends DataType<?>>, DataType> map =
      FXCollections.observableMap(new HashMap<>());

  public <T extends DataType<?>> Optional<T> get(Class<T> type) {
    return Optional.ofNullable(map.get(type));
  }

  public void set(Class<? extends DataType<?>> key, DataType<?> data) {
    if (key.isInstance(data))
      map.put(key, data);
    // wrong data type. Check code that supplied this data
    else
      throw new WrongTypeException(key, data);
  }

  public ObservableMap<Class<? extends DataType<?>>, DataType> getMap() {
    return map;
  }

  public Stream<DataType> stream() {
    return map.values().stream();
  }

  public FeatureStatus getDetectionType() {
    return get(DetectionType.class).map(DataType::getValue).orElse(FeatureStatus.UNKNOWN);
  }

  public Double getMZ() {
    return get(MZType.class).map(DataType::getValue).orElse(0d);
  }

  public Float getRT() {
    return get(RTType.class).map(DataType::getValue).orElse(0f);
  }

  public double getHeight() {
    return get(HeightType.class).map(DataType::getValue).orElse(0f);
  }

  public double getArea() {
    return get(AreaType.class).map(DataType::getValue).orElse(0f);
  }

  @Override
  public RawDataFile[] getRawDataFiles() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getID() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumberOfPeaks() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Feature[] getPeaks() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Feature getPeak(RawDataFile rawData) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addPeak(RawDataFile rawData, Feature peak) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removePeak(RawDataFile file) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean hasPeak(Feature peak) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean hasPeak(RawDataFile rawData) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public double getAverageMZ() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getAverageRT() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getAverageHeight() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getRowCharge() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getAverageArea() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getComment() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setComment(String comment) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setAverageMZ(double mz) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setAverageRT(double rt) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addPeakIdentity(PeakIdentity identity, boolean preffered) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removePeakIdentity(PeakIdentity identity) {
    // TODO Auto-generated method stub

  }

  @Override
  public PeakIdentity[] getPeakIdentities() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PeakIdentity getPreferredPeakIdentity() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setPreferredPeakIdentity(PeakIdentity identity) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setPeakInformation(PeakInformation information) {
    // TODO Auto-generated method stub

  }

  @Override
  public PeakInformation getPeakInformation() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getDataPointMaxIntensity() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Feature getBestPeak() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Scan getBestFragmentation() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public Scan[] getAllMS2Fragmentations() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IsotopePattern getBestIsotopePattern() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setID(int id) {
    // TODO Auto-generated method stub

  }

}
