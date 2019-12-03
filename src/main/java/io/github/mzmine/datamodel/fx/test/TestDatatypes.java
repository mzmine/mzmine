/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.fx.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.TypeColumnUndefinedException;
import io.github.mzmine.datamodel.data.WrongTypeException;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.RawFileType;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import io.github.mzmine.datamodel.data.types.numbers.IDType;
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import junit.framework.Assert;

public class TestDatatypes {
  ModularFeatureList flist;
  ModularFeatureListRow data;
  ModularFeatureListRow empty;
  private FeatureStatus detection;
  private ModularFeatureList flistWithRaw;

  @Before
  public void setUp() throws Exception {
    flist = new ModularFeatureList("flist");
    flist.addRowType(new DetectionType());
    flist.addRowType(new MZType());

    detection = FeatureStatus.ESTIMATED;
    // create row
    data = new ModularFeatureListRow(flist);
    data.set(DetectionType.class, detection);
    data.set(MZType.class, 200d);

    flist.addRow(data);

    // second
    List<RawDataFile> raw = new ArrayList<>();
    raw.add(new RawDataFileImpl("Raw"));
    flistWithRaw = new ModularFeatureList("flist name", raw);
  }

  @Test(expected = TypeColumnUndefinedException.class)
  public void testMissingTypeColumn() {
    data.set(AreaType.class, 20f);
  }

  @Test(expected = WrongTypeException.class)
  public void testInsertWrongTypeByInstance() {
    DataType type = new MZType();
    data.set(type, "String");
  }

  @Test(expected = WrongTypeException.class)
  public void testInsertWrongTypeByClass() {
    Class c = MZType.class;
    data.set(c, "String");
  }

  @Test
  public void testDataTypes() {
    // is not defined as column - should be null
    Assert.assertNull(data.get(AreaType.class).orElse(null));

    // detection type is present
    Assert.assertEquals(detection.toString(), data.getDetectionType().toString());
    Assert.assertEquals(detection.toString(), data.getFormattedString(DetectionType.class).get());
    Entry<DataType<FeatureStatus>, Optional<FeatureStatus>> entry =
        data.getEntry(DetectionType.class);
    Assert.assertEquals(detection.toString(),
        entry.getValue().map(v -> entry.getKey().getFormattedString(v)).get());

    // should contain a value
    Assert.assertNotNull(data.get(MZType.class).get());
    Assert.assertNotNull(data.get(new MZType()).get());
  }


  @Test
  public void createMinimalTest() throws IOException {
    // create and add
    flistWithRaw.addRowType(new IDType());
    flistWithRaw.addFeatureType(new DetectionType());

    for (int i = 0; i < 2; i++) {
      RawDataFile raw = flistWithRaw.getRawDataFile(0);
      ModularFeature p = new ModularFeature(flistWithRaw);
      p.set(RawFileType.class, raw);
      p.set(DetectionType.class, FeatureStatus.DETECTED);

      ModularFeatureListRow r = new ModularFeatureListRow(flistWithRaw);
      r.set(IDType.class, (i));
      r.addPeak(raw, p);
      flistWithRaw.addRow(r);
    }
  }

  @Test
  public void testInsertSubType() {
    // adding RawDataFileImpl into a DataType<RawDataFile>
    ModularFeature p = new ModularFeature(flistWithRaw);
    p.set(RawFileType.class, flistWithRaw.getRawDataFile(0));
  }

}
