package io.github.mzmine.modules.tools.rawfilerename;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class RawDataFileRenameParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter files = new RawDataFilesParameter(1, 1);

  public static final StringParameter newName = new StringParameter("New name",
      "The new name of the raw data file");

  public RawDataFileRenameParameters() {
    super(new Parameter[]{files, newName});
  }
}
