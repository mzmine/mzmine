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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.types.numbers.abstr.PercentType;
import org.jetbrains.annotations.NotNull;

/**
 * Relative height may be added to the Feature or FeatureListRow to identify the significance of a
 * feature. Here, the highest intensity feature with a specific m/z is set to 100%, and for all
 * other rows/features with same m/z the relative height is calculated. This is important for
 * spectral library export to filter out minor features with relative intensity cutoff
 */
public class RelativeHeightType extends PercentType {

  @Override
  public @NotNull String getUniqueID() {
    return "relative_height_percent";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Rel. height";
  }

  // TODO decide for best row binding. MAX? or just calculate for the row initially
//  @NotNull
//  @Override
//  public List<RowBinding> createDefaultRowBindings() {
//    return List.of(new SimpleRowBinding(this, BindingsType.MAX));
//  }
}
