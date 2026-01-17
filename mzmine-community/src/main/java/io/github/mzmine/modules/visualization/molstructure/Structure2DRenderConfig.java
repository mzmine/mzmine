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

package io.github.mzmine.modules.visualization.molstructure;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @param mode rendering mode
 * @param zoom the zoom factor (only applied when mode uses zoom)
 * @param bondLength the bond length in pixel
 */
public record Structure2DRenderConfig(Sizing mode, double zoom, double bondLength) {

  public static final double DEFAULT_BOND_LENGTH = 26.1;
  public static final double DEFAUlT_ZOOM = 0.6;

  public static final Structure2DRenderConfig DEFAULT_CONFIG = new Structure2DRenderConfig(
      Sizing.getDefault(), DEFAUlT_ZOOM, DEFAULT_BOND_LENGTH);

  public Structure2DRenderConfig(double zoom) {
    this(zoom, DEFAULT_BOND_LENGTH);
  }
  public Structure2DRenderConfig(double zoom, double bondLength) {
    this(Sizing.getDefault(), zoom, bondLength);
  }

  public enum Sizing implements UniqueIdSupplier {
    FIT_TO_SIZE, FIXED_SIZES_IN_BOUNDS, FIXED_SIZES_ALWAYS;

    public static Sizing getDefault() {
      return FIXED_SIZES_IN_BOUNDS;
    }

    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case FIT_TO_SIZE -> "FIT_TO_SIZE";
        case FIXED_SIZES_IN_BOUNDS ->  "FIXED_SIZES_IN_BOUNDS";
        case FIXED_SIZES_ALWAYS ->  "FIXED_SIZES_ALWAYS";
      };
    }

    @Override
    public String toString() {
      return switch (this) {
        case FIT_TO_SIZE -> "Fit to size";
        case FIXED_SIZES_IN_BOUNDS -> "Fixed sizes (within bounds)";
        case FIXED_SIZES_ALWAYS -> "Fixed sizes (always)";
      };
    }
  }
}
