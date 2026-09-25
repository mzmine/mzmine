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

package io.github.mzmine.modules.visualization.surface3d.plot;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;

/**
 * Round tick values (1, 2, 5 x 10^n) so that axis labels are easy to read.
 */
final class Surface3DTicks {

  private static final ThreadLocal<DecimalFormat> SCIENTIFIC = ThreadLocal.withInitial(
      () -> new DecimalFormat("0.0E0", DecimalFormatSymbols.getInstance(Locale.ROOT)));

  private Surface3DTicks() {
  }

  static double step(final double min, final double max, final int target) {
    final double raw = (max - min) / Math.max(1, target);
    if (!(raw > 0) || !Double.isFinite(raw)) {
      return 1;
    }
    final double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
    final double residual = raw / magnitude;
    final double nice = residual < 1.5 ? 1 : residual < 3 ? 2 : residual < 7 ? 5 : 10;
    return nice * magnitude;
  }

  static double @NotNull [] ticks(final double min, final double max, final int target) {
    if (!(max > min)) {
      return new double[]{min};
    }
    final double step = step(min, max, target);
    final List<Double> values = new ArrayList<>();
    for (double value = Math.ceil(min / step - 1e-9) * step; value <= max + step * 1e-9;
        value += step) {
      // avoid -0 and accumulated floating point noise
      values.add(Math.abs(value) < step * 1e-9 ? 0 : value);
    }
    return values.stream().mapToDouble(Double::doubleValue).toArray();
  }

  /**
   * @return 0 and the powers of ten up to the maximum, or linear ticks if fewer than two decades
   */
  static double @NotNull [] logTicks(final double maximum, final int target) {
    if (!(maximum >= 100)) {
      return ticks(0, maximum, target);
    }
    final List<Double> values = new ArrayList<>(List.of(0d));
    final int top = (int) Math.floor(Math.log10(maximum));
    final int stride = Math.max(1, (int) Math.ceil(top / (double) target));
    for (int exponent = top % stride; exponent <= top; exponent += stride) {
      values.add(Math.pow(10, exponent));
    }
    return values.stream().mapToDouble(Double::doubleValue).toArray();
  }

  static @NotNull String format(final double value, final double step) {
    final double abs = Math.abs(value);
    if (abs >= 100000 || (step > 0 && step < 1e-4)) {
      return intensity(value);
    }
    final int decimals = step > 0 ? Math.max(0, (int) Math.ceil(-Math.log10(step) - 1e-9)) : 4;
    return String.format(Locale.ROOT, "%." + Math.min(6, decimals) + "f", value);
  }

  static @NotNull String intensity(final double value) {
    if (value == 0) {
      return "0";
    }
    final double abs = Math.abs(value);
    if (abs >= 1000 || abs < 0.01) {
      return SCIENTIFIC.get().format(value);
    }
    return String.format(Locale.ROOT, "%.3g", value);
  }
}
