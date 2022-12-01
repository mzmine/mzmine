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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.deisotoper;


import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ColorParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import javafx.scene.paint.Color;

public class DPPIsotopeGrouperParameters extends SimpleParameterSet {

  public static final String ChooseTopIntensity = "Most intense";
  public static final String ChooseLowestMZ = "Lowest m/z";

  public static final String[] representativeIsotopeValues = {ChooseTopIntensity, ChooseLowestMZ};

  // public static final StringParameter element = new
  // StringParameter("Element", "Element symbol of the element to deisotope
  // for.");

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final BooleanParameter monotonicShape = new BooleanParameter("Monotonic shape",
      "If true, then monotonically decreasing height of isotope pattern is required",
      Boolean.FALSE);

  public static final IntegerParameter maximumCharge = new IntegerParameter("Maximum charge",
      "Maximum charge to consider for detecting the isotope patterns", 1);

  public static final ComboParameter<String> representativeIsotope = new ComboParameter<String>(
      "Representative isotope",
      "Which peak should represent the whole isotope pattern. For small molecular weight\n"
          + "compounds with monotonically decreasing isotope pattern, the most intense isotope\n"
          + "should be representative. For high molecular weight peptides, the lowest m/z\n"
          + "peptides, the lowest m/z isotope may be the representative.",
      representativeIsotopeValues, representativeIsotopeValues[0]);

  public static final BooleanParameter autoRemove = new BooleanParameter("Remove non-isotopes",
      "If checked, all peaks without an isotope pattern will not be displayed and not passed to the next module.",
      false);

  public static final BooleanParameter displayResults = new BooleanParameter("Display results",
      "Check if you want to display the deisotoping results in the plot. Displaying too much datasets might decrease clarity.",
      false);

  public static final ColorParameter datasetColor = new ColorParameter("Dataset color",
      "Set the color you want the detected isotope patterns to be displayed with.", Color.GREEN);

  public DPPIsotopeGrouperParameters() {
    super(new Parameter[] { /* element, */ mzTolerance, monotonicShape, maximumCharge,
        representativeIsotope, autoRemove, displayResults, datasetColor});
  }

}
