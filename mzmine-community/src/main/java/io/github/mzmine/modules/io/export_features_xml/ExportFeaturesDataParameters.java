package io.github.mzmine.modules.io.export_features_xml;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.Arrays;
import java.util.Collection;

public class ExportFeaturesDataParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameParameter file = new FileNameParameter("Export file name",
      "Specify the name of the exported feature lists. Use '{}' to automatically insert the name of the feature list.",
      FileSelectionType.SAVE);

  public static final ComboParameter<FeatureDataExportFormat> type = new ComboParameter<>(
      "Export format", "Select the format the feature data shall be saved as.",
      FeatureDataExportFormat.values(), FeatureDataExportFormat.XML);

  public ExportFeaturesDataParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/io_export_feature_data/export-feature-data.html",
        flists, file, type);
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    if (getValue(flists).getMatchingFeatureLists().length > 1 && !getValue(file).getName()
        .contains(SiriusExportTask.MULTI_NAME_PATTERN)) {
      errorMessages.add(
          "Multiple feature lists (%d) were selected, but the file name does not contain the pattern %s.".formatted(
              getValue(flists).getMatchingFeatureLists().length,
              inQuotes(SiriusExportTask.MULTI_NAME_PATTERN)));
    }

    return superCheck && errorMessages.isEmpty();
  }
}
