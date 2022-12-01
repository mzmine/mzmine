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

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.features.FeatureList;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  @NotNull
  Set<RetentionTimeMobilityDataPoint> getDataPoints();

  void setDataPoints(Set<RetentionTimeMobilityDataPoint> dataPoints);

  @NotNull
  Set<Integer> getFrameNumbers();

  void setFrameNumbers(Set<Integer> frameNumbers);

  @NotNull
  Set<MobilityScan> getScanNumbers();

  void setScanNumbers(Set<MobilityScan> scanNumbers);

  MobilityType getMobilityType();

  void setMobilityType(MobilityType mobilityType);

  String getRepresentativeString();

  void setRepresentativeString(String representativeString);

  FeatureList getFeatureList();

  void setFeatureList(FeatureList featureList);
}
