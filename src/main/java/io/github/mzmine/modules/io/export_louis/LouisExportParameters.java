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

package io.github.mzmine.modules.io.export_louis;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class LouisExportParameters extends SimpleParameterSet {

  /*
   * Define any parameters here (see io.github.mzmine.parameters for parameter types)
   */
  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "All data points >=this level");

  public static final FileNameParameter filename = new FileNameParameter("Filename",
      "Name of the output CSV file. "
      + "Use pattern \"{}\" in the file name to substitute with feature list name. "
      + "(i.e. \"blah{}blah.csv\" would become \"blahSourceFeatureListNameblah.csv\"). "
      + "If the file already exists, it will be overwritten.",
      "csv", FileSelectionType.SAVE);

  /**
   * Create a new parameterset
   */
  public LouisExportParameters() {
    /*
     * The order of the parameters is used to construct the parameter dialog automatically
     */
    super(new Parameter[]{featureLists, noiseLevel, filename});
  }

}
