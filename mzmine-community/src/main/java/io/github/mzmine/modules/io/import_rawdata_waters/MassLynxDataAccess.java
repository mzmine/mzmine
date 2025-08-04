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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_waters;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MetadataOnlyScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.IMSImagingRawDataFileImpl;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleImagingFrame;
import io.github.mzmine.datamodel.impl.SimpleImagingScan;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFileImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesDataImpl;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.gui.preferences.WatersLockmassParameters;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_imzml.Coordinates;
import io.github.mzmine.modules.io.import_rawdata_mzml.ConversionUtils;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.ImagingRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MassLynxDataAccess implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(MassLynxDataAccess.class.getName());

  private final Arena arena = Arena.ofConfined();
  private final MemorySegment handle;
  @NotNull
  private final File rawFolder;
  @Nullable
  private final MemoryMapStorage storage;
  private final int numberOfFunctions;
  private final String acqDate;
  private final int analogChannelCount;
  private final FunctionType[] functionTypes;
  private final MassSpectrumType requestedSpectrumType;

  private final @Nullable ScanImportProcessorConfig processor;
  private final boolean isDdaFile;
  private final boolean isImsFile;
  private final boolean isImagingFile;
  private final MemorySegment scanInfoBuffer = arena.allocate(ScanInfo.layout());
  @Nullable
  private final ImagingMetadata metadata;
  private double[] mobilities = null;
  @Nullable
  private Float threshold = null;

  /**
   * contains doubles
   */
  private MemorySegment intensityBuffer = arena.allocate(0);
  /**
   * contains doubles
   */
  private MemorySegment mzBuffer = arena.allocate(0);

  /**
   * contains floats
   */
  private MemorySegment mrmRtBuffer = arena.allocate(0);
  /**
   * contains floats
   */
  private MemorySegment mrmIntensityBuffer = arena.allocate(0);

  /**
   * contains floats
   */
  private MemorySegment analogRtBuffer = arena.allocate(0);
  /**
   * contains floats
   */
  private MemorySegment analogIntensityBuffer = arena.allocate(0);

  public MassLynxDataAccess(@NotNull File rawFolder, boolean centroid,
      @Nullable MemoryMapStorage storage, @Nullable ScanImportProcessorConfig processor) {
    handle = MassLynxLib.openFile(arena.allocateFrom(rawFolder.getAbsolutePath()));

    this.rawFolder = rawFolder;
    this.storage = storage;
    this.processor = processor;

    isImsFile = MassLynxLib.isIonMobilityFile(handle) > 0;
    isDdaFile = MassLynxLib.isDdaFile(handle) > 0;
    numberOfFunctions = MassLynxLib.getNumberOfFunctions(handle);
    analogChannelCount = MassLynxLib.getAnalogChannelCount(handle);
    requestedSpectrumType = centroid ? MassSpectrumType.CENTROIDED : MassSpectrumType.PROFILE;

    final MemorySegment dateBuffer = arena.allocate(MassLynxLib.C_CHAR, 32);
    MassLynxLib.getAcquisitionDate(handle, dateBuffer, (int) dateBuffer.byteSize());
    acqDate = dateBuffer.getString(0, StandardCharsets.UTF_8);

    functionTypes = new FunctionType[numberOfFunctions];
    for (int i = 0; i < numberOfFunctions; i++) {
      functionTypes[i] = readFunctionType(i);
    }

    checkAndApplyLockMassCorrection(rawFolder);
    MassLynxLib.setCentroid(handle, centroid ? 1 : 0);

    // its possible that a file has coordinates, but does not have more than one coordinate.
    // In that case, import as LC file.
    if (MassLynxLib.isImagingFile(handle) > 0) {
      final var tempMetadata = new ImagingMetadata(this);
      if (tempMetadata.hasMoreThanOnePosition()) {
        metadata = tempMetadata;
        isImagingFile = true;
      } else {
        metadata = null;
        isImagingFile = false;
      }
    } else {
      metadata = null;
      isImagingFile = false;
    }
  }

  public @NotNull RawDataFileImpl createDataFile() {
    if (isImsFile) {
      if (isImagingFile && metadata != null && metadata.getImagingParameters() != null) {
        logger.finest(
            "Opening data file %s as IMS-MS imaging file.".formatted(rawFolder.getName()));
        final IMSImagingRawDataFileImpl imagingRawDataFile = new IMSImagingRawDataFileImpl(
            rawFolder.getName(), rawFolder.getAbsolutePath(), storage);
        imagingRawDataFile.setImagingParam(metadata.getImagingParameters());
        return imagingRawDataFile;
      } else {
        logger.finest("Opening data file %s as IMS-MS file.".formatted(rawFolder.getName()));
        return new IMSRawDataFileImpl(rawFolder.getName(), rawFolder.getAbsolutePath(), storage);
      }
    } else {
      if (isImagingFile && metadata != null && metadata.getImagingParameters() != null) {
        logger.finest("Opening data file %s as MS imaging file.".formatted(rawFolder.getName()));
        final ImagingRawDataFileImpl imagingRawDataFile = new ImagingRawDataFileImpl(
            rawFolder.getName(), rawFolder.getAbsolutePath(), storage);
        imagingRawDataFile.setImagingParam(metadata.getImagingParameters());
        return imagingRawDataFile;
      } else {
        logger.finest("Opening data file %s as MS file.".formatted(rawFolder.getName()));
        return new RawDataFileImpl(rawFolder.getName(), rawFolder.getAbsolutePath(), storage);
      }
    }
  }

  private void checkAndApplyLockMassCorrection(@NotNull File rawFolder) {
    final Boolean applyLockMass = ConfigService.getPreferences()
        .getValue(MZminePreferences.watersLockmass);

    if (!applyLockMass) {
      logger.finest("Ignoring lock mass correction for file " + rawFolder.getAbsolutePath());
    }

    final int lockmassFunction = MassLynxLib.getLockmassFunction(handle);

    if (lockmassFunction == MassLynxConstants.NO_LOCKMASS_FUNCTION) {
      logger.finest("Did not find a lock mass function for file: " + rawFolder.getAbsolutePath());
      return;
    } else {
      logger.finest("Found lock mass function " + lockmassFunction + " for file "
          + rawFolder.getAbsolutePath());
    }

    int applied = MassLynxLib.applyAutoLockmassCorrection(handle);
    if (applied > 0) {
      logger.finest("Applied auto lock mass correction to file " + rawFolder.getName());
      return;
    }

    final ScanInfoWrapper scanInfo = getScanInfo(lockmassFunction, 0, scanInfoBuffer);
    final PolarityType polarityType = scanInfo.polarityType();

    final WatersLockmassParameters lmParam = ConfigService.getPreferences()
        .getEmbeddedParameterValue(MZminePreferences.watersLockmass);
    final double lockmass = switch (polarityType) {
      case POSITIVE -> lmParam.getValue(WatersLockmassParameters.positive);
      case NEGATIVE -> lmParam.getValue(WatersLockmassParameters.negative);
      case UNKNOWN, NEUTRAL, ANY ->
          throw new RuntimeException("Cannot derive polarity of lock mass function.");
    };

    applied = MassLynxLib.applyCustomLockmassCorrection(handle, (float) lockmass);
    if (applied > 0) {
      logger.finest(
          "Applied lock mass correction to file " + rawFolder.getName() + " with lock mass "
              + lockmass);
      return;
    }

    logger.finest("No lock mass correction applied for file " + rawFolder.getName());
  }

  public int getNumberOfScansInFunction(int function) {
    return MassLynxLib.getNumberOfScansInFunction(handle, function);
  }

  public int getNumberOfScansInFile() {
    return MassLynxLib.getNumberOfScans(handle);
  }

  public FunctionType getFunctionType(int i) {
    return functionTypes[i];
  }

  public int getLockMassFunction() {
    return MassLynxLib.getLockmassFunction(handle);
  }

  private FunctionType readFunctionType(int function) {
    if (function >= numberOfFunctions) {
      throw new IndexOutOfBoundsException(
          "Function " + function + " > number of functions (" + numberOfFunctions + ")");
    }
    if (function == MassLynxLib.getLockmassFunction(handle)) {
      return FunctionType.LOCKMASS;
    }
    if (MassLynxLib.isIonMobilityFunction(handle, function) == 1) {
      return FunctionType.IMS_MS;
    }
    if (MassLynxLib.getNumberOfMrmsInFunction(handle, function) > 0) {
      return FunctionType.MRM;
    }
    if (MassLynxLib.isMsFunction(handle, function) > 0) {
      return FunctionType.MS;
    }
    return FunctionType.NOT_MS;
//    throw new RuntimeException(
//        "Unknown function in file " + rawFolder.getName() + " - function " + function);
  }

  public SimpleScan readScan(RawDataFileImpl file, int function, int scan) {
    return switch (getFunctionType(function)) {
      case IMS_MS -> {
        yield readFrame((IMSRawDataFileImpl) file, function, scan);
      }
      case MS -> {
        yield readMsScan(file, function, scan);
      }
      case LOCKMASS -> {
        yield readMsScan(file, function, scan);
//        throw new IllegalStateException("Attempted to load lock mass function " + function);
      }
      case MRM -> throw new IllegalStateException("MRM function, cannot read scan.");
      case NOT_MS -> throw new IllegalStateException("Non-MS function, cannot read scan.");
    };
  }

  private @Nullable SimpleScan readMsScan(RawDataFileImpl file, int function, int scan) {
    final ScanInfoWrapper scanInfo = getScanInfo(function, scan, scanInfoBuffer);
    final MetadataOnlyScan metadataScan = scanInfo.metadataOnlyScan();

    if (processor != null && processor.hasProcessors() && processor.scanFilter() != null
        && processor.scanFilter().isActiveFilter()) {
      final ScanSelection scanSelection = processor.scanFilter();
      if (!scanSelection.matches(metadataScan)) {
        return null;
      }
    }

    final int numDp = MassLynxLib.getDataPoints(handle, function, scan, mzBuffer, intensityBuffer,
        (int) mzBuffer.byteSize());
    if (numDp * MassLynxLib.C_DOUBLE.byteSize() > mzBuffer.byteSize()) {
      mzBuffer = arena.allocate(numDp * MassLynxLib.C_DOUBLE.byteSize() * 2);
      intensityBuffer = arena.allocate(numDp * MassLynxLib.C_DOUBLE.byteSize() * 2);
      MassLynxLib.getDataPoints(handle, function, scan, mzBuffer, intensityBuffer,
          (int) mzBuffer.byteSize());
    }

    final double[] mzs = StorageUtils.sliceDoubles(mzBuffer, 0, numDp)
        .toArray(MassLynxLib.C_DOUBLE);
    final double[] intensities = StorageUtils.sliceDoubles(intensityBuffer, 0, numDp)
        .toArray(MassLynxLib.C_DOUBLE);

    final SimpleSpectralArrays dataPoints;
    MassSpectrumType spectrumType =
        scanInfo.isProfile() > 0 ? MassSpectrumType.PROFILE : MassSpectrumType.CENTROIDED;
    if (processor != null && processor.isMassDetectActive(scanInfo.msLevel())) {
      final SimpleSpectralArrays simpleSpectralArrays = new SimpleSpectralArrays(mzs, intensities);
      dataPoints = processor.processor().processScan(metadataScan, simpleSpectralArrays);
      spectrumType = MassSpectrumType.CENTROIDED;
    } else {
      dataPoints = new SimpleSpectralArrays(mzs, intensities);
    }

    final String scanDefinition = "func=%d, scan=%d".formatted(function, scan);

    if (isImagingFile && metadata != null) {
      final Coordinates coordinates = metadata.getCoordinates(scanInfo);
      return new SimpleImagingScan(file, scan, scanInfo.msLevel(), scanInfo.rt(), 0, 0,
          dataPoints.mzs(), dataPoints.intensities(), spectrumType, scanInfo.polarityType(),
          scanDefinition, getAcquisitionMassRange(function), coordinates);
    } else {
      return new SimpleScan(file, scan, scanInfo.msLevel(), scanInfo.rt(),
          scanInfo.msLevel() > 1 ? scanInfo.msMsInfo(isDdaFile, isImsFile) : null, dataPoints.mzs(),
          dataPoints.intensities(), spectrumType, scanInfo.polarityType(), scanDefinition,
          getAcquisitionMassRange(function));
    }

  }

  public @NotNull ScanInfoWrapper getScanInfo(int function, int scan,
      MemorySegment scanInfoBuffer) {
    if (scanInfoBuffer.byteSize() < ScanInfo.layout().byteSize()) {
      throw new IllegalStateException(
          "Buffer size is not large enough to store a ScanInfo object.");
    }

    MassLynxLib.getScanInfo(handle, function, scan, scanInfoBuffer);
    final ScanInfoWrapper scanInfo = ScanInfoWrapper.fromScanInfo(scanInfoBuffer);
    return scanInfo;
  }

  public @Nullable SimpleFrame readFrame(IMSRawDataFileImpl file, int function, int scan) {
    final ScanInfoWrapper scanInfo = getScanInfo(function, scan, scanInfoBuffer);
    final MetadataOnlyScan metadataScan = scanInfo.metadataOnlyScan();

    if (processor != null && processor.hasProcessors() && processor.scanFilter() != null
        && processor.scanFilter().isActiveFilter()) {
      final ScanSelection scanSelection = processor.scanFilter();
      if (!scanSelection.matches(metadataScan)) {
        return null;
      }
    }

    // todo: maybe create convenience method to get threshold of mass detector, then we can threshold in c++
//    if(processor != null && processor.isMassDetectActive(scanInfo.msLevel())) {
//      MassLynxLib.setAbsoluteThreshold(handle, );
//    }

    final int numDp = MassLynxLib.getDataPoints(handle, function, scan, mzBuffer, intensityBuffer,
        (int) mzBuffer.byteSize());

    if (numDp * MassLynxLib.C_DOUBLE.byteSize() > mzBuffer.byteSize()) {
      mzBuffer = arena.allocate(numDp * MassLynxLib.C_DOUBLE.byteSize() * 2);
      intensityBuffer = arena.allocate(numDp * MassLynxLib.C_DOUBLE.byteSize() * 2);
      MassLynxLib.getDataPoints(handle, function, scan, mzBuffer, intensityBuffer,
          (int) mzBuffer.byteSize());
    }

    final double[] mzs = StorageUtils.sliceDoubles(mzBuffer, 0, numDp)
        .toArray(MassLynxLib.C_DOUBLE);
    final double[] intensities = StorageUtils.sliceDoubles(intensityBuffer, 0, numDp)
        .toArray(MassLynxLib.C_DOUBLE);

    final SimpleSpectralArrays dataPoints;
    MassSpectrumType spectrumType =
        scanInfo.isProfile() > 0 ? MassSpectrumType.PROFILE : MassSpectrumType.CENTROIDED;
    if (processor != null && processor.isMassDetectActive(scanInfo.msLevel())) {
      final SimpleSpectralArrays simpleSpectralArrays = new SimpleSpectralArrays(mzs, intensities);
      dataPoints = processor.processor().processScan(metadataScan, simpleSpectralArrays);
      spectrumType = MassSpectrumType.CENTROIDED;
    } else {
      dataPoints = new SimpleSpectralArrays(mzs, intensities);
    }

    final String scanDefinition = "func=%d, scan=%d".formatted(function, scan);
    final SimpleFrame frame;

    if (isImagingFile && metadata != null) {
      final Coordinates coordinates = metadata.getCoordinates(scanInfo);
      frame = new SimpleImagingFrame(file, scan, scanInfo.msLevel(), scanInfo.rt(),
          dataPoints.mzs(), dataPoints.intensities(), spectrumType, scanInfo.polarityType(),
          scanDefinition, getAcquisitionMassRange(function), MobilityType.TRAVELING_WAVE,
          scanInfo.msLevel() > 1 ? Set.of(
              (IonMobilityMsMsInfo) scanInfo.msMsInfo(isDdaFile, isImsFile)) : null, null);
      ((SimpleImagingFrame) frame).setCoordinates(coordinates);
    } else {
      frame = new SimpleFrame(file, scan, scanInfo.msLevel(), scanInfo.rt(), dataPoints.mzs(),
          dataPoints.intensities(), spectrumType, scanInfo.polarityType(), scanDefinition,
          getAcquisitionMassRange(function), MobilityType.TRAVELING_WAVE,
          scanInfo.msLevel() > 1 ? Set.of(
              (IonMobilityMsMsInfo) scanInfo.msMsInfo(isDdaFile, isImsFile)) : null, null);
    }

    final List<BuildingMobilityScan> mobScans = readMobilityScansForFrame(function, scan,
        scanInfo.driftScanCount(), metadataScan);

    frame.setMobilityScans(mobScans, false);
    frame.setMobilities(getMobilityValues(function));
    return frame;
  }

  private @NotNull List<BuildingMobilityScan> readMobilityScansForFrame(int function, int scan,
      int driftScanCount, @NotNull final MetadataOnlyScan metadataOnlyScan) {
    final List<BuildingMobilityScan> mobScans = new ArrayList<>();

//    final Instant mobScanLoadStart = Instant.now();
    for (int i = 0; i < driftScanCount; i++) {
      final int numMobScanDp = MassLynxLib.getMobilityScanDataPoints(handle, function, scan, i,
          mzBuffer, intensityBuffer, (int) mzBuffer.byteSize());

      if (numMobScanDp * MassLynxLib.C_DOUBLE.byteSize() > mzBuffer.byteSize()) {
        mzBuffer = arena.allocate(numMobScanDp * MassLynxLib.C_DOUBLE.byteSize() * 2);
        intensityBuffer = arena.allocate(numMobScanDp * MassLynxLib.C_DOUBLE.byteSize() * 2);
        MassLynxLib.getDataPoints(handle, function, scan, mzBuffer, intensityBuffer,
            (int) mzBuffer.byteSize());
      }

      final double[] mobScanMzs = StorageUtils.sliceDoubles(mzBuffer, 0, numMobScanDp)
          .toArray(MassLynxLib.C_DOUBLE);
      final double[] mobScanIntensities = StorageUtils.sliceDoubles(intensityBuffer, 0,
          numMobScanDp).toArray(MassLynxLib.C_DOUBLE);

      final SimpleSpectralArrays dataPoints;
      if (processor != null && processor.isMassDetectActive(metadataOnlyScan.getMSLevel())) {
        final SimpleSpectralArrays simpleSpectralArrays = new SimpleSpectralArrays(mobScanMzs,
            mobScanIntensities);
        dataPoints = processor.processor().processScan(metadataOnlyScan, simpleSpectralArrays);
      } else {
        dataPoints = new SimpleSpectralArrays(mobScanMzs, mobScanIntensities);
      }

      mobScans.add(new BuildingMobilityScan(i, dataPoints.mzs(), dataPoints.intensities()));
    }
//    final Instant mobScanLoadEnd = Instant.now();
//    Duration duration = Duration.between(mobScanLoadStart, mobScanLoadEnd);
//    final long millis = duration.toMillis();
//    logger.finest("Loaded %d  mobility scans in %s ms".formatted(mobScans.size(), millis));

    return mobScans;
  }

  private double[] getMobilityValues(int function) {
    if (mobilities != null) {
      return mobilities;
    }

    mobilities = new double[MassLynxLib.getNumberOfMobilityScans(handle, function)];
    for (int i = 0; i < mobilities.length; i++) {
      mobilities[i] = MassLynxLib.getMobility(handle, i);
    }
    return mobilities;
  }

  public OtherFeature readMrm(int function, int index,
      @NotNull OtherTimeSeriesData timeSeriesData) {
    assert getFunctionType(function) == FunctionType.MRM;

    final double q1 = MassLynxLib.getMrmQ1Mass(handle, function, index);
    final double q3 = MassLynxLib.getMrmQ3Mass(handle, function, index);
    final int numDp = MassLynxLib.getMrmDataPoints(handle, function, index, mrmRtBuffer,
        mrmIntensityBuffer, (int) mrmRtBuffer.byteSize());
    if (StorageUtils.numDoubles(mrmRtBuffer) < numDp) {
      mrmRtBuffer = arena.allocate(numDp * MassLynxLib.C_FLOAT.byteSize() * 2);
      mrmIntensityBuffer = arena.allocate(numDp * MassLynxLib.C_FLOAT.byteSize() * 2);
      MassLynxLib.getMrmDataPoints(handle, function, index, mrmRtBuffer, mrmIntensityBuffer,
          (int) mrmRtBuffer.byteSize());
    }
    final float[] rts = mrmRtBuffer.asSlice(0, numDp * MassLynxLib.C_FLOAT.byteSize())
        .toArray(MassLynxLib.C_FLOAT);
    final double[] intensities = ArrayUtils.floatToDouble(
        mrmIntensityBuffer.asSlice(0, numDp * MassLynxLib.C_FLOAT.byteSize())
            .toArray(MassLynxLib.C_FLOAT));

    final NumberFormats formats = ConfigService.getGuiFormats();
    final SimpleOtherTimeSeries series = new SimpleOtherTimeSeries(storage, rts, intensities,
        "func=%d id=%d %s -> %s".formatted(function, index, formats.mz(q1), formats.mz(q3)),
        timeSeriesData);

    return ConversionUtils.newRawMrmFeature(q1, q3, ActivationMethod.CID, null, series);
  }

  public int getNumberOfFunctions() {
    return numberOfFunctions;
  }

  public int getNumberOfMrmsInFunction(int function) {
    assert getFunctionType(function) == FunctionType.MRM;
    return MassLynxLib.getNumberOfMrmsInFunction(handle, function);
  }

  public @Nullable Range<Double> getAcquisitionMassRange(int function) {
    final double acquisitionRangeStart = MassLynxLib.getAcquisitionRangeStart(handle, function);
    final double acquisitionRangeEnd = MassLynxLib.getAcquisitionRangeEnd(handle, function);

    if (acquisitionRangeStart < acquisitionRangeEnd
        && Float.compare((float) acquisitionRangeStart, MassLynxConstants.DEFAULT_FLOAT) != 0) {
      return Range.closed(acquisitionRangeStart, acquisitionRangeEnd);
    }
    return null;
  }

  @Override
  public void close() throws Exception {
    MassLynxLib.closeFile(handle);
    arena.close();
  }

  public boolean isDdaFile() {
    return isDdaFile;
  }

  public boolean isImsFile() {
    return isImsFile;
  }

  public boolean isImagingFile() {
    return isImagingFile;
  }

  public void readAndAddAnalogChannels(RawDataFile file) {

    final Map<String, OtherTimeSeriesDataImpl> unitToData = new HashMap<>();
    final int bufferSize = 1024;
    final MemorySegment stringBuffer = arena.allocate(MassLynxLib.C_CHAR, bufferSize);

    int channelCounter = 0;
    AtomicInteger unitsCounter = new AtomicInteger();
    for (int channel = 0; channel < MassLynxLib.getAnalogChannelCount(handle); channel++) {
      int length = MassLynxLib.getAnalogChannelDescription(handle, channel, stringBuffer,
          bufferSize);
      final String description = stringBuffer.asSlice(0, length)
          .getString(0, StandardCharsets.UTF_8);

      length = MassLynxLib.getAnalogChannelUnits(handle, channel, stringBuffer, bufferSize);
      final String units = stringBuffer.asSlice(0, length).getString(0, StandardCharsets.UTF_8);

      int numDp = MassLynxLib.getAnalogDataPoints(handle, channel, analogRtBuffer,
          analogIntensityBuffer, (int) analogRtBuffer.byteSize());
      if (numDp * MassLynxLib.C_FLOAT.byteSize() > analogRtBuffer.byteSize()) {
        analogRtBuffer = arena.allocate(MassLynxLib.C_FLOAT, 2L * numDp);
        analogIntensityBuffer = arena.allocate(MassLynxLib.C_FLOAT, 2L * numDp);
        numDp = MassLynxLib.getAnalogDataPoints(handle, channel, analogRtBuffer,
            analogIntensityBuffer, (int) analogRtBuffer.byteSize());
      }

      final OtherTimeSeriesDataImpl timeSeriesData = unitToData.computeIfAbsent(units, u -> {
        final OtherDataFileImpl otherFile = new OtherDataFileImpl(file);
        final OtherTimeSeriesDataImpl tsd = new OtherTimeSeriesDataImpl(otherFile);
        otherFile.setOtherTimeSeriesData(tsd);
        otherFile.setDescription(u + "_Waters_Analog");
        tsd.setTimeSeriesRangeUnit(u);
        unitsCounter.getAndIncrement();
        return tsd;
      });

      final SimpleOtherTimeSeries trace = new SimpleOtherTimeSeries(storage,
          analogRtBuffer.asSlice(0, numDp * MassLynxLib.C_FLOAT.byteSize())
              .toArray(MassLynxLib.C_FLOAT), ConversionUtils.convertFloatsToDoubles(
          analogIntensityBuffer.asSlice(0, numDp * MassLynxLib.C_FLOAT.byteSize())
              .toArray(MassLynxLib.C_FLOAT)), channel + "_" + description, timeSeriesData);
      final OtherFeatureImpl feature = new OtherFeatureImpl(trace);
      timeSeriesData.addRawTrace(feature);
      channelCounter++;
    }

    logger.finest(
        "Added %d analog channels with %d different units to file %s".formatted(channelCounter,
            unitsCounter.get(), file.getName()));
    ((RawDataFileImpl) file).addOtherDataFiles(
        unitToData.values().stream().map(OtherTimeSeriesDataImpl::getOtherDataFile).toList());
  }

  @Nullable
  public Float getThreshold() {
    return threshold;
  }

  public void setThreshold(@Nullable Float threshold) {
    this.threshold = threshold;
    MassLynxLib.setAbsoluteThreshold(handle, Objects.requireNonNullElse(threshold, -1f));
  }
}
