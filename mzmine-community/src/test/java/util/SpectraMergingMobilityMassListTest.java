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

package util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link SpectraMerging#extractSummedMobilityScanFromMassLists}: the mobility resolved spectrum the
 * isotope finder searches for IMS features.
 * <p>
 * The fixture is 3 frames at RT 1/2/3, each with 3 mobility scans at mobility 1/2/3. Every mobility
 * scan carries the same two signals (m/z 100 with intensity 1, m/z 200 with intensity 2), but the
 * feature itself only has intensity in the mobility 2 and 3 scans - so a merge that ignored the
 * feature's own mobilogram would pick up a third more signal than it should.
 */
public class SpectraMergingMobilityMassListTest {

  private static final MZTolerance TOL = new MZTolerance(0.01, 10);

  @Test
  void mergesMassListsOfAllFramesAndMobilities() {
    final MergedMassSpectrum merged = merge(true, Range.all(), Range.all(), null);

    Assertions.assertNotNull(merged);
    Assertions.assertEquals(2, merged.getNumberOfDataPoints());
    Assertions.assertEquals(100d, merged.getMzValue(0), 1e-9);
    Assertions.assertEquals(200d, merged.getMzValue(1), 1e-9);
    // 3 frames x 2 mobility scans with feature intensity
    Assertions.assertEquals(6d, merged.getIntensityValue(0), 1e-9);
    Assertions.assertEquals(12d, merged.getIntensityValue(1), 1e-9);
  }

  @Test
  void skipsMobilityScansWithoutFeatureIntensity() {
    // mobility 1 has no feature intensity in any frame, so restricting to it yields nothing
    Assertions.assertNull(merge(true, Range.closed(0.5f, 1.5f), Range.all(), null));
  }

  @Test
  void restrictsToMobilityRange() {
    final MergedMassSpectrum merged = merge(true, Range.closed(2.5f, 3.5f), Range.all(), null);

    Assertions.assertNotNull(merged);
    // 3 frames x 1 mobility scan
    Assertions.assertEquals(3d, merged.getIntensityValue(0), 1e-9);
  }

  @Test
  void restrictsToRtRange() {
    final MergedMassSpectrum merged = merge(true, Range.all(), Range.singleton(2f), null);

    Assertions.assertNotNull(merged);
    // 1 frame x 2 mobility scans
    Assertions.assertEquals(2d, merged.getIntensityValue(0), 1e-9);
  }

  @Test
  void restrictsToMzWindow() {
    final MergedMassSpectrum merged = merge(true, Range.all(), Range.all(),
        Range.closed(50d, 150d));

    Assertions.assertNotNull(merged);
    Assertions.assertEquals(1, merged.getNumberOfDataPoints());
    Assertions.assertEquals(100d, merged.getMzValue(0), 1e-9);
    Assertions.assertEquals(6d, merged.getIntensityValue(0), 1e-9);
  }

  @Test
  void returnsNullWithoutMassLists() {
    Assertions.assertNull(merge(false, Range.all(), Range.all(), null));
  }

  private static MergedMassSpectrum merge(final boolean withMassLists,
      final Range<Float> mobilityRange, final Range<Float> rtRange, final Range<Double> mzRange) {
    return SpectraMerging.extractSummedMobilityScanFromMassLists(feature(withMassLists), TOL,
        mobilityRange, rtRange, mzRange, null);
  }

  private static ModularFeature feature(final boolean withMassLists) {
    final IMSRawDataFile file = new IMSRawDataFileImpl("test", null, null, Color.BLACK);
    final ModularFeatureList flist = new ModularFeatureList("flist", null, file);

    final List<IonMobilitySeries> mobilograms = new ArrayList<>();
    for (int f = 0; f < 3; f++) {
      final SimpleFrame frame = new SimpleFrame(file, f, 1, f + 1f, new double[]{100d},
          new double[]{1d}, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
          Range.closed(50d, 250d), MobilityType.TIMS, null, null);
      frame.setMobilities(new double[]{1d, 2d, 3d});

      final List<BuildingMobilityScan> building = new ArrayList<>();
      for (int m = 0; m < 3; m++) {
        building.add(new BuildingMobilityScan(m, new double[]{100d, 200d}, new double[]{1d, 2d},
            MassSpectrumType.CENTROIDED));
      }
      frame.setMobilityScans(building, withMassLists);
      file.addScan(frame);

      // the feature is only present in the mobility 2 and 3 scans
      final List<MobilityScan> scans = List.copyOf(frame.getMobilityScans());
      mobilograms.add(new SimpleIonMobilitySeries(null, new double[]{100d, 100d, 100d},
          new double[]{0d, 10d, 5d}, scans));
    }

    final ModularFeature feature = new ModularFeature(flist, file,
        IonMobilogramTimeSeriesFactory.of(null, mobilograms,
            new BinningMobilogramDataAccess(file, 1)), FeatureStatus.DETECTED);
    return feature;
  }

  @Test
  void mzRangeDoesNotChangeMergedValuesInsideIt() {
    final MergedMassSpectrum full = merge(true, Range.all(), Range.all(), null);
    final MergedMassSpectrum windowed = merge(true, Range.all(), Range.all(),
        Range.closed(150d, 250d));

    Assertions.assertNotNull(full);
    Assertions.assertNotNull(windowed);
    Assertions.assertEquals(1, windowed.getNumberOfDataPoints());
    Assertions.assertEquals(full.getMzValue(1), windowed.getMzValue(0), 1e-9);
    Assertions.assertEquals(full.getIntensityValue(1), windowed.getIntensityValue(0), 1e-9);
  }

  @Test
  void sourceSpectraAreTheMobilityScans() {
    final MergedMassSpectrum merged = merge(true, Range.all(), Range.all(), null);

    Assertions.assertNotNull(merged);
    Assertions.assertEquals(6, merged.getSourceSpectra().size());
    Assertions.assertTrue(
        merged.getSourceSpectra().stream().allMatch(s -> s instanceof MobilityScan));
    // RT is derived from the source scans, so it must not be NaN - the mass lists alone could not
    // provide it
    Assertions.assertEquals(2f, merged.getRetentionTime(), 1e-6);
  }

  @Test
  void framesAreAllIncludedByDefault() {
    final MergedMassSpectrum merged = merge(true, Range.all(), Range.all(), null);

    Assertions.assertNotNull(merged);
    Assertions.assertEquals(3,
        merged.getSourceSpectra().stream().map(s -> ((MobilityScan) s).getFrame()).distinct()
            .count());
    Assertions.assertTrue(merged.getSourceSpectra().stream()
        .allMatch(s -> ((MobilityScan) s).getFrame() instanceof Frame));
  }
}
