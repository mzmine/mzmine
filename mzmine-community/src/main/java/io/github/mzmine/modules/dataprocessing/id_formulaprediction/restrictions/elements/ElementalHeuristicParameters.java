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

package io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;

public class ElementalHeuristicParameters extends SimpleParameterSet {

  public static final BooleanParameter checkHC =
      new BooleanParameter("H/C ratio", "0.1 <= H/C ratio <= 6", true);

  public static final BooleanParameter checkNOPS = new BooleanParameter("NOPS/C ratios",
      "N/C ratio <= 4, O/C ratio <= 3, P/C ratio <= 2, S/C ratio <= 3", true);

  public static final BooleanParameter checkMultiple = new BooleanParameter(
      "Multiple element counts",
      "Check for multiple element count restrictions. See help for detailed description of this rule",
      true);

  public ElementalHeuristicParameters() {
    super(new Parameter[] {checkHC, checkNOPS, checkMultiple});
  }

}
