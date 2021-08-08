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

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.FeatureInformationType;
import io.github.mzmine.datamodel.features.types.FeatureShapeIonMobilityRetentionTimeHeatMapType;
import io.github.mzmine.datamodel.features.types.IsotopePatternType;
import io.github.mzmine.datamodel.features.types.RawFileType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.BestFragmentScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.FragmentScanNumbersType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogram;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogrambuilder.Chromatogram;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvedPeak;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.IImage;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IIonMobilityTrace;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_manual.ManualFeature;
import io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder.TempIMTrace;
import io.github.mzmine.modules.dataprocessing.gapfill_samerange.SameRangePeak;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

public class FeatureConvertors {

  /**
   * Creates a ModularFeature on the basis of chromatogram results with the {@link
   * DataTypeUtils#addDefaultChromatographicTypeColumns(ModularFeatureList)} columns
   *
   * @param chromatogram input ADAP chromatogram
   * @return output modular feature
   */
  static public ModularFeature ADAPChromatogramToModularFeature(
      @NotNull ADAPChromatogram chromatogram) {

    if (chromatogram.getFeatureList() == null) {
      throw new NullPointerException("Feature list of the ADAP chromatogram is null.");
    }

    if (!(chromatogram.getFeatureList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from ADAP chromatogram of non-modular feature list.");
    }

    ModularFeature modularFeature = new ModularFeature(
        (ModularFeatureList) chromatogram.getFeatureList());

    modularFeature
        .set(FragmentScanNumbersType.class, List.of(chromatogram.getAllMS2FragmentScanNumbers()));
//    modularFeature.set(ScanNumbersType.class, List.of(chromatogram.getScanNumbers()));

    modularFeature
        .set(BestFragmentScanNumberType.class, chromatogram.getMostIntenseFragmentScanNumber());
    modularFeature.set(BestScanNumberType.class, chromatogram.getRepresentativeScanNumber());
    if (chromatogram.getIsotopePattern() != null) {
      modularFeature.set(IsotopePatternType.class, chromatogram.getIsotopePattern());
    }
    modularFeature.set(ChargeType.class, chromatogram.getCharge());

    modularFeature.set(RawFileType.class, chromatogram.getDataFile());
    modularFeature.set(DetectionType.class, chromatogram.getFeatureStatus());

    // Data points of feature
//    modularFeature.set(DataPointsType.class, new ArrayList<>(chromatogram.getDataPoints()));
    if (chromatogram.getDataPoints().size() != chromatogram.getScanNumbers().length) {
      throw new IllegalArgumentException(
          "Number of data points does not match number of scan numbers");
    }

    SimpleIonTimeSeries timeSeries = createSimpleTimeSeries(
        ((ModularFeatureList) chromatogram.getFeatureList()).getMemoryMapStorage(),
        chromatogram.getDataPoints().stream().collect(Collectors.toList()),
        Arrays.asList(chromatogram.getScanNumbers()));
    modularFeature.set(FeatureDataType.class, timeSeries);

    // recalculate data dependent types
    FeatureDataUtils.recalculateIonSeriesDependingTypes(modularFeature);

    ObservableList<Scan> allMS2 = Arrays.stream(ScanUtils
        .findAllMS2FragmentScans(chromatogram.getDataFile(),
            modularFeature.getRawDataPointsRTRange(), modularFeature.getRawDataPointsMZRange()))
        .collect(Collectors.toCollection(FXCollections::observableArrayList));
    modularFeature.setAllMS2FragmentScans(allMS2);

    return modularFeature;
  }

  public static ModularFeature IonMobilityIonTraceToModularFeature(
      @NotNull IIonMobilityTrace ionTrace, RawDataFile rawDataFile,
      BinningMobilogramDataAccess mobilogramBinner) {

    if (ionTrace.getFeatureList() == null) {
      throw new NullPointerException("Feature list of the ion trace is null.");
    }

    if (!(ionTrace.getFeatureList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from ion trace of non-modular feature list.");
    }

    final ModularFeature modularFeature = new ModularFeature(
        (ModularFeatureList) ionTrace.getFeatureList());

    // Add values to feature
    modularFeature.set(RawFileType.class, rawDataFile);
    modularFeature.set(DetectionType.class, FeatureStatus.DETECTED);
    modularFeature.setMobilityUnit(((IMSRawDataFile) rawDataFile).getMobilityType());

    final MemoryMapStorage storage = ((ModularFeatureList) ionTrace.getFeatureList())
        .getMemoryMapStorage();
    final List<IonMobilitySeries> mobilograms = new ArrayList<>();

    var sortedDp = FeatureConvertorIonMobility.groupDataPointsByFrameId(ionTrace.getDataPoints());
    for (Entry<Frame, SortedSet<RetentionTimeMobilityDataPoint>> entry : sortedDp.entrySet()) {
      double[][] data = DataPointUtils.getDataPointsAsDoubleArray(entry.getValue());
      SimpleIonMobilitySeries mobilogram = new SimpleIonMobilitySeries(storage, data[0], data[1],
          entry.getValue().stream().map(RetentionTimeMobilityDataPoint::getMobilityScan).toList());
      mobilograms.add(mobilogram);
    }

    final IonMobilogramTimeSeries imTimeSeries = IonMobilogramTimeSeriesFactory
        .of(storage, mobilograms, mobilogramBinner);
    modularFeature.set(FeatureDataType.class, imTimeSeries);

    // no need to calc quality parameters after feature detection.
    FeatureDataUtils
        .recalculateIonSeriesDependingTypes(modularFeature, FeatureDataUtils.DEFAULT_CENTER_MEASURE,
            false);

    return modularFeature;
  }

  public static ModularFeature tempIMTraceToModularFeature(@NotNull TempIMTrace ionTrace,
      RawDataFile rawDataFile, BinningMobilogramDataAccess mobilogramBinner,
      ModularFeatureList flist) {

    ModularFeature modularFeature = new ModularFeature(flist);

    // TODO
    modularFeature.set(RawFileType.class, rawDataFile);
    modularFeature.set(DetectionType.class, FeatureStatus.DETECTED);
    modularFeature.setMobilityUnit(((IMSRawDataFile) rawDataFile).getMobilityType());
    modularFeature.set(FeatureShapeIonMobilityRetentionTimeHeatMapType.class, false);

    MemoryMapStorage storage = flist.getMemoryMapStorage();
    IonMobilogramTimeSeries imTimeSeries = IonMobilogramTimeSeriesFactory
        .of(storage, ionTrace.getMobilograms(), mobilogramBinner);
    modularFeature.set(FeatureDataType.class, imTimeSeries);
    // no need to calc quality parameters after feature detection.
    FeatureDataUtils.recalculateIonSeriesDependingTypes(modularFeature, FeatureDataUtils.DEFAULT_CENTER_MEASURE, false);

    return modularFeature;
  }

  public static ModularFeature ImageToModularFeature(@NotNull IImage image,
      RawDataFile rawDataFile) {

    if (image.getFeatureList() == null) {
      throw new NullPointerException("Feature list of the image is null.");
    }

    if (!(image.getFeatureList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from image of non-modular feature list.");
    }

    ModularFeature modularFeature = new ModularFeature((ModularFeatureList) image.getFeatureList());

    // TODO
    modularFeature.setFragmentScan(null);
    modularFeature.setRepresentativeScan(null);
    // Add values to feature
    modularFeature.set(RawFileType.class, rawDataFile);
    modularFeature.set(DetectionType.class, FeatureStatus.DETECTED);
    modularFeature.set(MZType.class, image.getMz());
    modularFeature.set(RTType.class, (float) 0.f);

    modularFeature.set(HeightType.class, (float) image.getMaximumIntensity());
    // TODO
    modularFeature.set(AreaType.class, (float) 0);
    // TODO
    modularFeature.set(BestScanNumberType.class, null);

    // Data points of feature
    double[][] dp = DataPointUtils.getDataPointsAsDoubleArray(image.getDataPoints());
    SimpleIonTimeSeries data = new SimpleIonTimeSeries(
        ((ModularFeatureList) image.getFeatureList()).getMemoryMapStorage(), dp[0], dp[1],
        image.getScanNumbers().stream().collect(Collectors.toList()));
    modularFeature.set(FeatureDataType.class, data);

    // Ranges
    Range<Float> rtRange = Range.closed(0.f, 0.f);
    Range<Double> mzRange = Range
        .closed(image.getMzRange().lowerEndpoint(), image.getMzRange().upperEndpoint());
    Range<Float> intensityRange = Range
        .closed(image.getIntensityRange().lowerEndpoint().floatValue(),
            image.getIntensityRange().upperEndpoint().floatValue());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);
    // modularFeature.setAllMS2FragmentScanNumbers(IntStream
    // .of(ScanUtils.findAllMS2FragmentScans(chromatogram.getDataFile(), rtRange, mzRange)).boxed()
    // .collect(Collectors.toCollection(FXCollections::observableArrayList)));

    // Quality parameters
    // float fwhm = QualityParameters.calculateFWHM(modularFeature);
    // if (!Float.isNaN(fwhm)) {
    modularFeature.set(FwhmType.class, -1f);
    // }
    // float tf = QualityParameters.calculateTailingFactor(modularFeature);
    // if (!Float.isNaN(tf)) {
    modularFeature.set(TailingFactorType.class, -1f);
    // }
    // float af = QualityParameters.calculateAsymmetryFactor(modularFeature);
    // if (!Float.isNaN(af)) {
    modularFeature.set(AsymmetryFactorType.class, -1f);
    // }

    FeatureDataUtils.recalculateIonSeriesDependingTypes(modularFeature);

    return modularFeature;
  }


  /**
   * Creates a ModularFeature on the basis of manually picked feature {@link
   * ManualFeatureUtils#pickFeatureManually(RawDataFile, Range, Range)}
   *
   * @param manualFeature input manual feature
   * @return output modular feature
   */
  static public ModularFeature ManualFeatureToModularFeature(ModularFeatureList featureList,
      @NotNull ManualFeature manualFeature) {

    if (manualFeature.getFeatureList() == null) {
      throw new NullPointerException("Feature list of the manual feature is null.");
    }

    if (!(manualFeature.getFeatureList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from manual feature of non-modular feature list.");
    }

    // Add quality parameters to the feature list
    featureList.addFeatureType(new FwhmType());
    featureList.addFeatureType(new TailingFactorType());
    featureList.addFeatureType(new AsymmetryFactorType());

    ModularFeature modularFeature = new ModularFeature(featureList);

    modularFeature.setFragmentScan(manualFeature.getMostIntenseFragmentScanNumber());
    modularFeature.setRepresentativeScan(manualFeature.getRepresentativeScanNumber());
    // Add values to feature

    modularFeature
        .set(FragmentScanNumbersType.class, List.of(manualFeature.getAllMS2FragmentScanNumbers()));
//    modularFeature.set(ScanNumbersType.class, List.of(manualFeature.getScanNumbers()));

    modularFeature.set(RawFileType.class, manualFeature.getRawDataFile());
    modularFeature.set(DetectionType.class, manualFeature.getFeatureStatus());
    modularFeature.set(MZType.class, manualFeature.getMZ());
    modularFeature.set(RTType.class, manualFeature.getRT());
    modularFeature.set(HeightType.class, (float) manualFeature.getHeight());
    modularFeature.set(AreaType.class, (float) manualFeature.getArea());
    modularFeature.set(BestScanNumberType.class, manualFeature.getRepresentativeScanNumber());

    // Data points of feature
//    modularFeature.set(DataPointsType.class, new ArrayList<>(manualFeature.getDataPoints()));
    SimpleIonTimeSeries timeSeries = createSimpleTimeSeries(
        ((ModularFeatureList) manualFeature.getFeatureList()).getMemoryMapStorage(),
        manualFeature.getDataPoints().stream().collect(Collectors.toList()),
        Arrays.asList(manualFeature.getScanNumbers()));
    modularFeature.set(FeatureDataType.class, timeSeries);

    // Ranges
    Range<Float> rtRange = Range.closed(manualFeature.getRawDataPointsRTRange().lowerEndpoint(),
        manualFeature.getRawDataPointsRTRange().upperEndpoint());
    Range<Double> mzRange = Range.closed(manualFeature.getRawDataPointsMZRange().lowerEndpoint(),
        manualFeature.getRawDataPointsMZRange().upperEndpoint());
    Range<Float> intensityRange = Range
        .closed(manualFeature.getRawDataPointsIntensityRange().lowerEndpoint(),
            manualFeature.getRawDataPointsIntensityRange().upperEndpoint());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);

    // TODO this is controlled during feature deconvolution or with a module - do not get all MS2 this way
    // modularFeature.setAllMS2FragmentScanNumbers(IntStream.of(ScanUtils
    //    .findAllMS2FragmentScans(resolvedPeak.getRawDataFile(), rtRange, mzRange)).boxed()
    //    .collect(Collectors.toCollection(FXCollections::observableArrayList)));

    // Quality parameters
    float fwhm = QualityParameters.calculateFWHM(modularFeature);
    if (!Float.isNaN(fwhm)) {
      modularFeature.set(FwhmType.class, fwhm);
    }
    float tf = QualityParameters.calculateTailingFactor(modularFeature);
    if (!Float.isNaN(tf)) {
      modularFeature.set(TailingFactorType.class, tf);
    }
    float af = QualityParameters.calculateAsymmetryFactor(modularFeature);
    if (!Float.isNaN(af)) {
      modularFeature.set(AsymmetryFactorType.class, af);
    }

    return modularFeature;
  }

  public static io.github.mzmine.datamodel.features.Feature SameRangePeakToModularFeature(
      ModularFeatureList featureList, SameRangePeak sameRangePeak) {

    if (sameRangePeak.getPeakList() == null) {
      throw new NullPointerException("Feature list of the sameRangePeak is null.");
    }

    if (!(sameRangePeak.getPeakList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from sameRangePeak of non-modular feature list.");
    }

    ModularFeature modularFeature = new ModularFeature(featureList);

    modularFeature
        .set(FragmentScanNumbersType.class, List.of(sameRangePeak.getAllMS2FragmentScanNumbers()));
//    modularFeature.set(ScanNumbersType.class, List.of(sameRangePeak.getScanNumbers()));

    modularFeature
        .set(BestFragmentScanNumberType.class, sameRangePeak.getMostIntenseFragmentScanNumber());
    modularFeature.set(BestScanNumberType.class, sameRangePeak.getRepresentativeScanNumber());
    modularFeature.set(IsotopePatternType.class, sameRangePeak.getIsotopePattern());
    modularFeature.set(FeatureInformationType.class, sameRangePeak.getPeakInformation());
    modularFeature.set(ChargeType.class, sameRangePeak.getCharge());

    modularFeature.set(RawFileType.class, sameRangePeak.getRawDataFile());
    modularFeature.set(DetectionType.class, sameRangePeak.getFeatureStatus());
    modularFeature.set(MZType.class, sameRangePeak.getMZ());
    modularFeature.set(RTType.class, (float) sameRangePeak.getRT());
    modularFeature.set(HeightType.class, (float) sameRangePeak.getHeight());
    modularFeature.set(AreaType.class, (float) sameRangePeak.getArea());
    modularFeature.set(BestScanNumberType.class, sameRangePeak.getRepresentativeScanNumber());

    // Data points of feature
//    modularFeature.set(DataPointsType.class, new ArrayList<>(sameRangePeak.getDataPoints()));
    SimpleIonTimeSeries timeSeries = createSimpleTimeSeries(
        ((ModularFeatureList) sameRangePeak.getPeakList()).getMemoryMapStorage(),
        sameRangePeak.getDataPoints().stream().collect(Collectors.toList()),
        Arrays.asList(sameRangePeak.getScanNumbers()));
    modularFeature.set(FeatureDataType.class, timeSeries);

    // Ranges
    Range<Float> rtRange = Range.closed(sameRangePeak.getRawDataPointsRTRange().lowerEndpoint(),
        sameRangePeak.getRawDataPointsRTRange().upperEndpoint());
    Range<Double> mzRange = Range.closed(sameRangePeak.getRawDataPointsMZRange().lowerEndpoint(),
        sameRangePeak.getRawDataPointsMZRange().upperEndpoint());
    Range<Float> intensityRange = Range
        .closed(sameRangePeak.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            sameRangePeak.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);

    // TODO this is controlled during feature deconvolution or with a module - do not get all MS2 this way
    // modularFeature.setAllMS2FragmentScanNumbers(IntStream.of(ScanUtils
    //    .findAllMS2FragmentScans(resolvedPeak.getRawDataFile(), rtRange, mzRange)).boxed()
    //    .collect(Collectors.toCollection(FXCollections::observableArrayList)));

    // Quality parameters
    float fwhm = QualityParameters.calculateFWHM(modularFeature);
    if (!Float.isNaN(fwhm)) {
      modularFeature.set(FwhmType.class, fwhm);
    }
    float tf = QualityParameters.calculateTailingFactor(modularFeature);
    if (!Float.isNaN(tf)) {
      modularFeature.set(TailingFactorType.class, tf);
    }
    float af = QualityParameters.calculateAsymmetryFactor(modularFeature);
    if (!Float.isNaN(af)) {
      modularFeature.set(AsymmetryFactorType.class, af);
    }

    return modularFeature;
  }

  public static ModularFeature ChromatogramToModularFeature(ModularFeatureList featureList,
      Chromatogram sameRangePeak) {

    if (sameRangePeak.getPeakList() == null) {
      throw new NullPointerException("Feature list of the sameRangePeak is null.");
    }

    if (!(sameRangePeak.getPeakList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from sameRangePeak of non-modular feature list.");
    }

    ModularFeature modularFeature = new ModularFeature(featureList);

    modularFeature
        .set(FragmentScanNumbersType.class, List.of(sameRangePeak.getAllMS2FragmentScanNumbers()));
//    modularFeature.set(ScanNumbersType.class, List.of(sameRangePeak.getScanNumbers()));

    modularFeature
        .set(BestFragmentScanNumberType.class, sameRangePeak.getMostIntenseFragmentScanNumber());
    modularFeature.set(BestScanNumberType.class, sameRangePeak.getRepresentativeScanNumber());
    modularFeature.set(IsotopePatternType.class, sameRangePeak.getIsotopePattern());
    modularFeature.set(FeatureInformationType.class, sameRangePeak.getPeakInformation());
    modularFeature.set(ChargeType.class, sameRangePeak.getCharge());

    modularFeature.set(RawFileType.class, sameRangePeak.getRawDataFile());
    modularFeature.set(DetectionType.class, sameRangePeak.getFeatureStatus());
    modularFeature.set(MZType.class, sameRangePeak.getMZ());
    modularFeature.set(RTType.class, (float) sameRangePeak.getRT());
    modularFeature.set(HeightType.class, (float) sameRangePeak.getHeight());
    modularFeature.set(AreaType.class, (float) sameRangePeak.getArea());
    modularFeature.set(BestScanNumberType.class, sameRangePeak.getRepresentativeScanNumber());

    // Data points of feature
//    modularFeature.set(DataPointsType.class, new ArrayList<>(sameRangePeak.getDataPoints()));
    SimpleIonTimeSeries timeSeries = createSimpleTimeSeries(
        ((ModularFeatureList) sameRangePeak.getPeakList()).getMemoryMapStorage(),
        sameRangePeak.getDataPoints().stream().collect(Collectors.toList()),
        Arrays.asList(sameRangePeak.getScanNumbers()));
    modularFeature.set(FeatureDataType.class, timeSeries);

    // Ranges
    Range<Float> rtRange = Range.closed(sameRangePeak.getRawDataPointsRTRange().lowerEndpoint(),
        sameRangePeak.getRawDataPointsRTRange().upperEndpoint());
    Range<Double> mzRange = Range.closed(sameRangePeak.getRawDataPointsMZRange().lowerEndpoint(),
        sameRangePeak.getRawDataPointsMZRange().upperEndpoint());
    Range<Float> intensityRange = Range
        .closed(sameRangePeak.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            sameRangePeak.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);

    // TODO this is controlled during feature deconvolution or with a module - do not get all MS2 this way
    // modularFeature.setAllMS2FragmentScanNumbers(IntStream.of(ScanUtils
    //    .findAllMS2FragmentScans(resolvedPeak.getRawDataFile(), rtRange, mzRange)).boxed()
    //    .collect(Collectors.toCollection(FXCollections::observableArrayList)));

    // Quality parameters
    float fwhm = QualityParameters.calculateFWHM(modularFeature);
    if (!Float.isNaN(fwhm)) {
      modularFeature.set(FwhmType.class, fwhm);
    }
    float tf = QualityParameters.calculateTailingFactor(modularFeature);
    if (!Float.isNaN(tf)) {
      modularFeature.set(TailingFactorType.class, tf);
    }
    float af = QualityParameters.calculateAsymmetryFactor(modularFeature);
    if (!Float.isNaN(af)) {
      modularFeature.set(AsymmetryFactorType.class, af);
    }

    return modularFeature;
  }

  public static ModularFeature ResolvedPeakToMoularFeature(ModularFeatureList featureList,
      ResolvedPeak resolvedPeak, IonTimeSeries<? extends Scan> originalData) {

    if (resolvedPeak.getPeakList() == null) {
      throw new NullPointerException("Feature list of the resolvedPeak is null.");
    }

    if (!(resolvedPeak.getPeakList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from resolvedPeak of non-modular feature list.");
    }

    ModularFeature modularFeature = new ModularFeature(featureList);

    // Add values to feature
    modularFeature
        .set(FragmentScanNumbersType.class, List.of(resolvedPeak.getAllMS2FragmentScanNumbers()));
//    modularFeature.set(ScanNumbersType.class, List.of(resolvedPeak.getScanNumbers()));

    modularFeature
        .set(BestFragmentScanNumberType.class, resolvedPeak.getMostIntenseFragmentScanNumber());
    modularFeature.set(BestScanNumberType.class, resolvedPeak.getRepresentativeScanNumber());
    modularFeature.set(IsotopePatternType.class, resolvedPeak.getIsotopePattern());
    modularFeature.set(FeatureInformationType.class, resolvedPeak.getPeakInformation());
    modularFeature.set(ChargeType.class, resolvedPeak.getCharge());

    modularFeature.set(RawFileType.class, resolvedPeak.getRawDataFile());
    modularFeature.set(DetectionType.class, resolvedPeak.getFeatureStatus());
    modularFeature.set(MZType.class, resolvedPeak.getMZ());
    modularFeature.set(RTType.class, (float) resolvedPeak.getRT());
    modularFeature.set(HeightType.class, (float) resolvedPeak.getHeight());
    modularFeature.set(AreaType.class, (float) resolvedPeak.getArea());
    modularFeature.set(BestScanNumberType.class, resolvedPeak.getRepresentativeScanNumber());

    // Data points of feature
//    modularFeature.set(DataPointsType.class, resolvedPeak.getDataPoints());
//    SimpleIonTimeSeries timeSeries = createSimpleTimeSeries(
//        ((ModularFeatureList) resolvedPeak.getPeakList()).getMemoryMapStorage(),
//        resolvedPeak.getDataPoints().stream().collect(Collectors.toList()),
//        Arrays.asList(resolvedPeak.getScanNumbers()));
    IonTimeSeries<? extends Scan> resolvedData = null;
    if (originalData instanceof SimpleIonTimeSeries) {
      resolvedData = ((SimpleIonTimeSeries) originalData)
          .subSeries(featureList.getMemoryMapStorage(),
              Arrays.asList(resolvedPeak.getScanNumbers()));
    } else if (originalData instanceof IonMobilogramTimeSeries) {
      List<? extends Scan> scans = Arrays.asList(resolvedPeak.getScanNumbers());
      List<Frame> frames = (List<Frame>) scans;
      resolvedData = ((SimpleIonMobilogramTimeSeries) originalData)
          .subSeries(featureList.getMemoryMapStorage(), frames);
    } else {
      throw new IllegalArgumentException(
          "Smoothing is not yet supported for this kind of data. " + originalData.getClass()
              .getName());
    }
    modularFeature.set(FeatureDataType.class, resolvedData);

    // Ranges
    Range<Float> rtRange = Range.closed(resolvedPeak.getRawDataPointsRTRange().lowerEndpoint(),
        resolvedPeak.getRawDataPointsRTRange().upperEndpoint());
    Range<Double> mzRange = Range.closed(resolvedPeak.getRawDataPointsMZRange().lowerEndpoint(),
        resolvedPeak.getRawDataPointsMZRange().upperEndpoint());
    Range<Float> intensityRange = Range
        .closed(resolvedPeak.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            resolvedPeak.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);

    // TODO this is controlled during feature deconvolution or with a module - do not get all MS2 this way
    // modularFeature.setAllMS2FragmentScanNumbers(IntStream.of(ScanUtils
    //    .findAllMS2FragmentScans(resolvedPeak.getRawDataFile(), rtRange, mzRange)).boxed()
    //    .collect(Collectors.toCollection(FXCollections::observableArrayList)));

    // Quality parameters
    float fwhm = QualityParameters.calculateFWHM(modularFeature);
    if (!Float.isNaN(fwhm)) {
      modularFeature.set(FwhmType.class, fwhm);
    }
    float tf = QualityParameters.calculateTailingFactor(modularFeature);
    if (!Float.isNaN(tf)) {
      modularFeature.set(TailingFactorType.class, tf);
    }
    float af = QualityParameters.calculateAsymmetryFactor(modularFeature);
    if (!Float.isNaN(af)) {
      modularFeature.set(AsymmetryFactorType.class, af);
    }

    return modularFeature;
  }

  public static SimpleIonTimeSeries createSimpleTimeSeries(MemoryMapStorage storage,
      List<? extends DataPoint> dataPoints, List<? extends Scan> scans) {
    int numDp = dataPoints.size();
    double[] mzs = new double[numDp];
    double[] intensities = new double[numDp];
    List<Scan> scansList = new ArrayList<>();
    int i = 0;
    for (DataPoint dp : dataPoints) {
      mzs[i] = dp.getMZ();
      intensities[i] = dp.getIntensity();
      scansList.add(scans.get(i));
      i++;
    }

    SimpleIonTimeSeries timeSeries = new SimpleIonTimeSeries(storage, mzs, intensities, scansList);

    return timeSeries;
  }
}
