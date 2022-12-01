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

package io.github.mzmine.modules.dataprocessing.align_ransac;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.ExitCode;

public class RansacAlignerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final StringParameter peakListName =
      new StringParameter("Feature list name", "Feature list name", "Aligned feature list");

  public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();

  public static final RTToleranceParameter RTToleranceBefore = new RTToleranceParameter(
      "RT tolerance",
      "This value sets the range, in terms of retention time, to create the model using RANSAC"
          + "\nand non-linear regression algorithm. Maximum allowed retention time difference.");

  public static final RTToleranceParameter RTToleranceAfter =
      new RTToleranceParameter("RT tolerance after correction",
          "This value sets the range, in terms of retention time, to verify for possible peak"
              + "\nrows to be aligned. Maximum allowed retention time difference.");

  public static final IntegerParameter Iterations = new IntegerParameter("RANSAC iterations",
      "Maximum number of iterations allowed in the algorithm to find the right model consistent in all the"
          + "\npairs of aligned peaks. When its value is 0, the number of iterations (k) will be estimate automatically.");

  public static final PercentParameter NMinPoints = new PercentParameter("Minimum number of points",
      "% of points required to consider the model valid (d).");

  public static final DoubleParameter Margin = new DoubleParameter("Threshold value",
      "Threshold value (minutes) for determining when a data point fits a model (t)");

  public static final BooleanParameter Linear =
      new BooleanParameter("Linear model", "Switch between polynomial model or lineal model");

  public static final BooleanParameter SameChargeRequired = new BooleanParameter(
      "Require same charge state", "If checked, only rows having same charge state can be aligned");

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    RansacAlignerSetupDialog dialog = new RansacAlignerSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  public RansacAlignerParameters() {
    super(new Parameter[] {peakLists, peakListName, MZTolerance, RTToleranceBefore,
        RTToleranceAfter, Iterations, NMinPoints, Margin, Linear, SameChargeRequired},
        "https://mzmine.github.io/mzmine_documentation/module_docs/align_ransac/align_ransac.html");
  }
}
