/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General private License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General private License for more details.
 * 
 * You should have received a copy of the GNU General private License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.kendrickmassplot.chartutils;

import java.awt.Color;
import java.awt.Paint;
import com.google.common.collect.Range;

/**
 * Paint scales for XYZ datasets These scales can be used with the XYBlockRenderer
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class XYBlockPixelSizePaintScales {

  /*
   * returns an array with rainbow colors
   */
  public static Paint[] getFullRainBowScale() {
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
  public static Paint[] getFullRainBowScaleLowerUpperBound() {
    int ncolor = 360;
    Color[] readRainbow = new Color[ncolor];
    Color[] rainbow = new Color[ncolor + 10];

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
  public static Paint[] getFullRainBowScaleLowerBound() {
    int ncolor = 360;
    Color[] readRainbow = new Color[ncolor];
    Color[] rainbow = new Color[ncolor + 5];

    float x = (float) (1. / (ncolor + 160));
    for (int i = 0; i < readRainbow.length; i++) {
      readRainbow[i] = new Color(Color.HSBtoRGB((i) * x, 1.0F, 1.0F));
    }
    for (int i = 0; i < 5; i++) {
      rainbow[i] = new Color(0, 0, 0);
    }
    for (int i = 5; i < readRainbow.length; i++) {
      rainbow[i] = readRainbow[readRainbow.length - i - 1];
    }
    return rainbow;
  }

  /*
   * returns an array with rainbow colors with magenta as upper bound
   */
  public static Paint[] getFullRainBowScaleUpperBound() {
    int ncolor = 360;
    Color[] readRainbow = new Color[ncolor];
    Color[] rainbow = new Color[ncolor + 5];

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
  public static Paint[] getRedScale() {
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
  public static Paint[] getRedScaleLowerUpperBound() {
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
  public static Paint[] getRedScaleLowerBound() {
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
  public static Paint[] getRedScaleUpperBound() {
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
  public static Paint[] getGreenScale() {
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
  public static Paint[] getGreenScaleLowerUpperBound() {
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
  public static Paint[] getGreenScaleLowerBound() {
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
  public static Paint[] getGreenScaleUpperBound() {
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
  public static Paint[] getYellowScale() {
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
  public static Paint[] getYellowScaleLowerUpperBound() {
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
  public static Paint[] getYellowScaleLowerBound() {
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
  public static Paint[] getYellowScaleUpperBound() {
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
  public static Paint[] getCyanScale() {
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
  public static Paint[] getCyanScaleLowerUpperBound() {
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
  public static Paint[] getCyanScaleLowerBound() {
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
  public static Paint[] getCyanScaleUpperBound() {
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

  /*
   * Method to select the right scale for the applied settings in the modules kendrickmassplot and
   * vankrevelendiagram
   */
  public static Paint[] getPaintColors(String zAxisScaleType, Range<Double> zScaleRange,
      String paintScaleStyle) {
    Paint[] scale = null;
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
}
