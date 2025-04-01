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

package io.github.mzmine.javafx.components.factories;

import io.github.mzmine.javafx.components.formatters.NonZeroIntegerSpinnerValueFactory;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.Nullable;

public class FxSpinners {

  public static Spinner<Integer> newSpinner(IntegerProperty valueProperty) {
    return newSpinner(valueProperty, null);
  }

  public static Spinner<Integer> newSpinner(IntegerProperty valueProperty,
      @Nullable String tooltip) {
    return newSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, valueProperty, tooltip);
  }


  public static Spinner<Integer> newSpinner(int min, int max, IntegerProperty valueProperty) {
    return newSpinner(min, max, valueProperty, null);
  }

  public static Spinner<Integer> newSpinner(int min, int max, IntegerProperty valueProperty,
      @Nullable String tooltip) {

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
    if (tooltip != null) {
      spinner.setTooltip(new Tooltip(tooltip));
    }
    return spinner;
  }

  public static Spinner<Integer> newNonZeroSpinner(ObjectProperty<Integer> valueProperty,
      @Nullable String tooltip) {
    return newNonZeroSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, valueProperty, tooltip);
  }

  public static Spinner<Integer> newNonZeroSpinner(int min, int max,
      ObjectProperty<Integer> valueProperty, @Nullable String tooltip) {
    final Spinner<Integer> spinner = newSpinner(min, max, valueProperty, tooltip);
    spinner.setValueFactory(new NonZeroIntegerSpinnerValueFactory(min, max));
    return spinner;
  }

  public static Spinner<Integer> newSpinner(ObjectProperty<Integer> valueProperty) {
    return newSpinner(valueProperty, null);
  }

  public static Spinner<Integer> newSpinner(ObjectProperty<Integer> valueProperty,
      @Nullable String tooltip) {
    return newSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, valueProperty, tooltip);
  }

  public static Spinner<Integer> newSpinner(int min, int max,
      ObjectProperty<Integer> valueProperty) {
    return newSpinner(min, max, valueProperty, null);
  }

  public static Spinner<Integer> newSpinner(int min, int max, ObjectProperty<Integer> valueProperty,
      @Nullable String tooltip) {
    var spinner = new Spinner<Integer>();
    spinner.setValueFactory(new IntegerSpinnerValueFactory(min, max));
    spinner.getValueFactory().valueProperty().bindBidirectional(valueProperty);
    if (tooltip != null) {
      spinner.setTooltip(new Tooltip(tooltip));
    }

    return spinner;
  }
}
