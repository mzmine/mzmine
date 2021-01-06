/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IonMobilityTimeSeries;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.MsTimeSeries;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.impl.SimpleIonMobilityTimeSeries;
import io.github.mzmine.datamodel.impl.SimpleMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleMsTimeSeries;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;

public class MsTimeSeriesTest {

  private static final Logger logger = Logger.getLogger(MsTimeSeriesTest.class.getName());

  public MsTimeSeries<? extends Scan> makeSimpleTimeSeries() throws IOException {

    RawDataFile file = new RawDataFileImpl("test", Color.BLACK);
    List<Scan> scans = new ArrayList();
    scans.add(new SimpleScan(file, 0, 1, 1f, 0, 0, new double[]{10d, 10d}, new double[]{10d, 10d},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
        Range.closed(10d, 10d)));
    scans.add(new SimpleScan(file, 1, 1, 1f, 0, 0, new double[]{11d, 11d}, new double[]{11d, 11d},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
        Range.closed(11d, 11d)));
    SimpleMsTimeSeries series = new SimpleMsTimeSeries(new MemoryMapStorage(),
        new double[]{5d, 10d}, new double[]{30d, 31d}, scans);
    return series;
  }

  public MsTimeSeries<Frame> makeIonMobilityTimeSeries() throws IOException {
    RawDataFile file = new RawDataFileImpl("test", Color.BLACK);

    Map<Integer, Double> mobilities = new HashMap<>();
    mobilities.put(0, 0.1d);
    mobilities.put(1, 0.2d);
    MemoryMapStorage storage = new MemoryMapStorage();

    List<Frame> frames = new ArrayList<>();
    Frame frame = new SimpleFrame(file, 1, 1, 1f, 0, 0,
        new DataPoint[]{new SimpleDataPoint(1d, 1d)},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
        Range.closed(11d, 11d), MobilityType.TIMS, 2, mobilities, null);

    List<MobilityScan> mobilityScans = new ArrayList<>();
    mobilityScans.add(new SimpleMobilityScan(file, 0, frame, new double[] {1d, 1d}, new double[] {2d, 2d}));
    mobilityScans.add(new SimpleMobilityScan(file, 1, frame, new double[] {2d, 2d}, new double[] {4d, 4d}));

    SimpleIonMobilitySeries ionMobilitySeries = new SimpleIonMobilitySeries(storage, new double[] {1d, 2d}, new double[] {2d, 4d}, mobilityScans);

    return new SimpleIonMobilityTimeSeries(storage, List.of(ionMobilitySeries));
  }

  @Test
  public void testCasting() {

    try {
      MsTimeSeries<? extends Scan> scanSeries = makeSimpleTimeSeries();
      if(scanSeries instanceof SimpleMsTimeSeries) {
        logger.info("MsTimeSeries created by makeSimpleTimeSeries() is an instance of " + SimpleMsTimeSeries.class.getSimpleName());
      }
      if(scanSeries instanceof IonMobilityTimeSeries) {
        logger.info("MsTimeSeries created by makeSimpleTimeSeries() is an instance of " + IonMobilityTimeSeries.class.getSimpleName());
        throw new JUnitException("Illegal cast.");
      }

      MsTimeSeries<? extends Scan> imFrameSeries = makeIonMobilityTimeSeries();
      if(imFrameSeries instanceof SimpleMsTimeSeries) {
        logger.info("MsTimeSeries created by makeIonMobilityTimeSeries() is an instance of " + SimpleMsTimeSeries.class.getSimpleName());
        throw new JUnitException("Illegal cast.");
      }
      if(imFrameSeries instanceof IonMobilityTimeSeries) {
        logger.info("MsTimeSeries created by makeIonMobilityTimeSeries() is an instance of " + IonMobilityTimeSeries.class.getSimpleName());
      } else {
        logger.info("MsTimeSeries created by makeIonMobilityTimeSeries() is not an instance of " + IonMobilityTimeSeries.class.getSimpleName());
        throw new JUnitException("Illegal cast.");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}