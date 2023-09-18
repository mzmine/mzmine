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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.features.types.DataType;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Simple implementation of {@link ModularDataModel} to store values together with data types. This
 * class should not be used to store small elements but rather to load and save objects
 */
public class SimpleModularDataModel implements ModularDataModel {

  private final ObservableMap<DataType, Object> map = FXCollections.observableMap(new HashMap<>());

  private final ObservableMap<Class<? extends DataType>, DataType> types = FXCollections.observableMap(
      new LinkedHashMap<>());

  @Override
  public ObservableMap<Class<? extends DataType>, DataType> getTypes() {
    return types;
  }

  @Override
  public ObservableMap<DataType, Object> getMap() {
    return map;
  }

  @Override
  public <T> boolean set(final Class<? extends DataType<T>> tclass, final T value) {
    return ModularDataModel.super.set(tclass, value);
  }

}
