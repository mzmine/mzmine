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

package io.github.mzmine.datamodel.features.types.numbers.scores;

import org.jetbrains.annotations.NotNull;

/**
 * Identifier for the ML model that produced an {@link MLScore}. Kept separate from
 * {@code SpectralNetworkingOptions} so the score record stays ML-only and isn't conflated with
 * cosine algorithms.
 */
public enum MLModelId {

  MS2_DEEPSCORE("MS2Deepscore", "ms2deepscore"), //
  DREAMS("DREAMS", "dreams");

  private final String label;
  private final String xmlId;

  MLModelId(String label, String xmlId) {
    this.label = label;
    this.xmlId = xmlId;
  }

  /**
   * @return short human-readable label, used in score-cell text and tooltips.
   */
  public @NotNull String label() {
    return label;
  }

  /**
   * @return stable identifier used in XML serialization. Never change for an existing enum value.
   */
  public @NotNull String xmlId() {
    return xmlId;
  }

  /**
   * @return matching enum value for the given xml id, or null if unknown.
   */
  public static MLModelId fromXmlId(@NotNull String id) {
    for (MLModelId m : values()) {
      if (m.xmlId.equals(id)) {
        return m;
      }
    }
    return null;
  }
}
