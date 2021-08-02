/*
 * (C) Copyright 2015-2017 by MSDK Development Team
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.msdk.MSDKException;
import io.github.msdk.MSDKMethod;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.RawDataFile;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;

/**
 * <p>
 * This class contains methods which can be used to write data contained in a
 * {@link o.github.msdk.datamodel.rawdata.RawDataFile RawDataFile} to a file, in netCDF-3 format
 * </p>
 *
 */
public class NetCDFFileExportMethod implements MSDKMethod<Void> {

  private final @Nonnull RawDataFile rawDataFile;
  private final @Nonnull File target;
  private final @Nonnull double massValueScaleFactor;
  private final @Nonnull double intensityValueScaleFactor;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private boolean canceled = false;
  private int totalScans = 0;
  private float progress = 0f;
  private int lastLoggedProgress = 0;
  private List<MsScan> scans;
  private int[] scanStartPositions;
  private NetcdfFileWriter writer;

  /**
   * <p>
   * Constructor for NetCDFFileExportMethod.
   * </p>
   * 
   * @param rawDataFile the input {@link o.github.msdk.datamodel.rawdata.RawDataFile RawDataFile}
   *        which contains the data to be exported
   * @param target the target {@link File File} to write the data, in netCDF format
   */
  public NetCDFFileExportMethod(@Nonnull RawDataFile rawDataFile, @Nonnull File target) {
    this(rawDataFile, target, 1, 1);
  }

  /**
   * <p>
   * Constructor for NetCDFFileExportMethod.
   * </p>
   * 
   * @param rawDataFile the input {@link o.github.msdk.datamodel.rawdata.RawDataFile RawDataFile}
   *        which contains the data to be exported
   * @param target the target {@link File File} to write the data, in netCDF format
   * @param massValueScaleFactor double value by which the mass values have to be scaled by
   * @param intensityValueScaleFactor double value by which the intensity values have to be scaled
   *        by

   */
  public NetCDFFileExportMethod(@Nonnull RawDataFile rawDataFile, @Nonnull File target,
      double massValueScaleFactor, double intensityValueScaleFactor) {
    this.rawDataFile = rawDataFile;
    this.target = target;
    this.massValueScaleFactor = massValueScaleFactor;
    this.intensityValueScaleFactor = intensityValueScaleFactor;
  }

  /**
   * <p>
   * Execute the process of writing the data from the the input
   * {@link o.github.msdk.datamodel.rawdata.RawDataFile RawDataFile} to the target
   * {@link File File}
   * </p>
   * 
   * @return nothing, a void method
   */
  @Override
  public Void execute() throws MSDKException {

    logger.info("Started export of " + rawDataFile.getName() + " to " + target);

    scans = rawDataFile.getScans();
    totalScans = scans.size();
    scanStartPositions = new int[totalScans + 1];

    try {

      writer = NetcdfFileWriter.createNew(Version.netcdf3, target.getAbsolutePath());

      Array scanIndexArray = getScanIndexArray();
      Array scanTimeArray = getScanTimeArray();
      if (scanIndexArray == null)
        return null;

      List<Dimension> scanIndexDims = getScanIndexDims();
      Variable scanIndexVariable =
          writer.addVariable(null, "scan_index", DataType.INT, scanIndexDims);
      Variable scanTimeVariable =
          writer.addVariable(null, "scan_acquisition_time", DataType.FLOAT, scanIndexDims);

      progress = 0.2f;

      // data values storage dimension
      // Dimension to store the data arrays of various scans
      List<Dimension> pointNumValDims = getPointValDims();
      Variable massValueVariable = getMassValueVariable(pointNumValDims);
      Variable intensityValueVariable = getIntensityValueVariable(pointNumValDims);

      // populate the mass and the intensity values
      Array massValueArray =
          Array.factory(double.class, new int[] {scanStartPositions[totalScans]});
      Array intensityValueArray =
          Array.factory(double.class, new int[] {scanStartPositions[totalScans]});

      int idx = 0;
      double mzValues[] = new double[10000];
      float intensityValues[] = new float[10000];
      for (MsScan scan : scans) {
        mzValues = scan.getMzValues(mzValues);
        intensityValues = scan.getIntensityValues(intensityValues);

        if (canceled) {
          writer.close();
          return null;
        }

        for (int i = 0; i < scan.getNumberOfDataPoints(); i++) {
          massValueArray.setDouble(idx, mzValues[i]);
          intensityValueArray.setDouble(idx, intensityValues[i]);
          idx++;
        }

        progress = 0.2f + 0.6f * ((float) idx / scanStartPositions[totalScans]);
        if ((int) (progress * 100) >= lastLoggedProgress + 10) {
          lastLoggedProgress = (int) (progress * 10) * 10;
          logger.debug("Exporting in progress... " + lastLoggedProgress + "% completed");
        }

      }

      // Create the netCDF-3 file
      writer.create();

      // Write out the non-record variables
      writer.write(scanIndexVariable, scanIndexArray);
      writer.write(massValueVariable, massValueArray);
      writer.write(intensityValueVariable, intensityValueArray);
      writer.write(scanTimeVariable, scanTimeArray);

      // Close the writer
      writer.close();

      progress = 1.0f;

    } catch (IOException | InvalidRangeException e) {
      new MSDKException(e);
    }

    return null;
  }

  /**
   * 
   * @return scanIndexArray an {@link Array Array} containing scan indices of all scans
   * @throws IOException
   */
  private Array getScanIndexArray() throws IOException {
    // Populate the scan indices
    // Create a simple 1D array of type int, element i of the shape array contains the length of
    // the (i+1)th dimension of the array
    Array scanIndexArray = Array.factory(int.class, new int[] {totalScans});
    int idx = 0;
    for (MsScan scan : scans) {

      if (canceled) {
        writer.close();
        return null;
      }

      scanStartPositions[idx + 1] = scanStartPositions[idx] + scan.getNumberOfDataPoints();
      idx++;

    }

    for (int i = 0; i < scanStartPositions.length - 1; i++)
      scanIndexArray.setInt(i, scanStartPositions[i]);

    return scanIndexArray;
  }

  /**
   * 
   * @return scanIndexDims a {@link List List} containing all dimension definitions for
   *         the Scan Index Variable
   */
  private List<Dimension> getScanIndexDims() {
    // Write the scan indices
    // Create a variable and set the dimension scan_number=totalScans to it
    Dimension scanNumberDim = writer.addDimension(null, "scan_number", totalScans);
    List<Dimension> scanIndexDims = new ArrayList<>();
    scanIndexDims.add(scanNumberDim);

    return scanIndexDims;
  }

  /**
   * 
   * @return pointValDims a {@link List List} containing all dimension definitions for the
   *         mass and intensity value variables
   */
  private List<Dimension> getPointValDims() {
    // data values storage dimension
    // Dimension to store the data arrays of various scans
    Dimension pointNumDim =
        writer.addDimension(null, "point_number", scanStartPositions[totalScans]);
    List<Dimension> pointNumValDims = new ArrayList<>();
    pointNumValDims.add(pointNumDim);
    return pointNumValDims;
  }

  /**
   * 
   * @return scanTimeArray an {@link Array Array} containing scan retention times for all
   *         scans
   */
  private Array getScanTimeArray() {
    // Populate scan times
    Array scanTimeArray = Array.factory(float.class, new int[] {totalScans});
    int idx = 0;
    for (MsScan scan : scans)
      scanTimeArray.setFloat(idx++, scan.getRetentionTime());

    return scanTimeArray;
  }

  /**
   * Create mass values variable add its attributes and give it its dimension, short hand name and
   * data type
   * 
   * @param pointNumValDims a {@link List List} containing all dimension definitions for
   *        the mass and intensity value variables
   * @return massValueVariable {@link Variable Variable} containing the m/z data of the
   *         scans
   */
  private Variable getMassValueVariable(List<Dimension> pointNumValDims) {
    // Create mass values variable and add its attributes
    Variable massValueVariable =
        writer.addVariable(null, "mass_values", DataType.FLOAT, pointNumValDims);
    massValueVariable.addAttribute(new Attribute("units", "M/Z"));
    massValueVariable.addAttribute(new Attribute("scale_factor", massValueScaleFactor));

    return massValueVariable;
  }

  /**
   * Create intensity values variable add its attributes and give it its dimension, short hand name
   * and data type
   * 
   * @param pointNumValDims a {@link List List} containing all dimension definitions for
   *        the mass and intensity value variables
   * @return intensityValueVariable {@link Variable Variable} containing the intensity data
   *         of the scans
   */
  private Variable getIntensityValueVariable(List<Dimension> pointNumValDims) {
    // Create intensity values variable and add its attributes
    Variable intensityValueVariable =
        writer.addVariable(null, "intensity_values", DataType.FLOAT, pointNumValDims);
    intensityValueVariable.addAttribute(new Attribute("units", "Arbitrary Intensity Units"));
    intensityValueVariable.addAttribute(new Attribute("scale_factor", intensityValueScaleFactor));

    return intensityValueVariable;
  }

  /** {@inheritDoc} */
  @Override
  public Float getFinishedPercentage() {
    return progress;
  }

  /** {@inheritDoc} */
  @Override
  public Void getResult() {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public void cancel() {
    this.canceled = true;
  }

}
