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

package io.github.mzmine.modules.io.export_scans;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;

public class ExportScansParameters extends SimpleParameterSet {

  public static final FileNameParameter file =
      new FileNameParameter("File", "file destination", FileSelectionType.SAVE);
  public static final ComboParameter<ScanFormats> formats = new ComboParameter<>("Format",
      "Export formats. mgf: MASCOT, SIRIUS;  txt: plain text;  mzML: Open standard",
      ScanFormats.values(), ScanFormats.mgf);

  public static final BooleanParameter export_masslist = new BooleanParameter(
      "Export centroid mass list", "Exports the centroid mass list instead of raw data", true);

  public ExportScansParameters() {
    super(new Parameter[]{file, formats, export_masslist});
  }

}
