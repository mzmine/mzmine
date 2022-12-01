/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_csv_legacy;

import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class LegacyCSVExportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv"), //
      new ExtensionFilter("All files", "*.*") //
  );

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(1);

  public static final FileNameParameter filename = new FileNameParameter("Filename",
      "Name of the output CSV file. "
      + "Use pattern \"{}\" in the file name to substitute with feature list name. "
      + "(i.e. \"blah{}blah.csv\" would become \"blahSourceFeatureListNameblah.csv\"). "
      + "If the file already exists, it will be overwritten.",
      extensions, FileSelectionType.SAVE);

  public static final StringParameter fieldSeparator = new StringParameter("Field separator",
      "Character(s) used to separate fields in the exported file", ",");

  public static final MultiChoiceParameter<LegacyExportRowCommonElement> exportCommonItems =
      new MultiChoiceParameter<>("Export common elements",
          "Selection of row's elements to export", LegacyExportRowCommonElement.values());

  public static final MultiChoiceParameter<LegacyExportRowDataFileElement> exportDataFileItems =
      new MultiChoiceParameter<>("Export data file elements",
          "Selection of feature's elements to export", LegacyExportRowDataFileElement.values());

  public static final BooleanParameter exportAllFeatureInfo =
      new BooleanParameter("Export quantitation results and other information",
          "If checked, all feature-information results for a feature will be exported. ", false);

  public static final StringParameter idSeparator = new StringParameter("Identification separator",
      "Character(s) used to separate identification results in the exported file", ";");

  public static final ComboParameter<FeatureListRowsFilter> filter = new ComboParameter<>(
      "Filter rows", "Limit the exported rows to those with MS/MS data (or annotated rows)",
      FeatureListRowsFilter.values(), FeatureListRowsFilter.ALL);

  public LegacyCSVExportParameters() {
    super(new Parameter[]{featureLists, filename, fieldSeparator, exportCommonItems,
        exportDataFileItems, exportAllFeatureInfo, idSeparator, filter});
  }

}
