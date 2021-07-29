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
 *
 * Edited and modified by Owen Myers (Oweenm@gmail.com)
 */

package io.github.mzmine.util;

import io.github.mzmine.datamodel.IMSImagingRawDataFile;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.FeatureShapeIonMobilityRetentionTimeHeatMapType;
import io.github.mzmine.datamodel.features.types.FeatureShapeMobilogramType;
import io.github.mzmine.datamodel.features.types.FeatureShapeType;
import io.github.mzmine.datamodel.features.types.FeaturesType;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.datamodel.features.types.RawFileType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("null")
public class DataTypeUtils {

  public static final @NotNull
  List<DataType<?>> DEFAULT_CHROMATOGRAPHIC_ROW = List.of(
      new RTType(), new RTRangeType(),
      // needed next to each other for switching between RTType and RTRangeType
      new MZType(), new MZRangeType(), //
      new HeightType(), new AreaType(), new ManualAnnotationType(),
      new FeatureShapeType(), new FeaturesType());

  public static final @NotNull
  List<DataType<?>> DEFAULT_CHROMATOGRAPHIC_FEATURE =
      List.of(new RawFileType(), new DetectionType(), new MZType(),
          new MZRangeType(), new RTType(), new RTRangeType(), new HeightType(), new AreaType(),
          new BestScanNumberType(), new FeatureDataType(), new IntensityRangeType(), new FwhmType(),
          new TailingFactorType(), new AsymmetryFactorType());

  @NotNull
  public static final List<DataType<?>> DEFAULT_ION_MOBILITY_COLUMNS_ROW = List
      .of(new MobilityType(), new MobilityRangeType(),
          new FeatureShapeMobilogramType());

  @NotNull
  public static final List<DataType<?>> DEFAULT_ION_MOBILITY_COLUMNS_FEATURE = List
      .of(new MobilityType(), new MobilityRangeType());

  /**
   * Adds the default chromatogram DataType columns to a feature list
   *
   * @param flist
   */
  public static void addDefaultChromatographicTypeColumns(ModularFeatureList flist) {
    flist.addRowType(DEFAULT_CHROMATOGRAPHIC_ROW);
    flist.addFeatureType(DEFAULT_CHROMATOGRAPHIC_FEATURE);
    // row bindigns are now added in the table
  }

  public static void addDefaultIonMobilityTypeColumns(ModularFeatureList flist) {
    flist.addRowType(DEFAULT_ION_MOBILITY_COLUMNS_ROW);
    flist.addFeatureType(DEFAULT_ION_MOBILITY_COLUMNS_FEATURE);

    Optional<RawDataFile> imagingFile = flist.getRawDataFiles().stream()
        .filter(file -> file instanceof IMSImagingRawDataFile).findFirst();
    if (imagingFile.isPresent()) {
      flist.addFeatureType(new ImageType());
    }

    Optional<RawDataFile> lcIMS = flist.getRawDataFiles().stream()
        .filter(file -> file instanceof IMSRawDataFile && !(file instanceof IMSImagingRawDataFile))
        .findFirst();
    if(lcIMS.isPresent()) {
      flist.addFeatureType(new FeatureShapeIonMobilityRetentionTimeHeatMapType());
    }
  }
}
