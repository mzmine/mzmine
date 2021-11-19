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

package io.github.mzmine.modules.dataanalysis.projectionplots;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.util.FeatureMeasurementType;

public class ProjectionPlotParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final RawDataFilesParameter dataFiles =
      new RawDataFilesParameter(new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES));

  public static final ColoringTypeParameter coloringType = new ColoringTypeParameter();

  public static final ComboParameter<FeatureMeasurementType> featureMeasurementType =
      new ComboParameter<FeatureMeasurementType>("Peak measurement type", "Measure features using",
          FeatureMeasurementType.values());

  public static final Integer[] componentPossibleValues = {1, 2, 3, 4, 5};

  public static final ComboParameter<Integer> xAxisComponent = new ComboParameter<Integer>(
      "X-axis component", "Component on the X-axis", componentPossibleValues);

  public static final ComboParameter<Integer> yAxisComponent =
      new ComboParameter<Integer>("Y-axis component", "Component on the Y-axis",
          componentPossibleValues, componentPossibleValues[1]);

  public ProjectionPlotParameters() {
    super(new Parameter[] {featureLists, dataFiles, coloringType, featureMeasurementType, xAxisComponent,
        yAxisComponent});
  }

}
