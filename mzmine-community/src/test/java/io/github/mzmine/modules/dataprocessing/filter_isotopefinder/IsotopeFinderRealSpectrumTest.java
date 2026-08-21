package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectionResult;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.ElementAutoDetector;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeContext;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeFinderEngine;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeFinderEngineConfig;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Element;

/**
 * Regression tests on real MS1 mass lists (Orbitrap, positive) for the emitted pattern of the
 * SEARCHED signal. Every emitted charge hypothesis must contain the searched m/z: the pattern is the
 * isotope pattern OF that feature, so cropping has to happen around it.
 */
class IsotopeFinderRealSpectrumTest {

  private static final String RESOURCE_Z2 = "isotopefinder/realdata/ms1_667p3107.tsv";
  private static final double SEARCHED_MZ_Z2 = 667.31073;
  private static final String RESOURCE_Z3 = "isotopefinder/realdata/ms1_1306p2721.tsv";
  private static final double SEARCHED_MZ_Z3 = 1306.2721;
  // real-data settings from the reported cases: wide tolerance, high max charge
  private static final MZTolerance TOL = new MZTolerance(0.008, 25);
  private static final int MAX_CHARGE = 15;

  private static @NotNull MassSpectrum loadSpectrum(@NotNull final String resource)
      throws IOException {
    final List<Double> mzs = new ArrayList<>();
    final List<Double> intensities = new ArrayList<>();
    try (final InputStream in = IsotopeFinderRealSpectrumTest.class.getClassLoader()
        .getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource " + resource);
      try (final BufferedReader reader = new BufferedReader(
          new InputStreamReader(in, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          final String[] cols = line.trim().split("\t");
          if (cols.length != 2) {
            continue;
          }
          try {
            final double mz = Double.parseDouble(cols[0]);
            final double intensity = Double.parseDouble(cols[1]);
            mzs.add(mz);
            intensities.add(intensity);
          } catch (NumberFormatException e) {
            // header line
          }
        }
      }
    }
    final double[] mzArray = new double[mzs.size()];
    final double[] intensityArray = new double[intensities.size()];
    for (int i = 0; i < mzs.size(); i++) {
      mzArray[i] = mzs.get(i);
      intensityArray[i] = intensities.get(i);
    }
    return new SimpleMassSpectrum(mzArray, intensityArray);
  }

  private static @NotNull IsotopeFinderEngine engine(@NotNull final List<Element> elements,
      final boolean autoDetect) {
    final EnvelopeModel model = new CarbonAveragineEnvelopeModel(
        CarbonAveragineEnvelopeParameters.createDefault(), new EnvelopeContext(elements, TOL));
    final IsotopeFinderEngineConfig config = IsotopeFinderEngineConfig.of(elements, MAX_CHARGE, TOL,
        model, "test", true);
    return new IsotopeFinderEngine(autoDetect ? config.withElementDetection(
        ElementDetectionMode.AUTO_DETECT, ElementAutoDetector.DEFAULT_CANDIDATES) : config);
  }

  /**
   * The 0.5 Da spacing around 667.311 is a real z=2 envelope, so z=2 must win - and, whichever
   * charge is emitted, every emitted pattern has to contain the searched signal instead of being
   * cropped onto an unrelated ladder further down the spectrum (the reported bug cropped z=2 to
   * 652.306-654.315).
   */
  @Test
  void searchedSignalIsPartOfEveryEmittedPattern() throws IOException {
    assertPatternsAroundSearchedSignal(loadSpectrum(RESOURCE_Z2), SEARCHED_MZ_Z2, 1.56e8, 2);
  }

  /**
   * The same spectrum off the feature apex: the searched signal is now weaker than another cluster
   * the candidate walk reaches through the chained isotope distances (the 652-654 group). The crop
   * must still grow out of the searched signal instead of moving to the more intense cluster - the
   * reported bug emitted z=2 as 652.306-654.315 with the searched signal missing entirely.
   */
  @Test
  void searchedSignalIsKeptWhenAStrongerClusterIsInRange() throws IOException {
    final MassSpectrum apex = loadSpectrum(RESOURCE_Z2);
    final int n = apex.getNumberOfDataPoints();
    final double[] mzs = new double[n];
    final double[] intensities = new double[n];
    for (int i = 0; i < n; i++) {
      mzs[i] = apex.getMzValue(i);
      // scale down only the searched signal's own cluster, as an off-apex scan would
      intensities[i] =
          mzs[i] > 665 && mzs[i] < 673 ? apex.getIntensityValue(i) / 30d : apex.getIntensityValue(i);
    }
    assertPatternsAroundSearchedSignal(new SimpleMassSpectrum(mzs, intensities), SEARCHED_MZ_Z2,
        1.56e8, 2);
  }

  /**
   * A real z=3 envelope (1305.604-1308.268, 0.334 Da spacing) that sits at ~4% of the spectrum's
   * base peak, with the searched signal at its apex. It must be detected as z=3 with the whole
   * ladder, from ANY of its peaks as the search seed - the low intensity, the ~1 mDa per offset
   * spacing drift against the nominal 13C grid and the neighbouring 1263/1270/1313 clusters must not
   * cost the detection. The user-defined elements are H,C,N,O,S here, matching the reported run.
   */
  @Test
  void detectsChargeThreeEnvelopeFromEveryPeak() throws IOException {
    final MassSpectrum spectrum = loadSpectrum(RESOURCE_Z3);
    final List<Element> elements = List.of(new Element("H"), new Element("C"), new Element("N"),
        new Element("O"), new Element("S"));
    // the ladder of the z=3 envelope, apex first
    final double[] ladder = {1306.2721, 1305.60437, 1305.937866, 1306.60498, 1306.939941,
        1307.271606, 1307.603149, 1307.937622, 1308.2677};
    for (final double seed : ladder) {
      final DetectionResult result = engine(elements, false).detect(spectrum, seed, 1.86e6,
          PolarityType.POSITIVE);
      assertNotNull(result, "no isotope pattern detected for seed " + seed);
      assertEquals(3, result.bestCharge(),
          "the 0.334 Da spaced envelope must be detected as z=3, seed " + seed);
      final IsotopePattern best = result.patterns().getFirst();
      assertTrue(best.getNumberOfDataPoints() >= 8,
          "the z=3 ladder must be reported completely, seed " + seed + ": " + describe(best));
      assertTrue(contains(best, seed),
          "pattern does not contain the searched signal, seed " + seed + ": " + describe(best));
    }
  }

  private static void assertPatternsAroundSearchedSignal(@NotNull final MassSpectrum spectrum,
      final double searchedMz, final double height, final int expectedCharge) {
    final List<Element> elements = List.of(new Element("C"), new Element("Cl"), new Element("S"));
    final DetectionResult result = engine(elements, true).detect(spectrum, searchedMz, height,
        PolarityType.POSITIVE);
    assertNotNull(result, "no isotope pattern detected for the searched signal");
    assertEquals(expectedCharge, result.bestCharge(), "wrong charge for the searched envelope");
    for (final IsotopePattern pattern : result.patterns()) {
      assertTrue(contains(pattern, searchedMz),
          "pattern of charge " + pattern.getCharge() + " does not contain the searched signal: "
              + describe(pattern));
    }
  }

  private static boolean contains(@NotNull final IsotopePattern pattern, final double mz) {
    for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
      if (TOL.checkWithinTolerance(pattern.getMzValue(i), mz)) {
        return true;
      }
    }
    return false;
  }

  private static @NotNull String describe(@NotNull final IsotopePattern pattern) {
    final StringBuilder sb = new StringBuilder();
    sb.append("z=").append(pattern.getCharge()).append(" [");
    for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(String.format("%.4f", pattern.getMzValue(i)));
    }
    return sb.append(']').toString();
  }
}
