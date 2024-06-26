/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

<<<<<<<< HEAD:mzmine-community/src/main/java/io/github/mzmine/modules/dataprocessing/featdet_spectraldeconvolutiongc/SpectralDeconvolutionAlgorithm.java
package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.List;

public interface SpectralDeconvolutionAlgorithm extends MZmineModule {

  SpectralDeconvolutionAlgorithm create(ParameterSet parameters);

  List<List<ModularFeature>> groupFeatures(List<ModularFeature> features);

  RTTolerance getRtTolerance();

  default List<List<ModularFeature>> groupFeatures() {
    throw new UnsupportedOperationException("Method not implemented. Please implement me.");
  }
========
package io.github.mzmine.javafx.components.factories;

import javafx.beans.property.StringProperty;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.Nullable;

public class FxTextFields {

  public static TextField newTextField(@Nullable Integer columnCount,
      @Nullable StringProperty textProperty, @Nullable String tooltip) {
    return newTextField(columnCount, textProperty, tooltip);
  }

  public static TextField newTextField(@Nullable Integer columnCount,
      @Nullable StringProperty textProperty, @Nullable String prompt, @Nullable String tooltip) {
    var field = new TextField();
    if (textProperty != null) {
      field.textProperty().bindBidirectional(textProperty);
    }
    if (prompt != null) {
      field.setPromptText(prompt);
    }
    if (tooltip != null) {
      field.setTooltip(new Tooltip(tooltip));
    }
    if (columnCount == null) {
      field.setPrefColumnCount(columnCount);
    }
    return field;
  }

>>>>>>>> 69eda4f52 (Merge MZMine changes):javafx-framework/src/main/java/io/github/mzmine/javafx/components/factories/FxTextFields.java
}
