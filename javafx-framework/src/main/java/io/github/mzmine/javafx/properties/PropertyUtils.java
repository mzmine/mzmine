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

package io.github.mzmine.javafx.properties;

import java.util.Arrays;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Subscription;

public class PropertyUtils {

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
}
