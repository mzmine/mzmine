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
