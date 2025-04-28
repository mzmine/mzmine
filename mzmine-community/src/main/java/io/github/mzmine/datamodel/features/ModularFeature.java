/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.features;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureInformation;
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
import io.github.mzmine.datamodel.features.types.numbers.*;
import io.github.mzmine.datamodel.features.types.otherdectectors.MrmTransitionListType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.FeatureUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Feature with modular DataTypes
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ModularFeature implements Feature, ModularDataModel {

  private static final Logger logger = Logger.getLogger(ModularFeature.class.getName());
  private final ObservableMap<DataType, Object> map = FXCollections.observableMap(new HashMap<>());
  // buffert col charts and nodes
  @NotNull
  private final ModularFeatureList flist;

  private FeatureListRow parentRow;

  public ModularFeature(@NotNull ModularFeatureList flist) {
    this.flist = flist;

    //
    map.addListener((MapChangeListener<? super DataType, ? super Object>) change -> {
      if (change.wasAdded()) {
        this.flist.addFeatureType(change.getKey());
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
      FeatureStatus featureStatus, Scan representativeScan, List<Scan> allMS2FragmentScanNumbers,
      @NotNull Range<Float> rtRange, @NotNull Range<Double> mzRange,
      @NotNull Range<Float> intensityRange) {
    this(flist, dataFile, mz, rt, height, area, Arrays.asList(scanNumbers),
        DataPointUtils.getMZsAsDoubleArray(dataPointsPerScan),
        DataPointUtils.getIntenstiesAsDoubleArray(dataPointsPerScan), featureStatus,
        representativeScan, allMS2FragmentScanNumbers, rtRange, mzRange, intensityRange);
  }

  @Deprecated
  public ModularFeature(ModularFeatureList flist, RawDataFile dataFile, double mz, float rt,
      float height, float area, List<Scan> scans, double[] mzs, double[] intensities,
      FeatureStatus featureStatus, Scan representativeScan, List<Scan> allMS2FragmentScanNumbers,
      @NotNull Range<Float> rtRange, @NotNull Range<Double> mzRange,
      @NotNull Range<Float> intensityRange) {
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

    setAllMS2FragmentScans(allMS2FragmentScanNumbers);

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
   * Creates a new feature.
   *
   * @param flist    The feature list.
   * @param dataFile The raw data file of this feature.
   */
  public ModularFeature(ModularFeatureList flist, RawDataFile dataFile,
      FeatureStatus featureStatus) {
    this(flist);
    assert dataFile != null;

    set(RawFileType.class, dataFile);
    set(DetectionType.class, featureStatus);
  }

  /**
   * Creates a new feature. The properties are determined via
   * {@link FeatureDataUtils#recalculateIonSeriesDependingTypes(ModularFeature)}.
   *
   * @param flist         The feature list.
   * @param dataFile      The raw data file of this feature.
   * @param featureData   The {@link IonTimeSeries} of this feature.
   * @param featureStatus The feature status.
   */
  public ModularFeature(ModularFeatureList flist, RawDataFile dataFile,
      @Nullable IonTimeSeries<? extends Scan> featureData, FeatureStatus featureStatus) {
    this(flist, dataFile, featureStatus);

    if (featureData != null) {
      set(FeatureDataType.class, featureData);
      FeatureDataUtils.recalculateIonSeriesDependingTypes(this);
    }
  }

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
      setAllMS2FragmentScans(f.getAllMS2FragmentScans());

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

  /**
   * Maps listeners to their {@link DataType}s. Default returns an empty list.
   */
  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners() {
    return getFeatureList().getFeatureTypeChangeListeners();
  }

  @Override
  public Set<DataType> getTypes() {
    return flist.getFeatureTypes();
  }

  // todo make this private?
  @Override
  public ObservableMap<DataType, Object> getMap() {
    return map;
  }

  /**
   * Use {@link ModularFeature#getFeatureData()} and
   * {@link
   * io.github.mzmine.datamodel.featuredata.IonSpectrumSeries#getIntensityForSpectrum(MassSpectrum)}
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
    Range<Float> v = get(RTRangeType.class);
    return v == null ? Range.singleton(0f) : v;
  }

  @NotNull
  @Override
  public Range<Double> getRawDataPointsMZRange() {
    Range<Double> v = get(MZRangeType.class);
    return v == null ? Range.singleton(0d) : v;
  }

  @NotNull
  @Override
  public Range<Float> getRawDataPointsIntensityRange() {
    Range<Float> v = get(IntensityRangeType.class);
    return v == null ? Range.singleton(0f) : v;
  }

  @Override
  public Scan getMostIntenseFragmentScan() {
    List<Scan> allMS2 = get(FragmentScanNumbersType.class);
    if (allMS2 != null && !allMS2.isEmpty()) {
      return allMS2.get(0);
    } else {
      return null;
    }
  }

  @Override
  @NotNull
  public List<Scan> getAllMS2FragmentScans() {
    // return empty list instead
    return Objects.requireNonNullElse(get(FragmentScanNumbersType.class), List.of());
  }

  @Override
  public void setAllMS2FragmentScans(List<Scan> allFragmentScans) {
    boolean empty = allFragmentScans == null || allFragmentScans.isEmpty();
    set(FragmentScanNumbersType.class, empty ? null : allFragmentScans);
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
    Integer charge = get(ChargeType.class);
    return charge == null ? 0 : charge;
  }

  @Override
  public void setCharge(Integer charge) {
    set(ChargeType.class, charge);
  }

  @Override
  public Float getFWHM() {
    return get(FwhmType.class);
  }

  @Override
  public void setFWHM(Float fwhm) {
    set(FwhmType.class, fwhm);
  }

  @Override
  public Float getTailingFactor() {
    return get(TailingFactorType.class);
  }

  @Override
  public void setTailingFactor(Float tf) {
    set(TailingFactorType.class, tf);
  }

  @Override
  public Float getAsymmetryFactor() {
    return get(AsymmetryFactorType.class);
  }

  @Override
  public void setAsymmetryFactor(Float af) {
    set(AsymmetryFactorType.class, af);
  }

  @Override
  public void outputChromToFile() {

  }

  @Override
  public FeatureInformation getFeatureInformation() {
    return get(FeatureInformationType.class);
  }

  @Override
  public void setFeatureInformation(FeatureInformation featureInfo) {
    set(FeatureInformationType.class, featureInfo);
  }

  @Override
  public @NotNull FeatureList getFeatureList() {
    return flist;
  }

  @Override
  public int getNumberOfDataPoints() {
    final IonTimeSeries<? extends Scan> data = getFeatureData();
    return data == null ? 0 : data.getNumberOfValues();
  }

  @Nullable
  @Override
  public RawDataFile getRawDataFile() {
    return get(RawFileType.class);
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
    return data == null ? List.of() : (List<Scan>) data.getSpectra();
  }


  @Override
  public Scan getRepresentativeScan() {
    return get(BestScanNumberType.class);
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
  public List<DataPoint> getDataPoints() {
    IonTimeSeries<? extends Scan> data = getFeatureData();
    return data == null ? null
        : FXCollections.observableArrayList(data.stream().collect(Collectors.toList()));
  }

  @Override
  public @Nullable DataPoint getDataPointAtIndex(int i) {
    if (i < 0) {
      return null;
    }
    IonTimeSeries<? extends Scan> data = getFeatureData();
    return data == null || i >= data.getNumberOfValues() ? null
        : new SimpleDataPoint(data.getMZ(i), data.getIntensity(i));
  }

  public IonTimeSeries<? extends Scan> getFeatureData() {
    return get(FeatureDataType.class);
  }

  @Override
  public @Nullable FeatureListRow getRow() {
    return parentRow;
  }

  public void setRow(FeatureListRow row) {
    parentRow = row;
  }

  @Override
  public Float getRT() {
    return get(RTType.class);
  }

  @Override
  public void setRT(float rt) {
    set(RTType.class, rt);
  }

  @Override
  public Float getRI() {
    return get(RIType.class);
  }

  @Override
  public void setRI(float ri) {
    set(RIType.class, ri);
  }

  @NotNull
  @Override
  public FeatureStatus getFeatureStatus() {
    FeatureStatus v = get(DetectionType.class);
    return v == null ? FeatureStatus.UNKNOWN : v;
  }

  @Override
  public Double getMZ() {
    return get(MZType.class);
  }

  @Override
  public void setMZ(Double mz) {
    set(MZType.class, mz);
  }

  public Float getHeight() {
    return get(HeightType.class);
  }

  @Override
  public void setHeight(Float height) {
    set(HeightType.class, height);
  }

  public Float getArea() {
    return get(AreaType.class);
  }

  @Override
  public void setArea(float area) {
    set(AreaType.class, area);
  }

  @Nullable
  @Override
  public Float getMobility() {
    return get(io.github.mzmine.datamodel.features.types.numbers.MobilityType.class);
  }

  @Override
  public void setMobility(Float mobility) {
    set(io.github.mzmine.datamodel.features.types.numbers.MobilityType.class, mobility);
  }

  @Nullable
  @Override
  public MobilityType getMobilityUnit() {
    return get(MobilityUnitType.class);
  }

  @Override
  public void setMobilityUnit(MobilityType mobilityUnit) {
    set(MobilityUnitType.class, mobilityUnit);
  }

  @Override
  public Float getCCS() {
    return get(CCSType.class);
  }

  @Override
  public void setCCS(Float ccs) {
    set(CCSType.class, ccs);
  }

  @Nullable
  @Override
  public Range<Float> getMobilityRange() {
    return get(MobilityRangeType.class);
  }

  @Override
  public void setMobilityRange(Range<Float> range) {
    set(MobilityRangeType.class, range);
  }

  @Override
  public String toString() {
    return FeatureUtils.featureToString(this);
  }

  @Override
  public boolean isMrm() {
    return get(MrmTransitionListType.class) != null;
  }
}
