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

package util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.util.IonMobilityUtils;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IonMobilityUtilsTest {

  @Test
  void testIsPotentialIsotope() {
    final MZTolerance tol = new MZTolerance(0.003, 15);
    Assertions.assertTrue(IonMobilityUtils.isPotentialIsotope(760.5594, 761.5627, tol));
    Assertions.assertTrue(IonMobilityUtils.isPotentialIsotope(760.5594, 759.5590, tol));
    Assertions.assertTrue(IonMobilityUtils.isPotentialIsotope(760.5594, 760.5627, tol));
    Assertions.assertTrue(IonMobilityUtils.isPotentialIsotope(760.5594, 762.5660, tol));
    Assertions.assertTrue(IonMobilityUtils.isPotentialIsotope(760.5594, 763.5693, tol));
    Assertions.assertFalse(IonMobilityUtils.isPotentialIsotope(760.5594, 763.6000, tol));
  }

  @Test
  void testGetScanForMobility() {
    final IMSRawDataFile file = new IMSRawDataFileImpl("", null, null, Color.BLACK);
    final SimpleFrame timsFrame = new SimpleFrame(file, 0, 1, 1, null, null,
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.singleton(1d),
        MobilityType.TIMS, null, null);
    final SimpleFrame frame = new SimpleFrame(file, 0, 1, 1, null, null,
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.singleton(1d),
        MobilityType.DRIFT_TUBE, null, null);

    final List<BuildingMobilityScan> scans = new ArrayList<>();
    final double[] timsMobilities = new double[10];
    final double[] mobilities = new double[10];
    for (int i = 0; i < 10; i++) {
      scans.add(new BuildingMobilityScan(i, new double[]{}, new double[]{}));
      timsMobilities[9 - i] = i / 10d;
      mobilities[i] = i / 10d;
    }

    frame.setMobilityScans(scans, false);
    frame.setMobilities(mobilities);
    timsFrame.setMobilityScans(scans, false);
    timsFrame.setMobilities(timsMobilities);

    final MobilityScan scan = IonMobilityUtils.getMobilityScanForMobility(frame, 0.31d);
    Assertions.assertEquals(3, scan.getMobilityScanNumber());
    Assertions.assertEquals(0.3d, scan.getMobility());

    final MobilityScan timsScan = IonMobilityUtils.getMobilityScanForMobility(timsFrame, 0.31d);
    Assertions.assertEquals(0.3d, timsScan.getMobility());
    Assertions.assertEquals(6, timsScan.getMobilityScanNumber());
  }

}
