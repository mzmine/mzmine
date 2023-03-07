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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class RtLimitsFilterParameter extends
    ComboWithInputParameter<FeatureLimitOptions, RtLimitsFilter, RTToleranceParameter> {

  public RtLimitsFilterParameter() {
    this(new RtLimitsFilter(FeatureLimitOptions.USE_FEATURE_EDGES, null));
  }

  public RtLimitsFilterParameter(@NotNull final RtLimitsFilter defaultValue) {
    this(new RTToleranceParameter("Retention time filter", """
            This parameter either limits the grouping to the RT limits of each feature OR
            sets a tolerance around the feature RT.""", defaultValue.rtTolerance()),
        // options
        FeatureLimitOptions.values(), defaultValue);
  }

  private RtLimitsFilterParameter(final RTToleranceParameter embedded,
      FeatureLimitOptions[] options, RtLimitsFilter defaultValue) {
    super(embedded, options, FeatureLimitOptions.USE_TOLERANCE, defaultValue);
  }

  @Override
  public RtLimitsFilter createValue(final FeatureLimitOptions option,
      final RTToleranceParameter embeddedParameter) {
    return new RtLimitsFilter(option, embeddedParameter.getValue());
  }


  @Override
  public RtLimitsFilterParameter cloneParameter() {
    var embeddedParameterClone = embeddedParameter.cloneParameter();
    return new RtLimitsFilterParameter(embeddedParameterClone,
        choices.toArray(FeatureLimitOptions[]::new), value);
  }

}
