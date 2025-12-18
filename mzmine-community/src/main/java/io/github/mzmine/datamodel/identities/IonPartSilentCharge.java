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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used when parsing says there is a charge but it is unknown which part holds the charge
 */
record IonPartSilentCharge(int count) implements IonPart {

  /**
   * @return empty string for silent ion
   */
  @Override
  public @NotNull String name() {
    return "";
  }

  @Override
  public IonPart withSingleCharge(Integer singleCharge) {
    throw new UnsupportedOperationException(
        "Cannot change charge of silent single charge part. Rather set the count");
  }

  @Override
  public String toString(IonPartStringFlavor flavor) {
    return toString();
  }

  @Override
  public String toString() {
    return "";
  }

  @Override
  public @Nullable String singleFormula() {
    return null;
  }

  @Override
  public boolean isSilentCharge() {
    return true;
  }

  @Override
  public boolean isUndefinedMass() {
    return true;
  }

  @Override
  public double absSingleMass() {
    return 0d;
  }

  @Override
  public int singleCharge() {
    return 1;
  }

  @Override
  public int count() {
    return count;
  }

  @Override
  public IonPart withCount(int count) {
    if (count == this.count) {
      return this;
    }
    return new IonPartSilentCharge(count);
  }
}
