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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class DetectIsotopesParameter extends SimpleParameterSet {

  public static final ElementsParameter elements = new ElementsParameter("Chemical elements",
      "Chemical elements which isotopes will be considered");

  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter(0.0005, 10);

  public static final IntegerParameter maxCharge = new IntegerParameter("Maximum charge of isotope m/z",
      "Maximum possible charge of isotope distribution m/z's. All present m/z values obtained by dividing "
      + "isotope masses with 1, 2, ..., maxCharge values will be considered. The default value is 1, "
      + "but insert an integer greater than 1 if you want to consider ions of higher charge states.",
      1, true, 1, null);

  public DetectIsotopesParameter() {
    super(new UserParameter[] {elements, isotopeMzTolerance, maxCharge});
  }

}
