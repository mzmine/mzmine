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
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.RangeUtils;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public record IsolationWindow(@Nullable Range<Double> mzIsolation,
                              @Nullable Range<Float> mobilityIsolation) {

  private static final NumberFormats formats = ConfigService.getExportFormats();

  public IsolationWindow(IonMobilityMsMsInfo info) {
    this(info.getIsolationWindow(), info.getMobilityRange());
  }

  public boolean containsMobility(MobilityScan scan) {
    if (mobilityIsolation != null && !mobilityIsolation.contains((float) scan.getMobility())) {
      return false;
    }
    return true;
  }

  public boolean contains(Feature f) {
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

  /**
   * @return The lowest overlap (between 0 and 1) of the two isolation windows (mz and mobility).
   */
  double overlap(IsolationWindow other) {

    if (Objects.equals(mzIsolation, other.mzIsolation()) && Objects.equals(mobilityIsolation,
        other.mobilityIsolation())) {
      return 1d;
    }

    double overlap = 0d;
    if (mobilityIsolation == null && other.mobilityIsolation() == null) {
      overlap = 1d;
    } else if (mobilityIsolation != null && other.mobilityIsolation() != null) {
      if (mobilityIsolation.isConnected(other.mobilityIsolation())) {
        final Range<Float> intersection = mobilityIsolation.intersection(other.mobilityIsolation());
        overlap = RangeUtils.rangeLength(intersection) / RangeUtils.rangeLength(
            mobilityIsolation.span(other.mobilityIsolation()));
      } else {
        overlap = 0d;
      }
    } else {
      overlap = 0d;
    }

    if (mzIsolation == null && other.mzIsolation() == null) {
      overlap = Math.min(overlap, 1d);
    } else if (mzIsolation != null && other.mzIsolation() != null) {
      if (mzIsolation.isConnected(other.mzIsolation())) {
        final Range<Double> intersection = mzIsolation.intersection(other.mzIsolation());
        overlap = Math.min(overlap, RangeUtils.rangeLength(intersection) / RangeUtils.rangeLength(
            mzIsolation.span(other.mzIsolation())));
      } else {
        overlap = 0d;
      }
    } else {
      // one has mz isolation, the other does not. -> no overlap.
      overlap = 0d;
    }

    return overlap;
  }

  public IsolationWindow merge(IsolationWindow other) {
    Range<Double> mz = null;
    if (mzIsolation != null && other.mzIsolation() != null) {
      mz = mzIsolation.span(other.mzIsolation());
    }
    Range<Float> mobility = null;
    if (mobilityIsolation != null && other.mobilityIsolation() != null) {
      mobility = mobilityIsolation.span(other.mobilityIsolation());
    }
    return new IsolationWindow(mz, mobility);
  }

  @Override
  public String toString() {
    return "IsolationWindow{" + "mz=" + formats.mz(mzIsolation) + ", mobility=" + formats.mobility(
        mobilityIsolation) + '}';
  }
}
