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

package io.github.mzmine.modules.dataanalysis.significance.ttest;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.dataanalysis.significance.SignificanceTests;
import io.github.mzmine.util.StringUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public enum TTestSamplingConfig implements UniqueIdSupplier {

  UNPAIRED("unpaired",
      "Used for tests of unrelated groups, e.g., separate patients for control and treatment group."), //

  PAIRED("paired",
      "Used for tests of related groups, e.g., same patient before and after treatment."); //

  private static final Logger logger = Logger.getLogger(TTestSamplingConfig.class.getName());
  final String name;
  final String description;

  TTestSamplingConfig(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Parses the value and handles any exceptions, null and empty values.
   *
   * @return the parsed value or defaultValue in any exceptional case or for empty value
   */
  public static TTestSamplingConfig parseOrElse(final String value,
      final TTestSamplingConfig defaultValue) {
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }
    try {
      return switch (value.toLowerCase()) {
        case "unpaired" -> TTestSamplingConfig.UNPAIRED;
        case "paired" -> TTestSamplingConfig.PAIRED;
        default -> {
          logger.log(Level.WARNING, "Could not parse %s TTestSamplingConfig".formatted(value));
          yield defaultValue;
        }
      };
    } catch (Exception exception) {
      logger.log(Level.WARNING, "Could not parse %s TTestSamplingConfig".formatted(value));
    }
    return defaultValue;
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case UNPAIRED -> "unpaired";
      case PAIRED -> "paired";
    };
  }

  public @NotNull SignificanceTests toSignificanceTest() {
    return switch (this) {
      case PAIRED -> SignificanceTests.PAIRED_T_TEST;
      // the initial code already used the more robust Welch's t-test for unpaired
      case UNPAIRED -> SignificanceTests.WELCHS_T_TEST;
    };
  }
}
