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

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.Scan;

/**
 * Used in the {@link SpectraPlot} to determine if a continuous or centroid renderer is used.
 *
 * @author SteffenHeu https://github.com/SteffenHeu / steffen.heuckeroth@uni-muenster.de
 */
public enum SpectrumPlotType {
  AUTO, CENTROID, PROFILE;

  public static SpectrumPlotType fromScan(Scan scan) {
    if (scan.getSpectrumType() == MassSpectrumType.CENTROIDED) {
      return SpectrumPlotType.CENTROID;
    } else {
      return SpectrumPlotType.PROFILE;
    }
  }

  public static SpectrumPlotType fromMassSpectrumType(MassSpectrumType type) {
    if (type == MassSpectrumType.CENTROIDED) {
      return SpectrumPlotType.CENTROID;
    } else {
      return SpectrumPlotType.PROFILE;
    }
  }
}
