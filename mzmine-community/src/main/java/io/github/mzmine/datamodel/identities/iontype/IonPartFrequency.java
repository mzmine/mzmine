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

package io.github.mzmine.datamodel.identities.iontype;

import java.util.Comparator;
import org.jetbrains.annotations.NotNull;

/**
 * How commonly an {@link IonPart} is observed. Used by {@link IonTypeRanking} to score a whole
 * {@link IonType}. Any part without an entry is treated as frequency 0, see
 * {@link IonTypeRanking#UNRANKED_FREQUENCY}.
 *
 * @param part      the referenced ion part, matched by name, charge and count direction only
 * @param frequency how often this part is observed, relative to the most common part which should
 *                  be 1. Higher is more likely.
 */
public record IonPartFrequency(@NotNull IonPartReference part, float frequency) {

  /**
   * Most frequent first. The part reference breaks ties so that the order is total and the UI list
   * does not jump around when two entries share a frequency.
   */
  public static final Comparator<IonPartFrequency> MOST_FREQUENT_FIRST = Comparator.comparingDouble(
          (IonPartFrequency e) -> e.frequency()).reversed()
      .thenComparing(IonPartFrequency::part, IonPartReference.SORTER);

  public static @NotNull IonPartFrequency of(@NotNull final IonPart part, final float frequency) {
    return new IonPartFrequency(IonPartReference.of(part), frequency);
  }

  @Override
  public @NotNull String toString() {
    return "%s (%s)".formatted(part, frequency);
  }
}
