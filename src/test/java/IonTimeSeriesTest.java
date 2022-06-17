/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
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
