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

import java.util.Map.Entry;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.WrongTypeException;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import io.github.mzmine.datamodel.data.types.numbers.HeightType;
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import junit.framework.Assert;

public class TestDatatypes {
  ModularFeatureListRow data;
  private FeatureStatus detection;

  @Before
  public void setUp() throws Exception {
    detection = FeatureStatus.ESTIMATED;
    data = new ModularFeatureListRow();
    data.set(DetectionType.class, detection);
    data.set(MZType.class, 200);
  }

  @Test(expected = WrongTypeException.class)
  public void testInsertWrongType() {
    // should throw an error
    data.set(HeightType.class, (50d));
  }

  @Test
  public void testDataTypes() {
    Assert.assertNull(data.get(AreaType.class).get());
    // detection type is present
    Assert.assertEquals(detection.toString(), data.getDetectionType().toString());
    Assert.assertEquals(detection.toString(), data.getFormattedString(DetectionType.class).get());
    Entry<DataType<FeatureStatus>, Optional<FeatureStatus>> entry =
        data.getEntry(DetectionType.class);
    Assert.assertEquals(detection.toString(),
        entry.getValue().map(v -> entry.getKey().getFormattedString(v)).get());

    Assert.assertNotNull(data.get(MZType.class).get());
  }

}
