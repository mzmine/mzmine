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

import static io.github.mzmine.util.StringUtils.isDigit;
import static io.github.mzmine.util.StringUtils.isSign;
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
  // uncharged will be group1=count, group2=formula group3=charge
  // charged will be   group1=count, group4=formula group5=parenthesisCharge
  public static final Pattern PART_PATTERN = Pattern.compile("""
      (?x)                      # Enable comments/free-spacing mode
      (?<count>[+-]\\d*)        # count multiplier either +- or with number
      (?:                       # do not capture the OR| group
      (?![(])                   # negative lookahead NO ( first case without parentheses just +H2O (optional charge)
      (?<formula>[\\p{L}\\[]    # start with any unicode letter or [ for isotopes or [formulas] 
          [\\]\\[!=\\#$:%*@.\\p{L}0-9]* # allow many more characters and special symbols like [] for isotopes
          [\\p{L}0-9\\]]        # has to end with a letter or number or ] formulas may end with ] for charged version [F]-
          |[a-zA-Z]             # alternative to the above formula may be only one letter like F
      )                         # end of formula
      # charge should be entered with () but can also without
      ((?<charge>[+-]\\d*)(?=[+-]|$))? # optional charge state when without () then require NO additional +- after
      | [(]                     # alternative case requires () to enclose charge like +(Fe+3) and special symbols like +-
      (?<parenthesisFormula>
      (?:[\\p{L}\\[]
          [-+\\]\\[!=\\#$:%*@.\\p{L}0-9]*)? # formula has to start with letter, maybe followed by rest of formula
      (?:[\\p{L}]+[\\p{L}0-9\\]]*|[a-zA-Z]+))  # formulas has to end with letter or letter followed by number
      (?<parenthesisCharge>[+-]\\d*)?      # optional charge state followed by )
      [)]                       # ends with )
      )                         # end of non-capturing group""");


  private final List<IonPart> result = new ArrayList<>();
  private final String input;
  private final char[] chars;
  // formula or name starts after the number or after the (
  private int startFormula = -1;
  private int endFormula = -1;
  private int openParenthesis = 0;
  private int prefixMultiplier = 0;
  private Integer charge = null;
  private State state = State.PREFIX;
  private Format format = Format.SIMPLE;
  // current index
  private int index = 0;

  enum State {
    PREFIX, FIND_START_FORMULA, COLLECTING_FORMULA
  }

  enum Format {
    SIMPLE, FULL_PARANTHESIS
  }

  /**
   * Parse directly
   *
   * @param input
   */
  private IonPartParser(@NotNull String input) {
    // never remove whitespace because it is allowed
    // needs to handle it in the correct spots
    this.input = input;
    chars = input.toCharArray();
    processCharAtIndex();
  }

  private void processNextChar() {
    index++;
    processCharAtIndex();
  }

  private void processCharAtIndex() {
    if (index >= input.length()) {
      // found end of string either ends with exception if open parathesis or finish the last ion part
      if (state == State.COLLECTING_FORMULA) {
        createPart(index);
      }
      return;
    }
    if (Character.isWhitespace(charAt(index))) {
      processNextChar();
      return;
    }

    // always starts with multiplier
    if (state == State.PREFIX) {
      parsePrefixMultiplier();
      return;
    }

    final char c = charAt(index);
    // openParathesis is >0 for full ion part notation which always starts with ( after the multiplier
    if ((state == State.FIND_START_FORMULA || openParenthesis > 0)) {
      if (c == '(') {
        openParenthesis++;
        format = Format.FULL_PARANTHESIS;

        if (openParenthesis == 2 && state == State.FIND_START_FORMULA) {
          // formula started with ( after an opening (
          state = State.COLLECTING_FORMULA;
        }
        processNextChar();
        return;
      } else if (c == ')') {
        openParenthesis--;
        if (openParenthesis == 0) {
          // end of formula found
          createPart(index);
          return;
        }
        if (openParenthesis < 0) {
          throw new IonPartParsingException(input, index,
              "Cannot have closing parentheses ')' without opening one before. The ion part requires a format like +(Ca(OH-)2) for entries with +- in their name.");
        }
      }
    }

    // find start of formula
    if (state == State.FIND_START_FORMULA) {
      // this is automatically the current index as we captured the multiplier and optional ( before
      startFormula = index;
      state = State.COLLECTING_FORMULA;
      processNextChar();
      return;
    } else if (state == State.COLLECTING_FORMULA && openParenthesis == 0) {
      // simple format end formula with a + or - or end of string - has no open (
      if (StringUtils.isSign(c)) {
        createPart(index);
        return;
      }
    }

    processNextChar();
  }


  private char charAt(int index) {
    return chars[index];
  }

  private void createPart(int index) {
    if (prefixMultiplier == 0) {
      return; // finished
    }

    if (openParenthesis > 0) {
      throw new IonPartParsingException(input, index,
          "Input is missing %d closing parentheses ')'. The ion part requires a format like +(Ca(OH-)2) for entries with +- in their name.".formatted(
              openParenthesis));
    }

    // index might be out of bounds == input.length()
    // find charge
    findCharge();
    if (endFormula == -1) {
      endFormula = index;
    }
    if (startFormula == -1 || endFormula == -1) {
      throw new IonPartParsingException(input, index,
          "Cannot parse empty formula (start %d - end %d).".formatted(startFormula, endFormula));
    }
    if (endFormula - startFormula <= 0) {
      throw new IonPartParsingException(input, index,
          "Cannot parse empty formula (start %d - end %d).".formatted(startFormula, endFormula));
    }

    final String formula = input.substring(startFormula, endFormula).trim();
    if (StringUtils.isBlank(formula)) {
      throw new IonPartParsingException(input, startFormula, "Cannot parse empty formula.");
    }
    final IonPart part = IonParts.findPartByNameOrFormula(formula, prefixMultiplier, charge);
    result.add(part);

    reset();
    // index is at last index of current part
    processNextChar();
  }

  /**
   * Charge is always optional - sets the end of formula and advances the index to the end of the
   * current part last char.
   */
  private void findCharge() {
    switch (format) {
      case SIMPLE -> {
        findChargeSimpleFormat();
      }
      case FULL_PARANTHESIS -> {
        // charge
        findChargeFullFormat();
      }
    }
  }

  private void findChargeSimpleFormat() {
    endFormula = index;

    // might have no charge
    if (isEndOfInput(index)) {
      return;
    }

    // current index is the sign +- which may be the charge or the multiplier of the next part
    if (!isSign(charAt(index))) {
      return;
    }

    // regular format is number after sign like +2
    int digitsAfter = 0;
    int digitsBefore = 0;
    boolean allowFlippedChargeNotation = false;

    for (int i = index + 1; i < input.length(); i++) {
      char c = charAt(i);
      if (isDigit(c)) {
        digitsAfter++;
      } else {
        // other char check that there is another sign right after (allow whitespace between) or end
        // otherwise this first sign was multiplier of next part
        if (!hasSignCharNextOrEndOfString(i)) {
          this.index--; // have to add the index to new part as start
          return;
        }
        break;
      }
    }
    for (int i = index - 1; i >= 0; i--) {
      final char c = charAt(i);
      if (isDigit(c)) {
        digitsBefore++;
      } else if (c == ']' || c == ')') {
        allowFlippedChargeNotation = true;
        break;
      } else {
        break;
      }
    }

    Integer charge = null;
    if (allowFlippedChargeNotation) {
      if (digitsAfter > 0 && digitsBefore > 0) {
        throw new IonPartParsingException(input, index,
            "Cannot have both number before and after charge sign");
      } else if (digitsBefore > 0) {
        endFormula = index - digitsBefore;
        charge = IonTypeParser.parseChargeOrElse(input.substring(endFormula, index + 1), null);

      }
    }
    if (charge == null) {
      charge = StringUtils.parseSignAndIntegerOrElse(
          input.substring(index, index + digitsAfter + 1), null);
      this.index += digitsAfter;
    }
    if (charge == null) {
      throw new IonPartParsingException(input, index,
          "Error parsing charge in simple part notation. Requires charge either in () like +(Fe+2) or without +Fe+2");
    }
    this.charge = charge;
  }

  /**
   * Format is +(Fe+2) or +([Fe]2+) or +([Fe]+2). Index is currently at the closing )
   */
  private void findChargeFullFormat() {
    final int end = this.index;
    endFormula = end;
    int index = this.index - 1;

    boolean hasSign = false;
    int digits = 0;
    boolean flippedNotation = false;
    boolean flippedNotationAllowed = false;

    for (; index >= 0; index--) {
      final char c = charAt(index);
      if (isSign(c)) {
        if (hasSign) {
          // cannot have double sign this may be the end of the name like Glu- as a name and + as charge after
          index++;
          break;
        }
        hasSign = true;
        if (digits > 0) {
          break; // regular notation finished
        }
      } else if (isDigit(c)) {
        digits++;
        if (hasSign) {
          flippedNotation = true;
        }
      } else if (c == ']') {
        flippedNotationAllowed = true;
        index++;
        break;
      } else {
        // other char is not allowed
        index++;
        break;
      }
    }

    if (!hasSign) {
      return; // no charge
    }

    if (flippedNotation) {
      if (!flippedNotationAllowed) {
        // no ] found but 2+ found so the 2 actually belongs to the name and the + is the charge
        index += digits;
      }
      Integer charge = IonTypeParser.parseChargeOrElse(input.substring(index, end), null);
      if (charge == null) {
        throw new IonPartParsingException(input, index,
            "Error parsing charge in full ion part notation.");
      }
      this.charge = charge;
    } else {
      // regular charge
      Integer charge = StringUtils.parseSignAndIntegerOrElse(input.substring(index, end), null);
      if (charge == null) {
        throw new IonPartParsingException(input, index,
            "Error parsing charge in full ion part notation regular.");
      }
      this.charge = charge;
    }
    endFormula = index;
  }

  private boolean isEndOfInput(int index) {
    if (index >= input.length()) {
      return true;
    }
    for (int i = index; i < input.length(); i++) {
      if (!Character.isWhitespace(charAt(i))) {
        return false; // only allow white space at end
      }
    }
    return true;
  }

  private boolean hasSignCharNextOrEndOfString(int index) {
    for (int i = index; i < input.length(); i++) {
      final char c = charAt(i);
      if (isSign(c)) {
        return true;
      } else if (!Character.isWhitespace(c)) {
        // skip whitespace as it is allowed
        return false; // false on anything else than whitespace or
      }
    }
    return true;
  }

  private void reset() {
    state = State.PREFIX;
    format = Format.SIMPLE;
    prefixMultiplier = 0;
    openParenthesis = 0;
    startFormula = -1;
    endFormula = -1;
    charge = null;
  }

  private void parsePrefixMultiplier() {
    // requires + or -
    if (!StringUtils.isSign(charAt(index))) {
      throw new IonPartParsingException(input, index,
          "Expecting a + or - sign to start a new ion part.");
    }

    // next optional number
    int end = index + 1;
    while (end < input.length() && StringUtils.isDigit(charAt(end))) {
      end++;
    }

    //noinspection DataFlowIssue
    prefixMultiplier = StringUtils.parseSignAndIntegerOrElse(input.substring(index, end), 0);
    // first non digit + - sign char
    index = end;
    state = State.FIND_START_FORMULA;
    processCharAtIndex();
  }


  public List<IonPart> getResult() {
    return result;
  }

  /// Parses multiple [IonPart]. Formats:
  /// - simple: +3Fe+2    (doubly charged Fe; restriction: no +- in name)
  /// - full:   +3(Fe+2) or +(Ca(OH-)2) (required format when name contains additional +- symbols)
  ///
  /// @param input
  /// @return
  public static @Nullable IonPart parse(@NotNull String input) {
    final List<IonPart> parts = parseMultiple(input);
    if (parts.size() != 1) {
      throw new IonPartParsingException(input, 0,
          "Parsing resulted in %d parts but 1 was expected.".formatted(parts.size()));
    }
    return parts.getFirst();
  }

  public static @Nullable IonPart parsePattern(final @NotNull String part) {
    Matcher matcher = PART_PATTERN.matcher(StringUtils.removeAllWhiteSpace(part));
    while (matcher.find()) {
      IonPart ion = fromMatcher(matcher);
      if (ion != null) {
        return ion;
      }
    }
    return null;
  }

  @NotNull
  public static List<IonPart> parseMultiple(@NotNull final String input) {
    return new IonPartParser(input).getResult();
  }

  public static List<IonPart> parseMultiplePattern(final String input) {
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
    String chargeStr = matcher.group("charge");
    String formula = matcher.group("formula");
    if (chargeStr == null) {
      chargeStr = matcher.group("parenthesisCharge");
    }
    if (formula == null) {
      formula = matcher.group("parenthesisFormula");
    }

    if (StringUtils.isBlank(formula)) {
      return null;
    }
    final int count = parseSignAndIntegerOrElse(countStr, true, 1);
    Integer charge = parseSignAndIntegerOrElse(chargeStr, true, null);
    var ionPart = IonParts.findPartByNameOrFormula(formula, count, charge);

    return ionPart;
  }


}
