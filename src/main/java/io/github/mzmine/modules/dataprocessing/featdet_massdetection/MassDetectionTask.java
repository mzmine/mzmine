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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
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
  private final SelectedScanTypes scanTypes;
  private final Boolean denormalizeMSnScans;
  // scan counter
  private int processedScans = 0, totalScans = 0;
  // Mass detector
  private final MZmineProcessingStep<MassDetector> massDetector;
  // for outputting file
  private final File outFilename;
  private final boolean saveToCDF;
  private final ParameterSet parameters;

  /**
   * @param dataFile
   * @param parameters
   * @param storageMemoryMap
   */
  public MassDetectionTask(RawDataFile dataFile, ParameterSet parameters,
      MemoryMapStorage storageMemoryMap, @NotNull Instant moduleCallDate) {
    super(storageMemoryMap, moduleCallDate);

    this.dataFile = dataFile;

    this.massDetector = parameters.getParameter(MassDetectionParameters.massDetector).getValue();

    this.scanSelection = parameters.getParameter(MassDetectionParameters.scanSelection).getValue();

    this.saveToCDF = parameters.getParameter(MassDetectionParameters.outFilenameOption).getValue();
    this.scanTypes = parameters.getParameter(MassDetectionParameters.scanTypes).getValue();

    this.outFilename = parameters.getParameter(MassDetectionParameters.outFilenameOption)
        .getEmbeddedParameter().getValue();

    denormalizeMSnScans = parameters.getValue(MassDetectionParameters.denormalizeMSnScans);

    this.parameters = parameters;

  }

  @Override
  public String getTaskDescription() {
    return "Detecting masses in " + dataFile;
  }

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

  @Override
  public void run() {

    // make arrays to contain everything you need
    ArrayList<Integer> pointsInScans = new ArrayList<>();
    ArrayList<Double> allMZ = new ArrayList<>();
    ArrayList<Double> allIntensities = new ArrayList<>();
    // indices of full mass list where scan starts?
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

      // uses only a single array for each (mz and intensity) to loop over all scans
      ScanDataAccess data = EfficientDataAccess.of(dataFile, EfficientDataAccess.ScanDataType.RAW,
          scanSelection);
      totalScans = data.getNumberOfScans();

      MassDetector detector = massDetector.getModule();
      ParameterSet parameterSet = massDetector.getParameterSet();

      // all scans
      while (data.hasNextScan()) {
        if (isCanceled()) {
          return;
        }

        Scan scan = data.nextScan();

        double[][] mzPeaks = null;
        if (scanTypes.applyTo(scan)) {
          // run mass detection on data object
          // [mzs, intensities]
          mzPeaks = detector.getMassValues(data, parameterSet);

          if (denormalizeMSnScans && Objects.requireNonNullElse(scan.getMSLevel(), 1) > 1) {
            ScanUtils.denormalizeIntensitiesMultiplyByInjectTime(mzPeaks[1],
                scan.getInjectionTime());
          }

          // add mass list to scans and frames
          scan.addMassList(new SimpleMassList(getMemoryMapStorage(), mzPeaks[0], mzPeaks[1]));
        }

        if (scan instanceof SimpleFrame frame && (scanTypes == SelectedScanTypes.MOBLITY_SCANS
            || scanTypes == SelectedScanTypes.SCANS)) {
          // for ion mobility, detect subscans, too
          frame.getMobilityScanStorage()
              .generateAndAddMobilityScanMassLists(getMemoryMapStorage(), detector, parameterSet,
                  denormalizeMSnScans);
        }

        if (this.saveToCDF && mzPeaks != null) {
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
        if (!outFilename.getParentFile().exists()) {
          final boolean created = outFilename.getParentFile().mkdirs();
          if (!created) {
            logger.warning(() -> "Cannot create file " + outFilename.getAbsolutePath()
                + " to save mass detection results.");
          }
        }

        if (outFilename.getParentFile().exists()) {
          logger.info("Saving mass detector results to netCDF file " + outFileNamePath);
          NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3,
              outFileNamePath, null);

          Dimension dim_massValues = writer.addDimension(null, "mass_values", allMZ.size());
          Dimension dim_intensityValues = writer.addDimension(null, "intensity_values",
              allIntensities.size());
          Dimension dim_scanIndex = writer.addDimension(null, "scan_index", startIndex.size() - 1);
          Dimension dim_scanAcquisitionTime = writer.addDimension(null, "scan_acquisition_time",
              scanAcquisitionTime.size());
          Dimension dim_totalIntensity = writer.addDimension(null, "total_intensity",
              totalIntensity.size());
          Dimension dim_pointsInScans = writer.addDimension(null, "point_count",
              pointsInScans.size());

          // add dimensions to list
          List<Dimension> dims = new ArrayList<>();
          dims.add(dim_massValues);
          dims.add(dim_intensityValues);
          dims.add(dim_scanIndex);
          dims.add(dim_scanAcquisitionTime);
          dims.add(dim_totalIntensity);
          dims.add(dim_pointsInScans);

          // make the variables that contain the actual data I think.
          Variable var_massValues = writer.addVariable(null, "mass_values", DataType.DOUBLE,
              "mass_values");
          Variable var_intensityValues = writer.addVariable(null, "intensity_values",
              DataType.DOUBLE, "intensity_values");
          Variable var_scanIndex = writer.addVariable(null, "scan_index", DataType.INT,
              "scan_index");
          Variable var_scanAcquisitionTime = writer.addVariable(null, "scan_acquisition_time",
              DataType.DOUBLE, "scan_acquisition_time");
          Variable var_totalIntensity = writer.addVariable(null, "total_intensity", DataType.DOUBLE,
              "total_intensity");
          Variable var_pointsInScans = writer.addVariable(null, "point_count", DataType.INT,
              "point_count");

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
          ArrayDouble.D1 arr_scanAcquisitionTime = new ArrayDouble.D1(
              dim_scanAcquisitionTime.getLength());
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
      }

      dataFile.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(MassDetectionModule.class, parameters,
              getModuleCallDate()));
    } catch (Exception e) {
      e.printStackTrace();
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
    }

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished mass detector on " + dataFile);

  }
}
