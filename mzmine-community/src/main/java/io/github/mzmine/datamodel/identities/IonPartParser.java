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

package io.github.mzmine.datamodel.identities;

import static io.github.mzmine.util.StringUtils.parseSignAndIntegerOrElse;

import io.github.mzmine.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IonPartParser {

  // may have charge defined or not
  // +2(H+) +(Fe+3)
  // -2Cl or +Fe
  // will produce 4 groups with naming pattern
  // uncharged will be group1=charge group2=formula
  // charged will be   group1=charge group3=formula group4=charge
  public static final Pattern PART_PATTERN = Pattern.compile("""
      (?x)                      # Enable comments/free-spacing mode
      (?<count>[+-]\\d*)        # count multiplier either +- or with number
      (?:                       # do not capture the OR| group
      (?![(])                   # negative lookahead NO (
      (?<formula>[a-zA-Z][A-Za-z0-9]*[a-zA-Z0-9]|[a-zA-Z])  # first case without parentheses just +H2O (no charge)
      | [(]                     # alternative case for charged requires () to enclose charge like +(Fe+3)
      (?<parenthesisFormula>[a-zA-Z][-A-Za-z0-9=\\#$:%*@.]*[a-zA-Z0-9]|[a-zA-Z])  # formulas like +CH3-OH with bindings are allowed
      (?<charge>[+-]\\d*)?      # optional charge state followed by )
      [)])                      # ends with ) and end of non-capturing group""");

  public static @Nullable IonPart parse(final @NotNull String part) {
    Matcher matcher = PART_PATTERN.matcher(StringUtils.removeAllWhiteSpace(part));
    while (matcher.find()) {
      IonPart ion = fromMatcher(matcher);
      if (ion != null) {
        return ion;
      }
    }
    return null;
  }

  public static List<IonPart> parseMultiple(final String input) {
    Matcher matcher = PART_PATTERN.matcher(StringUtils.removeAllWhiteSpace(input));
    List<IonPart> parts = new ArrayList<>();
    while (matcher.find()) {
      IonPart ion = fromMatcher(matcher);
      if (ion != null) {
        parts.add(ion);
      }
    }
    return parts;
  }

  @SuppressWarnings("DataFlowIssue")
  @Nullable
  private static IonPart fromMatcher(final Matcher matcher) {
    final String countStr = matcher.group(1);
    final String chargeStr = matcher.group("charge");
    String formula = matcher.group("formula");
    if (formula == null) {
      formula = matcher.group("parenthesisFormula");
    }

    if (StringUtils.isBlank(formula)) {
      return null;
    }
    final int count = parseSignAndIntegerOrElse(countStr, true, 1);
    var ionPart = IonParts.findPartByNameOrFormula(formula, count);
    Integer charge = parseSignAndIntegerOrElse(chargeStr, true, null);

    // mismatch in charge
    // like when the current list of ion parts defines Fe+2 but not Fe+3 which was loaded
    // create a new instance with that charge
    if (charge != null && charge != ionPart.singleCharge()) {
      return ionPart.withSingleCharge(charge);
    }
    return ionPart;
  }


}
