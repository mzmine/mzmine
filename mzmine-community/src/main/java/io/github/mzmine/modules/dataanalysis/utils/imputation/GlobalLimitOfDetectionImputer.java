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

package io.github.mzmine.modules.dataanalysis.utils.imputation;

import io.github.mzmine.datamodel.statistics.DataTable;
import io.github.mzmine.datamodel.statistics.DataTableUtils;

public class GlobalLimitOfDetectionImputer implements ImputationFunction {

  public static final double DEVISOR = 5;

  public GlobalLimitOfDetectionImputer() {
  }


  @Override
  public <T extends DataTable> T processInPlace(T data) {
    // calculate minimum
    final double globalMinimum = DataTableUtils.getMinimum(data, true).orElse(1d) / DEVISOR;

    // do not use data array directly as it is not given that all tables.featureArray will reflect the changes
    for (int featureIndex = 0; featureIndex < data.getNumberOfFeatures(); featureIndex++) {
      DataTableUtils.replaceNaN(data, featureIndex, globalMinimum, true);
    }

    return data;
  }

}
