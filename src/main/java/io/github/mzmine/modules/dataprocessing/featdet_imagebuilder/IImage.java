/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.ImagingParameters;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public interface IImage {

  double getMz();

  void setMz(double mz);

  ImagingParameters getImagingParameters();

  PaintScale getPaintScale();

  void setPaintScale(PaintScale paintScale);

  void setImagingParameters(ImagingParameters imagingParameters);

  double getMaximumIntensity();

  void setMaximumIntensity(double maximumIntensity);

  @Nullable
  Range<Double> getMzRange();

  void setMzRange(Range<Double> mzRange);

  @Nullable
  Range<Double> getIntensityRange();

  void setIntensityRange(Range<Double> intensityRange);

  Set<ImageDataPoint> getDataPoints();

  void setDataPoints(Set<ImageDataPoint> dataPoints);

  @Nonnull
  Set<Integer> getScanNumbers();

  void setScanNumbers(Set<Integer> scanNumbers);

  String getRepresentativeString();

  void setRepresentativeString(String representativeString);

  FeatureList getFeatureList();

  void setFeatureList(FeatureList featureList);

}
