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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.compoundannotations;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class SchymanskiLevel implements Comparable<SchymanskiLevel> {

  public static final SchymanskiLevel LEVEL_1 = new SchymanskiLevel(1);
  public static final SchymanskiLevel LEVEL_2a = new SchymanskiLevel(2, "a");
  public static final SchymanskiLevel LEVEL_2b = new SchymanskiLevel(2, "b");
  public static final SchymanskiLevel LEVEL_3 = new SchymanskiLevel(3);
  public static final SchymanskiLevel LEVEL_4 = new SchymanskiLevel(4);
  public static final SchymanskiLevel LEVEL_5 = new SchymanskiLevel(5);
  private final int numberLevel;
  private final @NotNull String letterLevel;

  private SchymanskiLevel(int numberLevel, @NotNull String letterLevel) {
    this.numberLevel = numberLevel;
    this.letterLevel = letterLevel;
  }

  private SchymanskiLevel(int numberLevel) {
    this(numberLevel, "");
  }

  @Override
  public int compareTo(@NotNull SchymanskiLevel o) {
    final int cmpNumber = Integer.compare(numberLevel, o.numberLevel);
    if (cmpNumber != 0) {
      return cmpNumber;
    }

    return letterLevel.toLowerCase().compareTo(o.letterLevel.toLowerCase());
  }

  public int numberLevel() {
    return numberLevel;
  }

  public @NotNull String letterLevel() {
    return letterLevel;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (SchymanskiLevel) obj;
    return this.numberLevel == that.numberLevel && Objects.equals(this.letterLevel,
        that.letterLevel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(numberLevel, letterLevel);
  }

  @Override
  public String toString() {
    return "Schymanski level " + numberLevel + letterLevel;
  }
}
