/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.util.Map;

public class RtClusterFilterParameters extends SimpleParameterSet {

  public static final OptionalParameter<RTToleranceParameter> rtTolerance = new OptionalParameter<>(
      new RTToleranceParameter("RT tolerance filter",
          "If selected, transformation products will only be matched within the given RT tolerance window (+-).",
          new RTTolerance(0.15f, Unit.MINUTES)));

  public static final BooleanParameter rowCorrelationFilter = new BooleanParameter(
      "Filter by row correlation", """
      If selected, transformation products will only be matched to correlated features.
      The feature list must be grouped by the metaCorr module.""", false);

  public static final BooleanParameter reRankAnnotions = new BooleanParameter("Re-rank annotations",
      """
          If a row has previously been annotated by other annotation pipelines, the annotations
          will be sorted by the highest score, if enabled. If the original annotations shall be maintained
          in their current position, disable this parameter.""", true);

  public RtClusterFilterParameters() {
    super(rtTolerance, rowCorrelationFilter, reRankAnnotions);
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var map = super.getNameParameterMap();
    map.put("Filter by Row group", getParameter(rowCorrelationFilter));
    return map;
  }
}
