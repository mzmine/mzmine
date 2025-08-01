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

package io.github.mzmine.javafx.properties;

import java.util.Arrays;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import javafx.util.Subscription;

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
   *                  {@link #DEFAULT_TEXT_FIELD_DELAY}
   * @return a combined subscription that may be used to unsubscribe later (optional)
   */
  public static Subscription onChangeDelayedSubscription(Runnable operation, Duration delay,
      final ObservableValue<?>... triggers) {
    PauseTransition pause = new PauseTransition(delay);
    pause.setOnFinished(_ -> operation.run());
    return onChangeSubscription(pause::playFromStart, triggers);
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
}
