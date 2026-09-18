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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.util.CSVParsingUtils;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Value of a {@link FieldSeparatorParameter}.
 *
 * @param option          the selected separator
 * @param customSeparator only used with {@link FieldSeparatorOption#CUSTOM}
 */
public record FieldSeparator(@NotNull FieldSeparatorOption option,
                             @Nullable String customSeparator) implements
    ComboWithInputValue<FieldSeparatorOption, String> {

  public static final FieldSeparator AUTO = new FieldSeparator(FieldSeparatorOption.AUTO);
  public static final FieldSeparator COMMA = new FieldSeparator(FieldSeparatorOption.COMMA);
  public static final FieldSeparator SEMICOLON = new FieldSeparator(FieldSeparatorOption.SEMICOLON);
  public static final FieldSeparator TAB = new FieldSeparator(FieldSeparatorOption.TAB);

  public FieldSeparator {
    Objects.requireNonNull(option);
  }

  public FieldSeparator(@NotNull final FieldSeparatorOption option) {
    this(option, null);
  }

  /**
   * Maps a separator string to the matching option, or to
   * {@link FieldSeparatorOption#CUSTOM} if no option matches. Used to read the values of the old
   * string parameters and everything a user types into the custom field.
   */
  public static @NotNull FieldSeparator parse(@Nullable final String separator) {
    if (separator == null || separator.isEmpty()) {
      return AUTO;
    }
    for (FieldSeparatorOption option : FieldSeparatorOption.values()) {
      if (option.matches(separator)) {
        return new FieldSeparator(option);
      }
    }
    return new FieldSeparator(FieldSeparatorOption.CUSTOM, separator);
  }

  @Override
  public @NotNull FieldSeparatorOption getSelectedOption() {
    return option;
  }

  @Override
  public @Nullable String getEmbeddedValue() {
    return customSeparator;
  }

  public boolean isAuto() {
    return option == FieldSeparatorOption.AUTO;
  }

  /**
   * The separator as expected by {@link CSVParsingUtils#readData(java.io.File, String)}, which
   * detects the separator itself for {@link CSVParsingUtils#AUTO_SEPARATOR}. Use
   * {@link #separatorChar()} to write files, those need a real character.
   */
  public @NotNull String separator() {
    return switch (option) {
      case AUTO -> CSVParsingUtils.AUTO_SEPARATOR;
      // users are used to type the escaped tab into the old text field
      case CUSTOM -> "\\t".equals(customSeparator) ? "\t" : requireNonNullElse(customSeparator, "");
      case COMMA, SEMICOLON, TAB, SPACE -> Objects.requireNonNull(option.getSeparator());
    };
  }

  /**
   * @return the separator character for writing files
   * @throws IllegalStateException if this is {@link FieldSeparatorOption#AUTO} or an empty custom
   *                               separator, both are rejected by
   *                               {@link FieldSeparatorParameter#checkValue}
   */
  public char separatorChar() {
    final String separator = isAuto() ? "" : separator();
    if (separator.isEmpty()) {
      throw new IllegalStateException("No column separator defined (%s).".formatted(option));
    }
    return separator.charAt(0);
  }

  @Override
  public String toString() {
    return option == FieldSeparatorOption.CUSTOM ? option + " " + customSeparator
        : option.toString();
  }
}
