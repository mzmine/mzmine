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
   * Not really color blind friendly but how many colors really are?
   * https://sashamaps.net/docs/resources/20-colors/
   */
  Color[] palette22 = new Color[]{
      //
      new Color(0.0000f, 0.0000f, 0.0000f, 1f),  // #000000
      new Color(0.9020f, 0.0980f, 0.2941f, 1f), // #e6194B
      new Color(1.0000f, 0.8824f, 0.0980f, 1f), // #ffe119
      new Color(0.2627f, 0.3882f, 0.8471f, 1f), // #4363d8
      new Color(0.9608f, 0.5098f, 0.1922f, 1f), // #f58231
      new Color(0.5686f, 0.1176f, 0.7059f, 1f), // #911eb4
      new Color(0.2588f, 0.8314f, 0.9569f, 1f), // #42d4f4
      new Color(0.9412f, 0.1961f, 0.9020f, 1f), // #f032e6
      new Color(0.7490f, 0.9373f, 0.2706f, 1f), // #bfef45
      new Color(0.9804f, 0.7451f, 0.8314f, 1f), // #fabed4
      new Color(0.2745f, 0.6000f, 0.5647f, 1f), // #469990
      new Color(0.8627f, 0.7451f, 1.0000f, 1f), // #dcbeff
      new Color(0.6039f, 0.3882f, 0.1412f, 1f), // #9A6324
      new Color(0.2353f, 0.7059f, 0.2941f, 1f), // #3cb44b
      new Color(1.0000f, 0.9804f, 0.7843f, 1f), // #fffac8
      new Color(0.5020f, 0.0000f, 0.0000f, 1f), // #800000
      new Color(0.6667f, 1.0000f, 0.7647f, 1f), // #aaffc3
      new Color(0.5020f, 0.5020f, 0.0000f, 1f), // #808000
      new Color(1.0000f, 0.8471f, 0.6941f, 1f), // #ffd8b1
      new Color(0.0000f, 0.0000f, 0.4588f, 1f), // #000075
      new Color(0.6627f, 0.6627f, 0.6627f, 1f), // #a9a9a9
      new Color(1.0000f, 1.0000f, 1.0000f, 1f) // #ffffff
  };

  Color[] palette12 = new Color[]{new Color(0.5529f, 0.8275f, 0.7804f, 1f), // #8DD3C7
      new Color(1.0000f, 1.0000f, 0.7059f, 1f), // #FFFFB3
      new Color(0.7451f, 0.7294f, 0.8549f, 1f), // #BEBADA
      new Color(0.9843f, 0.5020f, 0.4471f, 1f), // #FB8072
      new Color(0.5020f, 0.6941f, 0.8275f, 1f), // #80B1D3
      new Color(0.9922f, 0.7059f, 0.3843f, 1f), // #FDB462
      new Color(0.7020f, 0.8706f, 0.4118f, 1f), // #B3DE69
      new Color(0.9882f, 0.8039f, 0.8980f, 1f), // #FCCDE5
      new Color(0.8510f, 0.8510f, 0.8510f, 1f), // #D9D9D9
      new Color(0.7373f, 0.5020f, 0.7412f, 1f), // #BC80BD
      new Color(0.8000f, 0.9216f, 0.7725f, 1f), // #CCEBC5
      new Color(1.0000f, 0.9294f, 0.4353f, 1f)  // #FFED6F
  };

  // 24-color palette (Glasbey)
  Color[] palette24 = new Color[]{new Color(0.9412f, 0.6392f, 1.0000f, 1f), // #F0A3FF
      new Color(0.0000f, 0.4588f, 0.8627f, 1f), // #0075DC
      new Color(0.6000f, 0.2471f, 0.0000f, 1f), // #993F00
      new Color(0.2980f, 0.0000f, 0.3608f, 1f), // #4C005C
      new Color(0.1020f, 0.1020f, 0.1020f, 1f), // #191919
      new Color(0.0000f, 0.3608f, 0.1922f, 1f), // #005C31
      new Color(0.1686f, 0.8078f, 0.2824f, 1f), // #2BCE48
      new Color(1.0000f, 0.8000f, 0.6000f, 1f), // #FFCC99
      new Color(0.5020f, 0.5020f, 0.5020f, 1f), // #808080
      new Color(0.5804f, 1.0000f, 0.7098f, 1f), // #94FFB5
      new Color(0.5608f, 0.4863f, 0.0000f, 1f), // #8F7C00
      new Color(0.6157f, 0.8000f, 0.0000f, 1f), // #9DCC00
      new Color(0.7608f, 0.0000f, 0.5333f, 1f), // #C20088
      new Color(0.0000f, 0.2000f, 0.5020f, 1f), // #003380
      new Color(1.0000f, 0.6431f, 0.0196f, 1f), // #FFA405
      new Color(1.0000f, 0.6588f, 0.7333f, 1f), // #FFA8BB
      new Color(0.2588f, 0.4000f, 0.0000f, 1f), // #426600
      new Color(1.0000f, 0.0000f, 0.0627f, 1f), // #FF0010
      new Color(0.3686f, 0.9451f, 0.9490f, 1f), // #5EF1F2
      new Color(0.0000f, 0.6000f, 0.5608f, 1f), // #00998F
      new Color(0.8784f, 1.0000f, 0.4000f, 1f), // #E0FF66
      new Color(0.4549f, 0.0392f, 1.0000f, 1f), // #740AFF
      new Color(0.6000f, 0.0000f, 0.0000f, 1f), // #990000
      new Color(1.0000f, 1.0000f, 0.5020f, 1f)  // #FFFF80
  };


  private static Color[] COLORS_DEUTERANOPIA_12 = new Color[]{new Color(0.000f, 0.000f, 0.000f, 1f),
      // Black
      new Color(0.902f, 0.624f, 0.000f, 1f), // Orange
      new Color(0.337f, 0.706f, 0.914f, 1f), // Sky Blue
      new Color(0.000f, 0.620f, 0.451f, 1f), // Bluish Green
      new Color(0.941f, 0.894f, 0.259f, 1f), // Yellow
      new Color(0.000f, 0.447f, 0.698f, 1f), // Blue
      new Color(0.835f, 0.369f, 0.000f, 1f), // Vermillion
      new Color(0.800f, 0.475f, 0.655f, 1f), // Pink
      new Color(0.545f, 0.271f, 0.075f, 1f), // Brown
      new Color(0.294f, 0.000f, 0.510f, 1f), // Purple
      new Color(0.596f, 1.000f, 0.596f, 1f), // Mint
      new Color(1.000f, 0.714f, 0.757f, 1f)  // Light Pink
  };


  private static Color[] COLORS_PROTANOPIA_16 = new Color[]{new Color(0.000f, 0.000f, 0.000f, 1f),
      // Black
      new Color(0.000f, 0.286f, 0.286f, 1f), // Dark Teal
      new Color(0.000f, 0.573f, 0.573f, 1f), // Teal
      new Color(1.000f, 0.427f, 0.714f, 1f), // Pink
      new Color(1.000f, 0.714f, 0.859f, 1f), // Light Pink
      new Color(0.286f, 0.000f, 0.573f, 1f), // Purple
      new Color(0.000f, 0.427f, 0.859f, 1f), // Blue
      new Color(0.714f, 0.427f, 1.000f, 1f), // Violet
      new Color(0.427f, 0.714f, 1.000f, 1f), // Light Blue
      new Color(0.573f, 0.000f, 0.000f, 1f), // Dark Red
      new Color(0.573f, 0.286f, 0.000f, 1f), // Brown
      new Color(0.859f, 0.427f, 0.000f, 1f), // Orange
      new Color(0.141f, 1.000f, 0.141f, 1f), // Bright Green
      new Color(1.000f, 1.000f, 0.427f, 1f), // Light Yellow
      new Color(1.000f, 0.714f, 0.341f, 1f), // Peach
      new Color(0.545f, 0.545f, 0.545f, 1f)  // Gray
  };

  private static Color[] COLORS_TRITANOPIA_24 = new Color[]{new Color(0.000f, 0.000f, 0.000f, 1f),
      // Black
      new Color(1.000f, 1.000f, 1.000f, 1f), // White
      new Color(1.000f, 0.000f, 0.000f, 1f), // Red
      new Color(0.000f, 1.000f, 0.000f, 1f), // Lime
      new Color(0.000f, 0.000f, 1.000f, 1f), // Blue
      new Color(1.000f, 1.000f, 0.000f, 1f), // Yellow
      new Color(0.000f, 1.000f, 1.000f, 1f), // Cyan
      new Color(1.000f, 0.000f, 1.000f, 1f), // Magenta
      new Color(0.502f, 0.000f, 0.000f, 1f), // Maroon
      new Color(0.000f, 0.502f, 0.000f, 1f), // Green
      new Color(0.000f, 0.000f, 0.502f, 1f), // Navy
      new Color(0.502f, 0.502f, 0.000f, 1f), // Olive
      new Color(0.000f, 0.502f, 0.502f, 1f), // Teal
      new Color(0.502f, 0.000f, 0.502f, 1f), // Purple
      new Color(0.502f, 0.502f, 0.502f, 1f), // Gray
      new Color(0.753f, 0.753f, 0.753f, 1f), // Silver
      new Color(1.000f, 0.647f, 0.000f, 1f), // Orange
      new Color(0.647f, 0.165f, 0.165f, 1f), // Brown
      new Color(1.000f, 0.753f, 0.796f, 1f), // Pink
      new Color(0.596f, 0.984f, 0.596f, 1f), // Pale Green
      new Color(0.871f, 0.722f, 0.529f, 1f), // Burlywood
      new Color(0.941f, 0.902f, 0.549f, 1f), // Khaki
      new Color(0.902f, 0.902f, 0.980f, 1f), // Lavender
      new Color(1.000f, 0.894f, 0.710f, 1f)  // Moccasin
  };

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
