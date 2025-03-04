/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import org.jetbrains.annotations.NotNull;

public abstract class ListDataType<T> extends DataType<List<T>> {

  @Override
  public Property<List<T>> createProperty() {
    return new SimpleObjectProperty<>(FXCollections.observableList(new ArrayList<>()));
  }

  @Override
  public Class<List<T>> getValueClass() {
    return (Class) List.class;
  }

  @NotNull
  @Override
  public String getFormattedString(List<T> list, boolean export) {
    return list == null || list.isEmpty() ? "" : list.getFirst().toString();
  }

  @Override
  public Object evaluateBindings(@NotNull BindingsType bindingType,
      @NotNull List<? extends ModularDataModel> models) {
    Object result = super.evaluateBindings(bindingType, models);
    if (result == null) {
      // general cases here - special cases handled in other classes
      switch (bindingType) {
        case LIST, CONSENSUS: {
          List<T> list = new ArrayList<>();
          for (var model : models) {
            List<T> value = model.get(this);
            if (value != null) {
              list.addAll(value);
            }
          }
          return list;
        }
        case COUNT: {
          int c = 0;
          for (var model : models) {
            List<T> value = model.get(this);
            if (value != null) {
              c += value.size();
            }
          }
          return c;
        }
        case AVERAGE, SUM, RANGE, MIN, MAX: {
          throw new UnsupportedOperationException(String.format(
              "Bindings type %s is not supported by standard list data type, create a special implementation or use a different binding.",
              bindingType));
        }
      }
    }
    return result;
  }
}
