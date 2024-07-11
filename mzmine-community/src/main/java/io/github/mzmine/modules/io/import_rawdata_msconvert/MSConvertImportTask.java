/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.ExternalTool;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class MSConvertImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MSConvertImportTask.class.getName());

  private final File path;
  private final ScanImportProcessorConfig config;
  private final MZmineProject project;
  private final MZmineModule module;
  private final ParameterSet parameters;

  protected MSConvertImportTask(@NotNull Instant moduleCallDate, File path,
      ScanImportProcessorConfig config, MZmineProject project, MZmineModule module,
      ParameterSet parameters) {
    super(moduleCallDate);
    this.path = path;
    this.config = config;
    this.project = project;
    this.module = module;
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final File msConvertPath = ExternalTool.MSCONVERT.getExternalToolPath();
    List<String> cmdLine = List.of(new File(msConvertPath, "msconvert.exe"), path.getAbsolutePath(),
        "-o", "-", // to stdout
        "--filter", "\"peakPicking true 1-\""); // vendor peak-picking

    Process dumper;
    try {
      dumper = Runtime.getRuntime().exec(cmdLine, null, msConvertPath);

      // Get the stdout of ThermoRawFileParser process as InputStream
      RawDataFile dataFile = null;
      MSDKmzMLImportTask msdkTask;
      try (InputStream mzMLStream = dumper.getInputStream()) //
//          BufferedInputStream bufStream = new BufferedInputStream(mzMLStream))//
      {

        msdkTask = new MSDKmzMLImportTask(project, path, mzMLStream, config, module, parameters,
            moduleCallDate, storage);

        this.addTaskStatusListener((task, newStatus, oldStatus) -> {
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
      dumper.destroy();

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
      if (dumper != null) {
        dumper.destroy();
      }

      if (getStatus() == TaskStatus.PROCESSING) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(ExceptionUtils.exceptionToString(e));
      }

      return;
    }

    logger.info(
        STR."Finished parsing \{fileToOpen}, parsed \{parsedScans} scans and after filtering remained \{convertedScans}");
    setStatus(TaskStatus.FINISHED);
  }
}
