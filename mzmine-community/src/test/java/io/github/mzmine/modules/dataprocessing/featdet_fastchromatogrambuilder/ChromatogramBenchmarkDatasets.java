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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The data sets of the chromatogram builder benchmarks, each with the settings of its batch wizard
 * and once more without noise filter in the mass detection. Options as system properties with the
 * prefix {@value #PROPERTY}:
 * <ul>
 *   <li>{@code root=<folder>} the example data, default {@value #DEFAULT_ROOT}</li>
 *   <li>{@code only=<names>} semicolon separated parts of data set names, e.g.,
 *   {@code GC-EI-QTOF;media}</li>
 *   <li>{@code noise=wizard|off|both} the noise filter of the mass detection, default both</li>
 *   <li>{@code files=<max files>} per data set</li>
 *   <li>{@code hydrate=true} reads OneDrive placeholders, which downloads them</li>
 * </ul>
 */
final class ChromatogramBenchmarkDatasets {

  static final String PROPERTY = "mzmine.test.chrombench.";
  static final String DEFAULT_ROOT = "D:\\OneDrive - mzio GmbH\\Example data - Documents";
  static final MZTolerance ORBITRAP_PRESET = new MZTolerance(0.002, 10);
  static final MZTolerance TOF_PRESET = new MZTolerance(0.005, 20);

  private static final double[] ORBITRAP_GRID = {3, 4, 5, 6, 7, 8, 10, 12, 15, 20, 30};
  private static final double[] TOF_GRID = {6, 8, 10, 12, 15, 20, 25, 30, 40, 60};

  // raw windows file attributes of cloud files that are not on this device
  private static final int FILE_ATTRIBUTE_OFFLINE = 0x1000;
  private static final int FILE_ATTRIBUTE_RECALL_ON_OPEN = 0x40000;
  private static final int FILE_ATTRIBUTE_RECALL_ON_DATA_ACCESS = 0x400000;

  private ChromatogramBenchmarkDatasets() {
  }

  /**
   * @return the data sets with the settings of their batch wizards
   */
  @NotNull
  static List<ChromatogramBenchmarkDataset> wizardDatasets() {
    final Path root = Path.of(System.getProperty(PROPERTY + "root", DEFAULT_ROOT));
    final Path qe = root.resolve("Thermo").resolve("20 years mzmine");
    final List<String> qeM1 = paths(qe, "171103_PMA_TK_M1_01.mzML", "171103_PMA_TK_M1_02.mzML");
    final List<String> qeMedia = paths(qe, "171103_PMA_TK_media_04.mzML");
    final Path zeno = root.resolve("SCIEX").resolve("ZenoTOF").resolve("RawData");
    final Range<Float> gcCrop = Range.closed(0.3f, 30f);

    // wizard workshop-microbes.mzmwizard, the crop leaves out the injection front
    final Range<Float> qeCrop = Range.closed(0.3f, 8f);

    final List<ChromatogramBenchmarkDataset> datasets = new ArrayList<>();
    // the workshop batch for Orbitrap data and a more sensitive setting with more data
    datasets.add(orbitrap("Orbitrap QE, sensitive", qeM1, 2d, 4, 1E4, 5E4, qeCrop));
    datasets.add(orbitrap("Orbitrap QE, workshop", qeM1, 5d, 6, 1E5, 5E5, qeCrop));
    datasets.add(orbitrap("Orbitrap QE media, sensitive", qeMedia, 2d, 4, 1E4, 5E4, qeCrop));
    datasets.add(orbitrap("Orbitrap QE media, workshop", qeMedia, 5d, 6, 1E5, 5E5, qeCrop));
    // reduced copy of the GC-QTOF series below, the integration test data
    datasets.add(
        tof("GC-EI-TOF", List.of("rawdatafiles/integration_tests/gc_tof_ms/019_KR8_20220715.mzML"),
            500d, 4, 1E3, 1E3, null, PolarityType.ANY));
    // wizard Agilent-GC-QTOF-milk.mzmwizard, the min group intensity of the wizard is
    // min(min height, 2 x MS1 noise) for an absolute noise level
    datasets.add(
        tof("GC-EI-QTOF", paths(root.resolve("Agilent").resolve("GC_TOF"), "021_ZR5_20220808.mzML"),
            500d, 4, 1E3, 1E3, gcCrop, PolarityType.ANY));
    // wizard workshop-zeno-dda.mzmwizard, NIST SRM 1950 plasma
    datasets.add(tof("LC-QTOF ZenoTOF DDA",
        paths(zeno.resolve("1_Srm1950_DDA").resolve("Pos"), "20230407_plasma_6_POS.mzML"), 500d, 5,
        1E3, 1E3, Range.closed(0.3f, 7.2f), PolarityType.POSITIVE));
    datasets.add(
        tof("LC-QTOF MSe", List.of("rawdatafiles/integration_tests/mse/mse_20180205_0125.mzML"),
            300d, 4, 600, 1000, null, PolarityType.ANY));
    datasets.add(new ChromatogramBenchmarkDataset("GC-Orbitrap",
        List.of("rawdatafiles/additional/gc_orbi_a.mzML"),
        MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL, 2d, 4, 5E3, 5E4, ORBITRAP_PRESET,
        new double[]{1, 1.5, 2, 3, 4, 5, 6, 8, 10, 15}, null, PolarityType.ANY));
    datasets.add(
        orbitrap("DOM Orbitrap", List.of("rawdatafiles/DOM_a.mzML", "rawdatafiles/DOM_b.mzXML"), 2d,
            4, 5E4, 2E5, null));
    return datasets;
  }

  /**
   * @return the data sets selected by the system properties, each with the noise filter of its
   * wizard followed by the variant without noise filter
   */
  @NotNull
  static List<ChromatogramBenchmarkDataset> selected() {
    final String noise = System.getProperty(PROPERTY + "noise", "both").trim().toLowerCase();
    final boolean withWizardNoise = !noise.equals("off");
    final boolean withoutNoise = !noise.equals("wizard");
    final int maxFiles = Integer.getInteger(PROPERTY + "files", Integer.MAX_VALUE);
    // semicolons, the names contain commas
    final List<String> only = Arrays.stream(System.getProperty(PROPERTY + "only", "").split(";"))
        .map(String::trim).filter(s -> !s.isEmpty()).toList();

    final List<ChromatogramBenchmarkDataset> selected = new ArrayList<>();
    for (final ChromatogramBenchmarkDataset wizard : wizardDatasets()) {
      final List<ChromatogramBenchmarkDataset> variants = new ArrayList<>();
      if (withWizardNoise) {
        variants.add(wizard);
      }
      if (withoutNoise) {
        variants.add(wizard.withoutNoiseFilter());
      }
      for (final ChromatogramBenchmarkDataset dataset : variants) {
        if (only.isEmpty() || only.stream().anyMatch(dataset.name()::contains)) {
          selected.add(dataset.limitFiles(maxFiles));
        }
      }
    }
    return selected;
  }

  /**
   * Missing files and OneDrive placeholders skip a data set. Reading a placeholder downloads it,
   * which stalls the benchmark.
   *
   * @return null if all files of the data set are on this device, otherwise the reason
   */
  @Nullable
  static String unavailableReason(@NotNull ChromatogramBenchmarkDataset dataset) {
    final boolean hydrate = Boolean.getBoolean(PROPERTY + "hydrate");
    for (final String path : dataset.paths()) {
      final File file = new File(path);
      if (!file.isAbsolute()) {
        if (ChromatogramBenchmarkDatasets.class.getClassLoader().getResource(path) == null) {
          return "missing test resource " + path;
        }
        continue;
      }
      if (!file.isFile()) {
        return "missing file %s, set -D%sroot=<folder of the example data>".formatted(file,
            PROPERTY);
      }
      if (!hydrate && isCloudPlaceholder(file.toPath())) {
        return """
            %s is a OneDrive placeholder, reading it downloads %.1f MB. Choose "Always keep on \
            this device" in the explorer first or run with -D%shydrate=true""".formatted(file,
            file.length() / 1e6, PROPERTY);
      }
    }
    return null;
  }

  private static boolean isCloudPlaceholder(@NotNull Path path) {
    try {
      // decision: the raw windows attributes, reading them does not download the file
      final Object attributes = Files.getAttribute(path, "dos:attributes",
          LinkOption.NOFOLLOW_LINKS);
      return attributes instanceof Integer bits &&
          (bits & (FILE_ATTRIBUTE_OFFLINE | FILE_ATTRIBUTE_RECALL_ON_OPEN
              | FILE_ATTRIBUTE_RECALL_ON_DATA_ACCESS)) != 0;
    } catch (IOException | UnsupportedOperationException | IllegalArgumentException e) {
      // no windows file system, no placeholders
      return false;
    }
  }

  @NotNull
  private static ChromatogramBenchmarkDataset orbitrap(@NotNull String name,
      @NotNull List<String> paths, double noiseFactor, int minConsecutive, double minGroup,
      double minHeight, @Nullable Range<Float> cropRt) {
    return new ChromatogramBenchmarkDataset(name, paths,
        MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL, noiseFactor, minConsecutive, minGroup,
        minHeight, ORBITRAP_PRESET, ORBITRAP_GRID, cropRt, PolarityType.ANY);
  }

  @NotNull
  private static ChromatogramBenchmarkDataset tof(@NotNull String name, @NotNull List<String> paths,
      double noiseLevel, int minConsecutive, double minGroup, double minHeight,
      @Nullable Range<Float> cropRt, @NotNull PolarityType polarity) {
    return new ChromatogramBenchmarkDataset(name, paths,
        MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL, noiseLevel, minConsecutive, minGroup,
        minHeight, TOF_PRESET, TOF_GRID, cropRt, polarity);
  }

  @NotNull
  private static List<String> paths(@NotNull Path folder, @NotNull String... names) {
    return Arrays.stream(names).map(n -> folder.resolve(n).toString()).toList();
  }
}
