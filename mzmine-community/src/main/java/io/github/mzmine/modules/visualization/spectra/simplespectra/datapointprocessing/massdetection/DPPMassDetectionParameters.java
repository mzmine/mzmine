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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.massdetection;


import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ColorParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import javafx.scene.paint.Color;

public class DPPMassDetectionParameters extends SimpleParameterSet {


  public static final ModuleOptionsEnumComboParameter<MassDetectors> massDetector = new ModuleOptionsEnumComboParameter<>(
      "Mass detector", "Algorithm to use for mass detection and its parameters",
      MassDetectors.FACTOR_OF_LOWEST);

  public static final BooleanParameter displayResults = new BooleanParameter("Display results",
      "Check if you want to display the mass detection results in the plot. Displaying too much datasets might decrease clarity.",
      false);

  public static final ColorParameter datasetColor = new ColorParameter("Dataset color",
      "Set the color you want the detected isotope patterns to be displayed with.", Color.CYAN);

  public DPPMassDetectionParameters() {
    super(massDetector, displayResults, datasetColor);
  }

}
