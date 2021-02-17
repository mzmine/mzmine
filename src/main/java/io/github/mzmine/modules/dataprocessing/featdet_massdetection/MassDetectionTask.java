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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.masslist.FrameMassList;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

// import ucar.ma2.*;

public class MassDetectionTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  private final MemoryMapStorage storageMemoryMap;
  // scan counter
  private int processedScans = 0, totalScans = 0;
  // Mass detector
  private MZmineProcessingStep<MassDetector> massDetector;
  // for outputting file
  private File outFilename;
  private boolean saveToCDF;
  private ParameterSet parameters;

  /**
   * @param dataFile
   * @param parameters
   * @param storageMemoryMap
   */
  public MassDetectionTask(RawDataFile dataFile, ParameterSet parameters,
      MemoryMapStorage storageMemoryMap) {

    this.dataFile = dataFile;

    this.massDetector = parameters.getParameter(MassDetectionParameters.massDetector).getValue();

    this.scanSelection = parameters.getParameter(MassDetectionParameters.scanSelection).getValue();

    this.saveToCDF = parameters.getParameter(MassDetectionParameters.outFilenameOption).getValue();
    this.storageMemoryMap = storageMemoryMap;

    this.outFilename = MassDetectionParameters.outFilenameOption.getEmbeddedParameter().getValue();

    this.parameters = parameters;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Detecting masses in " + dataFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0;
    } else {
      return (double) processedScans / totalScans;
    }
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {

    // make arrays to contain everything you need
    ArrayList<Integer> pointsInScans = new ArrayList<>();
    ArrayList<Double> allMZ = new ArrayList<>();
    ArrayList<Double> allIntensities = new ArrayList<>();
    // idecies of full mass list where scan starts?
    ArrayList<Integer> startIndex = new ArrayList<>();
    ArrayList<Float> scanAcquisitionTime = new ArrayList<>();
    // XCMS needs this one
    ArrayList<Double> totalIntensity = new ArrayList<>();

    double curTotalIntensity;
    int lastPointCount = 0;

    startIndex.add(0);

    try {

      setStatus(TaskStatus.PROCESSING);

      logger.info("Started mass detector on " + dataFile);

      final Scan scans[] = scanSelection.getMatchingScans(dataFile);
      totalScans = scans.length;
      // Process scans one by one
      for (Scan scan : scans) {

        if (isCanceled()) {
          return;
        }

        MassDetector detector = massDetector.getModule();
        // [mzs, intensities]
        double[][] mzPeaks = detector.getMassValues(scan, massDetector.getParameterSet());

        if (scan instanceof Frame) {
          // for ion mobility, detect subscans, too
          FrameMassList frameMassList = new FrameMassList(storageMemoryMap, mzPeaks[0], mzPeaks[1]);
          Frame frame = (Frame) scan;
          frameMassList.generateAndAddMobilityScanMassLists(frame.getMobilityScans(),
              storageMemoryMap, detector, massDetector.getParameterSet());
        } else {
          SimpleMassList newMassList = new SimpleMassList(storageMemoryMap, mzPeaks[0], mzPeaks[1]);
          scan.addMassList(newMassList);
        }

        if (this.saveToCDF) {
          curTotalIntensity = 0;
          double[] mzs = mzPeaks[0];
          double[] intensities = mzPeaks[1];
          int size = mzs.length;
          for (int a = 0; a < size; a++) {
            allMZ.add(mzs[a]);
            allIntensities.add(intensities[a]);
            curTotalIntensity += intensities[a];
          }

          scanAcquisitionTime.add(scan.getRetentionTime());
          pointsInScans.add(0);
          startIndex.add(mzPeaks.length + lastPointCount);
          totalIntensity.add(curTotalIntensity);

          lastPointCount = mzPeaks.length + lastPointCount;
        }

        processedScans++;
      }

      if (this.saveToCDF) {
        // ************** write mass list
        // *******************************
        final String outFileNamePath = outFilename.getPath();
        logger.info("Saving mass detector results to netCDF file " + outFileNamePath);
        NetcdfFileWriter writer =
            NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, outFileNamePath, null);

        Dimension dim_massValues = writer.addDimension(null, "mass_values", allMZ.size());
        Dimension dim_intensityValues =
            writer.addDimension(null, "intensity_values", allIntensities.size());
        Dimension dim_scanIndex = writer.addDimension(null, "scan_index", startIndex.size() - 1);
        Dimension dim_scanAcquisitionTime =
            writer.addDimension(null, "scan_acquisition_time", scanAcquisitionTime.size());
        Dimension dim_totalIntensity =
            writer.addDimension(null, "total_intensity", totalIntensity.size());
        Dimension dim_pointsInScans =
            writer.addDimension(null, "point_count", pointsInScans.size());

        // add dimensions to list
        List<Dimension> dims = new ArrayList<>();
        dims.add(dim_massValues);
        dims.add(dim_intensityValues);
        dims.add(dim_scanIndex);
        dims.add(dim_scanAcquisitionTime);
        dims.add(dim_totalIntensity);
        dims.add(dim_pointsInScans);

        // make the variables that contain the actual data I think.
        Variable var_massValues =
            writer.addVariable(null, "mass_values", DataType.DOUBLE, "mass_values");
        Variable var_intensityValues =
            writer.addVariable(null, "intensity_values", DataType.DOUBLE, "intensity_values");
        Variable var_scanIndex = writer.addVariable(null, "scan_index", DataType.INT, "scan_index");
        Variable var_scanAcquisitionTime = writer.addVariable(null, "scan_acquisition_time",
            DataType.DOUBLE, "scan_acquisition_time");
        Variable var_totalIntensity =
            writer.addVariable(null, "total_intensity", DataType.DOUBLE, "total_intensity");
        Variable var_pointsInScans =
            writer.addVariable(null, "point_count", DataType.INT, "point_count");

        var_massValues.addAttribute(new Attribute("units", "M/Z"));
        var_intensityValues.addAttribute(new Attribute("units", "Arbitrary Intensity Units"));
        var_scanIndex.addAttribute(new Attribute("units", "index"));
        var_scanAcquisitionTime.addAttribute(new Attribute("units", "seconds"));
        var_totalIntensity.addAttribute(new Attribute("units", "Arbitrary Intensity Units"));
        var_pointsInScans.addAttribute(new Attribute("units", "count"));

        var_massValues.addAttribute(new Attribute("scale_factor", 1.0));
        var_intensityValues.addAttribute(new Attribute("scale_factor", 1.0));
        var_scanIndex.addAttribute(new Attribute("scale_factor", 1.0));
        var_scanAcquisitionTime.addAttribute(new Attribute("scale_factor", 1.0));
        var_totalIntensity.addAttribute(new Attribute("scale_factor", 1.0));
        var_pointsInScans.addAttribute(new Attribute("scale_factor", 1.0));

        // create file
        writer.create();

        ArrayDouble.D1 arr_massValues = new ArrayDouble.D1(dim_massValues.getLength());
        ArrayDouble.D1 arr_intensityValues = new ArrayDouble.D1(dim_intensityValues.getLength());
        ArrayDouble.D1 arr_scanIndex = new ArrayDouble.D1(dim_scanIndex.getLength());
        ArrayDouble.D1 arr_scanAcquisitionTime =
            new ArrayDouble.D1(dim_scanAcquisitionTime.getLength());
        ArrayDouble.D1 arr_totalIntensity = new ArrayDouble.D1(dim_totalIntensity.getLength());
        ArrayDouble.D1 arr_pointsInScans = new ArrayDouble.D1(dim_pointsInScans.getLength());

        for (int i = 0; i < allMZ.size(); i++) {
          arr_massValues.set(i, allMZ.get(i));
          arr_intensityValues.set(i, allIntensities.get(i));
        }
        int i = 0;
        for (; i < scanAcquisitionTime.size(); i++) {
          arr_scanAcquisitionTime.set(i, scanAcquisitionTime.get(i) * 60);
          arr_pointsInScans.set(i, pointsInScans.get(i));
          arr_scanIndex.set(i, startIndex.get(i));
          arr_totalIntensity.set(i, totalIntensity.get(i));
        }
        // arr_scanIndex.set(i,startIndex.get(i));

        // For tiny test file
        // arr_intensityValues .set(0,200);
        // arr_scanIndex .set(0,0);
        // arr_scanAcquisitionTime .set(0,10);
        // arr_totalIntensity .set(0,200);
        // arr_pointsInScans .set(0,0);

        // arr_intensityValues .set(1,300);
        // arr_scanIndex .set(1,1);
        // arr_scanAcquisitionTime .set(1,20);
        // arr_totalIntensity .set(1,300);
        // arr_pointsInScans .set(1,0);

        writer.write(var_massValues, arr_massValues);
        writer.write(var_intensityValues, arr_intensityValues);
        writer.write(var_scanIndex, arr_scanIndex);
        writer.write(var_scanAcquisitionTime, arr_scanAcquisitionTime);
        writer.write(var_totalIntensity, arr_totalIntensity);
        writer.write(var_pointsInScans, arr_pointsInScans);
        writer.close();
      }
      dataFile.getAppliedMethods()
          .add(new SimpleFeatureListAppliedMethod(MassDetectionModule.class, parameters));
    } catch (Exception e) {
      e.printStackTrace();
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
    }

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished mass detector on " + dataFile);

  }
}
