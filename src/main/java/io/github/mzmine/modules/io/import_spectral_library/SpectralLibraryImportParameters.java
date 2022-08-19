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

package io.github.mzmine.modules.io.import_spectral_library;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class SpectralLibraryImportParameters extends SimpleParameterSet {

  public static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("All library files", "*.json", "*.mgf", "*.msp", "*.jdx"), //
      new ExtensionFilter("json files from MoNA or GNPS", "*.json"), //
      new ExtensionFilter("mgf files", "*.mgf"), //
      new ExtensionFilter("msp files from NIST", "*.msp"), //
      new ExtensionFilter("JCAM-DX files", "*.jdx"), //
      new ExtensionFilter("All files", "*.*") //
  );

  public static final FileNamesParameter dataBaseFiles = new FileNamesParameter(
      "Spectral library files",
      "Name of file that contains information for peak identification\n(GNPS json, MONA json, NIST msp, mgf, JCAMP-DX jdx)",
      extensions);

  public SpectralLibraryImportParameters() {
    super(new Parameter[]{dataBaseFiles});
  }

}
