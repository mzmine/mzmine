/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.javafx.util.color;

import javafx.scene.paint.Color;

/**
 * ColorPalletes (some based on
 * https://davidmathlogic.com/colorblind/#%23000000-%23E69F00-%2356B4E9-%23009E73-%23F0E442-%230072B2-%23D55E00-%23CC79A7)
 * https://www.nature.com/articles/nmeth.1618 for color blindness
 *
 * @author Robin Schmid
 */
public class ColorsFX {

  private static final Color NEUTRAL_MARKER = new Color(0.5f, 0.5f, 0.5f, 1f); // grey

  // colors to mark positive or negative results (color blindness aware)
  private static final Color POSITIVE_MARKER_COLORBLIND = new Color(0.f, 0.447f, 0.698f,
      1f); // blue
  private static final Color NEGATIVE_MARKER_COLORBLIND = new Color(0.835f, 0.369f, 0.f,
      1f); // orange


  private static final Color POSITIVE_MARKER = new Color(0.220f, 0.557f, 0.235f, 1f); // green
  private static final Color NEGATIVE_MARKER = new Color(0.808f, 0.090f, 0.161f, 1f); // red

  /**
   * Color palette with black+7colors for color blindness: <br> Black, orange, sky blue, bluish
   * green, yellow, blue, vermillion (darker orange), reddish purple
   * <br>
   * https://davidmathlogic.com/colorblind/#%23000000-%23E69F00-%2356B4E9-%23009E73-%23F0E442-%230072B2-%23D55E00-%23CC79A7
   * https://www.nature.com/articles/nmeth.1618
   */
  private static Color[] COLORS_DEUTERANOPIA_BLACK = new Color[]{ //
      Color.BLACK, // black
      new Color(0.902f, 0.624f, 0f, 1f), // orange
      new Color(0.337f, 0.706f, 0.914f, 1f), // sky blue
      new Color(0.f, 0.620f, 0.451f, 1f), // bluish green
      new Color(0.941f, 0.894f, 0.259f, 1f), // yellow
      new Color(0.f, 0.447f, 0.698f, 1f), // blue
      new Color(0.835f, 0.369f, 0.f, 1f), // vermillion (darker orange)
      new Color(0.749f, 0.1725f, 0.5176f, 1f)}; // reddish purple

  /**
   * Color palette with black+7colors for color blindness: <br> Orange, sky blue, bluish green,
   * yellow, blue, vermillion (darker orange), reddish purple <br>
   * <p>
   * https://davidmathlogic.com/colorblind/#%23000000-%23E69F00-%2356B4E9-%23009E73-%23F0E442-%230072B2-%23D55E00-%23CC79A7
   * https://www.nature.com/articles/nmeth.1618
   */
  private static Color[] COLORS_DEUTERANOPIA = new Color[]{ //
      new Color(0.902f, 0.624f, 0f, 1f), // orange
      new Color(0.337f, 0.706f, 0.914f, 1f), // sky blue
      new Color(0.f, 0.620f, 0.451f, 1f), // bluish green
      new Color(0.941f, 0.894f, 0.259f, 1f), // yellow
      new Color(0.f, 0.447f, 0.698f, 1f), // blue
      new Color(0.835f, 0.369f, 0.f, 1f), // vermillion (darker orange)
      new Color(0.749f, 0.1725f, 0.5176f, 1f)}; // reddish purple

  private static Color[] COLORS_PROTANOPIA = new Color[]{new Color(0.902f, 0.624f, 0f, 1f), // orang
      new Color(0.337f, 0.706f, 0.914f, 1f), // sky blue
      Color.web("0xB38C70"), Color.web("0xFFD44D"), Color.web("0x0071AA"), Color.web("0x9C6727"),
      Color.web("0x6583A3")};

  private static Color[] COLORS_PROTANOPIA_BLACK = new Color[]{Color.BLACK,
      new Color(0.902f, 0.624f, 0f, 1f), // orang
      new Color(0.337f, 0.706f, 0.914f, 1f), // sky blue
      Color.web("0xB38C70"), Color.web("0xFFD44D"), Color.web("0x0071AA"), Color.web("0x9C6727"),
      Color.web("0x6583A3")};

  private static Color[] COLORS_TRITANOPIA = new Color[]{Color.web("0xFF8695"), // orang
      Color.web("0x00B7CA"), Color.web("0x368D96"), Color.web("0xFFCCD3"), Color.web("0x007787"),
      Color.web("0xEA4761"), Color.web("0xCA7B84")};

  private static Color[] COLORS_TRITANOPIA_BLACK = new Color[]{Color.BLACK, Color.web("0xFF8695"),
      // orang
      Color.web("0x00B7CA"), Color.web("0x368D96"), Color.web("0xFFCCD3"), Color.web("0x007787"),
      Color.web("0xEA4761"), Color.web("0xCA7B84")};


  /**
   * Seven colors (+black)
   *
   * @param vision       color blindness?
   * @param includeBlack include black as the first color
   * @return
   */
  public static Color[] getSevenColorPalette(Vision vision, boolean includeBlack) {
    // so far always the same color palette

    switch (vision) {
      case DEUTERANOPIA:
        return includeBlack ? COLORS_DEUTERANOPIA_BLACK : COLORS_DEUTERANOPIA;
      case PROTANOPIA:
        return includeBlack ? COLORS_PROTANOPIA_BLACK : COLORS_PROTANOPIA;
      case TRITANOPIA:
        return includeBlack ? COLORS_TRITANOPIA_BLACK : COLORS_TRITANOPIA;
      case NORMAL_VISION:
        return includeBlack ? COLORS_DEUTERANOPIA_BLACK : COLORS_DEUTERANOPIA;
      default:
        return includeBlack ? COLORS_DEUTERANOPIA_BLACK : COLORS_DEUTERANOPIA;
    }
  }

  /**
   * Return positive feedback color
   *
   * @return
   */
  public static Color getPositiveColor(Vision vision) {
    switch (vision) {
      // color blind
      case DEUTERANOPIA:
        return POSITIVE_MARKER_COLORBLIND;
      case PROTANOPIA:
        return POSITIVE_MARKER_COLORBLIND;
      case TRITANOPIA:
        return POSITIVE_MARKER_COLORBLIND;
      // normal vision
      case NORMAL_VISION:
      default:
        return POSITIVE_MARKER;
    }
  }

  /**
   * Return positive feedback color
   *
   * @return
   */
  public static Color getNegativeColor(Vision vision) {
    switch (vision) {
      // color blind
      case DEUTERANOPIA:
        return NEGATIVE_MARKER_COLORBLIND;
      case PROTANOPIA:
        return NEGATIVE_MARKER_COLORBLIND;
      case TRITANOPIA:
        return NEGATIVE_MARKER_COLORBLIND;
      // normal vision
      case NORMAL_VISION:
      default:
        return NEGATIVE_MARKER;
    }
  }

  public static Color getNeutralColor() {
    return NEUTRAL_MARKER;
  }

  public static String toHexString(Color color) {
    return String.format("#%02x%02x%02x", Math.min(255, (int) (color.getRed() * 255)),
        Math.min(255, (int) (color.getGreen() * 255)),
        Math.min(255, (int) (color.getBlue() * 255)));
  }

  public static String toHexOpacityString(Color color) {
    return colorChanelToHex(color.getRed()) + colorChanelToHex(color.getGreen()) + colorChanelToHex(
        color.getBlue()) + colorChanelToHex(color.getOpacity());
  }

  private static String colorChanelToHex(double chanelValue) {
    String rtn = Integer.toHexString((int) Math.min(Math.round(chanelValue * 255), 255));
    if (rtn.length() == 1) {
      rtn = "0" + rtn;
    }
    return rtn;
  }
}
