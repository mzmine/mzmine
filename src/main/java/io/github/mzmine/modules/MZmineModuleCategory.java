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
  VISUALIZATIONRAWDATA("Visualization"), //
  VISUALIZATIONFEATURELIST("Visualization feature list"), //
  DATAANALYSIS("Data analysis"), //
  HELPSYSTEM("Help"), //
  TOOLS("Tools"); //

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
      case EIC_DETECTION, FEATURE_RESOLVING, GAPFILLING, ALIGNMENT, FEATURELIST -> MainCategory.FEATURE_DETECTION;
      case ISOTOPES, SPECTRALDECONVOLUTION, FEATURELISTFILTERING -> MainCategory.FEATURE_FILTERING;
      case NORMALIZATION, ANNOTATION, DATAANALYSIS, FEATURE_GROUPING, ION_IDENTITY_NETWORKS -> MainCategory.FEATURE_PROCESSING;
      case FEATURELISTEXPORT, FEATURELISTIMPORT -> MainCategory.FEATURE_IO;
      case VISUALIZATIONRAWDATA, VISUALIZATIONFEATURELIST -> MainCategory.VISUALIZATION;
      // no main category
      case HELPSYSTEM, TOOLS -> MainCategory.OTHER;
      // no default so that the compiler marks missing cases
    };
  }

  public enum MainCategory {
    PROJECT("Project"), //
    SPECTRAL_DATA("Spectral data"), //
    FEATURE_DETECTION("Feature detection"), //
    FEATURE_FILTERING("Feature filtering"), //
    FEATURE_PROCESSING("Feature processing"), //
    FEATURE_IO("Feature IO"), //
    VISUALIZATION("Visualization"), OTHER("Other");

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
