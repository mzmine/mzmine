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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
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
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("null")
public class DataTypeUtils {

  @NotNull
  public static final List<DataType> DEFAULT_CHROMATOGRAPHIC_ROW =
      List.of(new RTType(), new RTRangeType(),
          // needed next to each other for switching between RTType and RTRangeType
          new MZType(), new MZRangeType(), //
          new HeightType(), new AreaType(), new ManualAnnotationType(),
          new FeatureShapeType(), new FeaturesType());

  @NotNull
  public static final List<DataType> DEFAULT_CHROMATOGRAPHIC_FEATURE =
      List.of(new RawFileType(), new DetectionType(), new MZType(),
          new MZRangeType(), new RTType(), new RTRangeType(), new HeightType(), new AreaType(),
          new BestScanNumberType(), new FeatureDataType(), new IntensityRangeType(), new FwhmType(),
          new TailingFactorType(), new AsymmetryFactorType());

  @NotNull
  public static final List<DataType> DEFAULT_ION_MOBILITY_COLUMNS_ROW =
      List.of(new MobilityType(), new MobilityRangeType(),
          new FeatureShapeMobilogramType());

  @NotNull
  public static final List<DataType> DEFAULT_ION_MOBILITY_COLUMNS_FEATURE =
      List.of(new MobilityType(), new MobilityRangeType());

  public static final List<DataType> DEFAULT_IMAGING_COLUMNS_FEATURE = List.of(new ImageType());

  /**
   * Adds the default imaging DataType columns to a feature list
   *
   * @param flist
   */
  public static void addDefaultImagingTypeColumns(ModularFeatureList flist) {
    flist.addFeatureType(DEFAULT_IMAGING_COLUMNS_FEATURE);
  }

  /**
   * Adds the default chromatogram DataType columns to a feature list
   *
   * @param flist
   */
  public static void addDefaultChromatographicTypeColumns(ModularFeatureList flist) {
    flist.addRowType(DEFAULT_CHROMATOGRAPHIC_ROW);
    flist.addFeatureType(DEFAULT_CHROMATOGRAPHIC_FEATURE);
  }

  public static void addDefaultIonMobilityTypeColumns(ModularFeatureList flist) {
    flist.addRowType(DEFAULT_ION_MOBILITY_COLUMNS_ROW);
    flist.addFeatureType(DEFAULT_ION_MOBILITY_COLUMNS_FEATURE);
  }

  /**
   * Apply and activate graphical types for features.
   *
   * @param feature target
   */
  public static void applyFeatureSpecificGraphicalTypes(ModularFeature feature) {
    final RawDataFile raw = feature.getRawDataFile();
    if (raw instanceof ImagingRawDataFile) {
      feature.set(ImageType.class, true);
    } else if (feature.getFeatureData() instanceof IonMobilogramTimeSeries) {
      feature.set(FeatureShapeIonMobilityRetentionTimeHeatMapType.class, true);
    }
  }

  public static void copyTypes(FeatureList source, FeatureList target, boolean featureTypes,
      boolean rowTypes) {
    if (featureTypes) {
      target.addFeatureType(source.getFeatureTypes().values());
    }
    if (rowTypes) {
      target.addRowType(source.getRowTypes().values());
    }
  }


}
