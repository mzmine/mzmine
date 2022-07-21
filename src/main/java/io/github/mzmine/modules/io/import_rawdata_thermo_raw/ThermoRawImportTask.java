/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_thermo_raw;

import com.google.common.collect.Range;
import com.sun.jna.Platform;
import io.github.msdk.datamodel.MsScan;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.main.ExternalTool;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_mzml.ConversionUtils;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.MzMLFileImportMethod;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLMsScan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;


/**
 * This module binds spawns a separate process that dumps the native format's data in a text+binary
 * form into its standard output. This class then reads the output of that process.
 */
public class ThermoRawImportTask extends AbstractTask {

  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final File fileToOpen;
  private final MZmineProject project;
  private final RawDataFile newMZmineFile;
  private Process dumper = null;

  private String taskDescription;
  private int totalScans = 0, parsedScans = 0;

  /*
   * These variables are used during parsing of the RAW dump.
   */
  private final int scanNumber = 0;
  private final int msLevel = 0;
  private final int precursorCharge = 0;
  private final float retentionTime = 0;
  private String scanId;
  private PolarityType polarity;
  private Range<Double> mzRange;
  private final double precursorMZ = 0;
  private int numOfDataPoints;

  private MzMLFileImportMethod msdkTask;

  public ThermoRawImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // storage in raw data file
    this.project = project;
    this.fileToOpen = fileToOpen;
    taskDescription = "Opening file " + fileToOpen;
    this.newMZmineFile = newMZmineFile;
    this.parameters = parameters;
    this.module = module;
  }

  @Override
  public double getFinishedPercentage() {
    if (msdkTask == null || msdkTask.getFinishedPercentage() == null) {
      return 0.0;
    } else {
      return msdkTask.getFinishedPercentage().doubleValue();
    }
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Opening file " + fileToOpen);

    // Unzip ThermoRawFileParser
    try {
      final File thermoRawFileParserDir = ExternalTool.THERMO_RAW_PARSER.getExternalToolPath();

      String thermoRawFileParserCommand;

      if (Platform.isWindows()) {
        thermoRawFileParserCommand =
            thermoRawFileParserDir + File.separator + "ThermoRawFileParser.exe";
      } else if (Platform.isLinux()) {
        thermoRawFileParserCommand =
            thermoRawFileParserDir + File.separator + "ThermoRawFileParserLinux";
      } else if (Platform.isMac()) {
        thermoRawFileParserCommand =
            thermoRawFileParserDir + File.separator + "ThermoRawFileParserMac";
      } else {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Unsupported platform: JNA ID " + Platform.getOSType());
        return;
      }

      final String[] cmdLine = new String[]{ //
          thermoRawFileParserCommand, // program to run
          "-s", // output mzML to stdout
          "-p", // no peak picking
          "-z", // no zlib compression (higher speed)
          "-f=1", // no index, https://github.com/compomics/ThermoRawFileParser/issues/118
          "-i", // input RAW file name coming next
          fileToOpen.getPath() // input RAW file name
      };

      logger.fine("Running raw file parser as: " + String.join(" ", cmdLine));

      // Create a separate process and execute ThermoRawFileParser.
      // Use thermoRawFileParserDir as working directory; this is essential, otherwise the process will fail.
      dumper = Runtime.getRuntime().exec(cmdLine, null, thermoRawFileParserDir);

      // Get the stdout of ThermoRawFileParser process as InputStream
      InputStream mzMLStream = dumper.getInputStream();
      BufferedInputStream bufStream = new BufferedInputStream(mzMLStream);

      msdkTask = new MzMLFileImportMethod(bufStream);
      msdkTask.execute();
      io.github.msdk.datamodel.RawDataFile msdkFile = msdkTask.getResult();

      if (msdkFile == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("MSDK returned null");
        return;
      }
      totalScans = msdkFile.getScans().size();

      for (MsScan scan : msdkFile.getScans()) {

        if (isCanceled()) {
          bufStream.close();
          dumper.destroy();
          return;
        }

        Scan newScan = ConversionUtils.msdkScanToSimpleScan(newMZmineFile, (MzMLMsScan) scan);

        newMZmineFile.addScan(newScan);
        parsedScans++;
        taskDescription =
            "Importing " + fileToOpen.getName() + ", parsed " + parsedScans + "/" + totalScans
                + " scans";
      }

      // Finish
      bufStream.close();
      dumper.destroy();

      if (parsedScans == 0) {
        throw (new Exception("No scans found"));
      }

      if (parsedScans != totalScans) {
        throw (new Exception(
            "ThermoRawFileParser process crashed before all scans were extracted (" + parsedScans
                + " out of " + totalScans + ")"));
      }

      newMZmineFile.getAppliedMethods()
          .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
      project.addFile(newMZmineFile);

    } catch (Throwable e) {

      e.printStackTrace();

      if (dumper != null) {
        dumper.destroy();
      }

      if (getStatus() == TaskStatus.PROCESSING) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(ExceptionUtils.exceptionToString(e));
      }

      return;
    }

    logger.info("Finished parsing " + fileToOpen + ", parsed " + parsedScans + " scans");
    setStatus(TaskStatus.FINISHED);

  }

  @Override
  public String getTaskDescription() {
    return taskDescription;
  }


  @Override
  public void cancel() {
    super.cancel();

    if (msdkTask != null) {
      msdkTask.cancel();
    }

    // Try to destroy the dumper process
    if (dumper != null) {
      dumper.destroy();
    }
  }

}
