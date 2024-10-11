/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.Range;
import io.github.msdk.MSDKException;
import io.github.msdk.MSDKRuntimeException;
import io.github.msdk.datamodel.ActivationInfo;
import io.github.msdk.datamodel.IsolationInfo;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.SimpleIsolationInfo;
import io.github.msdk.util.MsSpectrumUtil;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MetadataOnlyScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private int scanNumber;
  private final int numOfDataPoints;
  private MassSpectrumType spectrumType;
  private Float retentionTime;
  private Range<Double> mzRange;
  private Range<Double> mzScanWindowRange;
  private Double tic;

  // temporary - set to null after load
  private MzMLBinaryDataInfo mzBinaryDataInfo;
  private MzMLBinaryDataInfo intensityBinaryDataInfo;
  private MzMLBinaryDataInfo wavelengthBinaryDataInfo;

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
   * @param id              the Scan ID
   * @param scanNumber      the Scan Number
   * @param numOfDataPoints the number of data points in the m/z and intensity arrays
   */
  public BuildingMzMLMsScan(String id, Integer scanNumber, int numOfDataPoints) {
    this.cvParams = new MzMLCVGroup();
    this.precursorList = new MzMLPrecursorList();
    this.productList = new MzMLProductList();
    this.scanList = new MzMLScanList();
    this.id = id;
    this.scanNumber = scanNumber;
    this.numOfDataPoints = numOfDataPoints;
    this.mzBinaryDataInfo = null;
    this.intensityBinaryDataInfo = null;
    this.spectrumType = null;
    this.retentionTime = null;
    this.mzRange = null;
    this.mzScanWindowRange = null;
    this.mzValues = null;
    this.intensityValues = null;
    this.wavelengthValues = null;
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
  public @Nullable Double getTIC() {
    if (tic != null) {
      return tic;
    }
    if (intensityValues == null) {
      throw new UnsupportedOperationException(
          "No data yet. Call load method to load data and memory map the scan.");
    }
    tic = Arrays.stream(getIntensityValues(new double[getNumberOfDataPoints()])).sum();
    return tic;
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
    // cannot run on data here as it's not necessarily loaded
    return spectrumType;
  }

  @Override
  public @NotNull Range<Double> getScanningMZRange() {
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
              throw (new MSDKRuntimeException(
                  "Could not convert scan window range value in mzML file to a double\n" + e));
            }
          }
        }
      }
    }

    return requireNonNullElse(mzScanWindowRange, Range.all());
  }

  @Override
  public @NotNull Range<Double> getDataPointMZRange() {
    if (mzRange == null) {
      Optional<String> cvv = getCVValue(MzMLCV.cvLowestMz);
      Optional<String> cvv1 = getCVValue(MzMLCV.cvHighestMz);
      if (cvv.isEmpty() || cvv1.isEmpty()) {
        // mz values is null if data was not loaded yet
        if (mzValues != null) {
          mzRange = MsSpectrumUtil.getMzRange(DataPointUtils.getDoubleBufferAsArray(mzValues),
              getMzBinaryDataInfo().getArrayLength());
        }
      } else {
        try {
          mzRange = Range.closed(Double.valueOf(cvv.get()), Double.valueOf(cvv1.get()));
        } catch (NumberFormatException e) {
          throw (new MSDKRuntimeException(
              "Could not convert mz range value in mzML file to a double\n" + e));
        }
      }
    }
    return requireNonNullElse(mzRange, Range.all());
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
  public io.github.mzmine.datamodel.PolarityType getPolarity() {
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

  public @NotNull List<IsolationInfo> getIsolations() {
    if (precursorList.getPrecursorElements().size() == 0) {
      return Collections.emptyList();
    }

    List<IsolationInfo> isolations = new ArrayList<>();

    for (MzMLPrecursorElement precursor : precursorList.getPrecursorElements()) {
      Optional<String> precursorMz = Optional.empty();
      Optional<String> precursorCharge = Optional.empty();
      Optional<Integer> precursorScanNumber = getScanNumber(precursor.getSpectrumRef().orElse(""));
      Optional<String> isolationWindowTarget = Optional.empty();
      Optional<String> isolationWindowLower = Optional.empty();
      Optional<String> isolationWindowUpper = Optional.empty();

      if (precursor.getSelectedIonList().isEmpty()) {
        return Collections.emptyList();
      }

      for (MzMLCVGroup cvGroup : precursor.getSelectedIonList().get().getSelectedIonList()) {
        precursorMz = getCVValue(cvGroup, MzMLCV.cvPrecursorMz);
        if (precursorMz.isEmpty()) {
          precursorMz = getCVValue(cvGroup, MzMLCV.cvMz);
        }
        precursorCharge = getCVValue(cvGroup, MzMLCV.cvChargeState);
      }

      if (precursor.getIsolationWindow().isPresent()) {
        MzMLIsolationWindow isolationWindow = precursor.getIsolationWindow().get();
        isolationWindowTarget = getCVValue(isolationWindow, MzMLCV.cvIsolationWindowTarget);
        isolationWindowLower = getCVValue(isolationWindow, MzMLCV.cvIsolationWindowLowerOffset);
        isolationWindowUpper = getCVValue(isolationWindow, MzMLCV.cvIsolationWindowUpperOffset);
      }

      if (precursorMz.isPresent()) {
        if (isolationWindowTarget.isEmpty()) {
          isolationWindowTarget = precursorMz;
        }
        if (isolationWindowLower.isEmpty()) {
          isolationWindowLower = Optional.of("0.5");
        }
        if (isolationWindowUpper.isEmpty()) {
          isolationWindowUpper = Optional.of("0.5");
        }
        // this is the isolation window center or if not available the precursor mz specified as isolated ion
        double targetWindowCenter = Double.parseDouble(isolationWindowTarget.get());
        Range<Double> isolationRange = Range.closed(
            targetWindowCenter - Double.parseDouble(isolationWindowLower.get()),
            targetWindowCenter + Double.parseDouble(isolationWindowUpper.get()));
        Integer precursorChargeInt = precursorCharge.map(Integer::valueOf).orElse(null);
        Integer precursorScanNumberInt = precursorScanNumber.orElse(null);

        /*
         use center of isolation window. At least for Orbitrap instruments and
         msconvert conversion we found that the isolated ion actually refers to the main peak in
         an isotope pattern whereas the isolation window is the correct isolation mz
         see issue https://github.com/mzmine/mzmine3/issues/717
        */
        IsolationInfo isolation = new SimpleIsolationInfo(isolationRange, null, targetWindowCenter,
            precursorChargeInt, null, precursorScanNumberInt);
        isolations.add(isolation);

      }

    }

    return Collections.unmodifiableList(isolations);
  }

  @Nullable
  public MzMLMobility getMobility() {
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
    if (!getIsolations().isEmpty()) {
      IsolationInfo isolationInfo = getIsolations().get(0);
      ActivationInfo activationInfo = isolationInfo.getActivationInfo();
      Float energy = activationInfo != null && activationInfo.getActivationEnergy() != null
          ? activationInfo.getActivationEnergy().floatValue() : null;
      ActivationMethod activationMethod = activationInfo != null ? ActivationMethod.valueOf(
          activationInfo.getActivationType().name()) : null;

      return new DDAMsMsInfoImpl(isolationInfo.getPrecursorMz(), isolationInfo.getPrecursorCharge(),
          energy, this, null, getMSLevel(), activationMethod, null);
    }
    return null;
  }

  /**
   * <p>
   * Search for the CV Parameter value for the given accession in the {@link MsScan MsScan}'s CV
   * Parameters
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
   * getScanNumber.
   *
   * @param spectrumId a {@link String} object.
   * @return a {@link Integer} object.
   */
  public Optional<Integer> getScanNumber(String spectrumId) {
    final Pattern pattern = Pattern.compile("scan=([0-9]+)");
    final Matcher matcher = pattern.matcher(spectrumId);
    boolean scanNumberFound = matcher.find();

    // Some vendors include scan=XX in the ID, some don't, such as
    // mzML converted from WIFF files. See the definition of nativeID in
    // http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo
    // So, get the value of the index tag if the scanNumber is not present in the ID
    if (scanNumberFound) {
      Integer scanNumber = Integer.parseInt(matcher.group(1));
      return Optional.ofNullable(scanNumber);
    }

    return Optional.ofNullable(null);
  }

  /**
   * Called when spectrum end is read. Load, process data points and memory map resulting data to
   * disk to save RAM.
   *
   * @return false if
   */
  public boolean loadProcessMemMapMzData(final MemoryMapStorage storage,
      final @NotNull ScanImportProcessorConfig config) {
    try {
      SimpleSpectralArrays specData = loadMzData();
      if (specData == null) {
        // may be null for UV spectra
        return false;
      }

      // process and filter - needs metadata so wrap
      specData = config.processor().processScan(this, specData);

      if (config.isMassDetectActive(getMSLevel())) {
        // after mass detection we have a centroid scan
        spectrumType = MassSpectrumType.CENTROIDED;
      }

      if (getMobility() != null) {
        // cannot memory map mobility scan data as we need to do this later all mobility scans at once
        mobilityScanSimpleSpectralData = specData;
      } else {
        // memory map regular scan data but not mobility scans
        this.mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, specData.mzs());
        this.intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage,
            specData.intensities());
      }

    } catch (MSDKException | IOException e) {
      logger.warning("Could not load data of scan #%d".formatted(getScanNumber()));
      return false;
    }
    return true;
  }

  /**
   * Decode and load data from binary arrays
   *
   * @return null if no data available otherwise the spectral arrays
   */
  private @Nullable SimpleSpectralArrays loadMzData() throws MSDKException, IOException {
    if (mzBinaryDataInfo == null) {
      // maybe UV spectrum
      clearUnusedData();
      return null;
    }

    if (mzBinaryDataInfo.getArrayLength() != intensityBinaryDataInfo.getArrayLength()) {
      logger.warning(
          "Binary data array contains an array of different length than the default array length of the scan (#"
              + getScanNumber() + ")");
    }
    double[] mzs = MzMLPeaksDecoder.decodeToDouble(mzBinaryDataInfo);
    double[] intensities = MzMLPeaksDecoder.decodeToDouble(intensityBinaryDataInfo);

    clearUnusedData();
    return new SimpleSpectralArrays(mzs, intensities);
  }

  public void clearUnusedData() {
    mzBinaryDataInfo = null;
    intensityBinaryDataInfo = null;
    wavelengthBinaryDataInfo = null;
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

  /**
   * Called when spectrum end is read. Load, process data points and memory map resulting data to
   * disk to save RAM.
   *
   * @return false if no data was loaded.
   */
  public boolean loadProcessMemMapUvData(final MemoryMapStorage storage,
      final @NotNull ScanImportProcessorConfig config) {
    try {
      SimpleSpectralArrays specData = loadUVData();
      if (specData == null) {
        return false;
      }

      this.wavelengthValues = StorageUtils.storeValuesToDoubleBuffer(storage, specData.mzs());
      this.intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage,
          specData.intensities());

    } catch (MSDKException | IOException e) {
      logger.warning("Could not load data of scan #%d".formatted(getScanNumber()));
      return false;
    }
    return true;
  }

  /**
   * Decode and load data from binary arrays
   *
   * @return null if no data available otherwise the spectral arrays. Wavelengths are represented as
   * mz as an intermediate.
   */
  private @Nullable SimpleSpectralArrays loadUVData() throws MSDKException, IOException {
    if (wavelengthBinaryDataInfo == null) {
      // maybe UV spectrum
      clearUnusedData();
      return null;
    }

    if (wavelengthBinaryDataInfo.getArrayLength() != intensityBinaryDataInfo.getArrayLength()) {
      logger.warning(
          "Binary data array contains an array of different length than the default array length of the scan (#"
              + getScanNumber() + ")");
    }
    double[] wavelength = MzMLPeaksDecoder.decodeToDouble(wavelengthBinaryDataInfo);
    double[] intensities = MzMLPeaksDecoder.decodeToDouble(intensityBinaryDataInfo);

    clearUnusedData();
    return new SimpleSpectralArrays(wavelength, intensities);
  }

  public boolean isMassSpectrum() {
    return mzBinaryDataInfo != null || mzValues != null;
  }

  public @Nullable MemorySegment getWavelengthValues() {
    return wavelengthValues;
  }

  public void setScanNumber(int newScanNumber) {
    this.scanNumber = newScanNumber;
  }
}
