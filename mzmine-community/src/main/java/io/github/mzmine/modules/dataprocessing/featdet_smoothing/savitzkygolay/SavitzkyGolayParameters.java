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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import java.util.Map;

public class SavitzkyGolayParameters extends SimpleParameterSet {

  public static final String rtSmoothingName = "Retention time width (scans)";
  public static final String mobilitySmoothingName = "Mobility width (scans)";

  public static final OptionalParameter<ComboParameter<Integer>> rtSmoothing = new OptionalParameter<>(
      new ComboParameter<Integer>(rtSmoothingName, "Enables intensity smoothing along the rt axis.",
          new Integer[]{0, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 5), false);

  public static final OptionalParameter<ComboParameter<Integer>> mobilitySmoothing = new OptionalParameter<>(
      new ComboParameter<Integer>(mobilitySmoothingName,
          "Enables intensity smoothing of the summed mobilogram.",
          new Integer[]{0, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 5), false);

  public SavitzkyGolayParameters() {
    super(new Parameter[]{rtSmoothing, mobilitySmoothing});
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    final Map<String, Parameter<?>> map = super.getNameParameterMap();
    map.put("Retention time smoothing", getParameter(rtSmoothing));
    map.put("Mobility smoothing", getParameter(mobilitySmoothing));
    return map;
  }
}
