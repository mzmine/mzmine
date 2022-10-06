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

package io.github.mzmine.modules.visualization.spectra.spectra_stack;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

/**
 * Holds all parameters for the multi msms visualizer
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class SpectraStackVisualizerParameters extends SimpleParameterSet {

  public static final OptionalParameter<IntegerParameter> columns = new OptionalParameter<>(
      new IntegerParameter("Specify columns", "Number of columns, otherwise auto detect", 2),
      false);

  public static final BooleanParameter useBestForEach = new BooleanParameter("Best MS/MS",
      "Use best MS/MS spectrum for each feature, might come from different raw files", true);
  public static final BooleanParameter useBestMissingRaw = new BooleanParameter(
      "Replace missing MS/MS",
      "Replace missing MS/MS (in selected file) with the best available in all raw data files",
      true);


  public static final BooleanParameter showCrosshair = new BooleanParameter("Crosshair", "", true);
  public static final BooleanParameter showAllAxes = new BooleanParameter("All axes",
      "Show all or only bottom axis", false);
  public static final BooleanParameter showTitle = new BooleanParameter("Titles", "", false);
  public static final BooleanParameter showLegend = new BooleanParameter("Legends", "", false);


  public SpectraStackVisualizerParameters() {
    super(new Parameter[]{columns, useBestForEach, useBestMissingRaw, showCrosshair, showAllAxes,
        showTitle, showLegend});
  }

}
