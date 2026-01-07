/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.otherdectectors.WavelengthType;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import java.util.Comparator;

/**
 * This is a helper class required for sorting other features
 */
public class OtherFeatureSorter implements Comparator<OtherFeature> {

  private final SortingProperty property;
  private final SortingDirection direction;

  public OtherFeatureSorter(SortingProperty property, SortingDirection direction) {
    this.property = property;
    this.direction = direction;
  }

  public int compare(OtherFeature a, OtherFeature b) {
    if (a == null && b == null) {
      return 0;
    } else if (a == null) {
      return -1 * direction.getFactor();
    } else if (b == null) {
      return 1 * direction.getFactor();
    }
    Double peak1Value = getValue(a);
    Double peak2Value = getValue(b);

    return Double.compare(peak1Value, peak2Value) * direction.getFactor();
  }

  private Double getValue(OtherFeature peak) {
    return switch (property) {
      case HEIGHT ->
          peak.get(HeightType.class) != null ? peak.get(HeightType.class).doubleValue() : 0d;
      case AREA -> peak.get(AreaType.class) != null ? peak.get(AreaType.class).doubleValue() : 0d;
      case RT -> peak.get(RTType.class) != null ? peak.get(RTType.class).doubleValue() : 0d;
      case WAVELENGTH ->
          peak.get(WavelengthType.class) != null ? peak.get(WavelengthType.class) : 0d;
    };
  }

  public enum SortingProperty {
    HEIGHT, AREA, RT, WAVELENGTH;
  }

}
