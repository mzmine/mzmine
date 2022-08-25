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
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.util.ParsingUtils;
import java.text.NumberFormat;
import java.util.List;
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
  public Class<Range<Double>> getValueClass() {
    return (Class)Range.class;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null) {
      return;
    }
    if (value instanceof Range r) {
      writer.writeCharacters(ParsingUtils.rangeToString(r));
    } else {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: " + value
              .getClass());
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    return ParsingUtils.stringToDoubleRange(reader.getElementText());
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
          double mean = 0d;
          int c = 0;
          for (var model : models) {
            Range<Double> range = model.get(this);
            if (range != null) {
              double center = (range.upperEndpoint() - range.lowerEndpoint()) / 2f;
              mean += center;
              c++;
            }
          }
          return c == 0 ? 0f : mean / c;
        }
        case SUM, CONSENSUS, RANGE: {
          // calc average center of ranges
          Range<Double> sum = null;
          for (var model : models) {
            Range<Double> range = model.get(this);
            if (range != null) {
              if (sum == null) {
                sum = range;
              } else {
                sum.span(range);
              }
            }
          }
          return sum;
        }
        case MIN, MAX: {
          throw new UnsupportedOperationException("min max bindings are undefined for Ranges");
        }
      }
    }
    return result;
  }
}
