/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_waters;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFileImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesDataImpl;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WatersRawImportTask extends AbstractTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(WatersRawImportTask.class.getName());

  private final File rawFolder;
  @NotNull
  private final Class<? extends MZmineModule> module;
  @NotNull
  private final ParameterSet parameters;
  @NotNull
  private final MZmineProject project;
  private final ScanImportProcessorConfig processor;

  private long totalItems = 0;
  private long loadedItems = 0;
  private RawDataFileImpl dataFile;

  public WatersRawImportTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      File rawFolder, @NotNull final Class<? extends MZmineModule> module,
      @NotNull final ParameterSet parameters, @NotNull final MZmineProject project, @Nullable ScanImportProcessorConfig processor) {

    super(storage, moduleCallDate);
    this.rawFolder = rawFolder;
    this.module = module;
    this.parameters = parameters;
    this.project = project;

    this.processor = processor;
  }

  @Override
  public String getTaskDescription() {
    return "Importing Waters (MassLynx) raw data file %s. Scan %d/%d".formatted(rawFolder.getName(), loadedItems, totalItems);
  }

  @Override
  public double getFinishedPercentage() {
    return loadedItems / (double) totalItems;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    final boolean centroid = parameters.getValue(
        AllSpectralDataImportParameters.applyVendorCentroiding);

    try (final MassLynxDataAccess ml = new MassLynxDataAccess(rawFolder, centroid, storage,
        processor)) {
      readTotalItems(ml);

      dataFile = ml.createDataFile();
      OtherTimeSeriesDataImpl mrmData = null;
      OtherDataFile mrmFileDataFile = null;

      final List<SimpleScan> scans = new ArrayList<>();
      for (int function = 0; function < ml.getNumberOfFunctions(); function++) {
        if (isCanceled()) {
          return;
        }

        switch (ml.getFunctionType(function)) {
          case MS -> readMsFunction(ml, function, dataFile, scans);
          case IMS_MS -> readImsFunction(ml, function, (IMSRawDataFileImpl) dataFile, scans);
          case MRM -> {
            if (mrmData == null) {
              mrmFileDataFile = new OtherDataFileImpl(dataFile);
              mrmData = new OtherTimeSeriesDataImpl(mrmFileDataFile);
            }
            readMrmFunction(ml, function, mrmData);
          }
          case LOCKMASS -> {
            // skip lock mass loading
          }
        }
      }

      if (isCanceled()) {
        return;
      }

      // scan definition starts with func=xy, so we effectively compare the function number
      // unlikely that the func >= 9
      final List<SimpleScan> sortedScans = scans.stream().sorted(
              Comparator.comparingDouble(Scan::getRetentionTime).thenComparing(Scan::getScanDefinition))
          .toList();
      for (int i = 0; i < sortedScans.size(); i++) {
        final SimpleScan scan = sortedScans.get(i);
        scan.setScanNumber(i + 1);
        dataFile.addScan(scan);
      }

      final var appliedMethod = new SimpleFeatureListAppliedMethod(module, parameters,
          getModuleCallDate());
      dataFile.getAppliedMethods().add(appliedMethod);

      if(isCanceled()) {
        return;
      }

      project.addFile(dataFile);

      setStatus(TaskStatus.FINISHED);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error while reading waters raw file " + rawFolder.getAbsolutePath(),
          e);
      error(e.getMessage());
    }
  }

  private void readMrmFunction(MassLynxDataAccess ml, int function,
      OtherTimeSeriesDataImpl mrmData) {
    for (int mrm = 0; mrm < ml.getNumberOfMrmsInFunction(function); mrm++) {
      final OtherFeature otherFeature = ml.readMrm(function, mrm, mrmData);
      mrmData.addRawTrace(otherFeature);

      if (isCanceled()) {
        return;
      }
    }
  }

  private void readImsFunction(MassLynxDataAccess ml, int function, IMSRawDataFileImpl dataFile,
      List<SimpleScan> scans) {
    for (int frame = 0; frame < ml.getNumberOfScansInFunction(function); frame++) {
      final SimpleScan loadedFrame = ml.readFrame(dataFile, function, frame);
      scans.add(loadedFrame);
      loadedItems++;

      if (isCanceled()) {
        return;
      }
    }
  }

  private void readMsFunction(MassLynxDataAccess ml, int function, RawDataFileImpl dataFile,
      List<SimpleScan> scans) {
    for (int scan = 0; scan < ml.getNumberOfScansInFunction(function); scan++) {
      final SimpleScan loadedScan = ml.readScan(dataFile, function, scan);
      scans.add(loadedScan);
      loadedItems++;

      if (isCanceled()) {
        return;
      }
    }
  }

  private void readTotalItems(MassLynxDataAccess ml) {
    for (int i = 0; i < ml.getNumberOfFunctions(); i++) {
      switch (ml.getFunctionType(i)) {
        case MS, IMS_MS -> totalItems += ml.getNumberOfScansInFunction(i);
        case MRM -> totalItems += ml.getNumberOfMrmsInFunction(i);
        case LOCKMASS -> {
          // skip
        }
      }
    }
  }

  @Override
  public @Nullable RawDataFile getImportedRawDataFile() {
    if (isCanceled()) {
      return null;
    }

    return dataFile;
  }
}
