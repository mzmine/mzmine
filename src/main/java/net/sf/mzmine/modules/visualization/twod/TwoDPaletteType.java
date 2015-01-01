/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.twod;

import java.awt.Color;

import net.sf.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;

/**
 * We try not to use InterpolatingLookupPaintScale, because it is quite slow
 */
enum TwoDPaletteType {

    PALETTE_GRAY20, PALETTE_GRAY5, PALETTE_GRAY1, PALETTE_RAINBOW, PALETTE_LOG;

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
	logScale.add(0, Color.cyan.darker());
	logScale.add(0.333, Color.white);
	logScale.add(0.666, Color.magenta.darker());
	logScale.add(1, Color.blue);
    }

    /**
     * 
     * @param intensity
     *            Intensity in range <0-1> inclusive
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

	case PALETTE_LOG:
	    return (Color) logScale.getPaint(intensity);

	}

	return Color.white;

    }

}
