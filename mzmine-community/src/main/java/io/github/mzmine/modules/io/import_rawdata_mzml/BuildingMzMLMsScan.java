/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MetadataOnlyScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MobilitySpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.MsProcessor;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_mzml.binary.MzMLBinaryDataInfo;
import io.github.mzmine.modules.io.import_rawdata_mzml.binary.MzMLPeaksDecoder;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLCV;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLCVGroup;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLCVParam;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLMobility;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLPrecursorElement;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLPrecursorList;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLProductList;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLScan;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLScanList;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLScanWindow;
import io.github.mzmine.modules.io.import_rawdata_mzml.model.MzMLScanWindowList;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * <p>
 * MzMLMsScan class.
 * </p>
 */
public class BuildingMzMLMsScan extends MetadataOnlyScan {

  private static final Logger logger = Logger.getLogger(BuildingMzMLMsScan.class.getName());

  private final MzMLCVGroup cvParams;
  private final MzMLPrecursorList precursorList;
  private final MzMLProductList productList;
  private final MzMLScanList scanList;
  private final @NotNull String id;
  private final int scanNumber;
  private MassSpectrumType spectrumType;
  private Float retentionTime;
  private @Nullable MzMLMobility mobility;
  private Range<Double> mzRange;
  private Range<Double> mzScanWindowRange;

  // temporary - set to null after load
  private MzMLBinaryDataInfo mzBinaryDataInfo;
  private MzMLBinaryDataInfo intensityBinaryDataInfo;
  private MzMLBinaryDataInfo wavelengthBinaryDataInfo;
  private MzMLBinaryDataInfo mobilityBinaryDataInfo;

  //Final memory-mapped processed data
  //No intermediate results
  /**
   * doubles
   */
  private @Nullable MemorySegment mzValues;
  /**
   * doubles
   */
  private @Nullable MemorySegment intensityValues;
  /**
   * doubles
   */
  private @Nullable MemorySegment wavelengthValues;

  // mobility scans are memory mapped later
  private @Nullable SimpleSpectralArrays mobilityScanSimpleSpectralData;


  /**
   * @param id         the Scan ID
   * @param scanNumber the Scan Number
   */
  BuildingMzMLMsScan(@NotNull final String id, final int scanNumber) {
    this.cvParams = new MzMLCVGroup();
    this.precursorList = new MzMLPrecursorList();
    this.productList = new MzMLProductList();
    this.scanList = new MzMLScanList();
    this.id = id;
    this.scanNumber = scanNumber;
  }

  public MzMLCVGroup getCVParams() {
    return cvParams;
  }

  /**
   * Binary data that is loaded only if scan is in selection
   *
   * @return a {@link MzMLBinaryDataInfo} object.
   */
  public MzMLBinaryDataInfo getMzBinaryDataInfo() {
    return mzBinaryDataInfo;
  }

  /**
   * Binary data that is loaded only if scan is in selection
   *
   * @param mzBinaryDataInfo a {@link MzMLBinaryDataInfo} object.
   */
  public void setMzBinaryDataInfo(MzMLBinaryDataInfo mzBinaryDataInfo) {
    this.mzBinaryDataInfo = mzBinaryDataInfo;
  }

  /**
   * Binary data that is loaded only if scan is in selection
   *
   * @return a {@link MzMLBinaryDataInfo} object.
   */
  public MzMLBinaryDataInfo getIntensityBinaryDataInfo() {
    return intensityBinaryDataInfo;
  }

  /**
   * Binary data that is loaded only if scan is in selection
   *
   * @param intensityBinaryDataInfo a {@link MzMLBinaryDataInfo} object.
   */
  public void setIntensityBinaryDataInfo(MzMLBinaryDataInfo intensityBinaryDataInfo) {
    this.intensityBinaryDataInfo = intensityBinaryDataInfo;
  }

  public MzMLPrecursorList getPrecursorList() {
    return precursorList;
  }

  public MzMLProductList getProductList() {
    return productList;
  }

  public MzMLScanList getScanList() {
    return scanList;
  }

  public @NotNull String getId() {
    return id;
  }

  public MemorySegment getDoubleBufferMzValues() {
    if (mzValues == null) {
      throw new UnsupportedOperationException(
          "No data yet. Call load method to load data and memory map the scan.");
    }
    return this.mzValues;
  }


  public MemorySegment getDoubleBufferIntensityValues() {
    if (intensityValues == null) {
      throw new UnsupportedOperationException(
          "No data yet. Call load method to load data and memory map the scan.");
    }
    return this.intensityValues;
  }

  @Override
  public int getNumberOfDataPoints() {
    if (intensityValues == null) {
      throw new UnsupportedOperationException(
          "No data yet. Call load method to load data and memory map the scan.");
    }
    return (int) StorageUtils.numDoubles(intensityValues);
  }

  @Override
  public double getTIC() {
    if (intensityValues == null) {
      throw new UnsupportedOperationException(
          "No data yet. Call load method to load data and memory map the scan.");
    }
    // usually not called on an mzml scan, dont cache and dont precompute
    return Arrays.stream(getIntensityValues(new double[getNumberOfDataPoints()])).sum();
  }

  @Override
  public double[] getMzValues(@NotNull final double[] dst) {
    throw new UnsupportedOperationException("Use double buffers directly instead");
  }

  @Override
  public double[] getIntensityValues(@NotNull final double[] dst) {
    throw new UnsupportedOperationException("Use double buffers directly instead");
  }

  @Override
  public @NotNull MassSpectrumType getSpectrumType() {
    if (spectrumType != null) {
      return spectrumType;
    }
    if (getCVValue(MzMLCV.cvCentroidSpectrum).isPresent()) {
      spectrumType = MassSpectrumType.CENTROIDED;
    }

    if (getCVValue(MzMLCV.cvProfileSpectrum).isPresent()) {
      spectrumType = MassSpectrumType.PROFILE;
    }
    // sometimes not set for UV data
    if (isUVSpectrum()) {
      return MassSpectrumType.PROFILE;
    }

    // cannot run on data here as it's not necessarily loaded
    return spectrumType;
  }

  @Override
  public @Nullable Range<Double> getScanningMZRange() {
    if (mzScanWindowRange == null) {
      if (!getScanList().getScans().isEmpty()) {
        Optional<MzMLScanWindowList> scanWindowList = getScanList().getScans().get(0)
            .getScanWindowList();
        if (scanWindowList.isPresent() && !scanWindowList.get().getScanWindows().isEmpty()) {
          MzMLScanWindow scanWindow = scanWindowList.get().getScanWindows().get(0);
          Optional<String> cvv = getCVValue(scanWindow, MzMLCV.cvScanWindowLowerLimit);
          Optional<String> cvv1 = getCVValue(scanWindow, MzMLCV.cvScanWindowUpperLimit);
          if (cvv.isEmpty() || cvv1.isEmpty()) {
            mzScanWindowRange = getDataPointMZRange();
          } else {
            try {
              mzScanWindowRange = Range.closed(Double.valueOf(cvv.get()),
                  Double.valueOf(cvv1.get()));
            } catch (NumberFormatException e) {
              throw (new IllegalArgumentException(
                  "Could not convert scan window range value in mzML file to a double", e));
            }
          }
        }
      }
    }

    return Objects.requireNonNullElse(mzScanWindowRange, Range.all());
  }

  @Override
  public @NotNull Range<Double> getDataPointMZRange() {
    if (mzRange == null) {
      Optional<String> cvv = getCVValue(MzMLCV.cvLowestMz);
      Optional<String> cvv1 = getCVValue(MzMLCV.cvHighestMz);
      if (cvv.isEmpty() || cvv1.isEmpty()) {
        // mz values is null if data was not loaded yet
        // note: mzBinaryDataInfo is cleared after loading (see clearUnusedData()), so use the
        // decoded array's own length instead
        if (mzValues != null && mzValues.byteSize() > 0) {
          final long last = mzValues.byteSize() / Double.BYTES - 1;
          mzRange = Range.closed(mzValues.getAtIndex(ValueLayout.JAVA_DOUBLE, 0),
              mzValues.getAtIndex(ValueLayout.JAVA_DOUBLE, last));
        }
      } else {
        try {
          mzRange = Range.closed(Double.valueOf(cvv.get()), Double.valueOf(cvv1.get()));
        } catch (NumberFormatException e) {
          throw (new IllegalArgumentException(
              "Could not convert mz range value in mzML file to a double", e));
        }
      }
    }
    return Objects.requireNonNullElse(mzRange, Range.all());
  }

  @Override
  public RawDataFile getDataFile() {
    return null;
  }

  @Override
  public int getScanNumber() {
    return scanNumber;
  }

  @Override
  public String getScanDefinition() {
    Optional<String> scanDefinition = Optional.empty();
    if (!getScanList().getScans().isEmpty()) {
      scanDefinition = getCVValue(getScanList().getScans().get(0), MzMLCV.cvScanFilterString);
    }
    return scanDefinition.orElse("");
  }

  @Override
  public int getMSLevel() {
    int msLevel = 1;
    Optional<String> value = getCVValue(MzMLCV.cvMSLevel);
    if (value.isPresent()) {
      msLevel = Integer.parseInt(value.get());
    }
    return msLevel;
  }


  @NotNull
  public PolarityType getPolarity() {
    if (getCVValue(MzMLCV.cvPolarityPositive).isPresent()) {
      return PolarityType.POSITIVE;
    }

    if (getCVValue(MzMLCV.cvPolarityNegative).isPresent()) {
      return PolarityType.NEGATIVE;
    }

    // Check in the scans of the spectrum for Polarity
    if (!getScanList().getScans().isEmpty()) {
      MzMLScan scan = getScanList().getScans().get(0);
      if (getCVValue(scan, MzMLCV.cvPolarityPositive).isPresent()) {
        return PolarityType.POSITIVE;
      }

      if (getCVValue(scan, MzMLCV.cvPolarityNegative).isPresent()) {
        return PolarityType.NEGATIVE;
      }
    }

    return PolarityType.UNKNOWN;
  }

  @Nullable
  public MzMLMobility getMobility() {
    if (mobility == null) {
      mobility = readMobility();
    }
    return mobility;
  }

  private @Nullable MzMLMobility readMobility() {

    if (isMergedMobilitySpectrum() && mobilityBinaryDataInfo != null) {
      return switch (mobilityBinaryDataInfo.getUnitAccession()) {
        case null -> new MzMLMobility(0d, MobilityType.DRIFT_TUBE);
        case MzMLCV.cvMobilityDriftTimeUnit -> new MzMLMobility(0d, MobilityType.DRIFT_TUBE);
        case MzMLCV.cvMobilityInverseReducedUnit -> new MzMLMobility(0d, MobilityType.TIMS);
        default -> null;
      };
    }

    if (!getScanList().getScans().isEmpty()) {
      for (MzMLCVParam param : getScanList().getScans().get(0).getCVParamsList()) {
        String accession = param.getAccession();
        if (param.getValue().isEmpty()) {
          continue;
        }
        switch (accession) {
          case MzMLCV.cvMobilityDriftTime -> {
            if (param.getUnitAccession().get().equals(MzMLCV.cvMobilityDriftTimeUnit)) {
              return new MzMLMobility(Double.parseDouble(param.getValue().get()),
                  MobilityType.DRIFT_TUBE);
            }
          }
          case MzMLCV.cvMobilityInverseReduced -> {
            if (param.getUnitAccession().get().equals(MzMLCV.cvMobilityInverseReducedUnit)) {
              return new MzMLMobility(Double.parseDouble(param.getValue().get()),
                  MobilityType.TIMS);
            }
          }
        }
      }
    }

    return null;
  }

  @Override
  public float getRetentionTime() {
    if (retentionTime != null) {
      return retentionTime;
    }

    if (!getScanList().getScans().isEmpty()) {
      for (MzMLCVParam param : getScanList().getScans().get(0).getCVParamsList()) {
        String accession = param.getAccession();
        Optional<String> unitAccession = param.getUnitAccession();
        Optional<String> value = param.getValue();

        // check accession
        switch (accession) {
          case MzMLCV.MS_RT_SCAN_START, MzMLCV.MS_RT_RETENTION_TIME,
               MzMLCV.MS_RT_RETENTION_TIME_LOCAL, MzMLCV.MS_RT_RETENTION_TIME_NORMALIZED -> {
            if (value.isEmpty()) {
              throw new IllegalStateException(
                  "For retention time cvParam the `value` must have been specified");
            }
            if (unitAccession.isPresent()) {
              // there was a time unit defined
              switch (param.getUnitAccession().orElse("")) {
                case MzMLCV.cvUnitsMin1, MzMLCV.cvUnitsMin2 ->
                    retentionTime = Float.parseFloat(value.get());
                case MzMLCV.cvUnitsSec -> retentionTime = Float.parseFloat(value.get()) / 60f;
                default -> throw new IllegalStateException(
                    "Unknown time unit encountered: [" + unitAccession + "]");
              }
            } else {
              // no time units defined, should be seconds
              retentionTime = Float.parseFloat(value.get()) / 60f;
            }
          }
          default -> {
            continue; // not a retention time parameter
          }
        }
      }
    }

    return retentionTime == null ? -1 : retentionTime;
  }

  @Override
  public @Nullable Float getInjectionTime() {
    try {
      return getScanList().getScans().get(0).getCVParamsList().stream()
          .filter(p -> MzMLCV.cvIonInjectTime.equals(p.getAccession()))
          .map(p -> p.getValue().map(Float::parseFloat)).map(Optional::get).findFirst()
          .orElse(null);
    } catch (Exception ex) {
      // float parsing error
      return null;
    }
  }

  @Override
  public @Nullable MsMsInfo getMsMsInfo() {

    final List<MzMLPrecursorElement> precursors = precursorList.getPrecursorElements();
    return switch (precursors.size()) {
      case 0 -> null;
      case 1 -> MsMsInfo.fromMzML(precursors.getFirst(), getMSLevel());
      default -> MSnInfoImpl.fromMzML(precursors, getMSLevel());
    };
  }

  /**
   * <p>
   * Search for the CV Parameter value for the given accession in the spectrum's CV Parameters
   * </p>
   *
   * @param accession the CV Parameter accession as {@link String String}
   * @return an {@link Optional Optional<String>} containing the CV Parameter value for the given
   * accession, if present <br> An empty {@link Optional Optional<String>} otherwise
   */
  public Optional<String> getCVValue(String accession) {
    return getCVValue(cvParams, accession);
  }


  /**
   * <p>
   * Search for the CV Parameter value for the given accession in the given
   * {@link MzMLCVGroup MzMLCVGroup}
   * </p>
   *
   * @param group     the {@link MzMLCVGroup MzMLCVGroup} to search through
   * @param accession the CV Parameter accession as {@link String String}
   * @return an {@link Optional Optional<String>} containing the CV Parameter value for the given
   * accession, if present <br> An empty {@link Optional Optional<String>} otherwise
   */
  public Optional<String> getCVValue(MzMLCVGroup group, String accession) {
    Optional<String> value;
    for (MzMLCVParam cvParam : group.getCVParamsList()) {
      if (cvParam.getAccession().equals(accession)) {
        value = cvParam.getValue();
        if (!value.isPresent()) {
          value = Optional.ofNullable("");
        }
        return value;
      }
    }
    return Optional.empty();
  }

  public Optional<MzMLCVParam> getCVParam(String accession) {
    for (MzMLCVParam cvParam : cvParams.getCVParamsList()) {
      if (cvParam.getAccession().equals(accession)) {
        return Optional.of(cvParam);
      }
    }
    return Optional.empty();
  }

  /**
   * Called when spectrum end is read. Load, process data points and memory map resulting data to
   * disk to save RAM.
   *
   * @return false if
   */
  public boolean loadProcessMemMapMzData(final MemoryMapStorage storage,
      final @NotNull ScanImportProcessorConfig config) {
    SimpleSpectralArrays specData = decodeSpectrum(mzBinaryDataInfo);
    if (specData == null) {
      // may be null for UV spectra
      return false;
    }

    if (getMobility() != null) {
      // mobility scans are processed and memory mapped later, all mobility scans of a frame at
      // once, see processMobilityScansOfFrame
      mobilityScanSimpleSpectralData = specData;
      return true;
    }

    // process and filter - needs metadata so wrap
    specData = config.processor().processScan(this, specData);

    if (config.isMassDetectActive(getMSLevel())) {
      // after mass detection we have a centroid scan
      spectrumType = MassSpectrumType.CENTROIDED;
    }

    // memory map regular scan data
    this.mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, specData.mzs());
    this.intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, specData.intensities());

    return true;
  }

  /**
   * Processes the raw data of all mobility scans of one frame together, see
   * {@link MsProcessor#processMobilityScans}. The processed data replaces
   * {@link #getMobilityScanSimpleSpectralData()} of each scan.
   *
   * @param metadata      first acquired spectrum of the frame
   * @param mobilityScans all mobility scans of one frame, loaded by
   *                      {@link #loadProcessMemMapMzData(MemoryMapStorage,
   *                      ScanImportProcessorConfig)}
   * @param config        the import processing config
   */
  public static void processMobilityScansOfFrame(@NotNull final BuildingMzMLMsScan metadata,
      @NotNull final List<BuildingMzMLMsScan> mobilityScans,
      @NotNull final ScanImportProcessorConfig config) {
    if (mobilityScans.isEmpty()) {
      return;
    }

    final List<SimpleSpectralArrays> rawSpectra = new ArrayList<>(mobilityScans.size());
    for (final BuildingMzMLMsScan mobilityScan : mobilityScans) {
      rawSpectra.add(mobilityScan.getMobilityScanSimpleSpectralData());
    }

    // assumption: all mobility scans of a frame share the metadata needed for processing (ms
    // level, spectrum type, injection time), so the first scan represents all
    final List<SimpleSpectralArrays> processedSpectra = config.processor()
        .processMobilityScans(metadata, rawSpectra);

    for (int i = 0; i < mobilityScans.size(); i++) {
      final BuildingMzMLMsScan mobilityScan = mobilityScans.get(i);
      mobilityScan.mobilityScanSimpleSpectralData = processedSpectra.get(i);
      if (config.isMassDetectActive(mobilityScan.getMSLevel())) {
        // after mass detection we have a centroid scan
        mobilityScan.spectrumType = MassSpectrumType.CENTROIDED;
      }
    }
  }

  /**
   * Packs the selected frame's bins after they have been placed in mobility-axis order.
   */
  public @NotNull BuildingMobilityScanStorage packMergedMobilityScans(
      @Nullable final MemoryMapStorage storage, @NotNull final ScanImportProcessorConfig config,
      @NotNull final List<MobilitySpectralArrays> mobilityScans) {
    final List<SimpleSpectralArrays> mobilityScanSpectra = mobilityScans.stream()
        .map(MobilitySpectralArrays::spectrum).toList();
    // all mobility scans of the frame are processed together, the merged frame spectrum is
    // calculated from the mobility scans later
    final List<SimpleSpectralArrays> processedSpectra = config.processor()
        .processMobilityScans(this, mobilityScanSpectra);
    final List<MobilitySpectralArrays> processedMobilityScanData = new ArrayList<>(
        mobilityScans.size());
    for (int i = 0; i < mobilityScans.size(); i++) {
      processedMobilityScanData.add(
          new MobilitySpectralArrays(mobilityScans.get(i).mobility(), processedSpectra.get(i)));
    }
    if (config.isMassDetectActive(getMSLevel())) {
      spectrumType = MassSpectrumType.CENTROIDED;
    }
    final BuildingMobilityScanStorage buildingMobilityScanStorage = new BuildingMobilityScanStorage(
        storage, this, processedMobilityScanData, getSpectrumType());
    clearUnusedData();
    return buildingMobilityScanStorage;
  }

  @NotNull
  public List<MobilitySpectralArrays> splitMergedMobilityScans() {
    if (!isMergedMobilitySpectrum()) {
      throw new IllegalStateException("Scan is not a merged mobility scan");
    }

    final MzMLBinaryDataInfo mobilityInfo = getMobilityBinaryDataInfo();
    final MzMLBinaryDataInfo mzInfo = getMzBinaryDataInfo();
    final MzMLBinaryDataInfo intensityInfo = getIntensityBinaryDataInfo();
    if (mobilityInfo == null || mzInfo == null || intensityInfo == null) {
      throw new IllegalStateException(
          "mzml scan %s did not contain all expected data (mz %s, intensity %s, mobility %s)".formatted(
              getId(), mzInfo != null, intensityInfo != null, mobilityInfo != null));
    }

    if (mobilityInfo.getArrayLength() != mzInfo.getArrayLength()
        || mobilityInfo.getArrayLength() != intensityInfo.getArrayLength()) {
      throw new IllegalStateException(
          "Array lengths don't match (mobility = %d, mz = %d, intensity = %d".formatted(
              mobilityInfo.getArrayLength(), mzInfo.getArrayLength(),
              intensityInfo.getArrayLength()));
    }

    final double[] mobilities = MzMLPeaksDecoder.decodeToDouble(mobilityInfo);
    final double[] mzs = MzMLPeaksDecoder.decodeToDouble(mzInfo);
    final double[] intensities = MzMLPeaksDecoder.decodeToDouble(intensityInfo);
    if (mzs.length != intensities.length || mzs.length != mobilities.length) {
      throw new IllegalArgumentException(
          "Decoded mobility, m/z and intensity lengths differ in " + id);
    }

    final List<MobilitySpectralArrays> mobilitySpectralArrays = new ArrayList<>();
    int lastOffset = 0;
    for (int i = 1; i <= mobilities.length; i++) {
      if (i == mobilities.length || Double.compare(mobilities[i - 1], mobilities[i]) != 0) {
        int numDp = i - lastOffset;

        final double[] extractedMzs = Arrays.copyOfRange(mzs, lastOffset, lastOffset + numDp);
        final double[] extractedIntensities = Arrays.copyOfRange(intensities, lastOffset,
            lastOffset + numDp);
        final MobilitySpectralArrays mobArrays = new MobilitySpectralArrays(mobilities[i - 1],
            new SimpleSpectralArrays(extractedMzs, extractedIntensities));

        mobilitySpectralArrays.add(mobArrays);
        lastOffset += numDp;
      }
    }

    return mobilitySpectralArrays;
  }

  /**
   * @param domain m/z or wavelength array for this spectrum
   * @return decoded, paired coordinates and intensities, or null for a missing domain array
   */
  private @Nullable SimpleSpectralArrays decodeSpectrum(@Nullable final MzMLBinaryDataInfo domain) {
    if (domain == null) {
      clearUnusedData();
      return null;
    }

    if (intensityBinaryDataInfo == null
        || domain.getArrayLength() != intensityBinaryDataInfo.getArrayLength()) {
      throw new IllegalArgumentException("Missing or mismatched intensity array in " + id);
    }
    final double[] coordinates = MzMLPeaksDecoder.decodeToDouble(domain);
    final double[] intensities = MzMLPeaksDecoder.decodeToDouble(intensityBinaryDataInfo);
    if (coordinates.length != intensities.length) {
      throw new IllegalArgumentException(
          "Decoded coordinate and intensity lengths differ in " + id);
    }

    clearUnusedData();
    return new SimpleSpectralArrays(coordinates, intensities);
  }

  public void clearUnusedData() {
    mzBinaryDataInfo = null;
    intensityBinaryDataInfo = null;
    wavelengthBinaryDataInfo = null;
    mobilityBinaryDataInfo = null;
  }

  /**
   * Only mobility scans have their data still in memory as they are memory mapped all at once
   */
  @Nullable
  public SimpleSpectralArrays getMobilityScanSimpleSpectralData() {
    return mobilityScanSimpleSpectralData;
  }

  /**
   * Gets cleared later - first we collect all scans and memory map them together for each frame
   * with the same retention time
   */
  public void clearMobilityData() {
    mobilityScanSimpleSpectralData = null;
  }

  public void setWavelengthBinaryDataInfo(MzMLBinaryDataInfo binaryDataInfo) {
    this.wavelengthBinaryDataInfo = binaryDataInfo;
  }

  public boolean isUVSpectrum() {
    return (mzBinaryDataInfo == null && (intensityBinaryDataInfo != null
        && wavelengthBinaryDataInfo != null)) || (mzValues == null && (intensityValues != null
        && wavelengthValues != null));
  }

  public boolean isMergedMobilitySpectrum() {
    return mobilityBinaryDataInfo != null;
  }

  /**
   * Called when spectrum end is read. Load, process data points and memory map resulting data to
   * disk to save RAM.
   *
   * @return false if no data was loaded.
   */
  public boolean loadProcessMemMapUvData(@Nullable final MemoryMapStorage storage) {
    final SimpleSpectralArrays specData = decodeSpectrum(wavelengthBinaryDataInfo);
    if (specData == null) {
      return false;
    }

    this.wavelengthValues = StorageUtils.storeValuesToDoubleBuffer(storage, specData.mzs());
    this.intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, specData.intensities());
    return true;
  }

  public MzMLBinaryDataInfo getMobilityBinaryDataInfo() {
    return mobilityBinaryDataInfo;
  }

  public void setMobilityBinaryDataInfo(MzMLBinaryDataInfo mobilityBinaryDataInfo) {
    this.mobilityBinaryDataInfo = mobilityBinaryDataInfo;
  }

  public boolean isMassSpectrum() {
    return mzBinaryDataInfo != null || mzValues != null;
  }

  public @Nullable MemorySegment getWavelengthValues() {
    return wavelengthValues;
  }

  @Override
  public void setSpectrumType(@NotNull MassSpectrumType spectrumType) {
    this.spectrumType = spectrumType;
  }
}
