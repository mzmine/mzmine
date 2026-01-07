/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.gui.preferences;

import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import java.text.DecimalFormat;

public class WatersLockmassParameters extends SimpleParameterSet {

  private static final double DEFAULT_POS_LOCKMASS = 556.276575;
  public static final DoubleParameter positive = new DoubleParameter("Positive lockmass",
      "Lockmass for positive ion mode for native Waters files. The default is 556.276575 (Leu-Enk).",
      new DecimalFormat("0.######"), DEFAULT_POS_LOCKMASS);

  private static final double DEFAULT_NEG_LOCKMASS = 554.262022;
  public static final DoubleParameter negative = new DoubleParameter("Negative lockmass",
      "Lockmass for negative ion mode for native Waters files. The default is 554.262022 (Leu-Enk).",
      new DecimalFormat("0.######"), DEFAULT_NEG_LOCKMASS);

  public WatersLockmassParameters() {
    super(positive, negative);
  }

  public static WatersLockmassParameters createDefault() {
    return create(DEFAULT_POS_LOCKMASS, DEFAULT_NEG_LOCKMASS);
  }

  public static WatersLockmassParameters create(double positiveLockmass, double negativeLockmass) {
    ParameterSet parameterSet = new WatersLockmassParameters().cloneParameterSet();
    parameterSet.setParameter(positive, positiveLockmass);
    parameterSet.setParameter(negative, negativeLockmass);

    return (WatersLockmassParameters) parameterSet;
  }
}
