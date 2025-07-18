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

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the polarity of ionization.
 */
public enum PolarityType implements UniqueIdSupplier {

  POSITIVE(+1, "+"), //
  NEGATIVE(-1, "-"), //
  NEUTRAL(0, "n"), //
  /**
   * Any is used in filters
   */
  ANY(0, "Any"), //
  UNKNOWN(0, "?");

  private final int sign;
  private final String charValue;

  PolarityType(int sign, String charValue) {
    this.sign = sign;
    this.charValue = charValue;
  }

  /**
   * @param str The string.
   * @return {@link PolarityType#UNKNOWN} if the str is null or cannot be matched.
   */
  @NotNull
  public static PolarityType parseFromString(@Nullable String str) {
    if (str == null) {
      return UNKNOWN;
    }

    return switch (str.toLowerCase()) {
      case "+", "positive", "pos", "+1", "1+", "1" -> PolarityType.POSITIVE;
      case "-", "negative", "neg", "-1", "1-" -> PolarityType.NEGATIVE;
      case "any polarity", "any" -> PolarityType.ANY; // sometimes used as filter option
      case "neutral", "n" -> PolarityType.NEUTRAL;
      case "unknown" -> PolarityType.UNKNOWN;
      default -> UNKNOWN;
    };
  }

  public static PolarityType fromSingleChar(String s) {
    for (PolarityType p : values()) {
      if (p.charValue.equals(s)) {
        return p;
      }
    }
    return UNKNOWN;
  }

  public static PolarityType fromInt(@Nullable Integer i) {
    if (i == null || i == 0) {
      return UNKNOWN;
    } else if (i < 0) {
      return NEGATIVE;
    } else {
      return POSITIVE;
    }
  }

  public String asSingleChar() {
    return charValue;
  }

  /**
   * @return +1 for positive polarity, -1 for negative polarity, and 0 for neutral or unknown
   * polarity.
   */
  public int getSign() {
    return sign;
  }

  public String toLabel() {
    if (this == ANY) {
      return "Any polarity";
    }
    return StringUtils.capitalize(name().toLowerCase());
  }

  @Override
  public String toString() {
    return asSingleChar();
  }

  /**
   * @return true if positive or negative
   */
  public static boolean isDefined(@Nullable PolarityType polarity) {
    return polarity != null && polarity.isDefined();
  }

  /**
   * @return true if positive or negative
   */
  public boolean isDefined() {
    return this == POSITIVE || this == NEGATIVE;
  }

  public boolean includesPositive() {
    return this == POSITIVE || this == ANY;
  }

  public boolean includesNegative() {
    return this == NEGATIVE || this == ANY;
  }

  /**
   * @return true if charge and polarity matches. e.g., -n and negative or any polarity
   */
  public boolean includesCharge(final int charge) {
    return this == ANY || (this == NEGATIVE && charge < 0) || (this == POSITIVE && charge > 0);
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case ANY -> "ANY";
      case NEGATIVE -> "NEGATIVE";
      case POSITIVE -> "POSITIVE";
      case NEUTRAL -> "NEUTRAL";
      case UNKNOWN -> "UNKNOWN";
    };
  }
}
