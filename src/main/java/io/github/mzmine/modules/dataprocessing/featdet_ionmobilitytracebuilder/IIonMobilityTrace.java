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

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.features.FeatureList;

/**
 *
 * This Interface represents a collection of frame numbers that form an ion trace within a rt range
 * m/z range mobility range
 *
 * @author Ansgar Korf
 */
public interface IIonMobilityTrace {

  double getMz();

  void setMz(double mz);

  Float getRetentionTime();

  void setRetentionTime(Float retentionTime);

  double getMobility();

  void setMobility(double mobility);

  double getMaximumIntensity();

  void setMaximumIntensity(double maximumIntensity);

  @Nullable
  Range<Float> getRetentionTimeRange();

  void setRetentionTimeRange(Range<Float> retentionTimeRange);

  @Nullable
  Range<Double> getMobilityRange();

  void setMobilityRange(Range<Double> mobilityRange);

  @Nullable
  Range<Double> getMzRange();

  void setMzRange(Range<Double> mzRange);

  @Nullable
  Range<Double> getIntensityRange();

  void setIntensityRange(Range<Double> intensityRange);

  @Nonnull
  Set<RetentionTimeMobilityDataPoint> getDataPoints();

  void setDataPoints(Set<RetentionTimeMobilityDataPoint> dataPoints);

  @Nonnull
  Set<Integer> getFrameNumbers();

  void setFrameNumbers(Set<Integer> frameNumbers);

  @Nonnull
  Set<Integer> getScanNumbers();

  void setScanNumbers(Set<Integer> scanNumbers);

  MobilityType getMobilityType();

  void setMobilityType(MobilityType mobilityType);

  String getRepresentativeString();

  void setRepresentativeString(String representativeString);

  FeatureList getFeatureList();

  void setFeatureList(FeatureList featureList);
}
