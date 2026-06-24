/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Objects;

public final class SimpleStoredMobilograms implements StoredMobilograms {

  private final List<IonMobilitySeries> storedMobilograms;
  private final MemorySegment storedMzValues;
  private final MemorySegment storedIntensityValues;

  public SimpleStoredMobilograms(List<? extends IonMobilitySeries> storedMobilograms, MemorySegment storedMzValues,
      MemorySegment storedIntensityValues) {
    this.storedMobilograms = (List<IonMobilitySeries>) storedMobilograms;
    this.storedMzValues = storedMzValues;
    this.storedIntensityValues = storedIntensityValues;
  }

  @Override
  public IonMobilitySeries mobilogram(int index) {
    return storedMobilograms().get(index);
  }

  @Override
  public List<IonMobilitySeries> storedMobilograms() {
    return storedMobilograms;
  }

  @Override
  public MemorySegment storedMzValues() {
    return storedMzValues;
  }

  @Override
  public MemorySegment storedIntensityValues() {
    return storedIntensityValues;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (SimpleStoredMobilograms) obj;
    return Objects.equals(this.storedMobilograms, that.storedMobilograms) && Objects.equals(this.storedMzValues,
        that.storedMzValues) && Objects.equals(this.storedIntensityValues, that.storedIntensityValues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(storedMobilograms, storedMzValues, storedIntensityValues);
  }

  @Override
  public String toString() {
    return "SimpleStoredMobilograms[" + "storedMobilograms=" + storedMobilograms + ", "
        + "storedMzValues=" + storedMzValues + ", " + "storedIntensityValues="
        + storedIntensityValues + ']';
  }

}
