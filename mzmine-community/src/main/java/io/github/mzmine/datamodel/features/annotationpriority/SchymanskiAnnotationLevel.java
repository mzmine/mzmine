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

package io.github.mzmine.datamodel.features.annotationpriority;

import org.jetbrains.annotations.NotNull;

/**
 * https://doi.org/10.1021/es5002105
 */
public enum SchymanskiAnnotationLevel {

  LEVEL_1(1), LEVEL_2a(2, "a"), LEVEL_2b(2, "b"), LEVEL_3(3), LEVEL_4(4), LEVEL_5(5);

  private static final String LABEL = "Schymanski et al. level";
  private final int numberLevel;
  private final @NotNull String letterLevel;

  SchymanskiAnnotationLevel(int numberLevel, @NotNull String letterLevel) {
    this.numberLevel = numberLevel;
    this.letterLevel = letterLevel;
  }

  SchymanskiAnnotationLevel(int numberLevel) {
    this(numberLevel, "");
  }

  public int numberLevel() {
    return numberLevel;
  }

  public @NotNull String letterLevel() {
    return letterLevel;
  }

  /**
   *
   * @return number + letter
   */
  public @NotNull String fullLevel() {
    return numberLevel + letterLevel;
  }

  @Override
  public String toString() {
    return LABEL + " " + numberLevel + letterLevel;
  }

  @NotNull
  public String getLabel() {
    return LABEL;
  }
}