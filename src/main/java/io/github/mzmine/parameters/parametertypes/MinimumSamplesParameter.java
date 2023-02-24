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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt.Mode;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeIntParameter;

public class MinimumSamplesParameter extends AbsoluteAndRelativeIntParameter {

  public static final String DEFAULT_NAME = "Minimum aligned features (samples)";
  public static final String DEFAULT_DESCRIPTION = "Minimum number of feature detections required per row. The value will be rounded down to the nearest whole number.";

  public MinimumSamplesParameter() {
    this(DEFAULT_NAME, DEFAULT_DESCRIPTION, new AbsoluteAndRelativeInt(1, 0f, Mode.ROUND_DOWN));
  }

  public MinimumSamplesParameter(final String name, final String description) {
    this(name, description, new AbsoluteAndRelativeInt(1, 0f, Mode.ROUND_DOWN));
  }

  public MinimumSamplesParameter(final String name, final String description,
      final Integer minAbsolute) {
    this(name, description, new AbsoluteAndRelativeInt(1, 0f, Mode.ROUND_DOWN), minAbsolute);
  }

  public MinimumSamplesParameter(final String name, final String description,
      final AbsoluteAndRelativeInt value) {
    super(name, description, "samples", value);
  }

  public MinimumSamplesParameter(final String name, final String description,
      final AbsoluteAndRelativeInt value, final Integer minAbsolute) {
    super(name, description, "samples", value, minAbsolute);
  }

  @Override
  public AbsoluteAndRelativeIntParameter cloneParameter() {
    return new MinimumSamplesParameter(name, description, value, minAbs);
  }
}
