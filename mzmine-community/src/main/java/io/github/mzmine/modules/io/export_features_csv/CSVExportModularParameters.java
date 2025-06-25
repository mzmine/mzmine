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

package io.github.mzmine.modules.io.export_features_csv;

import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import java.io.File;
import java.util.Collection;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

public class CSVExportModularParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(1);
  public static final StringParameter fieldSeparator = new StringParameter("Field separator",
      "Character(s) used to separate fields in the exported file", ",");
  public static final StringParameter idSeparator = new StringParameter("Identification separator",
      "Character(s) used to separate multi object columns in the exported file", ";");
  public static final BooleanParameter omitEmptyColumns = new BooleanParameter(
      "Remove empty columns", "Removes empty columns during data export", true);
  public static final ComboParameter<FeatureListRowsFilter> filter = new ComboParameter<>(
      "Filter rows", "Limit the exported rows to those with MS/MS data (or annotated rows)",
      FeatureListRowsFilter.values(), FeatureListRowsFilter.ALL);
  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv"), //
      new ExtensionFilter("All files", "*.*") //
  );
  public static final FileNameSuffixExportParameter filename = new FileNameSuffixExportParameter(
      "Filename", "Name of the output CSV file. "
      + "Use pattern \"{}\" in the file name to substitute with feature list name. "
      + "(i.e. \"blah{}blah.csv\" would become \"blahSourceFeatureListNameblah.csv\"). "
      + "If the file already exists, it will be overwritten.", extensions, "full_feature_table");


  public CSVExportModularParameters() {
    super(new Parameter[]{featureLists, filename, fieldSeparator, idSeparator, omitEmptyColumns,
        filter});
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    final boolean superCheck = super.checkParameterValues(errorMessages);

    // Check if substitute pattern is present in filename if several feature lists are selected by the user
    String plNamePattern = "{}";
    boolean substitute = this.getValue(filename).getPath().contains(plNamePattern);

    if (!substitute && this.getValue(featureLists).getMatchingFeatureLists().length > 1) {
      errorMessages.add("""
          Cannot export multiple feature lists to the same CSV file. Please use "{}" pattern in filename. \
          This will be replaced with the feature list name to generate one file per feature list.
          """);
    }

    return superCheck && errorMessages.isEmpty();
  }

  public static CSVExportModularParameters create(File csvExportFile,
      FeatureListRowsFilter rowsFilter, boolean omitEmpty, String idSeparator, String fieldSep,
      FeatureListsSelection featureListsSelection) {
    final ParameterSet parameters = new CSVExportModularParameters().cloneParameterSet();
    parameters.setParameter(CSVExportModularParameters.filename, csvExportFile);
    parameters.setParameter(CSVExportModularParameters.filter, rowsFilter);
    parameters.setParameter(CSVExportModularParameters.omitEmptyColumns, omitEmpty);
    parameters.setParameter(CSVExportModularParameters.idSeparator, idSeparator);
    parameters.setParameter(CSVExportModularParameters.fieldSeparator, fieldSep);
    parameters.setParameter(CSVExportModularParameters.featureLists, featureListsSelection);
    return (CSVExportModularParameters) parameters;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
