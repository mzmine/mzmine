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

package datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FeatureDataTypeTest {

  @Test
  void testSimpleIonTimeSeries() {
    RawDataFile file = null;
    file = new RawDataFileImpl("testfile", null, null, Color.BLACK);
    Assertions.assertNotNull(file);

    final ModularFeatureList flist = new ModularFeatureList("flist", null, file);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);
    final ModularFeature feature = new ModularFeature(flist, file, null, null);
    row.addFeature(file, feature);
    flist.addRow(row);

    List<Scan> scans = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      scans.add(new SimpleScan(file, i, 1, 0.1f * i, null, new double[0], new double[0],
          MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(0d, 1d)));
    }

    for (Scan scan : scans) {
      try {
        file.addScan(scan);
      } catch (IOException e) {
        e.printStackTrace();
        Assertions.fail("Cannot add scans to raw data file.");
      }
    }
    flist.setSelectedScans(file, scans.subList(3, 18));

    IonTimeSeries<Scan> series = new SimpleIonTimeSeries(null,
        new double[]{150d, 150d, 150d, 150d, 150d}, new double[]{1d, 5d, 20d, 5d, 1d}, scans.subList(5, 10));

    // test if load/save is good
    feature.set(FeatureDataType.class, series);
    DataTypeTestUtils.testSaveLoad(new FeatureDataType(), series, flist, row, feature, file);

    // test if equals is good
    IonTimeSeries<Scan> series_2 = new SimpleIonTimeSeries(null,
        new double[]{150d, 150d, 150d, 150d, 150d}, new double[]{1d, 5d, 20d, 5d, 1d}, scans.subList(5, 10));
    Assertions.assertEquals(series, series_2);

    // test if equals returns false
    IonTimeSeries<Scan> series_3 = new SimpleIonTimeSeries(null,
        new double[]{150d, 150d, 120d, 130d, 150d}, new double[]{1d, 4d, 20d, 4.999d, 1d}, scans.subList(5, 10));
    Assertions.assertNotEquals(series, series_3);

    DataTypeTestUtils.testSaveLoad(new FeatureDataType(), null, flist, row, feature, file);
  }

  @Test
  void testIonMobilogramTimeSeries() {
    IMSRawDataFile file = null;
    file = new IMSRawDataFileImpl("testfile", null, null, Color.BLACK);
    Assertions.assertNotNull(file);

    final ModularFeatureList flist = new ModularFeatureList("flist", null, file);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);
    final ModularFeature feature = new ModularFeature(flist, file, null, null);
    row.addFeature(file, feature);
    flist.addRow(row);

    for (int i = 0; i < 20; i++) {
      List<BuildingMobilityScan> scans = new ArrayList<>();
      for (int j = 0; j < 5; j++) {
        scans.add(new BuildingMobilityScan(j, new double[0], new double[0]));
      }
      SimpleFrame frame = new SimpleFrame(file, i, 1, 0.1f * i, new double[0], new double[0],
          MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(0d, 1d),
          MobilityType.TIMS, null, null);
      frame.setMobilities(new double[]{5d, 4d, 3d, 2d, 1d});
      frame.setMobilityScans(scans, true);
      try {
        file.addScan(frame);
      } catch (IOException e) {
        Assertions.fail();
      }
    }

    flist.setSelectedScans(file, file.getFrames().subList(3, 18));

    IonMobilogramTimeSeries series = generateTrace(file, 2);

    // test if load/save is good
    feature.set(FeatureDataType.class, series);
    DataTypeTestUtils.testSaveLoad(new FeatureDataType(), series, flist, row, feature, file);

    // test if equals is good
    IonMobilogramTimeSeries series_2 = generateTrace(file, 2);
    Assertions.assertEquals(series, series_2);

    IonMobilogramTimeSeries series_3 = generateTrace(file, 2.005d);
    Assertions.assertNotEquals(series, series_3);

    DataTypeTestUtils.testSaveLoad(new FeatureDataType(), null, flist, row, feature, file);
  }

  @NotNull
  private IonMobilogramTimeSeries generateTrace(IMSRawDataFile file, double seed) {
    List<IonMobilitySeries> mobilograms = new ArrayList<>();
    for (int i = 7; i < 12; i++) {
      mobilograms.add(new SimpleIonMobilitySeries(null,
          new double[]{seed * 1, seed * 2, seed * 3, seed * 4, seed * 5},
          new double[]{seed * 5, seed * 4, seed * 3, seed * 2, seed * 1},
          file.getFrame(i).getMobilityScans()));
    }

    IonMobilogramTimeSeries series = IonMobilogramTimeSeriesFactory.of(null,
        new double[]{150d, 150d, 150d, 150d, 150d}, new double[]{1d, 5d, 20d, 5d, 1d}, mobilograms,
        new BinningMobilogramDataAccess(file, 1));
    return series;
  }
}
