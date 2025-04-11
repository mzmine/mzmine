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

package io.github.mzmine.util;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringUtils {

  /**
   * @param str           input
   * @param onlyUseDigits use only digits contained in string - otherwise try on the whole string
   * @param defaultValue  default to return
   * @return parsed integer or the default value on exception
   */
  @Nullable
  public static Integer parseIntegerOrElse(String str, boolean onlyUseDigits,
      @Nullable Integer defaultValue) {
    try {
      return Integer.parseInt(onlyUseDigits ? getDigits(str) : str);
    } catch (Exception ex) {
      // silent
      return defaultValue;
    }
  }

  /**
   * Tries to find a number prefix like -2H will be -2
   *
   * @param str          input
   * @param defaultValue default to return
   * @return parsed integer or the default value on exception
   */
  public static int parseIntegerPrefixOrElse(String str, int defaultValue) {
    try {
      int i = 0;
      for (; i < str.length(); i++) {
        var c = str.charAt(i);
        if (!(Character.isDigit(c) || c == '+' || c == '-')) {
          break;
        }
      }

      return Integer.parseInt(str.substring(0, i));
    } catch (Exception ex) {
      // silent
      return defaultValue;
    }
  }

  /**
   * Tries to remove a number prefix like -2H will be H
   *
   * @param str input
   * @return str with removed integer prefix
   */
  public static String removeIntegerPrefix(String str) {
    try {
      int i = 0;
      for (; i < str.length(); i++) {
        var c = str.charAt(i);
        if (!(Character.isDigit(c) || c == '+' || c == '-')) {
          break;
        }
      }

      var sub = str.substring(0, i);
      int prefix = Integer.parseInt(sub);
      return sub;
    } catch (Exception ex) {
      // silent
      return str;
    }
  }

  /**
   * Will search for a sign + or - that precedes or succeeds a number so +2 and 2+ will both result
   * in +2
   *
   * @param str           input
   * @param onlyUseDigits use only digits contained in string - otherwise try on the whole string
   * @param defaultValue  default to return
   * @return parsed integer or the default value on exception
   */
  @Nullable
  public static Integer parseSignAndIntegerOrElse(@Nullable String str, boolean onlyUseDigits,
      @Nullable Integer defaultValue) {
    if (str == null || str.isBlank()) {
      return defaultValue;
    }
    int signMultiplier = findFirstPlusMinusSignMultiplier(str);
    try {
      var value = parseIntegerOrElse(str, onlyUseDigits, defaultValue);
      if (signMultiplier == 0 && value == null) {
        return null;
      } else if (value == null) {
        return signMultiplier;
      } else if (signMultiplier == 0) {
        return value;
      } else {
        return value * signMultiplier;
      }
    } catch (Exception ex) {
      // silent
      return defaultValue;
    }
  }

  /**
   * M+H+2 will return the index of the last + so length -2
   *
   * @param str             input
   * @param allowOnlyDigits between the end and the sign there can only be digits. Otherwise, allow
   *                        any char
   * @return last index of + or - or -1 if not found
   */
  public static int findLastPlusMinusSignIndex(@NotNull String str, boolean allowOnlyDigits) {
    for (int i = str.length() - 1; i >= 0; i--) {
      char c = str.charAt(i);
      if (c == '+') {
        return i;
      }
      if (c == '-') {
        return i;
      }
      if (allowOnlyDigits && !Character.isDigit(c)) {
        return -1;
      }
    }
    return -1;
  }

  /**
   * @param str             input
   * @param allowOnlyDigits between the end and the sign there can only be digits. Otherwise, allow
   *                        any char
   * @return first index of + or - or -1 if not found
   */
  private static int findFirstPlusMinusSignIndex(@NotNull final String str,
      boolean allowOnlyDigits) {
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '+') {
        return i;
      }
      if (c == '-') {
        return i;
      }
      if (allowOnlyDigits && !Character.isDigit(c)) {
        return -1;
      }
    }
    return -1;
  }

  /**
   * @param str input
   * @return the first + or - character or an empty string
   */
  private static String findFirstPlusMinusSign(@NotNull final String str) {
    var index = findFirstPlusMinusSignIndex(str, false);
    return index == -1 ? "" : String.valueOf(str.charAt(index));
  }

  /**
   * @return +1 or -1 depending on first + or - sign in string. Or defaultValue if no signs
   */
  private static int findFirstPlusMinusSignMultiplier(@NotNull final String str) {
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '+') {
        return 1;
      }
      if (c == '-') {
        return -1;
      }
    }
    return 0;
  }

  /**
   * @param str input
   * @return the digits contained in a string
   */
  @NotNull
  public static String getDigits(@Nullable final String str) {
    return str == null ? "" : org.apache.commons.lang3.StringUtils.getDigits(str);
  }

  /**
   * @return if input is null or blank - return defaultValue otherwise retain input str
   */
  public static String orDefault(@Nullable final String str, @Nullable final String defaultValue) {
    return isBlank(str) ? defaultValue : str;
  }

  public static boolean hasValue(@Nullable String str) {
    return !isBlank(str);
  }

  /**
   * @return true if input is null or blank
   */
  public static boolean isBlank(@Nullable String str) {
    return str == null || str.isBlank();
  }

  public static String inQuotes(String str) {
    return "\"" + str + "\"";
  }

  /**
   * Trim input and split at any space comma or tab. comma followed by space will be treated as one
   * separator. Parts are not trimmed - only input.
   * <p>
   * Example:
   * <p>
   * String s = "Hello,mzmine	 well, done ! " --> ["Hello", "mzmine", "", "well", "done", "!"]
   *
   * @param s input string
   * @return array of split parts
   */
  public static String[] splitAnyCommaTabSpace(String s) {
    if (isBlank(s)) {
      return new String[0];
    }
    s = s.trim();
    // solved in one regex below first try to split at , and space so that they will not produce double split
//    var split = s.split(", ");
//    if (split.length > 1) {
//      return split;
//    }
    // first split at , AND space combination then split at each , space or tab
    return s.split(", |[\\t, ]");
  }

  public static <T> String join(final List<T> values, @NotNull String delimiter,
      @NotNull final Function<T, String> mapper) {
    return values.stream().map(mapper::apply).collect(Collectors.joining(delimiter));
  }

  @Nullable
  public static Double parseDoubleOrElse(final @Nullable String s,
      final @Nullable Double defaultValue) {
    if (s == null) {
      return defaultValue;
    }
    try {
      return Double.parseDouble(s);
    } catch (Exception ex) {
      return defaultValue;
    }
  }
}
