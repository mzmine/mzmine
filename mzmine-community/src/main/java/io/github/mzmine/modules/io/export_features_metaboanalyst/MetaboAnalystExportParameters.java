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

package io.github.mzmine.modules.io.export_features_metaboanalyst;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class MetaboAnalystExportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv") //
  );

  public static final FileNameSuffixExportParameter filename = new FileNameSuffixExportParameter(
      "Filename", "Use pattern \"{}\" in the file name to substitute with feature list name. "
                  + "(i.e. \"blah{}blah.csv\" would become \"blahSourceFeatureListNameblah.csv\"). "
                  + "If the file already exists, it will be overwritten.", extensions,
      "metaboanalyst");


  public static final FeatureListsParameter featureLists = new FeatureListsParameter(1);
  public static final MetadataGroupingParameter grouping = new MetadataGroupingParameter();
  public static final ComboParameter<AbundanceMeasure> FEATURE_INTENSITY = new ComboParameter(
      "Feature intensity", "Either use height or area", AbundanceMeasure.values(),
      AbundanceMeasure.Area);

  public MetaboAnalystExportParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/io/data-exchange-with-other-software.html#metaboanalyst-export",
        featureLists, filename,
//        format,
        grouping, FEATURE_INTENSITY);
  }

//  public static final ComboParameter<StatsFormat> format = new ComboParameter<>("Export format",
//      "Different formats are supported by MetaboAnalyst and other software tools.",
//      StatsFormat.values(), StatsFormat.ONE_FACTOR);


  public enum StatsFormat {
    ONE_FACTOR, METADATA_TABLE, ALL_FACTORS;

    @Override
    public String toString() {
      return switch (this) {
        case ONE_FACTOR -> "One factor";
        case METADATA_TABLE -> "Metadata table";
        case ALL_FACTORS -> "All factors (not MetaboAnalyst)";
      };
    }
  }
}
