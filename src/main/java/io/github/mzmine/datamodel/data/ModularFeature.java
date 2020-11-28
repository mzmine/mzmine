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

package io.github.mzmine.datamodel.data;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.data.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.data.types.numbers.FwhmType;
import io.github.mzmine.datamodel.data.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.data.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.data.types.numbers.TailingFactorType;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.RawFileType;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import io.github.mzmine.datamodel.data.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.data.types.numbers.DataPointsType;
import io.github.mzmine.datamodel.data.types.numbers.HeightType;
import io.github.mzmine.datamodel.data.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import io.github.mzmine.datamodel.data.types.numbers.RTType;
import io.github.mzmine.datamodel.data.types.numbers.ScanNumbersType;
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

  // TODO: private variables to data types
  private SimpleFeatureInformation featureInfo;
  private int representiveScanNumber;
  private int charge;
  private int fragmentScanNumber;
  private ObservableList<Integer> allMS2FragmentScanNumbers;

  // Isotope pattern. Null by default but can be set later by deisotoping
  // method.
  private IsotopePattern isotopePattern;

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

    this.fragmentScanNumber = fragmentScanNumber;
    this.representiveScanNumber = representativeScan;
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

    this.allMS2FragmentScanNumbers = IntStream.of(allMS2FragmentScanNumbers).boxed()
        .collect(Collectors.toCollection(FXCollections::observableArrayList));

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
    return get(RTRangeType.class).getValue();
  }

  @Nonnull
  @Override
  public Range<Double> getRawDataPointsMZRange() {
    return get(MZRangeType.class).getValue();
  }

  @Nonnull
  @Override
  public Range<Float> getRawDataPointsIntensityRange() {
    return get(IntensityRangeType.class).getValue();
  }

  @Override
  public int getMostIntenseFragmentScanNumber() {
    return fragmentScanNumber;
  }

  @Override
  public void setFragmentScanNumber(int fragmentScanNumber) {
    this.fragmentScanNumber = fragmentScanNumber;
  }

  @Override
  public ObservableList<Integer> getAllMS2FragmentScanNumbers() {
    return allMS2FragmentScanNumbers;
  }

  @Override
  public void setAllMS2FragmentScanNumbers(ObservableList<Integer> allMS2FragmentScanNumbers) {
    this.allMS2FragmentScanNumbers = allMS2FragmentScanNumbers;
  }

  @Nullable
  @Override
  public IsotopePattern getIsotopePattern() {
    return isotopePattern;
  }

  @Override
  public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
    this.isotopePattern = isotopePattern;
  }

  @Override
  public int getCharge() {
    return charge;
  }

  @Override
  public void setCharge(int charge) {
    this.charge = charge;
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
    this.featureInfo = featureInfo;
  }

  @Override
  public SimpleFeatureInformation getFeatureInformation() {
    return featureInfo;
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
    return get(ScanNumbersType.class).getValue();
  }

  @Override
  public void setRepresentativeScanNumber(int representiveScanNumber) {
    this.representiveScanNumber = representiveScanNumber;
  }

  @Override
  public int getRepresentativeScanNumber() {
    return representiveScanNumber;
  }

  @Override
  public ObservableList<DataPoint> getDataPoints() {
    return get(DataPointsType.class).getValue();
  }

  public float getRT() {
    return get(RTType.class).getValue();
  }

  @Nonnull
  @Override
  public FeatureStatus getFeatureStatus() {
    return get(DetectionType.class).getValue();
  }

  public double getMZ() {
    return get(MZType.class).getValue();
  }

  public float getHeight() {
    return get(HeightType.class).getValue();
  }

  public float getArea() {
    return get(AreaType.class).getValue();
  }
}
