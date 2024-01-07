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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.export;

import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class ExportCorrAnnotationParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final FeatureListsParameter featureLists = new FeatureListsParameter();
  public static final BooleanParameter exportIIN = new BooleanParameter("Export IIN edges",
      "Export all edges of Ion Identity Networks (IIN)", true);
  public static final BooleanParameter exportIINRelationship = new BooleanParameter(
      "Export IIN relationship edges", "Export relationships between Ion Identity Networks (IIN)",
      false);
  public static final MultiChoiceParameter<RowsRelationship.Type> exportTypes = new MultiChoiceParameter<>(
      "Export row relationships", "Export all relationships of different rows to files",
      Type.values(), Type.values(), 1);
  public static final BooleanParameter allInOneFile = new BooleanParameter("Combine to one file",
      "Either combine to one file or export one file per relationship type", false);
  public static final ComboParameter<FeatureListRowsFilter> filter = new ComboParameter<>(
      "Filter rows", "Limit the exported rows to those with MS/MS data or annotated rows",
      FeatureListRowsFilter.values(), FeatureListRowsFilter.MS2_OR_ION_IDENTITY);
  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv"), //
      new ExtensionFilter("All files", "*.*") //
  );
  public static final FileNameParameter filename = new FileNameParameter("Filename",
      "Base file name of all edge files (Use {} to fill in the feature list name when exporting multiple feature lists at once)",
      extensions, FileSelectionType.SAVE);

  // Constructor
  public ExportCorrAnnotationParameters() {
    super(new Parameter[]{featureLists, filename, exportTypes, allInOneFile, exportIIN,
        exportIINRelationship, filter});
  }
}