/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 *
 * Edited and modified by Owen Myers (Oweenm@gmail.com)
 */

package io.github.mzmine.util;

import java.util.List;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.RowBinding;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.RawFileType;
import io.github.mzmine.datamodel.data.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import io.github.mzmine.datamodel.data.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.data.types.numbers.DataPointsType;
import io.github.mzmine.datamodel.data.types.numbers.HeightType;
import io.github.mzmine.datamodel.data.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.data.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import io.github.mzmine.datamodel.data.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.data.types.numbers.RTType;
import io.github.mzmine.datamodel.data.types.numbers.ScanNumbersType;

@SuppressWarnings("null")
public class DataTypeUtils {
  // bindings from row to features
  public static final @Nonnull List<RowBinding> DEFAULT_CHROMATOGRAPHIC_ROWBINDING =
      List.of(new RowBinding(new MZType(), BindingsType.AVERAGE),
          new RowBinding(new RTType(), BindingsType.AVERAGE),
          new RowBinding(new HeightType(), BindingsType.MAX),
          new RowBinding(new AreaType(), BindingsType.MAX),
          new RowBinding(new RTRangeType(), BindingsType.RANGE),
          new RowBinding(new MZRangeType(), BindingsType.RANGE));
  public static final @Nonnull List<DataType<?>> DEFAULT_CHROMATOGRAPHIC_ROW = List.of(new RTType(),
      new MZType(), new HeightType(), new AreaType(), new RTRangeType(), new MZRangeType());
  public static final @Nonnull List<DataType<?>> DEFAULT_CHROMATOGRAPHIC_FEATURE =
      List.of(new ScanNumbersType(), new RawFileType(), new DetectionType(), new MZType(),
          new RTType(), new HeightType(), new AreaType(), new BestScanNumberType(),
          new DataPointsType(), new RTRangeType(), new MZRangeType(), new IntensityRangeType());


  /**
   * Adds the default chromatogram DataType columns to a feature list
   * 
   * @param flist
   */
  public static void addDefaultChromatographicTypeColumns(ModularFeatureList flist) {
    flist.addRowType(DEFAULT_CHROMATOGRAPHIC_ROW);
    flist.addFeatureType(DEFAULT_CHROMATOGRAPHIC_FEATURE);
    flist.addRowBinding(DEFAULT_CHROMATOGRAPHIC_ROWBINDING);
  }

}
