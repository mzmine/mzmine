/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.types.*;
import io.github.mzmine.datamodel.features.types.exceptions.TypeColumnUndefinedException;
import io.github.mzmine.datamodel.features.types.numbers.*;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javax.annotation.Nullable;

/**
 * Feature with modular DataTypes
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class ModularFeature implements Feature, ModularDataModel {

  private @Nonnull ModularFeatureList flist;
  private final ObservableMap<DataType, Property<?>> map =
      FXCollections.observableMap(new HashMap<>());


  public ModularFeature(@Nonnull ModularFeatureList flist) {
    this.flist = flist;

    // add type property columns to maps
    flist.getFeatureTypes().values().forEach(type -> {
      this.setProperty(type, type.createProperty());
    });
  }

  // NOT TESTED
  /**
   * Initializes a new feature using given values
   *
   */
  public ModularFeature(RawDataFile dataFile, double mz, float rt, float height, float area,
      int[] scanNumbers, DataPoint[] dataPointsPerScan, FeatureStatus featureStatus,
      int representativeScan, int fragmentScanNumber, int[] allMS2FragmentScanNumbers,
      @Nonnull Range<Float> rtRange, @Nonnull Range<Double> mzRange,
      @Nonnull Range<Float> intensityRange) {

    this(new ModularFeatureList("", dataFile));

    assert dataFile != null;
    assert scanNumbers != null;
    assert dataPointsPerScan != null;
    assert featureStatus != null;

    if (dataPointsPerScan.length == 0) {
      throw new IllegalArgumentException("Cannot create a ModularFeature instance with no data points");
    }

    setFragmentScanNumber(fragmentScanNumber);
    setRepresentativeScanNumber(representativeScan);
    // add values to feature
    set(ScanNumbersType.class, IntStream.of(scanNumbers).boxed().collect(Collectors.toList()));
    set(RawFileType.class, dataFile);
    set(DetectionType.class, featureStatus);
    set(MZType.class, mz);
    set(RTType.class, rt);
    set(HeightType.class, height);
    set(AreaType.class, area);
    set(BestScanNumberType.class, representativeScan);

    // datapoints of feature
    set(DataPointsType.class, Arrays.asList(dataPointsPerScan));

    // ranges
    set(MZRangeType.class, mzRange);
    set(RTRangeType.class, rtRange);
    set(IntensityRangeType.class, intensityRange);

    set(FragmentScanNumbersType.class, IntStream.of(allMS2FragmentScanNumbers).boxed()
        .collect(Collectors.toCollection(FXCollections::observableArrayList)));

    float fwhm = QualityParameters.calculateFWHM(this);
    if(!Float.isNaN(fwhm)) {
      set(FwhmType.class, fwhm);
    }
    float tf = QualityParameters.calculateTailingFactor(this);
    if(!Float.isNaN(tf)) {
      set(TailingFactorType.class, tf);
    }
    float af = QualityParameters.calculateAsymmetryFactor(this);
    if(!Float.isNaN(af)) {
      set(AsymmetryFactorType.class, af);
    }
  }

  /**
   * Initializes a new feature using given feature list and values
   *
   */
  public ModularFeature(@Nonnull ModularFeatureList featureList, RawDataFile dataFile, double mz, float rt,
      float height, float area, int[] scanNumbers, DataPoint[] dataPointsPerScan, FeatureStatus featureStatus,
      int representativeScan, int fragmentScanNumber, int[] allMS2FragmentScanNumbers,
      @Nonnull Range<Float> rtRange, @Nonnull Range<Double> mzRange,
      @Nonnull Range<Float> intensityRange) {
    this(dataFile, mz, rt, height, area, scanNumbers, dataPointsPerScan, featureStatus, representativeScan,
        fragmentScanNumber, allMS2FragmentScanNumbers, rtRange, mzRange, intensityRange);
    setFeatureList(featureList);
  }

  /**
   * Copy constructor
   */
  public ModularFeature(@Nonnull Feature f) {
    this((ModularFeatureList) Objects.requireNonNull(f.getFeatureList()), f);
  }

  /**
   * Copy constructor with custom feature list
   */
  public ModularFeature(@Nonnull ModularFeatureList flist, Feature f) {
    this(flist);
    // add values to feature
    set(ScanNumbersType.class, f.getScanNumbers());
    set(RawFileType.class, (f.getRawDataFile()));
    set(DetectionType.class, (f.getFeatureStatus()));
    set(MZType.class, (f.getMZ()));
    set(RTType.class, (f.getRT()));
    set(HeightType.class, (f.getHeight()));
    set(AreaType.class, (f.getArea()));
    set(BestScanNumberType.class, (f.getRepresentativeScanNumber()));

    // datapoints of feature
    set(DataPointsType.class, f.getDataPoints());

    // ranges
    set(MZRangeType.class, f.getRawDataPointsMZRange());
    set(RTRangeType.class, f.getRawDataPointsRTRange());
    set(IntensityRangeType.class, f.getRawDataPointsIntensityRange());

    // quality parameters
    float fwhm = f.getFWHM();
    if(!Float.isNaN(fwhm)) {
      set(FwhmType.class, fwhm);
    }
    float tf = f.getTailingFactor();
    if(!Float.isNaN(tf)) {
      set(TailingFactorType.class, tf);
    }
    float af = f.getAsymmetryFactor();
    if(!Float.isNaN(af)) {
      set(AsymmetryFactorType.class, af);
    }
  }

  @Override
  public <T extends Property<?>> void set(Class<? extends DataType<T>> tclass, Object value) {
    // type in defined columns?
    if (!getTypes().containsKey(tclass)) {
      try {
        DataType newType = tclass.getConstructor().newInstance();
        ModularFeatureList flist = (ModularFeatureList) getFeatureList();
        flist.addFeatureType(newType);
        setProperty(newType, newType.createProperty());
      } catch (NullPointerException | InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        e.printStackTrace();
        return;
      }
    }
    // access default method
    ModularDataModel.super.set(tclass, value);
  }


  @Override
  public ObservableMap<Class<? extends DataType>, DataType> getTypes() {
    return flist.getFeatureTypes();
  }

  @Override
  public ObservableMap<DataType, Property<?>> getMap() {
    return map;
  }

  @Override
  public DataPoint getDataPoint(int scan) {
    int index = getScanNumbers().indexOf(scan);
    if (index < 0)
      return null;
    return getDataPoints().get(index);
  }

  @Nonnull
  @Override
  public Range<Float> getRawDataPointsRTRange() {
    if(!hasTypeColumn(RTRangeType.class))
      return Range.singleton(0f);
    return get(RTRangeType.class).getValue();
  }

  @Nonnull
  @Override
  public Range<Double> getRawDataPointsMZRange() {
    if(!hasTypeColumn(MZRangeType.class))
      return Range.singleton(0d);
    return get(MZRangeType.class).getValue();
  }

  @Nonnull
  @Override
  public Range<Float> getRawDataPointsIntensityRange() {
    if(!hasTypeColumn(IntensityRangeType.class))
      return Range.singleton(0f);
    return get(IntensityRangeType.class).getValue();
  }

  @Override
  public int getMostIntenseFragmentScanNumber() {
    if(!hasTypeColumn(BestFragmentScanNumberType.class))
      return -1;
    return get(BestFragmentScanNumberType.class).getValue();
  }

  @Override
  public void setFragmentScanNumber(int fragmentScanNumber) {
    set(BestFragmentScanNumberType.class, fragmentScanNumber);
  }

  @Override
  public ObservableList<Integer> getAllMS2FragmentScanNumbers() {
    if(!hasTypeColumn(FragmentScanNumbersType.class))
      return FXCollections.emptyObservableList();
    return get(FragmentScanNumbersType.class).getValue();
  }

  @Override
  public void setAllMS2FragmentScanNumbers(ObservableList<Integer> allMS2FragmentScanNumbers) {
    set(FragmentScanNumbersType.class, allMS2FragmentScanNumbers);
  }

  @Nullable
  @Override
  public IsotopePattern getIsotopePattern() {
    if(!hasTypeColumn(IsotopePatternType.class))
      return null;
    return get(IsotopePatternType.class).getValue();
  }

  @Override
  public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
    set(IsotopePatternType.class, isotopePattern);
  }

  @Override
  public int getCharge() {
    Integer charge = get(ChargeType.class).getValue();
    return charge==null? 0 : charge;
  }

  @Override
  public void setCharge(int charge) {
    set(ChargeType.class, charge);
  }

  @Override
  public float getFWHM() {
    if (get(FwhmType.class) == null || get(FwhmType.class).getValue() == null) {
      return Float.NaN;
    }
    return get(FwhmType.class).getValue();
  }

  @Override
  public float getTailingFactor() {
    if (get(TailingFactorType.class) == null || get(TailingFactorType.class).getValue() == null) {
      return Float.NaN;
    }
    return get(TailingFactorType.class).getValue();
  }

  @Override
  public float getAsymmetryFactor() {
    if (get(AsymmetryFactorType.class) == null || get(AsymmetryFactorType.class).getValue() == null) {
      return Float.NaN;
    }
    return get(AsymmetryFactorType.class).getValue();
  }

  @Override
  public void setMZ(double mz) {
    set(MZType.class, mz);
  }

  @Override
  public void setRT(float rt) {
    set(RTType.class, rt);
  }

  @Override
  public void setHeight(float height) {
    set(HeightType.class, height);
  }

  @Override
  public void setArea(float area) {
    set(AreaType.class, area);
  }

  @Override
  public void setFWHM(double fwhm) {
    set(FwhmType.class, fwhm);
  }

  @Override
  public void setTailingFactor(double tf) {
    set(TailingFactorType.class, tf);
  }

  @Override
  public void setAsymmetryFactor(double af) {
    set(AsymmetryFactorType.class, af);
  }

  @Override
  public void outputChromToFile() {

  }

  @Override
  public void setFeatureInformation(SimpleFeatureInformation featureInfo) {
    set(FeatureInformationType.class, featureInfo);
  }

  @Override
  public SimpleFeatureInformation getFeatureInformation() {
    if(!hasTypeColumn(FeatureInformationType.class))
      return null;
    return get(FeatureInformationType.class).getValue();
  }

  @Nullable
  @Override
  public FeatureList getFeatureList() {
    return flist;
  }

  @Override
  public void setFeatureList(@Nonnull FeatureList flist) {
    this.flist = (ModularFeatureList) flist;
  }

  public ListProperty<Integer> getScanNumbersProperty() {
    return get(ScanNumbersType.class);
  }

  public ListProperty<DataPoint> getDataPointsProperty() {
    return get(DataPointsType.class);
  }

  @Nonnull
  @Override
  public RawDataFile getRawDataFile() {
    ObjectProperty<RawDataFile> raw = get(RawFileType.class);
    return raw.getValue();
  }

  public Property<Float> getRTProperty() {
    return get(RTType.class);
  }

  public Property<Double> getMZProperty() {
    return get(MZType.class);
  }

  public Property<Float> getHeightProperty() {
    return get(HeightType.class);
  }

  public Property<Float> getAreaProperty() {
    return get(AreaType.class);
  }

  @Nonnull
  @Override
  public ObservableList<Integer> getScanNumbers() {
    if(!hasTypeColumn(ScanNumbersType.class))
      return FXCollections.emptyObservableList();
    return get(ScanNumbersType.class).getValue();
  }

  @Override
  public void setRepresentativeScanNumber(int representiveScanNumber) {
    set(BestScanNumberType.class, representiveScanNumber);
  }

  @Override
  public int getRepresentativeScanNumber() {
    if(!hasTypeColumn(BestScanNumberType.class))
      return -1;
    return get(BestScanNumberType.class).getValue();
  }

  @Override
  public ObservableList<DataPoint> getDataPoints() {
    if(!hasTypeColumn(DataPointsType.class))
      return FXCollections.emptyObservableList();
    return get(DataPointsType.class).getValue();
  }

  public float getRT() {
    if(!hasTypeColumn(RTType.class))
      return Float.NaN;
    return get(RTType.class).getValue();
  }

  @Nonnull
  @Override
  public FeatureStatus getFeatureStatus() {
    if(!hasTypeColumn(RTType.class))
      return FeatureStatus.UNKNOWN;
    return get(DetectionType.class).getValue();
  }

  public double getMZ() {
    if(!hasTypeColumn(MZType.class))
      return Double.NaN;
    return get(MZType.class).getValue();
  }

  public float getHeight() {
    if(!hasTypeColumn(HeightType.class))
      return Float.NaN;
    return get(HeightType.class).getValue();
  }

  public float getArea() {
    if(!hasTypeColumn(AreaType.class))
      return Float.NaN;
    return get(AreaType.class).getValue();
  }
}
