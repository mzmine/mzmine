/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_features_xml;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class ExportFeaturesDataParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameSuffixExportParameter file = new FileNameSuffixExportParameter(
      "Export file name",
      "Specify the name of the exported feature lists. Use '{}' to automatically insert the name of the feature list.",
      "features_data");

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

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
