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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package util;

import io.github.mzmine.modules.dataprocessing.filter_featurefilter.gaussian_fitter.FitQuality;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.gaussian_fitter.PeakFitterUtils;
import org.junit.jupiter.api.Test;

public class FitterTest {

  @Test
  void testGaussianFitter() {
    double[] rts = {3.660087824, 3.667550325, 3.674948215, 3.683954477, 3.691175222, 3.698764801,
        3.706291914, 3.713668823, 3.721143723, 3.728068829, 3.735060453, 3.742645741, 3.750104189,
        3.757412195, 3.767095804, 3.773991346, 3.781339407, 3.789007902, 3.796607971, 3.803955793,
        3.811603785, 3.818520308, 3.826234818, 3.833868027, 3.841547251, 3.850697279, 3.858409643,
        3.865926266, 3.873728514, 3.881476164, 3.889057398, 3.896905184, 3.904709339, 3.912369728,
        3.920167685, 3.930132151, 3.937448978, 3.944682121, 3.952284098, 3.959963322, 3.967581987,
        3.975161076, 3.982796431};

    double[] intensities = {813894.9375, 1566764.271, 2299633.515, 3475646.274, 6895500.881,
        1.18E+07, 2.28E+07, 5.19E+07, 1.10E+08, 1.89E+08, 2.60E+08, 2.91E+08, 2.59E+08, 1.89E+08,
        1.20E+08, 8.11E+07, 7.52E+07, 8.52E+07, 8.61E+07, 7.41E+07, 5.39E+07, 3.44E+07, 2.22E+07,
        1.45E+07, 1.08E+07, 8001745.548, 6183765.155, 4958968.786, 4280754.667, 3697009.167,
        3255895.036, 2946840.238, 2624884.976, 2371787.911, 2211696.577, 1900029.339, 1446694.143,
        1370919.417, 1338367.929, 1384401.946, 1394644.893, 1313078.786, 1118257.006};

    FitQuality gf = PeakFitterUtils.fitPeakModels(rts, intensities);
  }
}
