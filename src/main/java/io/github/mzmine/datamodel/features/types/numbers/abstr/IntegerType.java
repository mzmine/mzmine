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

package io.github.mzmine.datamodel.features.types.numbers.abstr;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class IntegerType extends NumberType<Integer> {

  protected IntegerType() {
    super(new DecimalFormat("0"));
  }

  @Override
  public NumberFormat getFormatter() {
    return DEFAULT_FORMAT;
  }

  @Override
  public Class<Integer> getValueClass() {
    return Integer.class;
  }

  @Override
  public @NotNull String getFormattedString(Integer value) {
    return value == null ? "" : getFormatter().format(value);
  }

  public @NotNull String getFormattedString(int value) {
    return getFormatter().format(value);
  }

  @Override
  public Property<Integer> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null) {
      return;
    }
    if (!(value instanceof Integer)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
          + value.getClass());
    }
    writer.writeCharacters(String.valueOf(value));
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {
    String str = reader.getElementText();
    if (str == null || str.isEmpty()) {
      return null;
    }
    return Integer.parseInt(str);
  }

  @Override
  public Object evaluateBindings(@NotNull BindingsType bindingType,
      @NotNull List<? extends ModularDataModel> models) {
    Object result = super.evaluateBindings(bindingType, models);
    if (result == null) {
      // general cases here - special cases handled in other classes
      switch (bindingType) {
        case AVERAGE: {
          // calc average center of ranges
          int mean = 0;
          int c = 0;
          for (var model : models) {
            Integer value = model.get(this);
            if (value != null) {
              mean += value;
              c++;
            }
          }
          return c == 0 ? 0f : mean / (double) c;
        }
        case SUM, CONSENSUS: {
          // calc average center of ranges
          int sum = 0;
          for (var model : models) {
            Integer value = model.get(this);
            if (value != null) {
              sum += value;
            }
          }
          return sum;
        }
        case RANGE: {
          // calc average center of ranges
          Range<Integer> range = null;
          for (var model : models) {
            Integer value = model.get(this);
            if (value != null) {
              if (range == null) {
                range = Range.singleton(value);
              } else {
                range.span(Range.singleton(value));
              }
            }
          }
          return range;
        }
        case MIN: {
          // calc average center of ranges
          Integer min = null;
          for (var model : models) {
            Integer value = model.get(this);
            if (value != null && (min == null || value < min)) {
              min = value;
            }
          }
          return min;
        }
        case MAX: {
          // calc average center of ranges
          Integer max = null;
          for (var model : models) {
            Integer value = model.get(this);
            if (value != null && (max == null || value > max)) {
              max = value;
            }
          }
          return max;
        }
      }
    }
    return result;
  }
}
