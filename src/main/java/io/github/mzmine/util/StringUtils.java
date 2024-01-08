/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import org.jetbrains.annotations.NotNull;

public class StringUtils {

  /**
   * @param str           input
   * @param onlyUseDigits use only digits contained in string - otherwise try on the whole string
   * @param defaultValue  default to return
   * @return parsed integer or the default value on exception
   */
  public static int parseIntegerOrElse(String str, boolean onlyUseDigits, int defaultValue) {
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
  public static int parseSignAndIntegerOrElse(String str, boolean onlyUseDigits, int defaultValue) {
    try {
      int signMultiplier = findFirstPlusMinusSignMultiplier(str, 1);
      return signMultiplier * parseIntegerOrElse(str, onlyUseDigits, defaultValue);
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
  public static int findLastPlusMinusSignIndex(String str, boolean allowOnlyDigits) {
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
   * @param str input
   * @param allowOnlyDigits between the end and the sign there can only be digits. Otherwise, allow
   *                        any char
   * @return first index of + or - or -1 if not found
   */
  private static int findFirstPlusMinusSignIndex(final String str, boolean allowOnlyDigits) {
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
  private static String findFirstPlusMinusSign(final String str) {
    var index = findFirstPlusMinusSignIndex(str, false);
    return index == -1 ? "" : String.valueOf(str.charAt(index));
  }

  /**
   * @return +1 or -1 depending on first + or - sign in string. Or defaultValue if no signs
   */
  private static int findFirstPlusMinusSignMultiplier(final String str, int defaultValue) {
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '+') {
        return 1;
      }
      if (c == '-') {
        return -1;
      }
    }
    return defaultValue;
  }

  /**
   * @param str input
   * @return the digits contained in a string
   */
  @NotNull
  public static String getDigits(final String str) {
    return str == null ? "" : org.apache.commons.lang3.StringUtils.getDigits(str);
  }

  /**
   * @return if input is null or blank - return defaultValue otherwise retain input str
   */
  public static String orDefault(final String str, final String defaultValue) {
    return str == null || str.isBlank() ? defaultValue : str;
  }
}
