/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util.color;

import java.awt.Color;

/**
 * ColorPalletes (some based on http://mkweb.bcgsc.ca/colorblind) for color
 * blindness
 * 
 * @author Robin Schmid
 *
 */
public class Colors {

    private static final Color NEUTRAL_MARKER = new Color(0.5f, 0.5f, 0.5f, 1f); // grey

    // colors to mark positive or negative results (color blindness aware)
    private static final Color POSITIVE_MARKER_COLORBLIND = new Color(0.f,
            0.447f, 0.698f, 1f); // blue
    private static final Color NEGATIVE_MARKER_COLORBLIND = new Color(0.835f,
            0.369f, 0.f, 1f); // orange

    private static final Color POSITIVE_MARKER = new Color(0.220f, 0.557f,
            0.235f, 1f); // green
    private static final Color NEGATIVE_MARKER = new Color(0.808f, 0.090f,
            0.161f, 1f); // red

    /**
     * Color palette with black+7colors for color blindness: <br>
     * Black, orange, sky blue, bluish green, yellow, blue, vermillion (darker
     * orange), reddish purple <br>
     * http://mkweb.bcgsc.ca/colorblind/img/colorblindness.palettes.trivial.png
     */
    private static Color[] COLORS_7_AND_BLACK = new Color[] { //
            Color.BLACK, // black
            new Color(0.902f, 0.624f, 0f, 1f), // orange
            new Color(0.337f, 0.706f, 0.914f, 1f), // sky blue
            new Color(0.f, 0.620f, 0.451f, 1f), // bluish green
            new Color(0.941f, 0.894f, 0.259f, 1f), // yellow
            new Color(0.f, 0.447f, 0.698f, 1f), // blue
            new Color(0.835f, 0.369f, 0.f, 1f), // vermillion (darker orange)
            new Color(0.800f, 0.475f, 0.655f, 1f) }; // reddish purple

    /**
     * Color palette with black+7colors for color blindness: <br>
     * Orange, sky blue, bluish green, yellow, blue, vermillion (darker orange),
     * reddish purple <br>
     * http://mkweb.bcgsc.ca/colorblind/img/colorblindness.palettes.trivial.png
     */
    private static Color[] COLORS_7 = new Color[] { //
            new Color(0.902f, 0.624f, 0f, 1f), // orange
            new Color(0.337f, 0.706f, 0.914f, 1f), // sky blue
            new Color(0.f, 0.620f, 0.451f, 1f), // bluish green
            new Color(0.941f, 0.894f, 0.259f, 1f), // yellow
            new Color(0.f, 0.447f, 0.698f, 1f), // blue
            new Color(0.835f, 0.369f, 0.f, 1f), // vermillion (darker orange)
            new Color(0.800f, 0.475f, 0.655f, 1f) }; // reddish purple

    /**
     * Seven colors (+black)
     * 
     * @param mode
     *            color blindness?
     * @param includeBlack
     *            include black as the first color
     * @return
     */
    public static Color[] getSevenColorPalette(Vision vision,
            boolean includeBlack) {
        // so far always the same color palette
        return includeBlack ? COLORS_7_AND_BLACK : COLORS_7;
    }

    /**
     * Return positive feedback color
     * 
     * @param mode
     * @return
     */
    public static Color getPositiveColor(Vision vision) {
        switch (vision) {
        // color blind
        case DEUTERANOPIA:
        case PROTANOPIA:
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
     * @param mode
     * @return
     */
    public static Color getNegativeColor(Vision vision) {
        switch (vision) {
        // color blind
        case DEUTERANOPIA:
        case PROTANOPIA:
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
}
