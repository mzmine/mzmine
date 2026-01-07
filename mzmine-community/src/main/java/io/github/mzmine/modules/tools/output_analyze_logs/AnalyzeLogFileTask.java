/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.output_analyze_logs;

import io.github.mzmine.modules.tools.output_compare_csv.CheckResult;
import io.github.mzmine.modules.tools.output_compare_csv.CheckResult.Severity;
import io.github.mzmine.modules.tools.output_compare_csv.CheckResultWriter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class AnalyzeLogFileTask extends AbstractSimpleToolTask {

  private static final Logger logger = Logger.getLogger(AnalyzeLogFileTask.class.getName());
  private final List<CheckResult> checks = new ArrayList<>();

  public AnalyzeLogFileTask(final @NotNull Instant moduleCallDate,
      final @NotNull ParameterSet parameters) {
    super(moduleCallDate, parameters);
  }

  @Override
  protected void process() {
    final File logFile = parameters.getValue(AnalyzeLogFileParameters.logFile);

    checks.add(CheckResult.create("file paths", Severity.INFO, logFile.getAbsolutePath(),
        logFile.getAbsolutePath(), "Analyze log file paths"));
    checks.add(CheckResult.create("file names", Severity.INFO, logFile.getName(),
        logFile.getAbsolutePath(), "Analyze log file names"));

    MZmineLogFileReader reader = new MZmineLogFileReader();
    try {
      final LogFile mzmineLog = reader.readLogFile(logFile);

      for (final LogFileLine line : mzmineLog.lines()) {
        final CheckResult check = convertLineToCheck(line);
        checks.add(check);
      }

      // finalize and output
      final Severity filter = parameters.getValue(AnalyzeLogFileParameters.filterLevel);
      final File outFile = parameters.getEmbeddedParameterValueIfSelectedOrElse(
          AnalyzeLogFileParameters.outFile, null);

      CheckResultWriter.filterAndLogResults(filter, outFile, checks);

    } catch (IOException e) {
      error("Error in log analysis of file " + logFile.getAbsolutePath(), e);
      return;
    }
  }

  private CheckResult convertLineToCheck(final LogFileLine line) {
    return CheckResult.create(line.createIdentifier(), line.severity(), line.message());
  }

  @Override
  public String getTaskDescription() {
    return "Analyze log file";
  }
}
