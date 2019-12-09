package io.github.mzmine.util.color;

import java.awt.Color;

public class ColorScaleUtil {

    /**
     * Get color of gradient between min and max color
     * 
     * @param min
     * @param max
     * @param minValue
     * @param maxValue
     * @param value
     * @return
     */
    public static Color getColor(Color min, Color max, double minValue,
            double maxValue, double value) {
        // hue saturation brightness
        float[] minHSB = Color.RGBtoHSB(min.getRed(), min.getGreen(),
                min.getBlue(), null);
        float[] maxHSB = Color.RGBtoHSB(max.getRed(), max.getGreen(),
                max.getBlue(), null);

        double diff = maxValue - minValue;
        double p = (Math.max(value, minValue) - minValue) / diff;
        if (p > 1)
            p = 1;

        // gradient
        float h = (float) ((maxHSB[0] - minHSB[0]) * p + minHSB[0]);
        float s = (float) ((maxHSB[1] - minHSB[1]) * p + minHSB[1]);
        float b = (float) ((maxHSB[2] - minHSB[2]) * p + minHSB[2]);
        return Color.getHSBColor(h, s, b);
    }
}
