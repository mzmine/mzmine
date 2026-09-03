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

package io.github.mzmine.parameters.parametertypes.tolerances;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

/**
 * The unit of a {@link SingleMzToleranceParameter}: an m/z tolerance is either absolute or
 * relative, never both.
 * <p>
 * The value is still carried in an {@link MZTolerance}, which takes the maximum of an absolute and
 * a relative term. A single unit is expressed by leaving the other term at zero, so that the
 * maximum is the one term that was set. {@link #of(MZTolerance)} reads the unit back out of such a
 * tolerance.
 */
public enum MzToleranceUnit implements UniqueIdSupplier {

  /**
   * An absolute tolerance in m/z units, which is what the mass spectrometry community calls Da for
   * a tolerance even though m/z and Da only coincide at charge 1.
   */
  DA,

  /**
   * A relative tolerance in parts per million of the m/z.
   */
  PPM;

  /**
   * @return the unit a tolerance is expressed in: relative if it has a ppm term, absolute
   * otherwise. A tolerance with both terms set is read as relative, which is the term
   * {@link MZTolerance} lets win above the crossover m/z.
   */
  public static @NotNull MzToleranceUnit of(@NotNull final MZTolerance tolerance) {
    return tolerance.getPpmTolerance() > 0 ? PPM : DA;
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case DA -> "da";
      case PPM -> "ppm";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case DA -> "Da";
      case PPM -> "ppm";
    };
  }

  /**
   * @return the number format the user preferences define for this unit. Shared and therefore only
   * to be read, never reconfigured.
   */
  public @NotNull NumberFormat format() {
    return switch (this) {
      case DA -> MZmineCore.getConfiguration().getMZFormat();
      case PPM -> MZmineCore.getConfiguration().getPPMFormat();
    };
  }

  /**
   * @return the term of the given tolerance that is expressed in this unit.
   */
  public double valueOf(@NotNull final MZTolerance tolerance) {
    return switch (this) {
      case DA -> tolerance.getMzTolerance();
      case PPM -> tolerance.getPpmTolerance();
    };
  }

  /**
   * @return a tolerance of the given size in this unit, with the other term left at zero.
   */
  public @NotNull MZTolerance toTolerance(final double value) {
    return switch (this) {
      case DA -> new MZTolerance(value, 0);
      case PPM -> new MZTolerance(0, value);
    };
  }
}
