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

import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.AsymmetricGaussianPeak;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.FitQuality;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.GaussianDoublePeak;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.GaussianPeak;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakFitterUtils;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakShapeClassification;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FitterTest {

  private static final Logger logger = Logger.getLogger(FitterTest.class.getName());

  @Test
  void testDoublePeak() {
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

    FitQuality fit = PeakFitterUtils.fitPeakModels(rts, intensities,
        List.of(new GaussianPeak(), new GaussianDoublePeak(), new AsymmetricGaussianPeak()));
    Assertions.assertNotNull(fit);
    Assertions.assertEquals(PeakShapeClassification.DOUBLE_GAUSSIAN, fit.peakShapeClassification());
    Assertions.assertEquals(0.9643966172323696, fit.fitScore(), 0.0000001);
  }

  @Test
  void testBadPeak() {
    double[] x = new double[]{8.495267868, 8.502846718, 8.511771202, 8.519396782, 8.526919365,
        8.534352303, 8.541919708, 8.549429893, 8.556567192, 8.564012527, 8.571388245, 8.578792572,
        8.586440086, 8.59390831, 8.601377487, 8.608750343, 8.616060257, 8.623598099};

    double[] y = new double[]{363934.1563, 534773.875, 495209.4688, 548776.875, 552176.375,
        543185.5625, 524146.0313, 641103.9375, 770880.5625, 564265.9375, 532109.4375, 543570.5625,
        603697.5625, 536073.8125, 580397.75, 421767.75, 456927, 495422.5313};

    final FitQuality fit = PeakFitterUtils.fitPeakModels(x, y, List.of(new GaussianPeak()));
    Assertions.assertNotNull(fit);
    Assertions.assertEquals(0.6896750547021087, fit.fitScore(), 0.0000001);
  }

  @Test
  void testTailingPeak() {
    double x[] = new double[]{8.436031342, 8.443263054, 8.450414658, 8.457859993, 8.464979172,
        8.472619057, 8.480257988, 8.48789978, 8.495381355, 8.502960205, 8.512058258, 8.519651413,
        8.527062416, 8.534116745, 8.541747093, 8.549307823, 8.556893349, 8.56427002, 8.571580887,
        8.578576088, 8.586263657, 8.593792915, 8.601468086, 8.60863018, 8.61608696, 8.623140335,
        8.630841255, 8.638524055, 8.64617157, 8.653250694, 8.660761833, 8.668057442, 8.675685883,
        8.685617447, 8.693324089, 8.700592041, 8.70777607, 8.715467453, 8.723162651, 8.730844498,
        8.738502502, 8.746123314, 8.753817558, 8.761521339, 8.768846512, 8.776566505, 8.784269333,
        8.791927338, 8.799625397, 8.807318687, 8.815002441, 8.8227005, 8.830416679, 8.838102341,
        8.846293449, 8.854008675, 8.861726761, 8.869428635, 8.877128601, 8.884835243, 8.892566681,
        8.90029335, 8.90803051, 8.91574955, 8.923474312, 8.931176186, 8.938930511, 8.946667671,
        8.954426765};

    double[] y = new double[]{45204.75893, 114784.2623, 242528.7083, 450466.8054, 723759.5126,
        1094652.171, 1510158.455, 1892077.274, 2377388.815, 2621148.839, 2858927.143, 3054205.274,
        3072362.5, 3105144.452, 3208537.917, 3172016.048, 3237762.107, 3232906.06, 3205781.369,
        3112509.917, 3007661.345, 2914828.155, 2844111.94, 2756956.476, 2753093.619, 2630266.929,
        2559327.762, 2413834.56, 2351139.333, 2323447.179, 2332292.393, 2366656.155, 2367857.726,
        2258578.53, 2135975.887, 2017613.554, 2006158.994, 1983034.804, 1922412.649, 1898818.911,
        1890629.113, 1819708.298, 1837718.613, 1768397.857, 1654868.56, 1650806.56, 1628665.792,
        1587515.827, 1540349.554, 1460334.208, 1418102.786, 1431727.804, 1396976.476, 1397118.607,
        1304515.429, 1213349.899, 1193048.256, 1168530.342, 1109329.408, 1106374.848, 1067001.253,
        1049427.548, 1069455.72, 1060020.563, 1019356.387, 1006609.667, 939384.9643, 877632.7292,
        832264.2589};

    FitQuality fit = PeakFitterUtils.fitPeakModels(x, y, List.of(new GaussianPeak()));
    Assertions.assertNotNull(fit);
    Assertions.assertEquals(0.7930620129683198, fit.fitScore(), 0.0000001);

    fit = PeakFitterUtils.fitPeakModels(x, y, List.of(new AsymmetricGaussianPeak()));
    Assertions.assertNotNull(fit);
    Assertions.assertEquals(0.9283789295872468, fit.fitScore(), 0.0000001);

    fit = PeakFitterUtils.fitPeakModels(x, y, List.of(new GaussianDoublePeak()));
    Assertions.assertNotNull(fit);
    Assertions.assertEquals(0.9332001170402136, fit.fitScore(), 0.0000001);
  }

  @Test
  void testThreeDataPoints() {
    double[] x = new double[] {1, 2, 3};
    double[] y = new double[] {1, 3, 2};

    final FitQuality fitQuality = PeakFitterUtils.fitPeakModels(x, y,
        List.of(new AsymmetricGaussianPeak()));
    Assertions.assertNull(fitQuality);
  }
}
