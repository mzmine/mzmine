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

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DAxisKind;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Formats plot values with the mzmine number formats, or with round-step formatting when the plot
 * is used without mzmine preferences.
 */
final class Surface3DFormat {

  static final Surface3DFormat PLAIN = new Surface3DFormat(null);

  private final @Nullable NumberFormats formats;

  Surface3DFormat(@Nullable final NumberFormats formats) {
    this.formats = formats;
  }

  /**
   * @param step tick spacing, used for axes without an mzmine number format
   */
  @NotNull String value(final double value, @NotNull final Surface3DAxisKind kind,
      final double step) {
    if (formats == null) {
      return Surface3DTicks.format(value, step);
    }
    final NumberFormat base = switch (kind) {
      case RETENTION_TIME -> formats.rtFormat();
      case MZ -> formats.mzFormat();
      case MOBILITY -> formats.mobilityFormat();
      case OTHER -> null;
    };
    if (base == null) {
      return Surface3DTicks.format(value, step);
    }
    // decision: the preference format with only the decimals the tick step needs, 500 instead
    // of 500.0000, keeps labels short enough for small tiles
    final NumberFormat tick = (NumberFormat) base.clone();
    final int decimals = Math.min(base.getMaximumFractionDigits(), decimals(step));
    tick.setMinimumFractionDigits(decimals);
    tick.setMaximumFractionDigits(decimals);
    return tick.format(value);
  }

  /**
   * @return decimals needed to show values of the round step, 0 for steps of 1 or more
   */
  static int decimals(final double step) {
    if (!(step > 0) || !Double.isFinite(step)) {
      return 2;
    }
    return Math.max(0, (int) Math.ceil(-Math.log10(step) - 1e-9));
  }

  /**
   * @return the value with the full preference format, e.g. for readouts
   */
  @NotNull String value(final double value, @NotNull final Surface3DAxisKind kind) {
    if (formats == null) {
      return Surface3DTicks.format(value, Math.abs(value) >= 100 ? 0.01 : 0.0001);
    }
    return switch (kind) {
      case RETENTION_TIME -> formats.rt(value);
      case MZ -> formats.mz(value);
      case MOBILITY -> formats.mobility(value);
      case OTHER -> Surface3DTicks.format(value, Math.abs(value) >= 100 ? 0.01 : 0.0001);
    };
  }

  @NotNull String intensity(final double value) {
    return formats == null ? Surface3DTicks.intensity(value) : formats.intensity(value);
  }
}
