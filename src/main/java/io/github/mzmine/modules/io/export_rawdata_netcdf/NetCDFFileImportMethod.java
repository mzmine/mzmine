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

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzmine.datamodel.msdk.MSDKException;
import io.github.mzmine.datamodel.msdk.MSDKMethod;
import io.github.mzmine.datamodel.msdk.FileType;
import io.github.mzmine.datamodel.msdk.MsScan;
import io.github.mzmine.datamodel.msdk.RawDataFile;
import io.github.mzmine.datamodel.msdk.SimpleRawDataFile;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * <p>
 * NetCDFFileImportMethod class.
 * </p>
 */
public class NetCDFFileImportMethod implements MSDKMethod<RawDataFile> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private int parsedScans, totalScans = 0;

  private int scanStartPositions[];
  private float scanRetentionTimes[];

  private final @Nonnull File sourceFile;
  private NetcdfFile inputNetcdfFile;
  private SimpleRawDataFile newRawFile;
  private boolean canceled = false;

  private Variable massValueVariable, intensityValueVariable;

  private Predicate<MsScan> msScanPredicate;

  // Some software produces netcdf files with a scale factor such as 0.05
  // TODO: need junit test for this
  private double massValueScaleFactor = 1;
  private double intensityValueScaleFactor = 1;

  /**
   * <p>
   * Constructor for NetCDFFileImportMethod.
   * </p>
   *
   * @param sourceFile a {@link File} object.
   * @throws IOException if any.
   */
  public NetCDFFileImportMethod(@Nonnull File sourceFile) {
    this(sourceFile, s -> true);
  }

  /**
   * <p>Constructor for NetCDFFileImportMethod.</p>
   *
   * @param sourceFile a {@link File} object.
   * @param msScanPredicate a {@link Predicate} object.
   */
  public NetCDFFileImportMethod(@Nonnull File sourceFile, Predicate<MsScan> msScanPredicate) {
    this.sourceFile = sourceFile;
    this.msScanPredicate = msScanPredicate;
  }

  /** {@inheritDoc} */
  @Override
  public RawDataFile execute() throws MSDKException {

    logger.info("Started parsing file " + sourceFile);

    // Check if the file is readable
    if (!sourceFile.canRead()) {
      throw new MSDKException("Cannot read file " + sourceFile);
    }

    try {

      this.inputNetcdfFile = NetcdfFile.open(sourceFile.getPath());

      // Instantiate the raw file
      String fileName = sourceFile.getName();
      newRawFile =
          new NetCDFRawDataFile(fileName, Optional.of(sourceFile), FileType.NETCDF, inputNetcdfFile);

      // Read NetCDF variables
      readVariables();

      // Parse scans
      for (int scanIndex = 0; scanIndex < totalScans; scanIndex++) {

        // Check if cancel is requested
        if (canceled) {
          return null;
        }

        NetCDFMsScan buildingScan = new NetCDFMsScan(scanIndex + 1, scanStartPositions,
            scanRetentionTimes, massValueVariable, intensityValueVariable, massValueScaleFactor,
            intensityValueScaleFactor);

        if (msScanPredicate.test(buildingScan))
          buildingScan.parseScan();

        newRawFile.addScan(buildingScan);
        parsedScans++;

      }

    } catch (Exception e) {
      throw new MSDKException(e);
    }

    logger.info("Finished parsing " + sourceFile + ", parsed " + parsedScans + " scans");

    return newRawFile;

  }

  private void readVariables() throws MSDKException, IOException {

    /*
     * DEBUG: dump all variables for (Variable v : inputFile.getVariables()) {
     * System.out.println("variable " + v.getShortName()); }
     */

    // Find mass_values and intensity_values variables
    massValueVariable = inputNetcdfFile.findVariable("mass_values");
    if (massValueVariable == null) {
      logger.error("Could not find variable mass_values");
      throw (new MSDKException("Could not find variable mass_values"));
    }
    assert (massValueVariable.getRank() == 1);

    Attribute massScaleFacAttr = massValueVariable.findAttribute("scale_factor");
    if (massScaleFacAttr != null) {
      massValueScaleFactor = massScaleFacAttr.getNumericValue().doubleValue();
    }

    intensityValueVariable = inputNetcdfFile.findVariable("intensity_values");
    if (intensityValueVariable == null) {
      logger.error("Could not find variable intensity_values");
      throw (new MSDKException("Could not find variable intensity_values"));
    }
    assert (intensityValueVariable.getRank() == 1);

    Attribute intScaleFacAttr = intensityValueVariable.findAttribute("scale_factor");
    if (intScaleFacAttr != null) {
      intensityValueScaleFactor = intScaleFacAttr.getNumericValue().doubleValue();
    }

    // Read number of scans
    Variable scanIndexVariable = inputNetcdfFile.findVariable("scan_index");
    if (scanIndexVariable == null) {
      throw (new MSDKException("Could not find variable scan_index"));
    }
    totalScans = scanIndexVariable.getShape()[0];
    logger.debug("Found " + totalScans + " scans");

    // Read scan start position. An extra element is required, because
    // element totalScans+1 is used to
    // find the stop position for last scan
    scanStartPositions = new int[totalScans + 1];

    Array scanIndexArray = scanIndexVariable.read();
    IndexIterator scanIndexIterator = scanIndexArray.getIndexIterator();
    int ind = 0;
    while (scanIndexIterator.hasNext()) {
      scanStartPositions[ind] = ((Integer) scanIndexIterator.next());
      ind++;
    }

    // Calc stop position for the last scan
    // This defines the end index of the last scan
    scanStartPositions[totalScans] = (int) massValueVariable.getSize();

    // Start scan RT
    scanRetentionTimes = new float[totalScans];
    Variable scanTimeVariable = inputNetcdfFile.findVariable("scan_acquisition_time");
    if (scanTimeVariable == null) {
      throw (new IOException(
          "Could not find variable scan_acquisition_time from file " + sourceFile));
    }
    Array scanTimeArray = null;
    scanTimeArray = scanTimeVariable.read();
    IndexIterator scanTimeIterator = scanTimeArray.getIndexIterator();
    ind = 0;
    while (scanTimeIterator.hasNext()) {
      if (scanTimeVariable.getDataType().getPrimitiveClassType() == float.class) {
        scanRetentionTimes[ind] = (Float) (scanTimeIterator.next());
      }
      if (scanTimeVariable.getDataType().getPrimitiveClassType() == double.class) {
        scanRetentionTimes[ind] = ((Double) scanTimeIterator.next()).floatValue();
      }
      ind++;
    }
    // End scan RT

    /*
     * Fix problems caused by new QStar data converter:
     * 
     * 1) assume scan is missing when scan_index[i]<0 for these scans
     * 
     * 2) fix variables:
     * 
     * - scan_acquisition_time: interpolate/extrapolate using times of present scans
     * 
     * - scan_index: fill with following good value
     *
     * TODO: need junit test for this
     */

    // Calculate number of good scans
    int numberOfGoodScans = 0;
    for (int i = 0; i < totalScans; i++) {
      if (scanStartPositions[i] >= 0) {
        numberOfGoodScans++;
      }
    }

    // Is there need to fix something?
    if (numberOfGoodScans < totalScans) {

      // Fix scan_acquisition_time
      // - calculate average delta time between present scans
      double sumDelta = 0;
      int n = 0;
      for (int i = 0; i < totalScans; i++) {
        // Is this a present scan?
        if (scanStartPositions[i] >= 0) {
          // Yes, find next present scan
          for (int j = i + 1; j < totalScans; j++) {
            if (scanStartPositions[j] >= 0) {
              sumDelta += (scanRetentionTimes[j] - scanRetentionTimes[i]) / ((double) (j - i));
              n++;
              break;
            }
          }
        }
      }

      double avgDelta = sumDelta / (double) n;
      // - fill missing scan times using nearest good scan and avgDelta
      for (int i = 0; i < totalScans; i++) {
        // Is this a missing scan?
        if (scanStartPositions[i] < 0) {
          // Yes, find nearest present scan
          int nearestI = Integer.MAX_VALUE;
          for (int j = 1; 1 < 2; j++) {
            if ((i + j) < totalScans) {
              if (scanStartPositions[i + j] >= 0) {
                nearestI = i + j;
                break;
              }
            }
            if ((i - j) >= 0) {
              if (scanStartPositions[i - j] >= 0) {
                nearestI = i + j;
                break;
              }
            }

            // Out of bounds?
            if (((i + j) >= totalScans) && ((i - j) < 0)) {
              break;
            }
          }

          if (nearestI != Integer.MAX_VALUE) {

            scanRetentionTimes[i] =
                (float) (scanRetentionTimes[nearestI] + (i - nearestI) * avgDelta);

          } else {
            if (i > 0) {
              scanRetentionTimes[i] = scanRetentionTimes[i - 1];
            } else {
              scanRetentionTimes[i] = 0;
            }
            logger.error("ERROR: Could not fix incorrect QStar scan times.");
          }
        }
      }

      // Fix scanStartPositions by filling gaps with next good value
      for (int i = 0; i < totalScans; i++) {
        if (scanStartPositions[i] < 0) {
          for (int j = i + 1; j < (totalScans + 1); j++) {
            if (scanStartPositions[j] >= 0) {
              scanStartPositions[i] = scanStartPositions[j];
              break;
            }
          }
        }
      }
    }

  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public RawDataFile getResult() {
    return newRawFile;
  }

  /** {@inheritDoc} */
  @Override
  public Float getFinishedPercentage() {
    return totalScans == 0 ? null : (float) parsedScans / totalScans;
  }

  /** {@inheritDoc} */
  @Override
  public void cancel() {
    this.canceled = true;
  }

}
