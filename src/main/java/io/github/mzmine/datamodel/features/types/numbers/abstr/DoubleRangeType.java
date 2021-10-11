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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.exceptions.UndefinedRowBindingException;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.util.ParsingUtils;
import java.text.NumberFormat;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.Property;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DoubleRangeType extends NumberRangeType<Double> {

  protected DoubleRangeType(NumberFormat defaultFormat) {
    super(defaultFormat);
  }

  @Override
  public ObjectBinding<?> createBinding(BindingsType bind, ModularFeatureListRow row) {
    // get all properties of all features
    @SuppressWarnings("unchecked") Property<Range<Double>>[] prop = row.streamFeatures()
        .map(f -> (ModularFeature) f).map(f -> f.get(this)).toArray(Property[]::new);
    switch (bind) {
      case RANGE:
        return Bindings.createObjectBinding(() -> {
          Range<Double> result = null;
          for (Property<Range<Double>> p : prop) {
            if (p.getValue() != null) {
              if (result == null) {
                result = p.getValue();
              } else {
                result = result.span(p.getValue());
              }
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

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if(value == null) {
      return;
    }
    if (value instanceof Range r) {
      writer.writeCharacters(ParsingUtils.rangeToString(r));
    } else {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: " + value.getClass());
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {
    return ParsingUtils.stringToDoubleRange(reader.getElementText());
  }
}
