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

package io.github.mzmine.datamodel.data.test;

import io.github.mzmine.util.FeatureTableFXUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.RawFileType;
import io.github.mzmine.datamodel.data.types.numbers.IDType;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXWindow;
import io.github.mzmine.project.impl.RawDataFileImpl;
import javafx.application.Application;
import javafx.stage.Stage;

public class FXTableWindow extends Application {

  Random rand = new Random(System.currentTimeMillis());
  Logger logger = Logger.getLogger(this.getClass().getName());

  @Override
  public void start(Stage stage) {
    logger.info("Init test");

    ModularFeatureList flist;
    try {
      flist = createMinimalTest();
      FeatureTableFXUtil.createFeatureTableWindow(flist);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ModularFeatureList createMinimalTest() throws IOException {
    List<RawDataFile> raw = new ArrayList<>();
    raw.add(new RawDataFileImpl("Raw"));
    ModularFeatureList flist = new ModularFeatureList("flist name", raw);
    // create and add
    createMinimalRows(flist);
    return flist;
  }


  private void createMinimalRows(ModularFeatureList flist) {
    flist.addRowType(new IDType());
    flist.addFeatureType(new DetectionType());

    for (int i = 0; i < 2; i++) {
      RawDataFile raw = flist.getRawDataFile(0);
      ModularFeature p = new ModularFeature(flist);
      p.set(RawFileType.class, raw);
      p.set(DetectionType.class, FeatureStatus.DETECTED);

      ModularFeatureListRow r = new ModularFeatureListRow(flist);
      r.set(IDType.class, (i));
      r.addPeak(raw, p);
      flist.addRow(r);
    }
  }

  public static void startThisApp(String[] args) {
    launch(args);
  }

}


