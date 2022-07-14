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

package io.github.mzmine.modules.io.import_rawdata_thermo_raw;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Range;
import com.google.common.io.CharStreams;
import com.sun.jna.Platform;
import io.github.msdk.datamodel.MsScan;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_mzml.ConversionUtils;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.MzMLFileImportMethod;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLMsScan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.ZipUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;


/**
 * This module binds spawns a separate process that dumps the native format's data in a text+binary
 * form into its standard output. This class then reads the output of that process.
 */
public class ThermoRawImportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private File fileToOpen;
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;

  private Process dumper = null;

  private String taskDescription;
  private int totalScans = 0, parsedScans = 0;

  /*
   * These variables are used during parsing of the RAW dump.
   */
  private int scanNumber = 0, msLevel = 0, precursorCharge = 0, numOfDataPoints;
  private String scanId;
  private PolarityType polarity;
  private Range<Double> mzRange;
  private float retentionTime = 0;
  private double precursorMZ = 0;

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

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (msdkTask == null || msdkTask.getFinishedPercentage() == null) {
      return 0.0;
    } else {
      return msdkTask.getFinishedPercentage().doubleValue();
    }
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Opening file " + fileToOpen);

    // Unzip ThermoRawFileParser
    try {
      final File thermoRawFileParserDir = unzipThermoRawFileParser();
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

      final String cmdLine[] = new String[]{ //
          thermoRawFileParserCommand, // program to run
          "-s", // output mzML to stdout
          "-p", // no peak picking
          "-z", // no zlib compression (higher speed)
          "-f=1", // no index, https://github.com/compomics/ThermoRawFileParser/issues/118
          "-i", // input RAW file name coming next
          fileToOpen.getPath() // input RAW file name
      };

      // Create a separate process and execute ThermoRawFileParser.
      // Use thermoRawFileParserDir as working directory; this is essential, otherwise the process will fail.
      logger.finest("Starting a separate process with cmdline " + Arrays.toString(cmdLine));
      dumper = Runtime.getRuntime().exec(cmdLine, null, thermoRawFileParserDir);

      // Get the stdout of ThermoRawFileParser process as InputStream
      InputStream mzMLStream = dumper.getInputStream();
      BufferedInputStream bufStream = new BufferedInputStream(mzMLStream);
      InputStream errorStream = dumper.getErrorStream();

      msdkTask = new MzMLFileImportMethod(bufStream);
      msdkTask.execute();
      io.github.msdk.datamodel.RawDataFile msdkFile = msdkTask.getResult();

      if (msdkFile == null) {
        bufStream.close();
        errorStream.close();
        dumper.destroy();
        setStatus(TaskStatus.ERROR);
        setErrorMessage("MSDK returned null");
        return;
      }
      totalScans = msdkFile.getScans().size();

      for (MsScan scan : msdkFile.getScans()) {

        if (isCanceled()) {
          bufStream.close();
          errorStream.close();
          dumper.destroy();
          FileUtils.deleteDirectory(thermoRawFileParserDir);
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

      if (errorStream.available() > 0) {
        String errMsg =
            "ThermoRawFileParser returned error output: " + IOUtils.toString(errorStream,
                StandardCharsets.UTF_8);
        errorStream.close();
        logger.warning(errMsg);
        setStatus(TaskStatus.ERROR);
        setErrorMessage(errMsg);
        dumper.destroy();
        FileUtils.deleteDirectory(thermoRawFileParserDir);
        return;
      }

      errorStream.close();
      dumper.destroy();
      FileUtils.deleteDirectory(thermoRawFileParserDir);

      if (parsedScans == 0) {
        String errMsg = "Parsing completed, but no scans were found.";
        setStatus(TaskStatus.ERROR);
        setErrorMessage(errMsg);
        return;
      }

      if (parsedScans != totalScans) {
        String errMsg =
            "ThermoRawFileParser process crashed before all scans were extracted (" + parsedScans
                + " out of " + totalScans + ")";
        setStatus(TaskStatus.ERROR);
        setErrorMessage(errMsg);
        return;
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

  private File unzipThermoRawFileParser() throws IOException {

    File thermoRawFileParserFolder = Files.createTempDirectory("mzmine_thermo_raw_parser").toFile();

    logger.finest("Unpacking ThermoRawFileParser to folder " + thermoRawFileParserFolder);
    InputStream zipStream = getClass()
        .getResourceAsStream("/vendorlib/thermo/ThermoRawFileParser.zip");
    if (zipStream == null) {
      throw new IOException(
          "Failed to open the resource /vendorlib/thermo/ThermoRawFileParser.zip");
    }
    ZipInputStream zipInputStream = new ZipInputStream(zipStream);
    ZipUtils.unzipStream(zipInputStream, thermoRawFileParserFolder);
    zipInputStream.close();

    // Delete the temporary folder on application exit
    FileUtils.forceDeleteOnExit(thermoRawFileParserFolder);

    return thermoRawFileParserFolder;
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
