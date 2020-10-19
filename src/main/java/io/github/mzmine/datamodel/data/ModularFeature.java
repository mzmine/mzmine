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
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.data.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.data.types.numbers.FwhmType;
import io.github.mzmine.datamodel.data.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.data.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.data.types.numbers.TailingFactorType;
import io.github.mzmine.datamodel.impl.SimplePeakInformation;
/*
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
 */
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.collections.ObservableList;
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
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import io.github.mzmine.datamodel.data.types.numbers.RTType;
import io.github.mzmine.datamodel.data.types.numbers.ScanNumbersType;
import io.github.mzmine.util.DataTypeUtils;
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
public class ModularFeature implements FeatureNew, ModularDataModel {

  private @Nonnull ModularFeatureList flist;
  private final ObservableMap<DataType, Property<?>> map =
      FXCollections.observableMap(new HashMap<>());

  // TODO: private variables to data types
  private SimplePeakInformation peakInfo;
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

  /**
   * Creates a ModularFeature on the basis of chromatogram results with the
   * {@link DataTypeUtils#addDefaultChromatographicTypeColumns(ModularFeatureList)} columns
   *
   * @param flist
   * @param p
   */
  public ModularFeature(@Nonnull ModularFeatureList flist, Feature p) {
    this(flist);

    fragmentScanNumber = p.getMostIntenseFragmentScanNumber();
    representiveScanNumber = p.getRepresentativeScanNumber();
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
    Range<Double> mzRange = Range.closed(p.getRawDataPointsMZRange().lowerEndpoint(),
        p.getRawDataPointsMZRange().upperEndpoint());
    Range<Float> intensityRange =
        Range.closed(p.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            p.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    set(MZRangeType.class, mzRange);
    set(RTRangeType.class, rtRange);
    set(IntensityRangeType.class, intensityRange);

    // TODO: findAllMS2FragmentScans rtRange parameter Range<Double> -> Range<Float>
    allMS2FragmentScanNumbers = FXCollections.observableArrayList(IntStream.of(ScanUtils.findAllMS2FragmentScans(p.getDataFile(),
        Range.closed(rtRange.lowerEndpoint().doubleValue(), rtRange.upperEndpoint().doubleValue()),
        mzRange)).boxed().collect(Collectors.toList()));

    // TODO:
    /*
    Float fwhm = QualityParameters.calculateFWHM(this);
    if(!fwhm.isNaN()) {
      set(FwhmType.class, fwhm);
    }
    Float tf = QualityParameters.calculateTailingFactor(this);
    if(!tf.isNaN()) {
      set(TailingFactorType.class, tf);
    }
    Float af = QualityParameters.calculateAsymmetryFactor(this);
    if(!af.isNaN()) {
      set(AsymmetryFactorType.class, af);
    }
    */
  }

  /**
   * Copy constructor
   */
  // TODO: calculations to p.get*()
  public ModularFeature(@Nonnull ModularFeatureList flist, ModularFeature p) {
    this(flist);

    // add values to feature
    int[] scans = (p.getScanNumbers()).stream().mapToInt(i -> i).toArray();
    set(ScanNumbersType.class, IntStream.of(scans).boxed().collect(Collectors.toList()));
    set(RawFileType.class, (p.getRawDataFile()));
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

    /*
    // ranges
    Range<Float> rtRange = Range.closed(p.getRawDataPointsRTRange().lowerEndpoint().floatValue(),
        p.getRawDataPointsRTRange().upperEndpoint().floatValue());
    Range<Float> intensityRange =
        Range.closed(p.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            p.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    //set(RTRangeType.class, rtRange);
    set(IntensityRangeType.class, intensityRange);
    */

    /*
    Float fwhm = QualityParameters.calculateFWHM(this);
    if(!fwhm.isNaN()) {
      set(FwhmType.class, fwhm);
    }
    Float tf = QualityParameters.calculateTailingFactor(this);
    if(!tf.isNaN()) {
      set(TailingFactorType.class, tf);
    }
    Float af = QualityParameters.calculateAsymmetryFactor(this);
    if(!af.isNaN()) {
      set(AsymmetryFactorType.class, af);
    }
    */
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
  public double getFWHM() {
    return 0;
  }

  @Override
  public double getTailingFactor() {
    return 0;
  }

  @Override
  public double getAsymmetryFactor() {
    return 0;
  }

  @Override
  public void setFWHM(double fwhm) {

  }

  @Override
  public void setTailingFactor(double tf) {

  }

  @Override
  public void setAsymmetryFactor(double af) {

  }

  @Override
  public void outputChromToFile() {

  }

  @Override
  public void setPeakInformation(SimplePeakInformation peakInfo) {
    this.peakInfo = peakInfo;
  }

  @Override
  public SimplePeakInformation getPeakInformation() {
    return peakInfo;
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
  public int getRepresentativeScanNumber() {
    return representiveScanNumber;
  }

  public ObservableList<DataPoint> getDataPoints() {
    return get(DataPointsType.class).getValue();
  }

  public float getRT() {
    return get(RTType.class).getValue();
  }

  @Nonnull
  @Override
  public FeatureStatus getFeatureStatus() {
    return null;
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
