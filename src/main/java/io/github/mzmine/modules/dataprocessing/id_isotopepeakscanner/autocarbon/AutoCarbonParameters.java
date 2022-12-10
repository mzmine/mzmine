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
package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.autocarbon;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

/**
 * 
 * @author Steffen Heuckeroth s_heuc03@uni-muenster.de
 *
 */
public class AutoCarbonParameters extends SimpleParameterSet {

  public static final IntegerParameter minCarbon =
      new IntegerParameter("Min. carbon", "Minumum amount of carbon to search for.", 15, true);
  public static final IntegerParameter maxCarbon =
      new IntegerParameter("Max. carbon", "Maximum amount of carbon to search for.", 25, true);
  public static final IntegerParameter minPatternSize = new IntegerParameter("Min. pattern size",
      "Set the minimum amount of data points in an isotope pattern\n"
          + "Make sure this does not exclude every pattern.",
      2, true);

  public AutoCarbonParameters() {
    super(new Parameter[] {minCarbon, maxCarbon, minPatternSize});
  }
}
