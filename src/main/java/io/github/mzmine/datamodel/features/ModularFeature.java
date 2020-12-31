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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.FeatureInformationType;
import io.github.mzmine.datamodel.features.types.IsotopePatternType;
import io.github.mzmine.datamodel.features.types.RawFileType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.BestFragmentScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.DataPointsType;
import io.github.mzmine.datamodel.features.types.numbers.FragmentScanNumbersType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.ScanNumbersType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javax.annotation.Nonnull;
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

    // register listener to types map to automatically generate default properties for new DataTypes
    flist.getFeatureTypes().addListener(
        (MapChangeListener<? super Class<? extends DataType>, ? super DataType>) change -> {
          if(change.wasAdded()) {
            // add type columns to maps
            DataType type = change.getValueAdded();
            this.setProperty(type, type.createProperty());
          } else if(change.wasRemoved()) {
            // remove type columns to maps
            DataType<Property<?>> type = change.getValueRemoved();
            this.removeProperty((Class<DataType<Property<?>>>) type.getClass());
          }
        });
  }

  // NOT TESTED
  /**
   * Initializes a new feature using given values
   *
   */
  public ModularFeature(ModularFeatureList flist, RawDataFile dataFile, double mz, float rt, float height, float area,
      int[] scanNumbers, DataPoint[] dataPointsPerScan, FeatureStatus featureStatus,
      int representativeScan, int fragmentScanNumber, int[] allMS2FragmentScanNumbers,
      @Nonnull Range<Float> rtRange, @Nonnull Range<Double> mzRange,
      @Nonnull Range<Float> intensityRange) {
    this(flist);

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
   * Copy constructor
   */
//  public ModularFeature(@Nonnull Feature f) {
//    this((ModularFeatureList) Objects.requireNonNull(f.getFeatureList()), f);
//  }

  /**
   * Copy constructor with custom feature list
   */
  public ModularFeature(@Nonnull ModularFeatureList flist, Feature f) {
    this(flist);
    if(f instanceof ModularFeature) {
      ((ModularFeature) f).stream().forEach(entry -> this.set(entry.getKey(), entry.getValue()));
    }
    else {
      // add values to feature
      set(ScanNumbersType.class, f.getScanNumbers());
      set(RawFileType.class, (f.getRawDataFile()));
      set(DetectionType.class, (f.getFeatureStatus()));
      set(MZType.class, (f.getMZ()));
      set(RTType.class, (f.getRT()));
      set(HeightType.class, (f.getHeight()));
      set(AreaType.class, (f.getArea()));
      set(BestScanNumberType.class, (f.getRepresentativeScanNumber()));
      set(BestFragmentScanNumberType.class, (f.getMostIntenseFragmentScanNumber()));
      set(FragmentScanNumbersType.class, (f.getAllMS2FragmentScanNumbers()));

      // datapoints of feature
      set(DataPointsType.class, f.getDataPoints());

      // ranges
      set(MZRangeType.class, f.getRawDataPointsMZRange());
      set(RTRangeType.class, f.getRawDataPointsRTRange());
      set(IntensityRangeType.class, f.getRawDataPointsIntensityRange());

      // quality parameters
      float fwhm = f.getFWHM();
      if (!Float.isNaN(fwhm)) {
        set(FwhmType.class, fwhm);
      }
      float tf = f.getTailingFactor();
      if (!Float.isNaN(tf)) {
        set(TailingFactorType.class, tf);
      }
      float af = f.getAsymmetryFactor();
      if (!Float.isNaN(af)) {
        set(AsymmetryFactorType.class, af);
      }
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
    ObjectProperty<Range<Float>> v = get(RTRangeType.class);
    return v == null || v.getValue() == null ? Range.singleton(0f) : v.getValue();
  }

  @Nonnull
  @Override
  public Range<Double> getRawDataPointsMZRange() {
    ObjectProperty<Range<Double>> v = get(MZRangeType.class);
    return v == null || v.getValue() == null ? Range.singleton(0d) : v.getValue();
  }

  @Nonnull
  @Override
  public Range<Float> getRawDataPointsIntensityRange() {
    ObjectProperty<Range<Float>> v = get(IntensityRangeType.class);
    return v == null || v.getValue() == null ? Range.singleton(0f) : v.getValue();
  }

  @Override
  public int getMostIntenseFragmentScanNumber() {
    Property<Integer> v = get(BestFragmentScanNumberType.class);
    return v == null || v.getValue() == null ? -1 : v.getValue();
  }

  @Override
  public void setFragmentScanNumber(int fragmentScanNumber) {
    set(BestFragmentScanNumberType.class, fragmentScanNumber);
  }

  @Override
  public ObservableList<Integer> getAllMS2FragmentScanNumbers() {
    ListProperty<Integer> v = get(FragmentScanNumbersType.class);
    return v == null || v.getValue() == null ? FXCollections
        .unmodifiableObservableList(FXCollections.emptyObservableList())
        : v.getValue();
  }

  @Override
  public void setAllMS2FragmentScanNumbers(ObservableList<Integer> allMS2FragmentScanNumbers) {
    set(FragmentScanNumbersType.class, allMS2FragmentScanNumbers);
  }

  @Nullable
  @Override
  public IsotopePattern getIsotopePattern() {
    Property<IsotopePattern> v = get(IsotopePatternType.class);
    return v == null ? null : v.getValue();
  }

  @Override
  public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
    set(IsotopePatternType.class, isotopePattern);
  }

  @Override
  public int getCharge() {
    Property<Integer> charge = get(ChargeType.class);
    return charge == null || charge.getValue() == null ? 0 : charge.getValue();
  }

  @Override
  public void setCharge(int charge) {
    set(ChargeType.class, charge);
  }

  @Override
  public float getFWHM() {
    Property<Float> v = get(FwhmType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public float getTailingFactor() {
    Property<Float> v = get(TailingFactorType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public float getAsymmetryFactor() {
    Property<Float> v = get(AsymmetryFactorType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
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
    ObjectProperty<SimpleFeatureInformation> v = get(FeatureInformationType.class);
    return v == null ? null : v.getValue();
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

  @Nullable
  @Override
  public RawDataFile getRawDataFile() {
    ObjectProperty<RawDataFile> raw = get(RawFileType.class);
    return raw == null ? null : raw.getValue();
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
    ListProperty<Integer> v = get(ScanNumbersType.class);
    return v == null || v.getValue() == null ?
        FXCollections.unmodifiableObservableList(FXCollections.emptyObservableList())
        : v.getValue();
  }

  @Override
  public void setRepresentativeScanNumber(int representiveScanNumber) {
    set(BestScanNumberType.class, representiveScanNumber);
  }

  @Override
  public int getRepresentativeScanNumber() {
    Property<Integer> v = get(BestScanNumberType.class);
    return v == null || v.getValue() == null ? -1 : v.getValue();
  }

  @Override
  public ObservableList<DataPoint> getDataPoints() {
    ListProperty<DataPoint> v = get(DataPointsType.class);
    return v == null || v.getValue() == null ?
        FXCollections.unmodifiableObservableList(FXCollections.emptyObservableList())
        : v.getValue();
  }

  public float getRT() {
    Property<Float> v = get(RTType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Nonnull
  @Override
  public FeatureStatus getFeatureStatus() {
    ObjectProperty<FeatureStatus> v = get(DetectionType.class);
    return v == null || v.getValue() == null ? FeatureStatus.UNKNOWN : v.getValue();
  }

  public double getMZ() {
    Property<Double> mz = get(MZType.class);
    return mz == null || mz.getValue() == null ? Double.NaN : mz.getValue();
  }

  public float getHeight() {
    Property<Float> v = get(HeightType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  public float getArea() {
    Property<Float> v = get(AreaType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }
}
