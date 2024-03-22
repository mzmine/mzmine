package io.github.mzmine.javafx.properties;

import java.util.Arrays;
import javafx.beans.value.ObservableValue;
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
}
