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

package io.github.mzmine.util.interpolatinglookuppaintscale;

import java.awt.Color;
import java.awt.Paint;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.util.PublicCloneable;

public class InterpolatingLookupPaintScale implements PaintScale, PublicCloneable, Serializable {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private class CompatibleEntry implements Map.Entry<Double, Color> {

    private Double key;
    private Color value;

    public CompatibleEntry(Double key, Color value) {
      this.key = key;
      this.value = value;
    }

    public Double getKey() {
      return key;
    }

    public Color getValue() {
      return value;
    }

    public Color setValue(Color value) {
      Color prevValue = value;
      this.value = value;
      return prevValue;
    }
  }

  private class CompatibleTreeMap extends TreeMap<Double, Color> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public Entry<Double, Color> floorEntry(Double value) {

      Double previousKey = null;
      for (Double currentKey : this.keySet()) {
        if (currentKey > value)
          break;
        previousKey = currentKey;
      }
      if (previousKey == null)
        return null;
      return new CompatibleEntry(previousKey, this.get(previousKey));

    }

    public Entry<Double, Color> ceilingEntry(Double value) {
      for (Double currentKey : this.keySet()) {
        if (currentKey > value)
          return new CompatibleEntry(currentKey, this.get(currentKey));
      }
      return null;
    }

  }

  private CompatibleTreeMap lookupTable;

  public InterpolatingLookupPaintScale() {
    lookupTable = new CompatibleTreeMap();
  }

  public void add(double value, Color color) {
    lookupTable.put(value, color);
  }

  public Double[] getLookupValues() {
    return lookupTable.keySet().toArray(new Double[0]);
  }

  public Paint getDefaultPaint() {
    return new Color(0, 0, 0);
  }

  public Paint getPaint(double value) {
    Entry<Double, Color> floor = lookupTable.floorEntry(value);
    Entry<Double, Color> ceil = lookupTable.ceilingEntry(value);

    // Special cases, no floor, ceil or both available for given value
    if ((floor == null) && (ceil == null))
      getDefaultPaint();
    if (floor == null) {
      assert ceil != null;
      return ceil.getValue();
    }

    if (ceil == null) {
      assert floor != null;
      return floor.getValue();
    }

    // Normal case, interpolate between floor and ceil
    double floorValue = floor.getKey();
    float[] floorRGB = floor.getValue().getRGBColorComponents(null);
    double ceilValue = ceil.getKey();
    float[] ceilRGB = ceil.getValue().getRGBColorComponents(null);

    float[] rgb = new float[3];
    for (int i = 0; i < 3; i++)
      rgb[i] = (float) (floorRGB[i]
              + (ceilRGB[i] - floorRGB[i]) * (value - floorValue) / (ceilValue - floorValue));

    return new Color(rgb[0], rgb[1], rgb[2]);

  }

  public double getLowerBound() {
    return lookupTable.firstKey();
  }

  public double getUpperBound() {
    return lookupTable.lastKey();
  }

  public boolean equals(Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof InterpolatingLookupPaintScale))
      return false;

    InterpolatingLookupPaintScale that = (InterpolatingLookupPaintScale) obj;

    if (that.lookupTable.equals(lookupTable))
      return true;
    else
      return false;

  }

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

}
