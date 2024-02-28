/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.gui.preferences;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public enum ImageNormalization {
  NO_NORMALIZATION, TIC_AVG_NORMALIZATION/*, ROOT_MEAN_SQUARE*/;

  public <S extends Scan, T extends IonTimeSeries<S>> T normalize(T series,
      List<S> allSelectedScans, @Nullable final MemoryMapStorage storage) {
    return switch (this) {
      case NO_NORMALIZATION -> series;
      case TIC_AVG_NORMALIZATION ->
          IonTimeSeriesUtils.normalizeToAvgTic(series, allSelectedScans, storage);
//      case ROOT_MEAN_SQUARE -> null;
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case NO_NORMALIZATION -> "No normalization";
      case TIC_AVG_NORMALIZATION -> "Average TIC normalization";
//      case ROOT_MEAN_SQUARE -> "Root mean square";
    };
  }
}
