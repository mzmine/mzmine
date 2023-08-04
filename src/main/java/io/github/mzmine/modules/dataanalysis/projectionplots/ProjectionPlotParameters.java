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

package io.github.mzmine.modules.dataanalysis.projectionplots;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;

public class ProjectionPlotParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(
      new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES));

  public static final ColoringTypeParameter coloringType = new ColoringTypeParameter();

  public static final ComboParameter<AbundanceMeasure> featureMeasurementType = new ComboParameter<AbundanceMeasure>(
      "Peak measurement type", "Measure features using", AbundanceMeasure.values());

  public static final Integer[] componentPossibleValues = {1, 2, 3, 4, 5};

  public static final ComboParameter<Integer> xAxisComponent = new ComboParameter<Integer>(
      "X-axis component", "Component on the X-axis", componentPossibleValues);

  public static final ComboParameter<Integer> yAxisComponent = new ComboParameter<Integer>(
      "Y-axis component", "Component on the Y-axis", componentPossibleValues,
      componentPossibleValues[1]);

  public ProjectionPlotParameters() {
    super(featureLists, dataFiles, coloringType, featureMeasurementType, xAxisComponent,
        yAxisComponent);
  }

}
