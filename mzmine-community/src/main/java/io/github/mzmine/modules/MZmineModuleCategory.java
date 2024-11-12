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

package io.github.mzmine.modules;

public enum MZmineModuleCategory {

  PROJECTIO("Project I/O"), //
  PROJECT("Project"), //
  PROJECTMETADATA("Project metadata"), //
  RAWDATAIMPORT("Raw data import"), //
  RAWDATA("Raw data methods"), //
  RAWDATAFILTERING("Raw data filtering"), //
  RAWDATAEXPORT("Raw data export"), //
  EIC_DETECTION("Chromatogram detection"), //
  GAPFILLING("Gap filling"), //
  ISOTOPES("Isotopes"), //
  FEATURELIST("Feature list methods"), //
  /**
   * Only feature list resolving that splits separate features. Modules like
   * {@link
   * io.github.mzmine.modules.dataprocessing.filter_blanksubtraction_chromatograms.ChromatogramBlankSubtractionModule}
   * use this to check if the module can be applied on the feature list
   */
  FEATURE_RESOLVING("Resolving"), //
  FEATURE_GROUPING("Feature grouping"), //
  ION_IDENTITY_NETWORKS("Ion identity networking"), //
  SPECTRALDECONVOLUTION("Spectral deconvolution"), //
  FEATURELISTFILTERING("Feature list filtering"), //
  ALIGNMENT("Alignment"), //
  NORMALIZATION("Normalization"), //
  ANNOTATION("Annotation"), //
  FEATURELISTEXPORT("Feature list export"), //
  FEATURELISTIMPORT("Feature list import"), //
  SPECLIBIMPORT("Spectral library import"), //
  SPECLIB_PROCESSING("Spectral library processing"), //
  SPECLIBEXPORT("Spectral library export"), //
  VISUALIZATIONRAWDATA("Visualization"), //
  VISUALIZATIONFEATURELIST("Visualization feature list"), //
  VISUALIZATION_RAW_AND_FEATURE("Visualization data"), //
  VISUALIZATION_OTHER_DATA("Visualization of data from other detectors"), //
  DATAANALYSIS("Data analysis"), //
  HELPSYSTEM("Help"), //
  TOOLS("Tools"), OTHER_DATA_PROCESSING("Processing other data"); //

  private final String name;

  MZmineModuleCategory(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public MainCategory getMainCategory() {
    return switch (this) {
      case PROJECT, PROJECTIO, PROJECTMETADATA -> MainCategory.PROJECT;
      case RAWDATAIMPORT, RAWDATAEXPORT, RAWDATA, RAWDATAFILTERING -> MainCategory.SPECTRAL_DATA;
      case EIC_DETECTION, FEATURE_RESOLVING, GAPFILLING, ALIGNMENT, FEATURELIST ->
          MainCategory.FEATURE_DETECTION;
      case ISOTOPES, SPECTRALDECONVOLUTION, FEATURELISTFILTERING, NORMALIZATION, ANNOTATION,
           DATAANALYSIS, FEATURE_GROUPING, ION_IDENTITY_NETWORKS -> MainCategory.FEATURE_PROCESSING;
      case FEATURELISTEXPORT, FEATURELISTIMPORT -> MainCategory.FEATURE_IO;
      case VISUALIZATIONRAWDATA, VISUALIZATIONFEATURELIST, VISUALIZATION_RAW_AND_FEATURE ->
          MainCategory.VISUALIZATION;
      case SPECLIBEXPORT, SPECLIBIMPORT, SPECLIB_PROCESSING -> MainCategory.SPECTRAL_LIBRARY;
      // no main category
      case HELPSYSTEM, TOOLS -> MainCategory.OTHER;
      // no default so that the compiler marks missing cases
      case OTHER_DATA_PROCESSING, VISUALIZATION_OTHER_DATA -> MainCategory.OTHER_DATA;
    };
  }

  public enum MainCategory {
    PROJECT("Project"), //
    SPECTRAL_DATA("Raw data methods"), //
    FEATURE_DETECTION("Feature detection"), //
    FEATURE_PROCESSING("Feature processing"), //
    SPECTRAL_LIBRARY("Spectral library"), //
    FEATURE_IO("Feature IO"), //
    VISUALIZATION("Visualization"), //
    OTHER("Other"), //
    OTHER_DATA("UV/Other data");

    private final String name;

    MainCategory(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
