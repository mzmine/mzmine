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

package io.github.mzmine.modules.dataprocessing.id_lipididentification;

import java.text.DecimalFormat;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class LipidSearchMSMSParameters extends SimpleParameterSet {

  public static final MZToleranceParameter mzToleranceMS2 =
      new MZToleranceParameter("m/z tolerance MS2 level:",
          "Enter m/z tolerance for exact mass database matching on MS2 level");

  public static final DoubleParameter minimumMsMsScore = new DoubleParameter("Minimum MS/MS score:",
      "Explained intensity [%] of all signals in MS/MS spectrum", new DecimalFormat("#.0"), 60.0,
      0.0, 100.0);

  public static final BooleanParameter keepUnconfirmedAnnotations = new BooleanParameter(
      "Keep unconfirmed annotations",
      "WARNING!: If checked, annotations based on accurate mass without headgroup fragment annotations are kept.");

  public LipidSearchMSMSParameters() {
    super(new Parameter[] {mzToleranceMS2, minimumMsMsScore, keepUnconfirmedAnnotations});
  }

}
