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

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import java.text.DecimalFormat;
import com.google.common.collect.Range;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;

public class KendrickMassDefectFilterParameters extends SimpleParameterSet {

  public static final DoubleRangeParameter kendrickMassDefectRange = new DoubleRangeParameter(
      "Kendrick mass defect", "Permissible range of Kendrick mass defect per row",
      MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 1.0));

  public static final StringParameter kendrickMassBase = new StringParameter("Kendrick mass base",
      "Enter a sum formula for a Kendrick mass base, e.g. \"CH2\"");

  public static final DoubleParameter shift = new DoubleParameter("Shift",
      "Enter a shift for shift dependent KMD filtering", new DecimalFormat("0.##"), 0.00);

  public static final IntegerParameter charge =
      new IntegerParameter("Charge", "Enter a charge for charge dependent KMD filtering", 1);

  public static final IntegerParameter divisor = new IntegerParameter("Divisor",
      "Enter a divisor for fractional base unit dependent KMD filtering", 1);

  public static final BooleanParameter useRemainderOfKendrickMass =
      new BooleanParameter("Use Remainder of Kendrick mass",
          "Use Remainder of Kendrick mass (RKM) instead of Kendrick mass defect (KMD)", false);

  public KendrickMassDefectFilterParameters() {
    super(new Parameter[] {kendrickMassDefectRange, kendrickMassBase, shift, charge, divisor,
        useRemainderOfKendrickMass});
  }

}
