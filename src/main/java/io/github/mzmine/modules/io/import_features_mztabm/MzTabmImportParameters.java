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

package io.github.mzmine.modules.io.import_features_mztabm;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import javafx.stage.FileChooser.ExtensionFilter;

import java.util.List;

public class MzTabmImportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> filters =
      List.of(new ExtensionFilter("mzTab-m files", "*.mztab"),
          new ExtensionFilter("mzTab-m files", "*.mzTab"));

  public static final FileNamesParameter file =
      new FileNamesParameter("mzTab-m files", "mzTab-m files to import", filters);

  public static final BooleanParameter importRawFiles = new BooleanParameter(
      "Import raw files too?",
      "If selected, raw data files will also be imported if they are available.\nFiles will be loaded from the msrun location defined in the mzTab-m file or from the same folder containing the mzTab-m file.\nIf raw data files cannot be found, empty files will be generated instead.\nPlease note that missing files may lead to issues with other modules that need access to scan data.",
      Boolean.TRUE
  );

  public MzTabmImportParameters() {
    super(new Parameter[]{file, importRawFiles});
  }

}
