/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.twod;

import java.awt.Color;

import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;

enum TwoDPaletteType {

    PALETTE_GRAY20, PALETTE_GRAY5, PALETTE_GRAY1, PALETTE_RAINBOW;

    private InterpolatingLookupPaintScale rainbowScale;

    TwoDPaletteType() {
        rainbowScale = new InterpolatingLookupPaintScale();
        rainbowScale.add(0, Color.white);
        rainbowScale.add(0.0078125, Color.red);
        rainbowScale.add(0.0625, Color.yellow);
        rainbowScale.add(0.125, Color.green);
        rainbowScale.add(0.25, Color.pink);
        rainbowScale.add(0.5, Color.blue);
        rainbowScale.add(1, Color.black);
    }

    /**
     * 
     * @param intensity Intensity in range <0-1> inclusive
     * @return Color
     */
    Color getColor(float intensity) {

        switch (this) {

        case PALETTE_GRAY20:
            if (intensity > 0.2f)
                return Color.black;
            float gray20color = 1f - (intensity / 0.2f);
            return new Color(gray20color, gray20color, gray20color);

        case PALETTE_GRAY5:
            if (intensity > 0.05f)
                return Color.black;
            float gray5color = 1f - (intensity / 0.05f);
            return new Color(gray5color, gray5color, gray5color);

        case PALETTE_GRAY1:
            if (intensity > 0.01f)
                return Color.black;
            float gray1color = 1f - (intensity / 0.01f);
            return new Color(gray1color, gray1color, gray1color);

        case PALETTE_RAINBOW:
            return (Color) rainbowScale.getPaint(intensity);

        }

        return Color.white;

    }

}
