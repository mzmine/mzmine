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

package io.github.mzmine.util.scans;

import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks that {@link SpectraMerging#calculatedMergedMzsAndIntensities} yields exactly the same
 * results as the legacy guava based {@link SpectraMerging#calculatedMergedMzsAndIntensitiesLegacy}
 * when merging all test spectra in rawdatafiles/testspectra.
 */
@TestInstance(Lifecycle.PER_CLASS)
class SpectraMergingRegressionTest {

  /**
   * From almost no merging to wide tolerances that stress the range trimming.
   */
  private static final List<MZTolerance> TOLERANCES = List.of(new MZTolerance(0.0005, 1),
      new MZTolerance(0.001, 3), new MZTolerance(0.002, 5), new MZTolerance(0.005, 10),
      new MZTolerance(0.008, 25), new MZTolerance(0.02, 50), new MZTolerance(0.1, 100));
  // merged results are shared by all rows of the same tolerance, key: legacy|tolerance|type
  private final Map<String, double[][]> mergedCache = new HashMap<>();
  private List<SimpleMassList> spectra;

  @BeforeAll
  void loadSpectra() throws IOException {
    spectra = TestSpectraLoader.loadTestSpectra();
  }

  /**
   * Expected values were calculated with the legacy implementation: the first and last signal and
   * 18 evenly spaced signals that were merged from multiple data points, for each tolerance.
   */
  @ParameterizedTest(name = "{0} m/z, {1} ppm, signal {3}")
  @CsvSource(textBlock = """
      # absTol, ppmTol, numSignals, index, mz, summed, maximum, average
      0.0005, 1, 7812, 0, 20.14366851, 1672.0, 1672.0, 1672.0
      0.0005, 1, 7812, 253, 151.0996646650321, 3895.0, 2533.0, 1947.5
      0.0005, 1, 7812, 955, 211.09233722925978, 5728.0, 3702.0, 2864.0
      0.0005, 1, 7812, 1676, 249.11055769572795, 14747.0, 9276.0, 7373.5
      0.0005, 1, 7812, 2271, 275.12751992329606, 5546.0, 4128.0, 2773.0
      0.0005, 1, 7812, 2879, 300.6972944546156, 18795.0, 14489.0, 9397.5
      0.0005, 1, 7812, 3548, 327.1542685521323, 12123.0, 6298.0, 6061.5
      0.0005, 1, 7812, 4439, 364.18060266655095, 14407.0, 8662.0, 7203.5
      0.0005, 1, 7812, 5715, 432.2803927663421, 1557764.0, 925747.0, 778882.0
      0.0005, 1, 7812, 6637, 508.219914, 6282.0, 3141.0, 3141.0
      0.0005, 1, 7812, 6764, 525.3173258, 8004.0, 4002.0, 4002.0
      0.0005, 1, 7812, 6906, 546.1613963, 6382.0, 3191.0, 3191.0
      0.0005, 1, 7812, 7004, 561.2546146, 9018.0, 4509.0, 4509.0
      0.0005, 1, 7812, 7143, 590.1644566, 4762.0, 2381.0, 2381.0
      0.0005, 1, 7812, 7271, 628.0159844, 5066.0, 2533.0, 2533.0
      0.0005, 1, 7812, 7391, 671.3498068, 6586.0, 3293.0, 3293.0
      0.0005, 1, 7812, 7512, 757.5114303, 5674.0, 2837.0, 2837.0
      0.0005, 1, 7812, 7639, 931.4090566999998, 5674.0, 2837.0, 2837.0
      0.0005, 1, 7812, 7808, 1288.687629, 1418.0, 709.0, 709.0
      0.0005, 1, 7812, 7811, 1295.741823, 1621.0, 1621.0, 1621.0
      0.001, 3, 7608, 0, 20.14366851, 1672.0, 1672.0, 1672.0
      0.001, 3, 7608, 202, 143.05706482752214, 11518.0, 9220.0, 5759.0
      0.001, 3, 7608, 907, 209.1391366637987, 12320.0, 9980.0, 6160.0
      0.001, 3, 7608, 1501, 242.11996096983523, 17484.0, 14850.0, 8742.0
      0.001, 3, 7608, 2055, 268.17460584265046, 101054.0, 59523.0, 50527.0
      0.001, 3, 7608, 2581, 291.1592049984358, 9078.0, 4610.0, 4539.0
      0.001, 3, 7608, 3087, 313.1505079586432, 5218.0, 2989.0, 2609.0
      0.001, 3, 7608, 3616, 335.06760564683793, 3542.0, 2989.0, 1771.0
      0.001, 3, 7608, 4231, 361.18699312076683, 6886.0, 3745.0, 3443.0
      0.001, 3, 7608, 5081, 406.0581174279905, 5066.0, 2989.0, 2533.0
      0.001, 3, 7608, 5939, 460.2055404223792, 7078.0, 5674.0, 3539.0
      0.001, 3, 7608, 6496, 516.224083, 10436.0, 5218.0, 5218.0
      0.001, 3, 7608, 6663, 539.2151673, 7802.0, 3901.0, 3901.0
      0.001, 3, 7608, 6794, 560.1909791905853, 9825.0, 3745.0, 3275.0
      0.001, 3, 7608, 6955, 594.2810251, 9322.0, 4661.0, 4661.0
      0.001, 3, 7608, 7119, 650.1295156, 5572.0, 2786.0, 2786.0
      0.001, 3, 7608, 7256, 722.2976356, 3648.0, 1824.0, 1824.0
      0.001, 3, 7608, 7396, 842.6115212, 2938.0, 1469.0, 1469.0
      0.001, 3, 7608, 7604, 1288.687629, 1418.0, 709.0, 709.0
      0.001, 3, 7608, 7607, 1295.741823, 1621.0, 1621.0, 1621.0
      0.002, 5, 7308, 0, 20.14366851, 1672.0, 1672.0, 1672.0
      0.002, 5, 7308, 46, 39.971472375000005, 1216.0, 608.0, 608.0
      0.002, 5, 7308, 783, 203.08054905423268, 13254.0, 8106.0, 6627.0
      0.002, 5, 7308, 1263, 234.07366684802935, 16213.0, 10894.0, 8106.5
      0.002, 5, 7308, 1679, 255.1585067727189, 8713.0, 5927.0, 4356.5
      0.002, 5, 7308, 2112, 276.14277683357324, 6949.0, 4213.0, 3474.5
      0.002, 5, 7308, 2549, 296.1515216830155, 4510.0, 3191.0, 2255.0
      0.002, 5, 7308, 2961, 315.1395830798921, 13343.0, 6808.0, 6671.5
      0.002, 5, 7308, 3404, 334.1643535492563, 6925.0, 4341.0, 3462.5
      0.002, 5, 7308, 3874, 356.18016965216674, 16315.0, 11148.0, 8157.5
      0.002, 5, 7308, 4540, 389.2352967095994, 9136.0, 4711.0, 4568.0
      0.002, 5, 7308, 5280, 434.28736511264856, 64893.0, 38853.0, 32446.5
      0.002, 5, 7308, 6141, 508.1768248, 5370.0, 2685.0, 2685.0
      0.002, 5, 7308, 6339, 536.3278427, 25126.0, 12563.0, 12563.0
      0.002, 5, 7308, 6502, 561.2546146, 9018.0, 4509.0, 4509.0
      0.002, 5, 7308, 6700, 606.6106437, 1216.0, 608.0, 608.0
      0.002, 5, 7308, 6884, 670.2442031, 2938.0, 1469.0, 1469.0
      0.002, 5, 7308, 7070, 807.3217176, 4762.0, 2381.0, 2381.0
      0.002, 5, 7308, 7304, 1288.687629, 1418.0, 709.0, 709.0
      0.002, 5, 7308, 7307, 1295.741823, 1621.0, 1621.0, 1621.0
      0.005, 10, 6253, 0, 20.14366851, 1672.0, 1672.0, 1672.0
      0.005, 10, 6253, 46, 39.971472375000005, 1216.0, 608.0, 608.0
      0.005, 10, 6253, 655, 199.08903032132454, 12291.0, 4964.0, 4097.0
      0.005, 10, 6253, 939, 223.10714584514537, 16407.0, 8808.0, 5469.0
      0.005, 10, 6253, 1225, 244.18654986256502, 4421.0, 2634.0, 2210.5
      0.005, 10, 6253, 1563, 265.16416538577323, 44845.0, 18135.0, 14948.333333333334
      0.005, 10, 6253, 1831, 282.1681161206991, 52756.0, 31813.0, 17585.333333333332
      0.005, 10, 6253, 2155, 300.160106804946, 10453.0, 9744.0, 5226.5
      0.005, 10, 6253, 2465, 317.16930205849724, 11033.0, 7234.0, 5516.5
      0.005, 10, 6253, 2813, 336.15948792538745, 7484.0, 4596.0, 3742.0
      0.005, 10, 6253, 3168, 356.131150737928, 8060.0, 3957.0, 2686.6666666666665
      0.005, 10, 6253, 3555, 378.17222785850174, 4205.0, 3445.0, 2102.5
      0.005, 10, 6253, 4043, 412.19500264673576, 10232.0, 8155.0, 5116.0
      0.005, 10, 6253, 4486, 444.1912167017075, 6735.0, 4000.0, 3367.5
      0.005, 10, 6253, 5126, 508.1552808, 4762.0, 2381.0, 2381.0
      0.005, 10, 6253, 5387, 548.2756433256252, 5198.0, 2766.0, 1732.6666666666667
      0.005, 10, 6253, 5646, 605.2677859823605, 4762.0, 2432.0, 2381.0
      0.005, 10, 6253, 5914, 728.2086822, 3140.0, 1570.0, 1570.0
      0.005, 10, 6253, 6249, 1288.687629, 1418.0, 709.0, 709.0
      0.005, 10, 6253, 6252, 1295.741823, 1621.0, 1621.0, 1621.0
      0.008, 25, 5408, 0, 20.14366851, 1672.0, 1672.0, 1672.0
      0.008, 25, 5408, 46, 39.971472375000005, 1216.0, 608.0, 608.0
      0.008, 25, 5408, 620, 200.1235305362292, 9738.0, 3659.0, 3246.0
      0.008, 25, 5408, 931, 230.06113035594007, 7340.0, 3090.0, 2446.6666666666665
      0.008, 25, 5408, 1207, 252.08890390057695, 3813.0, 1621.0, 1271.0
      0.008, 25, 5408, 1472, 272.15142434999996, 3952.0, 1976.0, 1976.0
      0.008, 25, 5408, 1748, 291.169442507371, 9768.0, 5471.0, 3256.0
      0.008, 25, 5408, 2035, 310.16632109013324, 10581.0, 5572.0, 3527.0
      0.008, 25, 5408, 2313, 328.0853492818462, 6814.0, 4559.0, 3407.0
      0.008, 25, 5408, 2616, 347.1719655289685, 15831.0, 7853.0, 5277.0
      0.008, 25, 5408, 2916, 368.1443642026717, 7748.0, 3395.0, 2582.6666666666665
      0.008, 25, 5408, 3224, 391.239682735756, 6687.0, 4205.0, 3343.5
      0.008, 25, 5408, 3528, 418.20449398718796, 5167.0, 4559.0, 2583.5
      0.008, 25, 5408, 3818, 444.24503491023535, 16912.0, 9423.0, 8456.0
      0.008, 25, 5408, 4173, 484.1784716595733, 1875.0, 1216.0, 937.5
      0.008, 25, 5408, 4489, 534.0751575, 3546.0, 1773.0, 1773.0
      0.008, 25, 5408, 4749, 585.228437685698, 5272.0, 3698.0, 2636.0
      0.008, 25, 5408, 5046, 712.1494163, 3850.0, 1925.0, 1925.0
      0.008, 25, 5408, 5404, 1288.687629, 1418.0, 709.0, 709.0
      0.008, 25, 5408, 5407, 1295.741823, 1621.0, 1621.0, 1621.0
      0.02, 50, 3986, 0, 20.14366851, 1672.0, 1672.0, 1672.0
      0.02, 50, 3986, 46, 39.971472375000005, 1216.0, 608.0, 608.0
      0.02, 50, 3986, 452, 194.1190670649907, 15056.0, 8814.0, 5018.666666666667
      0.02, 50, 3986, 682, 227.13907840782377, 10417.0, 7396.0, 5208.5
      0.02, 50, 3986, 887, 254.14322838203566, 9196.0, 5876.0, 4598.0
      0.02, 50, 3986, 1094, 278.11499578584363, 35560.0, 17426.0, 11853.333333333334
      0.02, 50, 3986, 1306, 301.06885730832454, 8565.0, 3799.0, 2855.0
      0.02, 50, 3986, 1502, 321.21027788453887, 5530.0, 3445.0, 2765.0
      0.02, 50, 3986, 1711, 343.09757376262917, 9676.0, 6282.0, 4838.0
      0.02, 50, 3986, 1933, 365.98731189292783, 4044.0, 2938.0, 2022.0
      0.02, 50, 3986, 2144, 388.65538005195987, 9261.0, 5218.0, 4630.5
      0.02, 50, 3986, 2366, 414.1454278909467, 6285.0, 2638.0, 2095.0
      0.02, 50, 3986, 2576, 439.1242014883255, 7041.0, 3799.0, 3520.5
      0.02, 50, 3986, 2810, 472.1487835912793, 5550.0, 3574.0, 2775.0
      0.02, 50, 3986, 3047, 512.2243873, 6990.0, 3495.0, 3495.0
      0.02, 50, 3986, 3214, 551.0487097, 7902.0, 3951.0, 3951.0
      0.02, 50, 3986, 3428, 612.0562333, 1216.0, 608.0, 608.0
      0.02, 50, 3986, 3666, 737.3323443000002, 3040.0, 1520.0, 1520.0
      0.02, 50, 3986, 3982, 1288.687629, 1418.0, 709.0, 709.0
      0.02, 50, 3986, 3985, 1295.741823, 1621.0, 1621.0, 1621.0
      0.1, 100, 2085, 0, 20.14366851, 1672.0, 1672.0, 1672.0
      0.1, 100, 2085, 15, 28.895757118605495, 1929.0, 1418.0, 964.5
      0.1, 100, 2085, 252, 176.099356085666, 13311.0, 5319.0, 4437.0
      0.1, 100, 2085, 360, 217.10122498518, 40749.0, 16869.0, 13583.0
      0.1, 100, 2085, 480, 256.17822185096566, 5592.0, 3495.0, 1864.0
      0.1, 100, 2085, 595, 288.1106160286417, 29684.0, 13373.0, 9894.666666666666
      0.1, 100, 2085, 695, 315.0389632634255, 15046.0, 8663.0, 7523.0
      0.1, 100, 2085, 798, 342.1876320035203, 27327.0, 10255.0, 9109.0
      0.1, 100, 2085, 916, 372.7070733535678, 6909.0, 5420.0, 3454.5
      0.1, 100, 2085, 1026, 404.1183159000359, 2787.0, 1976.0, 1393.5
      0.1, 100, 2085, 1129, 435.0644733404756, 4205.0, 3597.0, 2102.5
      0.1, 100, 2085, 1243, 469.21598326963596, 22497.0, 10284.0, 7499.0
      0.1, 100, 2085, 1350, 508.2241605529303, 7559.0, 3141.0, 2519.6666666666665
      0.1, 100, 2085, 1436, 544.2980332, 5168.0, 2584.0, 2584.0
      0.1, 100, 2085, 1514, 579.0652204, 1520.0, 760.0, 760.0
      0.1, 100, 2085, 1629, 637.2904590324512, 4610.0, 2989.0, 2305.0
      0.1, 100, 2085, 1747, 720.1331538, 1216.0, 608.0, 608.0
      0.1, 100, 2085, 1873, 842.3664825, 8004.0, 4002.0, 4002.0
      0.1, 100, 2085, 2081, 1288.687629, 1418.0, 709.0, 709.0
      0.1, 100, 2085, 2084, 1295.741823, 1621.0, 1621.0, 1621.0
      """)
  void mergedSignalMatchesExpected(final double absTolerance, final double ppmTolerance,
      final int numSignals, final int index, final double mz, final double summed,
      final double maximum, final double average) {
    final MZTolerance tolerance = new MZTolerance(absTolerance, ppmTolerance);
    final IntensityMergingType[] types = {IntensityMergingType.SUMMED, IntensityMergingType.MAXIMUM,
        IntensityMergingType.AVERAGE};
    final double[] intensities = {summed, maximum, average};

    for (final boolean legacy : new boolean[]{true, false}) {
      for (int t = 0; t < types.length; t++) {
        final double[][] merged = mergeCached(legacy, tolerance, types[t]);
        final String message = (legacy ? "legacy " : "range map ") + tolerance + " " + types[t];
        Assertions.assertEquals(numSignals, merged[0].length, message + ": number of signals");
        Assertions.assertEquals(mz, merged[0][index], message + ": m/z");
        Assertions.assertEquals(intensities[t], merged[1][index], message + ": intensity");
      }
    }
  }

  Stream<Arguments> parameterCombinations() {
    final CenterFunction median = new CenterFunction(CenterMeasure.MEDIAN, Weighting.NONE);
    final CenterFunction sqrtAvg = new CenterFunction(CenterMeasure.AVG, Weighting.SQRT);
    final List<Arguments> args = new ArrayList<>();
    for (final MZTolerance tolerance : TOLERANCES) {
      for (final IntensityMergingType type : IntensityMergingType.values()) {
        args.add(Arguments.of(tolerance, type, SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null,
            null));
      }
    }
    for (final MZTolerance tolerance : List.of(new MZTolerance(0.002, 5),
        new MZTolerance(0.02, 50))) {
      for (final IntensityMergingType type : IntensityMergingType.values()) {
        args.add(Arguments.of(tolerance, type, SpectraMerging.DEFAULT_CENTER_FUNCTION, 1000d, null,
            null));
        args.add(Arguments.of(tolerance, type, SpectraMerging.DEFAULT_CENTER_FUNCTION, null, 5000d,
            null));
        args.add(
            Arguments.of(tolerance, type, SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null, 2));
        args.add(Arguments.of(tolerance, type, median, 500d, 2000d, 3));
        args.add(Arguments.of(tolerance, type, sqrtAvg, null, null, null));
      }
    }
    return args.stream();
  }

  /**
   * Compares all merged signals of the new and the legacy implementation.
   */
  @SuppressWarnings("deprecation")
  @ParameterizedTest
  @MethodSource("parameterCombinations")
  void mergeMatchesLegacyWithParameters(@NotNull final MZTolerance tolerance,
      @NotNull final IntensityMergingType type, @NotNull final CenterFunction centerFunction,
      @Nullable final Double inputNoiseLevel, @Nullable final Double outputNoiseLevel,
      @Nullable final Integer minNumPeaks) {
    final double[][] legacy = SpectraMerging.calculatedMergedMzsAndIntensitiesLegacy(spectra,
        tolerance, type, centerFunction, inputNoiseLevel, outputNoiseLevel, minNumPeaks);
    final double[][] rangeMap = SpectraMerging.calculatedMergedMzsAndIntensities(spectra, tolerance,
        type, centerFunction, inputNoiseLevel, outputNoiseLevel, minNumPeaks);

    Assertions.assertEquals(legacy[0].length, rangeMap[0].length, "number of signals");
    Assertions.assertArrayEquals(legacy[0], rangeMap[0], "m/z");
    Assertions.assertArrayEquals(legacy[1], rangeMap[1], "intensity");
  }

  @SuppressWarnings("deprecation")
  @NotNull
  private double[][] mergeCached(final boolean legacy, @NotNull final MZTolerance tolerance,
      @NotNull final IntensityMergingType type) {
    final String key = legacy + "|" + tolerance + "|" + type;
    return mergedCache.computeIfAbsent(key,
        k -> legacy ? SpectraMerging.calculatedMergedMzsAndIntensitiesLegacy(spectra, tolerance,
            type, SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null, null)
            : SpectraMerging.calculatedMergedMzsAndIntensities(spectra, tolerance, type,
                SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null, null));
  }
}
