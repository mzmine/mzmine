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

package io.github.mzmine.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.msdk.datamodel.Feature;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.RawFileType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.DataPointsType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.ScanNumbersType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogram;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogrambuilder.Chromatogram;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvedPeak;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IIonMobilityTrace;
import io.github.mzmine.modules.dataprocessing.featdet_manual.ManualFeature;
import io.github.mzmine.modules.dataprocessing.featdet_peakextender.ExtendedPeak;
import io.github.mzmine.modules.dataprocessing.gapfill_samerange.SameRangePeak;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.util.scans.ScanUtils;
import javafx.collections.FXCollections;

public class FeatureConvertors {

  /**
   * Creates a ModularFeature on the basis of chromatogram results with the
   * {@link DataTypeUtils#addDefaultChromatographicTypeColumns(ModularFeatureList)} columns
   *
   * @param chromatogram input ADAP chromatogram
   * @return output modular feature
   */
  static public ModularFeature ADAPChromatogramToModularFeature(
      @Nonnull ADAPChromatogram chromatogram) {

    if (chromatogram.getFeatureList() == null) {
      throw new NullPointerException("Feature list of the ADAP chromatogram is null.");
    }

    if (!(chromatogram.getFeatureList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from ADAP chromatogram of non-modular feature list.");
    }

    ModularFeature modularFeature =
        new ModularFeature((ModularFeatureList) chromatogram.getFeatureList());

    modularFeature.setFragmentScanNumber(chromatogram.getMostIntenseFragmentScanNumber());
    modularFeature.setRepresentativeScanNumber(chromatogram.getRepresentativeScanNumber());
    // Add values to feature
    int[] scans = chromatogram.getScanNumbers();
    modularFeature.set(ScanNumbersType.class,
        IntStream.of(scans).boxed().collect(Collectors.toList()));
    modularFeature.set(RawFileType.class, chromatogram.getDataFile());
    modularFeature.set(DetectionType.class, chromatogram.getFeatureStatus());
    modularFeature.set(MZType.class, chromatogram.getMZ());
    modularFeature.set(RTType.class, (float) chromatogram.getRT());
    modularFeature.set(HeightType.class, (float) chromatogram.getHeight());
    modularFeature.set(AreaType.class, (float) chromatogram.getArea());
    modularFeature.set(BestScanNumberType.class, chromatogram.getRepresentativeScanNumber());

    // Data points of feature
    List<DataPoint> dps = new ArrayList<>();
    for (int scan : scans) {
      dps.add(chromatogram.getDataPoint(scan));
    }
    modularFeature.set(DataPointsType.class, dps);

    // Ranges
    Range<Float> rtRange = Range.closed(chromatogram.getRawDataPointsRTRange().lowerEndpoint(),
        chromatogram.getRawDataPointsRTRange().upperEndpoint());
    Range<Double> mzRange = Range.closed(chromatogram.getRawDataPointsMZRange().lowerEndpoint(),
        chromatogram.getRawDataPointsMZRange().upperEndpoint());
    Range<Float> intensityRange =
        Range.closed(chromatogram.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            chromatogram.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);

    modularFeature.setAllMS2FragmentScanNumbers(IntStream
        .of(ScanUtils.findAllMS2FragmentScans(chromatogram.getDataFile(), rtRange, mzRange)).boxed()
        .collect(Collectors.toCollection(FXCollections::observableArrayList)));

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

  public static ModularFeature IonMobilityIonTraceToModularFeature(
      @Nonnull IIonMobilityTrace ionTrace, RawDataFile rawDataFile) {

    if (ionTrace.getFeatureList() == null) {
      throw new NullPointerException("Feature list of the ion trace is null.");
    }

    if (!(ionTrace.getFeatureList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from ion trace of non-modular feature list.");
    }

    ModularFeature modularFeature =
        new ModularFeature((ModularFeatureList) ionTrace.getFeatureList());

    // TODO
    modularFeature.setFragmentScanNumber(-1);
    modularFeature.setRepresentativeScanNumber(-1);
    // Add values to feature
    modularFeature.set(ScanNumbersType.class, new ArrayList<>(ionTrace.getScanNumbers()));
    modularFeature.set(RawFileType.class, rawDataFile);
    modularFeature.set(DetectionType.class, FeatureStatus.DETECTED);
    modularFeature.set(MZType.class, ionTrace.getMz());
    modularFeature.set(RTType.class, (float) ionTrace.getRetentionTime());

    modularFeature.set(HeightType.class, (float) ionTrace.getMaximumIntensity());
    // TODO
    modularFeature.set(AreaType.class, (float) 0);
    // TODO
    modularFeature.set(BestScanNumberType.class, -1);

    // Data points of feature
    List<DataPoint> dps = new ArrayList<>(ionTrace.getDataPoints());
    modularFeature.set(DataPointsType.class, dps);

    // Ranges
    Range<Float> rtRange = Range.closed(ionTrace.getRetentionTimeRange().lowerEndpoint(),
        ionTrace.getRetentionTimeRange().upperEndpoint());
    Range<Double> mzRange =
        Range.closed(ionTrace.getMzRange().lowerEndpoint(), ionTrace.getMzRange().upperEndpoint());
    Range<Float> intensityRange =
        Range.closed(ionTrace.getIntensityRange().lowerEndpoint().floatValue(),
            ionTrace.getIntensityRange().upperEndpoint().floatValue());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);
    // modularFeature.setAllMS2FragmentScanNumbers(IntStream
    // .of(ScanUtils.findAllMS2FragmentScans(chromatogram.getDataFile(), rtRange, mzRange)).boxed()
    // .collect(Collectors.toCollection(FXCollections::observableArrayList)));

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

  /**
   * Creates a ModularFeature on the basis of manually picked feature
   * {@link ManualFeatureUtils#pickFeatureManually(RawDataFile, Range, Range)}
   *
   * @param manualFeature input manual feature
   * @return output modular feature
   */
  static public ModularFeature ManualFeatureToModularFeature(@Nonnull ManualFeature manualFeature) {

    if (manualFeature.getFeatureList() == null) {
      throw new NullPointerException("Feature list of the manual feature is null.");
    }

    if (!(manualFeature.getFeatureList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from manual feature of non-modular feature list.");
    }

    ModularFeatureList featureList = (ModularFeatureList) manualFeature.getFeatureList();

    // Add quality parameters to the feature list
    featureList.addFeatureType(new FwhmType());
    featureList.addFeatureType(new TailingFactorType());
    featureList.addFeatureType(new AsymmetryFactorType());

    ModularFeature modularFeature = new ModularFeature(featureList);

    modularFeature.setFragmentScanNumber(manualFeature.getMostIntenseFragmentScanNumber());
    modularFeature.setRepresentativeScanNumber(manualFeature.getRepresentativeScanNumber());
    // Add values to feature
    int[] scans = manualFeature.getScanNumbers();
    modularFeature.set(ScanNumbersType.class,
        IntStream.of(scans).boxed().collect(Collectors.toList()));
    modularFeature.set(RawFileType.class, manualFeature.getRawDataFile());
    modularFeature.set(DetectionType.class, manualFeature.getFeatureStatus());
    modularFeature.set(MZType.class, manualFeature.getMZ());
    modularFeature.set(RTType.class, manualFeature.getRT());
    modularFeature.set(HeightType.class, (float) manualFeature.getHeight());
    modularFeature.set(AreaType.class, (float) manualFeature.getArea());
    modularFeature.set(BestScanNumberType.class, manualFeature.getRepresentativeScanNumber());

    // Data points of feature
    List<DataPoint> dps = new ArrayList<>();
    for (int scan : scans) {
      dps.add(manualFeature.getDataPoint(scan));
    }
    modularFeature.set(DataPointsType.class, dps);

    // Ranges
    Range<Float> rtRange = Range.closed(manualFeature.getRawDataPointsRTRange().lowerEndpoint(),
        manualFeature.getRawDataPointsRTRange().upperEndpoint());
    Range<Double> mzRange = Range.closed(manualFeature.getRawDataPointsMZRange().lowerEndpoint(),
        manualFeature.getRawDataPointsMZRange().upperEndpoint());
    Range<Float> intensityRange =
        Range.closed(manualFeature.getRawDataPointsIntensityRange().lowerEndpoint(),
            manualFeature.getRawDataPointsIntensityRange().upperEndpoint());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);

    modularFeature.setAllMS2FragmentScanNumbers(IntStream
        .of(ScanUtils.findAllMS2FragmentScans(manualFeature.getRawDataFile(), rtRange, mzRange))
        .boxed().collect(Collectors.toCollection(FXCollections::observableArrayList)));

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

  // TODO:
  static public ModularFeature MSDKFeatureToModularFeature(Feature msdkFeature,
      RawDataFile dataFile, FeatureStatus detected) {
    return null;
  }

  static public ModularFeature ExtendedPeakToModularFeature(ExtendedPeak chromatogram) {

    if (chromatogram.getPeakList() == null) {
      throw new NullPointerException("Feature list of the ExtendedPeak is null.");
    }

    if (!(chromatogram.getPeakList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from ExtendedPeak of non-modular feature list.");
    }

    ModularFeature modularFeature =
        new ModularFeature((ModularFeatureList) chromatogram.getPeakList());

    modularFeature.setFragmentScanNumber(chromatogram.getMostIntenseFragmentScanNumber());
    modularFeature.setRepresentativeScanNumber(chromatogram.getRepresentativeScanNumber());
    // Add values to feature
    int[] scans = chromatogram.getScanNumbers();
    modularFeature.set(ScanNumbersType.class,
        IntStream.of(scans).boxed().collect(Collectors.toList()));
    modularFeature.set(RawFileType.class, chromatogram.getRawDataFile());
    modularFeature.set(DetectionType.class, chromatogram.getFeatureStatus());
    modularFeature.set(MZType.class, chromatogram.getMZ());
    modularFeature.set(RTType.class, (float) chromatogram.getRT());
    modularFeature.set(HeightType.class, (float) chromatogram.getHeight());
    modularFeature.set(AreaType.class, (float) chromatogram.getArea());
    modularFeature.set(BestScanNumberType.class, chromatogram.getRepresentativeScanNumber());

    // Data points of feature
    List<DataPoint> dps = new ArrayList<>();
    for (int scan : scans) {
      dps.add(chromatogram.getDataPoint(scan));
    }
    modularFeature.set(DataPointsType.class, dps);

    // Ranges
    Range<Float> rtRange = Range.closed(chromatogram.getRawDataPointsRTRange().lowerEndpoint(),
        chromatogram.getRawDataPointsRTRange().upperEndpoint());
    Range<Double> mzRange = Range.closed(chromatogram.getRawDataPointsMZRange().lowerEndpoint(),
        chromatogram.getRawDataPointsMZRange().upperEndpoint());
    Range<Float> intensityRange =
        Range.closed(chromatogram.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            chromatogram.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);

    modularFeature.setAllMS2FragmentScanNumbers(IntStream
        .of(ScanUtils.findAllMS2FragmentScans(chromatogram.getRawDataFile(), rtRange, mzRange))
        .boxed().collect(Collectors.toCollection(FXCollections::observableArrayList)));

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
      SameRangePeak sameRangePeak) {

    if (sameRangePeak.getPeakList() == null) {
      throw new NullPointerException("Feature list of the sameRangePeak is null.");
    }

    if (!(sameRangePeak.getPeakList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from sameRangePeak of non-modular feature list.");
    }

    ModularFeature modularFeature =
        new ModularFeature((ModularFeatureList) sameRangePeak.getPeakList());

    modularFeature.setFragmentScanNumber(sameRangePeak.getMostIntenseFragmentScanNumber());
    modularFeature.setRepresentativeScanNumber(sameRangePeak.getRepresentativeScanNumber());
    // Add values to feature
    int[] scans = sameRangePeak.getScanNumbers();
    modularFeature.set(ScanNumbersType.class,
        IntStream.of(scans).boxed().collect(Collectors.toList()));
    modularFeature.set(RawFileType.class, sameRangePeak.getRawDataFile());
    modularFeature.set(DetectionType.class, sameRangePeak.getFeatureStatus());
    modularFeature.set(MZType.class, sameRangePeak.getMZ());
    modularFeature.set(RTType.class, (float) sameRangePeak.getRT());
    modularFeature.set(HeightType.class, (float) sameRangePeak.getHeight());
    modularFeature.set(AreaType.class, (float) sameRangePeak.getArea());
    modularFeature.set(BestScanNumberType.class, sameRangePeak.getRepresentativeScanNumber());

    // Data points of feature
    List<DataPoint> dps = new ArrayList<>();
    for (int scan : scans) {
      dps.add(sameRangePeak.getDataPoint(scan));
    }
    modularFeature.set(DataPointsType.class, dps);

    // Ranges
    Range<Float> rtRange = Range.closed(sameRangePeak.getRawDataPointsRTRange().lowerEndpoint(),
        sameRangePeak.getRawDataPointsRTRange().upperEndpoint());
    Range<Double> mzRange = Range.closed(sameRangePeak.getRawDataPointsMZRange().lowerEndpoint(),
        sameRangePeak.getRawDataPointsMZRange().upperEndpoint());
    Range<Float> intensityRange =
        Range.closed(sameRangePeak.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            sameRangePeak.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);

    modularFeature.setAllMS2FragmentScanNumbers(IntStream
        .of(ScanUtils.findAllMS2FragmentScans(sameRangePeak.getRawDataFile(), rtRange, mzRange))
        .boxed().collect(Collectors.toCollection(FXCollections::observableArrayList)));

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

  public static ModularFeature ChromatogramToModularFeature(Chromatogram sameRangePeak) {

    if (sameRangePeak.getPeakList() == null) {
      throw new NullPointerException("Feature list of the sameRangePeak is null.");
    }

    if (!(sameRangePeak.getPeakList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from sameRangePeak of non-modular feature list.");
    }

    ModularFeature modularFeature =
        new ModularFeature((ModularFeatureList) sameRangePeak.getPeakList());

    modularFeature.setFragmentScanNumber(sameRangePeak.getMostIntenseFragmentScanNumber());
    modularFeature.setRepresentativeScanNumber(sameRangePeak.getRepresentativeScanNumber());
    // Add values to feature
    int[] scans = sameRangePeak.getScanNumbers();
    modularFeature.set(ScanNumbersType.class,
        IntStream.of(scans).boxed().collect(Collectors.toList()));
    modularFeature.set(RawFileType.class, sameRangePeak.getRawDataFile());
    modularFeature.set(DetectionType.class, sameRangePeak.getFeatureStatus());
    modularFeature.set(MZType.class, sameRangePeak.getMZ());
    modularFeature.set(RTType.class, (float) sameRangePeak.getRT());
    modularFeature.set(HeightType.class, (float) sameRangePeak.getHeight());
    modularFeature.set(AreaType.class, (float) sameRangePeak.getArea());
    modularFeature.set(BestScanNumberType.class, sameRangePeak.getRepresentativeScanNumber());

    // Data points of feature
    List<DataPoint> dps = new ArrayList<>();
    for (int scan : scans) {
      dps.add(sameRangePeak.getDataPoint(scan));
    }
    modularFeature.set(DataPointsType.class, dps);

    // Ranges
    Range<Float> rtRange = Range.closed(sameRangePeak.getRawDataPointsRTRange().lowerEndpoint(),
        sameRangePeak.getRawDataPointsRTRange().upperEndpoint());
    Range<Double> mzRange = Range.closed(sameRangePeak.getRawDataPointsMZRange().lowerEndpoint(),
        sameRangePeak.getRawDataPointsMZRange().upperEndpoint());
    Range<Float> intensityRange =
        Range.closed(sameRangePeak.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            sameRangePeak.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);

    modularFeature.setAllMS2FragmentScanNumbers(IntStream
        .of(ScanUtils.findAllMS2FragmentScans(sameRangePeak.getRawDataFile(), rtRange, mzRange))
        .boxed().collect(Collectors.toCollection(FXCollections::observableArrayList)));

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

  public static io.github.mzmine.datamodel.features.Feature ResolvedPeakToMoularFeature(
      ResolvedPeak resolvedPeak) {

    if (resolvedPeak.getPeakList() == null) {
      throw new NullPointerException("Feature list of the resolvedPeak is null.");
    }

    if (!(resolvedPeak.getPeakList() instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Can not create modular feature from resolvedPeak of non-modular feature list.");
    }

    ModularFeature modularFeature =
        new ModularFeature((ModularFeatureList) resolvedPeak.getPeakList());

    modularFeature.setFragmentScanNumber(resolvedPeak.getMostIntenseFragmentScanNumber());
    modularFeature.setRepresentativeScanNumber(resolvedPeak.getRepresentativeScanNumber());
    // Add values to feature
    int[] scans = resolvedPeak.getScanNumbers();
    modularFeature.set(ScanNumbersType.class,
        IntStream.of(scans).boxed().collect(Collectors.toList()));
    modularFeature.set(RawFileType.class, resolvedPeak.getRawDataFile());
    modularFeature.set(DetectionType.class, resolvedPeak.getFeatureStatus());
    modularFeature.set(MZType.class, resolvedPeak.getMZ());
    modularFeature.set(RTType.class, (float) resolvedPeak.getRT());
    modularFeature.set(HeightType.class, (float) resolvedPeak.getHeight());
    modularFeature.set(AreaType.class, (float) resolvedPeak.getArea());
    modularFeature.set(BestScanNumberType.class, resolvedPeak.getRepresentativeScanNumber());

    // Data points of feature
    List<DataPoint> dps = new ArrayList<>();
    for (int scan : scans) {
      dps.add(resolvedPeak.getDataPoint(scan));
    }
    modularFeature.set(DataPointsType.class, dps);

    // Ranges
    Range<Float> rtRange = Range.closed(resolvedPeak.getRawDataPointsRTRange().lowerEndpoint(),
        resolvedPeak.getRawDataPointsRTRange().upperEndpoint());
    Range<Double> mzRange = Range.closed(resolvedPeak.getRawDataPointsMZRange().lowerEndpoint(),
        resolvedPeak.getRawDataPointsMZRange().upperEndpoint());
    Range<Float> intensityRange =
        Range.closed(resolvedPeak.getRawDataPointsIntensityRange().lowerEndpoint().floatValue(),
            resolvedPeak.getRawDataPointsIntensityRange().upperEndpoint().floatValue());
    modularFeature.set(MZRangeType.class, mzRange);
    modularFeature.set(RTRangeType.class, rtRange);
    modularFeature.set(IntensityRangeType.class, intensityRange);

    modularFeature.setAllMS2FragmentScanNumbers(IntStream
        .of(ScanUtils.findAllMS2FragmentScans(resolvedPeak.getRawDataFile(), rtRange, mzRange))
        .boxed().collect(Collectors.toCollection(FXCollections::observableArrayList)));

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
}
