/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package io.github.mzmine.datamodel.data.types.numbers.abstr;

import java.text.NumberFormat;
import java.util.Arrays;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.modifiers.BindingsFactoryType;
import io.github.mzmine.datamodel.data.types.rowsum.BindingsType;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public abstract class DoubleType extends NumberType<DoubleProperty> implements BindingsFactoryType {

  protected DoubleType(NumberFormat defaultFormat) {
    super(defaultFormat);
  }

  @Override
  @Nonnull
  public String getFormattedString(@Nonnull DoubleProperty value) {
    if (value.getValue() == null)
      return "";
    return getFormatter().format(value.getValue().doubleValue());
  }

  @Override
  public DoubleProperty createProperty() {
    return new SimpleDoubleProperty();
  }

  @Override
  public NumberBinding createBinding(BindingsType bind, ModularFeatureListRow row) {
    // get all properties of all features
    DoubleProperty[] prop =
        row.streamFeatures().map(f -> f.get(this)).toArray(DoubleProperty[]::new);
    switch (bind) {
      case AVERAGE:
        return Bindings.createDoubleBinding(() -> {
          double sum = 0;
          int n = 0;
          for (DoubleProperty p : prop) {
            if (p.getValue() != null) {
              sum += p.get();
              n++;
            }
          }
          return sum / n;
        }, prop);
      case MIN:
        return Bindings.createDoubleBinding(() -> {
          double min = Double.POSITIVE_INFINITY;
          for (DoubleProperty p : prop)
            if (p.getValue() != null && p.get() < min)
              min = p.get();
          return min;
        }, prop);
      case MAX:
        return Bindings.createDoubleBinding(() -> {
          double max = Double.NEGATIVE_INFINITY;
          for (DoubleProperty p : prop)
            if (p.getValue() != null && p.get() > max)
              max = p.get();
          return max;
        }, prop);
      case SUM:
        return Bindings.createDoubleBinding(() -> {
          double sum = 0;
          for (DoubleProperty p : prop)
            if (p.getValue() != null)
              sum += p.get();
          return sum;
        }, prop);
      case COUNT:
        return Bindings.createLongBinding(() -> {
          return Arrays.stream(prop).filter(p -> p.getValue() != null).count();
        }, prop);
      default:
        return null;
    }
  }
}
