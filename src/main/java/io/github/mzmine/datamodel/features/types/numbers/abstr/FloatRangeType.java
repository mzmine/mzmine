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

public abstract class FloatRangeType extends NumberRangeType<Float> {

  protected FloatRangeType(NumberFormat defaultFormat) {
    super(defaultFormat);
  }

  @Override
  public ObjectBinding<?> createBinding(BindingsType bind, ModularFeatureListRow row) {
    // get all properties of all features
    @SuppressWarnings("unchecked") Property<Range<Float>>[] prop = row.streamFeatures()
        .map(f -> (ModularFeature) f).map(f -> f.get(this)).toArray(Property[]::new);
    return switch (bind) {
      case RANGE -> Bindings.createObjectBinding(() -> {
        Range<Float> result = null;
        for (Property<Range<Float>> p : prop) {
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
      case AVERAGE, MIN, MAX, SUM, COUNT, CONSENSUS, LIST -> throw new UndefinedRowBindingException(
          this, bind);
    };
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value instanceof Range r) {
      writer.writeCharacters(ParsingUtils.rangeToString(r));
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {
    return ParsingUtils.stringToFloatRange(reader.getElementText());
  }
}
