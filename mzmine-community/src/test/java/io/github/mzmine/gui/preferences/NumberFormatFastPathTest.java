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

package io.github.mzmine.gui.preferences;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Guards the assumption that mzmine's shared {@link NumberFormat} instances may be used from
 * multiple threads.
 * <p>
 * {@link DecimalFormat} is documented as not thread safe, but its general formatting route
 * ({@code digitList.set(...)} plus {@code subformat(...)}) is wrapped in
 * {@code synchronized (digitList)} in the JDK, so concurrent {@code format(...)} calls on one
 * instance are serialized and correct. Its optimized fast path for doubles is <b>not</b>
 * synchronized: it writes into a shared {@code char[]} container held in the instance, so
 * concurrent calls interleave and silently produce wrong strings.
 * <p>
 * The fast path is only entered by formats that match the shape of a default
 * {@code NumberFormat.getInstance()} - grouping with a group size of 3 being the most visible
 * condition. Every format mzmine configures is grouping free and therefore takes the synchronized
 * route. This test fails if that ever stops being true, e.g. because a default pattern gains
 * grouping.
 */
class NumberFormatFastPathTest {

  // the currency sign marks a currency pattern, for which the JDK expects 2 fraction digits
  private static final char CURRENCY_SIGN = 0x00A4;

  /**
   * Mirrors {@code DecimalFormat.checkAndSetFastPathStatus()} of the JDK (verified against JDK 21,
   * 25 and openjdk/jdk master).
   *
   * @param format the format to check, may be any {@link NumberFormat} implementation
   * @return true if the format would use the unsynchronized fast path for {@code format(double)}
   */
  private static boolean isFastPathEligible(final @Nullable NumberFormat format) {
    // the fast path only exists in DecimalFormat
    if (!(format instanceof DecimalFormat decimalFormat)) {
      return false;
    }
    if (decimalFormat.getRoundingMode() != RoundingMode.HALF_EVEN //
        || !decimalFormat.isGroupingUsed() //
        || decimalFormat.getGroupingSize() != 3 //
        || decimalFormat.getMultiplier() != 1 //
        || decimalFormat.isDecimalSeparatorAlwaysShown()) {
      return false;
    }
    // assumption: exponential notation is not exposed by the public API, so it is detected in the
    // pattern. mzmine only ever puts an 'E' into a pattern for exponents, never into affix text.
    final String pattern = decimalFormat.toPattern();
    if (pattern.indexOf('E') >= 0) {
      return false;
    }
    if (decimalFormat.getMinimumIntegerDigits() != 1
        || decimalFormat.getMaximumIntegerDigits() < 10) {
      return false;
    }
    // the JDK hardcodes the number of fraction digits: 2/2 for currency, 0/3 otherwise
    final boolean currencyFormat = pattern.indexOf(CURRENCY_SIGN) >= 0;
    final int expectedMinFractionDigits = currencyFormat ? 2 : 0;
    final int expectedMaxFractionDigits = currencyFormat ? 2 : 3;
    return decimalFormat.getMinimumFractionDigits() == expectedMinFractionDigits
        && decimalFormat.getMaximumFractionDigits() == expectedMaxFractionDigits;
  }

  private static void assertNotFastPathEligible(final @NotNull String description,
      final @Nullable NumberFormat format) {
    Assertions.assertFalse(isFastPathEligible(format),
        () -> description + " uses the unsynchronized DecimalFormat fast path (pattern " + (
            format instanceof DecimalFormat df ? df.toPattern() : String.valueOf(format))
            + "). Shared instances of it are not thread safe, see the class javadoc.");
  }

  /**
   * Sanity check of {@link #isFastPathEligible(NumberFormat)} itself, so that the negative
   * assertions below are meaningful.
   */
  @Test
  void detectsKnownFastPathFormats() {
    final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
    Assertions.assertTrue(isFastPathEligible(new DecimalFormat("#,##0.###", symbols)),
        "the default grouped pattern must be recognized as fast path eligible");

    final NumberFormat defaultInstance = NumberFormat.getInstance(Locale.US);
    Assertions.assertTrue(isFastPathEligible(defaultInstance),
        "NumberFormat.getInstance() must be recognized as fast path eligible");

    // grouping free patterns never qualify
    Assertions.assertFalse(isFastPathEligible(new DecimalFormat("0.0000", symbols)));
    Assertions.assertFalse(isFastPathEligible(new DecimalFormat("0.###", symbols)));
  }

  /**
   * The defaults of the user configurable format preferences.
   */
  @Test
  void preferenceFormatsAreNotFastPathEligible() {
    final List<NumberFormatParameter> formatParameters = List.of(MZminePreferences.mzFormat,
        MZminePreferences.rtFormat, MZminePreferences.mobilityFormat, MZminePreferences.ccsFormat,
        MZminePreferences.intensityFormat, MZminePreferences.ppmFormat,
        MZminePreferences.scoreFormat, MZminePreferences.percentFormat);

    for (final NumberFormatParameter parameter : formatParameters) {
      assertNotFastPathEligible(parameter.getName(), parameter.getValue());
    }
  }

  /**
   * The static export and stable formats, which are the ones actually shared across task threads.
   */
  @Test
  void exportAndStableFormatsAreNotFastPathEligible() {
    final MZminePreferences preferences = new MZminePreferences();
    assertAllNotFastPathEligible("export", preferences.getExportFormats());
    assertAllNotFastPathEligible("stable", preferences.getStableFormats());
  }

  private static void assertAllNotFastPathEligible(final @NotNull String name,
      final @NotNull NumberFormats formats) {
    assertNotFastPathEligible(name + " mz", formats.mzFormat());
    assertNotFastPathEligible(name + " rt", formats.rtFormat());
    assertNotFastPathEligible(name + " mobility", formats.mobilityFormat());
    assertNotFastPathEligible(name + " ccs", formats.ccsFormat());
    assertNotFastPathEligible(name + " intensity", formats.intensityFormat());
    assertNotFastPathEligible(name + " ppm", formats.ppmFormat());
    assertNotFastPathEligible(name + " percent", formats.percentFormat());
    assertNotFastPathEligible(name + " score", formats.scoreFormat());
  }

  /**
   * Every pattern shape the format preferences can produce: an integer part without grouping, an
   * optional fraction, an optional exponent and an optional percent sign.
   */
  @Test
  void generatedPatternShapesAreNotFastPathEligible() {
    for (int decimals = 0; decimals <= 20; decimals++) {
      final String fraction = decimals > 0 ? "." + "0".repeat(decimals) : "";
      for (final String exponent : List.of("", "E0")) {
        for (final String percent : List.of("", "%")) {
          final String pattern = "0" + fraction + exponent + percent;
          assertNotFastPathEligible(pattern, new DecimalFormat(pattern));
        }
      }
    }
  }
}
