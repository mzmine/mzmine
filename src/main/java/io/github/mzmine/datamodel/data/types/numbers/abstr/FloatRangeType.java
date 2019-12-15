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
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.exceptions.UndefinedRowBindingException;
import io.github.mzmine.datamodel.data.types.modifiers.BindingsType;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.Property;

public abstract class FloatRangeType extends NumberRangeType<Float> {

  protected FloatRangeType(NumberFormat defaultFormat) {
    super(defaultFormat);
  }

  @Override
  public ObjectBinding<?> createBinding(BindingsType bind, ModularFeatureListRow row) {
    // get all properties of all features
    @SuppressWarnings("unchecked")
    Property<Range<Float>>[] prop =
        row.streamFeatures().map(f -> f.get(this)).toArray(Property[]::new);
    switch (bind) {
      case RANGE:
        return Bindings.createObjectBinding(() -> {
          Range<Float> result = null;
          for (Property<Range<Float>> p : prop) {
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
