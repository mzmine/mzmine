/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_untargetedLabeling;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fractional contribution of the tracer label for an isotopologue cluster. Stored on the M+0 row of
 * the cluster only.
 */
public class LabeledFractionType extends DataType<Double> implements AnnotationType {

  @NotNull
  @Override
  public String getHeaderString() {
    return "Labeled fraction";
  }

  @Override
  public String getUniqueID() {
    return "labeled_fraction";
  }

  @Override
  public @NotNull Class<Double> getValueClass() {
    return Double.class;
  }

  @Override
  public @NotNull Property<Double> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Nullable
  public Double getValue(ModularFeatureListRow row) {
    return row.get(this);
  }

  @Override
  public @NotNull String getFormattedString(Double value, boolean export) {
    return value == null ? "" : String.format("%.4f", value);
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;  // Make columns visible by default
  }
}
