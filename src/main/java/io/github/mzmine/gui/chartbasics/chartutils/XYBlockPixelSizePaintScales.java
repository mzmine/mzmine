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

package io.github.mzmine.gui.chartbasics.chartutils;

import com.google.common.collect.Range;
import java.awt.Color;

/**
 * Paint scales for XYZ datasets These scales can be used with the XYBlockRenderer
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class XYBlockPixelSizePaintScales {

  /*
   * Method to select the right scale for the applied settings in the modules kendrickmassplot and
   * vankrevelendiagram
   */
  public static Color[] getPaintColors(String zAxisScaleType, Range<Double> zScaleRange,
      String paintScaleStyle) {
    Color[] scale = null;
    // lower and upper bound
    if (zAxisScaleType.contains("percentile") && zScaleRange.lowerEndpoint() != 0
        && zScaleRange.upperEndpoint() != 100) {
      if (paintScaleStyle.contains("Rainbow")) {
        scale = getFullRainBowScaleLowerUpperBound();
      } else if (paintScaleStyle.contains("red")) {
        scale = getRedScaleLowerUpperBound();
      } else if (paintScaleStyle.contains("green")) {
        scale = getGreenScaleLowerUpperBound();
      } else if (paintScaleStyle.contains("yellow")) {
        scale = getYellowScaleLowerUpperBound();
      } else if (paintScaleStyle.contains("cyan")) {
        scale = getCyanScaleLowerUpperBound();
      }

    }
    // lower bound
    else if (zAxisScaleType.contains("percentile") && zScaleRange.lowerEndpoint() != 0
        && zScaleRange.upperEndpoint() == 100) {
      if (paintScaleStyle.contains("Rainbow")) {
        scale = getFullRainBowScaleLowerBound();
      } else if (paintScaleStyle.contains("red")) {
        scale = getRedScaleLowerBound();
      } else if (paintScaleStyle.contains("green")) {
        scale = getGreenScaleLowerBound();
      } else if (paintScaleStyle.contains("yellow")) {
        scale = getYellowScaleLowerBound();
      } else if (paintScaleStyle.contains("cyan")) {
        scale = getCyanScaleLowerBound();
      }
    }
    // upper bound
    else if (zAxisScaleType.contains("percentile") && zScaleRange.lowerEndpoint() == 0
        && zScaleRange.upperEndpoint() != 100) {
      if (paintScaleStyle.contains("Rainbow")) {
        scale = getFullRainBowScaleUpperBound();
      } else if (paintScaleStyle.contains("red")) {
        scale = getRedScaleUpperBound();
      } else if (paintScaleStyle.contains("green")) {
        scale = getGreenScaleUpperBound();
      } else if (paintScaleStyle.contains("yellow")) {
        scale = getYellowScaleUpperBound();
      } else if (paintScaleStyle.contains("cyan")) {
        scale = getCyanScaleUpperBound();
      }
    }
    // no bound
    else {
      if (paintScaleStyle.contains("Rainbow")) {
        scale = getFullRainBowScale();
      } else if (paintScaleStyle.contains("red")) {
        scale = getRedScale();
      } else if (paintScaleStyle.contains("green")) {
        scale = getGreenScale();
      } else if (paintScaleStyle.contains("yellow")) {
        scale = getYellowScale();
      } else if (paintScaleStyle.contains("cyan")) {
        scale = getCyanScale();
      }
    }
    return scale;
  }

  public static Color[] scaleAlphaForPaintScale(Color[] colors) {
    Color[] colorsWithScaledAlpha = new Color[colors.length];
    for (int i = 0; i < colors.length; i++) {
      int alpha;
      alpha = scaleAlphaValueLinear(1, colors.length, i);
      colorsWithScaledAlpha[i] =
          new Color(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue(), alpha);
    }
    return colorsWithScaledAlpha;
  }

  private static int scaleAlphaValueLinear(int min, int max, int value) {
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


  /*
   * returns an array with rainbow colors
   */
  public static Color[] getFullRainBowScale() {
    int ncolor = 360;
    Color[] readRainbow = new Color[ncolor];
    Color[] rainbow = new Color[ncolor];

    float x = (float) (1. / (ncolor + 160));
    for (int i = 0; i < rainbow.length; i++) {
      readRainbow[i] = new Color(Color.HSBtoRGB((i) * x, 1.0F, 1.0F));
    }
    for (int i = 0; i < rainbow.length; i++) {
      rainbow[i] = readRainbow[readRainbow.length - i - 1];
    }
    return rainbow;
  }

  /*
   * returns an array with rainbow colors with black as lower bound and magenta as upper bound
   */
  public static Color[] getFullRainBowScaleLowerUpperBound() {
    int ncolor = 360;
    Color[] readRainbow = new Color[ncolor];
    Color[] rainbow = new Color[ncolor];

    float x = (float) (1. / (ncolor + 160));
    for (int i = 0; i < readRainbow.length; i++) {
      readRainbow[i] = new Color(Color.HSBtoRGB((i) * x, 1.0F, 1.0F));
    }
    for (int i = 0; i < 5; i++) {
      rainbow[i] = new Color(0, 0, 0);
    }
    for (int i = 5; i < readRainbow.length - 5; i++) {
      rainbow[i] = readRainbow[readRainbow.length - i - 1];
    }
    for (int i = rainbow.length - 5; i < rainbow.length; i++) {
      rainbow[i] = new Color(244, 66, 223);
    }
    return rainbow;
  }

  /*
   * returns an array with rainbow colors with black as lower bound
   */
  public static Color[] getFullRainBowScaleLowerBound() {
    int ncolor = 360;
    Color[] readRainbow = new Color[ncolor];
    Color[] rainbow = new Color[ncolor];

    float x = (float) (1. / (ncolor + 160));
    for (int i = 5; i < readRainbow.length; i++) {
      readRainbow[i] = new Color(Color.HSBtoRGB((i) * x, 1.0F, 1.0F));
    }
    for (int i = 0; i < 5; i++) {
      rainbow[i] = new Color(0, 0, 0);
    }
    for (int i = 5; i < readRainbow.length; i++) {
      rainbow[i] = readRainbow[readRainbow.length - i - 1 + 5];
    }
    return rainbow;
  }

  /*
   * returns an array with rainbow colors with magenta as upper bound
   */
  public static Color[] getFullRainBowScaleUpperBound() {
    int ncolor = 360;
    Color[] readRainbow = new Color[ncolor];
    Color[] rainbow = new Color[ncolor];

    float x = (float) (1. / (ncolor + 160));
    for (int i = 0; i < readRainbow.length; i++) {
      readRainbow[i] = new Color(Color.HSBtoRGB((i) * x, 1.0F, 1.0F));
    }
    for (int i = 0; i < readRainbow.length - 5; i++) {
      rainbow[i] = readRainbow[readRainbow.length - i - 1];
    }
    for (int i = rainbow.length - 5; i < rainbow.length; i++) {
      rainbow[i] = new Color(244, 66, 223);
    }
    return rainbow;
  }

  /*
   * returns an array with red colors
   */
  public static Color[] getRedScale() {
    int ncolor = 190;
    Color[] redScale = new Color[ncolor];

    for (int i = 0; i < redScale.length; i++) {
      redScale[i] = new Color(255, redScale.length - i - 1, redScale.length - i - 1);
    }

    return redScale;
  }

  /*
   * returns an array with red colors with black as lower bound and magenta as upper bound
   */
  public static Color[] getRedScaleLowerUpperBound() {
    int ncolor = 190;
    Color[] redScale = new Color[ncolor];

    for (int i = 0; i < 5; i++) {
      redScale[i] = new Color(0, 0, 0);
    }
    for (int i = 5; i < redScale.length - 5; i++) {
      redScale[i] = new Color(255, redScale.length - i - 1, redScale.length - i - 1);
    }
    for (int i = redScale.length - 5; i < redScale.length; i++) {
      redScale[i] = new Color(244, 66, 223);
    }

    return redScale;
  }

  /*
   * returns an array with red colors with black as lower bound
   */
  public static Color[] getRedScaleLowerBound() {
    int ncolor = 190;
    Color[] redScale = new Color[ncolor];

    for (int i = 0; i < 5; i++) {
      redScale[i] = new Color(0, 0, 0);
    }
    for (int i = 5; i < redScale.length - 5; i++) {
      redScale[i] = new Color(255, redScale.length - i - 1, redScale.length - i - 1);
    }

    return redScale;
  }

  /*
   * returns an array with red colors magenta as upper bound
   */
  public static Color[] getRedScaleUpperBound() {
    int ncolor = 190;
    Color[] redScale = new Color[ncolor];

    for (int i = 0; i < redScale.length - 5; i++) {
      redScale[i] = new Color(255, redScale.length - i - 1, redScale.length - i - 1);
    }
    for (int i = redScale.length - 5; i < redScale.length; i++) {
      redScale[i] = new Color(244, 66, 223);
    }

    return redScale;
  }

  /*
   * returns an array with green colors
   */
  public static Color[] getGreenScale() {
    int ncolor = 190;
    Color[] greenScale = new Color[ncolor];
    for (int i = 0; i < greenScale.length; i++) {

      greenScale[i] = new Color(greenScale.length - i - 1, 255, greenScale.length - i - 1);

    }

    return greenScale;
  }

  /*
   * returns an array with green colors with black as lower bound and magenta as upper bound
   */
  public static Color[] getGreenScaleLowerUpperBound() {
    int ncolor = 190;
    Color[] greenScale = new Color[ncolor];

    for (int i = 0; i < 5; i++) {
      greenScale[i] = new Color(0, 0, 0);
    }
    for (int i = 5; i < greenScale.length - 5; i++) {
      greenScale[i] = new Color(greenScale.length - i - 1, 255, greenScale.length - i - 1);
    }
    for (int i = greenScale.length - 5; i < greenScale.length; i++) {
      greenScale[i] = new Color(244, 66, 223);
    }

    return greenScale;
  }

  /*
   * returns an array with red colors with black as lower bound
   */
  public static Color[] getGreenScaleLowerBound() {
    int ncolor = 190;
    Color[] greenScale = new Color[ncolor];

    for (int i = 0; i < 5; i++) {
      greenScale[i] = new Color(0, 0, 0);
    }
    for (int i = 5; i < greenScale.length - 5; i++) {
      greenScale[i] = new Color(greenScale.length - i - 1, 255, greenScale.length - i - 1);
    }

    return greenScale;
  }

  /*
   * returns an array with red colors magenta as upper bound
   */
  public static Color[] getGreenScaleUpperBound() {
    int ncolor = 190;
    Color[] greenScale = new Color[ncolor];

    for (int i = 0; i < greenScale.length - 5; i++) {
      greenScale[i] = new Color(greenScale.length - i - 1, 255, greenScale.length - i - 1);
    }
    for (int i = greenScale.length - 5; i < greenScale.length; i++) {
      greenScale[i] = new Color(244, 66, 223);
    }

    return greenScale;
  }

  /*
   * returns an array with yellow colors
   */
  public static Color[] getYellowScale() {
    int ncolor = 190;
    Color[] yellowScale = new Color[ncolor];
    for (int i = 0; i < yellowScale.length; i++) {

      yellowScale[i] = new Color(255, 255, yellowScale.length - i - 1);

    }

    return yellowScale;
  }

  /*
   * returns an array with yellow colors with black as lower bound and magenta as upper bound
   */
  public static Color[] getYellowScaleLowerUpperBound() {
    int ncolor = 190;
    Color[] yellowScale = new Color[ncolor];

    for (int i = 0; i < 5; i++) {
      yellowScale[i] = new Color(0, 0, 0);
    }
    for (int i = 5; i < yellowScale.length - 5; i++) {
      yellowScale[i] = new Color(255, 255, yellowScale.length - i - 1);
    }
    for (int i = yellowScale.length - 5; i < yellowScale.length; i++) {
      yellowScale[i] = new Color(244, 66, 223);
    }

    return yellowScale;
  }

  /*
   * returns an array with yellow colors with black as lower bound
   */
  public static Color[] getYellowScaleLowerBound() {
    int ncolor = 190;
    Color[] yellowScale = new Color[ncolor];

    for (int i = 0; i < 5; i++) {
      yellowScale[i] = new Color(0, 0, 0);
    }
    for (int i = 5; i < yellowScale.length - 5; i++) {
      yellowScale[i] = new Color(255, 255, yellowScale.length - i - 1);
    }

    return yellowScale;
  }

  /*
   * returns an array with yellow colors with magenta as upper bound
   */
  public static Color[] getYellowScaleUpperBound() {
    int ncolor = 190;
    Color[] yellowScale = new Color[ncolor];

    for (int i = 0; i < yellowScale.length - 5; i++) {
      yellowScale[i] = new Color(yellowScale.length - i - 1, yellowScale.length - i - 1, 255);
    }
    for (int i = yellowScale.length - 5; i < yellowScale.length; i++) {
      yellowScale[i] = new Color(244, 66, 223);
    }

    return yellowScale;
  }

  /*
   * returns an array with cyan colors
   */
  public static Color[] getCyanScale() {
    int ncolor = 190;
    Color[] cyanScale = new Color[ncolor];
    for (int i = 0; i < cyanScale.length; i++) {

      cyanScale[i] = new Color(cyanScale.length - i - 1, 255, 255);

    }

    return cyanScale;
  }

  /*
   * returns an array with cyan colors with black as lower bound and magenta as upper bound
   */
  public static Color[] getCyanScaleLowerUpperBound() {
    int ncolor = 190;
    Color[] cyanScale = new Color[ncolor];

    for (int i = 0; i < 5; i++) {
      cyanScale[i] = new Color(0, 0, 0);
    }
    for (int i = 5; i < cyanScale.length - 5; i++) {
      cyanScale[i] = new Color(cyanScale.length - i - 1, 255, 255);
    }
    for (int i = cyanScale.length - 5; i < cyanScale.length; i++) {
      cyanScale[i] = new Color(244, 66, 223);
    }

    return cyanScale;
  }

  /*
   * returns an array with yellow colors with black as lower bound
   */
  public static Color[] getCyanScaleLowerBound() {
    int ncolor = 190;
    Color[] cyanScale = new Color[ncolor];

    for (int i = 0; i < 5; i++) {
      cyanScale[i] = new Color(0, 0, 0);
    }
    for (int i = 5; i < cyanScale.length - 5; i++) {
      cyanScale[i] = new Color(cyanScale.length - i - 1, 255, 255);
    }

    return cyanScale;
  }

  /*
   * returns an array with yellow colors with magenta as upper bound
   */
  public static Color[] getCyanScaleUpperBound() {
    int ncolor = 190;
    Color[] cyanScale = new Color[ncolor];

    for (int i = 0; i < cyanScale.length - 5; i++) {
      cyanScale[i] = new Color(cyanScale.length - i - 1, 255, 255);
    }
    for (int i = cyanScale.length - 5; i < cyanScale.length; i++) {
      cyanScale[i] = new Color(244, 66, 223);
    }

    return cyanScale;
  }

}
