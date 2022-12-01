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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class IonTimeSeriesTest {

  private static final Logger logger = Logger.getLogger(IonTimeSeriesTest.class.getName());

  public static IonTimeSeries<? extends Scan> makeSimpleTimeSeries() throws IOException {

    RawDataFile file = new RawDataFileImpl("test", null, null, Color.BLACK);
    List<Scan> scans = new ArrayList();
    scans.add(new SimpleScan(file, 0, 1, 1f, null, new double[]{10d, 10d}, new double[]{10d, 10d},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
        Range.closed(10d, 10d)));
    scans.add(new SimpleScan(file, 1, 1, 1f, null, new double[]{11d, 11d}, new double[]{11d, 11d},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
        Range.closed(11d, 11d)));
    SimpleIonTimeSeries series = new SimpleIonTimeSeries(null,
        new double[]{5d, 10d}, new double[]{30d, 31d}, scans);
    return series;
  }

  public static IonTimeSeries<Frame> makeIonMobilityTimeSeries() throws IOException {
    IMSRawDataFile file = new IMSRawDataFileImpl("test", null, null, Color.BLACK);

    List<Frame> frames = new ArrayList<>();
    SimpleFrame frame = new SimpleFrame(file, 1, 1, 1f,
        new double[]{1d}, new double[]{1d},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
        Range.closed(11d, 11d), MobilityType.TIMS, null, null);
    frame.setMobilities(new double[]{1d, 2d});

    List<BuildingMobilityScan> mobilityScans = new ArrayList<>();
    mobilityScans
        .add(new BuildingMobilityScan(0, new double[]{1d, 1d}, new double[]{2d, 2d}));
    mobilityScans
        .add(new BuildingMobilityScan(1, new double[]{2d, 2d}, new double[]{4d, 4d}));

    frame.setMobilityScans(mobilityScans, false);

    SimpleIonMobilitySeries ionMobilitySeries = new SimpleIonMobilitySeries(null,
        new double[]{1d, 2d}, new double[]{2d, 4d}, frame.getMobilityScans());

    return IonMobilogramTimeSeriesFactory
        .of(null, List.of(ionMobilitySeries), new BinningMobilogramDataAccess(file, 1));
  }

  @Disabled
  @Test
  void testCasting() {

    try {
      IonTimeSeries<? extends Scan> scanSeries = makeSimpleTimeSeries();
      Assertions.assertTrue(scanSeries instanceof SimpleIonTimeSeries);
      Assertions.assertFalse(scanSeries instanceof IonMobilogramTimeSeries);

      List<Scan> scans = (List<Scan>) scanSeries.getSpectra();
      Assertions.assertTrue(scans.get(0) instanceof Scan);
      Assertions.assertFalse(scans.get(0) instanceof Frame);

      IonTimeSeries<? extends Scan> imFrameSeries = makeIonMobilityTimeSeries();
      Assertions.assertFalse(imFrameSeries instanceof SimpleIonTimeSeries);
      Assertions.assertTrue(imFrameSeries instanceof IonMobilogramTimeSeries);

      List<Scan> frames = (List<Scan>) imFrameSeries.getSpectra();
      Assertions.assertTrue(frames.get(0) instanceof Scan);
      Assertions.assertTrue(frames.get(0) instanceof Frame);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
