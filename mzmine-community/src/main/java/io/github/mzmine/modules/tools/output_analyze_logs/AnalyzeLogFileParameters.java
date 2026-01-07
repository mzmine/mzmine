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

import io.github.mzmine.modules.tools.output_compare_csv.CheckResult.Severity;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.files.ExtensionFilters;
import java.util.List;


public class AnalyzeLogFileParameters extends SimpleParameterSet {

  public static final FileNameParameter logFile = new FileNameParameter("Log file",
      "This is csv file compared to the base file.", List.of(ExtensionFilters.MZ_LOG),
      FileSelectionType.OPEN, false);

  public static final OptionalParameter<FileNameParameter> outFile = new OptionalParameter<>(
      new FileNameSuffixExportParameter("Results file",
          "This is file compared to the base or original file.", List.of(ExtensionFilters.CSV),
          "log_analysis_results", false), false);

  public static final ComboParameter<Severity> filterLevel = new ComboParameter<>("Filter messages",
      "Filter messages to include levels %s<%s<%s.".formatted(Severity.INFO, Severity.WARN,
          Severity.ERROR), Severity.values(), Severity.INFO);

  public AnalyzeLogFileParameters() {
    super(logFile, filterLevel, outFile);
  }
}
