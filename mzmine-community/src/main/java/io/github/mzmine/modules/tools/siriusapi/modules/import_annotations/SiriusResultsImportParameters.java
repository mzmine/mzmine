package io.github.mzmine.modules.tools.siriusapi.modules.import_annotations;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class SiriusResultsImportParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1, 1);

  public static final FileNameParameter sirius = new FileNameParameter("Sirius project file",
      "Select the sirius project file to import results from.",
      List.of(new ExtensionFilter("Sirius project", "*.sirius")), FileSelectionType.OPEN);

  public SiriusResultsImportParameters() {
    super(flist, sirius);
  }
}
