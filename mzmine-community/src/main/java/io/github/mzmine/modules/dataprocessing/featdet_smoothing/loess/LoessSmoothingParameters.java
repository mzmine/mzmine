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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing.loess;

import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolayParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

public class LoessSmoothingParameters extends SimpleParameterSet {

  public static final OptionalParameter<IntegerParameter> rtSmoothing = new OptionalParameter<>(
      new IntegerParameter(SavitzkyGolayParameters.rtSmoothingName,
          "Enables intensity smoothing along the rt axis.", 5, 0, Integer.MAX_VALUE));

  public static final OptionalParameter<IntegerParameter> mobilitySmoothing = new OptionalParameter<IntegerParameter>(
      new IntegerParameter(SavitzkyGolayParameters.mobilitySmoothingName,
          "Enables intensity smoothing along the mobility axis.", 5, 0, Integer.MAX_VALUE));

  public LoessSmoothingParameters() {
    super("https://mzmine.github.io/mzmine_documentation/module_docs/featdet_smoothing/smoothing.html#loess-smoothing", rtSmoothing, mobilitySmoothing);
  }
}
