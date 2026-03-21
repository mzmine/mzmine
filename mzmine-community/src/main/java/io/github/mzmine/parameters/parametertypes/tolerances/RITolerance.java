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

package io.github.mzmine.parameters.parametertypes.tolerances;

import com.google.common.collect.Range;
import io.github.mzmine.util.RIColumn;
import io.github.mzmine.util.RIRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * RITolerance allows specifying retention index tolerance for comparing two compounds It is an
 * absolute unitless tolerance based on a specific RIColumn, which specifies the stationary phase
 * within the column
 */
public class RITolerance {

  private final float tolerance;
  private final RIColumn column;
  private final boolean matchOnNull;

  public RITolerance(final float riTolerance, @NotNull RIColumn type, final boolean matchOnNull) {
    this.tolerance = riTolerance;
    this.column = type;
    this.matchOnNull = matchOnNull;
  }

  public RITolerance withMatchOnNull(boolean matchOnNull) {
    return new RITolerance(tolerance, column, matchOnNull);
  }

  public float getTolerance() {
    return tolerance;
  }

  /**
   *
   * @return The tolerance around the given RI. Not that if riValue is 0 (e.g. not set in a row),
   * the return will be [-tolerance..+tolerance]
   */
  @NotNull
  public Range<Float> getToleranceRange(final float riValue) {
    return Range.closed(riValue - tolerance, riValue + tolerance);
  }

  /**
   *
   * @return Null if no valid (= selected or default) tolerance in this RI record is found, the
   * tolerance otherwise.
   */
  @Nullable
  public Range<Float> getToleranceRange(@Nullable final RIRecord riRecord) {
    if (riRecord == null) {
      return null;
    }
    final Float ri = riRecord.getRI(column);
    if (ri == null) {
      return null;
    }
    return getToleranceRange(ri);
  }

  /**
   *
   * @return Null if the given libRi is null or does not contain the correct tolerance.
   */
  @Nullable
  public Float getRiDifference(final float ri, @Nullable RIRecord libRi) {
    if (libRi == null || libRi.getRI(column) == null) {
      return null;
    }
    return ri - libRi.getRI(column);
  }

  /**
   * @return True if both values are within tolerance. True if libRI is null and
   * {@link #isMatchOnNull()} is true. false otherwise.
   */
  public boolean checkWithinTolerance(float ri, @Nullable RIRecord libRI) {
    if (libRI == null || libRI.getRI(column) == null) {
      return validInput(libRI);
    }
    final Range<Float> toleranceRange = getToleranceRange(libRI.getRI(column));
    return toleranceRange.contains(ri);
  }

  public boolean checkWithinTolerance(final float ri1, final float ri2) {
    return getToleranceRange(ri1).contains(ri2);
  }

  public boolean isMatchOnNull() {
    return matchOnNull;
  }

  private boolean validInput(@Nullable RIRecord record) {
    if (record == null) {
      return isMatchOnNull();
    }
    if (record.getRI(column) == null) {
      return isMatchOnNull();
    }

    return true;
  }

  public RIColumn getRIType() {
    return column;
  }

  @Override
  public String toString() {
    return tolerance + ", " + column.toString();
  }

}
