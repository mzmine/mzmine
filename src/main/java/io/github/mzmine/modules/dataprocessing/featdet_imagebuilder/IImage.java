package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.ImagingParameters;

public interface IImage {

  double getMz();

  void setMz(double mz);

  ImagingParameters getImagingParameters();

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
