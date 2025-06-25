package io.github.mzmine.modules.tools.output_compare_csv;

import io.github.mzmine.modules.tools.output_compare_csv.CheckResult.Severity;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.files.ExtensionFilters;
import java.util.List;


public class CompareModularCsvParameters extends SimpleParameterSet {

  public static final FileNameParameter baseFile = new FileNameParameter("Base file",
      "This is the csv base or original file that is used as base truth.",
      List.of(ExtensionFilters.CSV), FileSelectionType.OPEN, false);
  public static final FileNameParameter compareFile = new FileNameParameter("Compare file",
      "This is csv file compared to the base file.", List.of(ExtensionFilters.CSV),
      FileSelectionType.OPEN, false);

  public static final OptionalParameter<FileNameParameter> outFile = new OptionalParameter<>(
      new FileNameSuffixExportParameter("Results file",
          "This is file compared to the base or original file.", List.of(ExtensionFilters.CSV),
          "modular_csv_comparison_results", false), false);

  public static final ComboParameter<CheckResult.Severity> filterLevel = new ComboParameter<>(
      "Filter messages",
      "Filter messages to include levels %s<%s<%s.".formatted(Severity.INFO, Severity.WARN,
          Severity.ERROR), Severity.values(), Severity.INFO);

  public CompareModularCsvParameters() {
    super(baseFile, compareFile, filterLevel, outFile);
  }
}
