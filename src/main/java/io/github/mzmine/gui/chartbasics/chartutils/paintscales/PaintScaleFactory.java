/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package io.github.mzmine.gui.chartbasics.chartutils.paintscales;

import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class PaintScaleFactory {

  public PaintScale createColorsForPaintScale(PaintScale paintScale) {
    return createColorsForPaintScale(paintScale, false);
  }

  public PaintScale createColorsForPaintScale(PaintScale paintScale, boolean useAlpha) {
    Color[] colors = calculateColorsForPaintScale(paintScale.getPaintScaleColorStyle(),
        paintScale.getPaintScaleBoundStyle(), useAlpha);
    double delta = (paintScale.getUpperBound() - paintScale.getLowerBound()) / (colors.length - 1);
    double value = paintScale.getLowerBound();
    for (Color color : colors) {
      paintScale.add(value, color);
      value = value + delta;
    }

    return paintScale;
  }

  private Color[] calculateColorsForPaintScale(PaintScaleColorStyle paintScaleColorStyle,
      PaintScaleBoundStyle paintScaleBoundStyle, boolean useAlpha) {
    Color[] colors;
    switch (paintScaleColorStyle) {
      case CYAN:
        colors = getCyanScale(paintScaleBoundStyle);
        break;
      case GREEN:
        colors = getGreenScale(paintScaleBoundStyle);
        break;
      case RAINBOW:
        colors = getRainbowScale();
        break;
      case GRREN_RED:
        colors = getGreenYellowRedScale(paintScaleBoundStyle);
        break;
      case RED:
        colors = getRedScale(paintScaleBoundStyle);
        break;
      case YELLOW:
        colors = getYellowScale(paintScaleBoundStyle);
        break;
      default:
        colors = getRainbowScale();
        break;
    }
    return (useAlpha) ? scaleAlphaForPaintScale(colors) : colors;

  }

  /*
   * returns an array with cyan colors
   */
  public Color[] getCyanScale(PaintScaleBoundStyle paintScaleBoundStyle) {
    int ncolor = 190;
    Color[] cyanScale = new Color[ncolor];
    int adjustedLowerBound = adjustLowerBound(paintScaleBoundStyle);
    int adjustedUpperBound = adjustUpperBound(paintScaleBoundStyle);

    for (int i = 0; i < adjustedLowerBound; i++) {
      cyanScale[i] = new Color(0, 0, 0);
    }
    for (int i = adjustedLowerBound; i < cyanScale.length - adjustedLowerBound; i++) {
      cyanScale[i] = new Color(cyanScale.length - i - 1, 255, 255);
    }
    for (int i = cyanScale.length - adjustedUpperBound; i < cyanScale.length; i++) {
      cyanScale[i] = new Color(244, 66, 223);
    }

    return cyanScale;
  }

  /*
   * returns an array with green colors
   */
  public Color[] getGreenScale(PaintScaleBoundStyle paintScaleBoundStyle) {
    int ncolor = 190;
    Color[] greenScale = new Color[ncolor];
    int adjustedLowerBound = adjustLowerBound(paintScaleBoundStyle);
    int adjustedUpperBound = adjustUpperBound(paintScaleBoundStyle);

    for (int i = 0; i < adjustedLowerBound; i++) {
      greenScale[i] = new Color(0, 0, 0);
    }
    for (int i = adjustedLowerBound; i < greenScale.length - adjustedLowerBound; i++) {
      greenScale[i] = new Color(greenScale.length - i - 1, 255, greenScale.length - i - 1);
    }
    for (int i = greenScale.length - adjustedUpperBound; i < greenScale.length; i++) {
      greenScale[i] = new Color(244, 66, 223);
    }

    return greenScale;
  }

  /*
   * returns an array with rainbow colors
   */
  private Color[] getRainbowScale() {
    Color[] rainbow = new Color[1500];
    int r = 0;
    int g = 0;
    int b = 0;
    for (int i = 0; i < 255; i++) {
      b = i;
      rainbow[i] = new Color(r, g, b);
    }

    for (int i = 255; i < 510; i++) {
      g = i - 255;
      b = 255;
      rainbow[i] = new Color(r, g, b);
    }

    for (int i = 510; i < 735; i++) {
      g = 255;
      b = 735 - i;
      rainbow[i] = new Color(r, g, b);
    }

    for (int i = 735; i < 990; i++) {
      r = i - 735;
      g = 255;
      b = 0;
      rainbow[i] = new Color(r, g, b);
    }

    for (int i = 990; i < 1245; i++) {
      r = 255;
      g = 1245 - i;
      rainbow[i] = new Color(r, g, b);
    }

    for (int i = 1245; i < 1500; i++) {
      r = 255;
      g = 0;
      b = i - 1245;
      rainbow[i] = new Color(r, g, b);
    }

    return rainbow;
  }


  /*
   * returns an array with rainbow colors
   */
  private Color[] getGreenYellowRedScale(PaintScaleBoundStyle paintScaleBoundStyle) {
    int ncolor = 100;
    Color[] scale = new Color[ncolor];
    int adjustedLowerBound = adjustLowerBound(paintScaleBoundStyle);
    int adjustedUpperBound = adjustUpperBound(paintScaleBoundStyle);

    for (int i = 0; i < adjustedLowerBound; i++) {
      scale[i] = new Color(0, 0, 0);
    }
    for (int i = adjustedLowerBound; i < scale.length - adjustedLowerBound; i++) {
      scale[i] = new Color((255 * i / 100), ((255 * (100 - i)) / 100), 0);
    }
    for (int i = scale.length - adjustedUpperBound; i < scale.length; i++) {
      scale[i] = new Color(244, 66, 223);
    }

    return scale;
  }

  /*
   * returns an array with red colors
   */
  public Color[] getRedScale(PaintScaleBoundStyle paintScaleBoundStyle) {
    int ncolor = 190;
    Color[] redScale = new Color[ncolor];
    int adjustedLowerBound = adjustLowerBound(paintScaleBoundStyle);
    int adjustedUpperBound = adjustUpperBound(paintScaleBoundStyle);

    for (int i = 0; i < adjustedLowerBound; i++) {
      redScale[i] = new Color(0, 0, 0);
    }
    for (int i = adjustedLowerBound; i < redScale.length - adjustedLowerBound; i++) {
      redScale[i] = new Color(255, redScale.length - i - 1, redScale.length - i - 1);
    }
    for (int i = redScale.length - adjustedUpperBound; i < redScale.length; i++) {
      redScale[i] = new Color(244, 66, 223);
    }

    return redScale;
  }

  /*
   * returns an array with yellow colors
   */
  public Color[] getYellowScale(PaintScaleBoundStyle paintScaleBoundStyle) {
    int ncolor = 190;
    Color[] yellowScale = new Color[ncolor];
    int adjustedLowerBound = adjustLowerBound(paintScaleBoundStyle);
    int adjustedUpperBound = adjustUpperBound(paintScaleBoundStyle);

    for (int i = 0; i < adjustedLowerBound; i++) {
      yellowScale[i] = new Color(0, 0, 0);
    }
    for (int i = adjustedLowerBound; i < yellowScale.length - adjustedLowerBound; i++) {
      yellowScale[i] = new Color(255, 255, yellowScale.length - i - 1);
    }
    for (int i = yellowScale.length - adjustedUpperBound; i < yellowScale.length; i++) {
      yellowScale[i] = new Color(244, 66, 223);
    }

    return yellowScale;
  }

  private int adjustLowerBound(PaintScaleBoundStyle paintScaleBoundStyle) {
    switch (paintScaleBoundStyle) {
      case LOWER_AND_UPPER_BOUND:
        return 5;
      case LOWER_BOUND:
        return 5;
      case NONE:
        return 0;
      case UPPER_BOUND:
        return 0;
      default:
        return 0;
    }
  }

  private int adjustUpperBound(PaintScaleBoundStyle paintScaleBoundStyle) {
    switch (paintScaleBoundStyle) {
      case LOWER_AND_UPPER_BOUND:
        return 5;
      case LOWER_BOUND:
        return 0;
      case NONE:
        return 0;
      case UPPER_BOUND:
        return 5;
      default:
        return 0;
    }
  }

  public Color[] scaleAlphaForPaintScale(Color[] colors) {
    Color[] colorsWithScaledAlpha = new Color[colors.length];
    for (int i = 0; i < colors.length; i++) {
      int alpha;
      alpha = scaleAlphaValueLinear(1, colors.length, i);
      colorsWithScaledAlpha[i] =
          new Color(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue(), alpha);
    }
    return colorsWithScaledAlpha;
  }

  private int scaleAlphaValueLinear(int min, int max, int value) {
    if (min == max) {
      return 1;
    } else {
      int maxScaled = 255;
      int minScaled = 1;
      double a = (maxScaled - minScaled) / ((double) max - min);
      double b = maxScaled - a * max;
      return (int) (a * value + b);
    }
  }


  // ---------------------------------
  public PaintScale createColorsForCustomPaintScaleFX(PaintScale paintScale,
      PaintScaleTransform transform, Collection<javafx.scene.paint.Color> colors) {
    List<Color> awtColors = new ArrayList<>();
    for (javafx.scene.paint.Color clr : colors) {
      awtColors.add(FxColorUtil.fxColorToAWT(clr));
    }
    return createColorsForCustomPaintScale(paintScale, transform, awtColors);
  }

  public PaintScale createColorsForCustomPaintScale(PaintScale paintScale,
      PaintScaleTransform transform, Collection<Color> colors) {
    Color[] gradient = calculateColorsForCustomPaintScale(colors, 1024);

    final double transformedLower = transform.transform(paintScale.getLowerBound());
    final double transformedUpper = transform.transform(paintScale.getUpperBound());
    final double transformedDelta = (transformedUpper - transformedLower) / (gradient.length - 1);

    double transformedValue = transformedLower;
    for (int i = 0; i < gradient.length; i++) {
      transformedValue = transformedLower + transformedDelta * i;
      paintScale.add(transform.revertTransform(transformedValue), gradient[i]);
    }
    return paintScale;
  }

  public Color[] calculateColorsForCustomPaintScale(Collection<Color> colors, int totalColors) {
    int stepsPerColor = totalColors / (colors.size() - 1);

    Color[] gradient = new Color[totalColors];
    int colorNum = 0;

    Iterator<Color> color = colors.iterator();
    Color currentStartColor = color.next();
    for (int step = 1; step < colors.size(); step++) {
      final int startR = currentStartColor.getRed();
      final int startG = currentStartColor.getGreen();
      final int startB = currentStartColor.getBlue();
      final int startA = currentStartColor.getAlpha();
      final Color endColor = color.next();

      final int deltaR = endColor.getRed() - currentStartColor.getRed();
      final int deltaG = endColor.getGreen() - currentStartColor.getGreen();
      final int deltaB = endColor.getBlue() - currentStartColor.getBlue();
      final int deltaA = endColor.getAlpha() - currentStartColor.getAlpha();

      for (int i = 0; i < stepsPerColor; i++) {
        float r = (startR + (i / (float) stepsPerColor) * deltaR) / 255;
        float g = (startG + (i / (float) stepsPerColor) * deltaG) / 255;
        float b = (startB + (i / (float) stepsPerColor) * deltaB) / 255;
        float a = (startA + (i / (float) stepsPerColor) * deltaA) / 255;

        gradient[colorNum] = new Color(r, g, b, a);
        colorNum++;
      }
      currentStartColor = endColor;
    }

    while (colorNum < totalColors) {
      gradient[colorNum] = currentStartColor;
      colorNum++;
    }

    return gradient;
  }
}
