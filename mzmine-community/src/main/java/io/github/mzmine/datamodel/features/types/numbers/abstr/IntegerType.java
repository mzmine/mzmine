/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.types.numbers.abstr;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
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
  public NumberFormat getFormat() {
    return DEFAULT_FORMAT;
  }

  @Override
  public NumberFormat getExportFormat() {
    return DEFAULT_FORMAT;
  }

  @Override
  public Class<Integer> getValueClass() {
    return Integer.class;
  }

  public @NotNull String getFormattedString(int value) {
    return getFormat().format(value);
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
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
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
                range = range.span(Range.singleton(value));
              }
            }
          }
          return range;
        }
        case DIFFERENCE: {
          Integer min = null;
          Integer max = null;
          for (var model : models) {
            Integer value = model.get(this);
            if (value != null) {
              if (max == null || value > max) {
                max = value;
              }
              if (min == null || value < min) {
                min = value;
              }
            }
          }
          return min == null ? null : max - min;
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
