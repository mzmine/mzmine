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

package io.github.mzmine.modules.io.export_features_mztabm;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class MZTabmExportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("mzTab files for feature lists", "*.mzTab") //
  );


  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final FileNameParameter filename = new FileNameParameter("Filename",
      "Use pattern \"{}\" in the file name to substitute with feature list name. "
      + "(i.e. \"blah{}blah.mzTab\" would become \"blahSourcePeakListNameblah.mzTab\"). "
      + "If the file already exists, it will be overwritten.",
      extensions, 32, FileSelectionType.SAVE);

  public static final BooleanParameter exportAll = new BooleanParameter("Include all peaks"//
      , "Include peaks with unknown identity");

  public MZTabmExportParameters() {
    super(new Parameter[]{featureLists, filename, exportAll});
  }
}
