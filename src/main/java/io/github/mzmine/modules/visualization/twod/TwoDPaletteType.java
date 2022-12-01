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

package io.github.mzmine.modules.visualization.twod;

import java.awt.Color;

import io.github.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;

/**
 * We try not to use InterpolatingLookupPaintScale, because it is quite slow
 */
enum TwoDPaletteType {

  PALETTE_GRAY20, PALETTE_GRAY5, PALETTE_GRAY1, PALETTE_RAINBOW, PALETTE_logger;

  private InterpolatingLookupPaintScale rainbowScale;
  private InterpolatingLookupPaintScale logScale;

  TwoDPaletteType() {
    rainbowScale = new InterpolatingLookupPaintScale();
    rainbowScale.add(0, Color.white);
    rainbowScale.add(0.04, Color.red);
    rainbowScale.add(0.125, Color.orange);
    rainbowScale.add(0.25, Color.yellow);
    rainbowScale.add(0.5, Color.blue);
    rainbowScale.add(1, Color.cyan);
    logScale = new InterpolatingLookupPaintScale();
    // logScale.add(0, Color.cyan.darker());
    // logScale.add(0.5, Color.white);
    // logScale.add(1, Color.magenta.darker());
    logScale.add(0, Color.white);
    logScale.add(0.333, Color.cyan.darker());
    logScale.add(0.666, Color.magenta.darker());
    logScale.add(1, Color.blue);
  }

  /**
   * 
   * @param intensity Intensity in range <0-1> inclusive
   * @return Color
   */
  Color getColor(double intensity) {

    switch (this) {

      case PALETTE_GRAY20:
        if (intensity > 0.2f)
          return Color.black;
        float gray20color = (float) (1 - (intensity / 0.2d));
        return new Color(gray20color, gray20color, gray20color);

      case PALETTE_GRAY5:
        if (intensity > 0.05f)
          return Color.black;
        float gray5color = (float) (1 - (intensity / 0.05d));
        return new Color(gray5color, gray5color, gray5color);

      case PALETTE_GRAY1:
        if (intensity > 0.01f)
          return Color.black;
        float gray1color = (float) (1 - (intensity / 0.01d));
        return new Color(gray1color, gray1color, gray1color);

      case PALETTE_RAINBOW:
        return (Color) rainbowScale.getPaint(intensity);

      case PALETTE_logger:
        return (Color) logScale.getPaint(intensity);

    }

    return Color.white;

  }

}
