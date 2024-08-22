/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.MathUtils;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Relative m/z difference in ppm (parts-per-million: 10^-6), e.g., used in {@link FormulaListType}
 * to describe the difference between the measured (accurate) and the calculated (exact) m/z
 */
public class MzPpmDifferenceType extends FloatType {

  public MzPpmDifferenceType() {
    super(new DecimalFormat("0.000"));
  }

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "mz_diff_ppm";
  }

  @Override
  public @NotNull String getHeaderString() {
    // Delta
    return "\u0394 m/z ppm";
  }

  @Override
  public NumberFormat getFormat() {
    try {
      return MZmineCore.getConfiguration().getGuiFormats().ppmFormat();
    } catch (NullPointerException e) {
      // only happens if types are used without initializing the MZmineCore
      return DEFAULT_FORMAT;
    }
  }

  @Override
  public NumberFormat getExportFormat() {
    try {
      return MZmineCore.getConfiguration().getExportFormats().ppmFormat();
    } catch (NullPointerException e) {
      // only happens if types are used without initializing the MZmineCore
      return DEFAULT_FORMAT;
    }
  }

  /**
   * @param exactMass    the calculated mass.
   * @param accurateMass the measured mass.
   * @return the ppm difference or null if either of the parameters is null.
   */
  public static @Nullable Double calculate(@Nullable Double exactMass,
      @Nullable Double accurateMass) {
    if (exactMass == null || accurateMass == null) {
      return null;
    }
    return MathUtils.getPpmDiff(exactMass, accurateMass);
  }
}
