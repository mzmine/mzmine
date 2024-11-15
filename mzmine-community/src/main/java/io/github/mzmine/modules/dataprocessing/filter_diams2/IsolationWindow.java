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

package io.github.mzmine.modules.dataprocessing.filter_diams2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.features.Feature;
import org.jetbrains.annotations.Nullable;

public record IsolationWindow(@Nullable Range<Double> mzIsolation,
                              @Nullable Range<Float> mobilityIsolation) {

  boolean contains(MobilityScan scan) {
//    if (mzIsolation != null) {
//      if (!(scan.getMsMsInfo() instanceof MsMsInfo info)) {
//        return false;
//      }
//      if (!(info.getIsolationWindow() instanceof Range<Double> range)) {
//        return false;
//      }
//      if (!range.equals(mzIsolation)) {
//        return false;
//      }
//    }

    if (mobilityIsolation != null && !mobilityIsolation.contains((float) scan.getMobility())) {
      return false;
    }
    return true;
  }

  boolean contains(Feature f) {
    final Double mz = f.getMZ();
    final Float mobility = f.getMobility();

    if (mzIsolation != null && !mzIsolation.contains(mz)) {
      return false;
    }

    if (mobilityIsolation != null && (mobility != null && !mobilityIsolation.contains(mobility))) {
      return false;
    }
    return true;
  }
}
