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

import java.time.Instant;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

/**
 * Binds multiple observables and updates the latest change as an Instant
 */
public class LastUpdateProperty extends SimpleObjectProperty<Instant> {

  private LastUpdateProperty() {

  }

  public LastUpdateProperty(final Observable... triggers) {
    this();
    // all properties that cause a full update need to be bound here
    var binding = Bindings.createObjectBinding(Instant::now, triggers);
    this.bind(binding);
  }

  /**
   *
   * @param enabled  only updates if enabled is true
   * @param triggers any of the triggers or enabled changes value then triggers an update to the
   *                 instant
   */
  public static LastUpdateProperty withEnabledProperty(BooleanProperty enabled,
      final ObservableValue<?>... triggers) {
    final LastUpdateProperty lastUpdate = new LastUpdateProperty();

    // for enabled we always trigger a change event
    enabled.subscribe((_, _) -> lastUpdate.setValue(Instant.now()));

    // binding failed with infinite loop
    PropertyUtils.onChange(() -> {
      if (!enabled.get()) {
        return; // keep old value for now update happened
      }
      lastUpdate.setValue(Instant.now()); // update
    }, triggers);

    return lastUpdate;
  }
}
