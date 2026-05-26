/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.types.modifiers;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Can be used to generate values of data types on demand, e.g. if they are calculated when
 * requested and the data is not stored in the data model. See
 * {@link io.github.mzmine.datamodel.features.types.annotations.PreferredAnnotationType} for an
 * example.
 *
 * <b>WARNING</b> Types extending this interface MUST NOT call
 * {@link io.github.mzmine.datamodel.features.ModularFeatureListRow#get(DataType)} with themself as
 * an argument, as this is exactly where {@link this#getValue(ModularDataModel)} is called, which
 * would lead to a stack overflow. Should such a case be necessary, we recommend to directly use
 * {@link
 * io.github.mzmine.datamodel.features.columnar_data.ColumnarModularDataModelRow#get(DataType)}
 * instead.
 *
 * @param <T> Must be the same as the {@link DataType <T>}
 */
public interface MappingType<T> {

  @Nullable T getValue(@NotNull ModularDataModel model);
}
