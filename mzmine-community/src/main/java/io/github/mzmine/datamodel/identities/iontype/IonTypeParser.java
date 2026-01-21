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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.util.StringUtils;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Parses strings to [IonType].
///
/// Requirements:
/// - M is optional
/// - names starting with `(` need to be enclosed by `()`
/// - names with - or + need to be enclosed by `()`
/// - total charge of ion type needs to be enclosed by `()` or `[]`
///
/// Formats:
/// - total charge version: `[M+H]+2` `[M+H]2+` `(M+H)+2` `(M+H)2+`
/// - +H (without M)
/// - M+H or `[M+H]` (total charge missing)
/// - M+H+
/// - `M+(H+)`
public class IonTypeParser {

  /// Charge pattern at the end of string as +- with optional number
  ///
  /// Options are:
  /// - Just sign: + -
  /// - sign number: +2
  /// - bracket number sign: ]2+ or )2+
  /// - start of string with sign: +2M or -2M
  ///
  // function now goes through chars for more flexibility
//  public static final Pattern CHARGE_PATTERN = Pattern.compile("(^|[])])(\\d+[+-])$|([+-]\\d*)$");

  /// Charge pattern at the end of string as +- with optional number
  ///
  /// Options are:
  /// - Just sign: + -
  /// - sign number: +2
  /// - bracket number sign: ]2+ or )2+
  @Nullable
  public static Integer parseChargeOrElse(@Nullable String input, @Nullable Integer defaultValue) {
    if (input == null) {
      return null;
    }
    input = StringUtils.removeAllWhiteSpace(input);

    int signIndex = -1;
    boolean allowReversedNotation = true; // allowed if input is only number and sign like 2+ without prefix
    int index = input.length() - 1;
    for (; index >= 0; index--) {
      final char c = input.charAt(index);
      if (StringUtils.isSign(c)) {
        if (signIndex != -1) {
          break; // double sign means this belongs to the name
        }
        signIndex = index;
      } else if (c == ']' || c == ')') {
        allowReversedNotation = true;
        break;
      } else if (!StringUtils.isDigit(c)) {
        allowReversedNotation = false; // no ] ) so reverse is not allowed
        break;
      }
    }

    if (signIndex == -1) {
      return null;
    }

    if (allowReversedNotation) {
      index++;
    } else {
      index = signIndex;
    }

    final String chargeStr = input.substring(index);
    return StringUtils.parseSignAndIntegerOrElse(chargeStr, false, defaultValue);
  }


  /**
   *
   * @param input
   * @return the index of ] ) allowing digits, +, -, and whitespace
   */
  public static int findLastIndexOfClosingBracketBraceAllowCharge(@NotNull String input) {
    boolean hasSign = false;
    for (int i = input.length() - 1; i >= 0; i--) {
      final char c = input.charAt(i);
      if (c == ']' || c == ')') {
        return i;
      } else if (StringUtils.isSign(c)) {
        if (hasSign) {
          return -1; // double sign not allowed in charge
        }
        hasSign = true;
      } else if (!(StringUtils.isDigit(c) || Character.isWhitespace(c))) {
        return -1; // other char than digit or whitespace ends the charge
      }
    }
    return -1; // nothing found
  }

  /**
   * Do not remove whitespace as this is allowed in part names
   *
   * @param input
   * @return
   */
  @Nullable
  public static IonType parse(@Nullable String input) {
    if (StringUtils.isBlank(input)) {
      return null;
    }
    input = input.trim();

    // default charge is null, if total charge is unknown then use charge of known iparts
    Integer detectedCharge = null;
    // last index of ) ]
    final int chargeIndex = findLastIndexOfClosingBracketBraceAllowCharge(input);
    if (chargeIndex != -1) {
      // might be null still if notation was [M+H] which is fine as well
      detectedCharge = parseChargeOrElse(input.substring(chargeIndex).trim(), null);
      // remove leading bracket if end bracket
      final char first = input.charAt(0);
      int startIndex = first == '[' || first == '(' ? 1 : 0;

      input = input.substring(startIndex, chargeIndex).trim();
    }

    // input is now without total charge notation and without [] or () surrounding
    // next parse 2M multiplier
    final int molMIndex = findMolMIndex(input);

    final int molMultiplier;
    if (molMIndex > 0) {
      molMultiplier = Integer.parseInt(input.substring(0, molMIndex).trim());
    } else {
      molMultiplier = 1;
    }

    if (molMIndex >= 0) {
      // cut off the 2M or M
      input = input.substring(molMIndex + 1);
    }

    // rest is all parts
    final List<IonPart> parts = IonParts.parseMultiple(input);

    IonType ion = IonType.create(parts, molMultiplier);

    int chargeDiff = 0;
    if (detectedCharge == null && ion.totalCharge() == 0) {
      // default to charge 1 because we are looking at ions and mostly in positive ion mode
      // for negative the charge needs to be defined
      chargeDiff = 1;
    } else if (detectedCharge != null) {
      chargeDiff = detectedCharge - ion.totalCharge();
    }
    if (chargeDiff != 0) {
      // if we have a single modification then we can set its charge to the parsed charge
      if (parts.size() == 1) {
        final IonPart first = parts.getFirst();
        final int actualCharge = requireNonNullElse(detectedCharge, chargeDiff);
        // needs to be dividable by count
        if (Math.abs(actualCharge) % Math.abs(first.count()) == 0) {
          final List<IonPart> newChargeMods = List.of(
              first.withSingleCharge(actualCharge / first.count()));
          return IonType.create(newChargeMods, molMultiplier);
        }
      }
      // if more parts then add silent charges instead
      IonPart chargeChanger = IonParts.SILENT_CHARGE.withCount(chargeDiff);
      parts.add(chargeChanger);
      return IonType.create(parts, molMultiplier);
    } else {
      return ion;
    }
  }

  private static int findMolMIndex(@NotNull String input) {
    int molMIndex = -1;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (molMIndex == -1 && c == 'M') {
        molMIndex = i; // found - but need to check that its not part of a name like Methanol
      } else if (StringUtils.isSign(c)) {
        return molMIndex;
      } else if (!(StringUtils.isDigit(c) || Character.isWhitespace(c))) {
        return -1; // other character says this M was a name
      }
    }
    return molMIndex;
  }

}
