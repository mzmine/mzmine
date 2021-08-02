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

package io.github.mzmine.modules.io.export_rawdata_netcdf;

import io.github.mzmine.util.SpectrumTypeDetectionAlgorithm;
import java.io.IOException;

import io.github.mzmine.datamodel.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.msdk.MsSpectrumType;
import io.github.mzmine.datamodel.msdk.SimpleMsScan;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

/**
 * <p>
 * NetCDFMsScan class.
 * </p>
 *
 */
public class NetCDFMsScan extends SimpleMsScan {

  private int[] scanStartPositions;
  private float[] scanRetentionTimes;
  private Variable massValueVariable;
  private Variable intensityValueVariable;
  private double massValueScaleFactor;
  private double intensityValueScaleFactor;
  private double[] preLoadedMzValues;
  private float[] preLoadedIntensityValues;
  private Integer numOfDataPoints;
  private MsSpectrumType spectrumType;

  /**
   *
   * @param scanNumber the Scan Number
   * @param scanStartPositions an int[] containing start positions of all scans, and an extra
   *        element containing the stop position of the last scan
   * @param scanRetentionTimes a float[] containing retention times of all scans
   * @param massValueVariable {@link Variable Variable} containing the m/z data of the
   *        scans
   * @param intensityValueVariable {@link Variable Variable} containing the intensity data
   *        of the scans
   * @param massValueScaleFactor double value by which the mass values have been scaled by to
   * @param intensityValueScaleFactor double value by which the intensity values have been scaled by
   *        to
   */
  public NetCDFMsScan(Integer scanNumber, int[] scanStartPositions, float[] scanRetentionTimes,
      Variable massValueVariable, Variable intensityValueVariable, double massValueScaleFactor,
      double intensityValueScaleFactor) {
    super(scanNumber);
    this.scanStartPositions = scanStartPositions;
    this.scanRetentionTimes = scanRetentionTimes;
    this.massValueVariable = massValueVariable;
    this.intensityValueVariable = intensityValueVariable;
    this.massValueScaleFactor = massValueScaleFactor;
    this.intensityValueScaleFactor = intensityValueScaleFactor;
    this.preLoadedMzValues = null;
    this.preLoadedIntensityValues = null;
    this.spectrumType = null;
  }

  /** {@inheritDoc} */
  @Override
  public float[] getIntensityValues(float[] intensityValues) {
    if (preLoadedIntensityValues == null) {
      final Integer scanIndex = getScanIndex();
      numOfDataPoints = getNumberOfDataPoints();
      try {
        // int[] which defines the origin
        // Since intensity value is stored in a 1D array, there is only one element
        final int scanStartPosition[] = {scanStartPositions[scanIndex]};
        // int[] which defines the shape
        // shape is the length of each dimension to be considered from the origin
        // So, shape is an int[] containing only one element - 'size'
        final int scanLength[] =
            {scanStartPositions[scanIndex + 1] - scanStartPositions[scanIndex]};
        final Array intensityValueArray =
            intensityValueVariable.read(scanStartPosition, scanLength);
        final Index intensityValuesIndex = intensityValueArray.getIndex();

        if (intensityValues == null || intensityValues.length < numOfDataPoints)
          intensityValues = new float[numOfDataPoints];

        // Load the data points
        for (int i = 0; i < numOfDataPoints; i++) {
          // Change the Index according to i
          final Index intensityIndex0 = intensityValuesIndex.set0(i);
          // get the intensity value after multiplying with the scale factor
          intensityValues[i] =
              (float) (intensityValueArray.getDouble(intensityIndex0) * intensityValueScaleFactor);
        }
      } catch (IOException | InvalidRangeException e) {
        throw new MSDKRuntimeException(e);
      }
    } else {
      if (intensityValues == null || intensityValues.length < numOfDataPoints)
        intensityValues = new float[numOfDataPoints];

      // Copy values to a different array if needed
      for (int i = 0; i < preLoadedIntensityValues.length; i++)
        intensityValues[i] = preLoadedIntensityValues[i];
    }

    return intensityValues;
  }

  /** {@inheritDoc} */
  @Override
  public double[] getMzValues(double[] mzValues) {
    if (preLoadedMzValues == null) {
      final Integer scanIndex = getScanIndex();
      numOfDataPoints = getNumberOfDataPoints();
      try {
        // int[] which defines the origin
        // Since mass value is stored in a 1D array, there is only one element
        final int scanStartPosition[] = {scanStartPositions[scanIndex]};
        // int[] which defines the shape
        // shape is the length of each dimension to be considered from the origin
        // So, shape is an int[] containing only one element - 'size'
        final int scanLength[] =
            {scanStartPositions[scanIndex + 1] - scanStartPositions[scanIndex]};
        final Array massValueArray = massValueVariable.read(scanStartPosition, scanLength);
        final Index massValuesIndex = massValueArray.getIndex();

        if (mzValues == null || mzValues.length < numOfDataPoints)
          mzValues = new double[numOfDataPoints];

        // Load the data points
        for (int i = 0; i < numOfDataPoints; i++) {
          // Change the Index according to i
          final Index massIndex0 = massValuesIndex.set0(i);
          // get the mass value after multiplying with the scale factor
          mzValues[i] = massValueArray.getDouble(massIndex0) * massValueScaleFactor;
        }

      } catch (IOException | InvalidRangeException e) {
        throw new MSDKRuntimeException(e);
      }
    } else {
      if (mzValues == null || mzValues.length < numOfDataPoints)
        mzValues = new double[numOfDataPoints];

      // Copy values to a different array if needed
      for (int i = 0; i < preLoadedMzValues.length; i++)
        mzValues[i] = preLoadedMzValues[i];
    }

    return mzValues;
  }

  /** {@inheritDoc} */
  @Override
  public Integer getNumberOfDataPoints() {
    if (numOfDataPoints == null) {
      final Integer scanIndex = getScanIndex();
      numOfDataPoints = scanStartPositions[scanIndex + 1] - scanStartPositions[scanIndex];
    }

    return numOfDataPoints;
  }

  /** {@inheritDoc} */
  @Override
  public Float getRetentionTime() {
    return scanRetentionTimes[getScanIndex()];
  }

  /** {@inheritDoc} */
  @Override
  public MsSpectrumType getSpectrumType() {
    if (spectrumType == null)
      spectrumType = SpectrumTypeDetectionAlgorithm.detectSpectrumType(getMzValues(),
          getIntensityValues(), getNumberOfDataPoints());

    return spectrumType;
  }

  /**
   * The Scan Index is the inde of the scan in the array
   * 
   * @return the scan index of the scan
   */
  public Integer getScanIndex() {
    return getScanNumber() - 1;
  }

  /**
   * The mass and intensity arrays are loaded once this method is called
   * 
   * @throws IOException
   * @throws InvalidRangeException
   */
  public void parseScan() throws IOException, InvalidRangeException {
    // Load values to this scan instance itself, this method is called only when the scan passes the
    // predicate
    preLoadedMzValues = getMzValues();
    preLoadedIntensityValues = getIntensityValues();
    numOfDataPoints = getNumberOfDataPoints();

    setDataPoints(preLoadedMzValues, preLoadedIntensityValues, numOfDataPoints);
    spectrumType = SpectrumTypeDetectionAlgorithm.detectSpectrumType(preLoadedMzValues,
        preLoadedIntensityValues, numOfDataPoints);
  }
}
