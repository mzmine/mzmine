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

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Column separator of tabular text files, see {@link FieldSeparatorParameter}.
 */
public enum FieldSeparatorOption implements UniqueIdSupplier {

  /**
   * Only for reading files, the separator is determined by
   * {@link io.github.mzmine.util.CSVParsingUtils#autoDetermineSeparator(java.io.File)}.
   */
  AUTO,
  COMMA, SEMICOLON, TAB, SPACE,
  /**
   * Any other separator, defined by the embedded text field.
   */
  CUSTOM;

  /**
   * All options, including {@link #AUTO}. For reading files.
   */
  public static final FieldSeparatorOption[] READ_OPTIONS = values();

  /**
   * All options but {@link #AUTO}, which cannot be used to write a file.
   */
  public static final FieldSeparatorOption[] WRITE_OPTIONS = new FieldSeparatorOption[]{COMMA,
      SEMICOLON, TAB, SPACE, CUSTOM};

  @Override
  public String toString() {
    return switch (this) {
      case AUTO -> "Auto detect";
      case COMMA -> "Comma  ,";
      case SEMICOLON -> "Semicolon  ;";
      case TAB -> "Tab";
      case SPACE -> "Space";
      case CUSTOM -> "Custom";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case AUTO -> "auto";
      case COMMA -> "comma";
      case SEMICOLON -> "semicolon";
      case TAB -> "tab";
      case SPACE -> "space";
      case CUSTOM -> "custom";
    };
  }

  /**
   * @return the separator of this option or null for {@link #AUTO} and {@link #CUSTOM}, which have
   * no fixed separator
   */
  public @Nullable String getSeparator() {
    return switch (this) {
      case COMMA -> ",";
      case SEMICOLON -> ";";
      case TAB -> "\t";
      case SPACE -> " ";
      case AUTO, CUSTOM -> null;
    };
  }

  /**
   * Matches the values that were stored by the old string parameters and the values a user may type
   * into the custom field.
   *
   * @return true if the given separator is exactly this option
   */
  public boolean matches(@NotNull final String separator) {
    return switch (this) {
      case AUTO -> "auto".equalsIgnoreCase(separator);
      case COMMA -> ",".equals(separator);
      case SEMICOLON -> ";".equals(separator);
      // "\\t" is the escaped tab that users had to type into the old text field
      case TAB -> "\t".equals(separator) || "\\t".equals(separator) || "tab".equalsIgnoreCase(
          separator);
      case SPACE -> " ".equals(separator) || "space".equalsIgnoreCase(separator);
      case CUSTOM -> false; // only used as fallback
    };
  }
}
