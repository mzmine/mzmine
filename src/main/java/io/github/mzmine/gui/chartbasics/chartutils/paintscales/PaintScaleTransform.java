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

package io.github.mzmine.gui.chartbasics.chartutils.paintscales;

import java.util.function.DoubleFunction;

public enum PaintScaleTransform {
  LINEAR(d -> d, d -> d),
  LOG10(Math::log10, d -> Math.pow(10, d)),
  LOG2(val -> Math.log(val) / Math.log(2), d -> Math.pow(2, d)),
  SQRT(Math::sqrt, d -> Math.pow(d, 2));

  private final DoubleFunction<Double> transform;
  private final DoubleFunction<Double> revertTransform;

  PaintScaleTransform(DoubleFunction<Double> transform, DoubleFunction<Double> revertTransform) {
    this.transform = transform;
    this.revertTransform = revertTransform;
  }

  public double transform(double value) {
    return transform.apply(value);
  }

  public double revertTransform(double transformedValue) {
    return revertTransform.apply(transformedValue);
  }
}
