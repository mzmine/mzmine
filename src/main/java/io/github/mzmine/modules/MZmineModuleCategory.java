/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules;

public enum MZmineModuleCategory {

  PROJECTIO("Project I/O"), //
  PROJECT("Project"), //
  RAWDATA("Raw data methods"), //
  RAWDATAFILTERING("Raw data filtering"), //
  FEATUREDETECTION("Feature detection"), //
  GAPFILLING("Gap filling"), //
  ISOTOPES("Isotopes"), //
  FEATURELIST("Feature list methods"), //
  FEATURELISTDETECTION("Feature list processing"), //
  SPECTRALDECONVOLUTION("Spectral deconvolution"), //
  FEATURELISTFILTERING("Feature list filtering"), //
  ALIGNMENT("Alignment"), //
  NORMALIZATION("Normalization"), //
  IDENTIFICATION("Identification"), //
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

  public String toString() {
    return name;
  }

  public String getMainCategory() {
    return switch(this) {
      case PROJECT, PROJECTIO -> "Project";
      case RAWDATA, RAWDATAFILTERING -> "Spectral data";
      case FEATURELIST, FEATURELISTDETECTION, FEATUREDETECTION, GAPFILLING, ALIGNMENT -> "Feature detection";
      case ISOTOPES, SPECTRALDECONVOLUTION, FEATURELISTFILTERING -> "Feature filtering";
      case NORMALIZATION, DATAANALYSIS -> "Data analysis";
      case IDENTIFICATION -> "Feature identification";
      case FEATURELISTEXPORT, FEATURELISTIMPORT -> "Feature IO";
      case VISUALIZATIONRAWDATA, VISUALIZATIONFEATURELIST -> "Visualization";
      // no main category
      case HELPSYSTEM, TOOLS -> this.name;
    };
  }

}
