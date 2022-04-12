package io.github.mzmine.modules.dataprocessing.id_sirius_cli.sirius_bridge;

import io.github.mzmine.modules.io.export_features_sirius.SiriusExportParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class SiriusRatingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1);

  public static final ParameterSetParameter siriusExportParam = new ParameterSetParameter(
      "Sirius export parameters", "Parameters for the Sirius export module.",
      new SiriusExportParameters(true).cloneParameterSet());

  public static final FileNameParameter siriusPath = new FileNameParameter("sirius.exe path",
      "The path to the sirius.exe", List.of(new ExtensionFilter("executable", "*.exe")),
      FileSelectionType.OPEN);

  public static final DirectoryParameter siriusProject = new DirectoryParameter(
      "Output directory (Sirius project)", "The directory for the generated sirius project.");

  public SiriusRatingParameters() {
    super(new Parameter[]{flist, siriusExportParam, siriusPath, siriusProject});
  }
}
