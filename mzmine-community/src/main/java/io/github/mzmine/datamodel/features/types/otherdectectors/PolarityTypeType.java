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

package io.github.mzmine.datamodel.features.types.otherdectectors;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.abstr.EnumDataType;
import io.github.mzmine.datamodel.features.types.modifiers.MappingType;
import java.util.function.Function;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PolarityTypeType extends EnumDataType<PolarityType> implements
    MappingType<PolarityType> {

  public static final Function<@Nullable String, @Nullable PolarityType> mapper = s -> {
    if (s == null || s.isBlank()) {
      return null;
    }
    return PolarityType.parseFromString(s);
  };

  @Override
  public @NotNull String getUniqueID() {
    return "polarity";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Polarity";
  }

  @Override
  public Property<PolarityType> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<PolarityType> getValueClass() {
    return PolarityType.class;
  }

  @Override
  public @Nullable Function<@Nullable String, @Nullable PolarityType> getMapper() {
    return mapper;
  }

  /**
   * The polarity is not stored on the row but derived on demand from
   * {@link FeatureListRow#getRepresentativePolarity()}. Other {@link ModularDataModel}s (e.g.
   * {@link io.github.mzmine.datamodel.otherdetectors.OtherFeature} or feature annotations) still
   * store and read the value directly, as only
   * {@link ModularFeatureListRow#get(io.github.mzmine.datamodel.features.types.DataType)} routes
   * through {@link MappingType}.
   */
  @Override
  public @Nullable PolarityType getValue(@NotNull final ModularDataModel model) {
    return model instanceof FeatureListRow row ? row.getRepresentativePolarity() : null;
  }
}
