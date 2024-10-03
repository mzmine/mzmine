/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.BafImportModule;
import io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.BafImportParameters;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportModule;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportParameters;
import io.github.mzmine.modules.io.import_rawdata_bruker_tsf.TSFImportModule;
import io.github.mzmine.modules.io.import_rawdata_bruker_tsf.TSFImportParameters;
import io.github.mzmine.modules.io.import_rawdata_icpms_csv.IcpMsCVSImportModule;
import io.github.mzmine.modules.io.import_rawdata_icpms_csv.IcpMsCVSImportParameters;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImzMLImportModule;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImzMLImportParameters;
import io.github.mzmine.modules.io.import_rawdata_msconvert.MSConvertImportModule;
import io.github.mzmine.modules.io.import_rawdata_msconvert.MSConvertImportParameters;
import io.github.mzmine.modules.io.import_rawdata_mzdata.MzDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_mzdata.MzDataImportParameters;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportModule;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportParameters;
import io.github.mzmine.modules.io.import_rawdata_mzxml.MzXMLImportModule;
import io.github.mzmine.modules.io.import_rawdata_mzxml.MzXMLImportParameters;
import io.github.mzmine.modules.io.import_rawdata_netcdf.NetCDFImportModule;
import io.github.mzmine.modules.io.import_rawdata_netcdf.NetCDFImportParameters;
import io.github.mzmine.modules.io.import_rawdata_thermo_raw.ThermoRawImportModule;
import io.github.mzmine.modules.io.import_rawdata_thermo_raw.ThermoRawImportParameters;
import io.github.mzmine.modules.io.import_rawdata_zip.ZipImportModule;
import io.github.mzmine.modules.io.import_rawdata_zip.ZipImportParameters;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.project.ProjectService;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BatchUtils {

  /**
   * @return error message if imported files >= min samples filter otherwise null on success
   */
  @Nullable
  public static String checkMinSamplesFilter(@NotNull final BatchQueue batch) {
    var numFilesImported = getTotalNumImportedFiles(batch, true);

    var minSamplesFilter = batch.findFirst(
            RowsFilterModule.class) // find rows filter and then extract optional sample filter
        .map(params -> params.getEmbeddedParameterValueIfSelectedOrElse(
            RowsFilterParameters.MIN_FEATURE_COUNT, null)).orElse(new AbsoluteAndRelativeInt(0, 0));

    var minSamples = minSamplesFilter.getMaximumValue(numFilesImported);
    if (minSamples > numFilesImported) {
      var filterName = RowsFilterParameters.MIN_FEATURE_COUNT.getName();
      var error = """
          The "%s" parameter in the feature list rows filter step requires %d samples, but mzmine \
          detected that after the import only %d samples may be available and this would lead to empty feature lists.
          Check the feature list rows filter or disregard this message if its a wrong alert.""".formatted(
          filterName, minSamples, numFilesImported);
      return error;
    }
    return null;
  }

  /**
   * @param b                   batch
   * @param includeProjectFiles include already imported files
   * @return total number of files in import steps, optionally adding existing files
   */
  public static int getTotalNumImportedFiles(final @NotNull BatchQueue b,
      final boolean includeProjectFiles) {
    // all import steps
    int total = //
        getNumFiles(b, AllSpectralDataImportModule.class, AllSpectralDataImportParameters.fileNames)
        + getNumFiles(b, ThermoRawImportModule.class, ThermoRawImportParameters.fileNames) //
        + getNumFiles(b, ImzMLImportModule.class, ImzMLImportParameters.fileNames) //
        + getNumFiles(b, MzDataImportModule.class, MzDataImportParameters.fileNames) //
        + getNumFiles(b, MSDKmzMLImportModule.class, MSDKmzMLImportParameters.fileNames) //
        + getNumFiles(b, MzXMLImportModule.class, MzXMLImportParameters.fileNames) //
        + getNumFiles(b, NetCDFImportModule.class, NetCDFImportParameters.fileNames) //
        + getNumFiles(b, ZipImportModule.class, ZipImportParameters.fileNames) //
        + getNumFiles(b, TDFImportModule.class, TDFImportParameters.fileNames) //
        + getNumFiles(b, TSFImportModule.class, TSFImportParameters.fileNames) //
        + getNumFiles(b, MSConvertImportModule.class, MSConvertImportParameters.fileNames) //
        + getNumFiles(b, IcpMsCVSImportModule.class, IcpMsCVSImportParameters.fileNames) //
        + getNumFiles(b, BafImportModule.class, BafImportParameters.files);

    // maybe already imported?
    if (includeProjectFiles) {
      var project = ProjectService.getProject();
      if (project != null) {
        total += project.getNumberOfDataFiles();
      }
    }
    return total;
  }

  /**
   * @param batch       the batch
   * @param moduleClass filters for all steps of this module
   * @param parameter   defines the parameter in the module
   * @return number of files in all steps of a specific Module class and a parameter
   */
  public static int getNumFiles(final @NotNull BatchQueue batch,
      @NotNull final Class<? extends MZmineModule> moduleClass,
      final FileNamesParameter parameter) {
    return batch.streamStepParameterSets(moduleClass).map(params -> params.getParameter(parameter))
        .filter(Objects::nonNull).mapToInt(FileNamesParameter::numFiles).sum();
  }
}
