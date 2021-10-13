/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.FeatureInformationType;
import io.github.mzmine.datamodel.features.types.IsotopePatternType;
import io.github.mzmine.datamodel.features.types.MobilityUnitType;
import io.github.mzmine.datamodel.features.types.RawFileType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.BestFragmentScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.FragmentScanNumbersType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.util.DataPointUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Feature with modular DataTypes
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ModularFeature implements Feature, ModularDataModel {

  private final ObservableMap<DataType, Object> map = FXCollections
      .observableMap(new HashMap<>());
  // buffert col charts and nodes
  private final Map<String, Node> buffertColCharts = new HashMap<>();
  @NotNull
  private ModularFeatureList flist;

  public ModularFeature(@NotNull ModularFeatureList flist) {
    this.flist = flist;

    // register listener to types map to automatically generate default properties for new DataTypes
    flist.getFeatureTypes().addListener(
        (MapChangeListener<? super Class<? extends DataType>, ? super DataType>) change -> {
          if (change.wasAdded()) {
            // do nothing for now
          } else if (change.wasRemoved()) {
            // remove type columns to maps
            DataType type = change.getValueRemoved();
            this.remove((Class) type.getClass());
          }
        });
  }

  // NOT TESTED

  /**
   * Initializes a new feature using given values
   */
  @Deprecated
  public ModularFeature(ModularFeatureList flist, RawDataFile dataFile, double mz, float rt,
      float height, float area, Scan[] scanNumbers, DataPoint[] dataPointsPerScan,
      FeatureStatus featureStatus, Scan representativeScan, Scan fragmentScanNumber,
      Scan[] allMS2FragmentScanNumbers, @NotNull Range<Float> rtRange,
      @NotNull Range<Double> mzRange, @NotNull Range<Float> intensityRange) {
    this(flist, dataFile, mz, rt, height, area, Arrays.asList(scanNumbers),
        DataPointUtils.getMZsAsDoubleArray(dataPointsPerScan),
        DataPointUtils.getIntenstiesAsDoubleArray(dataPointsPerScan), featureStatus,
        representativeScan, fragmentScanNumber, allMS2FragmentScanNumbers, rtRange, mzRange,
        intensityRange);
  }

  public ModularFeature(ModularFeatureList flist, RawDataFile dataFile, double mz, float rt,
      float height, float area, List<Scan> scans, double[] mzs, double[] intensities,
      FeatureStatus featureStatus, Scan representativeScan, Scan fragmentScanNumber,
      Scan[] allMS2FragmentScanNumbers, @NotNull Range<Float> rtRange,
      @NotNull Range<Double> mzRange, @NotNull Range<Float> intensityRange) {
    this(flist);

    assert dataFile != null;
    assert scans != null;
    assert mzs != null;
    assert intensities != null;
    assert featureStatus != null;

    if (mzs.length != intensities.length) {
      throw new IllegalArgumentException(
          "Cannot create a ModularFeature instance with different number of mz and intensity values");
    }
    if (mzs.length != scans.size()) {
      throw new IllegalArgumentException(
          "Cannot create a ModularFeature instance with different number of data points and scans");
    }
    if (mzs.length == 0) {
      throw new IllegalArgumentException(
          "Cannot create a ModularFeature instance with no data points");
    }

    setFragmentScan(fragmentScanNumber);
    setRepresentativeScan(representativeScan);
    // add values to feature
//    set(ScanNumbersType.class, List.of(scanNumbers));
    set(RawFileType.class, dataFile);
    set(DetectionType.class, featureStatus);
    set(MZType.class, mz);
    set(RTType.class, rt);
    set(HeightType.class, height);
    set(AreaType.class, area);
    set(BestScanNumberType.class, representativeScan);

    // datapoints of feature
//    set(DataPointsType.class, Arrays.asList(dataPointsPerScan));
    SimpleIonTimeSeries featureData = new SimpleIonTimeSeries(flist.getMemoryMapStorage(), mzs,
        intensities, scans);
    set(FeatureDataType.class, featureData);

    // ranges
    set(MZRangeType.class, mzRange);
    set(RTRangeType.class, rtRange);
    set(IntensityRangeType.class, intensityRange);

    set(FragmentScanNumbersType.class, List.of(allMS2FragmentScanNumbers));

    float fwhm = QualityParameters.calculateFWHM(this);
    if (!Float.isNaN(fwhm)) {
      set(FwhmType.class, fwhm);
    }
    float tf = QualityParameters.calculateTailingFactor(this);
    if (!Float.isNaN(tf)) {
      set(TailingFactorType.class, tf);
    }
    float af = QualityParameters.calculateAsymmetryFactor(this);
    if (!Float.isNaN(af)) {
      set(AsymmetryFactorType.class, af);
    }
  }

  /**
   * Creates a new feature. The properties are determined via {@link FeatureDataUtils#recalculateIonSeriesDependingTypes(ModularFeature)}.
   *
   * @param flist         The feature list.
   * @param dataFile      The raw data file of this feature.
   * @param featureData   The {@link IonTimeSeries} of this feature.
   * @param featureStatus The feature status.
   */
  public ModularFeature(ModularFeatureList flist, RawDataFile dataFile,
      IonTimeSeries<? extends Scan> featureData, FeatureStatus featureStatus) {
    this(flist);
    assert dataFile != null;

    set(RawFileType.class, dataFile);
    set(DetectionType.class, featureStatus);
    set(FeatureDataType.class, featureData);

    FeatureDataUtils.recalculateIonSeriesDependingTypes(this);
  }

  /**
   * Copy constructor
   */
//  public ModularFeature(@NotNull Feature f) {
//    this((ModularFeatureList) Objects.requireNonNull(f.getFeatureList()), f);
//  }

  /**
   * Copy constructor with custom feature list
   */
  public ModularFeature(@NotNull ModularFeatureList flist, Feature f) {
    this(flist);
    if (f instanceof ModularFeature) {
      ((ModularFeature) f).stream().forEach(entry -> this.set(entry.getKey(), entry.getValue()));
    } else {
      // add values to feature
//      set(ScanNumbersType.class, f.getScanNumbers());
      set(RawFileType.class, (f.getRawDataFile()));
      set(DetectionType.class, (f.getFeatureStatus()));
      set(MZType.class, (f.getMZ()));
      set(RTType.class, (f.getRT()));
      set(HeightType.class, (f.getHeight()));
      set(AreaType.class, (f.getArea()));
      set(BestScanNumberType.class, (f.getRepresentativeScan()));
      set(BestFragmentScanNumberType.class, (f.getMostIntenseFragmentScan()));
      set(FragmentScanNumbersType.class, (f.getAllMS2FragmentScans()));

      // datapoints of feature
//      set(DataPointsType.class, f.getDataPoints());
//      if(f instanceof ModularFeature) {
//        set(FeatureDataType.class, ((ModularFeature)f).getFeatureData());
//      } else {
      double[][] dp = DataPointUtils.getDataPointsAsDoubleArray(f.getDataPoints());
      SimpleIonTimeSeries featureData = new SimpleIonTimeSeries(flist.getMemoryMapStorage(), dp[0],
          dp[1], f.getScanNumbers());
      set(FeatureDataType.class, featureData);
//      }

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

  public Node getBufferedColChart(String colname) {
    return buffertColCharts.get(colname);
  }

  public void addBufferedColChart(String colname, Node node) {
    buffertColCharts.put(colname, node);
  }

  @Override
  public <T> void set(Class<? extends DataType<T>> tclass, T value) {
    // type in defined columns?
    if (!getTypes().containsKey(tclass)) {
      try {
        DataType newType = tclass.getConstructor().newInstance();
        ModularFeatureList flist = (ModularFeatureList) getFeatureList();
        flist.addFeatureType(newType);
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

  // todo make this private?
  @Override
  public ObservableMap<DataType, Object> getMap() {
    return map;
  }

  /**
   * Use {@link ModularFeature#getFeatureData()} and {@link io.github.mzmine.datamodel.featuredata.IonSpectrumSeries#getIntensityForSpectrum(MassSpectrum)}
   * or {@link io.github.mzmine.datamodel.featuredata.IonSpectrumSeries#getIntensity} instead.
   *
   * @param scan
   * @return
   */
  @Override
  @Deprecated
  public DataPoint getDataPoint(Scan scan) {
    if (!scan.getDataFile().equals(getRawDataFile())) {
      throw new IllegalArgumentException("the scan data file must equal the feature data file");
    }
    int index = getScanNumbers().indexOf(scan);
    if (index < 0) {
      return null;
    }
    return new SimpleDataPoint(getFeatureData().getMZ(index), getFeatureData().getIntensity(index));
  }

  @NotNull
  @Override
  public Range<Float> getRawDataPointsRTRange() {
    ObjectProperty<Range<Float>> v = get(RTRangeType.class);
    return v == null || v.getValue() == null ? Range.singleton(0f) : v.getValue();
  }

  @NotNull
  @Override
  public Range<Double> getRawDataPointsMZRange() {
    ObjectProperty<Range<Double>> v = get(MZRangeType.class);
    return v == null || v.getValue() == null ? Range.singleton(0d) : v.getValue();
  }

  @NotNull
  @Override
  public Range<Float> getRawDataPointsIntensityRange() {
    ObjectProperty<Range<Float>> v = get(IntensityRangeType.class);
    return v == null || v.getValue() == null ? Range.singleton(0f) : v.getValue();
  }

  @Override
  public Scan getMostIntenseFragmentScan() {
    Property<Scan> v = get(BestFragmentScanNumberType.class);
    return v == null || v.getValue() == null ? null : v.getValue();
  }

  @Override
  public void setFragmentScan(Scan fragmentScan) {
    set(BestFragmentScanNumberType.class, fragmentScan);
  }

  @Override
  public ObservableList<Scan> getAllMS2FragmentScans() {
    ListProperty<Scan> v = get(FragmentScanNumbersType.class);
    return v == null || v.getValue() == null ? FXCollections
        .unmodifiableObservableList(FXCollections.emptyObservableList()) : v.getValue();
  }

  @Override
  public void setAllMS2FragmentScans(ObservableList<Scan> allMS2FragmentScanNumbers) {
    set(FragmentScanNumbersType.class, allMS2FragmentScanNumbers);
  }

  @Nullable
  @Override
  public IsotopePattern getIsotopePattern() {
    return get(IsotopePatternType.class);
  }

  @Override
  public void setIsotopePattern(@NotNull IsotopePattern isotopePattern) {
    set(IsotopePatternType.class, isotopePattern);
  }

  @Override
  public Integer getCharge() {
    Property<Integer> charge = get(ChargeType.class);
    return charge == null || charge.getValue() == null ? 0 : charge.getValue();
  }

  @Override
  public void setCharge(Integer charge) {
    set(ChargeType.class, charge);
  }

  @Override
  public Float getFWHM() {
    Property<Float> v = get(FwhmType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public void setFWHM(Float fwhm) {
    set(FwhmType.class, fwhm);
  }

  @Override
  public Float getTailingFactor() {
    Property<Float> v = get(TailingFactorType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public void setTailingFactor(Float tf) {
    set(TailingFactorType.class, tf);
  }

  @Override
  public Float getAsymmetryFactor() {
    Property<Float> v = get(AsymmetryFactorType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public void setAsymmetryFactor(Float af) {
    set(AsymmetryFactorType.class, af);
  }

  @Override
  public void outputChromToFile() {

  }

  @Override
  public SimpleFeatureInformation getFeatureInformation() {
    ObjectProperty<SimpleFeatureInformation> v = get(FeatureInformationType.class);
    return v == null ? null : v.getValue();
  }

  @Override
  public void setFeatureInformation(SimpleFeatureInformation featureInfo) {
    set(FeatureInformationType.class, featureInfo);
  }

  @Nullable
  @Override
  public FeatureList getFeatureList() {
    return flist;
  }

  @Override
  public void setFeatureList(@NotNull FeatureList flist) {
    this.flist = (ModularFeatureList) flist;
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

//  public ListProperty<Scan> getScanNumbersProperty() {
//    return get(ScanNumbersType.class);
//  }

//  public ListProperty<DataPoint> getDataPointsProperty() {
//    return get(DataPointsType.class);
//  }

  public Property<Float> getAreaProperty() {
    return get(AreaType.class);
  }

  /**
   * See {@link ModularFeature#getFeatureData()}
   *
   * @return
   */
  @NotNull
  @Override
  public List<Scan> getScanNumbers() {
    IonTimeSeries<? extends Scan> data = getFeatureData();
    return data == null ? Collections.emptyList() : (List<Scan>) data.getSpectra();
  }

  @Override
  public Scan getRepresentativeScan() {
    Property<Scan> v = get(BestScanNumberType.class);
    return v == null || v.getValue() == null ? null : v.getValue();
  }

  @Override
  public void setRepresentativeScan(Scan scan) {
    set(BestScanNumberType.class, scan);
  }

  /**
   * See {@link ModularFeature#getFeatureData()}
   *
   * @return
   */
  @Override
  @Deprecated
  public ObservableList<DataPoint> getDataPoints() {
//    ListProperty<DataPoint> v = get(DataPointsType.class);
//    return v == null || v.getValue() == null ?
//        FXCollections.unmodifiableObservableList(FXCollections.emptyObservableList())
//        : v.getValue();
    IonTimeSeries<? extends Scan> data = getFeatureData();
    return data == null ? null
        : FXCollections.observableArrayList(data.stream().collect(Collectors.toList()));
  }

  public IonTimeSeries<? extends Scan> getFeatureData() {
    ObjectProperty<IonTimeSeries<? extends Scan>> v = get(FeatureDataType.class);
    return v == null || v.getValue() == null ? null : v.getValue();
  }

  @Override
  public Float getRT() {
    Property<Float> v = get(RTType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public void setRT(float rt) {
    set(RTType.class, rt);
  }

  @NotNull
  @Override
  public FeatureStatus getFeatureStatus() {
    ObjectProperty<FeatureStatus> v = get(DetectionType.class);
    return v == null || v.getValue() == null ? FeatureStatus.UNKNOWN : v.getValue();
  }

  @Override
  public Double getMZ() {
    Property<Double> mz = get(MZType.class);
    return mz == null || mz.getValue() == null ? Double.NaN : mz.getValue();
  }

  @Override
  public void setMZ(Double mz) {
    set(MZType.class, mz);
  }

  public Float getHeight() {
    Property<Float> v = get(HeightType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public void setHeight(Float height) {
    set(HeightType.class, height);
  }

  public Float getArea() {
    Property<Float> v = get(AreaType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public void setArea(float area) {
    set(AreaType.class, area);
  }

  @Nullable
  @Override
  public Float getMobility() {
    Property<Float> v = get(io.github.mzmine.datamodel.features.types.numbers.MobilityType.class);
    return v == null || v.getValue() == null ? null : v.getValue();
  }

  @Override
  public void setMobility(Float mobility) {
    set(io.github.mzmine.datamodel.features.types.numbers.MobilityType.class, mobility);
  }

  @Nullable
  @Override
  public MobilityType getMobilityUnit() {
    Property<MobilityType> v = get(MobilityUnitType.class);
    return v == null || v.getValue() == null ? null : v.getValue();
  }

  @Override
  public void setMobilityUnit(MobilityType mobilityUnit) {
    set(MobilityUnitType.class, mobilityUnit);
  }

  @Override
  public Float getCCS() {
    Property<Float> v = get(CCSType.class);
    return v == null || v.getValue() == null ? null : v.getValue();
  }

  @Override
  public void setCCS(Float ccs) {
    set(CCSType.class, ccs);
  }

  @Nullable
  @Override
  public Range<Float> getMobilityRange() {
    ObjectProperty<Range<Float>> v = get(MobilityRangeType.class);
    return v == null || v.getValue() == null ? null : v.getValue();
  }

  @Override
  public void setMobilityRange(Range<Float> range) {
    set(MobilityRangeType.class, range);
  }
}
