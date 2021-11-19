/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
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
