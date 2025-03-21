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
