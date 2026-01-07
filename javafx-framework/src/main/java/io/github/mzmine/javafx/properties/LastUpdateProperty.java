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

import java.time.Instant;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Binds multiple observables and updates the latest change as an Instant
 */
public class LastUpdateProperty extends SimpleObjectProperty<Instant> {

  /**
   * @param triggers all triggers are bound, on any change the internal Instant is updated
   */
  public LastUpdateProperty(final Observable... triggers) {
    // all properties that cause a full update need to be bound here
    var binding = Bindings.createObjectBinding(Instant::now, triggers);
    this.bind(binding);
  }
}
