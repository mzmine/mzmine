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

import org.jetbrains.annotations.NotNull;

/**
 * How several overlays share the 3D view.
 */
enum Surface3DLayout {
  /**
   * All overlays at their coordinates. Coincident surfaces hide each other.
   */
  OVERLAY("Overlay"),
  /**
   * One surface of the maximum, colored by mixing up to three overlays by intensity ratio
   */
  BLEND("Blend colors"),
  /**
   * Small multiples with linked rotation and zoom
   */
  GRID("Side by side");

  private final String label;

  Surface3DLayout(@NotNull final String label) {
    this.label = label;
  }

  /**
   * @return true if the overlays are merged into one composite surface
   */
  public boolean composite() {
    return switch (this) {
      case BLEND -> true;
      case OVERLAY, GRID -> false;
    };
  }

  @Override
  public @NotNull String toString() {
    return label;
  }
}
