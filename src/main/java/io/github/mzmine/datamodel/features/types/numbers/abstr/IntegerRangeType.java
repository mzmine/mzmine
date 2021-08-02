/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features.types.numbers.abstr;

import io.github.mzmine.datamodel.features.ModularFeature;
import java.text.NumberFormat;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.exceptions.UndefinedRowBindingException;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.Property;

public abstract class IntegerRangeType extends NumberRangeType<Integer> {

  protected IntegerRangeType(NumberFormat defaultFormat) {
    super(defaultFormat);
  }

  @Override
  public ObjectBinding<?> createBinding(BindingsType bind, ModularFeatureListRow row) {
    // get all properties of all features
    @SuppressWarnings("unchecked")
    Property<Range<Integer>>[] prop =
        row.streamFeatures().map(f -> (ModularFeature) f)
            .map(f -> f.get(this)).toArray(Property[]::new);
    switch (bind) {
      case RANGE:
        return Bindings.createObjectBinding(() -> {
          Range<Integer> result = null;
          for (Property<Range<Integer>> p : prop) {
            if (p.getValue() != null) {
              if (result == null)
                result = p.getValue();
              else
                result = result.span(p.getValue());
            }
          }
          return result;
        }, prop);
      case AVERAGE:
      case MIN:
      case MAX:
      case SUM:
      case COUNT:
      default:
        throw new UndefinedRowBindingException(this, bind);
    }
  }

}
