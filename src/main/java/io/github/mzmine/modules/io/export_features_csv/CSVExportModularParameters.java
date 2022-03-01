/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.export_features_csv;

import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

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
  public static final FileNameParameter filename = new FileNameParameter("Filename",
      "Name of the output CSV file. "
          + "Use pattern \"{}\" in the file name to substitute with feature list name. "
          + "(i.e. \"blah{}blah.csv\" would become \"blahSourceFeatureListNameblah.csv\"). "
          + "If the file already exists, it will be overwritten.", extensions,
      FileSelectionType.SAVE);


  public CSVExportModularParameters() {
    super(new Parameter[]{featureLists, filename, fieldSeparator, idSeparator, omitEmptyColumns,
        filter});
  }

}
