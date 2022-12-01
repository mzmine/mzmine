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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.features.Feature;
import java.util.Comparator;

/**
 * This is a helper class required for sorting features
 */
public class FeatureSorter implements Comparator<Feature> {

  private final SortingProperty property;
  private final SortingDirection direction;

  public FeatureSorter(SortingProperty property, SortingDirection direction) {
    this.property = property;
    this.direction = direction;
  }

  public int compare(Feature a, Feature b) {
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

  private Double getValue(Feature peak) {
    return switch (property) {
      case Area -> {
        Float area = peak.getArea();
        yield area == null ? null : area.doubleValue();
      }
      case Intensity, Height -> {
        Float height = peak.getHeight();
        yield height == null ? null : height.doubleValue();
      }
      case ID -> null;
      case MZ -> {
        Float rt = peak.getRT();
        yield rt == null ? peak.getMZ() : peak.getMZ() + rt / 100000.0;
      }
      case RT -> {
        Float rt = peak.getRT();
        yield rt == null ? peak.getMZ() : peak.getMZ() / 1000000.0 + rt;
      }
    };
  }

}
