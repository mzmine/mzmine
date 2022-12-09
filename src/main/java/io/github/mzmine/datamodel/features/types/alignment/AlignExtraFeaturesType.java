/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.alignment;

import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import org.jetbrains.annotations.NotNull;

/**
 * This type describes the number of features that fall within the alignment ranges (mz, RT,
 * mobility) but were either not aligned or were extra features (2 features in a sample match the
 * ranges, this means 1 feature is extra).
 * <p>
 * This is calculated per sample level and the sum of all is reflected to the row. RowBinding is NOT
 * used because some sample might not have a feature while there were feature sin the range (failing
 * other filters)
 */
public class AlignExtraFeaturesType extends IntegerType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "align_extra_features";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Σ Extra features";
  }

}
