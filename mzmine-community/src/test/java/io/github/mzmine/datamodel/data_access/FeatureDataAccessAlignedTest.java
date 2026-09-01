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

package io.github.mzmine.datamodel.data_access;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.FeatureDataType;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import testutils.MZmineTestUtil;

/**
 * Tests {@link FeatureDataAccess#nextFeature()} on an <b>aligned</b> feature list, i.e. the code
 * path taken when no explicit {@link RawDataFile} is passed to
 * {@link EfficientDataAccess#of(io.github.mzmine.datamodel.features.FeatureList, FeatureDataType)}
 * and the list holds more than one file.
 * <p>
 * That path had two defects, both of which only show on a list with several files, and neither of
 * which any production caller reached — every existing caller either runs before alignment or
 * passes an explicit data file:
 * <ol>
 *   <li>{@code currentRawFileIndex} started at -1 and is only advanced when the row cursor wraps, so
 *       the very first call indexed a list with -1 and threw</li>
 *   <li>the file index counts the <em>feature list's</em> files but was resolved against
 *       {@code getRow().getRawDataFiles()}, which only holds the files that row was detected in — so
 *       for any partially detected row it selected the wrong file, or ran out of bounds</li>
 * </ol>
 * The rows below are deliberately asymmetric so that both defects are reachable: with every row
 * present in every file, the second one cannot be observed at all.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class FeatureDataAccessAlignedTest {

  private static final int POINTS_PER_FEATURE = 5;

  /**
   * Which files each row is detected in. Row 1 and row 3 are missing from the first file, so the
   * row cursor has to skip them while iterating file 0.
   */
  private static final int[][] ROW_FILES = {{0, 1, 2}, {1}, {0, 2}, {2}};

  private List<RawDataFile> files;
  private ModularFeatureList flist;

  /**
   * Marker intensity identifying the feature of one row/file pair. Encoded so a mix-up between rows
   * or files is visible in the assertion message.
   */
  private static double marker(int rowIndex, int fileIndex) {
    return 100d * (rowIndex + 1) + (fileIndex + 1);
  }

  @BeforeAll
  public void initialize() {
    MZmineTestUtil.startMzmineCore();

    files = new ArrayList<>();
    for (int f = 0; f < 3; f++) {
      files.add(new RawDataFileImpl("file" + f, null, null, Color.BLACK));
    }

    flist = new ModularFeatureList("aligned", null, files.toArray(RawDataFile[]::new));

    // every file needs its own scans, because a feature's series references them
    final List<List<Scan>> scansPerFile = new ArrayList<>();
    for (final RawDataFile file : files) {
      final List<Scan> scans = new ArrayList<>();
      for (int i = 0; i < POINTS_PER_FEATURE; i++) {
        scans.add(new SimpleScan(file, i, 1, 0.1f * i, null, new double[0], new double[0],
            MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(0d, 1d)));
      }
      for (final Scan scan : scans) {
        file.addScan(scan);
      }
      flist.setSelectedScans(file, scans);
      scansPerFile.add(scans);
    }

    for (int r = 0; r < ROW_FILES.length; r++) {
      final ModularFeatureListRow row = new ModularFeatureListRow(flist, r + 1);
      for (final int f : ROW_FILES[r]) {
        final double[] mzs = new double[POINTS_PER_FEATURE];
        final double[] intensities = new double[POINTS_PER_FEATURE];
        Arrays.fill(mzs, 200d + r);
        Arrays.fill(intensities, marker(r, f));
        row.addFeature(files.get(f), new ModularFeature(flist, files.get(f),
            new SimpleIonTimeSeries(null, mzs, intensities, scansPerFile.get(f)),
            FeatureStatus.DETECTED));
      }
      flist.addRow(row);
    }
  }

  private @NotNull Set<String> expectedFeatures() {
    final Set<String> expected = new HashSet<>();
    for (int r = 0; r < ROW_FILES.length; r++) {
      for (final int f : ROW_FILES[r]) {
        expected.add(describe(files.get(f).getName(), marker(r, f)));
      }
    }
    return expected;
  }

  private static String describe(String fileName, double markerIntensity) {
    return "%s@%.0f".formatted(fileName, markerIntensity);
  }

  @Test
  @DisplayName("aligned access yields every feature of every file exactly once")
  void visitsEveryFeatureExactlyOnce() {
    final FeatureDataAccess access = EfficientDataAccess.of(flist, FeatureDataType.ONLY_DETECTED);

    final Set<String> expected = expectedFeatures();
    Assertions.assertEquals(expected.size(), access.getNumOfFeatures(),
        "getNumOfFeatures must count every row/file pair that carries a feature");

    final List<String> visited = new ArrayList<>();
    while (access.hasNextFeature()) {
      final Feature feature = access.nextFeature();
      Assertions.assertNotNull(feature, "hasNextFeature promised another feature");
      // read through the access, not the feature, so the cursor's own data is checked
      visited.add(describe(feature.getRawDataFile().getName(), access.getIntensity(0)));
    }

    Assertions.assertEquals(expected.size(), visited.size(),
        "every feature must be visited exactly once, got " + visited);
    Assertions.assertEquals(expected, new HashSet<>(visited), "visited the wrong features");
  }

  @Test
  @DisplayName("the data of each visited feature belongs to the file it was reported for")
  void yieldsDataOfTheReportedFile() {
    final FeatureDataAccess access = EfficientDataAccess.of(flist, FeatureDataType.ONLY_DETECTED);

    while (access.hasNextFeature()) {
      final Feature feature = access.nextFeature();
      Assertions.assertNotNull(feature);

      // resolving the file index against the row instead of the feature list used to hand out a
      // feature of a different file here, which stays silent unless the data is checked
      Assertions.assertEquals(POINTS_PER_FEATURE, access.getNumberOfValues(),
          "the access exposes a different number of points than the feature has");
      for (int i = 0; i < access.getNumberOfValues(); i++) {
        Assertions.assertEquals(access.getIntensity(0), access.getIntensity(i), 1e-9,
            "all points of a synthetic feature carry its marker intensity");
      }
      Assertions.assertEquals(feature.getRawDataFile(),
          feature.getFeatureData().getSpectrum(0).getDataFile(),
          "the yielded series belongs to a different raw data file than the feature");
    }
  }

  @Test
  @DisplayName("passing an explicit data file still yields only that file's features")
  void perFileAccessIsUnaffected() {
    for (int f = 0; f < files.size(); f++) {
      final RawDataFile file = files.get(f);
      final FeatureDataAccess access = EfficientDataAccess.of(flist, FeatureDataType.ONLY_DETECTED,
          file);

      int expected = 0;
      for (final int[] rowFiles : ROW_FILES) {
        for (final int rowFile : rowFiles) {
          if (rowFile == f) {
            expected++;
          }
        }
      }

      int visited = 0;
      while (access.hasNextFeature()) {
        final Feature feature = access.nextFeature();
        Assertions.assertNotNull(feature);
        Assertions.assertEquals(file, feature.getRawDataFile());
        visited++;
      }
      Assertions.assertEquals(expected, visited, "wrong feature count for " + file.getName());
    }
  }
}
