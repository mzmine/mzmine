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
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.RangeUtils;
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
    return (Class) Range.class;
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
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
          + value.getClass());
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
                sum = sum.span(range);
              }
            }
          }
          return sum;
        }
        case DIFFERENCE: {
          Range conensus = (Range) evaluateBindings(BindingsType.RANGE, models);
          return conensus == null ? null : RangeUtils.rangeLength(conensus);
        }
        case MIN, MAX: {
          throw new UnsupportedOperationException("min max bindings are undefined for Ranges");
        }
      }
    }
    return result;
  }
}
