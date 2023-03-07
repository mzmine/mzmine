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

package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.stage.FileChooser.ExtensionFilter;

public class AllSpectralDataImportParameters extends SimpleParameterSet {

  public static final ExtensionFilter ALL_MS_DATA_FILTER = new ExtensionFilter("MS data", "*.mzML",
      "*.mzml", "*.mzXML", "*.mzxml", "*.imzML", "*.imzml", "*.d", "*.raw", "*.RAW", "*.mzData",
      "*.netcdf", "*.mzdata", "*.aird");

  public static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("mzML MS data", "*.mzML", "*.mzml"), //
      new ExtensionFilter("mzXML MS data", "*.mzXML", "*.mzxml"), //
      new ExtensionFilter("imzML MS imaging data", "*.imzML", "*.imzml"), //
      new ExtensionFilter("Bruker tdf files", "*.d"), //
      new ExtensionFilter("Thermo RAW files", "*.raw", "*.RAW"), //
      new ExtensionFilter("Waters RAW folders", "*.raw", "*.RAW"), //
      new ExtensionFilter("mzData MS data", "*.mzData", "*.mzdata"),
      new ExtensionFilter("aird MS data", "*.aird", "*.Aird", "*.AIRD"), //
      new ExtensionFilter("netCDF", "*.cdf", "*.CDF", "*.netcdf", "*.NETCDF", "*.nc", "*.NC"), //
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
    super(fileNames, //
        advancedImport, // directly process masslists
        // allow import of spectral libraries
        SpectralLibraryImportParameters.dataBaseFiles);
  }

  /**
   * @return true if parameterset is of this class or contains at least the parameters
   */
  public static boolean isParameterSetClass(final ParameterSet parameters) {
    return parameters != null && (
        parameters.getClass().equals(AllSpectralDataImportParameters.class) || (
            parameters.hasParameter(advancedImport) && parameters.hasParameter(fileNames)));
  }

  /**
   * Get all files in the project that match the file path
   * @param project
   * @param parameters
   * @return
   */
  public static List<RawDataFile> getLoadedRawDataFiles(MZmineProject project,
      final ParameterSet parameters) {
    // all files that should be loaded
    Set<File> loadFileSet = Arrays.stream(parameters.getValue(fileNames)).map(File::getAbsoluteFile).collect(
        Collectors.toSet());

    // the actual files in the list
    return project.getCurrentRawDataFiles().stream()
        .filter(raw -> loadFileSet.contains(raw.getAbsoluteFilePath())).toList();
  }
}
