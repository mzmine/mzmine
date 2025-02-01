/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util.scans.merging;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used for grouping - e.g. scans by fragmentation energy -> idea is to only use the value if it is
 * a single value. If it is undefined or multiple values, singleValue will be null to allow for
 * grouping of all that have multiple values.
 * <p>
 * Can be used as a key in maps
 *
 * @param singleValue the value if type is SINGLE otherwise null
 * @param type        defines if single multiple or undefined - all with multiple values will be
 *                    grouped no matter the values
 */
public record FloatGrouping(@Nullable Float singleValue, @NotNull Type type) {

  @NotNull
  public static FloatGrouping of(@NotNull final List<Float> energies) {
    final int distinctValues = (int) energies.stream().distinct().count();
    Type type = switch (distinctValues) {
      case 0 -> Type.UNDEFINED;
      case 1 -> Type.SINGLE_VALUE;
      default -> Type.MULTIPLE_VALUES;
    };
    Float energy = type == Type.SINGLE_VALUE ? energies.getFirst() : null;
    return new FloatGrouping(energy, type);
  }

  public static FloatGrouping ofUndefined() {
    return new FloatGrouping(null, Type.UNDEFINED);
  }

  public enum Type {
    SINGLE_VALUE, MULTIPLE_VALUES, UNDEFINED;

  }
}
