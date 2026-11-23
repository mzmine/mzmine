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

package io.github.mzmine.util;

import java.util.Collection;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides a short, user-facing description, e.g., of an option in a combo box. Used to explain
 * each option and when to use it in tooltips and help texts.
 */
public interface DescriptionProvider {

  /**
   * Bullet used to list options in {@link #describeOptions(Collection)}
   */
  String OPTION_BULLET = "• ";

  /**
   * @return a short description of this object and when to use it
   */
  @NotNull String getDescription();

  /**
   * Lists all options that implement {@link DescriptionProvider} as one bullet point per option in
   * the format "name: description". The name is the {@link Object#toString()} value of each option.
   * Options without a description are skipped.
   *
   * @param options the options to describe, e.g., all items of a combo box
   * @return the descriptions of the options separated by new lines or an empty string if no option
   * provides a description
   */
  static @NotNull String describeOptions(@NotNull Collection<?> options) {
    return options.stream().filter(DescriptionProvider.class::isInstance).map(
        option -> OPTION_BULLET + option + ": " + ((DescriptionProvider) option).getDescription()
            .strip()).collect(Collectors.joining("\n"));
  }

  /**
   * Combines a general description, e.g., of a parameter, with the descriptions of all options that
   * implement {@link DescriptionProvider}, see {@link #describeOptions(Collection)}.
   *
   * @param description general description shown first, may be null or blank
   * @param options     the options to describe
   * @return the combined description or an empty string if there is nothing to describe
   */
  static @NotNull String describeWithOptions(@Nullable String description,
      @NotNull Collection<?> options) {
    final String optionsText = describeOptions(options);
    if (StringUtils.isBlank(description)) {
      return optionsText;
    }
    if (optionsText.isEmpty()) {
      return description.strip();
    }
    return description.strip() + "\n\n" + optionsText;
  }
}
