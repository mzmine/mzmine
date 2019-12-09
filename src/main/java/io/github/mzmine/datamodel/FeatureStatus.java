/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General License as published by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General License for more details.
 * 
 * You should have received a copy of the GNU General License along with MZmine 2; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel;

import java.awt.Color;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.color.Colors;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.Vision;

public enum FeatureStatus {

    /**
     * Peak was not found
     */
    UNKNOWN,

    /**
     * Peak was found in primary peak picking
     */
    DETECTED,

    /**
     * Peak was estimated in secondary peak picking
     */
    ESTIMATED,

    /**
     * Peak was defined manually
     */
    MANUAL;

    public Color getColor() {
        Vision vision = MZmineCore.getConfiguration() != null
                ? MZmineCore.getConfiguration().getColorVision()
                : Vision.DEUTERANOPIA;
        switch (this) {
        case DETECTED:
            return Colors.getPositiveColor(vision);
        case ESTIMATED:
            return Colors.getNeutralColor();
        case MANUAL:
            return Color.BLACK;
        case UNKNOWN:
        default:
            return Colors.getNegativeColor(vision);
        }
    }

    public javafx.scene.paint.Color getColorFX() {
        Vision vision = MZmineCore.getConfiguration() != null
                ? MZmineCore.getConfiguration().getColorVision()
                : Vision.DEUTERANOPIA;
        switch (this) {
        case DETECTED:
            return ColorsFX.getPositiveColor(vision);
        case ESTIMATED:
            return ColorsFX.getNeutralColor();
        case MANUAL:
            return javafx.scene.paint.Color.BLACK;
        case UNKNOWN:
        default:
            return ColorsFX.getNegativeColor(vision);
        }
    }
}
