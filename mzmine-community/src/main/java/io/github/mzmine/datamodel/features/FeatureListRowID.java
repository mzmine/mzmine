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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import org.jetbrains.annotations.NotNull;

/**
 * Could in the future replace the ID in the CompoundId type etc for map keys
 *
 * @param type compound or classical feature
 * @param id   the actual id
 */
public record FeatureListRowID(@NotNull FeatureListMode type, int id) {

  public static FeatureListRowID of(@NotNull FeatureListRow row) {
    if (row instanceof CompoundRow comp) {
      // use compound ID here (compound has no real getID() feature row id but delegates to preferred
      return new FeatureListRowID(FeatureListMode.COMPOUND_ROW, comp.getCompoundId());
    } else {
      // use row ID
      return new FeatureListRowID(FeatureListMode.FEATURE_ROW, row.getID());
    }
  }

}
