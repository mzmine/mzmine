/*
 * (C) Copyright 2015-2016 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
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
  private MzMLBinaryDataInfo mzBinaryDataInfo;
  private MzMLBinaryDataInfo intensityBinaryDataInfo;
  private @NotNull InputStream inputStream;
  private final @NotNull String id;
  private final @NotNull Integer scanNumber;
  private final int numOfDataPoints;

  private MsSpectrumType spectrumType;
  private Float tic;
  private Float retentionTime;
  private Range<Double> mzRange;
  private Range<Double> mzScanWindowRange;
  private double[] mzValues;
  private float[] intensityValues;

  private Logger logger = Logger.getLogger(getClass().getName());

  /**
   * <p>
   * Constructor for {@link MzMLMsScan MzMLMsScan}
   * </p>
   *
   * @param dataFile a {@link MzMLRawDataFile MzMLRawDataFile} object the parser stores the data in
   * @param is an {@link InputStream InputStream} of the MzML format data
   * @param id the Scan ID
   * @param scanNumber the Scan Number
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
   *
   * @return a {@link io.github.msdk.io.mzml2.util.io.ByteBufferInputStream} object.
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
  public void setInputStream(InputStream inputStream) {
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
  public String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Integer getNumberOfDataPoints() {
    return getMzBinaryDataInfo().getArrayLength();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getMzValues(double array[]) {
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
  public float[] getIntensityValues(float array[]) {
    if (intensityValues == null) {
      if (getIntensityBinaryDataInfo().getArrayLength() != numOfDataPoints) {
        logger.warning(
            "Intensity binary data array contains a different array length from the default array length of the scan (#"
                + getScanNumber() + ")");
      }

      try {
        intensityValues =
            MzMLPeaksDecoder.decodeToFloat(inputStream, getIntensityBinaryDataInfo(), array);
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
  public MsSpectrumType getSpectrumType() {
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
  public Float getTIC() {
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
      if (!cvv.isPresent() || !cvv1.isPresent()) {
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
  public Integer getScanNumber() {
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
  public Integer getMsLevel() {
    Integer msLevel = 1;
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
  public MsScanType getMsScanType() {
    return MsScanType.UNKNOWN;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Range<Double> getScanningRange() {
    if (mzScanWindowRange == null) {
      if (!getScanList().getScans().isEmpty()) {
        Optional<MzMLScanWindowList> scanWindowList =
            getScanList().getScans().get(0).getScanWindowList();
        if (scanWindowList.isPresent() && !scanWindowList.get().getScanWindows().isEmpty()) {
          MzMLScanWindow scanWindow = scanWindowList.get().getScanWindows().get(0);
          Optional<String> cvv = getCVValue(scanWindow, MzMLCV.cvScanWindowLowerLimit);
          Optional<String> cvv1 = getCVValue(scanWindow, MzMLCV.cvScanWindowUpperLimit);
          if (!cvv.isPresent() || !cvv1.isPresent()) {
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
  public List<IsolationInfo> getIsolations() {
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

      if (!precursor.getSelectedIonList().isPresent()) {
        return Collections.emptyList();
      }

      for (MzMLCVGroup cvGroup : precursor.getSelectedIonList().get().getSelectedIonList()) {
        precursorMz = getCVValue(cvGroup, MzMLCV.cvPrecursorMz);
        if (!precursorMz.isPresent()) {
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
        if (!isolationWindowTarget.isPresent()) {
          isolationWindowTarget = precursorMz;
        }
        if (!isolationWindowLower.isPresent()) {
          isolationWindowLower = Optional.ofNullable("0.5");
        }
        if (!isolationWindowUpper.isPresent()) {
          isolationWindowUpper = Optional.ofNullable("0.5");
        }
        Range<Double> isolationRange = Range.closed(
            Double.valueOf(isolationWindowTarget.get())
                - Double.valueOf(isolationWindowLower.get()),
            Double.valueOf(isolationWindowTarget.get())
                + Double.valueOf(isolationWindowLower.get()));
        Integer precursorChargeInt =
            precursorCharge.isPresent() ? Integer.valueOf(precursorCharge.get()) : null;
        Integer precursorScanNumberInt =
            precursorScanNumber.isPresent() ? Integer.valueOf(precursorScanNumber.get()) : null;
        IsolationInfo isolation = new SimpleIsolationInfo(isolationRange, null,
            Double.valueOf(precursorMz.get()), precursorChargeInt, null, precursorScanNumberInt);
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
   *         accession, if present <br>
   *         An empty {@link Optional Optional<String>} otherwise
   */
  public Optional<String> getCVValue(String accession) {
    return getCVValue(cvParams, accession);
  }

  /**
   * <p>
   * Search for the CV Parameter value for the given accession in the given {@link MzMLCVGroup
   * MzMLCVGroup}
   * </p>
   *
   * @param group the {@link MzMLCVGroup MzMLCVGroup} to search through
   * @param accession the CV Parameter accession as {@link String String}
   * @return an {@link Optional Optional<String>} containing the CV Parameter value for the given
   *         accession, if present <br>
   *         An empty {@link Optional Optional<String>} otherwise
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
