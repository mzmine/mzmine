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

package io.github.mzmine.util.files;

import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class ExtensionFilters {

  /**
   * mzmine specific
   */
  public static final ExtensionFilter MZ_USER = new ExtensionFilter("mzmine user", "*.mzuser");
  public static final ExtensionFilter MZ_CONFIG = new ExtensionFilter("mzmine config",
      "*.mzconfig");
  public static final ExtensionFilter MZ_BATCH = new ExtensionFilter("mzmine batch", "*.mzbatch");
  public static final ExtensionFilter MZ_WIZARD = new ExtensionFilter("mzmine mzwizard",
      "*.mzmwizard");
  public static final ExtensionFilter MZ_LOG = new ExtensionFilter("mzmine log file", "*.log");

  /**
   * PyTorch DJL specific
   */
  public static final ExtensionFilter PT = new ExtensionFilter("PyTorch script", "*.pt");

  /*
   * General
   */
  public static final ExtensionFilter ZIP = new ExtensionFilter("zip compressed", "*.zip");
  public static final ExtensionFilter EXE = new ExtensionFilter("Microsoft Windows executable",
      "*.exe");

  /*
   * CSV and TSV import export
   */
  public static final ExtensionFilter CSV = new ExtensionFilter("comma-separated data", "*.csv");
  public static final ExtensionFilter TSV = new ExtensionFilter("tab-separated data", "*.tsv");
  public static final ExtensionFilter TXT = new ExtensionFilter("text file", "*.txt");
  public static final ExtensionFilter ALL_FILES = new ExtensionFilter("All files", "*.*");
  public static final ExtensionFilter CSV_OR_TSV = new ExtensionFilter("CSV or TSV data", "*.csv",
      "*.tsv");

  // LISTS
  public static final List<ExtensionFilter> CSV_TSV_IMPORT = List.of(CSV_OR_TSV, CSV, TSV,
      ALL_FILES);
  public static final List<ExtensionFilter> CSV_TSV_EXPORT = List.of(CSV, TSV);

  // general json format
  public static final ExtensionFilter JSON = new ExtensionFilter("json files", "*.json");
  /*
   * Spectral library formats
   */
  public static final ExtensionFilter JSON_LIBRARY = new ExtensionFilter(
      "json libraries from MoNA, GNPS, MZmine", "*.json");
  public static final ExtensionFilter MSP = new ExtensionFilter("msp mass spectra format (NIST)",
      "*.msp");
  public static final ExtensionFilter MGF = new ExtensionFilter("mgf mass spectra format", "*.mgf");
  public static final ExtensionFilter JDCAMX = new ExtensionFilter("JCAM-DX files", "*.jdx");

  /**
   * MASS SPEC formats
   */
  public static final ExtensionFilter MZML = new ExtensionFilter("mzML MS data", "*.mzML",
      "*.mzml");
  public static final ExtensionFilter MZXML = new ExtensionFilter("mzXML MS data", "*.mzXML",
      "*.mzxml");
  public static final ExtensionFilter IMZML = new ExtensionFilter("imzML MS imaging data",
      "*.imzML", "*.imzml");
  public static final ExtensionFilter BRUKER_D = new ExtensionFilter("Bruker .d files", "*.d",
      ".tsf", "*.tdf");
  public static final ExtensionFilter AGILENT_D = new ExtensionFilter("Agilent .d files", "*.d");
  public static final ExtensionFilter THERMO_RAW = new ExtensionFilter("Thermo RAW files", "*.raw",
      "*.RAW");
  public static final ExtensionFilter WATERS_RAW = new ExtensionFilter("Waters RAW folders",
      "*.raw", "*.RAW");
  public static final ExtensionFilter MZDATA = new ExtensionFilter("mzData MS data", "*.mzData",
      "*.mzdata");
  //  public static final ExtensionFilter AIRD = new ExtensionFilter("aird MS data", "*.aird",
//      "*.Aird", "*.AIRD");
  public static final ExtensionFilter NETCDF = new ExtensionFilter("netCDF", "*.cdf", "*.CDF",
      "*.netcdf", "*.NETCDF", "*.nc", "*.NC");
  public static final ExtensionFilter MZML_ZIP_GZIP = new ExtensionFilter("zip", "*.zip", "*.gz");
  public static final ExtensionFilter WIFF = new ExtensionFilter("wiff", "*.wiff");
  public static final ExtensionFilter WIFF2 = new ExtensionFilter("wiff2", "*.wiff2");
  public static final ExtensionFilter ALL_MS_DATA_FILTER = new ExtensionFilter("MS data", "*.mzML",
      "*.mzml", "*.mzXML", "*.mzxml", "*.imzML", "*.imzml", "*.d", "*.tdf", "*.tsf", "*.raw",
      "*.RAW", "*.mzData", "*.netcdf", "*.mzdata", /*"*.aird",*/ "*.wiff", "*.wiff2");
  public static final List<ExtensionFilter> MS_RAW_DATA = List.of( //
      ALL_MS_DATA_FILTER, //
      MZML, //
      MZXML, //
      IMZML, //
      BRUKER_D, //
      THERMO_RAW, //
      WATERS_RAW, //
      MZDATA, //
//      AIRD, //
      NETCDF, //
      MZML_ZIP_GZIP, //
      WIFF, //
      WIFF2, //
      ALL_FILES);
  private static final ExtensionFilter ALL_SPECTRAL_LIBRARY_FILTER = new ExtensionFilter(
      "All spectral libraries", "*.json", "*.msp", "*.mgf", "*.jdx");
  // LISTS
  public static final List<ExtensionFilter> ALL_LIBRARY = List.of(ALL_SPECTRAL_LIBRARY_FILTER,
      JSON_LIBRARY, MGF, MSP, JDCAMX, ALL_FILES);

  public static final ExtensionFilter MSCONVERT = new ExtensionFilter("MSConvert", "msconvert.exe");


  public static String getExtensionName(ExtensionFilter filter) {
    return filter.getExtensions().stream().findFirst().map(ext -> {
      var split = ext.split("\\.");
      return split[split.length - 1];
    }).orElse("");
  }
}
