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

package io.github.mzmine.datamodel.features.types.analysis;

import io.github.mzmine.datamodel.features.types.abstr.BooleanType;
import org.jetbrains.annotations.NotNull;

/**
 * Flag if signal is likely an in source fragment. For now, if the m/z of the row corresponds to a
 * (detected) signal (excluding the precursor ion) found in any of the MS2 scans corresponding to
 * all (detected) signals found in the MS1 scan, it is flagged as a likely in source fragment. This
 * method detects a lot of false positives. It extends the {@link BooleanType} class. This value is
 * typically used in the context of in source fragments analysis.
 */
public class IsLikelyISFragmentType extends BooleanType {

  @Override
  public @NotNull String getUniqueID() {
    return "is_likely_isf";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Likely in-source fragment";
  }
}