/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import io.github.msdk.datamodel.ActivationInfo;
import io.github.msdk.datamodel.IsolationInfo;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.MsScanType;
import io.github.msdk.datamodel.MsSpectrumType;
import io.github.msdk.datamodel.PolarityType;
import io.github.msdk.datamodel.RawDataFile;
import io.github.msdk.datamodel.SimpleIsolationInfo;
import io.github.msdk.spectra.centroidprofiledetection.SpectrumTypeDetectionAlgorithm;
import io.github.msdk.util.MsSpectrumUtil;
import io.github.msdk.util.tolerances.MzTolerance;
import io.github.mzmine.datamodel.MobilityType;
import java.io.InputStream;
import java.util.ArrayList;
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
public class MzMLMsScan implements MsScan {

  private final @NotNull MzMLRawDataFile dataFile;
  private final MzMLCVGroup cvParams;
  private final MzMLPrecursorList precursorList;
  private final MzMLProductList productList;
  private final MzMLScanList scanList;
  private final @NotNull String id;
  private final @NotNull Integer scanNumber;
  private final int numOfDataPoints;
  private final Logger logger = Logger.getLogger(getClass().getName());
  private MzMLBinaryDataInfo mzBinaryDataInfo;
  private MzMLBinaryDataInfo intensityBinaryDataInfo;
  private MsSpectrumType spectrumType;
  private Float tic;
  private Float retentionTime;
  private Range<Double> mzRange;
  private Range<Double> mzScanWindowRange;
  private double[] mzValues;
  private float[] intensityValues;
  private @NotNull InputStream inputStream;

  /**
   * <p>
   * Constructor for {@link MzMLMsScan MzMLMsScan}
   * </p>
   *
   * @param dataFile        a {@link MzMLRawDataFile MzMLRawDataFile} object the parser stores the
   *                        data in
   * @param is              an {@link InputStream InputStream} of the MzML format data
   * @param id              the Scan ID
   * @param scanNumber      the Scan Number
   * @param numOfDataPoints the number of data points in the m/z and intensity arrays
   */
  public MzMLMsScan(MzMLRawDataFile dataFile, InputStream is, String id, Integer scanNumber,
      int numOfDataPoints) {
    this.cvParams = new MzMLCVGroup();
    this.precursorList = new MzMLPrecursorList();
    this.productList = new MzMLProductList();
    this.scanList = new MzMLScanList();
    this.dataFile = dataFile;
    this.inputStream = is;
    this.id = id;
    this.scanNumber = scanNumber;
    this.numOfDataPoints = numOfDataPoints;
    this.mzBinaryDataInfo = null;
    this.intensityBinaryDataInfo = null;
    this.spectrumType = null;
    this.tic = null;
    this.retentionTime = null;
    this.mzRange = null;
    this.mzScanWindowRange = null;
    this.mzValues = null;
    this.intensityValues = null;

  }

  /**
   * <p>
   * getCVParams.
   * </p>
   *
   * @return a {@link ArrayList} object.
   */
  public MzMLCVGroup getCVParams() {
    return cvParams;
  }

  /**
   * <p>
   * Getter for the field <code>mzBinaryDataInfo</code>.
   * </p>
   *
   * @return a {@link MzMLBinaryDataInfo} object.
   */
  public MzMLBinaryDataInfo getMzBinaryDataInfo() {
    return mzBinaryDataInfo;
  }

  /**
   * <p>
   * Setter for the field <code>mzBinaryDataInfo</code>.
   * </p>
   *
   * @param mzBinaryDataInfo a {@link MzMLBinaryDataInfo} object.
   */
  public void setMzBinaryDataInfo(MzMLBinaryDataInfo mzBinaryDataInfo) {
    this.mzBinaryDataInfo = mzBinaryDataInfo;
  }

  /**
   * <p>
   * Getter for the field <code>intensityBinaryDataInfo</code>.
   * </p>
   *
   * @return a {@link MzMLBinaryDataInfo} object.
   */
  public MzMLBinaryDataInfo getIntensityBinaryDataInfo() {
    return intensityBinaryDataInfo;
  }

  /**
   * <p>
   * Setter for the field <code>intensityBinaryDataInfo</code>.
   * </p>
   *
   * @param intensityBinaryDataInfo a {@link MzMLBinaryDataInfo} object.
   */
  public void setIntensityBinaryDataInfo(MzMLBinaryDataInfo intensityBinaryDataInfo) {
    this.intensityBinaryDataInfo = intensityBinaryDataInfo;
  }

  /**
   * <p>
   * getInputStream.
   * </p>
   */
  public InputStream getInputStream() {
    return inputStream;
  }

  /**
   * <p>
   * setInputStream.
   * </p>
   *
   * @param inputStream a {@link InputStream} object.
   */
  public void setInputStream(@NotNull InputStream inputStream) {
    this.inputStream = inputStream;
  }

  /**
   * <p>
   * getPrecursorList.
   * </p>
   *
   * @return a {@link MzMLPrecursorList} object.
   */
  public MzMLPrecursorList getPrecursorList() {
    return precursorList;
  }

  /**
   * <p>
   * getProductList.
   * </p>
   *
   * @return a {@link MzMLProductList} object.
   */
  public MzMLProductList getProductList() {
    return productList;
  }

  /**
   * <p>
   * getScanList.
   * </p>
   *
   * @return a {@link MzMLScanList} object.
   */
  public MzMLScanList getScanList() {
    return scanList;
  }

  /**
   * <p>
   * Getter for the field <code>id</code>.
   * </p>
   *
   * @return a {@link String} object.
   */
  public @NotNull String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Integer getNumberOfDataPoints() {
    return getMzBinaryDataInfo().getArrayLength();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getMzValues(double[] array) {
    if (mzValues == null) {
      if (getMzBinaryDataInfo().getArrayLength() != numOfDataPoints) {
        logger.warning(
            "m/z binary data array contains a different array length from the default array length of the scan (#"
                + getScanNumber() + ")");
      }

      try {
        mzValues = MzMLPeaksDecoder.decodeToDouble(inputStream, getMzBinaryDataInfo(), array);
      } catch (Exception e) {
        throw (new MSDKRuntimeException(e));
      }
    }

    if (array == null || array.length < getNumberOfDataPoints()) {
      array = new double[getNumberOfDataPoints()];

      System.arraycopy(mzValues, 0, array, 0, numOfDataPoints);
    }

    return array;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float[] getIntensityValues(float[] array) {
    if (intensityValues == null) {
      if (getIntensityBinaryDataInfo().getArrayLength() != numOfDataPoints) {
        logger.warning(
            "Intensity binary data array contains a different array length from the default array length of the scan (#"
                + getScanNumber() + ")");
      }

      try {
        intensityValues = MzMLPeaksDecoder.decodeToFloat(inputStream, getIntensityBinaryDataInfo(),
            array);
      } catch (Exception e) {
        throw (new MSDKRuntimeException(e));
      }
    }

    if (array == null || array.length < numOfDataPoints) {
      array = new float[numOfDataPoints];

      System.arraycopy(intensityValues, 0, array, 0, numOfDataPoints);
    }

    return array;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MsSpectrumType getSpectrumType() {
    if (spectrumType == null) {
      if (getCVValue(MzMLCV.cvCentroidSpectrum).isPresent()) {
        spectrumType = MsSpectrumType.CENTROIDED;
      }

      if (getCVValue(MzMLCV.cvProfileSpectrum).isPresent()) {
        spectrumType = MsSpectrumType.PROFILE;
      }

      if (spectrumType != null) {
        return spectrumType;
      }

      spectrumType = SpectrumTypeDetectionAlgorithm.detectSpectrumType(getMzValues(),
          getIntensityValues(), numOfDataPoints);
    }
    return spectrumType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Float getTIC() {
    if (tic == null) {
      try {
        tic = MsSpectrumUtil.getTIC(getIntensityValues(), getNumberOfDataPoints());
      } catch (NumberFormatException e) {
        throw (new MSDKRuntimeException(
            "Could not convert TIC value in mzML file to a float\n" + e));
      }
    }

    return tic;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Range<Double> getMzRange() {
    if (mzRange == null) {
      Optional<String> cvv = getCVValue(MzMLCV.cvLowestMz);
      Optional<String> cvv1 = getCVValue(MzMLCV.cvHighestMz);
      if (cvv.isEmpty() || cvv1.isEmpty()) {
        mzRange = MsSpectrumUtil.getMzRange(getMzValues(), getMzBinaryDataInfo().getArrayLength());
        return mzRange;
      }
      try {
        mzRange = Range.closed(Double.valueOf(cvv.get()), Double.valueOf(cvv1.get()));
      } catch (NumberFormatException e) {
        throw (new MSDKRuntimeException(
            "Could not convert mz range value in mzML file to a double\n" + e));
      }
    }
    return mzRange;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RawDataFile getRawDataFile() {
    return dataFile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Integer getScanNumber() {
    return scanNumber;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getScanDefinition() {
    Optional<String> scanDefinition = Optional.empty();
    if (!getScanList().getScans().isEmpty()) {
      scanDefinition = getCVValue(getScanList().getScans().get(0), MzMLCV.cvScanFilterString);
    }
    return scanDefinition.orElse("");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMsFunction() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Integer getMsLevel() {
    int msLevel = 1;
    Optional<String> value = getCVValue(MzMLCV.cvMSLevel);
    if (value.isPresent()) {
      msLevel = Integer.parseInt(value.get());
    }
    return msLevel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MsScanType getMsScanType() {
    return MsScanType.UNKNOWN;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Range<Double> getScanningRange() {
    if (mzScanWindowRange == null) {
      if (!getScanList().getScans().isEmpty()) {
        Optional<MzMLScanWindowList> scanWindowList = getScanList().getScans().get(0)
            .getScanWindowList();
        if (scanWindowList.isPresent() && !scanWindowList.get().getScanWindows().isEmpty()) {
          MzMLScanWindow scanWindow = scanWindowList.get().getScanWindows().get(0);
          Optional<String> cvv = getCVValue(scanWindow, MzMLCV.cvScanWindowLowerLimit);
          Optional<String> cvv1 = getCVValue(scanWindow, MzMLCV.cvScanWindowUpperLimit);
          if (cvv.isEmpty() || cvv1.isEmpty()) {
            mzScanWindowRange = getMzRange();
            return mzScanWindowRange;
          }
          try {
            mzScanWindowRange = Range.closed(Double.valueOf(cvv.get()), Double.valueOf(cvv1.get()));
          } catch (NumberFormatException e) {
            throw (new MSDKRuntimeException(
                "Could not convert scan window range value in mzML file to a double\n" + e));
          }
        }
      }
    }

    return mzScanWindowRange;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull PolarityType getPolarity() {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public ActivationInfo getSourceInducedFragmentation() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Float getRetentionTime() {
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
          case MzMLCV.MS_RT_SCAN_START:
          case MzMLCV.MS_RT_RETENTION_TIME:
          case MzMLCV.MS_RT_RETENTION_TIME_LOCAL:
          case MzMLCV.MS_RT_RETENTION_TIME_NORMALIZED:
            if (!value.isPresent()) {
              throw new IllegalStateException(
                  "For retention time cvParam the `value` must have been specified");
            }
            if (unitAccession.isPresent()) {
              // there was a time unit defined
              switch (param.getUnitAccession().get()) {
                case MzMLCV.cvUnitsMin1:
                case MzMLCV.cvUnitsMin2:
                  retentionTime = Float.parseFloat(value.get()) * 60f;
                  break;
                case MzMLCV.cvUnitsSec:
                  retentionTime = Float.parseFloat(value.get());
                  break;

                default:
                  throw new IllegalStateException(
                      "Unknown time unit encountered: [" + unitAccession + "]");
              }
            } else {
              // no time units defined, return the value as is
              retentionTime = Float.parseFloat(value.get());
            }
            break;

          default:
            continue; // not a retention time parameter
        }
      }
    }

    return retentionTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MzTolerance getMzTolerance() {
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

  /**
   * <p>
   * getScanNumber.
   * </p>
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

}
