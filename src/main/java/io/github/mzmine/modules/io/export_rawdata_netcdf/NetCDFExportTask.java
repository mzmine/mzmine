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

package io.github.mzmine.modules.io.export_rawdata_netcdf;

import io.github.mzmine.datamodel.Scan;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import org.jetbrains.annotations.NotNull;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;

public class NetCDFExportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private final RawDataFile dataFile;

  private final @NotNull double massValueScaleFactor = 1.0;
  private final @NotNull double intensityValueScaleFactor = 1.0;

  // User parameters
  private File outFilename;
  private List<Scan> scans;
  private int[] scanStartPositions;
  private NetcdfFileWriter writer;
  private int totalScanIndex = 0, totalScans;

  /**
   * @param dataFile
   */
  public NetCDFExportTask(RawDataFile dataFile, File outFilename) {
    super(null); // no new data stored -> null
    this.dataFile = dataFile;
    this.outFilename = outFilename;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Exporting file " + dataFile + " to " + outFilename;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (scanStartPositions == null || scanStartPositions[totalScans] == 0) {
      return 0.0;
    } else {
      return (double) totalScanIndex / scanStartPositions[totalScans];
    }
  }

  /**
   * @see Runnable#run()
   */
  public void run() {

    try {

      setStatus(TaskStatus.PROCESSING);

      logger.info("Started export of file " + dataFile + " to " + outFilename);

      scans = dataFile.getScans();
      totalScans = scans.size();
      scanStartPositions = new int[totalScans + 1];

      writer = NetcdfFileWriter.createNew(Version.netcdf3, outFilename.getAbsolutePath());

      Array scanIndexArray = getScanIndexArray();
      Array scanTimeArray = getScanTimeArray();
      if (scanIndexArray == null) {
        return;
      }

      List<Dimension> scanIndexDims = getScanIndexDims();
      Variable scanIndexVariable =
          writer.addVariable(null, "scan_index", DataType.INT, scanIndexDims);
      Variable scanTimeVariable =
          writer.addVariable(null, "scan_acquisition_time", DataType.FLOAT, scanIndexDims);

      // data values storage dimension
      // Dimension to store the data arrays of various scans
      List<Dimension> pointNumValDims = getPointValDims();
      Variable massValueVariable = getMassValueVariable(pointNumValDims);
      Variable intensityValueVariable = getIntensityValueVariable(pointNumValDims);

      // populate the mass and the intensity values
      Array massValueArray =
          Array.factory(double.class, new int[]{scanStartPositions[totalScans]});
      Array intensityValueArray =
          Array.factory(double.class, new int[]{scanStartPositions[totalScans]});

      double mzValues[] = new double[10000];
      double intensityValues[] = new double[10000];

      for (Scan scan : scans) {
        if (isCanceled()) {
          writer.close();
          return;
        }

        mzValues = scan.getMzValues(mzValues);
        intensityValues = scan.getIntensityValues(intensityValues);

        for (int i = 0; i < scan.getNumberOfDataPoints(); i++) {
          massValueArray.setDouble(totalScanIndex, mzValues[i]);
          intensityValueArray.setDouble(totalScanIndex, intensityValues[i]);
          totalScanIndex++;
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

      setStatus(TaskStatus.FINISHED);

      logger.info("Finished export of file " + dataFile + " to " + outFilename);

    } catch (Exception e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error in NetCDF file export: " + e.getMessage());
    }

  }

  /**
   * @return scanIndexArray an {@link Array Array} containing scan indices of all scans
   * @throws IOException
   */
  private Array getScanIndexArray() throws IOException {
    // Populate the scan indices
    // Create a simple 1D array of type int, element i of the shape array contains the length of
    // the (i+1)th dimension of the array
    Array scanIndexArray = Array.factory(int.class, new int[]{totalScans});
    int idx = 0;
    for (Scan scan : scans) {

      if (isCanceled()) {
        writer.close();
        return null;
      }

      scanStartPositions[idx + 1] = scanStartPositions[idx] + scan.getNumberOfDataPoints();
      idx++;

    }

    for (int i = 0; i < scanStartPositions.length - 1; i++) {
      scanIndexArray.setInt(i, scanStartPositions[i]);
    }

    return scanIndexArray;
  }

  /**
   * @return scanIndexDims a {@link List List} containing all dimension definitions for the Scan
   * Index Variable
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
   * @return pointValDims a {@link List List} containing all dimension definitions for the mass and
   * intensity value variables
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
   * @return scanTimeArray an {@link Array Array} containing scan retention times for all scans
   */
  private Array getScanTimeArray() {
    // Populate scan times
    Array scanTimeArray = Array.factory(float.class, new int[]{totalScans});
    int idx = 0;
    for (Scan scan : scans) {
      scanTimeArray.setFloat(idx++, scan.getRetentionTime());
    }

    return scanTimeArray;
  }

  /**
   * Create mass values variable add its attributes and give it its dimension, short hand name and
   * data type
   *
   * @param pointNumValDims a {@link List List} containing all dimension definitions for the mass
   *                        and intensity value variables
   * @return massValueVariable {@link Variable Variable} containing the m/z data of the scans
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
   * @param pointNumValDims a {@link List List} containing all dimension definitions for the mass
   *                        and intensity value variables
   * @return intensityValueVariable {@link Variable Variable} containing the intensity data of the
   * scans
   */
  private Variable getIntensityValueVariable(List<Dimension> pointNumValDims) {
    // Create intensity values variable and add its attributes
    Variable intensityValueVariable =
        writer.addVariable(null, "intensity_values", DataType.FLOAT, pointNumValDims);
    intensityValueVariable.addAttribute(new Attribute("units", "Arbitrary Intensity Units"));
    intensityValueVariable.addAttribute(new Attribute("scale_factor", intensityValueScaleFactor));

    return intensityValueVariable;
  }

}
