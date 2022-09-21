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

package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class AllSpectralDataImportParameters extends SimpleParameterSet {

  public static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("MS data", "*.mzML", "*.mzml", "*.mzXML", "*.mzxml", "*.imzML", "*.imzml",
          "*.d", "*.raw", "*.RAW", "*.mzData", "*.mzdata", "*.aird"), //
      new ExtensionFilter("mzML MS data", "*.mzML", "*.mzml"), //
      new ExtensionFilter("mzXML MS data", "*.mzXML", "*.mzxml"), //
      new ExtensionFilter("imzML MS imaging data", "*.imzML", "*.imzml"), //
      new ExtensionFilter("Bruker tdf files", "*.d"), //
      new ExtensionFilter("Thermo RAW files", "*.raw", "*.RAW"), //
      new ExtensionFilter("Waters RAW folders", "*.raw", "*.RAW"), //
      new ExtensionFilter("mzData MS data", "*.mzData", "*.mzdata"),
      new ExtensionFilter("aird MS data", "*.aird", "*.Aird", "*.AIRD"), //
      new ExtensionFilter("zip", "*.zip", "*.gz"), //
      new ExtensionFilter("All files", "*.*") //
  );

  public static final List<ExtensionFilter> extensionsFolders = List.of( //
      new ExtensionFilter("Bruker tdf files", "*.d"), //
      new ExtensionFilter("Waters RAW folders", "*.raw"), //
      new ExtensionFilter("All files", "*.*") //
  );

  public static final FileNamesParameter fileNames = new FileNamesParameter("File names", "",
      extensions);

  public static final OptionalModuleParameter<AdvancedSpectraImportParameters> advancedImport = new OptionalModuleParameter<>(
      "Advanced import",
      "Caution: Advanced option that applies mass detection (centroiding+thresholding) directly to imported scans (see help).\nAdvantage: Lower memory consumption\nCaution: All processing steps will directly change the underlying data, with no way of retrieving raw data or initial results.",
      new AdvancedSpectraImportParameters(), true);

  public AllSpectralDataImportParameters() {
    super(new Parameter[]{fileNames, //
        advancedImport, // directly process masslists
        // allow import of spectral libraries
        SpectralLibraryImportParameters.dataBaseFiles});
  }

}
