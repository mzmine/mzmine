/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_msconvert;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

public class MSConvertImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MSConvertImportTask.class.getName());

  private final File rawFilePath;
  private final ScanImportProcessorConfig config;
  private final MZmineProject project;
  private final Class<? extends MZmineModule> module;
  private final ParameterSet parameters;
  private MSDKmzMLImportTask msdkTask;
  private Boolean convertToFile = ConfigService.getConfiguration().getPreferences()
      .getValue(MZminePreferences.keepConvertedFile);

  public MSConvertImportTask(@NotNull Instant moduleCallDate, File path,
      ScanImportProcessorConfig config, MZmineProject project, Class<? extends MZmineModule> module,
      ParameterSet parameters) {
    super(moduleCallDate);
    this.rawFilePath = path;
    this.config = config;
    this.project = project;
    this.module = module;
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return msdkTask != null ? msdkTask.getTaskDescription()
        : "Waiting for conversion/Importing MS data file %s".formatted(rawFilePath);
  }

  @Override
  public double getFinishedPercentage() {
    return msdkTask != null ? msdkTask.getFinishedPercentage() : 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final File msConvertPath = MSConvert.getMsConvertPath();
    if (msConvertPath == null) {
      error("MSConvert not found. Please install MSConvert.");
      return;
    }
    final File mzMLFile = getMzMLFileName(rawFilePath);

    if (mzMLFile.exists()) {
      logger.finest(
          "Discovered mzml file for MS data file %s. Importing mzml file from %s.".formatted(
              rawFilePath, mzMLFile));
      importFromMzML(mzMLFile);
      return;
    }

    final List<String> cmdLine = buildCommandLine(rawFilePath, msConvertPath);

    if (convertToFile) {
      ProcessBuilder builder = new ProcessBuilder(cmdLine);
      try {
        final Process process = builder.start();
        while (process.isAlive()) { // wait for conversion to finish
          if (isCanceled()) {
            process.destroy();
          }
          TimeUnit.MILLISECONDS.sleep(100);
        }
      } catch (IOException | InterruptedException e) {
        logger.log(Level.WARNING, "Error while converting %s to mzML file.".formatted(rawFilePath),
            e);
        setStatus(TaskStatus.ERROR);
        return;
      }
      importFromMzML(mzMLFile);
    } else {
      importFromStream(rawFilePath, cmdLine);
    }

    if (!isCanceled()) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private @NotNull File getMzMLFileName(File filePath) {
    final String fileName = filePath.getName();
    final String extension = FileAndPathUtil.getExtension(fileName);
    final String mzMLName = fileName.replace(extension, "mzML");
    final File mzMLFile = new File(filePath.getParent(), mzMLName);
    return mzMLFile;
  }

  private void importFromMzML(File mzMLFile) {
    RawDataFile dataFile = null;
    ParameterUtils.replaceRawFileName(parameters, rawFilePath, mzMLFile);
    msdkTask = new MSDKmzMLImportTask(project, mzMLFile, config, module, parameters, moduleCallDate,
        storage);

    this.addTaskStatusListener((_, _, _) -> {
      if (isCanceled()) {
        msdkTask.cancel();
      }
    });

    dataFile = msdkTask.importStreamOrFile();
    if (msdkTask.isCanceled()) {
      setStatus(msdkTask.getStatus());
      if (msdkTask.getStatus() == TaskStatus.ERROR) {
        setErrorMessage(msdkTask.getErrorMessage());
      }
      return;
    }

    if (dataFile == null || isCanceled()) {
      return;
    }

    var totalScans = msdkTask.getTotalScansInMzML();
    var parsedScans = msdkTask.getParsedMzMLScans();
    var convertedScans = msdkTask.getConvertedScansAfterFilter();

    if (parsedScans == 0) {
      throw (new RuntimeException("No scans found"));
    }

    if (parsedScans != totalScans) {
      throw (new RuntimeException(
          "MSConvert process crashed before all scans were extracted (" + parsedScans + " out of "
              + totalScans + ")"));
    }
    msdkTask.addAppliedMethodAndAddToProject(dataFile);
  }

  private void importFromStream(File rawFilePath, List<String> cmdLine) {
    ProcessBuilder builder = new ProcessBuilder(cmdLine);
    Process process = null;
    try {
      process = builder.start();

      // Get the stdout of MSConvert process as InputStream
      RawDataFile dataFile = null;
      try (InputStream mzMLStream = process.getInputStream()) //
      {
        msdkTask = new MSDKmzMLImportTask(project, rawFilePath, mzMLStream, config, module,
            parameters, moduleCallDate, storage);

        this.addTaskStatusListener((_, _, _) -> {
          if (isCanceled()) {
            msdkTask.cancel();
          }
        });
        dataFile = msdkTask.importStreamOrFile();
      }

      if (dataFile == null || isCanceled()) {
        return;
      }

      var totalScans = msdkTask.getTotalScansInMzML();
      var parsedScans = msdkTask.getParsedMzMLScans();
      var convertedScans = msdkTask.getConvertedScansAfterFilter();

      // Finish
      process.destroy();

      if (parsedScans == 0) {
        throw (new RuntimeException("No scans found"));
      }

      if (parsedScans != totalScans) {
        throw (new RuntimeException(
            "ThermoRawFileParser process crashed before all scans were extracted (" + parsedScans
                + " out of " + totalScans + ")"));
      }

      msdkTask.addAppliedMethodAndAddToProject(dataFile);

    } catch (Throwable e) {
      if (process != null) {
        process.destroy();
      }

      if (getStatus() == TaskStatus.PROCESSING) {
        logger.log(Level.SEVERE, "Error while parsing file %s".formatted(rawFilePath), e);
        setErrorMessage(ExceptionUtils.exceptionToString(e));
        setStatus(TaskStatus.ERROR);
      }
    }
  }

  private @NotNull List<String> buildCommandLine(File filePath, File msConvertPath) {

    List<String> cmdLine = new ArrayList<>();
    cmdLine.addAll(List.of(inQuotes(msConvertPath.toString()), // MSConvert path
        inQuotes(filePath.getAbsolutePath()), // raw file path
        "-o", !convertToFile ? "-" /* to stdout */ : inQuotes(filePath.getParent()) //
    )); // vendor peak-picking

    if (convertToFile) {
      cmdLine.add("--zlib");
      cmdLine.add("--numpressPic");
      cmdLine.add("--numpressLinear");
    }

    if (ConfigService.getPreferences().getValue(MZminePreferences.applyPeakPicking)) {
      cmdLine.addAll(List.of("--filter", "\"peakPicking vendor msLevel=1-\""));
    }

    // deactivated, since waters files converted by MSConvert have poor quality.
    /*final RawDataFileType fileType = RawDataFileTypeDetector.detectDataFileType(rawFilePath);
    if (fileType == RawDataFileType.WATERS_RAW) {
      final PolarityType polarity = getWatersPolarity(filePath);
      if (polarity == PolarityType.POSITIVE) {
        logger.finest(
            "Determined polarity of file %s to be %s. Applying lockmass correction.".formatted(
                rawFilePath, polarity));
        cmdLine.addAll(List.of("--filter", "\"lockmassRefiner mz=556.276575 tol=0.1\""));
      } else if (polarity == PolarityType.NEGATIVE) {
        logger.finest(
            "Determined polarity of file %s to be %s. Applying lockmass correction.".formatted(
                rawFilePath, polarity));
        cmdLine.addAll(List.of("--filter", "\"lockmassRefiner mz=554.262022 tol=0.1\""));
      }
    }*/

    cmdLine.addAll(List.of("--filter",
        "\"titleMaker <RunId>.<ScanNumber>.<ScanNumber>.<ChargeState> File:\"\"\"^<SourcePath^>\"\"\", NativeID:\"\"\"^<Id^>\"\"\"\""));

    logger.finest("Running msconvert with command line: %s".formatted(cmdLine.toString()));
    return cmdLine;
  }

  private PolarityType getWatersPolarity(File rawFilePath) {
    final File file = new File(rawFilePath, "_extern.inf");
    if (!file.exists() || !file.canRead()) {
      return PolarityType.UNKNOWN;
    }

    final Pattern polarityPattern = Pattern.compile("(Polarity)(\\s+)([a-zA-Z]+)([+-])");

    // somehow does not work with Files.newBufferedReader
    try (var reader = new BufferedReader(new FileReader(file))) {
      String line = reader.readLine();
      while (line != null) {
        final Matcher matcher = polarityPattern.matcher(line);
        if (matcher.matches()) {
          final PolarityType polarity = PolarityType.fromSingleChar(matcher.group(4));
          return polarity;
        }
        line = reader.readLine();
      }
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Cannot determine raw data polarity for file %s. Cannot apply lockmass correction.".formatted(
              rawFilePath), e);
      return PolarityType.UNKNOWN;
    }
    return PolarityType.UNKNOWN;
  }
}
