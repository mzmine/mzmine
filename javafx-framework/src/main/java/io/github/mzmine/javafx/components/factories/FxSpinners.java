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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.javafx.components.factories;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Spinner;
import org.jetbrains.annotations.NotNull;

public class FxSpinners {

  public static Spinner<Integer> newSpinner(int min, int max,
      @NotNull ObjectProperty<Integer> valueProperty) {
    final Spinner<Integer> spinner = new Spinner<>(min, max, min);
    if (valueProperty.get() != null) {
      spinner.getValueFactory().setValue(valueProperty.get());
    }
    spinner.getValueFactory().valueProperty().bindBidirectional(valueProperty);
    return spinner;
  }

  public static Spinner<Integer> newSpinner(int min, int max,
      @NotNull IntegerProperty valueProperty) {
    final Spinner<Integer> spinner = new Spinner<>(min, max, valueProperty.get());
    //    spCols.getValueFactory().valueProperty()
//        .bindBidirectional(model.gridNumColumnsProperty().asObject());
    // binding in this fashion did not work, somehow it stopped updating after a few changes.
    // maybe the asObject() was gc'ed at some point.
    spinner.getValueFactory().valueProperty().addListener((_, _, i) -> {
      valueProperty.set(i != null ? i : valueProperty.get());
    });
    valueProperty.addListener((_, _, i) -> {
      spinner.getValueFactory().setValue(valueProperty.get());
    });
    return spinner;
  }
}
