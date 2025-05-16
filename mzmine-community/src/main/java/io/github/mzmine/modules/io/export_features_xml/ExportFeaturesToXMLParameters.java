package io.github.mzmine.modules.io.export_features_xml;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class ExportFeaturesToXMLParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameParameter file = new FileNameParameter("Export file name",
      "Specify the name of the exported feature lists. Use '{}' to automatically insert the name of the feature list.", FileSelectionType.SAVE);


  public ExportFeaturesToXMLParameters() {
    super(flists, file);
  }
}
