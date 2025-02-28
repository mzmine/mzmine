package io.github.mzmine.modules.tools.output_compare;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;


public class CompareModularCsvParameters extends SimpleParameterSet {

  public static final FileNameParameter baseFile = new FileNameParameter("Base file",
      "This is the base or original file that is used as base truth.", FileSelectionType.OPEN,
      false);
  public static final FileNameParameter compareFile = new FileNameParameter("Compare file",
      "This is file compared to the base or original file.", FileSelectionType.OPEN, false);

  public CompareModularCsvParameters() {
    super(baseFile, compareFile);
  }
}
