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

package io.github.mzmine.javafx.properties;

import io.github.mzmine.util.StringUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TextFormatter;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PropertyUtils {

  /**
   * Default delay to wait for user input before using updated text field
   */
  public static final Duration DEFAULT_TEXT_FIELD_DELAY = Duration.millis(2000);

  /**
   * Add subscriptions (change) to all triggers and call operation on any change. If subscription is
   * never unsubscribed - then use {@link #onChange(Runnable, ObservableValue[])}
   *
   * @param operation called on any change to trigger values
   * @return a combined subscription that may be used to unsubscribe later (optional)
   */
  public static Subscription onChangeSubscription(Runnable operation,
      final ObservableValue<?>... triggers) {
    Subscription[] subscriptions = Arrays.stream(triggers)
        .map(trigger -> trigger.subscribe((_, _) -> operation.run())).toArray(Subscription[]::new);
    return Subscription.combine(subscriptions);
  }

  /**
   * Add subscriptions (change) to all triggers and call operation on any change, but with a delay.
   *
   * @param operation called on any change to trigger values
   * @param delay     delay to wait before triggering an update, may want to use
   *                  {@link #DEFAULT_TEXT_FIELD_DELAY}. null for no delay then equal to
   *                  {@link #onChangeSubscription(Runnable, ObservableValue[])}
   * @return a combined subscription that may be used to unsubscribe later (optional)
   */
  public static Subscription onChangeDelayedSubscription(Runnable operation,
      @Nullable Duration delay, final ObservableValue<?>... triggers) {
    if (delay == null) {
      return onChangeSubscription(operation, triggers);
    }

    PauseTransition pause = new PauseTransition(delay);
    // use FxThread runlater to run on the fxthread. Otherwise we cannot call dialog.showAndWait.
//    java.lang.IllegalStateException: showAndWait is not allowed during animation or layout processing
    // use Platform.runLater directly and not FxThread. Platform does extra checks
    pause.setOnFinished(_ -> Platform.runLater(operation));
    return onChangeSubscription(pause::playFromStart, triggers);
  }

  /**
   * Add ListChangeListener to all triggers and call operation on any change, but with a delay.
   *
   * @param operation called on any change to trigger values
   * @param delay     delay to wait before triggering an update, may want to use
   *                  {@link #DEFAULT_TEXT_FIELD_DELAY}
   */
  public static void onChangeListDelayed(Runnable operation, Duration delay,
      final ObservableList<?>... triggers) {
    PauseTransition pause = new PauseTransition(delay);
    // use FxThread runlater to run on the fxthread. Otherwise we cannot call dialog.showAndWait.
//    java.lang.IllegalStateException: showAndWait is not allowed during animation or layout processing
    // use Platform.runLater directly and not FxThread. Platform does extra checks
    pause.setOnFinished(_ -> Platform.runLater(operation));

    onChangeList(pause::playFromStart, triggers);
  }

  /**
   * Add ListChangeListener to all triggers and call operation on any change
   *
   * @param operation called on any change to trigger values
   */
  public static void onChangeList(Runnable operation, final ObservableList<?>... triggers) {
    for (ObservableList<?> trigger : triggers) {
      trigger.addListener((ListChangeListener) _ -> operation.run());
    }
  }

  /**
   * Add change listeners to all triggers and call operation on any change. Consider
   * {@link #onChangeSubscription(Runnable, ObservableValue[])} when removing the listeners is
   * important! Then retain the subscription and call {@link Subscription#unsubscribe()} later.
   *
   * @param operation called on any change to trigger values
   */
  public static void onChange(Runnable operation, final ObservableValue<?>... triggers) {
    for (final ObservableValue<?> trigger : triggers) {
      trigger.addListener((_, _, _) -> operation.run());
    }
  }

  /**
   * Add change listeners to all triggers and call operation on any change. Consider
   * {@link #onChangeSubscription(Runnable, ObservableValue[])} when removing the listeners is
   * important! Then retain the subscription and call {@link Subscription#unsubscribe()} later.
   *
   * @param operation called on any change to trigger values
   */
  public static void onChange(final Runnable operation, final ObservableList<?> observableList,
      final ObservableValue<?>... triggers) {
    for (final ObservableValue<?> trigger : triggers) {
      trigger.addListener((_, _, _) -> operation.run());
    }
    observableList.addListener((ListChangeListener) change -> {
      operation.run();
    });
  }

  /**
   * Unidirectional binding by derived mapped property.
   *
   * @param <T> elements of list and binding
   * @return create a new ObservableValue for the first element in a list
   */
  public static <T> ObservableValue<T> firstElementProperty(ObjectProperty<List<T>> listProp) {
    return listProp.map(list -> list == null || list.isEmpty() ? null : list.getFirst());
  }


  /**
   * Different to {@link Bindings#bindBidirectional(Property, Property, StringConverter)} to allow
   * text strings that do not map to a valid value. This means that the user enters text, conversion
   * fails, the value is null, until the text converts to a value. The intermediate text is not
   * cleared as it happens in the original bindings or when using {@link TextFormatter}.
   *
   * @param text      the text property, will be updated when the value is not null
   * @param value     the value property, will be updated when the text is changed.
   * @param converter a converter to convert between String and value class
   * @param <T>       the value class
   * @return a read-only boolean property to signal if the current text is valid. true if valid and
   * false if conversion to value failed.
   */
  public static <T> ReadOnlyBooleanProperty bindBidirectionalKeepTextDelayed(
      @NotNull Property<String> text, @NotNull Property<T> value,
      @NotNull StringConverter<T> converter) {
    return bindBidirectionalKeepText(text, value, DEFAULT_TEXT_FIELD_DELAY, converter);
  }

  /**
   * Different to {@link Bindings#bindBidirectional(Property, Property, StringConverter)} to allow
   * text strings that do not map to a valid value. This means that the user enters text, conversion
   * fails, the value is null, until the text converts to a value. The intermediate text is not
   * cleared as it happens in the original bindings or when using {@link TextFormatter}.
   *
   * @param text      the text property, will be updated when the value is not null
   * @param value     the value property, will be updated when the text is changed.
   * @param textDelay A delay to wait for user input like {@link #DEFAULT_TEXT_FIELD_DELAY} or null
   *                  if value should be updated without delay.
   * @param converter a converter to convert between String and value class
   * @param <T>       the value class
   * @return a read-only boolean property to signal if the current text is valid. true if valid and
   * false if conversion to value failed.
   */
  public static <T> ReadOnlyBooleanProperty bindBidirectionalKeepText(
      @NotNull Property<String> text, @NotNull Property<T> value, @Nullable Duration textDelay,
      @NotNull StringConverter<T> converter) {
    ReadOnlyBooleanWrapper validText = new ReadOnlyBooleanWrapper(true);

    // on change already set text to true
    text.subscribe((_) -> validText.set(true));

    onChangeDelayedSubscription(() -> {
      String currentText = text.getValue();
      // skip change if value equals text
      final T currentValue = value.getValue();
      if (currentValue != null) {
        final String currentValueStr = converter.toString(currentValue);
        if (Objects.equals(currentValueStr, currentText)) {
          // text still equals current value so no need to change
          // this is checked because converter may return null if this is an item that is not captured by converter
          validText.set(true);
          return;
        }
      }

      if (StringUtils.isBlank(currentText)) {
        value.setValue(null);
        validText.set(true);
        return;
      }

      try {
        final T convertedValue = converter.fromString(currentText);
        value.setValue(convertedValue);
        validText.set(convertedValue != null);
      } catch (Exception e) {
        // remove value
        value.setValue(null);
        validText.set(false);
      }
    }, textDelay, text);

    value.subscribe((nv) -> {
      // only update the text if the value is not null
      // otherwise the text would be always cleared in a text field if the user is typing
      // and the text does not convert to a proper value
      if (nv != null) {
        String newText = null;
        try {
          newText = converter.toString(nv);
        } catch (Exception e) {
        }
        text.setValue(newText);
        validText.set(true);
      }
    });
    return validText.getReadOnlyProperty();
  }

}
