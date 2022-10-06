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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.anyelementdeisotoper;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ColorParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import javafx.scene.paint.Color;

/**
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPAnyElementIsotopeGrouperParameters extends SimpleParameterSet {

  public static final DoubleRangeParameter mzRange = new DoubleRangeParameter("m/z range",
      "The range of m/z to scan for isotope peaks", MZmineCore.getConfiguration().getMZFormat());

  public static final StringParameter element =
      new StringParameter("Element", "Element symbol of the element to deisotope for.");

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final IntegerParameter maximumCharge = new IntegerParameter("Maximum charge",
      "Maximum charge to consider for detecting the isotope patterns");

  public static final BooleanParameter autoRemove = new BooleanParameter("Remove non-isotopes",
      "If checked, all peaks without an isotope pattern will not be displayed and not passed to the next module.");

  public static final BooleanParameter displayResults = new BooleanParameter("Display results",
      "Check if you want to display the deisotoping results in the plot. Displaying too much datasets might decrease clarity.",
      false);

  public static final ColorParameter datasetColor = new ColorParameter("Dataset color",
      "Set the color you want the detected isotope patterns to be displayed with.", Color.GREEN);

  public DPPAnyElementIsotopeGrouperParameters() {
    super(new Parameter[] {element, mzTolerance, mzRange, maximumCharge, autoRemove, displayResults,
        datasetColor});
  }

}
