/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_featureML;

import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.Collection;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class FeatureMLExportModularParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();
  public static final ComboParameter<FeatureListRowsFilter> filter = new ComboParameter<>(
      "Filter rows", "Limit the exported rows to those with MS/MS data (or annotated rows)",
      FeatureListRowsFilter.values(), FeatureListRowsFilter.ALL);
  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("featureML", "*.featureML"));
  public static final FileNameSuffixExportParameter filename = new FileNameSuffixExportParameter(
      "Filename", "Name of the output CSV file. "
                  + "Use pattern \"{}\" in the file name to substitute with feature list name. "
                  + "(i.e. \"blah{}blah.csv\" would become \"blahSourceFeatureListNameblah.csv\"). "
                  + "If the file already exists, it will be overwritten.", extensions, "fml");


  public FeatureMLExportModularParameters() {
    super(new Parameter[]{featureLists, filename, filter});
  }


  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    final boolean superCheck = super.checkParameterValues(errorMessages);

    // Check if substitute pattern is present in filename if several feature lists are selected by the user
    String plNamePattern = "{}";
    boolean substitute = this.getValue(filename).getPath().contains(plNamePattern);

    if (!substitute && this.getValue(featureLists).getMatchingFeatureLists().length > 1) {
      errorMessages.add("""
          Cannot export multiple feature lists to the same featureML file. Please use "{}" pattern in filename. \
          This will be replaced with the feature list name to generate one file per feature list.
          """);
    }

    return superCheck && errorMessages.isEmpty();
  }
}
