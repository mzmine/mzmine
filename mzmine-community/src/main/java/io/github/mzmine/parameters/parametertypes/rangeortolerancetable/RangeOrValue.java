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

package io.github.mzmine.parameters.parametertypes.rangeortolerancetable;

import com.google.common.collect.Range;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.Nullable;

public class RangeOrValue<T extends Number & Comparable<T>> {

  private final ObjectProperty<@Nullable T> lower = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable T> upper = new SimpleObjectProperty<>();

  private final BooleanProperty valid = new SimpleBooleanProperty(false);
  private final BooleanProperty isRange = new SimpleBooleanProperty(false);

  public RangeOrValue() {
    this(null, null);
  }

  public RangeOrValue(@Nullable T value) {
    this(value, null);
  }

  public RangeOrValue(@Nullable T lowerBound, @Nullable T upperBound) {
    lower.set(lowerBound);
    upper.set(upperBound);

    valid.bind(Bindings.createBooleanBinding(() -> {
      if (lower.get() == null && upper.get() == null) {
        return false;
      }
      if (lower.get() != null && upper.get() != null && lower.get().compareTo(upper.get()) >= 0) {
        return false;
      }
      // either one null or both not null and lower < upper
      return true;
    }, lower, upper));

    isRange.bind(Bindings.createBooleanBinding(
        () -> valid.get() && (lower.get() != null && upper.get() != null), valid, lower, upper));
  }


  public @Nullable T getLower() {
    return lower.get();
  }

  public ObjectProperty<@Nullable T> lowerProperty() {
    return lower;
  }

  public @Nullable T getUpper() {
    return upper.get();
  }

  public ObjectProperty<@Nullable T> upperProperty() {
    return upper;
  }

  public boolean isValid() {
    return valid.get();
  }

  public BooleanProperty validProperty() {
    return valid;
  }

  public boolean isRange() {
    return isRange.get();
  }

  public BooleanProperty isRangeProperty() {
    return isRange;
  }

  public RangeOrValue<T> copy() {
    return new RangeOrValue<>(lower.get(), upper.get());
  }

  public @Nullable Range<T> getRange(Tolerance<T> tolerance) {
    if (!isValid()) {
      return null;
    }

    if (isRange()) {
      return Range.closed(lower.get(), upper.get());
    }

    if (lower.get() != null) {
      return tolerance.getToleranceRange(lower.get());
    }
    return tolerance.getToleranceRange(upper.get());
  }

  @Override
  public String toString() {
    if (lower.get() != null && upper.get() != null) {
      return "%f-%f".formatted(lower.get(), upper.get());
    }
    if (lower.get() != null || upper.get() != null) {
      return "%f".formatted(lower.get() != null ? lower.get() : upper.get());
    }
    return "null-null";
  }
}
